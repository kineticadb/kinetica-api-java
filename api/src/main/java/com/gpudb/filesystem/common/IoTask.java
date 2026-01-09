package com.gpudb.filesystem.common;

import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.GPUdbLogger;
import com.gpudb.filesystem.download.DownloadOptions;
import com.gpudb.filesystem.download.MultiPartDownloadInfo;
import com.gpudb.filesystem.upload.MultiPartUploadInfo;
import com.gpudb.filesystem.upload.UploadOptions;
import com.gpudb.protocol.DownloadFilesResponse;
import com.gpudb.protocol.UploadFilesRequest;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;



/**
 * This is an internal class and not meant to be used by the end users of the
 * {@code filesystem} API. The consequences of using this class directly in
 * client code is not guaranteed and maybe undesirable.

 * This class is only used for multi-part upload and download and an instance
 * of this class is created only by {@link com.gpudb.filesystem.upload.FileUploader}
 * and {@link com.gpudb.filesystem.download.FileDownloader} classes.
 *
 * This class models a single background tasks running in a thread.
 * It implements the {@link Callable} interface and the overridden
 * {@link #call()} method is the one that executes the main logic in this
 * class. Depending on the value of the instance variable {@link #opMode} it
 * calls either the {@link #upload()} or the {@link #download()} method.
 */
public class IoTask implements Callable<Result> {

    private final GPUdb db;
    private final OpMode opMode;
    private final UploadOptions uploadOptions;

    @SuppressWarnings("unused")
    private final DownloadOptions downloadOptions;

    /**
     * Identifier for the sequential part number for a multi-part upload.
     */
    private final long taskNumber;

    /**
     * Name of the file to upload/download.
     */
    private final String fileName;
    private MultiPartUploadInfo multiPartUploadInfo;
    private MultiPartDownloadInfo multiPartDownloadInfo;

    /**
     * This is the data to be uploaded for this part of the multi-part upload.
     */
    private final ByteBuffer dataBytes;

    /**
     * Constructs a task for uploading a part of a multi-part upload.
     * 
     * @param db  The {@link GPUdb} instance used to access KiFS.
     * @param fileName  Name of the file to be uploaded.
     * @param multiPartUploadInfo  Configuration information about this part of
     *        the upload as well as the overall upload itself.
     * @param options  Options to use during the upload.
     * @param taskNumber  Sequence number of this part of the overall upload.
     * @param dataBytes  Data to upload for this part, in a binary format.
     */
    public IoTask(GPUdb db,
                  String fileName,
                  MultiPartUploadInfo multiPartUploadInfo,
                  UploadOptions options,
                  long taskNumber,
                  ByteBuffer dataBytes) {
        this.db = db;
        this.opMode = OpMode.UPLOAD;
        this.fileName = fileName;
        this.multiPartUploadInfo = multiPartUploadInfo;
        this.multiPartDownloadInfo = null;
        this.uploadOptions = options;
        this.downloadOptions = null;
        this.taskNumber = taskNumber;
        this.dataBytes = dataBytes;
    }

    /**
     * Constructs a task for downloading a part of a multi-part download.
     * 
     * @param db  The {@link GPUdb} instance used to access KiFS.
     * @param fileName  Name of the file to be downloaded.
     * @param multiPartDownloadInfo  Configuration information about this part
     *        of the download as well as the overall download itself.
     * @param options  Options to use during the download (reserved for
     *        future use).
     */
    public IoTask(GPUdb db,
                  String fileName,
                  MultiPartDownloadInfo multiPartDownloadInfo,
                  DownloadOptions options) {
        this.db = db;
        this.opMode = OpMode.DOWNLOAD;
        this.fileName = fileName;
        this.multiPartUploadInfo = null;
        this.multiPartDownloadInfo = multiPartDownloadInfo;
        this.uploadOptions = null;
        this.downloadOptions = options;
        this.taskNumber = 0;
        this.dataBytes = null;
    }


    /**
     * Executes this upload/download task by a threaded service.
     * 
     * @return  The {@link Result} of the upload/download operation.
     */
    @Override
    public Result call() {
        Result result = null;

        try {
            // FIX: Check for interruption status before starting heavy lifting
            if (Thread.currentThread().isInterrupted()) {
                throw new GPUdbException("Task interrupted before execution");
            }

            if (this.opMode == OpMode.UPLOAD) {
                result = upload();
            }
            else {
                result = download();
            }
        } catch (GPUdbException gpe) {
            // Log logic remains, but now we respect interruption flow
            GPUdbLogger.error( gpe.getMessage() );
            result = new Result();
            result.setSuccessful(false);
            result.setFileName( this.fileName );
            result.setOpMode( this.opMode );
            result.setUploadInfo( this.multiPartUploadInfo );
            result.setMultiPart(true);
            result.setException(gpe);
        }
        return result;
    }

    public MultiPartUploadInfo getMultiPartUploadInfo() {
        return this.multiPartUploadInfo;
    }

    public MultiPartDownloadInfo getMultiPartDownloadInfo() {
        return this.multiPartDownloadInfo;
    }


    /**
     * Uploads a part of a multi-part upload, first configuring the upload
     * request accordingly.
     */
    private Result upload() throws GPUdbException {
        Result result;
        switch ( this.multiPartUploadInfo.getPartOperation() ) {
            case INIT:
            case CANCEL:
            case COMPLETE: {
                Map<String, String> options = new HashMap<>();
                options.put( UploadFilesRequest.Options.MULTIPART_OPERATION, this.multiPartUploadInfo.getPartOperation().getValue() );
                options.put( UploadFilesRequest.Options.MULTIPART_UPLOAD_UUID , this.multiPartUploadInfo.getUuid() );
                result = upload(options);
                break;
            }
            default: {
                Map<String, String> options = new HashMap<>();
                options.put( UploadFilesRequest.Options.MULTIPART_OPERATION , this.multiPartUploadInfo.getPartOperation().getValue());
                options.put( UploadFilesRequest.Options.MULTIPART_UPLOAD_UUID , this.multiPartUploadInfo.getUuid() );
                options.put( UploadFilesRequest.Options.MULTIPART_UPLOAD_PART_NUMBER , String.valueOf( this.taskNumber ) );
                result = upload(options);
                break;
            }
        }
        return result;
    }


    /**
     * This method calls the actual GPUdb endpoint for uploading a part of the
     * multi-part upload being handled by the current instance of this class.
     *
     * @param options  Multi-part options for the upload endpoint.
     * @return  The {@link Result} of the upload operation.
     * @throws GPUdbException  If an error occurs at the endpoint.
     *
     * @see Result
     * @see MultiPartUploadInfo
     */
    private Result upload(Map<String, String> options) throws GPUdbException {
        List<ByteBuffer> data = null;
        if ( this.dataBytes == null ) {
            if ( this.uploadOptions.getTtl() > 0 )
                options.put( "ttl", String.valueOf( this.uploadOptions.getTtl() ) );

            // This case indicates that the multi part operation is a command
            // one. There is no data to be sent, but the endpoint expects an
            // empty data list to be sent in instead of a null.
            data = new ArrayList<>();
        } else {
            // This is the case for actual part file upload and the actual
            // data is being passed in.
            data = Collections.singletonList( this.dataBytes );
        }
        this.db.uploadFiles(Collections.singletonList( this.fileName ), data, options);
        Result result = new Result();
        result.setSuccessful(true);
        result.setFileName( this.fileName );
        result.setOpMode( this.opMode );
        result.setUploadInfo( this.multiPartUploadInfo );
        result.setMultiPart(true);
        return result;
    }

    /**
     * Downloads a part of a multi-part download, first configuring the download
     * request accordingly.
     */
    private Result download() throws GPUdbException {
        Result downloadResult = new Result();
        DownloadFilesResponse downloadFilesResponse = this.db.downloadFiles(
                Collections.singletonList( this.fileName ),
                Collections.singletonList( this.multiPartDownloadInfo.getReadOffset() ),
                Collections.singletonList( this.multiPartDownloadInfo.getReadLength() ),
                new HashMap<>() );
        this.multiPartDownloadInfo.setData( downloadFilesResponse.getFileData().get( 0 ));
        downloadResult.setSuccessful(true);
        downloadResult.setFileName( this.fileName );
        downloadResult.setOpMode( this.opMode );
        downloadResult.setDownloadInfo( this.multiPartDownloadInfo );
        downloadResult.setMultiPart(true);
        return downloadResult;
    }
}