package com.gpudb.filesystem;

import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.GPUdbLogger;
import com.gpudb.filesystem.common.FileOperation;
import com.gpudb.filesystem.common.KifsDirectoryInfo;
import com.gpudb.filesystem.common.KifsFileInfo;
import com.gpudb.filesystem.download.DownloadOptions;
import com.gpudb.filesystem.download.FileDownloadListener;
import com.gpudb.filesystem.download.FileDownloader;
import com.gpudb.filesystem.ingest.FileIngestor;
import com.gpudb.filesystem.ingest.IngestOptions;
import com.gpudb.filesystem.ingest.TableCreationOptions;
import com.gpudb.filesystem.upload.FileUploadListener;
import com.gpudb.filesystem.upload.FileUploader;
import com.gpudb.filesystem.upload.UploadOptions;
import com.gpudb.protocol.*;

import java.util.*;

/**
 * This is the main class which exposes the API to be used by the end users.
 * This class exposes methods to upload e.g.,
 * ({@link #upload(String, String, UploadOptions, FileUploadListener)},
 * {@link #upload(List, String, UploadOptions, FileUploadListener)})
 * and similarly for download.
 *
 * The entire filesystem API is exposed through this class and an instance of
 * this class is enough to consume all functionalities provided by the KIFS.
 *
 * Apart from this class the other classes which could be used by the users of
 * this API are:
 * {@link UploadOptions}
 * {@link DownloadOptions}
 * {@link FileUploadListener}
 * {@link FileDownloadListener}
 */
public class GPUdbFileHandler {

    private static final int DEFAULT_THREAD_POOL_TERMINATION_TIMEOUT = 90;

    private static final int DEFAULT_FULL_FILE_DISPATCHER_THREADPOOL_SIZE = 5;

    private static final long DEFAULT_FILE_SIZE_TO_SPLIT = 62914560; //60 MB

    private final GPUdb db;

    private final GPUdbFileHandler.Options options;

    /**
     * Constructs a {@link GPUdbFileHandler} object that allows the user to
     * upload and download files.
     * @param db - {@link GPUdb} instance
     * @param options - An instance of {@link GPUdbFileHandler.Options} class
     */
    public GPUdbFileHandler(GPUdb db, Options options) {
        this.db = db;

        if( options != null ) {
            this.options = options;
        } else {
            this.options = new Options();
        }
    }

    /**
     * Constructor using default {@link Options}
     * @param db - the {@link GPUdb} instance
     */
    public GPUdbFileHandler( GPUdb db ) {
        this.db = db;
        options = new Options();
    }

    public static int getDefaultThreadPoolTerminationTimeout() {
        return DEFAULT_THREAD_POOL_TERMINATION_TIMEOUT;
    }

    public static int getDefaultFullFileDispatcherThreadpoolSize() {
        return DEFAULT_FULL_FILE_DISPATCHER_THREADPOOL_SIZE;
    }

    /**
     * Returns the {@link GPUdbFileHandler.Options} instance
     */
    public Options getOptions() {
        return options;
    }

    /** This method facilitates the upload of a single file from a given
     * local directory to a given remote directory. The filename passed in
     * is preserved between the client and the server.
     *
     * @param fileName - Name of the file along with the full path
     *                 e.g., "/home/user1/dir1/dir2/a.txt"
     * @param remoteDirName - Name of the remote directory to which the
     *                      file is to be saved.
     * @param options - {@link UploadOptions} - Various options to be used for
     *                uploading a file. If the value passed is null then
     *                default values would be used. The default values for
     *                the options can also be set explicitly using the method
     *                {@link UploadOptions#defaultOptions()}.
     *
     * @throws GPUdbException - from the method {@link #upload(List, String, UploadOptions, FileUploadListener)}
     *
     * @see FileUploadListener
     * @see UploadOptions
     *
     */
    public void upload(final String fileName,
                       final String remoteDirName,
                       UploadOptions options,
                       FileUploadListener callback) throws GPUdbException {

        upload( Collections.singletonList( fileName ), remoteDirName, options, callback );

    }

    /** This method facilitates uploading of multiple files from the same
     * local directory to a given remote directory. The filenames are preserved
     * between the client and the server.
     *
     * @param fileNames - List<String> - Names of the files to be uploaded
     *                  including the full local path.
     * @param remoteDirName - Name of the remote directory to which the
     *                      file is to be saved. This indicates a KIFS directory
     * @param uploadOptions - {@link UploadOptions} - Various uploadOptions to be used for
     *                uploading a file. If the value passed is null then
     *                default values would be used. The default values for
     *                the uploadOptions can also be set explicitly using the method
     *                {@link UploadOptions#defaultOptions()}.
     * @throws GPUdbException - Where parameters value checks fail
     *
     * @see FileUploadListener
     * @see UploadOptions

     */
    public void upload(final List<String> fileNames,
                       final String remoteDirName,
                       UploadOptions uploadOptions,
                       FileUploadListener callback) throws GPUdbException {

        // This method will throw an exception if one or more files are not
        // found and will log an error message with the ones that are not found.
        final List<String> nonExistentFileNames = new ArrayList<>();

        if( fileNames != null && fileNames.size() > 0 ) {
            for (String fileName : fileNames) {
                if( fileName != null && !fileName.isEmpty() ) {
                    if ( !FileOperation.localFileExists( fileName ) ) {
                        nonExistentFileNames.add( fileName );
                    }
                } else {
                    throw new GPUdbException( "Name of local file to upload cannot be null or empty" );
                }
            }
        } else {
            throw new GPUdbException( "List of local files to upload cannot be null or empty" );
        }

        if( nonExistentFileNames.size() > 0 ) {
            throw new GPUdbException( String.format( "Input file names [%s] are not found ", nonExistentFileNames ) );
        }

        if( uploadOptions == null ) {
            uploadOptions = UploadOptions.defaultOptions();
        }

        FileUploader fileUploader = new FileUploader(
                                                    db,
                                                    fileNames,
                                                    remoteDirName,
                                                    uploadOptions,
                                                    callback, this.options
                                                    );

        fileUploader.upload();

    }

    /**
     * This method downloads files from a KIFS
     * directory to a local directory. The local directory must exist or else
     * this method will throw an exception. The 'fileNames' argument contains
     * a list of fully qualified file names on a KIFS directory.
     *
     * @param fileNames - A list of fully qualified file names
     *                  residing on the KIFS directory e.g., 'a/b/c/d.txt'
     *                  where 'a/b/c' is a full path on the KIFS.
     * @param localDirName - String - This is the name of a local directory where the
     *                     files will be saved. This directory must exist on the
     *                     local filesystem.
     * @param downloadOptions - {@link DownloadOptions} - Options to be used for
     *                        downloading files from KIFS to the local directory.
     *                        if the value passed is null then default values
     *                        would be used. The default values for the options
     *                        can also be set explicitly using the method
     *                        {@link DownloadOptions#defaultOptions()}
     */
    public void download(List<String> fileNames,
                         String localDirName,
                         DownloadOptions downloadOptions,
                         FileDownloadListener callback) throws GPUdbException {

        if( fileNames!= null && fileNames.size() > 0 ) {
            for ( String fileName: fileNames ) {
                if( fileName == null || fileName.isEmpty() ) {
                    throw new GPUdbException( "KIFS file name cannot be null or empty" );
                }
            }
        } else {
            throw new GPUdbException( "List of KIFS file names cannot be null or empty" );
        }

        // This method will throw an exception if the local directory name
        // passed using 'localDirName' is not found
        if( !FileOperation.localDirExists( localDirName ) ) {
            throw new GPUdbException( String.format( "Local directory %s does not exist", localDirName ) );
        }

        if( downloadOptions == null ) {
            downloadOptions = DownloadOptions.defaultOptions();
        }
        FileDownloader downloader = new FileDownloader(
                                                    db,
                                                    fileNames,
                                                    localDirName,
                                                    downloadOptions,
                                                    callback,
                                                    options
                                                    );

        downloader.download();

    }

    /**
     * This method downloads a single file from a KIFS
     * directory to a local directory. The local directory must exist or else
     * this method will throw an exception. The 'fileNames' argument contains
     * a list of fully qualified file names on a KIFS directory.
     *
     * @param fileName - String - A fully qualified file name
     *                   residing on the KIFS directory e.g., 'a/b/c/d.txt'
     *                   where 'a/b/c' is a full path on the KIFS.
     * @param localDirName - String - This is the name of a local directory where the
     *                      files will be saved. This directory must exist on the
     *                      local filesystem.
     * @param downloadOptions - {@link DownloadOptions} - Options to be used for
     *                         downloading files from KIFS to the local directory.
     *                        if the value passed is null then default values
     *                        would be used. The default values for the options
     *                        can also be set explicitly using the method
     *                        {@link DownloadOptions#defaultOptions()}
     *
     */
    public void download(String fileName,
                         String localDirName,
                         DownloadOptions downloadOptions,
                         FileDownloadListener callback) throws GPUdbException {

        download( Collections.singletonList( fileName ), localDirName, downloadOptions, callback );

    }

    /**
     * This method will download all files in a KIFS directory to a directory on
     * the local filesystem.
     *
     * @param remoteDirName - Name of the KIFS directory
     * @param localDirName - Name of the local directory
     * @param downloadOptions - A {@link DownloadOptions} object
     * @throws GPUdbException - throws a GPUdbException if
     *                          1. the remoteDirName is not existing
     *                          2. the localDirName is not existing
     *                          3. something goes wrong with the download
     *
     * @see DownloadOptions
     */
    public void downloadDir(String remoteDirName,
                            String localDirName,
                            DownloadOptions downloadOptions,
                            FileDownloadListener callback) throws GPUdbException {

        // This method will throw an exception if the local directory name
        // passed using 'localDirName' is not found
        if( !FileOperation.localDirExists( localDirName ) ) {
            throw new GPUdbException( String.format( "Local directory %s does not exist", localDirName ) );
        }

        // Get the list of all files in the remote directory
        // If there is an error here, an exception is handled and re-thrown
        ShowFilesResponse showFilesResponse = db.showFiles( Collections.singletonList(remoteDirName), new HashMap<String, String>() );

        // Download all the files found if there is no error
        // The call to the download method could throw exceptions
        // while checking for existence of local directory to save the
        // downloaded files.
        if( !showFilesResponse.getFileNames().isEmpty() ) {
            download(showFilesResponse.getFileNames(), localDirName, downloadOptions, callback);
        }
    }

    /**
     * This method will ingest several files in one go. It will use the
     * method {@link #upload(List, String, UploadOptions, FileUploadListener)} to
     * upload the files to the directory 'sys_temp' on the KIFS and use the
     * method {@link GPUdb#insertRecordsFromFiles(String, List, Map, Map, Map)}
     * to update the table data.
     *
     * @param fileNames - Names of the local files to be ingested
     * @param tableName - Name of the table to ingest into
     * @param ingestOptions - Options for ingestion
     * @param createTableOptions - Options for table creation
     *
     * @throws - GPUdbException
     * @see IngestOptions
     * @see TableCreationOptions
     */
    public void ingest( final List<String> fileNames,
                        final String tableName,
                        final IngestOptions ingestOptions,
                        final TableCreationOptions createTableOptions ) throws GPUdbException{
        // This method will throw an exception if one or more files are not
        // found and will log an error message with the ones that are not found.
        final List<String> nonExistentFileNames = new ArrayList<>();

        if( fileNames != null && fileNames.size() > 0 ) {
            for (String fileName : fileNames) {
                if( fileName != null && !fileName.isEmpty() ) {
                    if ( !FileOperation.localFileExists( fileName ) ) {
                        nonExistentFileNames.add( fileName );
                    }
                } else {
                    throw new GPUdbException( "Name of file to ingest cannot be null or empty" );
                }
            }
        } else {
            throw new GPUdbException( "List of files to ingest cannot be null or empty" );
        }

        if( nonExistentFileNames.size() > 0 ) {
            throw new GPUdbException( String.format( "Input file names [%s] to ingest are not found ", nonExistentFileNames ) );
        }

        FileIngestor ingestor = new FileIngestor(
                db,
                tableName,
                fileNames,
                ingestOptions,
                createTableOptions
        );

        ingestor.ingestFromFiles();

    }

    /**
     * This method takes a list of fully qualified KIFS file paths and deletes
     * the files in one go.
     *
     * @param fileNames - a list of files on the KIFS
     * @param noErrorIfNotExists - true indicates not to throw an error if the
     *                           files are not found on KIFS and false otherwise
     */
    public void deleteFiles(List<String> fileNames, boolean noErrorIfNotExists) throws GPUdbException {

        Map<String, String> options = new HashMap<>();

        options.put( DeleteFilesRequest.Options.NO_ERROR_IF_NOT_EXISTS,
                noErrorIfNotExists ? DeleteFilesRequest.Options.TRUE : DeleteFilesRequest.Options.FALSE );

        db.deleteFiles( fileNames, options );

    }

    /**
     * This method deletes all files in a directory.
     *
     * @param remoteDirName - Name of the KIFS directory
     */
    public void deleteFilesInDir(String remoteDirName, boolean noErrorIfNotExists) throws GPUdbException {

        // Retrieve the files in the directory and delete them
        ShowFilesResponse showFilesResponse = db.showFiles( Collections.singletonList( remoteDirName ), new HashMap<String, String>());

        Map<String, String> deleteFilesOptions = new HashMap<>();
        deleteFilesOptions.put( DeleteFilesRequest.Options.NO_ERROR_IF_NOT_EXISTS ,
                noErrorIfNotExists ? DeleteFilesRequest.Options.TRUE : DeleteFilesRequest.Options.FALSE );
        db.deleteFiles( showFilesResponse.getFileNames(), deleteFilesOptions );

    }

    /**
     * This method shows the files
     * @param remoteDirNames - List of KIFS directory names
     * @return - List of {@link KifsFileInfo} objects
     */
    public List<KifsFileInfo> showFiles(List<String> remoteDirNames) throws GPUdbException {
        List<KifsFileInfo> resp = new ArrayList<>();

        if( remoteDirNames == null || remoteDirNames.isEmpty() ) {
            throw new GPUdbException( "List of KIFS directory names cannot be null or empty list" );
        }

        if( kifsDirectoriesExist( new LinkedHashSet<>( remoteDirNames ) ) ) {
            ShowFilesResponse sfResp = db.showFiles( remoteDirNames, new HashMap<String, String>() );

            int count = sfResp.getFileNames().size();

            for( int i = 0; i < count; i++ ) {
                KifsFileInfo fileInfo = new KifsFileInfo();

                fileInfo.setFileName( sfResp.getFileNames().get( i ) );
                fileInfo.setFileSize( sfResp.getSizes().get( i ) );
                fileInfo.setCreationTime( sfResp.getCreationTimes().get( i ) );
                fileInfo.setCreatedBy( sfResp.getUsers().get( i ) );

                fileInfo.setInfo( sfResp.getInfo() );

                resp.add( fileInfo );
            }

            return resp;
        } else {
            GPUdbLogger.error( String.format( "One or more KIFS directories/files in [%s] does not exist", remoteDirNames ) );
            return null;
        }
    }

    /**
     * This method retrieves the directory information for a given list of KIFS
     * directories and returns the information as a list of {@link KifsDirectoryInfo}
     * objects.
     * @param remoteDirNames - Set of names of the KIFS directories
     * @param options - Map of String to String; the endpoint expects an empty
     *                map.
     * @return - List of {@link KifsDirectoryInfo} objects.
     */
    public List<KifsDirectoryInfo> showDirectories(Set<String> remoteDirNames,
                                                   Map<String, String> options) throws GPUdbException {

        if( remoteDirNames == null || remoteDirNames.isEmpty() ) {
            throw new GPUdbException( "Set of KIFS directory names cannot be null or empty" );
        }

        List<KifsDirectoryInfo> showDirResp = new ArrayList<>();

        for( String dirName: remoteDirNames ) {
            if( dirName == null || dirName.trim().isEmpty() ) {
                throw new GPUdbException( "Set of KIFS directory names cannot contain null or empty directory name" );
            } else {
                ShowDirectoriesResponse resp = db.showDirectories( dirName, options );

                KifsDirectoryInfo dirInfo = new KifsDirectoryInfo();

                dirInfo.setKifsPath( resp.getDirectories().get(0) );
                dirInfo.setCreatedBy( resp.getUsers().get(0) );
                dirInfo.setPermission( resp.getPermissions().get(0) );
                dirInfo.setCreationTime( resp.getCreationTimes().get(0) );

                showDirResp.add( dirInfo );
            }
        }
        return showDirResp;
    }

    /**
     * This method retrieves the directory information for a given KIFS
     * directory and returns the information as a list of {@link KifsDirectoryInfo}
     * objects.
     * @param remoteDirName - Name of the KIFS directory, cannot be null or empty
     * @param options - Map of String to String; the endpoint expects an empty
     *                map.
     * @return - List of {@link KifsDirectoryInfo} objects.
     */
    public List<KifsDirectoryInfo> showDirectory(String remoteDirName,
                                                 Map<String, String> options) throws GPUdbException {

        if( remoteDirName == null || remoteDirName.isEmpty() ) {
            throw new GPUdbException( "KIFS directory name [remoteDirName] cannot be null or empty" );
        }

        List<KifsDirectoryInfo> showDirResp = new ArrayList<>();

        ShowDirectoriesResponse resp = db.showDirectories( remoteDirName, options );

        int count = resp.getDirectories().size();

        for( int i=0; i < count; i++) {
            KifsDirectoryInfo dirInfo = new KifsDirectoryInfo();

            dirInfo.setKifsPath( resp.getDirectories().get(i) );
            dirInfo.setCreatedBy( resp.getUsers().get(i) );
            dirInfo.setPermission( resp.getPermissions().get(i) );
            dirInfo.setCreationTime( resp.getCreationTimes().get(i) );

            showDirResp.add( dirInfo );
        }

        return showDirResp;
    }


    /**
     * This method retrieves the directory information all KIFS
     * directories and returns the information as a list of {@link KifsDirectoryInfo}
     * objects.
     * @param options - Map of String to String; the endpoint expects an empty
     *                map.
     * @return - List of {@link KifsDirectoryInfo} objects.
     */
    public List<KifsDirectoryInfo> showAllDirectories( Map<String, String> options) throws GPUdbException {

        List<KifsDirectoryInfo> showDirResp = new ArrayList<>();

        ShowDirectoriesResponse resp = db.showDirectories( "", options );

        int count = resp.getDirectories().size();

        for( int i=0; i < count; i++) {
            KifsDirectoryInfo dirInfo = new KifsDirectoryInfo();

            dirInfo.setKifsPath( resp.getDirectories().get(i) );
            dirInfo.setCreatedBy( resp.getUsers().get(i) );
            dirInfo.setPermission( resp.getPermissions().get(i) );
            dirInfo.setCreationTime( resp.getCreationTimes().get(i) );

            showDirResp.add( dirInfo );
        }

        return showDirResp;
    }

    /**
     * This method will create a KIFS directory with options as explained
     * below.
     *
     * @param remoteDirName - String - Name of the KIFS directory to create
     * @param noErrorIfExists - boolean - true means if the directory exists
     *                        there will be no error thrown and false means
     *                        an existing directory name will throw an error
     */
    public void createDirectory(String remoteDirName, boolean noErrorIfExists) throws GPUdbException {

        Map<String, String> options = new HashMap<>();
        options.put( CreateDirectoryRequest.Options.NO_ERROR_IF_EXISTS ,
                noErrorIfExists ? CreateDirectoryRequest.Options.TRUE : CreateDirectoryRequest.Options.FALSE);

        db.createDirectory(remoteDirName, options);
    }

    /**
     * This method deletes a KIFS directory. It will do a recursive delete if
     * such an option is given.
     *
     * @param remoteDirName - Name of the KIFS directory
     * @param recursive - Indicates deletion of all sub-directories
     * @param noErrorIfNotExists - If set to true will ignore if the directory
     *                           does not exist
     */
        public void deleteDirectory(String remoteDirName,
                                   boolean recursive,
                                   boolean noErrorIfNotExists) throws GPUdbException {

        Map<String, String> options = new HashMap<>();
        options.put( DeleteDirectoryRequest.Options.RECURSIVE,
                recursive ? DeleteDirectoryRequest.Options.TRUE : DeleteDirectoryRequest.Options.FALSE);
        options.put( DeleteDirectoryRequest.Options.NO_ERROR_IF_NOT_EXISTS,
                noErrorIfNotExists ? DeleteDirectoryRequest.Options.TRUE : DeleteDirectoryRequest.Options.FALSE);

        db.deleteDirectory(remoteDirName, options);
    }

    /**
     * This class models the options available for modifying some behaviours
     * of the {@link GPUdbFileHandler} class
     *
     * The two options available right now are:
     * 1. threadPoolTerminationTimeout - in seconds
     * 2. fullFileDispatcherThreadpoolSize - an integer
     */
    public static final class Options {

        private long fileSizeToSplit;
        private int fullFileDispatcherThreadpoolSize;

        /**
         * Constructor with default values for the class members
         */
        public Options() {
            this.fullFileDispatcherThreadpoolSize = DEFAULT_FULL_FILE_DISPATCHER_THREADPOOL_SIZE;
            this.fileSizeToSplit = DEFAULT_FILE_SIZE_TO_SPLIT;
        }

        /**
         * A copy constructor
         * @param other - {@link Options} instance to copy from
         */
        public Options( Options other ) {
            this.fullFileDispatcherThreadpoolSize = other.fullFileDispatcherThreadpoolSize;
            this.fileSizeToSplit = other.fileSizeToSplit;
        }

        /**
         * This value is used to configure the size of the thread pool used
         * internally to handle batches of full file uploads. The default value
         * is set to 5

         * @return Return the value of the 'fullFileDispatcherThreadpoolSize'
         *
         * @see #setFullFileDispatcherThreadpoolSize(int)
         */
        public int getFullFileDispatcherThreadpoolSize() {
            return fullFileDispatcherThreadpoolSize;
        }

        /**
         * This value is used to configure the size of the thread pool used
         * internally to handle batches of full file uploads. The default value
         * is set to 5

         * @param fullFileDispatcherThreadpoolSize - an Integer value indicating
         *                                         the size of the thread pool
         * @return - the current Options instance
         *
         * @see #getFullFileDispatcherThreadpoolSize()
         */
        public Options setFullFileDispatcherThreadpoolSize(int fullFileDispatcherThreadpoolSize) throws GPUdbException {
            if( fullFileDispatcherThreadpoolSize <= 0
                    || fullFileDispatcherThreadpoolSize > Runtime.getRuntime().availableProcessors() ) {
                throw new GPUdbException(String.format("Thread pool size must be between 1 and %d", Runtime.getRuntime().availableProcessors()));
            }
            this.fullFileDispatcherThreadpoolSize = fullFileDispatcherThreadpoolSize;
            return this;
        }

        /**
         * Split files greater than this size so that they are uploaded multi-part.
         * This value is also used to split multiple small files into batches
         * where the total size of the files in each batch is determined by this
         * value.
         *
         * Gets the value of 'fileSizeToSplit'
         *
         * @return - the file size in bytes
         *
         * @see #setFileSizeToSplit(long)
         */
        public long getFileSizeToSplit() {
            return fileSizeToSplit;
        }

        /**
         * Split files greater than this size so that they are uploaded multi-part.
         * This value is also used to split multiple small files into batches
         * where the total size of the files in each batch is determined by this
         * value.
         *
         * Sets the value of 'fileSizeToSplit'
         *
         * @param fileSizeToSplit - the file size in bytes
         */
        public void setFileSizeToSplit( long fileSizeToSplit ) throws GPUdbException {
            if( fileSizeToSplit <=0 || fileSizeToSplit > DEFAULT_FILE_SIZE_TO_SPLIT ) {
                throw new GPUdbException( String.format( "FileSizeToSplit : Value must be a positive value less than or equal to %s", DEFAULT_FILE_SIZE_TO_SPLIT ));
            }
            this.fileSizeToSplit = fileSizeToSplit;
        }
    }

    /**
     * This method checks whether KIFS paths exist or not.
     *
     * @param dirNames - the set of full KIFS paths
     * @return - true if all the directories exist and false if one or more of them
     * does not exist
     */
    public boolean kifsDirectoriesExist( Set<String> dirNames ) {

        if( dirNames == null || dirNames.isEmpty() ) {
            GPUdbLogger.error( "List of Directory names cannot be null or empty" );
            return false;
        }

        // Get all the KIFS directories and put them into a Set
        ShowDirectoriesResponse sdResp;
        try {
            sdResp = db.showDirectories( "", new HashMap<String, String>());
            Set<String> remoteDirNames = new LinkedHashSet<>( sdResp.getDirectories() );

            // Find the intersection of the two sets
            remoteDirNames.retainAll( dirNames );

            // Return true if the intersection equals the input set
            return remoteDirNames.equals( dirNames );
        } catch (GPUdbException e) {
            GPUdbLogger.error( e.getMessage() );
            return false;
        }
    }

    /**
     * This method checks whether a KIFS path exists or not.
     *
     * @param dirName - the full KIFS paths
     * @return - true if if the directory exists and false otherwise
     *          Also returns false if the GPUdb instance is null.
     */
    public boolean kifsDirectoryExists( String dirName ) {

        // 'show/directories' will only return the KIFS roots so an embedded
        // '/' will always result in a failed match.
        if( dirName == null || dirName.isEmpty() || dirName.contains( "/" )) {
            GPUdbLogger.error( "KIFS directory name cannot be null or empty and cannot contain an embedded '/' " );
            return false;
        }

        // Get all the KIFS directories and put them into a Set
        ShowDirectoriesResponse sdResp = null;
        try {
            sdResp = db.showDirectories( dirName, new HashMap<String, String>());
            // Return true if the list of directories returned contain the input
            // directory name
            return sdResp.getDirectories().contains( dirName );
        } catch (GPUdbException e) {
            GPUdbLogger.error( e.getMessage() );
            return false;
        }
    }

    /**
     * This method checks whether a KIFS file exists or not.
     *
     * @param fileName - the full KIFS path to a file
     * @return - true if if the file exists and false otherwise
     */
    public boolean kifsFileExists( String fileName ) {

        if( fileName == null || fileName.isEmpty() ) {
            GPUdbLogger.error( "KIFS file name cannot be null" );
            return false;
        }

        ShowFilesResponse sfResp = null;
        try {
            sfResp = db.showFiles( Collections.singletonList( fileName ), new HashMap<String, String>());
            // Return true if the list of directories returned contain the input
            // directory name
            return sfResp.getFileNames().contains( fileName );
        } catch (GPUdbException e) {
            GPUdbLogger.error( e.getMessage() );
            return false;
        }

    }
}