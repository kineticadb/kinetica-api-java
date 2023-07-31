package com.gpudb.filesystem.common;

import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.GPUdbLogger;
//import com.gpudb.filesystem.download.MultiPartDownloadInfo;
import com.gpudb.filesystem.download.DownloadOptions;
import com.gpudb.filesystem.download.MultiPartDownloadInfo;
import com.gpudb.filesystem.upload.MultiPartUploadInfo;
import com.gpudb.filesystem.upload.UploadOptions;
import com.gpudb.protocol.DownloadFilesResponse;
import com.gpudb.protocol.UploadFilesRequest;
import com.gpudb.protocol.UploadFilesResponse;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * This is an internal class and not meant to be used by the end users of the
 * filesystem API. The consequences of using this class directly in client code
 * is not guaranteed and maybe undesirable.

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

    /**
     * This is the identifier for the
     * {@link com.gpudb.filesystem.upload.UploadIoJob} or
     * {@link com.gpudb.filesystem.download.DownloadIoJob}
     * that created this task.
     */
    private final String jobId;

    private final UploadOptions uploadOptions;

    private final DownloadOptions downloadOptions;

    /**
     * This field is actually an identifier for the part number for a
     * multi-part upload/download. It has been named
     */
    private final long taskNumber;

    /**
     * Name of the file to upload/download.
     */
    private final String fileName;

    /**
     *
     */
    private MultiPartUploadInfo multiPartUploadInfo;

    /**
     *
     */
    private MultiPartDownloadInfo multiPartDownloadInfo;

    /**
     * This is the actual data to be uploaded/downloaded for this part of the
     * multi-part upload/download.
     */
    private final ByteBuffer dataBytes;

    /**
     * Constructor
     * @param opMode - Identifies whether this task is an upload/download
     * @param jobId - Identifies the upload/download job that created this task
     * @param fileName - String - Name of the file to be uploaded/downloaded
     * @param uploadOptions
     */
    public IoTask(GPUdb db,
                  OpMode opMode,
                  String jobId,
                  String fileName,
                  UploadOptions uploadOptions,
                  DownloadOptions downloadOptions,
                  long taskNumber,
                  ByteBuffer dataBytes) {
        this.db = db;
        this.opMode = opMode;
        this.jobId = jobId;
        this.fileName = fileName;
        this.uploadOptions = uploadOptions;
        this.downloadOptions = downloadOptions;
        this.taskNumber = taskNumber;
        this.dataBytes = dataBytes;
    }

    /**
     * This method is called automatically the thread in which the current
     * instance of this class runs.
     * @return - {@link Result} - The result of the operation (Upload/download)
     */
    @Override
    public Result call() {

        Result result = null;

        try {
            if (opMode == OpMode.UPLOAD) {
                result = upload();
            }
            else {
                result = download();
            }
        } catch (GPUdbException gpe) {
            GPUdbLogger.error( gpe.getMessage() );
        }
        return result;
    }

    public void setMultiPartUploadInfo(MultiPartUploadInfo multiPartUploadInfo) {
        this.multiPartUploadInfo = multiPartUploadInfo;
    }

    public void setMultiPartDownloadInfo(MultiPartDownloadInfo multiPartDownloadInfo) {
        this.multiPartDownloadInfo = multiPartDownloadInfo;
    }

    public MultiPartUploadInfo getMultiPartUploadInfo() {
        return multiPartUploadInfo;
    }

    public MultiPartDownloadInfo getMultiPartDownloadInfo() {
        return multiPartDownloadInfo;
    }

    /**
     * This method handles the different stages of the multi-part upload
     *
     */
    private Result upload() throws GPUdbException {
        Result result;

        switch ( multiPartUploadInfo.getPartOperation() ) {
            // There could be four different multi-part operation values;
            // 'init', 'complete', 'upload_part' and 'cancel'.
            // For 'init' and 'complete', data should not be sent and the only
            // discriminator is the value of the option 'multipart_operation'.
            // Hence the treatment of these two options are the same and the
            // code to handle the cases is the same but the values differ.
            case INIT:
            case COMPLETE: {
                Map<String, String> options = new HashMap<>();
                options.put( UploadFilesRequest.Options.MULTIPART_OPERATION, multiPartUploadInfo.getPartOperation().getValue() );
                options.put( UploadFilesRequest.Options.MULTIPART_UPLOAD_UUID , multiPartUploadInfo.getUuid() );

                result = upload(options, fileName, null);

                break;
            }
            // Right now, since we are not handling cancelling multi-part
            // downloads, the only default case is 'upload_part'. This case
            // needs actual data to be sent across for each part of the file
            // that is uploaded.
            default: {
                //Part upload with data
                Map<String, String> options = new HashMap<>();
                options.put( UploadFilesRequest.Options.MULTIPART_OPERATION , multiPartUploadInfo.getPartOperation().getValue());
                options.put( UploadFilesRequest.Options.MULTIPART_UPLOAD_UUID , multiPartUploadInfo.getUuid() );
                options.put( UploadFilesRequest.Options.MULTIPART_UPLOAD_PART_NUMBER , String.valueOf( taskNumber ) );

                result = upload(options, fileName, dataBytes);

                break;
            }
        }
        return result;
    }

    /**
     * This method calls the actual GPUdb endpoint for uploading a part of the
     * multi-part upload being handled by the current instance of this class
     *
     * @param options - Map<String, String> - Options which are passed to the
     *                GPUdb endpoint
     * @param fileName - String - Name of the file to be uploaded/downloaded
     * @param dataBytes - ByteBuffer - The data bytes
     * @return - {@link Result} - The result of the upload operation
     * @throws GPUdbException - thrown if the endpoint throws an exception.
     *
     * @see Result
     * @see MultiPartUploadInfo
     */
    private Result upload(Map<String, String> options,
                          String fileName,
                          ByteBuffer dataBytes) throws GPUdbException {
        UploadFilesResponse ufResp;

        if( dataBytes == null ) {
            // This case indicates that the multi part operation is either
            // 'init' or 'complete'. There is no data to be sent but the
            // endpoint expects an empty list to be sent in instead of a null.
            if( uploadOptions.getTtl() > 0 ) {
                options.put( "ttl", String.valueOf( uploadOptions.getTtl() ) );
            }

            ufResp = db.uploadFiles( Collections.singletonList( fileName ), new ArrayList<ByteBuffer>(), options );
        } else {
            // This is the case for actual part file upload and the actual
            // data is being passed in.
            ufResp = db.uploadFiles(Collections.singletonList( fileName ), Collections.singletonList( dataBytes ), options);
        }

        Result result = new Result();
        result.setSuccessful(true);
        result.setFileName( fileName );
        result.setOpMode( opMode );
        result.setUploadInfo( multiPartUploadInfo );
        result.setMultiPart(true);

        return result;
    }

    /**
     *
     */
    private Result download() throws GPUdbException {

        Result downloadResult = new Result();

        DownloadFilesResponse downloadFilesResponse = db.downloadFiles(
                Collections.singletonList( fileName ),
                Collections.singletonList( multiPartDownloadInfo.getReadOffset() ),
                Collections.singletonList( multiPartDownloadInfo.getReadLength() ),
                new HashMap<String, String>() );

        multiPartDownloadInfo.setData( downloadFilesResponse.getFileData().get( 0 ));

        downloadResult.setSuccessful(true);
        downloadResult.setFileName( fileName );
        downloadResult.setOpMode( opMode );
        downloadResult.setDownloadInfo( multiPartDownloadInfo );
        downloadResult.setMultiPart(true);

        return downloadResult;
    }

}