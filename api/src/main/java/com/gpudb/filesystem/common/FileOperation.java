package com.gpudb.filesystem.common;

import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.GPUdbLogger;
import com.gpudb.filesystem.GPUdbFileHandler;
import com.gpudb.filesystem.ingest.FileIngestor;
import com.gpudb.protocol.InsertRecordsFromFilesRequest;
import com.gpudb.protocol.ShowFilesResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * This is an internal class and not meant to be used by the end users of the
 * {@code filesystem} API. The consequences of using this class directly in
 * client code is not guaranteed and maybe undesirable.

 * This is the base class from which the classes {@link com.gpudb.filesystem.upload.FileUploader}
 * and {@link com.gpudb.filesystem.download.FileDownloader} are derived.
 * The purpose of this class is to model certain basic functions like
 * searching directories for specific file patterns, retrieving file attributes
 * like size, creation time etc. for both files on the KIFS and local directories
 * and also determining which files to be uploaded/downloaded either in one go
 * or in parts based on the size and a threshold which is given by the method
 * getFileSizeToSplit() of the GPUdbFileHandler.Options class.
 */
public class FileOperation {

    protected final OpMode opMode;

    protected final GPUdb db;

    /**
     * Indicates whether the search for files is to be recursive through a
     * directory hierarchy or not. This applies to only uploads for the time
     * being and the search through local directories could be recursive.
     */
    protected final boolean recursive;

    /**
     * The list of file names to be either uploaded or downloaded.
     * For download, each element in the list must have the full KIFS path
     * and for upload each element must have the full local path.
     */
    protected List<String> fileNames;

    /**
     * The list of files uploaded, used by the {@link FileIngestor} to insert
     * records from the uploaded files into the database.
     */
    protected Set<String> namesOfFilesUploaded;

    /**
     * This stores the directory name into which the source files for any
     * operation (upload or download) will be written. So, for uploads,
     * this is the name of the KIFS directory; and for downloads, this is the
     * name of the local directory.
     */
    protected String dirName;

    /**
     * List of file names that are multi part uploads/downloads
     */
    protected List<String> multiPartList;

    protected List<String> multiPartRemoteFileNames;

    /**
     * List of file names that can be downloaded in full
     */
    protected List<String> fullFileList;

    protected List<String> fullRemoteFileNames;

    protected FullFileBatchManager fullFileBatchManager;

    protected final GPUdbFileHandler.Options fileHandlerOptions;

    /**
     * Constructs a new file operation instance, managing the transfer of a set
     * of files to a target directory.
     * 
     * @param db  The {@link GPUdb} instance used to access KiFS.
     * @param opMode  Indicates whether this is an upload or download operation.
     * @param fileNames  List of source file names.
     * @param dirName  Name of the local/remote target directory depending upon
     *        whether it is a download/upload operation.
     * @param recursive  Indicates whether any directories given in
     *        {@code fileNames} should be searched for files recursively.
     * @param fileHandlerOptions  Options for setting up the files for transfer.
     * @throws GPUdbException  propagates exceptions raised from various argument
     *        validations.
     */
    public FileOperation(final GPUdb db,
                         final OpMode opMode,
                         final List<String> fileNames,
                         final String dirName,
                         boolean recursive,
                         GPUdbFileHandler.Options fileHandlerOptions) throws GPUdbException {
        this.db = db;
        this.opMode = opMode;
        this.fileNames = fileNames;
        this.dirName = dirName;
        this.recursive = recursive;
        this.fileHandlerOptions = fileHandlerOptions;
        this.namesOfFilesUploaded = new LinkedHashSet<>();

        decideMultiPart();
    }

    /**
     * This method returns the list of file names uploaded without the path
     * information. This used by the {@link FileIngestor#ingestFromFiles()}
     * method to prepare the list of file names to be passed on to the
     * {@link GPUdb#insertRecordsFromFiles(InsertRecordsFromFilesRequest)}
     * endpoint.
     * 
     * @return  A set of file names without the path information.
     */
    public Set<String> getNamesOfFilesUploaded() {
        return this.namesOfFilesUploaded;
    }

    /**
     * This method resolves the file names passed in and decides on which could
     * be downloaded in one go and which ones are large enough to be downloaded
     * in parts.
     * 
     * This method calls {@link #traverseLocalDirectories(String, List)} and
     * gets the file names properly resolved in a list.
     */
    protected void decideMultiPart() throws GPUdbException {

        if( this.opMode == OpMode.UPLOAD ) {
            decideMultiPartUpload();
        } else {
            decideMultiPartDownload();
        }

    }

    /**
     * This method sifts through the list of input files names to be uploaded
     * examines their sizes and suitably adds them to two different lists; one
     * for full file uploads and another for multi-part uploads. It first parses
     * the file names {@link #parseFileNames(List)} and then if wildcards are
     * found in the input file names then calls the method
     * {@link #traverseLocalDirectories(String, List)} to get the file names
     * normalized with fully resolved paths.
     */
    private void decideMultiPartUpload() {
        if (this.fullFileList == null)
            this.fullFileList = new ArrayList<>();
        else
            this.fullFileList.clear();

        if( this.fullFileBatchManager == null )
            this.fullFileBatchManager = new FullFileBatchManager( this.fileHandlerOptions );
        else
            this.fullFileBatchManager.clearBatches();

        if (this.multiPartList == null)
            this.multiPartList = new ArrayList<>();
        else
            this.multiPartList.clear();

        if ( this.multiPartRemoteFileNames == null )
            this.multiPartRemoteFileNames = new ArrayList<>();
        else
            this.multiPartRemoteFileNames.clear();

        try {
            List<Triple<String, String, String>> parsedFileNames = parseFileNames( this.fileNames );
            Pair<List<String>, List<String>> resolvedFileLists;

            for (Triple<String, String, String> triple : parsedFileNames) {

                String path = triple.getMiddle();
                String name = triple.getRight();

                List<String> localFileList = null;
                List<String> remoteFileList = null;
                
                // This is a case of a wildcard search either recursive over
                // directories (e.g., "<some_path>/**.csv", ./**.csv, ../*.csv etc.)
                // Since we have wildcards we need to traverse the directories
                // in the hierarchy and figure out all the files that exist
                // in the directory and its sub directories.
                if (name.contains("*") || name.contains("?")) {
                    resolvedFileLists = traverseLocalDirectories( path, Collections.singletonList(name) );
                    localFileList = resolvedFileLists.getLeft();
                    remoteFileList = resolvedFileLists.getRight();
                } else {
                    // This is the case where we are dealing with actual file
                    // names with no wildcards.
                    localFileList = Collections.singletonList( path + File.separator + name);
                    remoteFileList = Collections.singletonList( this.dirName + GPUdbFileHandler.KIFS_PATH_SEPARATOR + name );
                }

                // Put the files found into the correct single/multi-part upload buckets
                sortFilesIntoFullAndMultipartLists( localFileList, remoteFileList );
            }
        } catch (IOException e) {
            GPUdbLogger.error( e.getMessage() );
        }
    }

    /**
     * This method examines the sizes of the files to be downloaded from the
     * server and puts them into two separate lists for downloading files in
     * one go and for multi-part downloads for large files.
     */
    private void decideMultiPartDownload() throws GPUdbException {
        if (this.fullFileList == null)
            this.fullFileList = new ArrayList<>();
        else
            this.fullFileList.clear();

        if (this.multiPartList == null)
            this.multiPartList = new ArrayList<>();
        else
            this.multiPartList.clear();

        List<Triple<String, String, String>> parsedFileNames = parseFileNames( this.fileNames );

        for (Triple<String, String, String> triple : parsedFileNames) {
            String path = triple.getMiddle();
            String name = triple.getRight();

            ShowFilesResponse sfResp = this.db.showFiles( Collections.singletonList( path + GPUdbFileHandler.KIFS_PATH_SEPARATOR + name), new HashMap<>());

            if( sfResp.getSizes().get( 0 ) > this.fileHandlerOptions.getFileSizeToSplit() ) {
                this.multiPartList.add( sfResp.getFileNames().get( 0 ) );
            } else {
                this.fullFileList.add( sfResp.getFileNames().get( 0 ) );
            }
        }
    }

    /**
     * This method takes a list of completely resolved file paths and
     * puts each file into either the {@link #multiPartList} or
     * {@link #fullFileList} depending on the file size threshold returned by
     * {@link com.gpudb.filesystem.GPUdbFileHandler.Options#getFileSizeToSplit}.
     *
     * @param fileList  A list of source file names to triage by size.
     * @param remoteFileList  A list of target file names for the given sources.
     */
    protected void sortFilesIntoFullAndMultipartLists( List<String> fileList, List<String> remoteFileList ) {

        for (int i = 0, fileListSize = fileList.size(); i < fileListSize; i++) {
            String localFileName = fileList.get(i);
            String fileNameWithoutPath = StringUtils.substringAfterLast( localFileName, File.separator );

            this.namesOfFilesUploaded.add( fileNameWithoutPath );

            String remoteFileName = remoteFileList.get( i );

            Path filePath = Paths.get(localFileName);

            try {
                long fileSize = Files.size(filePath);

                if ( fileSize > this.fileHandlerOptions.getFileSizeToSplit() ) {
                    this.multiPartList.add(localFileName);
                    this.multiPartRemoteFileNames.add( remoteFileName );
                } else {
                    // Add the 'localFileName', remoteFileName and fileSize
                    // to the FullFileBatchManager instance.
                    this.fullFileBatchManager.addFile( localFileName, remoteFileName, fileSize );
                }

            } catch (IOException e) {
                GPUdbLogger.error(String.format("Error getting file size of <%s>: %s", localFileName, e.getMessage()));
            }
        }
    }


    /**
     * This method gets the file stats for the files residing on the KIFS.
     *
     * @param path  Name of the KIFS file or directory of files to retrieve info on.
     * @return  List of {@link KifsFileInfo} objects for the KiFS file(s) found.
     * @throws GPUdbException  If the '/show/files' endpoint call fails.
     */
    protected List<KifsFileInfo> getFileInfoFromServer( String path ) throws GPUdbException {
        List<KifsFileInfo> resp = new ArrayList<>();

        ShowFilesResponse sfResp = this.db.showFiles( Collections.singletonList(path), new HashMap<>() );

        int count = sfResp.getFileNames().size();

        for( int i = 0; i < count; i++ ) {
            KifsFileInfo fileInfo = new KifsFileInfo();

            fileInfo.setFileName( sfResp.getFileNames().get( i ) );
            fileInfo.setFileSize( sfResp.getSizes().get( i ) );
            fileInfo.setCreationTime( sfResp.getCreationTimes().get( i ) );
            fileInfo.setCreatedBy( sfResp.getUsers().get( i ) );

            resp.add( fileInfo );

        }

        return resp;
    }


    /**
     * This method traverses the local directory hierarchy starting with the
     * directory given by the argument {@code startingDirName} and the list of file
     * names. The method takes care of recursive searches based on a pattern
     * like (*|**|?) and returns the list of all files found with their fully
     * normalized and absolute paths for the other methods to work with.
     *
     * This method is internal and is only invoked when the input file names
     * have a wildcard pattern.
     *
     * @param startingDirName  Local directory to start searching from
     * @param localFileNames  Local file names and wildcard patterns to search for
     * @return  Pair of lists; the first element is the list of local file
     *        names and the second a list of remote file names
     */
    protected Pair<List<String>, List<String>> traverseLocalDirectories(String startingDirName,
                                                                        List<String> localFileNames) throws IOException {
        List<String> fileList = new ArrayList<>();
        List<String> remoteFileNamesList = new ArrayList<>();

        for ( String pattern : localFileNames ) {

            LocalFileFinder finder = new LocalFileFinder( startingDirName, pattern, this.recursive, this.dirName );
            Files.walkFileTree( Paths.get(startingDirName), finder );
            fileList.addAll( finder.getMatchingFileNames() );

            remoteFileNamesList.addAll( finder.getRemoteFileNames() );

            finder = null;
        }

        return Pair.of( fileList, remoteFileNamesList );
    }

    /**
     * This is a utility method that checks if a local directory exists or not.
     * 
     * @param localDirName  Name of the local directory to check for.
     * @return  True if the directory exists, and false if it doesn't exist or
     *        if the {@code localDirName} is null or empty.
     */
    public static boolean localDirExists(final String localDirName) {
        if( ( localDirName == null ) || localDirName.trim().isEmpty() ) {
            GPUdbLogger.error("Directory name cannot be null or empty");
            return false;
        }

        return Files.isDirectory( Paths.get( localDirName ) );
    }


    /**
     * This method checks if a local file exists or not. If the file name is a
     * wildcard pattern it skips the check.
     *
     * @param localFileName  Name of the file.
     * @return  True if the file exists, false otherwise.
     */
    public static boolean localFileExists(final String localFileName) {
        if( ( localFileName == null ) || localFileName.trim().isEmpty() ) {
            GPUdbLogger.error("File name cannot be null or empty");
            return false;
        }

        // Extract just the file name from the path; if it is just the file name
        // then the {@code File.separator} will not be found and an empty string
        // will be returned; in that case reinstate the value to 'fileName'.
        String extractedFileName = StringUtils.substringAfterLast( localFileName, File.separator );
        if( extractedFileName.isEmpty() ) {
            extractedFileName = localFileName;
        }

        // Input filenames could be wildcard forms as well; if they are then do
        // not check for existence; otherwise check if the file exists
        if( !( extractedFileName.contains("*") || extractedFileName.contains("?") || extractedFileName.contains( "**") ) ) {
            return Files.exists( Paths.get( localFileName ) );
        }

        // Return 'true' here because if there are wildcards the file list will
        // be evaluated anyway and the case of missing files would never arise.
        return true;
    }

    
    /**
     * Use {@link GPUdbFileHandler#KIFS_PATH_SEPARATOR} directly instead.
     * 
     * @deprecated
     * @return  The separator character used by KiFS between a directory and
     *        the files it contains; can also be used in file names to create
     *        "virtual" subdirectories.
     */
    @Deprecated(since = "7.2.3", forRemoval = true)
    public static String getKifsPathSeparator() {
        return GPUdbFileHandler.KIFS_PATH_SEPARATOR;
    }


    /**
     * This method parses the list of file names passed in to the constructor
     * of the classes {@link com.gpudb.filesystem.upload.FileUploader},
     * {@link com.gpudb.filesystem.download.FileDownloader} which are derivatives
     * of this class. It resolves the file names, normalizes them and returns
     * a corresponding list of absolute paths which are used by the methods of
     * this class.
     *
     * @param fileNamesToParse  List of file names passed in to the constructor
     *        of the derivatives of this class.
     * @return  A list of {@link Triple} objects where the first element is the
     *        root of the file path, the second the full path without the file
     *        name and the third just the file name itself.
     */
    public static List<Triple<String, String, String>> parseFileNames(List<String> fileNamesToParse) {

        List<Triple<String, String, String>> listOfRootPathFilenameTriples = new ArrayList<>();

        for ( String fileNameToParse: fileNamesToParse ) {
            Path path = Paths.get( fileNameToParse );
            Path parent = path.getParent();
            String name = path.getFileName().toString();
            String root;

            if( parent == null ) {
                parent = Paths.get("");
                root = Paths.get("").toString();
            } else {
                root = parent.subpath( 0,1).toString();
            }

            listOfRootPathFilenameTriples.add( Triple.of( root, parent.toString(), name));

        }

        return listOfRootPathFilenameTriples;
    }

}
