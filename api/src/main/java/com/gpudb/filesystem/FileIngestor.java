package com.gpudb.filesystem;

import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.GPUdbLogger;
import com.gpudb.filesystem.ingest.IngestOptions;
import com.gpudb.filesystem.ingest.IngestResult;
import com.gpudb.filesystem.ingest.TableCreationOptions;
import com.gpudb.filesystem.upload.UploadOptions;
import com.gpudb.protocol.DeleteFilesRequest;
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
class FileIngestor {

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

    FileIngestor(final GPUdb db,
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

    IngestOptions getIngestOptions() {
        return this.ingestOptions;
    }

    void setIngestOptions(IngestOptions ingestOptions) {
        this.ingestOptions = ingestOptions;
    }

    TableCreationOptions getCreateTableOptions() {
        return this.createTableOptions;
    }

    void setCreateTableOptions(TableCreationOptions createTableOptions) {
        this.createTableOptions = createTableOptions;
    }

    /**
     * This method uploads the files using the {@link #fileUploader} object
     * and calls the method {@link GPUdb#insertRecordsFromFiles(String, List, Map, Map, Map)}
     * to ingest from the uploaded files.
     *
     * The files are uploaded to the location identified by 'kifs://~/<some_uuid>' where '~' indicates the user's
     * home directory on the server and ingested there from.  The uploaded copies
     * are deleted afterward, whether or not the upload or ingest succeeded.
     *
     * @return  An {@link IngestResult} object
     */
    IngestResult ingestFromFiles() {
        final String ingestStagingDir = UUID.randomUUID().toString();

        // Upload the files to '~/" on KIFS, this directory is automatically aliased to
        // the users' home directory
        final String remoteDirName = GPUdbFileHandler.REMOTE_USER_HOME_DIR_PREFIX + GPUdbFileHandler.KIFS_PATH_SEPARATOR + ingestStagingDir;

        // KiFS paths of the staged copies, without the 'kifs://' prefix.  Known
        // before the upload starts, so that a partial upload is removed as well.
        final List<String> stagedFiles = new ArrayList<>();

        try {
            this.fileUploader = new FileUploader(
                    this.db,
                    this.fileNames,
                    remoteDirName,
                    UploadOptions.defaultOptions(),
                    null,
                    new GPUdbFileHandler.Options());

            // Get the names of the files uploaded without any path component
            for (String fileName : this.fileUploader.getNamesOfFilesUploaded())
                stagedFiles.add( remoteDirName + GPUdbFileHandler.KIFS_PATH_SEPARATOR + fileName );

            this.fileUploader.upload();


            List<String> filePaths = new ArrayList<>();

            for (String stagedFile : stagedFiles)
                filePaths.add( GPUdbFileHandler.KIFS_PATH_PREFIX + stagedFile );

            InsertRecordsFromFilesResponse resp = this.db.insertRecordsFromFiles(
                    this.tableName,
                    filePaths,
                    new LinkedHashMap<>(),
                    this.createTableOptions.getOptions(),
                    this.ingestOptions.getOptions()
            );

            return convertToIngestResult(resp, null);
        } catch (GPUdbException ex) {
            GPUdbLogger.error( ex.getMessage() );
            return convertToIngestResult( null, ex );
        } finally {
            deleteStagedFiles( stagedFiles );
        }
    }

    /**
     * Deletes the staged copies of the ingested files.
     *
     * They are deleted by name rather than by removing the staging directory:
     * that directory is a subdirectory of the user's KiFS home directory, and
     * {@link GPUdb#deleteDirectory(String, Map)} accepts only top-level
     * directories.  A KiFS subdirectory exists only as part of the paths of the
     * files in it, so nothing is left once they are gone.
     *
     * A failure here is logged rather than thrown, so that it does not mask the
     * outcome of the ingest itself.
     *
     * @param stagedFiles  KiFS paths, without the 'kifs://' prefix, of the
     *        staged copies to delete; files never uploaded are ignored
     */
    private void deleteStagedFiles(final List<String> stagedFiles) {
        if (stagedFiles.isEmpty())
            return;

        Map<String, String> options = new HashMap<>();
        options.put( DeleteFilesRequest.Options.NO_ERROR_IF_NOT_EXISTS, DeleteFilesRequest.Options.TRUE );

        try {
            this.db.deleteFiles( stagedFiles, options );
        } catch (GPUdbException e) {
            GPUdbLogger.error( String.format( "Could not delete staged ingest files %s: %s", stagedFiles, e.getMessage() ) );
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
