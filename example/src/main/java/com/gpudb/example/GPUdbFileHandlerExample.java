package com.gpudb.example;

import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.GPUdbLogger;
import com.gpudb.filesystem.GPUdbFileHandler;
import com.gpudb.filesystem.common.Result;
import com.gpudb.filesystem.upload.FileUploadListener;
import com.gpudb.filesystem.upload.UploadOptions;
import com.gpudb.protocol.ShowFilesResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * This examples demonstrates how to use the GPUdbFileHandler class and its
 * methods for uploading and downloading files.
 *
 * This example demonstrates the following:
 *  -   Upload a set of files from a directory named '_kifs_Example_date_files'
 *      which has a few sub-directories populated with files. This example
 *      will upload all the files starting from the root directory traversing
 *      the directory hierarchy. The upload will preserver the sub-directories
 *      on the KIFS after upload.
 *  -   Download all the files that have been uploaded in one go into the
 *      directory named '_kifs_example_downloaded_files'.
 *  -   The example also demonstrates how to use a class implementing the
 *      interface {@link FileUploadListener} {@link FileUploadObserver} to
 *      receive notifications about completion/progress of downloads.
 *
 *  Running the example
 *      This example takes two VM arguments which can be passed using the
 *      -D flag. The arguments needed by this example are -
 *      1. -Durl="some_kinetica_url"
 *      2. -DlogLevel=[INFO | DEBUG | etc.]
 *      e.g., -Durl="http://10.0.0.10:9191" -DlogLevel="INFO"
 *
 *      Once the example jar is built this example can be run from the command
 *      line thus :
 *      java -Durl="http://10.0.0.10:9191" -DlogLevel="INFO" -cp ./target/gpudb-api-example-1.0-jar-with-dependencies.jar com.gpudb.example.GPUdbFileHandlerExample
 *
 *      The 'url' and the 'logLevel' arguments should be changed as the command
 *      given above is just an example.
 */
public class GPUdbFileHandlerExample {

    /**
     * This is an example implementation of the interface
     * {@link FileUploadListener} which can be used to get notifications
     * about the uploads.
     *
     * It is to be noted that using this listener is not mandatory. If
     * notifications about upload progress/completion are not needed then
     * this should be passed in as 'null' to the
     * {@link GPUdbFileHandler#upload(List, String, UploadOptions, FileUploadListener)}.
     *
     * @see FileUploadListener
     */
    private static class FileUploadObserver implements FileUploadListener {

        /**
         * This callback will be fired when all the parts of a multi-part
         * upload has been completed successfully.
         *
         * Print out the resultList as received by the callback method.
         * The method could be used as suitable for the user of the
         * {@link GPUdbFileHandler} class for further processing of the
         * information about the uploads and downloads.
         *
         * @param resultList - list of {@link Result} objects
         */
        @Override
        public void onMultiPartUploadComplete( List<Result> resultList ) {
            for ( Result result : resultList ) {
                System.out.println( result );
                GPUdbLogger.info( result.toString() );
            }
        }

        /**
         * This method is fired everytime a part of a multi-part upload has
         * been completed. This can be used to show status of an ongoing large
         * upload.
         *
         * @param result - a {@link Result} object
         */
        @Override
        public void onPartUpload( Result result ) {

            StringBuilder builder = new StringBuilder();
            builder.append( result.getUploadInfo().getUploadPartNumber())
                    .append("th part of ")
                    .append( result.getUploadInfo().getTotalParts() )
                    .append(" of ")
                    .append( result.getFileName() )
                    .append( " - uploaded successfully ..");

            GPUdbLogger.info( builder.toString() );
            System.out.println( builder.toString() );
        }

        /**
         * This method is fired when a list of files have been uploaded one
         * shot successfully. This just returns a list of file names for
         * display purposes
         *
         * @param fileNames
         */
        @Override
        public void onFullFileUpload( List<String> fileNames ) {
            for ( String fileName : fileNames ) {
                System.out.println( fileName );
                GPUdbLogger.info( fileName );
            }
        }
    }

    /**
     * main method
     * @param args - None
     * @throws GPUdbException
     */
    public static void main( String ... args ) throws GPUdbException {

        String url = System.getProperty("url", "http://127.0.0.1:9191");

        // Get the log level from the command line, if any
        String logLevel = System.getProperty("logLevel", "");
        if ( !logLevel.isEmpty() ) {
            System.out.println( "Log level given by the user: " + logLevel );
            GPUdbLogger.setLoggingLevel(logLevel);
        } else {
            System.out.println( "No log level given by the user." );
        }

        // Establish a connection with a locally running instance of GPUdb
        GPUdb.Options options = new GPUdb.Options();
        GPUdb gpudb = new GPUdb( url, options );

        exampleUploadAndDownload( gpudb );
        System.exit(0);
    }

    /**
     * Example method to upload a list of files to KIFS
     * This method uploads all files from the directory
     * '_kifs_example_data_files' which is pre-created with subdirectories
     * and files residing in them.
     *
     * The method {@link GPUdbFileHandler#upload(List, String, UploadOptions, FileUploadListener)}
     * is invoked with a 'null' value for the third argument which is of type
     * {@link UploadOptions} as a result of which the default options
     * returned by the method {@link UploadOptions#defaultOptions()} is used and
     * that sets the 'recursive' search mode to 'true'.
     * So, this example will upload all the files traversing the entire
     * directory hierarchy.
     *
     * @param db - the {@link GPUdb} object
     *
     * @throws GPUdbException
     */
    public static void exampleUploadAndDownload(GPUdb db ) throws GPUdbException {

        FileUploadObserver observer = new FileUploadObserver();

        String m_kifsRemoteRootDir = "kifs_example-uploads";

        GPUdbFileHandler gpUdbFileHandler = new GPUdbFileHandler( db );

        List<String> localFileNames = Collections.singletonList( "./_kifs_example_data_files/**" );

        gpUdbFileHandler.upload( localFileNames, m_kifsRemoteRootDir, null, observer );

        //Invoke 'showFiles' after upload and list the files uploaded
        ShowFilesResponse resp = db.showFiles( Collections.singletonList( m_kifsRemoteRootDir ),
                new HashMap<String, String>());

        for ( String fileName: resp.getFileNames() ) {
            System.out.println( fileName );
        }

        //Download the same files
        gpUdbFileHandler.download( resp.getFileNames(),
                        "_kifs_example_downloaded_files",
                    null,
                        null );

    }
}
