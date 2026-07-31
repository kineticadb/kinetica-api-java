package com.gpudb.filesystem.common;

import com.gpudb.filesystem.MultiPartDownloadInfo;
import com.gpudb.filesystem.MultiPartUploadInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * This class encapsulates information about the outcome of an upload or a
 * download operation. It is returned through the callback methods in the
 * interfaces {@link com.gpudb.filesystem.upload.FileUploadListener} and
 * {@link com.gpudb.filesystem.download.FileDownloadListener} for multi-part
 * operations. This is useful if the end user of the API is interested in
 * knowing the outcome of a large file upload/download.
 */
public class Result {

    /**
     * Default constructor
     */
    public Result() {
    }

    /**
     * Name of the file uploaded/downloaded
     */
    private String fileName;

    /**
     * Names of all file in case the current upload operation is full file upload
     * <p>
     * Only the stages of a multi-part operation that actually commit a file
     * populate this list, so it is initialized to an empty list rather than
     * left null: callbacks receive a {@link Result} for every stage of a job
     * and must be able to read this field on all of them without a null check.
     */
    private List<String> fullFileNames = new ArrayList<>();

    /**
     * Whether the upload/download is successful or not
     */
    private boolean successful;

    /**
     * The exception object if any.
     */
    private Exception exception;

    /**
     * Any error message if applicable
     */
    private String errorMessage;

    /**
     * This indicates whether it is an upload or a download
     * @see OpMode
     */
    private OpMode opMode;

    /**
     * Indicates whether this Result is for a multi-part operation or not
     */
    private boolean multiPart;

    /**
     * Carries information about the multi-part upload
     * @see MultiPartUploadInfo
     */
    private MultiPartUploadInfo uploadInfo;

    /**
     * Carries information about the multi-part download
     * @see MultiPartDownloadInfo
     */
    private MultiPartDownloadInfo downloadInfo;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<String> getFullFileNames() {
        return fullFileNames;
    }

    public void setFullFileNames(List<String> fullFileNames) {
        // Preserve the never-null invariant that callbacks rely on
        this.fullFileNames = ( fullFileNames == null ) ? new ArrayList<>() : fullFileNames;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public OpMode getOpMode() {
        return opMode;
    }

    public void setOpMode(OpMode opMode) {
        this.opMode = opMode;
    }

    public boolean isMultiPart() {
        return multiPart;
    }

    public void setMultiPart(boolean multiPart) {
        this.multiPart = multiPart;
    }

    public MultiPartUploadInfo getUploadInfo() {
        return uploadInfo;
    }

    public void setUploadInfo(MultiPartUploadInfo uploadInfo) {
        this.uploadInfo = uploadInfo;
    }

    public MultiPartDownloadInfo getDownloadInfo() {
        return downloadInfo;
    }

    public void setDownloadInfo(MultiPartDownloadInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
    }

    @Override
    public String toString() {
        return "Result{" + "fileName='" + fileName + '\'' + ", " +
                "successful=" + successful + ", " +
                "exception=" + exception + ", " +
                "errorMessage='" + errorMessage + '\'' + ", " +
                "opMode=" + opMode + ", multiPart=" + multiPart + ", " +
                "uploadInfo=" + uploadInfo + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Result result = (Result) o;
        return isSuccessful() == result.isSuccessful()
                && isMultiPart() == result.isMultiPart()
                && Objects.equals(getFileName(), result.getFileName())
                && Objects.equals(getErrorMessage(), result.getErrorMessage())
                && getOpMode() == result.getOpMode();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFileName(), isSuccessful(), getErrorMessage(), getOpMode(), isMultiPart());
    }

    /**
     * This is an implementation of the {@link Comparator} interface used to
     * sort the list of {@link Result} objects returned by background threads
     * downloading parts of a file. This is used internally by the file
     * download implementation.
     */
    public static class SortByDownloadPartNumber implements Comparator<Result> {

        @Override
        public int compare(Result result1, Result result2) {
            return Long.compare( result1.getDownloadInfo().getDownloadPartNumber(),
                    result2.getDownloadInfo().getDownloadPartNumber() );
        }
    }

}


