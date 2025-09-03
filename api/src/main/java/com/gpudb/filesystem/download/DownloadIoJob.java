package com.gpudb.filesystem.download;

import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.GPUdbLogger;
import com.gpudb.filesystem.GPUdbFileHandler;
import com.gpudb.filesystem.common.*;
import com.gpudb.filesystem.utils.GPUdbFileHandlerUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/** This class models a an I/O operation of a specific
 * {@link OpMode} i.e., UPLOAD or DOWNLOAD. Different instances of this
 * class will be created for different operations and no instance of this
 * class will be shared.
 */
public class DownloadIoJob {

    private final GPUdb db;

    /**
     * A unique ID to identify this overall multipart download job.
     */
    private final String jobId;

    /**
     *
     */
    private final String downloadRemoteFileName;

    /**
     *
     */
    private final String downloadLocalFileName;

    /**
     *
     */
    private final DownloadOptions downloadOptions;

    private final List<Result> resultList;

    private final FileDownloadListener callback;
    /**
     * Fixed thread pool of size 1
     */
    private final ExecutorService threadPool;

    private final KifsFileInfo kifsFileInfo;
    private final GPUdbFileHandler.Options fileHandlerOptions;

    /**
     * Constructor
     *
     * @param db - The {@link GPUdb} instance
     * @param fileHandlerOptions
     * @param remoteFileName - Name of the KIFS file to be downloaded
     * @param localFileName - Name of the local file with directory
     * @param kifsFileInfo - A {@link KifsFileInfo} object
     * @param downloadOptions - A {@link DownloadOptions} object.
     * @param callback - A {@link FileDownloadListener} implementation
     * @throws GPUdbException
     */
    private DownloadIoJob(GPUdb db,
                          GPUdbFileHandler.Options fileHandlerOptions,
                          String remoteFileName,
                          String localFileName,
                          KifsFileInfo kifsFileInfo,
                          DownloadOptions downloadOptions,
                          FileDownloadListener callback) throws GPUdbException {
        if( remoteFileName == null || remoteFileName.trim().isEmpty()) {
            throw new GPUdbException("File name cannot be null or empty");
        }

        this.db = db;
        this.fileHandlerOptions = fileHandlerOptions;
        this.downloadRemoteFileName = remoteFileName;
        this.downloadLocalFileName = localFileName;
        this.kifsFileInfo = kifsFileInfo;
        this.downloadOptions = downloadOptions;
        this.callback = callback;

        this.jobId = UUID.randomUUID().toString();
        this.threadPool = Executors.newSingleThreadExecutor();

        this.resultList = new ArrayList<>();
    }

    public String getDownloadFileName() {
        return this.downloadRemoteFileName;
    }

    public String getDownloadLocalFileName() {
        return this.downloadLocalFileName;
    }

    /**
     * Static helper method to create new instance of {@link DownloadIoJob}
     *
     * @param db - The {@link GPUdb} instance
     * @param fileHandlerOptions
     * @param remoteFileName - Name of the KIFS file to be downloaded
     * @param localFileName - Name of the local file with directory
     * @param kifsFileInfo - A {@link KifsFileInfo} object
     * @param downloadOptions - A {@link DownloadOptions} object.
     * @param callback - A {@link FileDownloadListener} implementation
     *
     * @return - A {@link Pair} of JobId and {@link DownloadIoJob} object.
     * @throws GPUdbException
     */
    public static Pair<String, DownloadIoJob> createNewJob(GPUdb db,
                                                           GPUdbFileHandler.Options fileHandlerOptions,
                                                           String remoteFileName,
                                                           String localFileName,
                                                           KifsFileInfo kifsFileInfo,
                                                           DownloadOptions downloadOptions,
                                                           FileDownloadListener callback) throws GPUdbException {
        DownloadIoJob newJob = new DownloadIoJob(
                db,
                fileHandlerOptions,
                remoteFileName,
                localFileName,
                kifsFileInfo,
                downloadOptions,
                callback);
        String jobId = newJob.getJobId();

        return Pair.of( jobId, newJob);
    }

    public String getJobId() {
        return this.jobId;
    }

    public ExecutorService getThreadPool() {
        return this.threadPool;
    }

    private CompletableFuture<Result> doDownload(final IoTask task) {
        return CompletableFuture.supplyAsync(task::call, getThreadPool());
    }

    private void handleDownloadResult(FileChannel out, final IoTask task, String normalizedName) throws GPUdbException {
        try {
            Result taskResult = doDownload(task).get();
            this.resultList.add( taskResult );
            
            ByteBuffer data = taskResult.getDownloadInfo().getData();
            out.write(data);
            taskResult.getDownloadInfo().setData(null);

            if( this.callback != null )
                this.callback.onPartDownload(taskResult);

        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new GPUdbException(String.format("Could not complete download part #%s : %s",task.getMultiPartDownloadInfo().getDownloadPartNumber(), e.getMessage()));
        }
    }

    /**
     * This method starts the job to download the multipart file by submitting
     * different segments of the file according to the value returned by
     * getFileSizeToSplit() method of GPUdbFileHandler.Options class.
     * Each segment of the file is submitted as a new {@link IoTask} which is
     * run in an independent thread
     * @throws GPUdbException
     *
     * @see GPUdbFileHandler#getOptions()
     */
    public void start() throws GPUdbException {

        final long sourceSize = this.kifsFileInfo.getFileSize();
        final long bytesPerSplit = this.fileHandlerOptions.getFileSizeToSplit();
        final long numSplits = sourceSize / bytesPerSplit;
        final long totalParts = numSplits + 1;

        long partNo = 0;
        long offset = 0;

        Path normalizedPath = Paths.get( this.downloadLocalFileName ).normalize().toAbsolutePath();

        if( Files.notExists( normalizedPath ) || this.downloadOptions.isOverwriteExisting() ) {

            try ( FileChannel outChannel = FileChannel.open(normalizedPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING) ) {
    
                while( offset < sourceSize ) {
    
                    MultiPartDownloadInfo downloadInfo = new MultiPartDownloadInfo();
    
                    downloadInfo.setReadOffset( offset );
                    downloadInfo.setReadLength( bytesPerSplit );
                    downloadInfo.setDownloadPartNumber( partNo );
                    downloadInfo.setTotalParts( totalParts );
    
                    IoTask newTask = new IoTask(
                            this.db,
                            this.downloadRemoteFileName,
                            downloadInfo,
                            this.downloadOptions);
    
                    handleDownloadResult(outChannel, newTask, normalizedPath.toString());
    
                    offset += bytesPerSplit;
                    partNo++;
                }
    
            } catch (IOException e) {
                GPUdbLogger.error( e.getMessage() );
                throw new GPUdbException(e.getMessage());
            }
        }
    }

    /**
     * This method is used to stop all the running {@link IoTask} instances
     * by shutting down the thread pool running them after they're completed.
     * Each completed {@link Future} object will return a {@link Result} object
     * which can be examined to get the exact status of the background job.
     *
     * @see Result
     * @see CompletionService
     * @see FileDownloader - method terminateJobs().
     *
     * @return - List<Result> - A list of Result objects.
     */
    public List<Result> stop() {
        GPUdbFileHandlerUtils.awaitTerminationAfterShutdown( this.threadPool,
                GPUdbFileHandler.getDefaultThreadPoolTerminationTimeout() );

        return this.resultList;
    }

}