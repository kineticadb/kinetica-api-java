package com.gpudb.filesystem.download;

import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.GPUdbLogger;
import com.gpudb.filesystem.GPUdbFileHandler;
import com.gpudb.filesystem.common.*;
import com.gpudb.filesystem.utils.GPUdbFileHandlerUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/** This class models a an Io operation of a specific
 * {@link OpMode} i.e., UPLOAD or DOWNLOAD. Different instances of this
 * class will be created for different operations and no instance of this
 * class will be shared.
 */
public class DownloadIoJob {

    private final GPUdb db;

    /**
     *
     */
    private final String jobId;

    /**
     *
     */
    private final String downloadFileName;

    /**
     *
     */
    private final String downloadLocalFileName;

    /**
     *
     */
    private final String kifsDirName;

    /**
     *
     */
    private final DownloadOptions downloadOptions;

    /**
     *
     */
    private final List<IoTask> listOfTasks;

    private final FileDownloadListener callback;
    /**
     * Fixed threadpool of size 1
     */
    private final ExecutorService threadPool;

    /**
     *
     */
    private final CompletionService<Result> jobExecutor;

    private final KifsFileInfo kifsFileInfo;
    private final GPUdbFileHandler.Options fileHandlerOptions;

    /**
     * Constructor
     *
     * @param db - The {@link GPUdb} instance
     * @param fileHandlerOptions
     * @param dirName - Name of the KIFS directory
     * @param fileName - Name of the KIFS file to be downloaded
     * @param localFileName - Name of the local file with directory
     * @param kifsFileInfo - A {@link KifsFileInfo} object
     * @param downloadOptions - A {@link DownloadOptions} object.
     * @param callback - A {@link FileDownloadListener} implementation
     * @throws GPUdbException
     */
    private DownloadIoJob(GPUdb db,
                          GPUdbFileHandler.Options fileHandlerOptions,
                          String dirName,
                          String fileName,
                          String localFileName,
                          KifsFileInfo kifsFileInfo,
                          DownloadOptions downloadOptions,
                          FileDownloadListener callback) throws GPUdbException {
        if( fileName == null || fileName.trim().isEmpty()) {
            throw new GPUdbException("File name cannot be null or empty");
        }

        this.db = db;
        this.fileHandlerOptions = fileHandlerOptions;
        this.kifsDirName = dirName;
        this.downloadFileName = fileName;
        this.downloadLocalFileName = localFileName;
        this.kifsFileInfo = kifsFileInfo;
        this.downloadOptions = downloadOptions;
        this.callback = callback;

        this.listOfTasks = new ArrayList<>();

        threadPool = Executors.newSingleThreadExecutor();
        jobExecutor = new ExecutorCompletionService<>(threadPool);

        jobId = UUID.randomUUID().toString();
    }

    public String getDownloadFileName() {
        return downloadFileName;
    }

    public String getDownloadLocalFileName() {
        return downloadLocalFileName;
    }

    /**
     * Static helper method to create new instance of {@link DownloadIoJob}
     *
     * @param db - The {@link GPUdb} instance
     * @param fileHandlerOptions
     * @param dirName - Name of the KIFS directory
     * @param fileName - Name of the KIFS file to be downloaded
     * @param localFileName - Name of the local file with directory
     * @param kifsFileInfo - A {@link KifsFileInfo} object
     * @param downloadOptions - A {@link DownloadOptions} object.
     * @param callback - A {@link FileDownloadListener} implementation
     *
     * @return - A {@link Pair} of JobId and {@link DownloadIoJob} object.
     * @throws GPUdbException
     */
    public static Pair<String, DownloadIoJob> createNewJob(GPUdb db,
                                                           GPUdbFileHandler.Options fileHandlerOptions,
                                                           String dirName,
                                                           String fileName,
                                                           String localFileName,
                                                           KifsFileInfo kifsFileInfo,
                                                           DownloadOptions downloadOptions,
                                                           FileDownloadListener callback) throws GPUdbException {
        DownloadIoJob newJob = new DownloadIoJob(db,
                fileHandlerOptions,
                dirName,
                fileName,
                localFileName,
                kifsFileInfo,
                downloadOptions,
                callback);
        String jobId = newJob.getJobId();

        return Pair.of( jobId, newJob);
    }

    public String getJobId() {
        return jobId;
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    public CompletionService<Result> getJobExecutor() {
        return jobExecutor;
    }

    /** This method starts the job to download the multipart file by submitting
     * different segments of the file according to the value returned by
     * getFileSizeToSplit() method of GPUdbFileHandler.Options class.
     * Each segment of the file is submitted as a new {@link IoTask} which is
     * run in an independent thread
     *
     * @see GPUdbFileHandler#getOptions()
     */
    public void start() {

        final long sourceSize = kifsFileInfo.getFileSize();
        final int bytesPerSplit = (int) fileHandlerOptions.getFileSizeToSplit();
        final long numSplits = sourceSize / bytesPerSplit;
        final long totalParts = numSplits + 1;

        int partNo = 0;
        int offset = 0;

        while( offset < sourceSize ) {

            MultiPartDownloadInfo downloadInfo = new MultiPartDownloadInfo();

            downloadInfo.setReadOffset( offset );
            downloadInfo.setReadLength( bytesPerSplit );
            downloadInfo.setDownloadPartNumber( partNo );
            downloadInfo.setTotalParts( totalParts );

            IoTask newTask = new IoTask( db,
                    OpMode.DOWNLOAD,
                    jobId,
                    downloadFileName,
                    null,
                    downloadOptions,
                    partNo,
                    null );

            newTask.setMultiPartDownloadInfo( downloadInfo );

            jobExecutor.submit( newTask );

            listOfTasks.add( newTask );

            offset += bytesPerSplit;
            partNo++;
        }
    }

    /**
     * This method is used to stop all the running {@link IoTask} instances
     * by handling the completed {@link Future} objects returned by the
     * running {@link CompletionService} instance identified by the {@link #jobExecutor}
     * instance variable. Each completed {@link Future} object will return a
     * {@link Result} object which can be examined to get the exact status of
     * the background job.
     *
     * @see Result
     * @see CompletionService
     * @see FileDownloader - method terminateJobs().
     *
     * @return - List<Result> - A list of Result objects.
     */
    public List<Result> stop() {
        List<Result> results = new ArrayList<>();

        int countTasks = listOfTasks.size();

        while( countTasks-- > 0 ) {
            try {
                Result result = jobExecutor.take().get();
                results.add( result );

                if( callback != null ) {
                    callback.onPartDownload( result );
                }
            } catch (InterruptedException | ExecutionException e) {
                GPUdbLogger.error( e.getMessage() );
            }
        }

        if( callback != null ) {
            callback.onMultiPartDownloadComplete( results );
        }

        GPUdbFileHandlerUtils.awaitTerminationAfterShutdown( this.threadPool,
                GPUdbFileHandler.getDefaultThreadPoolTerminationTimeout() );

        return results;


    }

}