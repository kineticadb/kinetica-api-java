package com.gpudb.filesystem;

import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.GPUdbLogger;
import com.gpudb.filesystem.common.KifsFileInfo;
import com.gpudb.filesystem.common.OpMode;
import com.gpudb.filesystem.common.Result;
import com.gpudb.filesystem.download.DownloadOptions;
import com.gpudb.filesystem.download.FileDownloadListener;
import com.gpudb.protocol.DownloadFilesResponse;
import com.gpudb.protocol.ShowFilesResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This is an internal class and not meant to be used by the end users of the
 * {@code filesystem} API. The consequences of using this class directly in
 * client code is not guaranteed and maybe undesirable.

 * This class handles downloading of either single part file or multiple part
 * downloads. This class extends the class {@link FileOperation} and provides
 * additional functionalities of creating instances of {@link DownloadIoJob},
 * starting them and waiting for them to terminate.
 *
 * The main exposed method to call is {@link #download()} ()} which calls two
 * private methods named {@link #downloadFullFiles()} ()} and
 * {@link #downloadMultiPartFiles()} ()} respectively.
 *
 * The method {@link #downloadFullFiles()} ()} does the download by calling
 * the Java endpoint to download all files in one go.
 *
 * The method {@link #downloadMultiPartFiles()} ()} does the downloads
 * by creating background threads since each file could take a long time to
 * download. The multiple parts of a single file are downloaded sequentially in a
 * single thread and multiple files are downloaded in different threads.
 *
 */
class FileDownloader extends FileOperation {

    private final FileDownloadListener callback;

    private final DownloadOptions downloadOptions;


    /**
     * Constructs a new {@link FileDownloader} manager for downloading a given
     * set of files from a given KiFS directory to a local directory.
     *
     * @param db  The {@link GPUdb} instance used to access KiFS.
     * @param fileNames  List of names of the KiFS files to download.
     * @param localDirName  Name of local directory to download to.
     * @param options  The {@link DownloadOptions} object which is used to
     *        configure the download operation.
     * @param callback  The callback {@link FileDownloadListener} for this
     *        download manager to notify as the download job progresses.
     * @param fileHandlerOptions  Options for setting up the files for transfer.
     */
    FileDownloader(GPUdb db,
                   final List<String> fileNames,
                   final String localDirName,
                   DownloadOptions options,
                   FileDownloadListener callback,
                   GPUdbFileHandler.Options fileHandlerOptions) throws GPUdbException {
        super(db, OpMode.DOWNLOAD, fileNames, localDirName, false, fileHandlerOptions);

        this.downloadOptions = options;
        this.callback = callback;

    }


    /**
     * This method downloads the small files which could be downloaded in one
     * shot without chunking.
     *
     * If a {@link #callback} object is available it will be invoked.
     *
     * @return A list of {@link Result} objects for all full file downloads (both successful and failed).
     */
    private List<Result> downloadFullFiles() {

        List<Result> allResults = new ArrayList<>();

        if( this.dirName == null || this.dirName.trim().isEmpty() ) {
            Result result = new Result();
            result.setSuccessful(false);
            result.setErrorMessage("Name of local directory to save files cannot be null or empty");
            result.setException(new GPUdbException(result.getErrorMessage()));
            allResults.add(result);
            return allResults;
        }

        List<List<String>> batches = createBatches();

        for (int batchNum = 0; batchNum < batches.size(); batchNum++) {
            List<String> batch = batches.get(batchNum);
            List<Result> batchResults = downloadFullFileBatch(batch);
            allResults.addAll(batchResults);
        }

        return allResults;
    }


    /**
     * This method does the download for a batch files which are small enough to
     * be downloaded in one go. Right now the size threshold for such files have
     * been kept at 60 MB. In case a callback object {@link FileDownloadListener}
     * has been specified, {@link FileDownloadListener#onFullFileDownload(List)}
     * will be called.
     *
     * @return A list of {@link Result} objects for the batch download (both successful and failed).
     */
    private List<Result> downloadFullFileBatch(List<String> fullFileBatch) {

        List<Result> results = new ArrayList<>();
        List<String> successfulFiles = new ArrayList<>();

        DownloadFilesResponse dfResp;
        try {
            dfResp = this.db.downloadFiles(
                    fullFileBatch,
                    null,
                    null,
                    new HashMap<>());
        } catch (GPUdbException e) {
            // If the batch download fails, create error results for all files in the batch
            String errorMessage = String.format("Failed to download file batch: %s", e.getMessage());
            GPUdbLogger.error(errorMessage);
            for (String fileName : fullFileBatch) {
                Result result = new Result();
                result.setFileName(fileName);
                result.setSuccessful(false);
                result.setException(e);
                result.setErrorMessage(errorMessage);
                result.setOpMode(OpMode.DOWNLOAD);
                results.add(result);
            }
            return results;
        }

        int count = dfResp.getFileNames().size();
        for( int i = 0; i < count; i++ ) {
            String fileName = dfResp.getFileNames().get( i );
            ByteBuffer data = dfResp.getFileData().get( i );

            Result result = new Result();
            result.setFileName(fileName);
            result.setOpMode(OpMode.DOWNLOAD);

            try {
                saveFullFile( fileName, data );
                result.setSuccessful(true);
                successfulFiles.add(fileName);
            } catch (GPUdbException e) {
                String errorMessage = String.format("Failed to save downloaded file <%s>: %s", fileName, e.getMessage());
                GPUdbLogger.error(errorMessage);
                result.setSuccessful(false);
                result.setException(e);
                result.setErrorMessage(errorMessage);
            }

            results.add(result);
        }

        // Notify callback about successful downloads
        if( this.callback != null && !successfulFiles.isEmpty() ) {
            this.callback.onFullFileDownload( successfulFiles );
        }

        return results;
    }


    /**
     * Splits files to download into subsets, each subset containing a group of
     * files that collectively fall under the configured split/batch size limit.
     *
     * @return  A list of lists of files to download, one outer-list for each
     *        inner-list batch of file names to download at once.
     */
    private List<List<String>> createBatches() {

       long sum = 0;

        List<List<String>> batches = new ArrayList<>();
        List<String> batch = new ArrayList<>();

        if( this.fullFileList.size() == 0 )
            return batches;

        List<Long> sizes = this.fullFileList.stream().map(file -> {
            ShowFilesResponse sfResp;
            try {
                sfResp = this.db.showFiles( Collections.singletonList(file), new HashMap<>());
                return sfResp.getSizes().get(0);
            } catch (GPUdbException e) {
                GPUdbLogger.error(e.getMessage());
            }
            return 0L;
        }).collect(Collectors.toList());

        for( int i=0, s = this.fullFileList.size(); i < s; i++  ) {

            String file = this.fullFileList.get(i);

            long size = sizes.get(i);
            batch.add( file );
            sum += size;

            if( sum > this.fileHandlerOptions.getFileSizeToSplit() ) {
                // This must go into the next bucket as it overshoots the
                // partitionSum value
                batch.remove( file );

                // Re-adjust the index so that current value is re-read in the
                // next iteration
                i--;
            }

            if( sum >= this.fileHandlerOptions.getFileSizeToSplit() ) {
                batches.add( new ArrayList<>(batch) );
                batch.clear();
                sum = 0;
            }
        }

        if( batch.size() > 0) { // We have an incomplete batch left, add it
            batches.add(batch);
        }

        // If batches is empty here it means all files together didn't add up to the size threshold
        // so, we just create a single batch
        if( batches.size() == 0) {
            batches.add(this.fullFileList);
        }

        return batches;
    }


    /**
     * This method writes out a ByteBuffer to a file in the local directory.
     * It checks if the directory exists in the local file system and of not
     * it creates the directory tree if it doesn't exist.
     *
     * @param kifsFileName  Fully-qualified KiFS path of the file to save.
     * @param byteBuffer  The downloaded file content as bytes.
     * @throws GPUdbException  If an error is encountered writing out the file.
     */
    private void saveFullFile(String kifsFileName, ByteBuffer byteBuffer) throws GPUdbException {

        String fileName = StringUtils.substringAfterLast( kifsFileName, GPUdbFileHandler.KIFS_PATH_SEPARATOR );

        String localPath = this.dirName + File.separator + fileName;

        Path normalizedPath = Paths.get( localPath ).normalize().toAbsolutePath();

        GPUdbLogger.debug(String.format("Writing downloaded KiFS file <%s> to <%s>", fileName, normalizedPath.toString()));

        if( Files.notExists( normalizedPath) || this.downloadOptions.isOverwriteExisting() ) {

            try ( FileChannel out = FileChannel.open(normalizedPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING) ) {
                while ( byteBuffer.hasRemaining() ) {
                    out.write(byteBuffer); // Write data from ByteBuffer to file
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                throw new GPUdbException(String.format("Error writing downloaded KiFS file <%s> to <%s>:  %s", kifsFileName, normalizedPath.toString(), ex.getMessage()), ex.getCause());
            }
        } else {
            GPUdbLogger.warn( String.format("File <%s> exists in local directory <%s>; Use 'overwriteExisting' to download and overwrite ", kifsFileName, this.dirName ) );
        }

    }


    /**
     * This method downloads files which are candidates for multi-part downloads
     * as determined from their size.
     *
     * It creates a list of {@link DownloadIoJob} instances, one for each file,
     * to be downloaded in parts.
     *
     * Once the jobs are created it calls the method to execute jobs
     * and finally the method to terminate jobs.
     *
     * @return A list of {@link Result} objects for all multi-part downloads (both successful and failed).
     *
     * @see DownloadIoJob#createNewJob(GPUdb, GPUdbFileHandler.Options, String, String, KifsFileInfo, DownloadOptions, FileDownloadListener)
     */
    private List<Result> downloadMultiPartFiles() {

        List<Result> results = new ArrayList<>();

        // For each file in the multi part list create an IoJob instance
        for (String kifsFileName : this.multiPartList) {
            String fileName = StringUtils.substringAfterLast( kifsFileName, GPUdbFileHandler.KIFS_PATH_SEPARATOR );

            String localFileName = this.dirName + File.separator + fileName;

            Result result = new Result();
            result.setFileName(kifsFileName);
            result.setMultiPart(true);
            result.setOpMode(OpMode.DOWNLOAD);

            List<KifsFileInfo> kifsFileInfos;
            try {
                kifsFileInfos = getFileInfoFromServer(kifsFileName);
            } catch (GPUdbException e) {
                String errorMessage = String.format("Failed to get file info for multi-part download <%s>: %s", kifsFileName, e.getMessage());
                GPUdbLogger.error(errorMessage);
                result.setSuccessful(false);
                result.setException(e);
                result.setErrorMessage(errorMessage);
                results.add(result);
                continue;
            }

            Pair<String, DownloadIoJob> idJobPair;
            try {
                idJobPair = DownloadIoJob.createNewJob(
                        this.db,
                        this.fileHandlerOptions,
                        kifsFileName,
                        localFileName,
                        kifsFileInfos.get(0),
                        this.downloadOptions,
                        this.callback);
            } catch (GPUdbException e) {
                String errorMessage = String.format("Failed to create download job for multi-part file <%s>: %s", kifsFileName, e.getMessage());
                GPUdbLogger.error(errorMessage);
                result.setSuccessful(false);
                result.setException(e);
                result.setErrorMessage(errorMessage);
                results.add(result);
                continue;
            }

            try {
                // Try to download, but catch exception to prevent breaking the loop
                idJobPair.getValue().start();
                result.setSuccessful(true);
            } catch (GPUdbException e) {
                String errorMessage = String.format("Failed to download multi-part file <%s>: %s", kifsFileName, e.getMessage());
                GPUdbLogger.error(errorMessage);
                result.setSuccessful(false);
                result.setException(e);
                result.setErrorMessage(errorMessage);
            } finally {
                // Wait for it to stop (clean up threads) before processing the next file
                idJobPair.getValue().stop();
            }

            results.add(result);
        }

        return results;
    }


    /**
     * This is the main download method which is to be called by the users of
     * this class. Internally depending whether there are files to be downloaded
     * one shot or in parts it will call the methods
     * {@link #downloadFullFiles()} and {@link #downloadMultiPartFiles()}
     *
     * @throws GPUdbException If any file downloads fail. The exception message contains
     *         all collected error messages from failed downloads.
     */
    void download() throws GPUdbException {

        List<Result> multiPartResults = downloadMultiPartFiles();
        List<Result> allResults = new ArrayList<>(multiPartResults);

        List<Result> fullFileResults = downloadFullFiles();
        allResults.addAll(fullFileResults);

        if(this.fullFileList.isEmpty() && this.multiPartList.isEmpty())
            GPUdbLogger.warn( "No files found to download ..." );

        // Collect all error messages from failed downloads
        List<String> errorMessages = new ArrayList<>();
        for (Result result : allResults) {
            if (!result.isSuccessful()) {
                errorMessages.add(result.getErrorMessage());
            }
        }

        // If there were any errors, throw an exception with all collected error messages
        if (!errorMessages.isEmpty()) {
            throw new GPUdbException(String.join("\n", errorMessages));
        }
    }

}
