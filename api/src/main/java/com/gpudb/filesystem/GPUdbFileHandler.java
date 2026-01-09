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
 * <p>The entire {@code filesystem} API is exposed through this class and an
 * instance of this class is enough to consume all functionalities provided by
 * the KiFS.
 * * <p>Supported file path patterns include standard Glob syntax (e.g. {@code *.csv},
 * {@code data/**}, {@code file_?.txt}).
 *
 * <p>Apart from this class the other classes which could be used by the users of
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
     * Uploads a single file from a given local path to a given KiFS directory.
     * The filename passed in is preserved between the client and the server.
     * This method uses the default upload options and does not use a callback.
     *
     * @param fileName  Name and path of the file (or Glob pattern) to upload;
     *        e.g., "/home/user1/dir1/dir2/a.txt" or "/data/*.csv".
     * @param remoteDirName  Name of the KiFS directory to upload to.
     * @throws GPUdbException  If an error occurs uploading the file(s) to the
     *         server.
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
     * Uploads a single file from a given local path to a given KiFS directory
     * using the given upload options and callback. The filename passed in is
     * preserved between the client and the server.
     *
     * @param fileName  Name and path of the file (or Glob pattern) to upload;
     *        e.g., "/home/user1/dir1/dir2/a.txt" or "/data/*.csv".
     * @param remoteDirName  Name of the KiFS directory to upload to.
     * @param uploadOptions  Various options to be used for uploading a file;
     *        use null for
     *        {@link UploadOptions#defaultOptions() default options}.
     * @param callback  An instance of {@link FileUploadListener}, which is used
     *        to report the upload progress.
     * @throws GPUdbException  If an error occurs uploading the file(s) to the
     *         server.
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
     * Uploads multiple files from the given local paths to a given KiFS
     * directory. The filenames passed in are preserved between the client and
     * the server. This method uses the default upload options and does not use
     * a callback.
     *
     * @param fileNames  Names and paths of the files (or Glob patterns) to
     *        upload; e.g., "/home/user1/dir1/dir2/a.txt" or "/data/*.csv".
     * @param remoteDirName  Name of the KiFS directory to upload to.
     * @throws GPUdbException  If an error occurs uploading the file(s) to the
     *         server.
     *
     * @see FileUploadListener
     * @see UploadOptions
     * @see UploadOptions#defaultOptions()
     * @see #upload(List, String, UploadOptions, FileUploadListener)
     */
    public void upload(final List<String> fileNames, final String remoteDirName) throws GPUdbException {
        upload(fileNames, remoteDirName, UploadOptions.defaultOptions(), null );
    }


    /**
     * Uploads multiple files from the given local paths to a given KiFS
     * directory. The filenames passed in are preserved between the client and
     * the server.
     *
     * @param fileNames  Names and paths of the files (or Glob patterns) to
     *        upload; e.g., "/home/user1/dir1/dir2/a.txt" or "/data/*.csv".
     * @param remoteDirName  Name of the KiFS directory to upload to.
     * @param uploadOptions  Various options to be used for uploading a file;
     *        use null for
     *        {@link UploadOptions#defaultOptions() default options}.
     * @param callback  An instance of {@link FileUploadListener}, which is used
     *        to report the upload progress.
     * @throws GPUdbException  If an error occurs uploading the file(s) to the
     *         server.
     *
     * @see FileUploadListener
     * @see UploadOptions
     */
    public void upload(final List<String> fileNames,
                       final String remoteDirName,
                       UploadOptions uploadOptions,
                       FileUploadListener callback) throws GPUdbException {

        // Validates that explicit files exist.
        // Note: FileOperation.localFileExists now returns TRUE for Glob patterns
        // to allow the FileUploader to handle the resolution.
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
            throw new GPUdbException( String.format( "Input files [%s] not found (patterns must match valid Glob syntax)", nonExistentFileNames ) );
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
     * Downloads KiFS files with the given paths to a given local directory.
     * This method uses the default download options and does not use a
     * callback.
     *
     * @param fileNames  Paths of the KiFS files to download.
     * @param localDirName  Path of the writable local directory to download to.
     *
     * @see DownloadOptions
     * @see DownloadOptions#defaultOptions()
     * @see FileDownloadListener
     */
    public void download(List<String> fileNames, String localDirName) throws GPUdbException {
        download(fileNames, localDirName, DownloadOptions.defaultOptions(), null );
    }


    /**
     * Downloads KiFS files with the given paths to a given local directory.
     *
     * @param fileNames  Paths of the KiFS files to download.
     * @param localDirName  Path of the writable local directory to download to.
     * @param downloadOptions  Various options to be used for downloading a
     *        file; use null for
     *        {@link DownloadOptions#defaultOptions() default options}.
     * @param callback  An instance of {@link FileDownloadListener}, which is
     *        used to report the download progress.
     */
    public void download(List<String> fileNames,
                         String localDirName,
                         DownloadOptions downloadOptions,
                         FileDownloadListener callback) throws GPUdbException {

        if( fileNames!= null && fileNames.size() > 0 ) {
            for ( String fileName: fileNames ) {
                if( fileName == null || fileName.isEmpty() ) {
                    throw new GPUdbException( "KiFS file name cannot be null or empty" );
                }
            }
        } else {
            throw new GPUdbException( "List of KiFS file names cannot be null or empty" );
        }

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
     * Downloads a KiFS file with the given path to a given local directory.
     * This method uses the default download options and does not use a
     * callback.
     *
     * @param fileName  Paths of the KiFS file to download.
     * @param localDirName  Path of the writable local directory to download to.
     *
     * @see DownloadOptions
     * @see DownloadOptions#defaultOptions()
     * @see FileDownloadListener
     */
    public void download(String fileName, String localDirName) throws GPUdbException {
        download(fileName, localDirName, DownloadOptions.defaultOptions(), null );
    }


    /**
     * Downloads a KiFS file with the given path to a given local directory.
     *
     * @param fileName  Paths of the KiFS file to download.
     * @param localDirName  Path of the writable local directory to download to.
     * @param downloadOptions  Various options to be used for downloading a
     *        file; use null for
     *        {@link DownloadOptions#defaultOptions() default options}.
     * @param callback  An instance of {@link FileDownloadListener}, which is
     *        used to report the download progress.
     */
    public void download(String fileName,
                         String localDirName,
                         DownloadOptions downloadOptions,
                         FileDownloadListener callback) throws GPUdbException {

        download( Collections.singletonList( fileName ), localDirName, downloadOptions, callback );
    }


    /**
     * Downloads all files in a given KiFS directory to a given local directory.
     *
     * @param remoteDirName - Path of the KiFS directory to download.
     * @param localDirName - Path of the local directory to download to.
     * @param downloadOptions  Various options to be used for downloading a
     *        file; use null for
     *        {@link DownloadOptions#defaultOptions() default options}.
     * @param callback  An instance of {@link FileDownloadListener}, which is
     *        used to report the download progress.
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

        if( !FileOperation.localDirExists( localDirName ) ) {
            throw new GPUdbException( String.format( "Local directory %s does not exist", localDirName ) );
        }

        ShowFilesResponse showFilesResponse = this.db.showFiles( Collections.singletonList(remoteDirName), new HashMap<>() );

        if( !showFilesResponse.getFileNames().isEmpty() ) {
            download(showFilesResponse.getFileNames(), localDirName, downloadOptions, callback);
        }
    }


    /**
     * Uploads and ingests multiple files from the given local paths to a given
     * table.
     *
     * @param fileNames  Names and paths of the files (or Glob patterns) to
     *        ingest; e.g., "/home/user1/dir1/dir2/a.txt" or "/data/*.csv".
     * @param tableName  Name of the table to ingest into.
     * @param ingestOptions  Various options to be used for ingesting a file;
     *        use null for
     *        {@link IngestOptions default options}.
     * @param createTableOptions  Various options to be used for creating the
     *        target table; use null for
     *        {@link TableCreationOptions default options}.
     *
     * @throws  GPUdbException If the upload or ingest fails.
     *
     * @see IngestOptions
     * @see TableCreationOptions
     */
    public void ingest( final List<String> fileNames,
                        final String tableName,
                        final IngestOptions ingestOptions,
                        final TableCreationOptions createTableOptions ) throws GPUdbException{

        final List<String> nonExistentFileNames = new ArrayList<>();

        if ( fileNames != null && fileNames.size() > 0 ) {
            for (String fileName : fileNames) {
                if( fileName != null && !fileName.isEmpty() ) {
                    // Delegated to FileOperation.localFileExists for Glob/Existence check
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
     * Deletes the given list of fully-qualified KiFS file paths, suppressing
     * the errors if the files are not found in KiFS.
     *
     * @param fileNames  List of files in KiFS to delete.
     * @throws GPUdbException  If an error occurs deleting the files.
     */
    public void deleteFiles(List<String> fileNames) throws GPUdbException {
        deleteFiles(fileNames, true);
    }


    /**
     * Deletes the given list of fully-qualified KiFS file paths.
     *
     * @param fileNames  List of files in KiFS to delete.
     * @param noErrorIfNotExists  Whether or not to suppress errors if the given
     *        files are not found in KiFS.
     * @throws GPUdbException  If an error occurs deleting the files.
     */
    public void deleteFiles(List<String> fileNames, boolean noErrorIfNotExists) throws GPUdbException {

        Map<String, String> deleteOptions = new HashMap<>();

        deleteOptions.put( DeleteFilesRequest.Options.NO_ERROR_IF_NOT_EXISTS,
                noErrorIfNotExists ? DeleteFilesRequest.Options.TRUE : DeleteFilesRequest.Options.FALSE );

        this.db.deleteFiles( fileNames, deleteOptions );
    }


    /**
     * Deletes the files in the given KiFS directory, suppressing the errors if
     * the directory is not found in KiFS.
     *
     * @param remoteDirName  Name of the KiFS directory containing files to
     *        delete.
     * @throws GPUdbException  If an error occurs deleting the files.
     */
    public void deleteFilesInDir(String remoteDirName) throws GPUdbException {
        deleteFilesInDir(remoteDirName, true);
    }


    /**
     * Deletes the files in the given KiFS directory.
     * 
     * @param remoteDirName  Name of the KiFS directory containing files to
     *        delete.
     * @param noErrorIfNotExists  Whether or not to suppress errors if the given
     *        directory is not found in KiFS.
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
     * Returns statistics about the given KiFS files and/or directories.
     * Wildcards "?" and "*" can be used to represent one character and zero or
     * more characters, respectively.
     *
     * @param remotePaths  List of KiFS files and/or directory names to report
     *        statistics on.
     * @return List of {@link KifsFileInfo} objects, containing statistics
     *         about the given files.
     * @throws GPUdbException  If an error occurs looking up the files.
     */
    public List<KifsFileInfo> showFiles(List<String> remotePaths) throws GPUdbException {
        List<KifsFileInfo> resp = new ArrayList<>();

        if( remotePaths == null || remotePaths.isEmpty() ) {
            throw new GPUdbException( "List of KiFS directory names cannot be null or empty list" );
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
     * Returns statistics about the given KiFS directories.
     *
     * @deprecated
     * @param remoteDirNames  Set of KiFS directory names to report statistics
     *        on.
     * @return List of {@link KifsDirectoryInfo} objects, containing statistics
     *         about the given directories.
     * @throws GPUdbException  If an error occurs looking up the directories.
     * @see #showDirectories(Set, Map)
     */
    @Deprecated(since = "7.2.3", forRemoval = true)
    public List<KifsDirectoryInfo> showDirectories(Set<String> remoteDirNames) throws GPUdbException {
        return showDirectories(new ArrayList<>(remoteDirNames), new HashMap<>());
    }


    /**
     * Returns statistics about the given KiFS directories.
     *
     * @deprecated
     * @param remoteDirNames  Set of KiFS directory names to report statistics
     *        on.
     * @param showDirectoryOptions  Unused.
     * @return List of {@link KifsDirectoryInfo} objects, containing statistics
     *         about the given directories.
     * @throws GPUdbException  If an error occurs looking up the directories.
     */
    @Deprecated(since = "7.2.3", forRemoval = true)
    public List<KifsDirectoryInfo> showDirectories(Set<String> remoteDirNames,
                                                   Map<String, String> showDirectoryOptions) throws GPUdbException {
        return showDirectories(new ArrayList<>(remoteDirNames), showDirectoryOptions);
    }


    /**
     * Returns statistics about the given KiFS directories.
     *
     * @param remoteDirNames  List of KiFS directory names.
     * @return List of {@link KifsDirectoryInfo} objects, containing statistics
     *         about the given directories.
     * @throws GPUdbException  If an error occurs looking up the directories.
     */
    public List<KifsDirectoryInfo> showDirectories(List<String> remoteDirNames) throws GPUdbException {
        return showDirectories(remoteDirNames, new HashMap<>());
    }

    /**
     * Returns statistics about the given KiFS directories.
     * 
     * @param remoteDirNames  List of KiFS directory names.
     * @param showDirectoryOptions  Reserved for future use.
     * @return List of {@link KifsDirectoryInfo} objects, containing statistics
     *         about the given directories.
     * @throws GPUdbException  If an error occurs looking up the directories.
     */
    public List<KifsDirectoryInfo> showDirectories(List<String> remoteDirNames,
                                                   Map<String, String> showDirectoryOptions) throws GPUdbException {

        if( remoteDirNames == null || remoteDirNames.isEmpty() ) {
            throw new GPUdbException( "Set of KiFS directory names cannot be null or empty" );
        }

        List<KifsDirectoryInfo> showDirResp = new ArrayList<>();

        for( String dirName: remoteDirNames ) {
            if( dirName == null || dirName.trim().isEmpty() ) {
                throw new GPUdbException( "Set of KiFS directory names cannot contain null or empty directory name" );
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
     * Returns statistics about the given KiFS directory.
     *
     * @param remoteDirName  Name of the KiFS directory.
     * @return List of {@link KifsDirectoryInfo} objects, containing statistics
     *         about the given directory.
     * @throws GPUdbException  If an error occurs looking up the directory.
     */
    public List<KifsDirectoryInfo> showDirectory(String remoteDirName) throws GPUdbException {
        return showDirectory(remoteDirName, new HashMap<>());
    }


    /**
     * Returns statistics about the given KiFS directory.
     * 
     * @param remoteDirName  Name of the KiFS directory.
     * @param showDirectoryOptions  Reserved for future use.
     * @return List of {@link KifsDirectoryInfo} objects, containing statistics
     *         about the given directory.
     * @throws GPUdbException  If an error occurs looking up the directory.
     */
    public List<KifsDirectoryInfo> showDirectory(String remoteDirName,
                                                 Map<String, String> showDirectoryOptions) throws GPUdbException {

        if( remoteDirName == null || remoteDirName.isEmpty() ) {
            throw new GPUdbException( "KiFS directory name [remoteDirName] cannot be null or empty" );
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
     * Returns statistics about all KiFS directories.
     *
     * @return List of {@link KifsDirectoryInfo} objects, containing statistics
     *         about all directories.
     * @throws GPUdbException  If an error occurs looking up the directories.
     */
    public List<KifsDirectoryInfo> showAllDirectories() throws GPUdbException {
        return showAllDirectories(new HashMap<>());
    }


    /**
     * Returns statistics about all KiFS directories.
     * 
     * @param showDirectoryOptions  Reserved for future use.
     * @return List of {@link KifsDirectoryInfo} objects, containing statistics
     *         about all directories.
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
     * Creates a KiFS directory with the given name, suppressing the error if
     * the directory already exists in KiFS.
     *
     * @param remoteDirName  Name of the KiFS directory to create.
     * @throws GPUdbException  If the directory creation fails.
     */
    public void createDirectory(String remoteDirName) throws GPUdbException {
        createDirectory(remoteDirName, true);
    }


    /**
     * Creates a KiFS directory with the given name.
     *
     * @param remoteDirName  Name of the KiFS directory to create.
     * @param noErrorIfExists  Whether or not to suppress an error if the given
     *        directory already exists in KiFS.
     * @throws GPUdbException  If the directory creation fails.
     */
    public void createDirectory(String remoteDirName, boolean noErrorIfExists) throws GPUdbException {

        Map<String, String> createDirectoryOptions = new HashMap<>();
        createDirectoryOptions.put( CreateDirectoryRequest.Options.NO_ERROR_IF_EXISTS ,
                noErrorIfExists ? CreateDirectoryRequest.Options.TRUE : CreateDirectoryRequest.Options.FALSE);

        this.db.createDirectory(remoteDirName, createDirectoryOptions);
    }


    /**
     * Deletes the given KiFS directory and all files under it, suppressing the
     * error if the directory is not found in KiFS.
     *
     * @param remoteDirName  Name of the KiFS directory to delete.
     * @throws GPUdbException  If the directory deletion fails.
     * @see #deleteDirectory(String, boolean, boolean)
     */
    public void deleteDirectory(String remoteDirName) throws GPUdbException {
        deleteDirectory(remoteDirName, true, true);
    }


    /**
     * Deletes the given KiFS directory.
     *
     * @param remoteDirName  Name of the KiFS directory to delete.
     * @param recursive  Whether or not to delete all files and virtual
     *        subdirectories under {@code remoteDirName} as well.
     * @param noErrorIfNotExists  Whether or not to suppress an error if the
     *        given directory does not exist in KiFS.
     * @throws GPUdbException  If the directory deletion fails.
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
     */
    public static final class Options {

        private long fileSizeToSplit;
        private int fullFileDispatcherThreadpoolSize;

        /**
         * Create default file handling options.
         */
        public Options() {
            this.fullFileDispatcherThreadpoolSize = DEFAULT_FULL_FILE_DISPATCHER_THREADPOOL_SIZE;
            this.fileSizeToSplit = DEFAULT_FILE_SIZE_TO_SPLIT;
        }

        /**
         * Create file handling options as a copy of the given ones.
         * 
         * @param other  The {@link Options} instance to copy from.
         */
        public Options( Options other ) {
            this.fullFileDispatcherThreadpoolSize = other.fullFileDispatcherThreadpoolSize;
            this.fileSizeToSplit = other.fileSizeToSplit;
        }

        /**
         * Returns the size of the thread pool used internally to handle batches
         * of full file uploads. The default value is 5.

         * @return  The configured size of the dispatcher thread pool.
         *
         * @see #setFullFileDispatcherThreadpoolSize(int)
         */
        public int getFullFileDispatcherThreadpoolSize() {
            return this.fullFileDispatcherThreadpoolSize;
        }

        /**
         * Sets the size of the thread pool used internally to handle batches of
         * full file uploads. The default value is 5.

         * @param fullFileDispatcherThreadpoolSize  The number of threads that
         *        the file dispatcher pool should use.
         * @return  The current {@link Options} instance.
         * @throws GPUdbException  If the given thread pool size is invalid in
         *         the current execution environment.
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
     * Checks whether the given KiFS directories exist or not.
     *
     * @param dirNames  The set of full KiFS directory paths to check for.
     * @return  Whether or not all of the given directories exist.
     */
    public boolean kifsDirectoriesExist( Set<String> dirNames ) {

        if( dirNames == null || dirNames.isEmpty() ) {
            GPUdbLogger.error( "List of Directory names cannot be null or empty" );
            return false;
        }

        ShowDirectoriesResponse sdResp;
        try {
            sdResp = this.db.showDirectories( "", new HashMap<>());
            Set<String> remoteDirNames = new LinkedHashSet<>( sdResp.getDirectories() );

            remoteDirNames.retainAll( dirNames );

            return remoteDirNames.equals( dirNames );
        } catch (GPUdbException e) {
            GPUdbLogger.error( e.getMessage() );
            return false;
        }
    }


    /**
     * Checks whether the given KiFS directory exists or not.
     * <p>
     * Directory name may contain separators for nested directory checks.
     *
     * @param dirName  The full KiFS directory path to check for.
     * @return  Whether or not the given directory exists.
     */
    public boolean kifsDirectoryExists( String dirName ) {

        if( dirName == null || dirName.isEmpty() ) {
            GPUdbLogger.error( "KiFS directory name cannot be null or empty" );
            return false;
        }

        ShowDirectoriesResponse sdResp = null;
        try {
            sdResp = this.db.showDirectories( dirName, new HashMap<>());
            return sdResp.getDirectories().contains( dirName );
        } catch (GPUdbException e) {
            GPUdbLogger.error( e.getMessage() );
            return false;
        }
    }


    /**
     * Checks whether the given KiFS file exists or not.
     *
     * @param fileName  The full KiFS file path to check for.
     * @return  Whether or not the given file exists.
     */
    public boolean kifsFileExists( String fileName ) {

        if( fileName == null || fileName.isEmpty() ) {
            GPUdbLogger.error( "KiFS file name cannot be null" );
            return false;
        }

        ShowFilesResponse sfResp = null;
        try {
            sfResp = this.db.showFiles( Collections.singletonList( fileName ), new HashMap<>());
            return sfResp.getFileNames().contains( fileName );
        } catch (GPUdbException e) {
            GPUdbLogger.error( e.getMessage() );
            return false;
        }

    }
}