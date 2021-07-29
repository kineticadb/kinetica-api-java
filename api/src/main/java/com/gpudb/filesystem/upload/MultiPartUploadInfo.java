package com.gpudb.filesystem.upload;

import java.util.List;
import java.util.Map;

/**
 * This is an internal class and not meant to be used by the end users of the
 * filesystem API. The consequences of using this class directly in client code
 * is not guaranteed and maybe undesirable.

 * This class packages and mimics certain important attributes of a multi-part
 * upload operation. The attributes of this class are in one-one correspondence
 * to those specified in the {@link com.gpudb.GPUdb#uploadFiles(List, List, Map)}
 * endpoint.
 */
public class MultiPartUploadInfo {

    /**
     * This enum specifies the different operations/states that a multi-part
     * file upload could go through.
     */
    public enum MultiPartOperation {
        NONE("none"),
        INIT("init"),
        UPLOAD_PART("upload_part"),
        COMPLETE("complete"),
        CANCEL("cancel");

        private final String value;

        MultiPartOperation(String operation) {
            this.value = operation;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * This uniquely identifies a multi-part upload operation.
     */
    private String uuid;

    /**
     * The current value of the operation/state according to the enum
     * {@link MultiPartOperation}
     */
    private MultiPartOperation partOperation;

    /**
     * This identifies the part number for a particular part of the file being
     * uploaded.
     */
    private int uploadPartNumber;

    /**
     *
     */
    private long totalParts;

    /**
     * This is the name of the file being uploaded.
     */
    private String fileName;

    /**
     * Default constructor
     */
    public MultiPartUploadInfo() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public MultiPartOperation getPartOperation() {
        return partOperation;
    }

    public void setPartOperation(MultiPartOperation partOperation) {
        this.partOperation = partOperation;
    }

    public int getUploadPartNumber() {
        return uploadPartNumber;
    }

    public void setUploadPartNumber(int uploadPartNumber) {
        this.uploadPartNumber = uploadPartNumber;
    }

    public long getTotalParts() {
        return totalParts;
    }

    public void setTotalParts(long totalParts) {
        this.totalParts = totalParts;
    }



    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return "MultiPartUploadInfo{" + "uuid='" + uuid + '\'' + ", " +
                "partOperation=" + partOperation + ", " +
                "uploadPartNumber=" + uploadPartNumber + ", " +
                "totalParts=" + totalParts + ", fileName='" + fileName + '\'' + '}';
    }
}