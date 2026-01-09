package com.gpudb.filesystem.common;

import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.GPUdbLogger;
import com.gpudb.filesystem.GPUdbFileHandler;
import com.gpudb.filesystem.ingest.FileIngestor;
import com.gpudb.protocol.ShowFilesResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * This is an internal class and not meant to be used by the end users of the
 * {@code filesystem} API. The consequences of using this class directly in
 * client code is not guaranteed and maybe undesirable.
 *
 * <p>This is the base class from which the classes {@link com.gpudb.filesystem.upload.FileUploader}
 * and {@link com.gpudb.filesystem.download.FileDownloader} are derived.
 * The purpose of this class is to model certain basic functions like
 * searching directories for specific file patterns, retrieving file attributes
 * and determining transfer strategies (single vs multi-part).
 */
public class FileOperation {

    protected final OpMode opMode;
    protected final GPUdb db;

    /**
     * Indicates whether the search for files is to be recursive through a local
     * directory hierarchy.
     */
    protected final boolean recursive;

    /**
     * The list of source file names/patterns to be processed.
     */
    protected List<String> fileNames;

    /**
     * The list of files uploaded, used by the {@link FileIngestor} to insert
     * records from the uploaded files into the database.
     */
    protected Set<String> namesOfFilesUploaded;

    /**
     * Target directory name (KIFS for upload, local for download).
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
     *         validations.
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

        // Initialize collections immediately to avoid null checks later
        this.namesOfFilesUploaded = new LinkedHashSet<>();
        this.multiPartList = new ArrayList<>();
        this.multiPartRemoteFileNames = new ArrayList<>();
        this.fullFileList = new ArrayList<>();
        this.fullFileBatchManager = new FullFileBatchManager(this.fileHandlerOptions);

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
     * Resolves file names and categorizes them into single-part or multi-part transfers.
     */
    protected void decideMultiPart() throws GPUdbException {
        // Clear previous state if this instance is reused (though unusual for this class design)
        this.fullFileList.clear();
        this.multiPartList.clear();
        this.multiPartRemoteFileNames.clear();
        this.fullFileBatchManager.clearBatches();

        if (this.opMode == OpMode.UPLOAD) {
            decideMultiPartUpload();
        } else {
            decideMultiPartDownload();
        }
    }

    /**
     * Processes upload requests:  parses local file patterns (globs),
     * resolves them to actual files, checks sizes, and buckets them for full or
     * multi-part upload.
     */
    private void decideMultiPartUpload() {
        try {
            // Parse inputs into (Root, BasePath, FileName/Pattern)
            List<Triple<String, String, String>> parsedFileNames = parseFileNames(this.fileNames);

            for (Triple<String, String, String> triple : parsedFileNames) {
                String searchBaseDir = triple.getMiddle();
                String patternOrName = triple.getRight();

                List<String> localResolvedFiles = new ArrayList<>();
                List<String> remoteResolvedFiles = new ArrayList<>();

                // Check if the filename contains glob characters (*, ?, {}, [], etc.)
                boolean isPattern = isGlobPattern(patternOrName);

                if (isPattern || this.recursive) {
                    // Perform a filesystem search using Java NIO
                    Pair<List<String>, List<String>> searchResults = searchLocalDirectories(searchBaseDir, patternOrName);
                    localResolvedFiles.addAll(searchResults.getLeft());
                    remoteResolvedFiles.addAll(searchResults.getRight());
                } else {
                    // Simple file case - Explicit single file
                    String fullPath = searchBaseDir + File.separator + patternOrName;
                    if (localFileExists(fullPath)) {
                        localResolvedFiles.add(fullPath);
                        remoteResolvedFiles.add(this.dirName + GPUdbFileHandler.KIFS_PATH_SEPARATOR + patternOrName);
                    } else {
                        GPUdbLogger.warn("Local file not found, skipping: " + fullPath);
                    }
                }

                if (!localResolvedFiles.isEmpty()) {
                    sortFilesIntoFullAndMultipartLists(localResolvedFiles, remoteResolvedFiles);
                }
            }
        } catch (IOException e) {
            GPUdbLogger.error("Error resolving local files for upload: " + e.getMessage());
        }
    }

    /**
     * Buckets files to be downloaded into full or multi-part groups, based on KIFS file sizes.
     */
    private void decideMultiPartDownload() throws GPUdbException {
        // Note: KIFS currently treats wildcards differently (server-side),
        // so we assume the input list contains valid KIFS paths or patterns handled by showFiles.

        // We use the raw fileNames here because showFiles handles KIFS paths directly
        for (String kifsPath : this.fileNames) {

            ShowFilesResponse sfResp = this.db.showFiles(Collections.singletonList(kifsPath), new HashMap<>());

            if (sfResp != null && sfResp.getFileNames() != null) {
                for (int i = 0; i < sfResp.getFileNames().size(); i++) {
                    String name = sfResp.getFileNames().get(i);
                    Long size = sfResp.getSizes().get(i);

                    if (size > this.fileHandlerOptions.getFileSizeToSplit()) {
                        this.multiPartList.add(name);
                    } else {
                        this.fullFileList.add(name);
                    }
                }
            }
        }
    }

    /**
     * Buckets files to be uploaded into full or multi-part groups, based on local file sizes.
     *
     * @param fileList  A list of source file names to triage by size.
     * @param remoteFileList  A list of target file names for the given sources.
     */
    protected void sortFilesIntoFullAndMultipartLists(List<String> fileList, List<String> remoteFileList) {
        for (int i = 0; i < fileList.size(); i++) {
            String localFileName = fileList.get(i);
            String remoteFileName = remoteFileList.get(i);

            // Track simple name for Ingestor
            String simpleName = Paths.get(localFileName).getFileName().toString();
            this.namesOfFilesUploaded.add(simpleName);

            try {
                long fileSize = Files.size(Paths.get(localFileName));

                if (fileSize > this.fileHandlerOptions.getFileSizeToSplit()) {
                    this.multiPartList.add(localFileName);
                    this.multiPartRemoteFileNames.add(remoteFileName);
                } else {
                    this.fullFileBatchManager.addFile(localFileName, remoteFileName, fileSize);
                }
            } catch (IOException e) {
                GPUdbLogger.error(String.format("Error getting attributes for file <%s>: %s", localFileName, e.getMessage()));
            }
        }
    }

    /**
     * Searches the local filesystem starting from a specified base directory using standard
     * Java NIO glob patterns. This method resolves local file paths and calculates their
     * corresponding target remote paths, preserving the relative directory structure found
     * during the search.
     *
     * <p>This method leverages {@link java.nio.file.FileSystem#getPathMatcher(String)} and
     * supports the full standard glob syntax.</p>
     *
     * <h3>Supported Glob Patterns</h3>
     * <ul>
     * <li><b>{@code *.java}</b> - Matches any file ending with the specific extension.</li>
     * <li><b>{@code *}</b> - Matches any number of characters (e.g., {@code *.csv} matches all CSV files).</li>
     * <li><b>{@code **}</b> - Matches any number of directories. Used implicitly if the recursive
     * flag is set, but can be used explicitly (e.g., {@code **\/test/*.xml}).</li>
     * <li><b>{@code ?}</b> - Matches exactly one character (e.g., {@code data_?.txt} matches
     * {@code data_1.txt} but not {@code data_10.txt}).</li>
     * <li><b>{@code {sun,moon,stars}}</b> - Matches any of the comma-separated subpatterns
     * (e.g., {@code *.{jpg,png}} matches both JPG and PNG files).</li>
     * <li><b>{@code [A-Z]}</b> - Matches any uppercase character (e.g., {@code grade_[A-F].txt}).</li>
     * <li><b>{@code [0-9]}</b> - Matches any digit (e.g., {@code file[0-9].log}).</li>
     * </ul>
     *
     * <h3>Remote Path Calculation</h3>
     * For every file found, the method calculates a "Remote Path" to ensure the directory
     * structure is mirrored on the destination (KIFS).
     * <pre>
     * Logic: TargetRemoteDir + (FoundFilePath - BaseSearchDir)
     * Example:
     * Base Dir:          /data/logs
     * Found File:        /data/logs/2023/jan/access.log
     * Target Remote Dir: /kifs/backup
     * Result:            /kifs/backup/2023/jan/access.log
     * </pre>
     *
     * @param baseDir The absolute or relative path to the local directory where the search begins.
     * @param pattern The glob pattern to match against file names (e.g., {@code "*.csv"}, {@code "data_2023_*.{json,xml}"}).
     * @return A {@link Pair} where:
     * <ul>
     * <li><b>Left:</b> A list of absolute local file paths found.</li>
     * <li><b>Right:</b> A list of corresponding full remote target paths.</li>
     * </ul>
     * @throws IOException If an I/O error occurs during the file walk (e.g., permission denied).
     * @see java.nio.file.FileSystem#getPathMatcher(String)
     */
    protected Pair<List<String>, List<String>> searchLocalDirectories(String baseDir, String pattern) throws IOException {
        List<String> localPaths = new ArrayList<>();
        List<String> remotePaths = new ArrayList<>();

        Path startPath = Paths.get(baseDir).normalize().toAbsolutePath();
        if (!Files.exists(startPath) || !Files.isDirectory(startPath)) {
            GPUdbLogger.error("Directory does not exist: " + baseDir);
            return Pair.of(localPaths, remotePaths);
        }

        // Determine if we need to match against the Full Path or just the Name
        boolean matchFullPath = pattern.contains(File.separator) || pattern.contains("/");
        String globPattern;

        if (matchFullPath) {
            // Pattern includes directory structure (e.g. **/*.xml or sub/*.txt)
            // We must construct a glob for the absolute path to match correctly
            globPattern = "glob:" + startPath + File.separator + pattern;

            // Handle Windows backslashes in glob syntax if necessary
            if (File.separatorChar == '\\') {
                globPattern = globPattern.replace("\\", "\\\\");
            }
        } else {
            // Simple pattern (e.g. *.csv) - match against filename only
            globPattern = "glob:" + pattern;
        }

        @SuppressWarnings("resource")
		final PathMatcher matcher = FileSystems.getDefault().getPathMatcher(globPattern);

        // Max depth depends on recursive flag or if pattern contains **
        int maxDepth = (this.recursive || pattern.contains("**")) ? Integer.MAX_VALUE : 1;

        try (java.util.stream.Stream<Path> stream = Files.walk(startPath, maxDepth)) {
            stream.filter(path -> !Files.isDirectory(path)) // Files only
                    .filter(path -> {
                        // CRITICAL FIX: Match against full path or filename depending on pattern type
                        if (matchFullPath) {
                            return matcher.matches(path.normalize().toAbsolutePath());
                        }
                        return matcher.matches(path.getFileName());
                    })
                    .forEach(path -> {
                        localPaths.add(path.toString());

                        // Calculate Remote Path: TargetDir + (File Relative to Start)
                        Path relativePath = startPath.relativize(path);

                        String remoteRelative = relativePath.toString().replace(File.separator, GPUdbFileHandler.KIFS_PATH_SEPARATOR);
                        String remoteFull = this.dirName + GPUdbFileHandler.KIFS_PATH_SEPARATOR + remoteRelative;

                        remotePaths.add(remoteFull);
                    });
        }

        return Pair.of(localPaths, remotePaths);
    }

    /**
     * Checks the given string for the presence of glob characters.
     * 
     * @param s String to check for glob characters.
     * @return  Whether or not glob characters exist in the given string.
     */
    private static boolean isGlobPattern(String s) {
        return StringUtils.containsAny(s, '*', '?', '{', '[', '\\');
    }

    /**
     * Parses the given file paths into structured path components. It resolves
     * the file names, normalizes them and returns a corresponding list of absolute
     * paths.
     *
     * @param fileNamesToParse  List of file names to parse.
     * @return  A list of {@link Triple} objects where the first element is the
     *        root of the file path, the second the full path without the file
     *        name and the third just the file name itself.
     */
    public static List<Triple<String, String, String>> parseFileNames(List<String> fileNamesToParse) {
        List<Triple<String, String, String>> listOfRootPathFilenameTriples = new ArrayList<>();

        for (String rawPath : fileNamesToParse) {
            String root = "";
            String baseDir;
            String pattern;

            // Check if path contains glob characters
            if (isGlobPattern(rawPath)) {
                // Split logic: Move up the tree until we find a directory without wildcards
                File f = new File(rawPath);
                String buildingPattern = f.getName();
                File parent = f.getParentFile();

                while (parent != null && isGlobPattern(parent.getPath())) {
                    // Prepend the current parent name to the pattern
                    buildingPattern = parent.getName() + File.separator + buildingPattern;
                    parent = parent.getParentFile();
                }

                if (parent == null) {
                    // Entire path was a pattern or relative
                    baseDir = ".";
                    pattern = rawPath;
                } else {
                    baseDir = parent.getPath();
                    pattern = buildingPattern;
                    // Extract root if available
                    Path parentPath = parent.toPath();
                    if (parentPath.getRoot() != null) {
                        root = parentPath.getRoot().toString();
                    }
                }
            } else {
                // Standard logic for normal paths
                Path path = Paths.get(rawPath);
                Path parentPath = path.getParent();

                if (parentPath == null) {
                    baseDir = ".";
                    root = "";
                } else {
                    baseDir = parentPath.toString();
                    if (parentPath.getRoot() != null) {
                        root = parentPath.getRoot().toString();
                    }
                }
                pattern = path.getFileName().toString();
            }

            listOfRootPathFilenameTriples.add(Triple.of(root, baseDir, pattern));
        }

        return listOfRootPathFilenameTriples;
    }

    // --- Utility Methods ---

    /**
     * Checks if a local directory exists or not.
     * 
     * @param localDirName  Name of the local directory to check for.
     * @return  True if the directory exists, and false if it doesn't exist or
     *        if the {@code localDirName} is null or empty.
     */
    public static boolean localDirExists(final String localDirName) {
        if (StringUtils.isBlank(localDirName)) return false;
        return Files.isDirectory(Paths.get(localDirName));
    }

    /**
     * Checks if a local file exists or not. If the file name is a
     * wildcard pattern, it skips the check.
     *
     * @param localFileName  Name of the file.
     * @return  True if the file exists, false otherwise.
     */
    public static boolean localFileExists(final String localFileName) {
        if (StringUtils.isBlank(localFileName)) return false;

        // If it looks like a pattern, assume true and let the resolver handle it
        if (StringUtils.containsAny(localFileName, '*', '?', '{', '[')) {
            return true;
        }
        return Files.exists(Paths.get(localFileName));
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

    // --- Data Classes ---

    /**
     * Retrieves the file stats for the files residing in KIFS.
     *
     * @param path  Name of the KIFS file or directory of files to retrieve info on.
     * @return  List of {@link KifsFileInfo} objects for the KiFS file(s) found.
     * @throws GPUdbException  If the KiFS lookup fails.
     */
    protected List<KifsFileInfo> getFileInfoFromServer(String path) throws GPUdbException {
        List<KifsFileInfo> resp = new ArrayList<>();
        ShowFilesResponse sfResp = this.db.showFiles(Collections.singletonList(path), new HashMap<>());

        if (sfResp.getFileNames() != null) {
            for (int i = 0; i < sfResp.getFileNames().size(); i++) {
                KifsFileInfo fileInfo = new KifsFileInfo();
                fileInfo.setFileName(sfResp.getFileNames().get(i));
                fileInfo.setFileSize(sfResp.getSizes().get(i));
                fileInfo.setCreationTime(sfResp.getCreationTimes().get(i));
                fileInfo.setCreatedBy(sfResp.getUsers().get(i));
                resp.add(fileInfo);
            }
        }
        return resp;
    }
}