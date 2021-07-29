package com.gpudb.filesystem.upload;

import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.GPUdbLogger;
import com.gpudb.filesystem.GPUdbFileHandler;
import com.gpudb.filesystem.common.FileOperation;
import com.gpudb.filesystem.common.Result;
import com.gpudb.filesystem.utils.GPUdbFileHandlerUtils;

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
 * using a threadpool of a certain configurable size. The default size for the
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
    private List<FullFileUploadTask> taskList;

    public FullFileDispatcher(GPUdbFileHandler.Options fileHandlerOptions, FileUploadListener callback) {
        this.fileHandlerOptions = fileHandlerOptions;
        this.callback = callback;
        threadPool = Executors.newFixedThreadPool( this.fileHandlerOptions.getFullFileDispatcherThreadpoolSize() );
        jobExecutor = new ExecutorCompletionService<>(threadPool);

    }

    /**
     * Submit an instance of {@link FullFileUploadTask} to the internal
     * threadpool.
     *
     * @param task a {@link FullFileUploadTask} object
     */
    public void submit( FullFileUploadTask task ) {
        if( taskList == null ) {
            taskList = new ArrayList<>();
        }
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
                Result result = new Result();
                result.setSuccessful( false );
                result.setException( e );
                result.setErrorMessage( e.getMessage() );

                if( callback != null ) {
                    callback.onFullFileUpload( result );
                }
                GPUdbLogger.error( e.getMessage() );
                throw new GPUdbException( e.getMessage() );
            }
        }

        taskList.clear();

    }

    /**
     * This method is called to terminate the threadpool once an instance of
     * the class {@link FullFileDispatcher} has done its job.
     */
    public void terminate() {
        GPUdbFileHandlerUtils.awaitTerminationAfterShutdown( this.threadPool, GPUdbFileHandler.getDefaultThreadPoolTerminationTimeout() );
    }

    /**
     * This class represents a task handling the upload of a bunch of full
     * file uploads. This class models a single batch of full file uploads
     * in terms of the names on KIFS and the corresponding payloads as
     * ByteBuffers. The {@link #call()} method is the one which runs in the
     * thread and calls the endpoint {@link GPUdb#uploadFiles(List, List, Map)}
     * to upload the current batch of files to KIFS
     */
    public static final class FullFileUploadTask implements Callable<Result> {

        private final GPUdb db;

        private final List<String> remoteFileNames;

        private final List<ByteBuffer> payloads;

        private final Map<String, String> options;

        /**
         *
         * @param db - the {@link GPUdb} object
         * @param remoteFileNames - Names of the files on KIFS
         * @param payloads - The payloads for the files as ByteBuffers
         * @param options - Options passed to 'uploadFiles' ebd point
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
}
