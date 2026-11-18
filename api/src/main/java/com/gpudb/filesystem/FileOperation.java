package com.gpudb.filesystem;

import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.GPUdbLogger;
import com.gpudb.filesystem.common.KifsFileInfo;
import com.gpudb.filesystem.common.OpMode;
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
 * <p>This is the base class from which the classes {@link FileUploader}
 * and {@link FileDownloader} are derived.
 * The purpose of this class is to model certain basic functions like
 * searching directories for specific file patterns, retrieving file attributes
 * and determining transfer strategies (single vs multi-part).
 */
class FileOperation {

    protected final OpMode opMode;
    protected final GPUdb db;

    /**
     * Indicates whether the search for files is to be recursive through a local
     * directory hierarchy.  It applies to patterns and to directories; a file
     * given by name is that one file.
     */
    protected final boolean recursive;

    /**
     * The list of source file names/patterns to be processed.
     */
    protected List<String> fileNames;

    /**
     * The KiFS paths the files are uploaded to, in the order found.  Used by
     * the {@link FileIngestor} to insert records from the uploaded files into
     * the database, and to delete them afterward.
     */
    protected Set<String> remoteNamesOfFilesUploaded;

    /**
     * The local files, among those to upload, that would be uploaded to the
     * KiFS path of another of them, and so overwrite it.
     */
    protected Set<String> filesWithDuplicateNames;

    /**
     * The first local file found for each KiFS path in
     * {@link #remoteNamesOfFilesUploaded}.
     */
    private final Map<String, String> localFileOfRemoteName = new HashMap<>();

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
    FileOperation(final GPUdb db,
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
        this.remoteNamesOfFilesUploaded = new LinkedHashSet<>();
        this.filesWithDuplicateNames = new LinkedHashSet<>();
        this.multiPartList = new ArrayList<>();
        this.multiPartRemoteFileNames = new ArrayList<>();
        this.fullFileList = new ArrayList<>();
        this.fullFileBatchManager = new FullFileBatchManager(this.fileHandlerOptions);

        decideMultiPart();
    }

    /**
     * This method returns the KiFS paths the files are uploaded to:  the target
     * directory, then each file's path under the directory or pattern it was
     * found by.  This is used by the {@link FileIngestor#ingestFromFiles()}
     * method to prepare the list of file names to be passed on to the
     * {@link GPUdb#insertRecordsFromFiles(com.gpudb.protocol.InsertRecordsFromFilesRequest)}
     * endpoint.
     *
     * @return  A set of KiFS paths, without the 'kifs://' prefix.
     */
    Set<String> getRemoteNamesOfFilesUploaded() {
        return this.remoteNamesOfFilesUploaded;
    }

    /**
     * Returns the local files to upload that would be uploaded to the same
     * KiFS path as another of them, each with the first file of that path.
     *
     * @return  The paths of the local files with duplicate KiFS paths; empty
     *        if every KiFS path is unique.
     */
    Set<String> getFilesWithDuplicateNames() {
        return this.filesWithDuplicateNames;
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
    private void decideMultiPartUpload() throws GPUdbException {
        // The names and patterns given that match no local file, and those
        // whose resolution failed, each with its failure
        List<String> unmatched = new ArrayList<>();
        List<String> unreadable = new ArrayList<>();

        // Parse inputs into (Root, BasePath, FileName/Pattern)
        List<Triple<String, String, String>> parsedFileNames = parseFileNames(this.fileNames);

        for (int n = 0; n < parsedFileNames.size(); n++) {
            Triple<String, String, String> triple = parsedFileNames.get(n);
            String searchBaseDir = triple.getMiddle();
            String patternOrName = triple.getRight();

            List<String> localResolvedFiles = new ArrayList<>();
            List<String> remoteResolvedFiles = new ArrayList<>();

            String explicitPath = searchBaseDir + File.separator + patternOrName;

            // A name holding a glob character (*, ?, {, [, and the escape where
            // that is not the separator) is a pattern -- unless a file or
            // directory of exactly that name exists.  "[" and "{" are legal
            // in file names, and a name that exists is what the caller meant,
            // not the class of names it could also be read as:  given
            // "q[1].csv", the file of that name, never q1.csv.
            boolean isPattern = isGlobPattern(patternOrName) && !literalExists(explicitPath);

            try {
                if (isPattern) {
                    // Perform a filesystem search using Java NIO; through the
                    // subdirectories too, if recursive
                    Pair<List<String>, List<String>> searchResults = searchLocalDirectories(searchBaseDir, patternOrName);
                    localResolvedFiles.addAll(searchResults.getLeft());
                    remoteResolvedFiles.addAll(searchResults.getRight());

                    // A pattern that matches nothing is an error, as a file
                    // that does not exist is, not an upload of nothing
                    if (localResolvedFiles.isEmpty())
                        unmatched.add(this.fileNames.get(n));
                } else if (Files.isDirectory(Paths.get(explicitPath))) {
                    // A directory:  every file in it; and in its
                    // subdirectories too, if recursive
                    Pair<List<String>, List<String>> searchResults = searchLocalDirectories(explicitPath, "*");
                    localResolvedFiles.addAll(searchResults.getLeft());
                    remoteResolvedFiles.addAll(searchResults.getRight());

                    // A directory holding no file to upload is refused as a
                    // pattern matching none is, not taken as an upload of nothing
                    if (localResolvedFiles.isEmpty())
                        unmatched.add(this.fileNames.get(n));
                } else {
                    // Simple file case - Explicit single file.  It is taken
                    // alone, whether recursive or not:  searching its directory
                    // tree for the name would also upload any file of the same
                    // name in a subdirectory.
                    String fullPath = explicitPath;
                    if (localFileExists(fullPath)) {
                        localResolvedFiles.add(fullPath);
                        // The name keeps any directory elements under the base
                        // the parse left it (a directory named with a glob
                        // character), written in the KiFS separator
                        remoteResolvedFiles.add(this.dirName + GPUdbFileHandler.KIFS_PATH_SEPARATOR
                                                + patternOrName.replace(File.separator, GPUdbFileHandler.KIFS_PATH_SEPARATOR));
                    } else {
                        unmatched.add(this.fileNames.get(n));
                    }
                }
            } catch (IOException | java.io.UncheckedIOException e) {
                // One entry's failure ends neither the resolution of the others
                // nor the request silently:  it is refused below, with every
                // other problem found.  A walk reports a directory it cannot
                // read as the unchecked form.
                Throwable cause = (e instanceof java.io.UncheckedIOException) ? e.getCause() : e;
                unreadable.add(this.fileNames.get(n) + " (" + cause + ")");
                continue;
            }

            if (!localResolvedFiles.isEmpty()) {
                sortFilesIntoFullAndMultipartLists(localResolvedFiles, remoteResolvedFiles);
            }
        }

        // Refused here, before anything is uploaded
        if (!unmatched.isEmpty() || !unreadable.isEmpty()) {
            StringBuilder reason = new StringBuilder();
            if (!unmatched.isEmpty())
                reason.append( "No local file matches " ).append( unmatched );
            if (!unreadable.isEmpty()) {
                if (reason.length() > 0)
                    reason.append( "; " );
                reason.append( "Local files could not be resolved: " ).append( unreadable );
            }
            throw new GPUdbException( reason + "; nothing was uploaded" );
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

            // Track the KiFS path for the Ingestor
            if (!this.remoteNamesOfFilesUploaded.add(remoteFileName)) {
                String first = this.localFileOfRemoteName.get(remoteFileName);
                if (isSameLocalFile(localFileName, first)) {
                    // The same local file given twice, however each is
                    // spelled:  uploaded once
                    continue;
                }
                // Another file goes to this KiFS path; name them both, for
                // the refusal made before anything is uploaded
                this.filesWithDuplicateNames.add(first);
                this.filesWithDuplicateNames.add(localFileName);
            } else {
                this.localFileOfRemoteName.put(remoteFileName, localFileName);
            }

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
     * Walks a local directory tree, as {@link Files#walk(Path, int, java.nio.file.FileVisitOption...)}
     * does.  The one place the local filesystem is read for an upload, so
     * that a test can make it fail.
     */
    protected java.util.stream.Stream<Path> walkLocal(Path start, int maxDepth) throws IOException {
        return Files.walk(start, maxDepth);
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
        String globPattern = getGlobPattern(pattern, matchFullPath, startPath);

        @SuppressWarnings("resource")
		final PathMatcher matcher = FileSystems.getDefault().getPathMatcher(globPattern);

        // Max depth depends on recursive flag or if pattern contains **
        int maxDepth = (this.recursive || pattern.contains("**")) ? Integer.MAX_VALUE : 1;

        try (java.util.stream.Stream<Path> stream = walkLocal(startPath, maxDepth)) {
            stream.filter(path -> !Files.isDirectory(path)) // Files only
                .filter(path -> {
                    // CRITICAL : Match against full path or filename depending on pattern type
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
                    String remoteFull = ( this.dirName  != null && !this.dirName.isEmpty() ) ? this.dirName + GPUdbFileHandler.KIFS_PATH_SEPARATOR + remoteRelative : remoteRelative;

                    remotePaths.add(remoteFull);
                });
        }

        return Pair.of(localPaths, remotePaths);
    }

    private static String getGlobPattern(String pattern, boolean matchFullPath, Path startPath) {
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
        return globPattern;
    }

    /**
     * Checks the given string for the presence of glob characters.
     *
     * <p>A backslash counts only where it is not the platform's path
     * separator.  In the glob syntax it escapes the character after it, but
     * on Windows it separates path elements, and reading it as an escape
     * there would make every absolute path a pattern.
     *
     * @param s String to check for glob characters.
     * @return  Whether or not glob characters exist in the given string.
     */
    private static boolean isGlobPattern(String s) {
        return StringUtils.containsAny(s, '*', '?', '{', '[')
               || ((File.separatorChar != '\\') && StringUtils.contains(s, '\\'));
    }

    /**
     * Checks whether a file or directory of exactly the given name exists.
     * A name the platform cannot form at all -- one holding a character its
     * file names may not -- names nothing.
     */
    private static boolean literalExists(String path) {
        try {
            return Files.exists(Paths.get(path));
        } catch (java.nio.file.InvalidPathException e) {
            return false;
        }
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
    static List<Triple<String, String, String>> parseFileNames(List<String> fileNamesToParse) {
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
     * Checks whether two local paths name the same file, however each is
     * written.
     */
    private static boolean isSameLocalFile(final String one, final String other) {
        return Paths.get(one).toAbsolutePath().normalize()
                .equals(Paths.get(other).toAbsolutePath().normalize());
    }

    /**
     * Checks if a local directory exists or not.
     *
     * @param localDirName  Name of the local directory to check for.
     * @return  True if the directory exists, and false if it doesn't exist or
     *        if the {@code localDirName} is null or empty.
     */
    static boolean localDirExists(final String localDirName) {
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
    static boolean localFileExists(final String localFileName) {
        if (StringUtils.isBlank(localFileName)) return false;

        // If it looks like a pattern, assume true and let the resolver handle
        // it:  a pattern that matches no file is refused there
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
    static String getKifsPathSeparator() {
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
