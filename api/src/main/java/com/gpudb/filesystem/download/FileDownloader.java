package com.gpudb.filesystem.download;

import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.GPUdbLogger;
import com.gpudb.GPUdbRuntimeException;
import com.gpudb.filesystem.GPUdbFileHandler;
import com.gpudb.filesystem.common.FileOperation;
import com.gpudb.filesystem.common.KifsFileInfo;
import com.gpudb.filesystem.common.OpMode;
import com.gpudb.filesystem.common.Result;
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
import java.util.stream.IntStream;

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
public class FileDownloader extends FileOperation {

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
    public FileDownloader(GPUdb db,
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
     * @throws GPUdbException  If an error occurs transferring any batches of
     *        non-multi-part designated files from the server.
     */
    private void downloadFullFiles() throws GPUdbException {

        if( this.dirName == null || this.dirName.trim().isEmpty() )
            throw new GPUdbException("Name of local directory to save files cannot be null or empty");

        List<List<String>> batches = createBatches();

        try {
            IntStream.range(0, batches.size()).forEach(batchNum -> {
                List<String> batch = batches.get( batchNum );
                try {
                    downloadFullFileBatch(batch);
                } catch (GPUdbException e) {
                    throw new GPUdbRuntimeException(e);
                }
            });
        }
        catch (GPUdbRuntimeException e) {
            throw new GPUdbException("Error downloading file batch", e);
        }
    }


    /**
     * This method does the download for a batch files which are small enough to
     * be downloaded in one go. Right now the size threshold for such files have
     * been kept at 60 MB. In case a callback object {@link FileDownloadListener}
     * has been specified, {@link FileDownloadListener#onFullFileDownload(Result)}
     * will be called.
     * 
     * @throws GPUdbException  If an error occurs transferring any file in this
     *        batch of non-multi-part designated files to the server.
     */
    private void downloadFullFileBatch(List<String> fullFileBatch) throws GPUdbException {
        
        DownloadFilesResponse dfResp = this.db.downloadFiles(
                fullFileBatch,
                null,
                null,
                new HashMap<>());

        int count = dfResp.getFileNames().size();
        for( int i = 0; i < count; i++ ) {
            String fileName = dfResp.getFileNames().get( i );
            ByteBuffer data = dfResp.getFileData().get( i );

            saveFullFile( fileName, data );
        }

        if( this.callback != null )
            this.callback.onFullFileDownload( dfResp.getFileNames() );
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
     * Once the jobs are created it calls the method {@link #executeJobs()}
     * and finally the method {@link #terminateJobs()}
     *
     * @see DownloadIoJob#createNewJob(GPUdb, GPUdbFileHandler.Options, String, String, String, KifsFileInfo, DownloadOptions, FileDownloadListener)
     * 
     * @throws GPUdbException  If an error occurs transferring any of the files
     *        from the server.
     */
    private void downloadMultiPartFiles() throws GPUdbException {

        // For each file in the multi part list create an IoJob instance
        for (String kifsFileName : this.multiPartList) {
            String fileName = StringUtils.substringAfterLast( kifsFileName, GPUdbFileHandler.KIFS_PATH_SEPARATOR );

            String localFileName = this.dirName + File.separator + fileName;

            List<KifsFileInfo> kifsFileInfos = getFileInfoFromServer(kifsFileName);

            Pair<String, DownloadIoJob> idJobPair = DownloadIoJob.createNewJob(
                    this.db,
                    this.fileHandlerOptions,
                    kifsFileName,
                    localFileName,
                    kifsFileInfos.get(0),
                    this.downloadOptions,
                    this.callback);

            // start the job immediately
            idJobPair.getValue().start();

            // Wait for it to stop before processing the next file
            idJobPair.getValue().stop();
        }
    }


    /**
     * This is the main download method which is to be called by the users of
     * this class. Internally depending whether there are files to be downloaded
     * one shot or in parts it will call the methods
     * {@link #downloadFullFiles()} ()} and {@link #downloadMultiPartFiles()} ()}
     * 
     * @throws GPUdbException  If an error occurs transferring any of the files
     *        from the server.
     */
    public void download() throws GPUdbException {

        downloadMultiPartFiles();

        downloadFullFiles();

        if( this.fullFileList.size() == 0 && this.multiPartList.size() == 0 )
            GPUdbLogger.warn( "No files found to download ..." );
    }

}