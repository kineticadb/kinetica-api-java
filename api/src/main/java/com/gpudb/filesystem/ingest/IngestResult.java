package com.gpudb.filesystem.ingest;

import java.util.List;

/**
 * 
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
     * Cound of records updated
     */
    private long countUpdated;

    /**
     * The name of the table ingested into
     */
    private String tableName;

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }

    public long getCountInserted() {
        return countInserted;
    }

    public void setCountInserted(long countInserted) {
        this.countInserted = countInserted;
    }

    public long getCountSkipped() {
        return countSkipped;
    }

    public void setCountSkipped(long countSkipped) {
        this.countSkipped = countSkipped;
    }

    public long getCountUpdated() {
        return countUpdated;
    }

    public void setCountUpdated(long countUpdated) {
        this.countUpdated = countUpdated;
    }
}