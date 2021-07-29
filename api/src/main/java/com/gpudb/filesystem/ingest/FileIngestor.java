package com.gpudb.filesystem.ingest;

import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.filesystem.GPUdbFileHandler;
import com.gpudb.filesystem.common.FileOperation;
import com.gpudb.filesystem.upload.FileUploader;
import com.gpudb.filesystem.upload.UploadOptions;
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
    private final String SYS_TEMP_DIR = "sys_temp";

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
     * The files are uploaded to the location identified by 'kifs://sys_temp'
     * and ingested therefrom.
     *
     * @return - An {@link IngestResult} object
     */
    public IngestResult ingestFromFiles() throws GPUdbException {
        // Upload the files to 'sys_temp" on KIFS
        fileUploader = new FileUploader( db,
                fileNames,
                SYS_TEMP_DIR,
                UploadOptions.defaultOptions(),
                null,
                new GPUdbFileHandler.Options() );

        fileUploader.upload();

        // Get the names of the files uploaded without any path component
        Set<String> filesUploaded = fileUploader.getNamesOfFilesUploaded();

        List<String> filePaths = new ArrayList<>();

        for( String fileName: filesUploaded ) {
            filePaths.add( KIFS_PREFIX + SYS_TEMP_DIR + FileOperation.getKifsPathSeparator() + fileName );
        }

        InsertRecordsFromFilesResponse resp = db.insertRecordsFromFiles(
                                                    tableName,
                                                    filePaths,
                                                    new LinkedHashMap< String, Map<String,String> >(),
                                                    createTableOptions.getOptions(),
                                                    ingestOptions.getOptions()
                                            );

        return convertToIngestResult( resp );
    }

    /**
     * This method converts an object of type {@link InsertRecordsFromFilesResponse}
     * to an object of type {@link IngestResult}.
     * @param resp - An object of type {@link InsertRecordsFromFilesResponse}
     * @return - An object of type {@link IngestResult}
     */
    private IngestResult convertToIngestResult(InsertRecordsFromFilesResponse resp) {
        IngestResult ingestResult = new IngestResult();
        if( resp != null ) {
            ingestResult.setSuccessful( true );
            ingestResult.setException( null );
            ingestResult.setErrorMessage( null );
            ingestResult.setFiles( resp.getFiles() );
            ingestResult.setTableName( resp.getTableName() );
            ingestResult.setCountInserted( resp.getCountInserted() );
            ingestResult.setCountSkipped( resp.getCountSkipped() );
            ingestResult.setCountUpdated( resp.getCountUpdated() );
        }
        return ingestResult;
    }


}