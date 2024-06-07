package com.gpudb.filesystem.upload;

import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.GPUdbLogger;
import com.gpudb.filesystem.GPUdbFileHandler;
import com.gpudb.filesystem.common.IoTask;
import com.gpudb.filesystem.common.OpMode;
import com.gpudb.filesystem.common.Result;
import com.gpudb.filesystem.utils.GPUdbFileHandlerUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * This is an internal class and not meant to be used by the end users of the
 * filesystem API. The consequences of using this class directly in client code
 * is not guaranteed and maybe undesirable.

 * This class models a an IO operation of a specific
 * {@link OpMode} i.e., UPLOAD . Different instances of this
 * class will be created for different operations and no instance of this
 * class will be shared. This class handles only multi part upload. It uses
 * a single threaded threadpool internally with an unbounded queue which is
 * used to submit the background tasks modeled by an {@link IoTask} instance,
 * since multi part upload has to be handled sequentially. An instance of this
 * class is used to handle the upload of a single large file. If there are
 * multiple large files to be uploaded each such file will be handled by a
 * new instance of this class.
 */
public class UploadIoJob {

    private final GPUdb db;

    /**
     * A unique job ID to identify this job. Basically a UUID converted to a
     * String.
     */
    private final String jobId;

    private final String uploadFileName;

    private final String uploadRemoteFileName;

    private final UploadOptions uploadOptions;

    private final String kifsDirName;

    private final List<Result> resultList;

    private final FileUploadListener callback;

    /**
     * Fixed thread pool of size 1 uses {@link Executors#newSingleThreadExecutor()}
     */
    private final ExecutorService threadPool;

    private final OpMode opMode;
    private final GPUdbFileHandler.Options fileHandlerOptions;

    /**
     * Constructor
     * Sets up the member variables and the threadpool to execute
     * background jobs.
     *
     *
     * @param fileHandlerOptions
     * @param opMode - {@link OpMode} - Indicates whether it is an upload or
     *               download.
     * @param dirName
     * @param fileName - String - Name of the file to be uploaded.
     * @param remoteFileName
     * @param uploadOptions
     * @param callback
     * @throws GPUdbException
     */
    private UploadIoJob(GPUdb db,
                        GPUdbFileHandler.Options fileHandlerOptions,
                        OpMode opMode,
                        String dirName,
                        String fileName,
                        String remoteFileName,
                        UploadOptions uploadOptions,
                        FileUploadListener callback) throws GPUdbException {
        if( fileName == null || fileName.trim().isEmpty()) {
            throw new GPUdbException("File name cannot be null or empty");
        }
        this.db = db;
        this.fileHandlerOptions = fileHandlerOptions;
        this.opMode = opMode;
        this.kifsDirName = dirName;
        this.uploadFileName = fileName;
        this.uploadRemoteFileName = remoteFileName;
        this.uploadOptions = uploadOptions;
        this.callback = callback;

        threadPool = Executors.newSingleThreadExecutor();

        this.resultList = new ArrayList<>();

        jobId = UUID.randomUUID().toString();
    }

    /**
     * Factory method for creating a new IoJob instance.
     *
     * @param db - the {@link GPUdb} instance
     * @param fileHandlerOptions - options to configure {@link GPUdbFileHandler}
     * @param opMode - {@link OpMode} - Whether upload or download
     * @param dirName - String - The KIFS directory name
     * @param fileName - String - The fully qualified local file name
     * @param remoteFileName - Name of the KIFS file
     * @param uploadOptions - the {@link UploadOptions} object
     * @param callback - the {@link FileUploadListener} object
     * @return {@link Pair} of String (Job id) and IoJob instances
     *
     * @throws GPUdbException
     */
    public static Pair<String, UploadIoJob> createNewJob(GPUdb db,
                                                         GPUdbFileHandler.Options fileHandlerOptions,
                                                         OpMode opMode,
                                                         String dirName,
                                                         String fileName,
                                                         String remoteFileName,
                                                         UploadOptions uploadOptions,
                                                         FileUploadListener callback) throws GPUdbException {
        UploadIoJob newJob = new UploadIoJob( db,
                fileHandlerOptions,
                opMode,
                dirName,
                fileName,
                remoteFileName, uploadOptions,
                callback );
        String jobId = newJob.getJobId();

        return Pair.of( jobId, newJob );
    }

    public String getJobId() {
        return jobId;
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    private CompletableFuture<Result> doUpload(final IoTask task) {
        return CompletableFuture.supplyAsync(task::call, getThreadPool());
    }

    private void handleUploadResult(final IoTask task) throws GPUdbException {
        try {
            Result taskResult = doUpload(task).get();
            resultList.add( taskResult );
            if( callback != null ) {
                callback.onPartUpload( taskResult );
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new GPUdbException(String.format("Could not complete upload stage - %s : %s",task.getMultiPartUploadInfo().getPartOperation().getValue(), e.getMessage()));
        }

    }

    /** This method starts the job to upload the multipart file by submitting
     * different stages {@link com.gpudb.filesystem.upload.MultiPartUploadInfo.MultiPartOperation}
     * to indicate which stage is being executed. Each stage is modeled as an
     * {@link IoTask} which is an instance of {@link Callable} and the
     * task instance is submitted to the {@link #handleUploadResult(IoTask)} member of the class
     */
    public void start() throws GPUdbException {

        String kifsFileName = kifsDirName + File.separator
                + StringUtils.substringAfterLast( uploadFileName, File.separator );

        //Setup the INIT task
        MultiPartUploadInfo initInfo = new MultiPartUploadInfo();
        initInfo.setFileName( kifsFileName );
        initInfo.setPartOperation( MultiPartUploadInfo.MultiPartOperation.INIT );
        UUID uuid = UUID.randomUUID();
        initInfo.setUuid( uuid.toString() );

        IoTask initTask = new IoTask( db,
                OpMode.UPLOAD,
                jobId,
                uploadRemoteFileName,
                uploadOptions,
                null,
                0,
                null);
        initTask.setMultiPartUploadInfo( initInfo );

        handleUploadResult( initTask );

        //Upload the file chunks with data
        try (RandomAccessFile sourceFile = new RandomAccessFile( uploadFileName, "r" );
             FileChannel sourceChannel = sourceFile.getChannel()) {

            final long sourceSize = Files.size( Paths.get( uploadFileName ) );
            final int bytesPerSplit = (int) fileHandlerOptions.getFileSizeToSplit();
            final long numSplits = sourceSize / bytesPerSplit;
            final long remainingBytes = sourceSize % bytesPerSplit;
            final long totalParts = numSplits + 1;

            int position = 0;
            for ( ; position < numSplits; position++ ) {

                ByteBuffer buffer = ByteBuffer.allocate( bytesPerSplit );
                sourceChannel.read( buffer, (long) position * bytesPerSplit );

                IoTask newTask = createPartialUploadTask( uuid,
                        uploadRemoteFileName,
                        position,
                        buffer,
                        totalParts );

                handleUploadResult( newTask );
            }

            if ( remainingBytes > 0 ) {
                ByteBuffer remaining = ByteBuffer.allocate( (int) remainingBytes );
                sourceChannel.read( remaining, (long) position * bytesPerSplit );

                //Set up MultiPartUploadInfo instance as appropriate
                IoTask newTask = createPartialUploadTask( uuid,
                        uploadRemoteFileName,
                        position,
                        remaining,
                        totalParts);

                handleUploadResult( newTask );

            }
        } catch (IOException e) {
            GPUdbLogger.error( e.getMessage() );
            throw new GPUdbException( e.getMessage() );
        }

        //Setup the COMPLETE task
        MultiPartUploadInfo completeInfo = new MultiPartUploadInfo();
        completeInfo.setFileName( kifsFileName );
        completeInfo.setPartOperation( MultiPartUploadInfo.MultiPartOperation.COMPLETE );
        completeInfo.setUuid( uuid.toString() );

        IoTask completeTask = new IoTask( db,
                OpMode.UPLOAD,
                jobId,
                uploadRemoteFileName,
                uploadOptions,
                null,
                0,
                null);
        completeTask.setMultiPartUploadInfo( completeInfo );

        handleUploadResult( completeTask );
        if( callback != null ) {
            callback.onMultiPartUploadComplete( resultList );
        }

    }

    /**
     * This method uploads a single part of the multi part upload with data
     * and the {@link com.gpudb.filesystem.upload.MultiPartUploadInfo.MultiPartOperation}
     * set to {@link com.gpudb.filesystem.upload.MultiPartUploadInfo.MultiPartOperation#UPLOAD_PART}
     *
     * This method is called in a loop from the method {@link #start()}.
     *
     * @param uuid - {@link UUID} - The UUID as String identifying the current
     *             multi part upload
     * @param kifsFileName - Name of the KIFS file
     * @param partNumber - int - This is the multi file part number
     * @param buffer - ByteBuffer - This is the data in a binary format
     *
     * @param totalParts - total number of parts to upload
     * @return - IoTask - An instance of an {@link IoTask}.
     */
    private IoTask createPartialUploadTask(UUID uuid,
                                           String kifsFileName,
                                           int partNumber,
                                           ByteBuffer buffer,
                                           long totalParts) {

        //Set up MultiPartUploadInfo instance as appropriate
        MultiPartUploadInfo uploadInfo = new MultiPartUploadInfo();
        uploadInfo.setUuid( uuid.toString() );
        uploadInfo.setFileName( kifsFileName );

        uploadInfo.setPartOperation( MultiPartUploadInfo.MultiPartOperation.UPLOAD_PART );

        uploadInfo.setUploadPartNumber( partNumber + 1 );
        uploadInfo.setTotalParts( totalParts );

        // As we write into a ByteBuffer instance, the current position of the
        // buffer is set to the end of the buffer. The next statement will
        // reset the position to the beginning of the buffer so that once the
        // write operation is complete it is ready to be read from.
        buffer.flip();

        IoTask newTask = new IoTask( db,
                opMode,
                jobId,
                kifsFileName,
                uploadOptions,
                null,
                partNumber + 1,
                buffer );
        newTask.setMultiPartUploadInfo( uploadInfo );

        return newTask;
    }

    /**
     * This method is used to stop all the running threadpool and return the
     * result list.
     *
     * @see Result
     * @see CompletionService
     * @see FileUploader - method terminateJobs().
     *
     * @return - A list of Result objects.
     */
    public List<Result> stop() {

        GPUdbFileHandlerUtils.awaitTerminationAfterShutdown( this.threadPool, GPUdbFileHandler.getDefaultThreadPoolTerminationTimeout() );

        return resultList;

    }


}