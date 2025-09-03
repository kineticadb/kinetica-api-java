package com.gpudb.filesystem.ingest;

import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.GPUdbLogger;
import com.gpudb.filesystem.GPUdbFileHandler;
import com.gpudb.filesystem.upload.FileUploader;
import com.gpudb.filesystem.upload.UploadOptions;
import com.gpudb.protocol.DeleteDirectoryRequest;
import com.gpudb.protocol.InsertRecordsFromFilesResponse;

import java.util.*;

/**
 * This is an internal class and not meant to be used by the end users of the
 * {@code filesystem} API. The consequences of using this class directly in
 * client code is not guaranteed and maybe undesirable.
 *
 * This class uses FileUploader class to upload files to server and then
 * uses insertRecordsFromFile method to ingest the uploaded file.
 */
public class FileIngestor {

    private final GPUdb db;

    private final String tableName;

    /**
     * List of files to ingest
     */
    private final List<String> fileNames;

    /**
     * Ingest options that could be set and passed on to the server.
     */
    private IngestOptions ingestOptions;

    /**
     * Table creation options that could be set and passed on to the server
     */
    private TableCreationOptions createTableOptions;

    /**
     * An instance of uploader used internally to upload the files to KIFS.
     */
    private FileUploader fileUploader;

    public FileIngestor(final GPUdb db,
                        final String tableName,
                        final List<String> fileNames,
                        IngestOptions ingestOptions,
                        TableCreationOptions createTableOptions) {
        this.db = db;
        this.tableName = tableName;
        this.fileNames = fileNames;
        this.ingestOptions = ingestOptions == null ? new IngestOptions() : ingestOptions;
        this.createTableOptions = createTableOptions == null ? new TableCreationOptions() : createTableOptions;
    }

    public IngestOptions getIngestOptions() {
        return this.ingestOptions;
    }

    public void setIngestOptions(IngestOptions ingestOptions) {
        this.ingestOptions = ingestOptions;
    }

    public TableCreationOptions getCreateTableOptions() {
        return this.createTableOptions;
    }

    public void setCreateTableOptions(TableCreationOptions createTableOptions) {
        this.createTableOptions = createTableOptions;
    }

    /**
     * This method uploads the files using the {@link #fileUploader} object
     * and calls the method {@link GPUdb#insertRecordsFromFiles(String, List, Map, Map, Map)}
     * to ingest from the uploaded files.
     *
     * The files are uploaded to the location identified by 'kifs://~/<some_uuid>' where '~' indicates the user's
     * home directory on the server and ingested there from.
     *
     * @return  An {@link IngestResult} object
     */
    public IngestResult ingestFromFiles() {
        final String ingestStagingDir = UUID.randomUUID().toString();
        Set<String> filesUploaded = new HashSet<>();

        try {
            // Upload the files to '~/" on KIFS, this directory is automatically aliased to
            // the users' home directory
            final String remoteDirName = GPUdbFileHandler.REMOTE_USER_HOME_DIR_PREFIX + GPUdbFileHandler.KIFS_PATH_SEPARATOR + ingestStagingDir;

            this.fileUploader = new FileUploader(
                    this.db,
                    this.fileNames,
                    remoteDirName,
                    UploadOptions.defaultOptions(),
                    null,
                    new GPUdbFileHandler.Options());

            // Get the names of the files uploaded without any path component
            filesUploaded = this.fileUploader.getNamesOfFilesUploaded();

            this.fileUploader.upload();


            List<String> filePaths = new ArrayList<>();

            for (String fileName : filesUploaded) {
                String fullyQualifiedFileName = String.format("%s%s%s%s",
                        GPUdbFileHandler.KIFS_PATH_PREFIX,
                        remoteDirName,
                        GPUdbFileHandler.KIFS_PATH_SEPARATOR,
                        fileName);
                filePaths.add( fullyQualifiedFileName );
            }

            InsertRecordsFromFilesResponse resp = this.db.insertRecordsFromFiles(
                    this.tableName,
                    filePaths,
                    new LinkedHashMap<>(),
                    this.createTableOptions.getOptions(),
                    this.ingestOptions.getOptions()
            );

            deleteIngestedFiles( ingestStagingDir);
            return convertToIngestResult(resp, null);
        } catch (GPUdbException ex) {
            GPUdbLogger.error( ex.getMessage() );
            deleteIngestedFiles( ingestStagingDir);
            return convertToIngestResult( null, ex );
        }
    }

    private void deleteIngestedFiles(final String remoteDirName) {

        Map<String, String> options = new HashMap<>();
        options.put( DeleteDirectoryRequest.Options.RECURSIVE, DeleteDirectoryRequest.Options.TRUE);
        options.put( DeleteDirectoryRequest.Options.NO_ERROR_IF_NOT_EXISTS, DeleteDirectoryRequest.Options.TRUE);

        try {
            this.db.deleteDirectory(remoteDirName, options);
        } catch (GPUdbException e) {
            GPUdbLogger.error( e.getMessage() );
        }
    }

    /**
     * This method converts an object of type {@link InsertRecordsFromFilesResponse}
     * to an object of type {@link IngestResult}.
     * 
     * @param resp  The response object from a given insert records call.
     * @param ex  The exception object from a given insert records call.
     * @return  An object of type {@link IngestResult}, combining the two.
     */
    private static IngestResult convertToIngestResult( InsertRecordsFromFilesResponse resp, GPUdbException ex ) {
        IngestResult ingestResult = new IngestResult();
        ingestResult.setException( ex );
        ingestResult.setErrorMessage( ex != null ? ex.getMessage() : null );
        ingestResult.setSuccessful( ex == null );

        if( resp != null ) {
            ingestResult.setFiles( resp.getFiles() );
            ingestResult.setTableName( resp.getTableName() );
            ingestResult.setCountInserted( resp.getCountInserted() );
            ingestResult.setCountSkipped( resp.getCountSkipped() );
            ingestResult.setCountUpdated( resp.getCountUpdated() );
        }

        return ingestResult;
    }


}