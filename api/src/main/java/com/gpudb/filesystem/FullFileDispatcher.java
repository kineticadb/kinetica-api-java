package com.gpudb.filesystem;

import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.GPUdbLogger;
import com.gpudb.filesystem.common.Result;
import com.gpudb.filesystem.upload.FileUploadListener;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
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
class FullFileDispatcher {

    /**
     * The callback if passed in, is used to notify the users when a complete
     * batch of full file uploads has been completed.
     */
    private final FileUploadListener callback;
    private final ExecutorService threadPool;
    private final CompletionService<Result> jobExecutor;
    private final GPUdbFileHandler.Options fileHandlerOptions;

    /**
     * The task behind each result still to be collected.  Results arrive in
     * completion order, so this is what names a task's files when the task
     * itself could not:  one that ended in an error rather than a result, or
     * one still running when the wait for it was interrupted.
     */
    private final Map<Future<Result>, FullFileUploadTask> inFlight = new IdentityHashMap<>();

    /**
     * This keeps track of all the tasks which have been submitted to the
     * {@link #jobExecutor} instance. Each task corresponds to a batch of
     * full file uploads where the batch is determined by the limit of the
     * cumulative file sizes and the value returned by the call to
     * {@link GPUdbFileHandler.Options#getFileSizeToSplit()}
     */
    private final List<FullFileUploadTask> taskList;

    FullFileDispatcher(GPUdbFileHandler.Options fileHandlerOptions, FileUploadListener callback) {
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

        this.jobExecutor = new ExecutorCompletionService<>(this.threadPool);
        this.taskList = new ArrayList<>();
    }

    /**
     * Submit an instance of {@link FullFileUploadTask} to the internal
     * thread pool.
     *
     * @param task a {@link FullFileUploadTask} object
     */
    void submit( FullFileUploadTask task ) {
        this.inFlight.put( this.jobExecutor.submit(task), task );
        this.taskList.add( task );
    }

    /**
     * Wait for the tasks submitted to the thread pool and collect the
     * results {@link Result} using the futures accessed using the
     * {@link FullFileDispatcher#jobExecutor} object.
     *
     * @return A list of {@link Result} objects for all upload tasks (both successful and failed).
     */
    List<Result> collect() {

        List<Result> results = new ArrayList<>();
        int countTasks = this.taskList.size();

        while( countTasks-- > 0 ) {
            Future<Result> done;
            try {
                done = this.jobExecutor.take();
            } catch (InterruptedException e) {
                // The wait was cut short:  every task still in flight is
                // reported as failed, each naming its files, and the wait is
                // not resumed
                Thread.currentThread().interrupt();
                for( FullFileUploadTask task : this.inFlight.values() ) {
                    report( failure( e, task.remoteFileNames ), results );
                }
                this.inFlight.clear();
                break;
            }

            FullFileUploadTask task = this.inFlight.remove( done );
            Result result;
            try {
                // A task reports its own failure, in a result that names its
                // files; see FullFileUploadTask#call
                result = done.get();
            } catch (ExecutionException | CancellationException | InterruptedException e) {
                // The task ended without a result -- an error, or a failure
                // its own handling did not cover; its files are named here
                Throwable cause = (e instanceof ExecutionException) ? e.getCause() : e;
                result = failure( cause, (task == null) ? null : task.remoteFileNames );
            }
            report( result, results );
        }

        this.taskList.clear();
        return results;
    }

    /**
     * This method is called to terminate the thread pool once an instance of
     * the class {@link FullFileDispatcher} has done its job.
     */
    void terminate() {
        this.threadPool.shutdownNow();
        GPUdbFileHandlerUtils.awaitTerminationAfterShutdown( this.threadPool, GPUdbFileHandler.getDefaultThreadPoolTerminationTimeout() );
    }

    /**
     * A result for a task that produced none, naming the files it carried,
     * as the listener is promised in either case.
     */
    private static Result failure( Throwable cause, List<String> fileNames ) {
        Result result = new Result();
        result.setSuccessful( false );
        result.setFullFileNames( fileNames );
        result.setException( (cause instanceof Exception) ? (Exception) cause : new GPUdbException( cause.toString(), cause ) );

        // The same shape as a task's own report, naming the files; an
        // interrupt has no message of its own, so it is said in words
        String reason;
        if ( cause instanceof InterruptedException )
            reason = "the wait for it was interrupted";
        else if ( cause instanceof CancellationException )
            reason = "it was cancelled";
        else
            reason = (cause.getMessage() == null) ? cause.toString() : cause.getMessage();
        result.setErrorMessage( String.format( "Failed to upload %s: %s", fileNames, reason ) );
        return result;
    }


    /**
     * Records a task's result and hands it to the listener; a failure is
     * logged, and the rest of the batch goes on regardless.
     */
    private void report( Result result, List<Result> results ) {
        results.add( result );

        if( this.callback != null ) {
            this.callback.onFullFileUpload( result );
        }

        if( !result.isSuccessful() )
            GPUdbLogger.error("Error in full file upload batch: " + result.getErrorMessage());
    }


    /**
     * This class represents a task handling the upload of a bunch of full
     * file uploads.
     */
    static final class FullFileUploadTask implements Callable<Result> {

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
        FullFileUploadTask(GPUdb db,
                           List<String> remoteFileNames,
                           List<ByteBuffer> payloads,
                           Map<String, String> options) {
            this.db = db;
            this.remoteFileNames = remoteFileNames;
            this.payloads = payloads;
            this.options = options;
        }

        /**
         * Uploads the files of this task in one request.
         *
         * A failure is returned, not thrown, so that the result can name the
         * files of the batch:  the one request fails for all of them, and the
         * caller could not otherwise tell which files were not uploaded.
         *
         * @return  the outcome of the upload, with the KiFS names of the
         *          files in the batch, whether it succeeded or failed
         */
        @Override
        public Result call() {
            Result result = new Result();
            result.setFullFileNames( this.remoteFileNames );

            try {
                this.db.uploadFiles( this.remoteFileNames, this.payloads, this.options );
                result.setSuccessful( true );
            } catch (Exception e) {
                result.setSuccessful( false );
                result.setException( e );
                result.setErrorMessage( String.format( "Failed to upload %s: %s", this.remoteFileNames, e.getMessage() ) );
            }

            return result;
        }
    }

}
