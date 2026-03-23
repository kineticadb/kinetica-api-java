package com.gpudb.filesystem;

import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.GPUdbLogger;
import com.gpudb.filesystem.common.OpMode;
import com.gpudb.filesystem.common.Result;
import com.gpudb.filesystem.upload.FileUploadListener;
import com.gpudb.filesystem.upload.UploadOptions;

import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * This is an internal class and not meant to be used by the end users of the
 * {@code filesystem} API. The consequences of using this class directly in
 * client code is not guaranteed and maybe undesirable.
 *
 * This class handles uploading of either single part file or multiple part
 * uploads. This class extends the class {@link FileOperation} and provides
 * additional functionalities of creating instances of {@link UploadIoJob},
 * starting them and waiting for them to terminate.
 *
 * The main exposed method to call is {@link #upload()} which calls two private
 * methods named {@link #uploadFullFiles()} and {@link #uploadMultiPartFiles()}
 * respectively.
 *
 * The method {@link #uploadFullFiles()} does the upload by calling the Java
 * endpoint to upload all files in one go.
 *
 * The method {@link #uploadMultiPartFiles()} does the uploads by creating
 * background threads since each file could take a long time to upload.
 * The multiple parts of a single file are uploaded sequentially in a single
 * thread and multiple files are uploaded in different threads.
 */
class FileUploader extends FileOperation {

    private final FileUploadListener callback;

    private UploadOptions uploadOptions;

    private int rankForLocalDist = 0;


    /**
     * Constructs a new {@link FileUploader} manager for uploading a given set
     * of files to a given KiFS directory.
     *
     * @param db  The {@link GPUdb} instance used to access KiFS.
     * @param fileNames  List of names of the local files to upload.
     * @param remoteDirName  Name of KiFS directory to upload to.
     * @param options  The {@link UploadOptions} object which is used to
     *        configure the upload operation.
     * @param callback  The callback {@link FileUploadListener} for this upload
     *        manager to notify as the upload job progresses.
     * @param fileHandlerOptions  Options for setting up the files for transfer.
     */
    FileUploader(final GPUdb db,
                 final List<String> fileNames,
                 final String remoteDirName,
                 final UploadOptions options,
                 final FileUploadListener callback,
                 final GPUdbFileHandler.Options fileHandlerOptions) throws GPUdbException {

        super( db, OpMode.UPLOAD, fileNames, remoteDirName, options.isRecursive(), fileHandlerOptions);

        this.uploadOptions = options;
        this.callback = callback;
    }


    /**
     * This method does the upload for files which are small enough to be
     * uploaded in one go. Right now the size threshold for such files have been
     * kept at 60 MB. In case a callback object {@link FileUploadListener} has
     * been specified, {@link FileUploadListener#onFullFileUpload(Result)} will
     * be called.
     *
     * @return A list of {@link Result} objects for all full file uploads (both successful and failed).
     */
    private List<Result> uploadFullFiles() {

        FullFileDispatcher fullFileDispatcher = new FullFileDispatcher( this.fileHandlerOptions, this.callback);

        // Collect all results encountered during the upload process
        List<Result> allResults = new ArrayList<>();

        // While iterating over the list of maps keep track of the count
        // Once the count reaches the thread pool size
        // wait for all the submitted tasks to complete and
        // then proceed with the next bunch of tasks if there are any
        int count = 0;

        // Iterate over each map in the list 'listOfFullFileNameToRemoteFileNameMap'
        // Each map contains
        // A. The key is the local file name
        // B. The value is the fully constructed file name on the KIFS
        this.fullFileBatchManager.createBatches();
        int batches = this.fullFileBatchManager.getNumberOfBatches();

        if (batches == 0)
            return allResults;

        // Process the batches in a loop
        for( int n=0; n < batches; n++ ) {

            // Each batch is a Map of localFileName to remoteFileName
            Map<String, String> batch = this.fullFileBatchManager.getNthBatch( n );

            List<String> fullFileBatch = new ArrayList<>( batch.keySet() );

            // Used to pass the payloads to the endpoint 'upload/files'
            List<ByteBuffer> payloads = new ArrayList<>( fullFileBatch.size() );

            // Used to pass the file names to the endpoint 'upload/files'
            List<String> remoteFileNames = new ArrayList<>( fullFileBatch.size() );

            //Read the files as ByteBuffers
            for ( String fileName: fullFileBatch ) {
                try {
                    ByteBuffer payload = ByteBuffer.wrap( Files.readAllBytes( Paths.get( fileName ) ) );
                    payloads.add( payload );
                    remoteFileNames.add( batch.get( fileName ) );

                } catch (IOException e) {
                    // Log error and collect result, but continue to next file instead of aborting the whole batch
                    String errorMessage = String.format("Failed to read source file <%s>: %s", fileName, e.getMessage());
                    GPUdbLogger.error(errorMessage);

                    Result result = new Result();
                    result.setFileName(fileName);
                    result.setSuccessful(false);
                    result.setException(e);
                    result.setErrorMessage(errorMessage);
                    allResults.add(result);

                    // Notify callback about the file read failure
                    if (this.callback != null) {
                        this.callback.onFullFileUpload(result);
                    }
                }
            }

            if(!payloads.isEmpty()) {
                //Handle upload options here first
                Map<String, String> options = new HashMap<>();

                if( this.uploadOptions.getTtl() > 0 ) {
                    options.put( "ttl", String.valueOf( this.uploadOptions.getTtl() ) );
                }

                // Create a FullFileUploadTask instance with all the details
                // needed to invoke the 'upload/files' endpoint
                FullFileDispatcher.FullFileUploadTask task =
                        new FullFileDispatcher.FullFileUploadTask(
                                this.db,
                                remoteFileNames,
                                payloads,
                                options);

                // Submit the task instance to the thread pool
                fullFileDispatcher.submit( task );


            } // End of processing files in each partition

            count++;
            // Check if the value of count has reached the size of the
            // thread pool and if it has, get the Results of the
            // operations and reset the payloads list and count.
            if( count % this.fileHandlerOptions.getFullFileDispatcherThreadpoolSize() == 0 ) {
                // Wait for the tasks to complete and collect results
                List<Result> batchResults = fullFileDispatcher.collect();
                allResults.addAll(batchResults);
                count = 0;
                payloads.clear();
            }

        } // End of processing batches

        // Wait for jobs to complete if the only batch is less than size of the
        // thread pool or if the size of the last batch is not a multiple of
        // the size of the thread pool.
        List<Result> finalBatchResults = fullFileDispatcher.collect();
        allResults.addAll(finalBatchResults);

        fullFileDispatcher.terminate();

        return allResults;

    } //End uploadFullFiles method


    /**
     * This method uploads files which are candidates for multi-part uploads as
     * determined from their size.
     * <p>
     * This method first checks whether the KIFS directory exists or not and
     * creates it if it doesn't.
     * <p>
     * Then it creates a list of {@link UploadIoJob} instances, one for each
     * file, to be uploaded in parts.
     *
     * @return A list of {@link Result} objects for all multi-part uploads (both successful and failed).
     *
     * @see UploadIoJob#createNewJob(GPUdb, GPUdbFileHandler.Options, String, String, UploadOptions, FileUploadListener)
     */
    private List<Result> uploadMultiPartFiles() {

        List<Result> results = new ArrayList<>();

        // For each file in the multi part list create an IoJob instance
        for (int i = 0, multiPartListSize = this.multiPartList.size(); i < multiPartListSize; i++) {
            String fileName = this.multiPartList.get(i);
            String remoteFileName = this.multiPartRemoteFileNames.get( i );

            Result result = new Result();
            result.setFileName(fileName);
            result.setMultiPart(true);
            result.setOpMode(OpMode.UPLOAD);

            Pair<String, UploadIoJob> idJobPair;
            try {
                idJobPair = UploadIoJob.createNewJob(
                        this.db,
                        this.fileHandlerOptions,
                        fileName,
                        remoteFileName,
                        this.uploadOptions,
                        this.callback);
            } catch (GPUdbException e) {
                String errorMessage = String.format("Failed to create upload job for multi-part file <%s>: %s", fileName, e.getMessage());
                GPUdbLogger.error(errorMessage);
                result.setSuccessful(false);
                result.setException(e);
                result.setErrorMessage(errorMessage);
                results.add(result);
                continue;
            }

            try {
                // Try to upload, but catch exception to prevent breaking the loop
                idJobPair.getValue().start();
                result.setSuccessful(true);
            } catch (GPUdbException e) {
                String errorMessage = String.format("Failed to upload multi-part file <%s>: %s", fileName, e.getMessage());
                GPUdbLogger.error(errorMessage);
                result.setSuccessful(false);
                result.setException(e);
                result.setErrorMessage(errorMessage);
            } finally {
                // Wait for it to stop (clean up threads) before processing the next file
                idJobPair.getValue().stop();
            }

            results.add(result);
        }

        return results;
    }


    /**
     * This is the main upload method which is to be called by the users of
     * this class. Internally depending whether there are files to be uploaded
     * one shot or in parts it will call the methods
     * {@link #uploadFullFiles()} and {@link #uploadMultiPartFiles()}
     *
     * @throws GPUdbException If any file uploads fail. The exception message contains
     *         all collected error messages from failed uploads.
     */
    void upload() throws GPUdbException {

        List<Result> allResults = new ArrayList<>();

        List<Result> multiPartResults = uploadMultiPartFiles();
        allResults.addAll(multiPartResults);

        List<Result> fullFileResults = uploadFullFiles();
        allResults.addAll(fullFileResults);

        if( this.fullFileBatchManager.getNumberOfBatches() == 0  && this.multiPartList.size() == 0 )
            GPUdbLogger.warn( "No files found to upload ..." );

        // Collect all error messages from failed uploads
        List<String> errorMessages = new ArrayList<>();
        for (Result result : allResults) {
            if (!result.isSuccessful()) {
                errorMessages.add(result.getErrorMessage());
            }
        }

        // If there were any errors, throw an exception with all collected error messages
        if (!errorMessages.isEmpty()) {
            throw new GPUdbException(String.join("\n", errorMessages));
        }
    }


    UploadOptions getUploadOptions() {
        return this.uploadOptions;
    }

    void setUploadOptions(UploadOptions uploadOptions) {
        this.uploadOptions = uploadOptions;
    }

    int getRankForLocalDist() {
        return this.rankForLocalDist;
    }

    void setRankForLocalDist(int rankForLocalDist) {
        this.rankForLocalDist = rankForLocalDist;
    }
}
