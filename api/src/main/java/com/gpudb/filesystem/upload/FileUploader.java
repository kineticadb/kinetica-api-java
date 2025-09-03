package com.gpudb.filesystem.upload;

import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.GPUdbLogger;
import com.gpudb.filesystem.GPUdbFileHandler;
import com.gpudb.filesystem.common.FileOperation;
import com.gpudb.filesystem.common.OpMode;
import com.gpudb.filesystem.common.Result;

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
public class FileUploader extends FileOperation {

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
    public FileUploader(final GPUdb db,
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
     * @throws GPUdbException  If an error occurs transferring any batches of
     *        non-multi-part designated files to the server.
     */
    private void uploadFullFiles() throws GPUdbException {

        FullFileDispatcher fullFileDispatcher = new FullFileDispatcher( this.fileHandlerOptions, this.callback);

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
        	return;

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
                    throw new GPUdbException("Error reading source files", e);
                }
            }

            if( payloads.size() > 0 ) {
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
            // Check if the value of count is has reached the size of the
            // thread pool and if it has reached get the Results of the
            // operations and reset the payloads list and count.
            if( count % this.fileHandlerOptions.getFullFileDispatcherThreadpoolSize() == 0 ) {
                // Wait for the tasks to complete
                fullFileDispatcher.collect();
                count = 0;
                payloads.clear();
            }

        } // End of processing batches

        // Wait for jobs to complete if the only batch is less than size of the
        // thread pool or if the size of the last batch is not a multiple of
        // the size of the thread pool.
        fullFileDispatcher.collect();

        fullFileDispatcher.terminate();

    } //End uploadFullFiles method


    /**
     * This method uploads files which are candidates for multi-part uploads as
     * determined from their size.
     * 
     * This method first checks whether the KIFS directory exists or not and
     * creates it if it doesn't.
     * 
     * Then it creates a list of {@link UploadIoJob} instances, one for each
     * file, to be uploaded in parts.
     * 
     * @throws GPUdbException  If an error occurs transferring any multi-part
     *        designated files to the server.
     * 
     * @see UploadIoJob#createNewJob(GPUdb, GPUdbFileHandler.Options, OpMode, String, String, String, UploadOptions, FileUploadListener)
     * 
     * @throws GPUdbException  If an error occurs transferring any of the files
     *        to the server.
     */
    private void uploadMultiPartFiles() throws GPUdbException {

        // For each file in the multi part list create an IoJob instance
        for (int i = 0, multiPartListSize = this.multiPartList.size(); i < multiPartListSize; i++) {
            String fileName = this.multiPartList.get(i);

            String remoteFileName = this.multiPartRemoteFileNames.get( i );

            Pair<String, UploadIoJob> idJobPair = UploadIoJob.createNewJob(
                    this.db,
                    this.fileHandlerOptions,
                    fileName,
                    remoteFileName,
                    this.uploadOptions,
                    this.callback);

            // start the job immediately
            idJobPair.getValue().start();

            // Wait for it to stop before processing the next file
            idJobPair.getValue().stop();
        }
    }


    /**
     * This is the main upload method which is to be called by the users of
     * this class. Internally depending whether there are files to be uploaded
     * one shot or in parts it will call the methods
     * {@link #uploadFullFiles()} and {@link #uploadMultiPartFiles()}
     * 
     * @throws GPUdbException  If an error occurs transferring any of the files
     *        to the server.
     */
    public void upload() throws GPUdbException {

        uploadMultiPartFiles();

        uploadFullFiles();

        if( this.fullFileBatchManager.getNumberOfBatches() == 0  && this.multiPartList.size() == 0 )
            GPUdbLogger.warn( "No files found to upload ..." );
    }


    public UploadOptions getUploadOptions() {
        return this.uploadOptions;
    }

    public void setUploadOptions(UploadOptions uploadOptions) {
        this.uploadOptions = uploadOptions;
    }

    public int getRankForLocalDist() {
        return this.rankForLocalDist;
    }

    public void setRankForLocalDist(int rankForLocalDist) {
        this.rankForLocalDist = rankForLocalDist;
    }
}
