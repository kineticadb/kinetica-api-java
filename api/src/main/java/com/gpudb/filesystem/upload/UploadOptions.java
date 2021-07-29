package com.gpudb.filesystem.upload;

import java.util.List;

/**
 * This class encapsulates the options that the users of the API exposed
 * by {@link com.gpudb.filesystem.GPUdbFileHandler} class can use to set
 * the options for the methods
 * {@link com.gpudb.filesystem.GPUdbFileHandler#upload(List, String, UploadOptions, FileUploadListener)}
 * and {@link com.gpudb.filesystem.GPUdbFileHandler#upload(String, String, UploadOptions, FileUploadListener)}
 *
 * The static method {@link #defaultOptions()} can be used to retrieve the
 * default options for upload.
 */
public class UploadOptions {

    /**
     * This option indicates whether the search in the local directory is
     * a recursive search along a directory hierarchy or not. This is only
     * applicable if the file names passed to the upload methods of the class
     * {@link com.gpudb.filesystem.GPUdbFileHandler} contain wildcards like
     * '**.txt', '<some_path>/</>*.csv' etc.
     */
    private final boolean recursive;

    /**
     * This is the 'ttl' value which is passed in as it is to the endpoint
     * if set.
     */
    private final int ttl;

    public static UploadOptions defaultOptions() {
        return new UploadOptions(true, -1);
    }

    /**
     * Default constructor
     *
     * @param recursive - indicates whether file search is recursive or not
     * @param ttl - indicates a ttl value for the uploaded files
     */
    public UploadOptions(boolean recursive, int ttl) {
        this.recursive = recursive;
        this.ttl = ttl;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public int getTtl() {
        return ttl;
    }
}