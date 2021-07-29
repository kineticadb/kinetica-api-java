package com.gpudb.filesystem.common;

import java.util.*;

/**
 *
 */
public class KifsFileInfo {

    @Override
    public String toString() {
        return "KifsFileInfo{" + "fileSize=" + fileSize + ", fileName='" + fileName + '\'' + ", createdBy='" + createdBy + '\'' + ", creationTime=" + creationTime + ", info=" + info + '}';
    }

    /**
     *
     */
    private long fileSize;

    /**
     * Full KIFS file name
     */
    private String fileName;

    /**
     *
     */
    private String createdBy;

    /**
     *
     */
    private long creationTime;

    /**
     *
     */
    private Map<String, String> info;


    /**
     * Default constructor
     */
    public KifsFileInfo() {
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public Map<String, String> getInfo() {
        return info;
    }

    public void setInfo(Map<String, String> info) {
        this.info = info;
    }

}