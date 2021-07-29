package com.gpudb.filesystem.ingest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpudb.GPUdbException;
import com.gpudb.GPUdbLogger;
import com.gpudb.filesystem.utils.GPUdbFileHandlerUtils;
import com.gpudb.protocol.InsertRecordsFromFilesRequest;
import org.apache.commons.lang3.tuple.Pair;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 */
@JsonIgnoreProperties(value = { "options", "mapper" })
public class IngestOptions {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String badRecordTableName;
    private long badRecordTableLimit;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Pair<String,String>> columnFormats;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Set<String> columnsToLoad;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Set<String> columnsToSkip;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> defaultColumnFormats;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ErrorHandlingMode errorHandlingMode;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private FileType fileType;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> primaryKeys;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> shardKeys;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String textCommentString;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String textDelimiter;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String textEscapeCharacter;

    private boolean textHasHeader;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String textHeaderPropertyDelimiter;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String textNullString;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String textQuoteCharacter;

    private boolean truncateTable;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private TypeInferenceMode typeInferenceMode;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> additionalOptions;

    private Map<String, String> options;

    private final ObjectMapper mapper = GPUdbFileHandlerUtils.getJacksonObjectMapper();


    /**
     * Default constructor
     */
    public IngestOptions() {
    }

    /**
     * Returns the value of 'bad_record_table_name'
     * @return - a string
     *
     * @see #setBadRecordTableName(String)
     */
    public String getBadRecordTableName() {
        return badRecordTableName;
    }

    /**
     * Sets the value of 'bad_record_table_name'.
     * @param badRecordTableName - a string
     * @return - an {@link IngestOptions} object
     *
     * @see #getBadRecordTableName()
     *
     */
    public IngestOptions setBadRecordTableName(String badRecordTableName) {
        if( badRecordTableName != null && !badRecordTableName.isEmpty() ) {
            this.badRecordTableName = badRecordTableName;
            setMapValue(InsertRecordsFromFilesRequest.Options.BAD_RECORD_TABLE_NAME, this.badRecordTableName );
        } else {
            GPUdbLogger.warn( "BAD_RECORD_TABLE_NAME is either null or empty, value not set");
        }
        return this;
    }

    /**
     * Returns the value of 'bad_record_table_limit'
     * @return - a long value
     *
     * @see #setBadRecordTableLimit(long)
     */
    public long getBadRecordTableLimit() {
        return badRecordTableLimit;
    }

    /**
     * Sets the value of 'bad_record_table_limit'
     *
     * @param badRecordTableLimit - a long value
     * @return - an {@link IngestOptions} object
     *
     * @see #getBadRecordTableLimit()
     */
    public IngestOptions setBadRecordTableLimit(long badRecordTableLimit) {
        this.badRecordTableLimit = badRecordTableLimit;
        setMapValue( InsertRecordsFromFilesRequest.Options.BAD_RECORD_TABLE_LIMIT, String.valueOf( badRecordTableLimit) );

        return this;
    }

    /**
     * Gets the 'column_format' as a Map representing a JSON
     * @return - a Map of a String to a Pair of Strings
     *
     * @see #setColumnFormats(Map)
     */
    public Map<String, Pair<String, String>> getColumnFormats() {
        return columnFormats;
    }

    /**
     * Sets the column formats
     * @param columnFormats - A Map of String to a Pair of Strings
     * @return- an {@link IngestOptions} object
     * @throws JsonProcessingException
     *
     * @see - {@link #getColumnFormats()}
     *
     */
    public IngestOptions setColumnFormats(Map<String, Pair<String, String>> columnFormats) throws JsonProcessingException {
        if( columnFormats != null && columnFormats.size() > 0 ) {
            this.columnFormats = columnFormats;
            setMapValue( InsertRecordsFromFilesRequest.Options.COLUMN_FORMATS, mapper.writeValueAsString( this.columnFormats ));
        } else {
            GPUdbLogger.warn( "COLUMN_FORMATS is either null or empty; value not set" );
        }

        return this;
    }

    /**
     * Gets the set of column names to load
     * @return - a Set of column names
     *
     * @see #setColumnsToLoad(Set)
     */
    public Set<String> getColumnsToLoad() {
        return columnsToLoad;
    }

    /**
     * Sets the set of column names to load
     * @param columnsToLoad - a Set of column names
     * @return an {@link IngestOptions} object
     * @throws GPUdbException
     *
     * @see #getColumnsToLoad()
     */
    public IngestOptions setColumnsToLoad(Set<String> columnsToLoad) throws GPUdbException {
        if( columnsToLoad != null && columnsToSkip != null ) {
            Set<String> result = GPUdbFileHandlerUtils.setIntersection( columnsToLoad, columnsToSkip );
            if( result.size() > 0 )
                throw new GPUdbException(" Columns to load and columns to skip are mutually exclusive ..");
        }
        this.columnsToLoad = columnsToLoad;
        try {
            setMapValue( InsertRecordsFromFilesRequest.Options.COLUMNS_TO_LOAD, mapper.writeValueAsString( this.columnsToLoad ));
        } catch (JsonProcessingException e) {
            GPUdbLogger.error( String.format( "Error in Json processing [%s]; COLUMNS_TO_LOAD value not set", e.getMessage() ) );
        }

        return this;
    }

    /**
     * Gets the set of column names to skip
     * @return - a Set of column names
     *
     * @see #setColumnsToSkip(Set)
     */
    public Set<String> getColumnsToSkip() {
        return columnsToSkip;
    }

    /**
     * Sets the column names to skip
     * @param columnsToSkip - a Set of column names
     * @return - an {@link IngestOptions} object
     * @throws GPUdbException
     *
     * @see #getColumnsToSkip()
     */
    public IngestOptions setColumnsToSkip(Set<String> columnsToSkip) throws GPUdbException {
        if( columnsToSkip != null && columnsToLoad != null ) {
            Set<String> result = GPUdbFileHandlerUtils.setIntersection( columnsToSkip, columnsToLoad );
            if( result.size() > 0 )
                throw new GPUdbException(" Columns to load and columns to skip are mutually exclusive ..");
        }
        this.columnsToSkip = columnsToSkip;
        try {
            setMapValue( InsertRecordsFromFilesRequest.Options.COLUMNS_TO_SKIP, mapper.writeValueAsString( this.columnsToSkip ));
        } catch (JsonProcessingException e) {
            GPUdbLogger.error( String.format( "Error in Json processing [%s]: COLUMNS_TO_SKIP value not set", e.getMessage() ) );
        }

        return this;
    }

    /**
     * Gets a Map of default column formats
     * @return
     */
    public Map<String, String> getDefaultColumnFormats() {
        return defaultColumnFormats;
    }

    /**
     * Sets the default column formats
     * @param defaultColumnFormats - a Map of string to String
     * @return an {@link IngestOptions} object
     * @throws JsonProcessingException
     *
     * @see #getDefaultColumnFormats()
     */
    public IngestOptions setDefaultColumnFormats(Map<String, String> defaultColumnFormats) throws JsonProcessingException {
        if( defaultColumnFormats != null && defaultColumnFormats.size() > 0 ) {
            this.defaultColumnFormats = defaultColumnFormats;
            setMapValue( InsertRecordsFromFilesRequest.Options.DEFAULT_COLUMN_FORMATS, mapper.writeValueAsString( this.defaultColumnFormats ));
        } else {
            GPUdbLogger.warn( "DEFAULT_COLUMN_FORMATS cannot be null or empty; value not set" );
        }

        return this;
    }

    /**
     * 
     * @return
     */
    public ErrorHandlingMode getErrorHandlingMode() {
        return errorHandlingMode;
    }

    public IngestOptions setErrorHandlingMode(ErrorHandlingMode errorHandlingMode) {
        this.errorHandlingMode = errorHandlingMode;
        setMapValue( InsertRecordsFromFilesRequest.Options.ERROR_HANDLING, this.errorHandlingMode.getText() );

        return this;
    }

    public FileType getFileType() {
        return fileType;
    }

    public IngestOptions setFileType(FileType fileType) {
        this.fileType = fileType;
        setMapValue( InsertRecordsFromFilesRequest.Options.FILE_TYPE, this.fileType.getText() );
        return this;
    }

    public List<String> getPrimaryKeys() {
        return primaryKeys;
    }

    public IngestOptions setPrimaryKeys(List<String> primaryKeys) {
        if( primaryKeys != null && !primaryKeys.isEmpty() ) {
            this.primaryKeys = primaryKeys;
            setMapValue( InsertRecordsFromFilesRequest.Options.PRIMARY_KEYS, GPUdbFileHandlerUtils.joinStrings( primaryKeys, ',' ) );
        } else {
            GPUdbLogger.warn( "PRIMARY_KEYS cannot be null or empty; value not set" );
        }

        return this;
    }

    public List<String> getShardKeys() {
        return shardKeys;
    }

    public IngestOptions setShardKeys(List<String> shardKeys) {
        if( shardKeys != null && !shardKeys.isEmpty() ) {
            this.shardKeys = shardKeys;
            setMapValue( InsertRecordsFromFilesRequest.Options.SHARD_KEYS, GPUdbFileHandlerUtils.joinStrings( shardKeys, ',' ) );
        } else {
            GPUdbLogger.warn( "SHARD_KEYS cannot be null or empty; value not set");
        }
        return this;
    }

    public String getTextCommentString() {
        return textCommentString;
    }

    public IngestOptions setTextCommentString(String textCommentString) {
        this.textCommentString = textCommentString;
        setMapValue( InsertRecordsFromFilesRequest.Options.TEXT_COMMENT_STRING, this.textCommentString );

        return this;
    }

    public String getTextDelimiter() {
        return textDelimiter;
    }

    public IngestOptions setTextDelimiter(String textDelimiter) {
        this.textDelimiter = textDelimiter;
        setMapValue( InsertRecordsFromFilesRequest.Options.TEXT_DELIMITER, this.textDelimiter );
        return this;
    }

    public String getTextEscapeCharacter() {
        return textEscapeCharacter;
    }

    public IngestOptions setTextEscapeCharacter(String textEscapeCharacter) {
        this.textEscapeCharacter = textEscapeCharacter;
        setMapValue( InsertRecordsFromFilesRequest.Options.TEXT_ESCAPE_CHARACTER, this.textEscapeCharacter );
        return this;
    }

    public boolean isTextHasHeader() {
        return textHasHeader;
    }

    public IngestOptions setTextHasHeader(boolean textHasHeader) {
        this.textHasHeader = textHasHeader;
        setMapValue( InsertRecordsFromFilesRequest.Options.TEXT_HAS_HEADER, textHasHeader
                ? InsertRecordsFromFilesRequest.Options.TRUE
                : InsertRecordsFromFilesRequest.Options.FALSE );
        return this;
    }

    public String getTextHeaderPropertyDelimiter() {
        return textHeaderPropertyDelimiter;
    }

    public IngestOptions setTextHeaderPropertyDelimiter(String textHeaderPropertyDelimiter) {
        this.textHeaderPropertyDelimiter = textHeaderPropertyDelimiter;
        setMapValue( InsertRecordsFromFilesRequest.Options.TEXT_HEADER_PROPERTY_DELIMITER, this.textHeaderPropertyDelimiter );

        return this;
    }

    public String getTextNullString() {
        return textNullString;
    }

    public IngestOptions setTextNullString(String textNullString) {
        this.textNullString = textNullString;
        setMapValue( InsertRecordsFromFilesRequest.Options.TEXT_NULL_STRING, this.textNullString );
        return this;
    }

    public String getTextQuoteCharacter() {
        return textQuoteCharacter;
    }

    /**
     *
     * @param textQuoteCharacter
     * @return
     */
    public IngestOptions setTextQuoteCharacter(String textQuoteCharacter) {
        this.textQuoteCharacter = textQuoteCharacter;
        setMapValue( InsertRecordsFromFilesRequest.Options.TEXT_QUOTE_CHARACTER, this.textQuoteCharacter );
        return this;
    }

    public boolean isTruncateTable() {
        return truncateTable;
    }

    public IngestOptions setTruncateTable(boolean truncateTable) {
        this.truncateTable = truncateTable;
        setMapValue( InsertRecordsFromFilesRequest.Options.TRUNCATE_TABLE, truncateTable
                ? InsertRecordsFromFilesRequest.Options.TRUE
                : InsertRecordsFromFilesRequest.Options.FALSE );

        return this;
    }

    public TypeInferenceMode getTypeInferenceMode() {
        return typeInferenceMode;
    }

    public IngestOptions setTypeInferenceMode(TypeInferenceMode typeInferenceMode) {
        this.typeInferenceMode = typeInferenceMode;
        setMapValue( InsertRecordsFromFilesRequest.Options.TYPE_INFERENCE_MODE, this.typeInferenceMode.getText());
        return this;
    }

    public void put(String key, int value ) {
        setAdditionalOptions( key, String.valueOf( value ) );
    }

    public void put( String key, long value ) {
        setAdditionalOptions( key, String.valueOf( value ) );
    }

    public void put ( String key, String value ) {
        setAdditionalOptions( key, value );
    }

    public void put( String key, boolean value ) {
        setAdditionalOptions( key, String.valueOf( value ) );
    }

    /**
     * This method
     * @return
     */
    public Map<String, String> getOptions() {
        if( options == null ) {
            options = new LinkedHashMap<>();
        }

        if( additionalOptions != null && additionalOptions.size() > 0 ) {
            options.putAll( additionalOptions );
        }
        return options;
    }

    /**
     *
     * @param key -String object
     * @param value - String object
     */
    private void setMapValue( String key, String value ) {
        if( options == null ) {
            options = new LinkedHashMap<>();
        }
        options.put( key, value );
    }

    private void setAdditionalOptions( String key, String value ) {
        if( this.additionalOptions == null ) {
            this.additionalOptions = new LinkedHashMap<>();
        }
        additionalOptions.put( key, value );
    }

    /**
     *
     */
    public static final class Option {

        public static final String BAD_RECORD_TABLE_NAME = "bad_record_table_name";
        public static final String BAD_RECORD_TABLE_LIMIT = "bad_record_table_limit";
        public static final String BATCH_SIZE = "batch_size";
        public static final String COLUMN_FORMATS = "column_formats";
        public static final String COLUMNS_TO_LOAD = "columns_to_load";
        public static final String COLUMNS_TO_SKIP = "columns_to_skip";
        public static final String DATASOURCE_NAME = "datasource_name";
        public static final String DEFAULT_COLUMN_FORMATS = "default_column_formats";
        public static final String ERROR_HANDLING = "error_handling";
        public static final String FILE_TYPE = "file_type";
        public static final String INGESTION_MODE = "ingestion_mode";
        public static final String LOADING_MODE = "loading_mode";
        public static final String PRIMARY_KEYS = "primary_keys";
        public static final String SHARD_KEYS = "shard_keys";
        public static final String SUBSCRIBE = "subscribe";
        public static final String POLL_INTERVAL = "poll_interval";
        public static final String TEXT_COMMENT_STRING = "text_comment_string";
        public static final String TEXT_DELIMITER = "text_delimiter";
        public static final String TEXT_ESCAPE_CHARACTER = "text_escape_character";
        public static final String TEXT_HAS_HEADER = "text_has_header";
        public static final String TEXT_HEADER_PROPERTY_DELIMITER = "text_header_property_delimiter";
        public static final String TEXT_NULL_STRING = "text_null_string";
        public static final String TEXT_QUOTE_CHARACTER = "text_quote_character";
        public static final String TRUNCATE_TABLE = "truncate_table";
        public static final String NUM_TASKS_PER_RANK = "num_tasks_per_rank";
        public static final String TYPE_INFERENCE_MODE = "type_inference_mode";
        public static final String TABLE_INSERT_MODE = "table_insert_mode";

    }


}