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
 *  -   Upload a set of files from a given directory,
 *      which has a few subdirectories populated with files. This example
 *      will upload all the files starting from the root directory traversing
 *      the directory hierarchy. The upload will preserver the subdirectories
 *      on the KIFS after upload.
 *  -   Download all the files that have been uploaded in one go into the
 *      given directory.
 *  -   The example also demonstrates how to use a class implementing the
 *      interface {@link FileUploadListener} {@link FileUploadObserver} to
 *      receive notifications about completion/progress of downloads.
 *
 * Running the Example
 *
 *      This example takes seven VM arguments which can be passed using the
 *      -D flag. The arguments needed by this example are -
 *
 *      1. -Durl="<Kinetica connection URL>"
 *      2. -Duser="<Kinetica username>"
 *      3. -Dpass="<Kinetica password>"
 *      4. -Dupload="<existing/path/to/recursively/upload>"
 *      5. -Ddownload="<existing/path/to/download/uploaded/files/into>"
 *      6. -Dkifs="<Pre-existing KiFS directory to upload into>"
 *      7. -DlogLevel=[INFO | DEBUG | etc.]
 *
 *      For example:
 *      java ... -Durl="http://10.0.0.10:9191" -Duser=auser -Dpass=12345 -Dupload=./upload -Ddownload=./download -Dkifs=example -DlogLevel="INFO"
 *
 *      Once the example jar is built this example can be run from the command
 *      line thus:
 *
 *      java -cp ./target/gpudb-api-example-1.0-jar-with-dependencies.jar \
 *          -Durl="http://10.0.0.10:9191" -Duser=auser -Dpass=12345 -Dupload=./upload -Ddownload=./download -Dkifs=example \
 *          com.gpudb.example.GPUdbFileHandlerExample
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
                GPUdbLogger.info( "Uploaded multipart file part: <" + result.getUploadInfo() + ">" );
            }
        }

        /**
         * This method is fired every time a part of a multi-part upload has
         * been completed. This can be used to show status of an ongoing large
         * upload.
         *
         * @param result - a {@link Result} object
         */
        @Override
        public void onPartUpload( Result result ) {

            StringBuilder builder = new StringBuilder();
            builder.append("Uploaded multipart file part <")
                    .append( result.getUploadInfo().getUploadPartNumber())
                    .append("/")
                    .append( result.getUploadInfo().getTotalParts() )
                    .append("> of file <")
                    .append( result.getFileName() )
                    .append(">");

            GPUdbLogger.info( builder.toString() );
        }

        /**
         * This method is fired when a list of files have been uploaded one
         * shot successfully. This just returns a list of file names for
         * display purposes
         *
         * @param result - A {@link Result} object
         */
        @Override
        public void onFullFileUpload(Result result) {
        	if (result == null)
        		System.err.println("No result after upload");
        	else {
	            List<String> fileNames = result.getFullFileNames();
	            if (fileNames == null)
	            	System.err.println("No filenames in result after upload");
	            else
		            for ( String fileName : fileNames )
		                GPUdbLogger.info( "Uploaded file: <" + fileName + ">" );
        	}
        }

    }

    private static void usage() {
    	System.err.println("Usage:  java -cp <example.jar> [<options>] com.gpudb.example.GPUdbFileHandlerExample");
    	System.err.println("Where:");
    	System.err.println("        <example.jar> - the path to the compiled examples JAR");
    	System.err.println("        <options> - a list of options to the program, prefixed with -D");
    	System.err.println("                    <url> -      Kinetica connection URL");
    	System.err.println("                    <user> -     Kinetica connection username");
    	System.err.println("                    <pass> -     Kinetica connection password");
    	System.err.println("                    <upload> -   Path to directory to recusively upload");
    	System.err.println("                    <download> - Path where uploaded files will be downloaded (must exist)");
    	System.err.println("                    <kifs> -     KiFS directory to upload into (must exist)");
    	System.err.println("                    <logLevel> - Level of logging for the example [INFO|DEBUG|...]");
    	System.err.println("For Example:");
    	System.err.println("        java -cp ./target/gpudb-api-example-1.0-jar-with-dependencies.jar \\");
    	System.err.println("             -Durl=\"http://10.0.0.10:9191\" -Duser=auser -Dpass=12345 -Dupload=./upload -Ddownload=./download \\");
    	System.err.println("             com.gpudb.example.GPUdbFileHandlerExample");
    }

    /**
     * Example method to upload files under the given directory to KIFS.
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
     * @param uploadDir - the path of the directory to upload to KiFS
     * @param downloadDir - the path of the existing directory to download
     *                      uploaded files into
     * @param kifsDir - the name of the existing KiFS directory to upload into
     *
     * @throws GPUdbException
     */
    public static void exampleUploadAndDownload(GPUdb db, String uploadDir, String downloadDir, String kifsDir) throws GPUdbException {

        FileUploadObserver observer = new FileUploadObserver();

        GPUdbFileHandler fh = new GPUdbFileHandler( db );

        List<String> localFileNames = Collections.singletonList( uploadDir + "/**" );

        fh.upload( localFileNames, kifsDir, null, observer );

        //Invoke 'showFiles' after upload and list the files uploaded
        ShowFilesResponse resp = db.showFiles( Collections.singletonList( kifsDir ), new HashMap<String, String>());

        for ( String fileName: resp.getFileNames() )
            GPUdbLogger.info( "Found uploaded file for download: <" + fileName + ">" );

        //Download the same files
        fh.download( resp.getFileNames(), downloadDir, null, null );
    }
    
    /**
     * main method
     * @param args - Options for running the example.  See
     *               {@link GPUdbFileHandler} for details
     * @throws GPUdbException
     */
    public static void main( String ... args ) throws GPUdbException {

        String url = System.getProperty("url", "http://127.0.0.1:9191");
        String user = System.getProperty("user", "");
        String pass = System.getProperty("pass", "");
        String uploadDir = System.getProperty("upload", "");
        String downloadDir = System.getProperty("download", "");
        String kifsDir = System.getProperty("kifs", "kifs_example-uploads");
        String logLevel = System.getProperty("logLevel", "INFO");

        if ( uploadDir.isEmpty() ) {
            System.err.println("Please specify a directory to upload files from");
            usage();
            System.exit(1);
        }

        if ( downloadDir.isEmpty() ) {
            System.err.println("Please specify a directory to download files to");
            usage();
            System.exit(1);
        }

        if ( !logLevel.isEmpty() )
            GPUdbLogger.setLoggingLevel(logLevel);

        // Establish a connection with a locally running instance of GPUdb
        GPUdb.Options options = new GPUdb.Options();
        options.setUsername(user);
        options.setPassword(pass);
        GPUdb gpudb = new GPUdb( url, options );

        exampleUploadAndDownload( gpudb, uploadDir, downloadDir, kifsDir );
        System.exit(0);
    }
}
