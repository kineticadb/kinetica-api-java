package com.gpudb.filesystem.download;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * This is an internal class and not meant to be used by the end users of the
 * filesystem API. The consequences of using this class directly in client code
 * is not guaranteed and maybe undesirable.

 * This class packages and mimics certain important attributes of a multi-part
 * download operation. The attributes of this class are in one-one correspondence
 * to those specified in the {@link com.gpudb.GPUdb#downloadFiles(List, List, List, Map)}
 * endpoint.
 */
public class MultiPartDownloadInfo {

    /**
     * Default constructor
     */
    public MultiPartDownloadInfo() {
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

    public void setReadOffset(long readOffset) {
        this.readOffset = readOffset;
    }

    public long getReadLength() {
        return readLength;
    }

    public void setReadLength(long readLength) {
        this.readLength = readLength;
    }

    public long getDownloadPartNumber() {
        return downloadPartNumber;
    }

    public void setDownloadPartNumber(long downloadPartNumber) {
        this.downloadPartNumber = downloadPartNumber;
    }

    public long getTotalParts() {
        return totalParts;
    }

    public void setTotalParts(long totalParts) {
        this.totalParts = totalParts;
    }

    public ByteBuffer getData() {
        return data;
    }

    public void setData(ByteBuffer data) {
        this.data = data;
    }
}