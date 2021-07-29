package com.gpudb.filesystem.common;

import java.nio.file.Path;
import java.util.List;

/**
 * This is an internal class and not meant to be used by the end users of the
 * filesystem API. The consequences of using this class directly in client code
 * is not guaranteed and maybe undesirable.
 *
 * This class wraps the statistics about a local file like the name the
 * normalized absolute path and size. This is used internally by the class
 * {@literal FileOperation#getLocalFileInfo(List)}.
 *
 */
public class LocalFileInfo {

    /**
     *
     */
    private Path filePath;

    /**
     *
     */
    private String fileName;

    /**
     *
     */
    private Long fileSize;


    public Path getFilePath() {
        return filePath;
    }

    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * Default constructor
     */
    public LocalFileInfo() {
    }

}