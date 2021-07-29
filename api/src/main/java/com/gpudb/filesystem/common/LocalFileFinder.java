package com.gpudb.filesystem.common;

import com.gpudb.GPUdbLogger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

/**
 * This is an internal class and not meant to be used by the end users of the
 * filesystem API. The consequences of using this class directly in client code
 * is not guaranteed and maybe undesirable.

 * This class handles the file searching operations on the local filesystem.
 * It handles standard glob patterns like "**", "*", "?" etc. and can do
 * the recursive searches on file patterns along the directory hierarchy
 * starting from a particular directory.
 *
 * This class extends the Java class {@link SimpleFileVisitor} and overrides
 * the file and the directory visit methods. It is essentially an implementation
 * of the visitor pattern.
 */
public class LocalFileFinder extends SimpleFileVisitor<Path> {

    private final String startingDir;
    private final PathMatcher matcher;
    private final boolean recursive;
    private final String remoteDirName;

    private int numMatches = 0;
    private final List<String> matchingFileNames = new ArrayList<>();
    private final List<String> remoteFileNames = new ArrayList<>();

    /**
     * Constructor for the class
     * @param startingDir - The search starts from this directory.
     * @param pattern - This is the pattern to search for, e.g., '*.csv', '**.csv'
     *                etc.
     * @param recursive - This is a flag indicating whether a recursive search
     * @param remoteDirName - The KIFS path
     */
    LocalFileFinder(String startingDir, String pattern, boolean recursive, String remoteDirName) {
        this.startingDir = startingDir;
        matcher = FileSystems.getDefault().getPathMatcher( "glob:" + pattern );
        this.recursive = recursive;
        this.remoteDirName = remoteDirName;
    }

    public List<String> getMatchingFileNames() {
        return matchingFileNames;
    }

    public List<String> getRemoteFileNames() {
        return remoteFileNames;
    }

    public int getNumMatches() {
        return numMatches;
    }

    /**
     * Compares the glob pattern against
     * the file or directory name.
     *
     * @param file - {@link Path}
     */
    void find(Path file) {
        Path fileParent = file.getParent();

        Path name = file.getFileName();

        int pathLen = file.getNameCount();

        if ( name != null && matcher.matches( name ) ) {

            //filter out special files like device files etc.
            if( Files.isRegularFile( file ) ) {
                matchingFileNames.add( file.normalize().toAbsolutePath().toString() );

                if( fileParent.toString().equals( startingDir )) {
                    remoteFileNames.add( remoteDirName + FileOperation.getKifsPathSeparator() + name.toString() );

                } else {
                    remoteFileNames.add( remoteDirName + FileOperation.getKifsPathSeparator() + file.subpath( 2, pathLen ).normalize().toString() );
                }

                numMatches++;
            }

        }
    }

    /**
     * This method is automatically called as a file is found.
     *
     * Invoke the pattern matching method on each file.
     * @param file - the {@link Path} representing the file.
     * @param attrs - the attributes {@link BasicFileAttributes} of the file.
     * @return - The {@link FileVisitResult} object used internally by the
     * FileWalker.
     */
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        find(file);
        return CONTINUE;
    }

    /**
     * This method is automatically called as a directory is found.
     *
     * Invoke the pattern matching method on each directory.
     * @param dir - The {@link Path} object representing the directory found
     * @param attrs - the attributes {@link BasicFileAttributes} of the
     *              directory.
     * @return - The {@link FileVisitResult} object used internally by the
     * FileWalker.
     */
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        // If recursive search has not been opted for skip traversing down the
        // sub tree of this directory.
        if( !( recursive || dir.toString().equals(startingDir) ) )
            return SKIP_SUBTREE;
        find(dir);
        return CONTINUE;
    }

    /**
     * This method is called in case there is an exception raised while
     * traversing a directory tree.
     *
     * @param file - the file Path object
     * @param exc - the IOException object
     * @return - {@link FileVisitResult} object
     */
    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        GPUdbLogger.error( exc.getMessage() );
        return CONTINUE;
    }

}
