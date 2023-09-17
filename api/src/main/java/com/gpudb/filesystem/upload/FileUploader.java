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
 * filesystem API. The consequences of using this class directly in client code
 * is not guaranteed and maybe undesirable.
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
 * The method {@link #uploadFullFiles()} does the upload by calling the Jave
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

    private String encoding;

    /**
     * Constructor -
     * @param db - The {@link GPUdb} instance
     * @param fileNames - List<String> - Names of the files to be uploaded
     * @param remoteDirName - String - Name of remote directory under KIFS
     * @param options - The {@link UploadOptions} object which is used to
     *                configure the upload operation.
     */
    public FileUploader(final GPUdb db,
                        final List<String> fileNames,
                        final String remoteDirName,
                        final UploadOptions options,
                        final FileUploadListener callback,
                        final GPUdbFileHandler.Options fileHandlerOptions) throws GPUdbException {

        super( db, OpMode.UPLOAD, fileNames, remoteDirName, options.isRecursive(), fileHandlerOptions);

        this.dirName = remoteDirName;
        this.uploadOptions = options;
        this.callback = callback;

    }

    /**
     * This method does the upload for files which are small enough to be
     * uploaded in one go. Right now the size threshold for such files have
     * been kept at 60 MB. In case the callback object {@link FileUploadListener}
     * has been passed in the method {@link FileUploadListener#onFullFileUpload(Result)}
     * will be called.
     */
    private void uploadFullFiles() throws GPUdbException {

        FullFileDispatcher fullFileDispatcher = new FullFileDispatcher( fileHandlerOptions, callback);

        // While iterating over the list of maps keep track of the count
        // Once the count reaches the thread pool size
        // wait for all the submitted tasks to complete and
        // then proceed with the next bunch of tasks if there are any
        int count = 0;

        // Iterate over each map in the list 'listOfFullFileNameToRemoteFileNameMap'
        // Each map contains
        // A. The key is the local file name
        // B. The value is the fully constructed file name on the KIFS
        int batches = fullFileBatchManager.getNumberOfBatches();
        if( batches > 0 ) {

            // Process the batches in a loop
            for( int n=0; n < batches; n++ ) {

                // Each batch is a Map of localFileName to remoteFileName
                Map<String, String> batch = fullFileBatchManager.getNthBatch( n );

                List<String> fullFileList = new ArrayList<>( batch.keySet() );

                // Used to pass the payloads to the endpoint 'upload/files'
                List<ByteBuffer> payloads = new ArrayList<>( fullFileList.size() );

                // Used to pass the file names to the endpoint 'upload/files'
                List<String> remoteFileNames = new ArrayList<>( fullFileList.size() );

                //Read the files as ByteBuffers
                for ( String fileName: fullFileList ) {
                    try {
                        ByteBuffer payload = ByteBuffer.wrap( Files.readAllBytes( Paths.get( fileName ) ) );
                        payloads.add( payload );
                        remoteFileNames.add( batch.get( fileName ) );

                    } catch (IOException e) {
                        throw new GPUdbException( e.getMessage() );
                    }
                }

                if( payloads.size() > 0 ) {
                    //Handle upload options here first
                    Map<String, String> options = new HashMap<>();

                    if( uploadOptions.getTtl() > 0 ) {
                        options.put( "ttl", String.valueOf( uploadOptions.getTtl() ) );
                    }

                    // Create a FullFileUploadTask instance with all the details
                    // needed to invoke the 'upload/files' endpoint
                    FullFileDispatcher.FullFileUploadTask task =
                            new FullFileDispatcher.FullFileUploadTask( db, remoteFileNames,
                                    payloads,
                                    options);

                    // Submit the task instance to the threadpool
                    fullFileDispatcher.submit( task );


                } // End of processing files in each partition

                count++;
                // Check if the value of count is has reached the size of the
                // thread pool and if it has reached get the Results of the
                // operations and reset the payloads list and count.
                if( count % fileHandlerOptions.getFullFileDispatcherThreadpoolSize() == 0 ) {
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
        }

    } //End uploadFullFiles method

    /**
     * This method uploads files which are candidates for multi-part uploads as
     * determined from their size. This method first checks whether the KIFS
     * directory exists or not and creates it if it doesn't.
     * Then it creates a list of {@link UploadIoJob} instances one for each
     * file to be uploaded in parts.
     *
     * @see UploadIoJob#createNewJob(GPUdb, GPUdbFileHandler.Options, OpMode, String, String, String, UploadOptions, FileUploadListener)
     *
     */
    private void uploadMultiPartFiles() throws GPUdbException {

        // For each file in the multi part list create an IoJob instance
        for (int i = 0, multiPartListSize = multiPartList.size(); i < multiPartListSize; i++) {
            String fileName = multiPartList.get(i);

            String remoteFileName = multiPartRemoteFileNames.get( i );

            Pair<String, UploadIoJob> idJobPair = UploadIoJob.createNewJob(db,
                    fileHandlerOptions,
                    OpMode.UPLOAD,
                    dirName,
                    fileName,
                    remoteFileName,
                    uploadOptions,
                    callback);

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
     */
    public void upload() throws GPUdbException {
        if( multiPartList.size() > 0 ) {
            uploadMultiPartFiles();
        }

        if( fullFileBatchManager.getFullFileList() != null
                && fullFileBatchManager.getFullFileList().size() > 0 ) {
            uploadFullFiles();
        }

        if( ( fullFileBatchManager.getFullFileList() != null
                && fullFileBatchManager.getFullFileList().size() == 0 )
                && multiPartList.size() == 0 ) {
            GPUdbLogger.warn( "No files found to upload ..." );
        }
    }

    public UploadOptions getUploadOptions() {
        return uploadOptions;
    }

    public void setUploadOptions(UploadOptions uploadOptions) {
        this.uploadOptions = uploadOptions;
    }

    public int getRankForLocalDist() {
        return rankForLocalDist;
    }

    public void setRankForLocalDist(int rankForLocalDist) {
        this.rankForLocalDist = rankForLocalDist;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

}