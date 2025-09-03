package com.gpudb.filesystem.upload;

import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.filesystem.GPUdbFileHandler;
import com.gpudb.filesystem.common.IoTask;
import com.gpudb.filesystem.common.OpMode;
import com.gpudb.filesystem.common.Result;
import com.gpudb.filesystem.utils.GPUdbFileHandlerUtils;
import org.apache.commons.lang3.tuple.Pair;

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
 * {@code filesystem} API. The consequences of using this class directly in
 * client code is not guaranteed and maybe undesirable.

 * This class models a an IO operation of a specific
 * {@link OpMode} i.e., UPLOAD . Different instances of this
 * class will be created for different operations and no instance of this
 * class will be shared. This class handles only multi part upload. It uses
 * a single threaded thread pool internally with an unbounded queue which is
 * used to submit the background tasks modeled by an {@link IoTask} instance,
 * since multi part upload has to be handled sequentially. An instance of this
 * class is used to handle the upload of a single large file. If there are
 * multiple large files to be uploaded each such file will be handled by a
 * new instance of this class.
 */
public class UploadIoJob {

    private final GPUdb db;

    /**
     * A unique ID to identify this overall multipart upload job.  This will be
     * the UUID passed to the /upload/files endpoint to identify the upload.
     */
    private final String jobId;

    private final String uploadLocalFileName;

    private final String uploadRemoteFileName;

    private final UploadOptions uploadOptions;

    private final List<Result> resultList;

    private final FileUploadListener callback;

    /**
     * Fixed thread pool of size 1 uses {@link Executors#newSingleThreadExecutor()}
     */
    private final ExecutorService threadPool;

    private final GPUdbFileHandler.Options fileHandlerOptions;

    /**
     * Constructor
     * Sets up the member variables and the thread pool to execute
     * background jobs.
     *
     * @param db  The {@link GPUdb} instance used to access KiFS.
     * @param fileHandlerOptions  Options for setting up the files for upload.
     * @param localFileName  Fully-qualified name of the local file to upload.
     * @param remoteFileName  Fully-qualified name of the uploaded KiFS file.
     * @param uploadOptions  Options to use during the upload.
     * @param callback  {@link FileUploadListener} that can trigger events at
     *        various stages of the upload process.
     * @throws GPUdbException
     */
    private UploadIoJob(GPUdb db,
                        GPUdbFileHandler.Options fileHandlerOptions,
                        String localFileName,
                        String remoteFileName,
                        UploadOptions uploadOptions,
                        FileUploadListener callback) throws GPUdbException {

        if( localFileName == null || localFileName.trim().isEmpty())
            throw new GPUdbException("File name cannot be null or empty");

        this.db = db;
        this.fileHandlerOptions = fileHandlerOptions;
        this.uploadLocalFileName = localFileName;
        this.uploadRemoteFileName = remoteFileName;
        this.uploadOptions = uploadOptions;
        this.callback = callback;

        this.jobId = UUID.randomUUID().toString();
        this.threadPool = Executors.newSingleThreadExecutor();

        this.resultList = new ArrayList<>();
    }

    /**
     * Factory method for creating a new IoJob instance.
     *
     * @param db  The {@link GPUdb} instance used to access KiFS.
     * @param fileHandlerOptions  Options for setting up the files for upload.
     * @param localFileName  Fully-qualified name of the local file to upload.
     * @param remoteFileName  Fully-qualified name of the uploaded KiFS file.
     * @param uploadOptions  Options to use during the upload.
     * @param callback  {@link FileUploadListener} that can trigger events at
     *        various stages of the upload process.
     * @return {@link Pair} of upload job ID & management object
     *
     * @throws GPUdbException
     */
    public static Pair<String, UploadIoJob> createNewJob(GPUdb db,
                                                         GPUdbFileHandler.Options fileHandlerOptions,
                                                         String localFileName,
                                                         String remoteFileName,
                                                         UploadOptions uploadOptions,
                                                         FileUploadListener callback) throws GPUdbException {
        UploadIoJob newJob = new UploadIoJob(
                db,
                fileHandlerOptions,
                localFileName,
                remoteFileName,
                uploadOptions,
                callback );
        String jobId = newJob.getJobId();

        return Pair.of( jobId, newJob );
    }

    public String getJobId() {
        return this.jobId;
    }

    public ExecutorService getThreadPool() {
        return this.threadPool;
    }

    private CompletableFuture<Result> doUpload(final IoTask task) {
        return CompletableFuture.supplyAsync(task::call, getThreadPool());
    }

    private void handleUploadResult(final IoTask task) throws GPUdbException {
        try {
            Result taskResult = doUpload(task).get();

            if (!taskResult.isSuccessful())
                throw new GPUdbException(String.format("Could not complete upload stage <%s> for file <%s>", task.getMultiPartUploadInfo().getPartOperation().getValue(), taskResult.getFileName()));

            // If the task succeeded, add it to the list
            this.resultList.add( taskResult );

            // If callback exists, post the corresponding message to this op
            if( this.callback != null ) {
                switch (task.getMultiPartUploadInfo().getPartOperation()) {
                    case UPLOAD_PART:
                        this.callback.onPartUpload( taskResult );
                        break;
                    case CANCEL:
                    case COMPLETE:
                        this.callback.onMultiPartUploadComplete( this.resultList );
                        break;
                    default:
                        break;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new GPUdbException(String.format("Could not complete upload stage - %s : %s",task.getMultiPartUploadInfo().getPartOperation().getValue(), e.getMessage()));
        }
    }


    /**
     * This method starts the job to upload the multipart file by submitting
     * different stages {@link com.gpudb.filesystem.upload.MultiPartUploadInfo.MultiPartOperation}
     * to indicate which stage is being executed. Each stage is modeled as an
     * {@link IoTask} which is an instance of {@link Callable} and the
     * task instance is submitted to the {@link #handleUploadResult(IoTask)}
     * member of the class.
     */
    public void start() throws GPUdbException {

        IoTask finalTask = null;

        // Set up the INIT task
        IoTask initTask = createUploadTask(MultiPartUploadInfo.MultiPartOperation.INIT);

        handleUploadResult( initTask );

        // Upload the file chunks with data
        try (RandomAccessFile sourceFile = new RandomAccessFile( this.uploadLocalFileName, "r" );
             FileChannel sourceChannel = sourceFile.getChannel()) {

            final long sourceSize = Files.size( Paths.get( this.uploadLocalFileName ) );
            final int bytesPerSplit = (int) this.fileHandlerOptions.getFileSizeToSplit();
            final long numSplits = sourceSize / bytesPerSplit;
            final long remainingBytes = sourceSize % bytesPerSplit;
            final long totalParts = numSplits + 1;

            int position = 0;
            for ( ; position < numSplits; position++ ) {

                ByteBuffer buffer = ByteBuffer.allocate( bytesPerSplit );
                sourceChannel.read( buffer, (long) position * bytesPerSplit );

                // Set up the upload part task
                IoTask newTask = createUploadTask(
                        MultiPartUploadInfo.MultiPartOperation.UPLOAD_PART,
                        position,
                        buffer,
                        totalParts );

                handleUploadResult( newTask );
            }

            if ( remainingBytes > 0 ) {
                ByteBuffer remaining = ByteBuffer.allocate( (int) remainingBytes );
                sourceChannel.read( remaining, (long) position * bytesPerSplit );

                // Set up a final upload part task, if any remains
                IoTask newTask = createUploadTask(
                        MultiPartUploadInfo.MultiPartOperation.UPLOAD_PART,
                        position,
                        remaining,
                        totalParts);

                handleUploadResult( newTask );
            }

            // Set up the COMPLETE task
            finalTask = createUploadTask(MultiPartUploadInfo.MultiPartOperation.COMPLETE);

            handleUploadResult( finalTask );

        } catch (IOException e) {
            throw new GPUdbException( String.format("Error uploading multi-part file <%s> to <%s>", this.uploadLocalFileName, this.uploadRemoteFileName), e );
        } finally {
            // If COMPLETE task has not been set up, set up the CANCEL task
            if (finalTask == null) {
                finalTask = createUploadTask(MultiPartUploadInfo.MultiPartOperation.CANCEL);
    
                handleUploadResult( finalTask );
            }
        }
    }


    /**
     * This method creates a non-data task for a multi-part upload.
     *
     * This method is called at the beginning and end of a multi-part upload.
     *
     * @param op  Type of {@link MultiPartUploadInfo.MultiPartOperation} this is.
     * @param uuid  This multi-part upload's unique {@link UUID}.
     * @return  The {@link IoTask} handling this upload part.
     */
    private IoTask createUploadTask(MultiPartUploadInfo.MultiPartOperation op) {
        return createUploadTask(op, -1, null, 0);
    }


    /**
     * This method creates a task for a single part of the multi-part upload.
     *
     * This method is called in a loop from the method {@link #start()}.
     *
     * @param op  Type of {@link MultiPartUploadInfo.MultiPartOperation} this is.
     * @param partNumber  Sequence number of this part of the overall upload.
     * @param buffer  Data to upload for this part, in a binary format.
     * @param totalParts  Total number of parts to upload.
     * @return  The {@link IoTask} handling this upload part.
     */
    private IoTask createUploadTask(
            MultiPartUploadInfo.MultiPartOperation op,
            int partNumber,
            ByteBuffer buffer,
            long totalParts) {

        //Set up MultiPartUploadInfo instance as appropriate
        MultiPartUploadInfo uploadInfo = new MultiPartUploadInfo();
        uploadInfo.setUuid( this.jobId );
        uploadInfo.setFileName( this.uploadRemoteFileName );
        uploadInfo.setPartOperation( op );
        uploadInfo.setUploadPartNumber( partNumber + 1 );
        uploadInfo.setTotalParts( totalParts );

        // As we write into a ByteBuffer instance, the current position of the
        // buffer is set to the end of the buffer. The next statement will
        // reset the position to the beginning of the buffer so that once the
        // write operation is complete it is ready to be read from.
        if (buffer != null)
            buffer.flip();

        IoTask newTask = new IoTask(
                this.db,
                this.uploadRemoteFileName,
                uploadInfo,
                this.uploadOptions,
                partNumber + 1,
                buffer );

        return newTask;
    }


    /**
     * This method is used to stop all the running thread pool and return the
     * result list.
     *
     * @see Result
     * @see CompletionService
     * @see FileUploader - method terminateJobs().
     *
     * @return  List of upload results for each file.
     */
    public List<Result> stop() {

        GPUdbFileHandlerUtils.awaitTerminationAfterShutdown( this.threadPool, GPUdbFileHandler.getDefaultThreadPoolTerminationTimeout() );

        return this.resultList;
    }

}
