package com.gpudb.filesystem.ingest;

import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.GPUdbLogger;
import com.gpudb.filesystem.GPUdbFileHandler;
import com.gpudb.filesystem.common.FileOperation;
import com.gpudb.filesystem.upload.FileUploader;
import com.gpudb.filesystem.upload.UploadOptions;
import com.gpudb.protocol.DeleteDirectoryRequest;
import com.gpudb.protocol.InsertRecordsFromFilesResponse;

import java.util.*;

/**
 * This is an internal class and not meant to be used by the end users of the
 * filesystem API. The consequences of using this class directly in client code
 * is not guaranteed and maybe undesirable.
 *
 * This class uses FileUploader class to upload files to server and then
 * uses insertRecordsFromFile method to ingest the uploaded file.
 */
public class FileIngestor {

    private final String KIFS_PREFIX = "kifs://";
    private final String REMOTE_USER_HOME_DIR = "~";

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
                        TableCreationOptions createTableOptions) throws GPUdbException {
        this.db = db;
        this.tableName = tableName;
        this.fileNames = fileNames;
        this.ingestOptions = ingestOptions == null ? new IngestOptions() : ingestOptions;
        this.createTableOptions = createTableOptions == null ? new TableCreationOptions() : createTableOptions;

    }

    public IngestOptions getIngestOptions() {
        return ingestOptions;
    }

    public void setIngestOptions(IngestOptions ingestOptions) {
        this.ingestOptions = ingestOptions;
    }

    public TableCreationOptions getCreateTableOptions() {
        return createTableOptions;
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
     * home directory on the server and ingested therefrom.
     *
     * @return - An {@link IngestResult} object
     */
    public IngestResult ingestFromFiles() {
        final String temp_dir = UUID.randomUUID().toString();
        Set<String> filesUploaded = new HashSet<>();

        try {
            // Upload the files to '~/" on KIFS, this directory is automatically aliased to
            // the users' home directory
            final String remoteDirName = REMOTE_USER_HOME_DIR + FileOperation.getKifsPathSeparator() + temp_dir;

            fileUploader = new FileUploader(db,
                    fileNames,
                    remoteDirName,
                    UploadOptions.defaultOptions(),
                    null,
                    new GPUdbFileHandler.Options());

            // Get the names of the files uploaded without any path component
            filesUploaded = fileUploader.getNamesOfFilesUploaded();

            fileUploader.upload();


            List<String> filePaths = new ArrayList<>();

            for (String fileName : filesUploaded) {
                String fullyQualifiedFileName = String.format("%s%s%s%s%s%s",
                        KIFS_PREFIX,
                        REMOTE_USER_HOME_DIR,
                        FileOperation.getKifsPathSeparator(),
                        temp_dir,
                        FileOperation.getKifsPathSeparator(),
                        fileName);
                filePaths.add( fullyQualifiedFileName );
            }

            InsertRecordsFromFilesResponse resp = db.insertRecordsFromFiles(
                    tableName,
                    filePaths,
                    new LinkedHashMap<String, Map<String, String>>(),
                    createTableOptions.getOptions(),
                    ingestOptions.getOptions()
            );

            deleteIngestedFiles( temp_dir);
            return convertToIngestResult(resp, null);
        } catch (GPUdbException ex) {
            GPUdbLogger.error( ex.getMessage() );
            deleteIngestedFiles( temp_dir);
            return convertToIngestResult( null, ex );
        }
    }

    private void deleteIngestedFiles(final String remoteDirName) {

        Map<String, String> options = new HashMap<>();
        options.put( DeleteDirectoryRequest.Options.RECURSIVE, DeleteDirectoryRequest.Options.TRUE);
        options.put( DeleteDirectoryRequest.Options.NO_ERROR_IF_NOT_EXISTS, DeleteDirectoryRequest.Options.TRUE);

        try {
            db.deleteDirectory(remoteDirName, options);
        } catch (GPUdbException e) {
            GPUdbLogger.error( e.getMessage() );
        }
    }

    /**
     * This method converts an object of type {@link InsertRecordsFromFilesResponse}
     * to an object of type {@link IngestResult}.
     * @param resp - An object of type {@link InsertRecordsFromFilesResponse}
     * @param ex - A GPUdbException object
     * @return - An object of type {@link IngestResult}
     */
    private IngestResult convertToIngestResult( InsertRecordsFromFilesResponse resp, GPUdbException ex ) {
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