package com.gpudb.filesystem.download;

import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.GPUdbLogger;
import com.gpudb.filesystem.GPUdbFileHandler;
import com.gpudb.filesystem.common.FileOperation;
import com.gpudb.filesystem.common.KifsFileInfo;
import com.gpudb.filesystem.common.OpMode;
import com.gpudb.filesystem.common.Result;
import com.gpudb.protocol.DownloadFilesResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

/**
 * This is an internal class and not meant to be used by the end users of the
 * filesystem API. The consequences of using this class directly in client code
 * is not guaranteed and maybe undesirable.

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

    private String encoding;

    private Map<String, DownloadIoJob> multiPartInfoMap;

    /**
     * Constructor
     *  @param db - GPUdb - The GPUdb instance
     * @param fileNames - List<String> - The names of the files on the KIFS to
     *                  downloaded.
     * @param localDirName - String - The name of the directory on the KIFS.
     * @param downloadOptions - {@link DownloadOptions} - Various options
     * @param callback - {@link FileDownloadListener} - The callback object used
     * @param fileHandlerOptions - a GPUdbFileHandler.Options type object
     */
    public FileDownloader(GPUdb db,
                          final List<String> fileNames,
                          final String localDirName,
                          DownloadOptions downloadOptions,
                          FileDownloadListener callback,
                          GPUdbFileHandler.Options fileHandlerOptions) throws GPUdbException {
        super(db, OpMode.DOWNLOAD, fileNames, localDirName,false, fileHandlerOptions);

        this.downloadOptions = downloadOptions;
        this.callback = callback;

    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * This method downloads the small files which could be downloaded in one
     * shot without chunking.
     *
     * If a {@link #callback} object is available it will be invoked.
     *
     * @throws GPUdbException - An exception object in case of error.
     */
    private void downloadFullFiles() throws GPUdbException {

        if( dirName == null || dirName.trim().isEmpty() ) {
            throw new GPUdbException("Name of local directory to save files cannot be null or empty");
        }

        DownloadFilesResponse dfResp = db.downloadFiles(new ArrayList<>(fullFileList),
                null,
                null,
                new HashMap<String, String>());

        int count = dfResp.getFileNames().size();
        for( int i = 0; i < count; i++ ) {
            String fileName = dfResp.getFileNames().get( i );
            ByteBuffer data = dfResp.getFileData().get( i );

            saveFullFile( fileName, data );
        }

        if( callback != null ) {
            callback.onFullFileDownload( dfResp.getFileNames() );
        }
    }

    /**
     * This method writes out a ByteBuffer to a file in the local directory.
     * It checks if the directory exists in the local filesystem and of not
     * it creates the directory tree if it doesn't exist.
     *
     * @param fileName - Full name of the local file to save.
     * @param byteBuffer - Bytebuffer - The data as bytes.
     * @throws GPUdbException - An exception object in case of error.
     */
    private void saveFullFile(String fileName, ByteBuffer byteBuffer) throws GPUdbException {

        String kifsFileName = StringUtils.substringAfterLast( fileName, FileOperation.KIFS_PATH_SEPARATOR );

        String localPath = dirName + File.separator + kifsFileName;

        String normalizedName = Paths.get( localPath ).normalize().toAbsolutePath().toString();

        if( Files.notExists( Paths.get( normalizedName )) || downloadOptions.isOverwriteExisting() ) {

            try ( FileChannel out = new FileOutputStream( normalizedName, !downloadOptions.isOverwriteExisting() ).getChannel() ) {
                while ( byteBuffer.hasRemaining() ) {
                    out.write(byteBuffer); // Write data from ByteBuffer to file
                }

            } catch (IOException ex) {
                GPUdbLogger.error( ex.getMessage() );
                throw new GPUdbException(ex.getMessage());
            }
        } else {
            GPUdbLogger.warn( String.format("File : %s exists in local directory %s ; Use 'overwriteExisting' to download and overwrite ", fileName, dirName ) );
        }

    }

    /**
     * This method takes a list of {@link ByteBuffer} objects and saves the
     * file to the local directory.
     *
     * @param fileName - Name of the file to save
     * @param payloads - A list of {@link ByteBuffer} objects
     *
     */
    private void saveMultiPartFile(String fileName, List<ByteBuffer> payloads) throws GPUdbException {

        String normalizedName = Paths.get( fileName ).normalize().toAbsolutePath().toString();

        if( Files.notExists( Paths.get( normalizedName )) || downloadOptions.isOverwriteExisting() ) {

            try ( FileChannel out = new FileOutputStream( normalizedName, !downloadOptions.isOverwriteExisting() ).getChannel() ) {

                for (ByteBuffer payload: payloads ) {
                    // Write data from ByteBuffer to file
                    out.write(payload);
                }

            } catch (IOException ex) {
                GPUdbLogger.error( ex.getMessage() );
                throw new GPUdbException(ex.getMessage());
            }
        } else {
            GPUdbLogger.warn( "File : " + fileName + " exists; Use 'overwriteExisting' to download and overwrite " );
        }

    }

    /**
     * This method downloads files which are candidates for multi-part downloads as
     * determined from their size.
     * Then it creates a list of {@link DownloadIoJob} instances one for each
     * file to be downloaded in parts.
     *
     * Once the jobs are created it calls the method {@link #executeJobs()}
     * and finally the method {@link #terminateJobs()}
     *
     * @see DownloadIoJob#createNewJob(GPUdb, GPUdbFileHandler.Options, String, String, String, KifsFileInfo, DownloadOptions, FileDownloadListener)
     *
     * @throws GPUdbException - An exception indicating what has gone wrong.
     */
    private void downloadMultiPartFiles() throws GPUdbException {
        // TODO implement here
        if( multiPartInfoMap == null ) {
            multiPartInfoMap = new HashMap<>();
        } else {
            multiPartInfoMap.clear();
        }

        // For each file in the multi part list create an IoJob instance
        for (String fileName : multiPartList) {
            String kifsFileName = StringUtils.substringAfterLast( fileName, FileOperation.KIFS_PATH_SEPARATOR );

            String localFileName = dirName + File.separator + kifsFileName;

            List<KifsFileInfo> kifsFileInfos = getFileInfoFromServer(fileName);

            Pair<String, DownloadIoJob> idJobPair = DownloadIoJob.createNewJob(db,
                    fileHandlerOptions,
                    dirName,
                    fileName,
                    localFileName,
                    kifsFileInfos.get(0),
                    downloadOptions,
                    callback);

            // Store the jobId-UploadIoJob pair in a map so that we can use them
            // later to start and stop the jobs as has been done in the methods
            // executeJobs and terminateJobs.
            multiPartInfoMap.put(idJobPair.getKey(), idJobPair.getValue());
        }

        executeJobs();

        Map<String, List<Result>> jobResultsMap = terminateJobs();

        //Now the save the file to the local directory.

        for ( Entry<String, List<Result>> result: jobResultsMap.entrySet() ) {
            List<Result> sortedList = result.getValue();

            // Sort by downloadPartNumber - ascending order
            // so that we can get the data buffers in correct sequence
            Collections.sort( sortedList, new Result.SortByDownloadPartNumber() );

            // Traverse the sorted list of Results and add the data for each
            // segment of the multi-part download to a list.
            List<ByteBuffer> allData = new ArrayList<>();
            for ( Result val: sortedList ) {
                allData.add( val.getDownloadInfo().getData() );
            }

            saveMultiPartFile( multiPartInfoMap.get( result.getKey() ).getDownloadLocalFileName(), allData );

        }

    }

    /**
     * This method traverses the map containing the {@link DownloadIoJob#getJobId()}-
     * {@link DownloadIoJob} values so that we can call the {@link DownloadIoJob#start()}
     * method on each of the 'DownloadIoJob' instances created to start the job.
     */
    private void executeJobs() {
        for (Entry<String, DownloadIoJob> entry: multiPartInfoMap.entrySet() ) {
            entry.getValue().start();
        }
    }

    /**
     * This method traverses the {@link #multiPartInfoMap} instance variable
     * to call the method {@link DownloadIoJob#stop()} to stop the jobs created
     * by the method {@link #executeJobs()}.
     *
     * @return - A map of jobId [String] to a list of {@link Result} objects so
     *          that it is possible to process the results of individual jobs
     */
    private Map<String, List<Result>> terminateJobs() {
        Map<String, List<Result>> results = new HashMap<>();
        for (Entry<String, DownloadIoJob> entry: multiPartInfoMap.entrySet() ) {

            if( !results.containsKey( entry.getKey() ) ) {
                List<Result> resultList = entry.getValue().stop();
                results.put( entry.getKey(), resultList );
            }
        }

        return results;
    }


    /**
     * This is the main upload method which is to be called by the users of
     * this class. Internally depending whether there are files to be uploaded
     * one shot or in parts it will call the methods
     * {@link #downloadFullFiles()} ()} and {@link #downloadMultiPartFiles()} ()}
     */
    public void download() throws GPUdbException {
        if( multiPartList.size() > 0 ) {
            downloadMultiPartFiles();
        }

        if( fullFileList.size() > 0 ) {
            downloadFullFiles();
        }

        if( fullFileList.size() == 0 && multiPartList.size() == 0 ) {
            GPUdbLogger.warn( "No files found to download ..." );
        }
    }

}