package com.gpudb.filesystem.ingest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.gpudb.GPUdbLogger;
import com.gpudb.filesystem.utils.GPUdbFileHandlerUtils;
import com.gpudb.protocol.InsertRecordsFromFilesRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(value = { "options" })
public class TableCreationOptions {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String typeId;

    private boolean noErrorIfExists;
    private boolean isReplicated;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> foreignKeys;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String foreignShardKey;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private PartitionType partitionType;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> partitionKeys;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> partitionDefinitions;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private boolean automaticPartition;

    private int ttl;
    private boolean resultTable;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> additionalOptions;

    private Map<String, String> options;

    public TableCreationOptions() {

    }

    /**
     * Gets the type ID set for table creation.
     * @return - The type ID string
     * 
     * @see #setTypeId(String)
     *
     */
    public String getTypeId() {
        return typeId;
    }

    /**
     * Sets the type ID for the table
     * @param typeId - A type ID string
     * @return - This {@link TableCreationOptions} object
     *
     * @see #getTypeId() 
     */
    public TableCreationOptions setTypeId(String typeId) {
        this.typeId = typeId;
        setMapValue( InsertRecordsFromFilesRequest.CreateTableOptions.TYPE_ID, this.typeId );
        return this;
    }


    /**
     * Gets the value of 'noErrorIfExists'
     * @return
     *
     * @see #setNoErrorIfExists(boolean)
     */
    public boolean isNoErrorIfExists() {
        return noErrorIfExists;
    }

    /**
     * Sets the value of 'noErrorIfExists'
     * A truthy value indicates that no error will be thrown if there is an attempt
     * to re-create an already existent table and false otherwise.
     * @param noErrorIfExists - true or false
     * @return- This {@link TableCreationOptions} object
     */
    public TableCreationOptions setNoErrorIfExists(boolean noErrorIfExists) {
        this.noErrorIfExists = noErrorIfExists;
        setMapValue( InsertRecordsFromFilesRequest.CreateTableOptions.NO_ERROR_IF_EXISTS, this.noErrorIfExists
                ? InsertRecordsFromFilesRequest.CreateTableOptions.TRUE
                : InsertRecordsFromFilesRequest.CreateTableOptions.FALSE );
        return this;
    }

    /**
     * Gets the value of 'isReplicated'
     * @return true or false
     */
    public boolean isReplicated() {
        return isReplicated;
    }

    /**
     * Sets the value of 'replicated' indicating whether the table to be
     * created will be a replicated table or not.
     * @param replicated - True or false
     * @return - This {@link TableCreationOptions} object
     *
     * @see #isReplicated()
     */
    public TableCreationOptions setReplicated(boolean replicated) {
        this.isReplicated = replicated;
        setMapValue( InsertRecordsFromFilesRequest.CreateTableOptions.IS_REPLICATED, this.noErrorIfExists
                ? InsertRecordsFromFilesRequest.CreateTableOptions.TRUE
                : InsertRecordsFromFilesRequest.CreateTableOptions.FALSE );
        return this;
    }

    /**
     * Gets the list of foreign keys
     * @return - The list of foreign keys
     *
     * @see #setForeignKeys(List)
     */
    public List<String> getForeignKeys() {
        return foreignKeys;
    }

    /**
     * Sets the list of foreign keys
     * @param foreignKeys - A list of strings specifying the foreign keys
     * @return - This {@link TableCreationOptions} object
     *
     * @see #getForeignKeys()
     */
    public TableCreationOptions setForeignKeys(List<String> foreignKeys) {
        if( foreignKeys != null && !foreignKeys.isEmpty() ) {
            this.foreignKeys = foreignKeys;
            setMapValue( InsertRecordsFromFilesRequest.CreateTableOptions.FOREIGN_KEYS, GPUdbFileHandlerUtils.joinStrings( foreignKeys, ',' ) );
        } else {
            GPUdbLogger.warn( "FOREIGN_KEYS cannot be null or empty; value not set");
        }
        return this;
    }

    /**
     * Gets the foreign shard key
     * @return - the value of the foreign shard key
     */
    public String getForeignShardKey() {
        return foreignShardKey;
    }

    /**
     * Sets the foreign shard key value
     * @param foreignShardKey
     * @return - This {@link TableCreationOptions} object
     *
     * @see #getForeignShardKey()
     */
    public TableCreationOptions setForeignShardKey(String foreignShardKey) {
        if( foreignShardKey != null && !foreignShardKey.isEmpty() ) {
            this.foreignShardKey = foreignShardKey;
            setMapValue( InsertRecordsFromFilesRequest.CreateTableOptions.FOREIGN_SHARD_KEY, foreignShardKey );
        } else {
            GPUdbLogger.warn( "FOREIGN_SHARD_KEY cannot be null or empty; value not set" );
        }
        return this;
    }

    /**
     * Gets the value of 'partitionType' used for table creation
     *
     * @return A {@link PartitionType} value
     *
     * @see PartitionType
     * @see #setPartitionType(PartitionType)
     */
    public PartitionType getPartitionType() {
        return partitionType;
    }

    /**
     * Sets the parition type value used for table creation
     * @param partitionType enum {@link PartitionType}
     * @return - This {@link TableCreationOptions} object
     *
     * @see #getPartitionType()
     */
    public TableCreationOptions setPartitionType(PartitionType partitionType) {
        this.partitionType = partitionType;
        setMapValue( InsertRecordsFromFilesRequest.CreateTableOptions.PARTITION_TYPE, partitionType.getText() );
        return this;
    }

    /**
     * Gets the list of partition keys
     * @return A list of partition keys
     *
     * @see #setPartitionKeys(List)
     */
    public List<String> getPartitionKeys() {
        return partitionKeys;
    }

    /**
     * Sets the list of partition keys
     * @param partitionKeys - A list of partition ksys
     * @return - This {@link TableCreationOptions} object
     *
     * @see #getPartitionKeys()
     */
    public TableCreationOptions setPartitionKeys(List<String> partitionKeys) {
        if( partitionKeys != null && !partitionKeys.isEmpty() ) {
            this.partitionKeys = partitionKeys;
            setMapValue( InsertRecordsFromFilesRequest.CreateTableOptions.PARTITION_KEYS, GPUdbFileHandlerUtils.joinStrings( partitionKeys, ',' ) );
        } else {
            GPUdbLogger.warn( "PARTITION_KEYS list cannot be null or empty; value not set" );
        }
        return this;
    }

    /**
     * Gets the list of partition definitions
     * @return - a list of partition definitions
     *
     * @see #setPartitionDefinitions(List)
     */
    public List<String> getPartitionDefinitions() {
        return partitionDefinitions;
    }

    /**
     * Sets the list of partition definitions
     * @param partitionDefinitions
     * @return - This {@link TableCreationOptions} object
     *
     * @see #getPartitionDefinitions()
     */
    public TableCreationOptions setPartitionDefinitions(List<String> partitionDefinitions) {
        if( partitionDefinitions != null && !partitionDefinitions.isEmpty() ) {
            this.partitionDefinitions = partitionDefinitions;
            setMapValue( InsertRecordsFromFilesRequest.CreateTableOptions.PARTITION_DEFINITIONS, GPUdbFileHandlerUtils.joinStrings( partitionDefinitions, ',' ) );
        } else {
            GPUdbLogger.warn( "PARTITION_DEFINITIONS list cannot be null or empty; value not set" );
        }
        return this;
    }

    /**
     * Gets whether the partiion is automatic or not
     * @return true or false
     *
     * @see #setAutomaticPartition(boolean)
     */
    public boolean isAutomaticPartition() {
        return automaticPartition;
    }

    /**
     * Sets the value of 'automaticPartition'
     * @param automaticPartition - true or false
     * @return - This {@link TableCreationOptions} object
     *
     * @see #isAutomaticPartition()
     */
    public TableCreationOptions setAutomaticPartition(boolean automaticPartition) {
        this.automaticPartition = automaticPartition;
        setMapValue( InsertRecordsFromFilesRequest.CreateTableOptions.IS_AUTOMATIC_PARTITION, automaticPartition
                ? InsertRecordsFromFilesRequest.CreateTableOptions.TRUE
                : InsertRecordsFromFilesRequest.CreateTableOptions.FALSE );
        return this;
    }

    /**
     * Gets the value of 'ttl' for the table
     * @return an int value indicating the 'ttl'
     *
     *
     */
    public int getTtl() {
        return ttl;
    }

    /**
     * Sets the 'ttl' value for the table
     * @param ttl - an int value
     * @return - This {@link TableCreationOptions} object
     *
     * @see #getTtl()
     */
    public TableCreationOptions setTtl(int ttl) {
        this.ttl = ttl;
        setMapValue( InsertRecordsFromFilesRequest.CreateTableOptions.TTL, String.valueOf( ttl ) );
        return this;
    }

    /**
     * Gets whether the table is a result table or not
     *
     * @return true or false
     *
     * @see #setResultTable(boolean)
     */
    public boolean isResultTable() {
        return resultTable;
    }

    /**
     * Sets the value of 'resultTable' indicating whether this table is a
     * result table or not
     * @param resultTable - true or false
     * @return - This {@link TableCreationOptions} object
     *
     * @see @isResultTable()
     */
    public TableCreationOptions setResultTable(boolean resultTable) {
        this.resultTable = resultTable;
        setMapValue( InsertRecordsFromFilesRequest.CreateTableOptions.IS_RESULT_TABLE, resultTable
                ? InsertRecordsFromFilesRequest.CreateTableOptions.TRUE
                : InsertRecordsFromFilesRequest.CreateTableOptions.FALSE );
        return this;
    }

    /**
     * One of the four overloaded methods based on varying data types for
     * setting options not already exposed through the other setter methods
     * as documented here.
     * @param key - a string
     * @param value - an int
     *
     * @see #put(String, int)
     * @see #put(String, long)
     * @see #put(String, String)
     * @see #put(String, boolean)
     */
    public void put(String key, int value ) {
        setAdditionalOptions( key, String.valueOf( value ) );
    }

    /**
     * One of the four overloaded methods based on varying data types for
     * setting options not already exposed through the other setter methods
     * as documented here.
     * @param key - a string
     * @param value - a long
     *
     * @see #put(String, int)
     * @see #put(String, long)
     * @see #put(String, String)
     * @see #put(String, boolean)
     */
    public void put( String key, long value ) {
        setAdditionalOptions( key, String.valueOf( value ) );
    }

    /**
     * One of the four overloaded methods based on varying data types for
     * setting options not already exposed through the other setter methods
     * as documented here.
     * @param key - a string
     * @param value - a string
     *
     * @see #put(String, int)
     * @see #put(String, long)
     * @see #put(String, String)
     * @see #put(String, boolean)
     */
    public void put ( String key, String value ) {
        setAdditionalOptions( key, value );
    }

    /**
     * One of the four overloaded methods based on varying data types for
     * setting options not already exposed through the other setter methods
     * as documented here.
     * @param key - a string
     * @param value - a boolean
     *
     * @see #put(String, int)
     * @see #put(String, long)
     * @see #put(String, String)
     * @see #put(String, boolean)
     */
    public void put( String key, boolean value ) {
        setAdditionalOptions( key, String.valueOf( value ) );
    }


    /**
     * This method returns a Map of String to String carrying all the options
     * that have been set by the user. This is internally used to get the
     * options so that they can be passed to the endpoints which expect the
     * options as a Map of String to String.
     * @return - a Map of String to String
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

        public static final String TYPE_ID = "type_id";
        public static final String NO_ERROR_IF_EXISTS = "no_error_if_exists";
        public static final String IS_REPLICATED = "is_replicated";
        public static final String FOREIGN_KEYS = "foreign_keys";
        public static final String FOREIGN_SHARD_KEY = "foreign_shard_key";
        public static final String PARTITION_TYPE = "partition_type";
        public static final String PARTITION_KEYS = "partition_keys";
        public static final String PARTITION_DEFINITIONS = "partition_definitions";
        public static final String IS_AUTOMATIC_PARTITION = "is_automatic_partition";
        public static final String TTL = "ttl";
        public static final String CHUNK_SIZE = "chunk_size";
        public static final String CHUNK_BYTES = "chunk_bytes";
        public static final String IS_RESULT_TABLE = "is_result_table";
        public static final String STRATEGY_DEFINITION = "strategy_definition";

    }

}

