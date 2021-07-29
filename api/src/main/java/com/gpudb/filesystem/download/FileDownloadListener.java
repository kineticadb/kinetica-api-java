package com.gpudb.filesystem.download;

import com.gpudb.filesystem.common.Result;

import java.util.List;

/**
 * This interface provides callback method for -
 * a. a complete single shot download of multiple files
 * b. a part/segment download of a multi-part download
 * c. one or more complete single part download
 */
public interface FileDownloadListener {

    /**
     * This method is called when all segments/parts of a multi-part download
     * have been completed.
     *
     * @param results
     */
    public void onMultiPartDownloadComplete(List<Result> results);

    /**
     * This method is called whenever any part of a multi-part download
     * has been completed. This could be useful for getting information about
     * how many parts out of the total number of parts have been completed.
     * @param result - a {@link Result} object
     */
    public void onPartDownload( Result result );

    /**
     * This method is called when a single shot full file download has been
     * completed.
     *
     * @param fileNames - a list of file names
     */
    public void onFullFileDownload(List<String> fileNames);

}