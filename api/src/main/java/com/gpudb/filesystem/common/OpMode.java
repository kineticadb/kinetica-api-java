package com.gpudb.filesystem.common;

/**
 * This is an enum indicating whether a particular operation is an upload
 * or a download
 *
 * This is an enum used by the class
 * {@link Result} which is used by the interfaces
 * {@link com.gpudb.filesystem.upload.FileUploadListener} and
 * {@link com.gpudb.filesystem.download.FileDownloadListener}
 *
 */
public enum OpMode {
    UPLOAD,
    DOWNLOAD
}