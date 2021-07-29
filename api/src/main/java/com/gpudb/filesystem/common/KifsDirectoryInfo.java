package com.gpudb.filesystem.common;

import java.util.*;

/**
 *
 */
public class KifsDirectoryInfo {

    @Override
    public String toString() {
        return "KifsDirectoryInfo{" + "kifsPath='" + kifsPath + '\'' + ", createdBy='" + createdBy + '\'' + ", creationTime=" + creationTime + ", permission='" + permission + '\'' + ", info=" + info + '}';
    }

    /**
     * Default constructor
     */
    public KifsDirectoryInfo() {
    }

    /**
     *
     */
    public String kifsPath;

    /**
     *
     */
    public String createdBy;

    /**
     *
     */
    public long creationTime;

    /**
     *
     */
    public String permission;

    /**
     *
     */
    public Map<String, String> info;

    public String getKifsPath() {
        return kifsPath;
    }

    public void setKifsPath(String kifsPath) {
        this.kifsPath = kifsPath;
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

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public Map<String, String> getInfo() {
        return info;
    }

    public void setInfo(Map<String, String> info) {
        this.info = info;
    }
}