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

/**
 * This is an internal class and not meant to be used by the end users of the
 * {@code filesystem} API. The consequences of using this class directly in
 * client code is not guaranteed and maybe undesirable.

 * This class models a download {@link OpMode}. Different instances of this
 * class will be created for different operations and no instance of this
 * class will be shared.
 */
public class DownloadIoJob {

    private final GPUdb db;

    /**
     * A unique ID to identify this overall multipart download job.
     */
    private final String jobId;

    private final String downloadRemoteFileName;
    private final String downloadLocalFileName;
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
     * Constructs the member variables and the thread pool to execute
     * background download jobs.
     *
     * @param db  The {@link GPUdb} instance used to access KiFS.
     * @param fileHandlerOptions  Options for setting up the files for download.
     * @param remoteFileName  Name of the KIFS file to be downloaded
     * @param localFileName  Fully-qualified name of the downloaded local file.
     * @param kifsFileInfo  a {@link KifsFileInfo} object
     * @param downloadOptions  options to use during the download.
     * @param callback  a {@link FileDownloadListener} that can trigger events at
     *        various stages of the download process.
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

        // FIX 3: Use Daemon threads to prevent JVM hangs
        this.threadPool = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("Download-Worker-" + this.jobId);
            return t;
        });

        this.resultList = new ArrayList<>();
    }

    public String getDownloadFileName() {
        return this.downloadRemoteFileName;
    }

    public String getDownloadLocalFileName() {
        return this.downloadLocalFileName;
    }

    /**
     * Creates a new job for downloading a file.
     *
     * @param db  The {@link GPUdb} instance used to access KiFS.
     * @param fileHandlerOptions  Options for setting up the files for download.
     * @param remoteFileName  Name of the KIFS file to be downloaded
     * @param localFileName  Fully-qualified name of the downloaded local file.
     * @param kifsFileInfo  a {@link KifsFileInfo} object
     * @param downloadOptions  options to use during the download.
     * @param callback  a {@link FileDownloadListener} that can trigger events at
     *        various stages of the download process.
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

    /**
     * Handles the execution of the given download stage task, notifying any
     * callback configured for this download.  Properly handles job cancellation
     * and ensures job terminates early upon error.
     * 
     * @param out  the file channel to write the downloaded file to
     * @param task  download task to execute and report on
     */
    private void handleDownloadResult(FileChannel out, final IoTask task, String normalizedName) throws GPUdbException {
        // Capture the future so we can cancel it if interrupted
        CompletableFuture<Result> future = doDownload(task);

        try {
            Result taskResult = future.get();

            // FIX 1: Fail Fast - Check if the inner task logic actually succeeded
            if (!taskResult.isSuccessful()) {
                String errorMsg = String.format("Could not complete download part #%s for file <%s>",
                        task.getMultiPartDownloadInfo().getDownloadPartNumber(),
                        taskResult.getFileName());

                // If there is an exception attached to the result, append it
                if (taskResult.getException() != null) {
                    errorMsg += " : " + taskResult.getException().getMessage();
                }

                GPUdbLogger.error(errorMsg);
                throw new GPUdbException(errorMsg);
            }

            this.resultList.add( taskResult );

            ByteBuffer data = taskResult.getDownloadInfo().getData();
            out.write(data);
            taskResult.getDownloadInfo().setData(null); // Release memory

            if( this.callback != null )
                this.callback.onPartDownload(taskResult);

        } catch (IOException | InterruptedException | ExecutionException e) {
            // FIX 2: If interrupted, cancel the running task to free the thread
            if (e instanceof InterruptedException) {
                future.cancel(true); // Interrupt the worker thread
                Thread.currentThread().interrupt(); // Restore interrupt status
            }

            throw new GPUdbException(String.format("Could not complete download part #%s : %s",
                    task.getMultiPartDownloadInfo().getDownloadPartNumber(), e.getMessage()));
        }
    }

    /**
     * Starts the job to download the multipart file by submitting
     * different segments of the file for download in an independent thread.
     * @throws GPUdbException
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

                    // Check for external interruption during the loop
                    if (Thread.currentThread().isInterrupted()) {
                        throw new GPUdbException("Download job interrupted");
                    }

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
     * This method is used to stop all the running {@link IoTask} instances.
     */
    public List<Result> stop() {
        // FIX 4: Force immediate shutdown
        this.threadPool.shutdownNow();

        GPUdbFileHandlerUtils.awaitTerminationAfterShutdown( this.threadPool,
                GPUdbFileHandler.getDefaultThreadPoolTerminationTimeout() );

        return this.resultList;
    }

}