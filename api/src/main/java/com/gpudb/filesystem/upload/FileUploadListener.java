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
 *          public void onFullFileUpload( Result result ) {
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
     * have been completed successfully.
     * <p>
     * The list contains a {@link Result} for every stage of the job, in the
     * order they were executed: the initiating stage, one entry per uploaded
     * part, and finally the completing stage. Only the completing stage
     * reports the uploaded file through
     * {@link Result#getFullFileNames()}; for the other stages that list is
     * empty. Use {@link Result#getUploadInfo()} to tell the stages apart.
     *
     * @param uploadResults - List of {@link Result} objects, one per stage.
     */
    void onMultiPartUploadComplete(List<Result> uploadResults);

    /**
     * This method is called when a multi-part upload was aborted because an
     * earlier stage of the job failed. It is never the result of an explicit
     * request to cancel, as no such operation is exposed.
     * <p>
     * The list contains a {@link Result} for every stage that completed before
     * the failure, followed by the cancelling stage; none of them report an
     * uploaded file, since nothing was committed. This callback is purely
     * informational — the failure is also reported to the caller of the
     * upload method as a thrown
     * {@link com.gpudb.GPUdbException}, which remains the authoritative error
     * channel.
     *
     * @param uploadResults - List of {@link Result} objects, one per stage.
     */
    default void onMultiPartUploadCancel(List<Result> uploadResults) {
        // Optional callback; ignored unless overridden.
    }

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