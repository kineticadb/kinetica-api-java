package com.gpudb.filesystem.download;

import java.util.List;

/**
 * This class allows the user of the {@link com.gpudb.filesystem.GPUdbFileHandler}
 * class methods for download like -
 * {@link com.gpudb.filesystem.GPUdbFileHandler#download(String, String, DownloadOptions, FileDownloadListener)}
 * and {@link com.gpudb.filesystem.GPUdbFileHandler#download(List, String, DownloadOptions, FileDownloadListener)}
 * to specify the download options which could be applied to the current
 * download operation.
 *
 * Right now there is only option and that is whether to allow overwriting of
 * files existing in the local directory or not.
 *
 * The static method {@link #defaultOptions()} can be used to retrieve the
 * default options for download.
 *
 */
public class DownloadOptions {

    /**
     * Default constructor
     */
    public DownloadOptions() {
    }

    public static DownloadOptions defaultOptions() {
        DownloadOptions options = new DownloadOptions();
        options.setOverwriteExisting( true );

        return options;
    }
    /**
     *
     */
    private boolean overwriteExisting;


    public boolean isOverwriteExisting() {
        return overwriteExisting;
    }

    public void setOverwriteExisting(boolean overwriteExisting) {
        this.overwriteExisting = overwriteExisting;
    }

}