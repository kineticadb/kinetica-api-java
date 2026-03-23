package com.gpudb.filesystem;

import java.util.List;
import java.util.Map;

/**
 * This class packages and mimics certain important attributes of a multi-part
 * upload operation. The attributes of this class are in one-one correspondence
 * to those specified in the {@link com.gpudb.GPUdb#uploadFiles(List, List, Map)}
 * endpoint.
 *
 * <p>This class is read-only for API consumers - instances are created internally
 * and provided via Result callbacks. Users should not attempt to instantiate
 * this class directly.</p>
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
            return this.value;
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
     * Package-private constructor - prevents external instantiation.
     */
    MultiPartUploadInfo() {
    }

    public String getUuid() {
        return this.uuid;
    }

    void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public MultiPartOperation getPartOperation() {
        return this.partOperation;
    }

    void setPartOperation(MultiPartOperation partOperation) {
        this.partOperation = partOperation;
    }

    public int getUploadPartNumber() {
        return this.uploadPartNumber;
    }

    void setUploadPartNumber(int uploadPartNumber) {
        this.uploadPartNumber = uploadPartNumber;
    }

    public long getTotalParts() {
        return this.totalParts;
    }

    void setTotalParts(long totalParts) {
        this.totalParts = totalParts;
    }



    public String getFileName() {
        return this.fileName;
    }

    void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return
                "MultiPartUploadInfo{" +
                    "uuid='" + this.uuid + '\'' + ", " +
                    "partOperation=" + this.partOperation + ", " +
                    "uploadPartNumber=" + this.uploadPartNumber + ", " +
                    "totalParts=" + this.totalParts + ", " +
                    "fileName='" + this.fileName + '\'' +
                '}';
    }
}
