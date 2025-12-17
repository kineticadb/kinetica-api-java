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
import com.gpudb.filesystem.ingest.IngestResult;
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
 * The entire {@code filesystem} API is exposed through this class and an
 * instance of this class is enough to consume all functionalities provided by
 * the KIFS.
 *
 * Apart from this class the other classes which could be used by the users of
 * this API are:
 * {@link UploadOptions}
 * {@link DownloadOptions}
 * {@link FileUploadListener}
 * {@link FileDownloadListener}
 */
public class GPUdbFileHandler {

    /**
     * Separator character between a KiFS directory and KiFS file name.  This
     * can also be used within a file name to create a virtual directory within
     * a path.
     */
	public static final String KIFS_PATH_SEPARATOR = "/";

	/**
	 * Prefix to use when referencing KiFS files; e.g., for purposes for file
	 * ingest.
	 */
    public static final String KIFS_PATH_PREFIX = "kifs://";

    /**
     * Alias for a user's home directory.
     */
    public static final String REMOTE_USER_HOME_DIR_PREFIX = "~";

    private static final int DEFAULT_THREAD_POOL_TERMINATION_TIMEOUT = 90;

    private static final int DEFAULT_FULL_FILE_DISPATCHER_THREADPOOL_SIZE = 5;

    private static final long DEFAULT_FILE_SIZE_TO_SPLIT = 62914560; //60 MB

    private final GPUdb db;

    private final GPUdbFileHandler.Options options;


    /**
     * Constructs a {@link GPUdbFileHandler} object that allows the user to
     * upload and download files.
     * 
     * @param db  The {@link GPUdb} instance used to access KiFS.
     * @param options  Options for setting up the files for transfer.
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
     * Constructs a {@link GPUdbFileHandler} object that allows the user to
     * upload and download files using default {@link Options}.
     * 
     * @param db  The {@link GPUdb} instance used to access KiFS.
     */
    public GPUdbFileHandler( GPUdb db ) {
        this.db = db;
        this.options = new Options();
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
        return this.options;
    }


    /**
     * This method facilitates the upload of a single file from a given
     * local directory to a given remote directory. The filename passed in
     * is preserved between the client and the server. This method uses the
     * default Upload options and does not use a callback.
     *
     * @param fileName  Name of the file along with the full path
     *        e.g., "/home/user1/dir1/dir2/a.txt".
     * @param remoteDirName  Name of KiFS directory to upload to.
     * @throws GPUdbException  If an error occurs uploading any of the files to
     *        the server.
     *
     * @see FileUploadListener
     * @see UploadOptions
     * @see UploadOptions#defaultOptions()
     * @see #upload(String, String, UploadOptions, FileUploadListener)
     */
    public void upload(final String fileName, final String remoteDirName) throws GPUdbException {
        upload(fileName, remoteDirName, UploadOptions.defaultOptions(), null );
    }


    /**
     * This method facilitates the upload of a single file from a given
     * local directory to a given remote directory. The filename passed in
     * is preserved between the client and the server.
     *
     * @param fileName  Name of the file along with the full path
     *        e.g., "/home/user1/dir1/dir2/a.txt".
     * @param remoteDirName  Name of KiFS directory to upload to.
     * @param uploadOptions  Various options to be used for
     *                uploading a file. If the value passed is null then
     *                default values would be used. The default values for
     *                the options can also be set explicitly using the method
     *                {@link UploadOptions#defaultOptions()}.
     * @param callback  An instance of {@link FileUploadListener} class which
     *                 is used to report status of an ongoing/completed upload
     *                 to the caller of the method.
     * @throws GPUdbException  If an error occurs uploading any of the files to
     *        the server.
     *
     * @see FileUploadListener
     * @see UploadOptions
     *
     */
    public void upload(final String fileName,
                       final String remoteDirName,
                       UploadOptions uploadOptions,
                       FileUploadListener callback) throws GPUdbException {

        upload( Collections.singletonList( fileName ), remoteDirName, uploadOptions, callback );
    }


    /**
     * This method facilitates uploading of multiple files from the same
     * local directory to a given remote directory. The filenames are preserved
     * between the client and the server. This method uses default Upload options
     * and does not use a callback.
     *
     * @param fileNames  Names of the files along with the full path
     *        e.g., "/home/user1/dir1/dir2/a.txt".
     * @param remoteDirName  Name of KiFS directory to upload to.
     * @throws GPUdbException  If an error occurs uploading any of the files to
     *        the server.
     *
     * @see FileUploadListener
     * @see UploadOptions
     * @see UploadOptions#defaultOptions()
     * @see #upload(List, String, UploadOptions, FileUploadListener)
     */
    public void upload(final List<String> fileNames, final String remoteDirName) throws GPUdbException {
        upload(fileNames, remoteDirName, UploadOptions.defaultOptions(), null );
    }


    /** This method facilitates uploading of multiple files from the same
     * local directory to a given remote directory. The filenames are preserved
     * between the client and the server.
     *
     * @param fileNames  Names of the files along with the full path
     *        e.g., "/home/user1/dir1/dir2/a.txt".
     * @param remoteDirName  Name of KiFS directory to upload to.
     * @param uploadOptions  Various uploadOptions to be used for
     *        uploading a file. If the value passed is null then
     *        default values would be used. The default values for
     *        the uploadOptions can also be set explicitly using the method
     *        {@link UploadOptions#defaultOptions()}.
     * @param callback  An instance of {@link FileUploadListener} class which
     *        is used to report status of an ongoing/completed upload
     *        to the caller of the method.
     * @throws GPUdbException  If an error occurs uploading any of the files to
     *        the server.
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

        FileUploader fileUploader = new FileUploader(
                this.db,
                fileNames,
                remoteDirName,
                (uploadOptions == null) ? UploadOptions.defaultOptions() : uploadOptions,
                callback,
                this.options
        );

        fileUploader.upload();
    }


    /**
     * This method downloads files from a KIFS
     * directory to a local directory. The local directory must exist or else
     * this method will throw an exception. The 'fileNames' argument contains
     * a list of fully qualified file names on a KIFS directory. This method
     * uses default download options and doesn't use a callback.
     *
     * @param fileNames  Names of the KiFS files to download
     *        e.g., 'a/b/c/d.txt'.
     * @param localDirName  Name of local directory to download to.  This
     *        directory must exist on the local file system.
     *
     * @see DownloadOptions
     * @see DownloadOptions#defaultOptions()
     * @see FileDownloadListener
     */
    public void download(List<String> fileNames, String localDirName) throws GPUdbException {
        download(fileNames, localDirName, DownloadOptions.defaultOptions(), null );
    }


    /**
     * This method downloads files from a KIFS
     * directory to a local directory. The local directory must exist or else
     * this method will throw an exception. The 'fileNames' argument contains
     * a list of fully qualified file names on a KIFS directory.
     *
     * @param fileNames  Names of the KiFS files to download
     *        e.g., 'a/b/c/d.txt'.
     * @param localDirName  Name of local directory to download to.  This
     *        directory must exist on the local file system.
     * @param downloadOptions  Options to be used for
     *        downloading files from KIFS to the local directory.
     *        if the value passed is null then default values
     *        would be used. The default values for the options
     *        can also be set explicitly using the method
     *        {@link DownloadOptions#defaultOptions()}.
     * @param callback  An instance of {@link FileDownloadListener} class which
     *        is used to report status of an ongoing/completed download
     *        to the caller of the method.
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

        FileDownloader downloader = new FileDownloader(
                this.db,
                fileNames,
                localDirName,
                (downloadOptions == null) ? DownloadOptions.defaultOptions() : downloadOptions,
                callback,
                this.options
        );

        downloader.download();
    }


    /**
     * This method downloads a single file from a KIFS
     * directory to a local directory. The local directory must exist or else
     * this method will throw an exception. The 'fileNames' argument contains
     * a list of fully qualified file names on a KIFS directory.
     * 
     * @param fileName  Name of the KiFS file to download
     *        e.g., 'a/b/c/d.txt'.
     * @param localDirName  Name of local directory to download to.  This
     *        directory must exist on the local file system.
     *
     */
    public void download(String fileName, String localDirName) throws GPUdbException {
        download(fileName, localDirName, DownloadOptions.defaultOptions(), null );
    }


    /**
     * This method downloads a single file from a KIFS
     * directory to a local directory. The local directory must exist or else
     * this method will throw an exception. The 'fileNames' argument contains
     * a list of fully qualified file names on a KIFS directory.
     *
     * @param fileName  Name of the KiFS file to download
     *        e.g., 'a/b/c/d.txt'.
     * @param localDirName  Name of local directory to download to.  This
     *        directory must exist on the local file system.
     * @param downloadOptions  Options to be used for
     *        downloading files from KIFS to the local directory.
     *        if the value passed is null then default values
     *        would be used. The default values for the options
     *        can also be set explicitly using the method
     *        {@link DownloadOptions#defaultOptions()}.
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
     * the local file system.
     *
     * @param remoteDirName - Name of the KIFS directory to download.
     * @param localDirName - Name of the local directory.
     * @param downloadOptions  Options to be used for
     *        downloading files from KIFS to the local directory.
     *        if the value passed is null then default values
     *        would be used. The default values for the options
     *        can also be set explicitly using the method
     *        {@link DownloadOptions#defaultOptions()}.
     * @throws GPUdbException  If:
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
        ShowFilesResponse showFilesResponse = this.db.showFiles( Collections.singletonList(remoteDirName), new HashMap<>() );

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
     * @param fileNames  Names of the local files to be ingested.
     * @param tableName  Name of the table to ingest into.
     * @param ingestOptions  Options for ingestion.
     * @param createTableOptions  Options for table creation.
     *
     * @throws  If the ingestion or the underlying upload fails.
     *
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

        if ( fileNames != null && fileNames.size() > 0 ) {
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

        if ( nonExistentFileNames.size() > 0 ) {
            throw new GPUdbException( String.format( "Input file names [%s] to ingest are not found ", nonExistentFileNames ) );
        }

        FileIngestor ingestor = new FileIngestor(
                this.db,
                tableName,
                fileNames,
                ingestOptions,
                createTableOptions
        );

        IngestResult ingestResult = ingestor.ingestFromFiles();

        if ( ingestResult.getException() != null ) {
            throw new GPUdbException( ingestResult.getErrorMessage() );
        }
    }


    /**
     * This method takes a list of fully qualified KIFS file paths and deletes
     * the files in one go.
     * 
     * @param fileNames  List of files in KIFS to delete.
     */
    public void deleteFiles(List<String> fileNames) throws GPUdbException {
        deleteFiles(fileNames, true);
    }


    /**
     * This method takes a list of fully qualified KIFS file paths and deletes
     * the files in one go.
     *
     * @param fileNames  List of files in KIFS to delete.
     * @param noErrorIfNotExists  True indicates not to throw an error if the
     *        files are not found on KIFS and false otherwise.
     */
    public void deleteFiles(List<String> fileNames, boolean noErrorIfNotExists) throws GPUdbException {

        Map<String, String> deleteOptions = new HashMap<>();

        deleteOptions.put( DeleteFilesRequest.Options.NO_ERROR_IF_NOT_EXISTS,
                noErrorIfNotExists ? DeleteFilesRequest.Options.TRUE : DeleteFilesRequest.Options.FALSE );

        this.db.deleteFiles( fileNames, deleteOptions );
    }


    /**
     * This method deletes files in the KIFS directory whose name is passed in
     * using {@code remoteDirName}. This method uses default options.
     *
     * @param remoteDirName  Name of the KIFS directory containing files to
     *        delete.
     * @throws GPUdbException  If an error occurs deleting the files.
     */
    public void deleteFilesInDir(String remoteDirName) throws GPUdbException {
        deleteFilesInDir(remoteDirName, true);
    }


    /**
     * This method deletes files in the KIFS directory whose name is passes in
     * using {@code remoteDirName}.
     * 
     * @param remoteDirName  Name of the KIFS directory containing files to
     *        delete.
     * @param noErrorIfNotExists  Indicates whether to allow deletion of files
     *        in non-existent KIFS directory or not.
     * @throws GPUdbException  If an error occurs deleting the files.
     */
    public void deleteFilesInDir(String remoteDirName, boolean noErrorIfNotExists) throws GPUdbException {

        // Retrieve the files in the directory and delete them
        ShowFilesResponse showFilesResponse = this.db.showFiles( Collections.singletonList( remoteDirName ), new HashMap<>());

        Map<String, String> deleteFilesOptions = new HashMap<>();
        deleteFilesOptions.put( DeleteFilesRequest.Options.NO_ERROR_IF_NOT_EXISTS ,
                noErrorIfNotExists ? DeleteFilesRequest.Options.TRUE : DeleteFilesRequest.Options.FALSE );
        this.db.deleteFiles( showFilesResponse.getFileNames(), deleteFilesOptions );
    }


    /**
     * This method returns statistics about the given files and/or directories.
     * Wildcards "?" and "*" can be used to represent one character and zero or
     * more characters, respectively.
     * 
     * @param remotePaths  List of KIFS files and/or directory names to report
     *        statistics on.
     * @return  List of {@link KifsFileInfo} objects, containing statistics
     *        about the given files.
     * @throws GPUdbException  If an error occurs looking up the files.
     */
    public List<KifsFileInfo> showFiles(List<String> remotePaths) throws GPUdbException {
        List<KifsFileInfo> resp = new ArrayList<>();

        if( remotePaths == null || remotePaths.isEmpty() ) {
            throw new GPUdbException( "List of KIFS directory names cannot be null or empty list" );
        }

        ShowFilesResponse sfResp = this.db.showFiles( remotePaths, new HashMap<>() );

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
    }


    /**
     * This method retrieves the directory information for a given list of KIFS
     * directories and returns the information as a list of {@link KifsDirectoryInfo}
     * objects.
     *
     * @deprecated
     * @param remoteDirNames  Set of KIFS directory names to report statistics
     *        on.
     * @return  List of {@link KifsDirectoryInfo} objects, containing statistics
     *        about the given directories.
     * @throws GPUdbException  If an error occurs looking up the directories.
     * @see #showDirectories(Set, Map)
     */
    @Deprecated(since = "7.2.3", forRemoval = true)
    public List<KifsDirectoryInfo> showDirectories(Set<String> remoteDirNames) throws GPUdbException {
        return showDirectories(new ArrayList<>(remoteDirNames), new HashMap<>());
    }


    /**
     * This method retrieves the directory information for a given list of KIFS
     * directories and returns the information as a list of {@link KifsDirectoryInfo}
     * objects.
     * 
     * @deprecated
     * @param remoteDirNames  Set of KIFS directory names to report statistics
     *        on.
     * @param showDirectoryOptions  Unused.
     * @return  List of {@link KifsDirectoryInfo} objects, containing statistics
     *        about the given directories.
     */
    @Deprecated(since = "7.2.3", forRemoval = true)
    public List<KifsDirectoryInfo> showDirectories(Set<String> remoteDirNames,
                                                   Map<String, String> showDirectoryOptions) throws GPUdbException {
        return showDirectories(new ArrayList<>(remoteDirNames), showDirectoryOptions);
    }


    /**
     * This method retrieves the directory information for a given list of KIFS
     * directories and returns the information as a list of {@link KifsDirectoryInfo}
     * objects.
     *
     * @param remoteDirNames  List of KIFS directory names to report statistics
     *        on.
     * @return  List of {@link KifsDirectoryInfo} objects, containing statistics
     *        about the given directories.
     * @throws GPUdbException  If an error occurs looking up the directories.
     * @see #showDirectories(Set, Map)
     */
    public List<KifsDirectoryInfo> showDirectories(List<String> remoteDirNames) throws GPUdbException {
        return showDirectories(remoteDirNames, new HashMap<>());
    }

    /**
     * This method retrieves the directory information for a given list of KIFS
     * directories and returns the information as a list of {@link KifsDirectoryInfo}
     * objects.
     * 
     * @param remoteDirNames  List of KIFS directory names to report statistics
     *        on.
     * @param showDirectoryOptions  Reserved for future use.
     * @return  List of {@link KifsDirectoryInfo} objects, containing statistics
     *        about the given directories.
     */
    public List<KifsDirectoryInfo> showDirectories(List<String> remoteDirNames,
                                                   Map<String, String> showDirectoryOptions) throws GPUdbException {

        if( remoteDirNames == null || remoteDirNames.isEmpty() ) {
            throw new GPUdbException( "Set of KIFS directory names cannot be null or empty" );
        }

        List<KifsDirectoryInfo> showDirResp = new ArrayList<>();

        for( String dirName: remoteDirNames ) {
            if( dirName == null || dirName.trim().isEmpty() ) {
                throw new GPUdbException( "Set of KIFS directory names cannot contain null or empty directory name" );
            }

            ShowDirectoriesResponse resp = this.db.showDirectories( dirName, showDirectoryOptions );

            KifsDirectoryInfo dirInfo = new KifsDirectoryInfo();

            dirInfo.setKifsPath( resp.getDirectories().get(0) );
            dirInfo.setCreatedBy( resp.getUsers().get(0) );
            dirInfo.setPermission( resp.getPermissions().get(0) );
            dirInfo.setCreationTime( resp.getCreationTimes().get(0) );

            showDirResp.add( dirInfo );
        }

        return showDirResp;
    }


    /**
     * This method retrieves the directory information for a given KIFS
     * directory and returns the information as a list of {@link KifsDirectoryInfo}
     * objects. This method uses default options.
     *
     * @param remoteDirName  Name of the KIFS directory to report statistics on;
     *        cannot be null or empty.
     * @return  List of {@link KifsDirectoryInfo} objects, containing statistics
     *        about the given directory.
     * @see #showDirectory(String, Map)
     */
    public List<KifsDirectoryInfo> showDirectory(String remoteDirName) throws GPUdbException {
        return showDirectory(remoteDirName, new HashMap<>());
    }


    /**
     * This method retrieves the directory information for a given KIFS
     * directory and returns the information as a list of {@link KifsDirectoryInfo}
     * objects.
     * 
     * @param remoteDirName  Name of the KIFS directory to report statistics on;
     *        cannot be null or empty.
     * @param showDirectoryOptions  Reserved for future use.
     * @return  List of {@link KifsDirectoryInfo} objects, containing statistics
     *        about the given directory.
     */
    public List<KifsDirectoryInfo> showDirectory(String remoteDirName,
                                                 Map<String, String> showDirectoryOptions) throws GPUdbException {

        if( remoteDirName == null || remoteDirName.isEmpty() ) {
            throw new GPUdbException( "KIFS directory name [remoteDirName] cannot be null or empty" );
        }

        List<KifsDirectoryInfo> showDirResp = new ArrayList<>();

        ShowDirectoriesResponse resp = this.db.showDirectories( remoteDirName, showDirectoryOptions );

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
     *
     * @return  List of {@link KifsDirectoryInfo} objects, containing statistics
     *        about the given directory.
     * @throws GPUdbException  If an error occurs looking up the directories.
     * @see #showAllDirectories(Map)
     */
    public List<KifsDirectoryInfo> showAllDirectories() throws GPUdbException {
        return showAllDirectories(new HashMap<>());
    }


    /**
     * This method retrieves the directory information all KIFS
     * directories and returns the information as a list of {@link KifsDirectoryInfo}
     * objects.
     * 
     * @param showDirectoryOptions  Reserved for future use.
     * @return  List of {@link KifsDirectoryInfo} objects, containing statistics
     *        about the given directory.
     * @throws GPUdbException  If an error occurs looking up the directories.
     */
    public List<KifsDirectoryInfo> showAllDirectories( Map<String, String> showDirectoryOptions) throws GPUdbException {

        List<KifsDirectoryInfo> showDirResp = new ArrayList<>();

        ShowDirectoriesResponse resp = this.db.showDirectories( "", showDirectoryOptions );

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
     * This method will create a KIFS directory with default options.
     *
     * @param remoteDirName  Name of the KIFS directory to create.
     * @throws GPUdbException  If the directory creation fails.
     */
    public void createDirectory(String remoteDirName) throws GPUdbException {
        createDirectory(remoteDirName, true);
    }


    /**
     * This method will create a KIFS directory with options as explained
     * below.
     *
     * @param remoteDirName  Name of the KIFS directory to create.
     * @param noErrorIfExists  True means that if the directory exists, there
     *        will be no error thrown; false means an error will be thrown.
     * @throws GPUdbException  If the directory creation fails.
     */
    public void createDirectory(String remoteDirName, boolean noErrorIfExists) throws GPUdbException {

        Map<String, String> createDirectoryOptions = new HashMap<>();
        createDirectoryOptions.put( CreateDirectoryRequest.Options.NO_ERROR_IF_EXISTS ,
                noErrorIfExists ? CreateDirectoryRequest.Options.TRUE : CreateDirectoryRequest.Options.FALSE);

        this.db.createDirectory(remoteDirName, createDirectoryOptions);
    }


    /**
     * This method deletes a KIFS directory. This method uses default options.
     *
     * @param remoteDirName  Name of the KIFS directory to delete.
     * @throws GPUdbException  If the directory deletion fails.
     * @see #deleteDirectory(String, boolean, boolean)
     */
    public void deleteDirectory(String remoteDirName) throws GPUdbException {
        deleteDirectory(remoteDirName, true, true);
    }


    /**
     * This method deletes a KIFS directory. It will do a recursive delete if
     * such an option is given.
     *
     * @param remoteDirName  Name of the KIFS directory to delete.
     * @param recursive  Indicates all files and virtual subdirectories under
     *        {@code remoteDirName} will also be deleted; if false, an error
     *        will be returned if the directory is not empty.
     * @param noErrorIfNotExists  True means no error will be returned if the
     *        given directory does not exist.
     */
    public void deleteDirectory(String remoteDirName,
                                boolean recursive,
                                boolean noErrorIfNotExists) throws GPUdbException {

        Map<String, String> deleteDirectoryOptions = new HashMap<>();
        deleteDirectoryOptions.put( DeleteDirectoryRequest.Options.RECURSIVE,
                recursive ? DeleteDirectoryRequest.Options.TRUE : DeleteDirectoryRequest.Options.FALSE);
        deleteDirectoryOptions.put( DeleteDirectoryRequest.Options.NO_ERROR_IF_NOT_EXISTS,
                noErrorIfNotExists ? DeleteDirectoryRequest.Options.TRUE : DeleteDirectoryRequest.Options.FALSE);

        this.db.deleteDirectory(remoteDirName, deleteDirectoryOptions);
    }


    /**
     * This class models the options available for modifying some behaviors
     * of the {@link GPUdbFileHandler} class.
     *
     * The two options available right now are:
     * 1. threadPoolTerminationTimeout - in seconds
     * 2. fullFileDispatcherThreadpoolSize - an integer
     */
    public static final class Options {

        private long fileSizeToSplit;
        private int fullFileDispatcherThreadpoolSize;

        /**
         * Constructor with default values for the class members.
         */
        public Options() {
            this.fullFileDispatcherThreadpoolSize = DEFAULT_FULL_FILE_DISPATCHER_THREADPOOL_SIZE;
            this.fileSizeToSplit = DEFAULT_FILE_SIZE_TO_SPLIT;
        }

        /**
         * A copy constructor
         * 
         * @param other  The {@link Options} instance to copy from.
         */
        public Options( Options other ) {
            this.fullFileDispatcherThreadpoolSize = other.fullFileDispatcherThreadpoolSize;
            this.fileSizeToSplit = other.fileSizeToSplit;
        }

        /**
         * This value is used to configure the size of the thread pool used
         * internally to handle batches of full file uploads. The default value
         * is set to 5.

         * @return  The value of the 'fullFileDispatcherThreadpoolSize'.
         *
         * @see #setFullFileDispatcherThreadpoolSize(int)
         */
        public int getFullFileDispatcherThreadpoolSize() {
            return this.fullFileDispatcherThreadpoolSize;
        }

        /**
         * This value is used to configure the size of the thread pool used
         * internally to handle batches of full file uploads. The default value
         * is set to 5.

         * @param fullFileDispatcherThreadpoolSize  The number of threads that
         *        should exist in the file dispatcher pool.
         * @return  The current {@link Options} instance.
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
         * Gets the value of the file split size--files smaller than this size
         * will be uploaded in batches up to this size, and files greater than
         * this size will be uploaded in parts up to this size.
         *
         * @return  The file split size in bytes.
         *
         * @see #setFileSizeToSplit(long)
         */
        public long getFileSizeToSplit() {
            return this.fileSizeToSplit;
        }

        /**
         * Sets the value of the file split size--files smaller than this size
         * will be uploaded in batches up to this size, and files greater than
         * this size will be uploaded in parts up to this size.
         *
         * @param fileSizeToSplit  The file split size in bytes.
         */
        public void setFileSizeToSplit( long fileSizeToSplit ) throws GPUdbException {
            if( fileSizeToSplit <=0 || fileSizeToSplit > DEFAULT_FILE_SIZE_TO_SPLIT ) {
                throw new GPUdbException( String.format( "FileSizeToSplit : Value must be a positive value less than or equal to %s", DEFAULT_FILE_SIZE_TO_SPLIT ));
            }
            this.fileSizeToSplit = fileSizeToSplit;
        }
    }


    /**
     * This method checks whether the given KIFS directories exist or not.
     *
     * @param dirNames  The set of full KIFS directory paths to check for.
     * @return  True if all the given directories exist, and false if one or
     *        more of them does not exist.
     */
    public boolean kifsDirectoriesExist( Set<String> dirNames ) {

        if( dirNames == null || dirNames.isEmpty() ) {
            GPUdbLogger.error( "List of Directory names cannot be null or empty" );
            return false;
        }

        // Get all the KIFS directories and put them into a Set
        ShowDirectoriesResponse sdResp;
        try {
            sdResp = this.db.showDirectories( "", new HashMap<>());
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
     * This method checks whether the given KIFS directory exists or not.
     *
     * @param dirName  The full KIFS directory path to check for.
     * @return  True if the given directory exists, and false if it doesn't.
     */
    public boolean kifsDirectoryExists( String dirName ) {

        // 'show/directories' will only return the KIFS roots so an embedded
        // '/' will always result in a failed match.
        if( dirName == null || dirName.isEmpty() || dirName.contains( KIFS_PATH_SEPARATOR )) {
            GPUdbLogger.error( "KIFS directory name cannot be null or empty and cannot contain an embedded '/' " );
            return false;
        }

        // Get all the KIFS directories and put them into a Set
        ShowDirectoriesResponse sdResp = null;
        try {
            sdResp = this.db.showDirectories( dirName, new HashMap<>());
            // Return true if the list of directories returned contain the input
            // directory name
            return sdResp.getDirectories().contains( dirName );
        } catch (GPUdbException e) {
            GPUdbLogger.error( e.getMessage() );
            return false;
        }
    }


    /**
     * This method checks whether the given KIFS file exists or not.
     *
     * @param fileName  The full KIFS file path to check for.
     * @return  True if the given file exists, and false if it doesn't.
     */
    public boolean kifsFileExists( String fileName ) {

        if( fileName == null || fileName.isEmpty() ) {
            GPUdbLogger.error( "KIFS file name cannot be null" );
            return false;
        }

        ShowFilesResponse sfResp = null;
        try {
            sfResp = this.db.showFiles( Collections.singletonList( fileName ), new HashMap<>());
            // Return true if the list of directories returned contain the input
            // directory name
            return sfResp.getFileNames().contains( fileName );
        } catch (GPUdbException e) {
            GPUdbLogger.error( e.getMessage() );
            return false;
        }

    }
}
