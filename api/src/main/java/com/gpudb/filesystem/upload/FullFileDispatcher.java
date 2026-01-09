package com.gpudb.filesystem.upload;

import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.GPUdbLogger;
import com.gpudb.filesystem.GPUdbFileHandler;
import com.gpudb.filesystem.common.Result;
import com.gpudb.filesystem.utils.GPUdbFileHandlerUtils;
import com.gpudb.protocol.DownloadFilesResponse;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * This is an internal class and not meant to be used by the end users of the
 * filesystem API. The consequences of using this class directly in client code
 * is not guaranteed and maybe undesirable.
 *
 * This class is used to handle batches of full file uploads
 * using a thread pool of a certain configurable size. The default size for the
 * thread pool is configured in the class GPUdbFileHandler.Options
 */
public class FullFileDispatcher {

    /**
     * The callback if passed in, is used to notify the users when a complete
     * batch of full file uploads has been completed.
     */
    private final FileUploadListener callback;
    private final ExecutorService threadPool;
    private final CompletionService<Result> jobExecutor;
    private final GPUdbFileHandler.Options fileHandlerOptions;

    /**
     * This keeps track of all the tasks which have been submitted to the
     * {@link #jobExecutor} instance. Each task corresponds to a batch of
     * full file uploads where the batch is determined by the limit of the
     * cumulative file sizes and the value returned by the call to
     * {@link GPUdbFileHandler.Options#getFileSizeToSplit()}
     */
    private final List<FullFileUploadTask> taskList;

    public FullFileDispatcher(GPUdbFileHandler.Options fileHandlerOptions, FileUploadListener callback) {
        this.fileHandlerOptions = fileHandlerOptions;
        this.callback = callback;

        // Use a ThreadFactory to create Daemon threads.
        this.threadPool = Executors.newFixedThreadPool(
                this.fileHandlerOptions.getFullFileDispatcherThreadpoolSize(),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = Executors.defaultThreadFactory().newThread(r);
                        t.setDaemon(true);
                        t.setName("FullFileDispatcher-Worker");
                        return t;
                    }
                }
        );

        jobExecutor = new ExecutorCompletionService<>(threadPool);
        this.taskList = new ArrayList<>();
    }

    /**
     * Submit an instance of {@link FullFileUploadTask} to the internal
     * thread pool.
     *
     * @param task a {@link FullFileUploadTask} object
     */
    public void submit( FullFileUploadTask task ) {
        jobExecutor.submit(task);
        taskList.add( task );
    }

    /**
     * Wait for the tasks submitted to the thread pool and collect the
     * results {@link Result} using the futures accessed using the
     * {@link FullFileDispatcher#jobExecutor} object.
     */
    public void collect() throws GPUdbException {

        int countTasks = taskList.size();

        while( countTasks-- > 0 ) {
            try {
                Result result = jobExecutor.take().get();

                if( callback != null ) {
                    callback.onFullFileUpload( result );
                }
            } catch (Exception e) {
                // Determine actual cause
                Throwable cause = (e instanceof ExecutionException) ? e.getCause() : e;

                Result result = new Result();
                result.setSuccessful( false );
                result.setException( (Exception) cause );
                result.setErrorMessage( cause.getMessage() );

                if( callback != null ) {
                    callback.onFullFileUpload( result );
                }

                // Log error but DO NOT shutdown or throw.
                // Allow other tasks in the batch to finish.
                GPUdbLogger.error("Error in full file upload batch: " + cause.getMessage());

                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    // If interrupted, we might want to stop, but for general exceptions we continue.
                }
            }
        }

        taskList.clear();
    }

    /**
     * This method is called to terminate the thread pool once an instance of
     * the class {@link FullFileDispatcher} has done its job.
     */
    public void terminate() {
        this.threadPool.shutdownNow();
        GPUdbFileHandlerUtils.awaitTerminationAfterShutdown( this.threadPool, GPUdbFileHandler.getDefaultThreadPoolTerminationTimeout() );
    }

    /**
     * This class represents a task handling the upload of a bunch of full
     * file uploads.
     */
    public static final class FullFileUploadTask implements Callable<Result> {

        private final GPUdb db;
        private final List<String> remoteFileNames;
        private final List<ByteBuffer> payloads;
        private final Map<String, String> options;

        /**
         * Constructs an upload task with the resources required to upload
         * files to KiFS in a thread.
         *
         * @param db - the {@link GPUdb} object
         * @param remoteFileNames - Names of the files on KIFS
         * @param payloads - The payloads for the files as ByteBuffers
         * @param options - Options passed to {@link GPUdb#uploadFiles(List, List, Map)}
         */
        public FullFileUploadTask(GPUdb db,
                                  List<String> remoteFileNames,
                                  List<ByteBuffer> payloads,
                                  Map<String, String> options) {
            this.db = db;
            this.remoteFileNames = remoteFileNames;
            this.payloads = payloads;
            this.options = options;
        }

        @Override
        public Result call() throws Exception {
            db.uploadFiles( remoteFileNames, payloads, options );

            Result result = new Result();
            result.setFullFileNames( remoteFileNames );
            return result;
        }
    }

    /**
     * This class represents a task handling the download of a bunch of full
     * file downloads.
     */
    public static final class FullFileDownloadTask implements Callable<Result> {

        private final GPUdb db;
        private final List<String> remoteFileNames;
        private final List<ByteBuffer> payloads;
        private final Map<String, String> options;

        /**
         * Constructs a download task with the resources required to download
         * files from KiFS in a thread.
         *
         * @param db - the {@link GPUdb} object
         * @param remoteFileNames - Names of the files on KIFS
         * @param payloads - The payloads for the files as ByteBuffers
         * @param options - Options passed to  {@link GPUdb#downloadFiles(List, List, List, Map)}
         */
        public FullFileDownloadTask(GPUdb db,
                                    List<String> remoteFileNames,
                                    List<ByteBuffer> payloads,
                                    Map<String, String> options) {
            this.db = db;
            this.remoteFileNames = remoteFileNames;
            this.payloads = payloads;
            this.options = options;
        }

        @Override
        public Result call() throws Exception {
            DownloadFilesResponse downloadFilesResponse = db.downloadFiles( remoteFileNames, null, null, options );

            Result result = new Result();
            result.setFullFileNames( remoteFileNames );
            return result;
        }
    }

}