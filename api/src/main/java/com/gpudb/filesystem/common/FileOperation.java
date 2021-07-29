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
 * filesystem API. The consequences of using this class directly in client code
 * is not guaranteed and maybe undesirable.

 * This is the base class from which the classes {@link com.gpudb.filesystem.upload.FileUploader}
 * and {@link com.gpudb.filesystem.download.FileDownloader} are derived.
 * The purpose of this class is to model certain basic functions like
 * searching directories for specific file patterns, retrieving file attributes
 * like size, creation time etc. for both files on the KIFS and local directories
 * and also determining which files to be uploaded/downloaded either in one go
 * or in parts based on the size and a threshold which is given by the method
 * getFileSizeToSplit() of the GPUdbFileHandler.Options class.
 *
 */
public class FileOperation {

    protected static final String KIFS_PATH_SEPARATOR = "/";

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

    protected Set<String> namesOfFilesUploaded;

    /**
     * This stores the directory name from which the source files for any
     * operation (upload or download) is to be searched in. So, for uploads
     * this is the name of the KIFS directory and for downloads this is the
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
     *
     * @param db - The {@link GPUdb} instance
     * @param opMode - Indicates whether this is an upload or download operation
     * @param fileNames - List<String> - List of inout file names
     * @param dirName - Name of the local/remote directory depending upon
     *                whether it is a download/upload operation.
     * @param recursive - boolean - Indicates whether the path resolution needs
     * @param fileHandlerOptions - a GPUdbFileHandler.Options type object
     * @throws GPUdbException - propagates exceptions raised from various argument
     * validations.
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

        decideMultiPart( opMode );
    }

    /**
     * This method returns the list of file names uploaded without the path
     * information. This used by the {@link FileIngestor#ingestFromFiles()}
     * method to prepare the list of file names to be passed on to the
     * {@link GPUdb#insertRecordsFromFiles(InsertRecordsFromFilesRequest)}
     * endpoint.
     * @return - A set of file names without the path information.
     */
    public Set<String> getNamesOfFilesUploaded() {
        return namesOfFilesUploaded;
    }

    /**
     * This method resolves the file names passed in and decides on which could
     * be downloaded in one go and which ones are large enough to be downloaded
     * in parts.
     * This method internally calls the method {@link #traverseLocalDirectories(String, List)}
     * and gets the file names properly resolved in a list.
     *
     * @param opMode - {@link OpMode}
     */
    protected void decideMultiPart(OpMode opMode) throws GPUdbException {

        if( opMode == OpMode.UPLOAD ) {
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
    private void decideMultiPartUpload( ) {
        if (fullFileList == null) {
            fullFileList = new ArrayList<>();
        }
        else {
            fullFileList.clear();
        }

        if( fullFileBatchManager == null ) {
            fullFileBatchManager = new FullFileBatchManager( this.fileHandlerOptions );
        } else {
            fullFileBatchManager.clearBatches();
        }

        if (multiPartList == null) {
            multiPartList = new ArrayList<>();
        }
        else {
            multiPartList.clear();
        }

        if( multiPartRemoteFileNames == null ) {
            multiPartRemoteFileNames = new ArrayList<>();
        } else {
            multiPartRemoteFileNames.clear();
        }

        try {
            List<Triple<String, String, String>> parsedFileNames = parseFileNames( fileNames );
            Pair<List<String>, List<String>> resolvedFileLists;

            for (Triple<String, String, String> triple : parsedFileNames) {
                String path = triple.getMiddle();
                String name = triple.getRight();

                // This is a case of a wildcard search either recursive over
                // directories (e.g., "<some_path>/**.csv", ./**.csv, ../*.csv etc.)
                // Since we have wildcards we need to traverse the directories
                // in the hierarchy and figure out all the files that exist
                // in the directory and its sub directories.
                if (name.contains("*") || name.contains("?")) {
                    resolvedFileLists = traverseLocalDirectories( path, Collections.singletonList(name) );
                    sortFilesIntoFullAndMultipartLists( resolvedFileLists.getLeft(), resolvedFileLists.getRight() );
                } else {
                    // This is the case where we are dealing with actual file
                    // names with no wildcards.
                    sortFilesIntoFullAndMultipartLists( Collections.singletonList( path + File.separator + name),
                            Collections.singletonList( dirName + FileOperation.KIFS_PATH_SEPARATOR + name ) );
                }

            }
        } catch (IOException e) {
            GPUdbLogger.error( e.getMessage() );
        }
    }

    /**
     * This method examines the sizes of the files to be downloaded from the
     * server and puts them into two separate lists for downloading files in
     * one go and for multi-part downloads for large files.
     *
     */
    private void decideMultiPartDownload() throws GPUdbException {
        if (fullFileList == null) {
            fullFileList = new ArrayList<>();
        }
        else {
            fullFileList.clear();
        }

        if (multiPartList == null) {
            multiPartList = new ArrayList<>();
        }
        else {
            multiPartList.clear();
        }

        List<Triple<String, String, String>> parsedFileNames = parseFileNames( fileNames );

        for (Triple<String, String, String> triple : parsedFileNames) {
            String path = triple.getMiddle();
            String name = triple.getRight();

            ShowFilesResponse sfResp = db.showFiles( Collections.singletonList( path + FileOperation.KIFS_PATH_SEPARATOR + name), new HashMap<String, String>());

            if( sfResp.getSizes().get( 0 ) > fileHandlerOptions.getFileSizeToSplit() ) {
                multiPartList.add( sfResp.getFileNames().get( 0 ) );
            } else {
                fullFileList.add( sfResp.getFileNames().get( 0 ) );
            }
        }
    }

    /**
     * This method takes a list of completely resolved file paths and
     * puts each file into either the {@link #multiPartList} or
     * {@link #fullFileList} depending on the file size threshold returned by
     * the method getFileSizeToSplit() of the GPUdbFileHandler.Options class.
     *
     * @param fileList - a list of file names
     */
    protected void sortFilesIntoFullAndMultipartLists( List<String> fileList, List<String> remoteFileList ) {

        for (int i = 0, fileListSize = fileList.size(); i < fileListSize; i++) {
            String localFileName = fileList.get(i);
            String fileNameWithoutPath = StringUtils.substringAfterLast( localFileName, File.separator );

            namesOfFilesUploaded.add( fileNameWithoutPath );

            String remoteFileName = remoteFileList.get( i );

            Path filePath = Paths.get(localFileName);

            try {
                long fileSize = Files.size(filePath);

                if ( fileSize > fileHandlerOptions.getFileSizeToSplit() ) {
                    multiPartList.add(localFileName);
                    multiPartRemoteFileNames.add( remoteFileName );
                } else {
                    // Add the 'localFileName', remoteFileName and fileSize
                    // to the FullFileBatchManager instance.
                    fullFileBatchManager.addFile( localFileName, remoteFileName, fileSize );
                }

            } catch (IOException e) {
                GPUdbLogger.error(e.getMessage());
            }
        }

    }

    /**
     * This method gets the file stats for the files residing on the KIFS.
     *
     * @param dirName - Name of the KIFS directory root.
     * @return - List of {@link KifsFileInfo} objects
     * @throws GPUdbException if 'show/files' endpoint fails
     */
    protected List<KifsFileInfo> getFileInfoFromServer( String dirName, String fileName ) throws GPUdbException {
        List<KifsFileInfo> resp = new ArrayList<>();

        ShowFilesResponse sfResp = db.showFiles( Collections.singletonList( dirName + KIFS_PATH_SEPARATOR + fileName ), new HashMap<String, String>() );

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
     * This method gets the file stats for the files residing on the KIFS.
     *
     * @param dirName - Name of the KIFS directory root.
     * @return - List of {@link KifsFileInfo} objects
     * @throws GPUdbException in case 'show/files' endpoint fails
     */
    protected List<KifsFileInfo> getFileInfoFromServer( String dirName ) throws GPUdbException {
        List<KifsFileInfo> resp = new ArrayList<>();

        ShowFilesResponse sfResp = db.showFiles( Collections.singletonList(dirName), new HashMap<String, String>() );

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
     * directory given by the argument 'startingDirName' and the list of file
     * names. The method takes care of recursive searches based on a pattern
     * like (*|**|?) and returns the list of all files found with their fully
     * normalized and absolute paths for the other methods to work with.
     *
     * This method is internal and is only invoked when the input file names
     * have a wildcard pattern.
     *
     * @param startingDirName - Directory to start searching from
     * @param fileNames - Wildcard pattern to search for
     * @return - Pair of lists; the first element is the list of local file names
     * and and the second a list of remote file names
     */
    protected Pair<List<String>, List<String>> traverseLocalDirectories(String startingDirName,
                                                                        List<String> fileNames) throws IOException {
        List<String> fileList = new ArrayList<>();
        List<String> remoteFileNamesList = new ArrayList<>();

        for ( String pattern: fileNames ) {

            LocalFileFinder finder = new LocalFileFinder( startingDirName, pattern, recursive, dirName );
            Files.walkFileTree( Paths.get(startingDirName), finder );
            fileList.addAll( finder.getMatchingFileNames() );

            remoteFileNamesList.addAll( finder.getRemoteFileNames() );

            finder = null;
        }

        return Pair.of( fileList, remoteFileNamesList );
    }

    /**
     * This is an utility method that checks if a local directory exists or not
     * @param dirName - Name of the directory
     * @return - true if the directory exists and false if it doesn't exist or if
     * the 'dirName' is null or empty.
     */
    public static boolean localDirExists(final String dirName) {
        if( ( dirName == null ) || dirName.trim().isEmpty() ) {
            GPUdbLogger.error("Directory name cannot be null or empty");
            return false;
        }

        return Files.isDirectory( Paths.get( dirName ) );
    }

    /**
     * This method checks if a local file exists or not. If the file name is a
     * wildcard pattern it skips the check.
     *
     * @param fileName - String - Name of the file
     * @return - true if the file exists, false otherwise
     */
    public static boolean localFileExists(final String fileName) {
        if( ( fileName == null ) || fileName.trim().isEmpty() ) {
            GPUdbLogger.error("File name cannot be null or empty");
            return false;
        }

        // Extract just the file name from the path; if it is just the file name
        // then the 'File.separator' will not be found and an empty string will
        // be returned; in that case reinstate the value to 'fileName'.
        String extractedFileName = StringUtils.substringAfterLast( fileName, File.separator );
        if( extractedFileName.isEmpty() ) {
            extractedFileName = fileName;
        }

        // Input filenames could be wildcard forms as well; if they are then do
        // not check for existence; otherwise check if the file exists
        if( !( extractedFileName.contains("*") || extractedFileName.contains("?") || extractedFileName.contains( "**") ) ) {
            return Files.exists( Paths.get( fileName ) );
        }

        // Return 'true' here because if there are wildcards the file list will
        // be evaluated anyway and the case of missing files would never arise.
        return true;
    }

    public static String getKifsPathSeparator() {
        return KIFS_PATH_SEPARATOR;
    }

    /**
     * This method parses the list of file names passed in to the constructor
     * of the classes {@link com.gpudb.filesystem.upload.FileUploader},
     * {@link com.gpudb.filesystem.download.FileDownloader} which are derivatives
     * of this class. It resolves the file names, normalizes them and returns
     * a corresponding list of absolute paths which are used ny the methods of
     * this class.
     *
     * @param fileNames - List of file names passed in to the
     *                  constructor of the derivatives of this class.
     * @return - Returns a list of
     * {@link Triple} objects where the first element is the root of the file
     * path, the second the full path without the file name and the third just
     * the file name itself.
     */
    public static List<Triple<String, String, String>> parseFileNames(List<String> fileNames) {

        List<Triple<String, String, String>> listOfRootPathFilenameTriples = new ArrayList<>();

        for ( String file: fileNames ) {
            Path path = Paths.get( file );
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