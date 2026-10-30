package com.gpudb.filesystem.ingest;

import com.gpudb.filesystem.GPUdbFileHandler;

import java.util.List;

/**
 * The outcome of a file ingest, as returned by
 * {@link GPUdbFileHandler#ingest(List, String, IngestOptions, TableCreationOptions)}.
 * <p>
 * {@code ingest} returns a result only when the ingest succeeds; a failure is
 * thrown as a {@link com.gpudb.GPUdbException} instead.  A returned result
 * therefore always reports {@link #isSuccessful() success}, with no
 * {@link #getException() exception} or {@link #getErrorMessage() error message}.
 */
public class IngestResult {

    /**
     * Default constructor
     */
    public IngestResult() {
    }

    /**
     * Indicates whether the ingest is successful or not
     */
    private boolean successful;

    /**
     * The exception object if any; could be null
     */
    private Exception exception;

    /**
     * The error message if any exists; could be null
     */
    private String errorMessage;

    /**
     * List of files ingested
     */
    private List<String> files;

    /**
     * Count of records ingested
     */
    private long countInserted;

    /**
     * Count of records skipped
     */
    private long countSkipped;

    /**
     * Count of records updated
     */
    private long countUpdated;

    /**
     * The name of the table ingested into
     */
    private String tableName;

    /**
     * @return  whether the ingest succeeded
     */
    public boolean isSuccessful() {
        return this.successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    /**
     * @return  the error the ingest failed with, or {@code null} if it succeeded
     */
    public Exception getException() {
        return this.exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    /**
     * @return  the message of the error the ingest failed with, or {@code null}
     *          if it succeeded
     */
    public String getErrorMessage() {
        return this.errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * @return  the name of the table the records were ingested into
     */
    public String getTableName() {
        return this.tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Returns the source files the records would be ingested from.  The server
     * reports these only for a dry run -- an
     * {@link IngestOptions.Option#INGESTION_MODE ingestion mode} of
     * {@code dry_run} -- and the list is otherwise empty.
     *
     * @return  the source files of a dry run
     */
    public List<String> getFiles() {
        return this.files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }

    /**
     * @return  the number of records inserted; for a dry run, the number that
     *          would be inserted
     */
    public long getCountInserted() {
        return this.countInserted;
    }

    public void setCountInserted(long countInserted) {
        this.countInserted = countInserted;
    }

    /**
     * @return  the number of records skipped
     */
    public long getCountSkipped() {
        return this.countSkipped;
    }

    public void setCountSkipped(long countSkipped) {
        this.countSkipped = countSkipped;
    }

    /**
     * @return  the number of records updated
     */
    public long getCountUpdated() {
        return this.countUpdated;
    }

    public void setCountUpdated(long countUpdated) {
        this.countUpdated = countUpdated;
    }
}