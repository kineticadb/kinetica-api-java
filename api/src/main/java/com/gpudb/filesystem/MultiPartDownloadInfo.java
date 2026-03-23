package com.gpudb.filesystem;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * This class packages and mimics certain important attributes of a multi-part
 * download operation. The attributes of this class are in one-one correspondence
 * to those specified in the {@link com.gpudb.GPUdb#downloadFiles(List, List, List, Map)}
 * endpoint.
 *
 * <p>This class is read-only for API consumers - instances are created internally
 * and provided via Result callbacks. Users should not attempt to instantiate
 * this class directly.</p>
 */
public class MultiPartDownloadInfo {

    /**
     * Package-private constructor - prevents external instantiation.
     */
    MultiPartDownloadInfo() {
    }

    /**
     * The position to start reading the file from.
     */
    private long readOffset;

    /**
     * The number of bytes to read starting from {@link #readOffset}
     */
    private long readLength;

    /**
     * The part number out of {@link #totalParts}
     */
    private long downloadPartNumber;

    private ByteBuffer data;

    /**
     * The total number of parts
     */
    private long totalParts;

    public long getReadOffset() {
        return readOffset;
    }

    void setReadOffset(long readOffset) {
        this.readOffset = readOffset;
    }

    public long getReadLength() {
        return readLength;
    }

    void setReadLength(long readLength) {
        this.readLength = readLength;
    }

    public long getDownloadPartNumber() {
        return downloadPartNumber;
    }

    void setDownloadPartNumber(long downloadPartNumber) {
        this.downloadPartNumber = downloadPartNumber;
    }

    public long getTotalParts() {
        return totalParts;
    }

    void setTotalParts(long totalParts) {
        this.totalParts = totalParts;
    }

    public ByteBuffer getData() {
        return data;
    }

    void setData(ByteBuffer data) {
        this.data = data;
    }
}
