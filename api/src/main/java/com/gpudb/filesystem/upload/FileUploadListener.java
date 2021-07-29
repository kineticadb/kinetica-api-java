package com.gpudb.filesystem.upload;

import com.gpudb.filesystem.common.Result;

import java.util.List;

/**
 * This interface provides callback methods for -
 * a. complete multi-part file upload
 * b. a part of multi-part upload
 * c. a complete single shot upload of multiple files
 *
 * An example usage can be as given below :
 * <pre>
 *        private class FileUploadObserver implements FileUploadListener {
 *
 *          public void onMultiPartUploadComplete( List<Result> resultList ) {
 *
 *          }
 *
 *          public void onPartUpload( Result result ) {
 *
 *          }
 *
 *          public void onFullFileUpload( List<String> fileNames ) {
 *
 *          }
 *        }
 * </pre>
 *
 *
 * @see Result class
 */
public interface FileUploadListener {

    /**
     * This method is called when all the parts of a multi-part upload
     * have been completed.
     * @param uploadResults - List of {@link Result} objects.
     */
    void onMultiPartUploadComplete(List<Result> uploadResults);

    /**
     * This method is called whenever any part of a multi-part upload
     * has been completed. This could be useful for getting information about
     * how many parts out of the total number of parts have been completed.
     * @param result - a {@link Result} object
     */
    void onPartUpload( Result result );

    /**
     * This method is called when a single shot complete upload has been
     * completed.
     *
     * @param result - a list of file names
     */
    void onFullFileUpload(Result result);


}