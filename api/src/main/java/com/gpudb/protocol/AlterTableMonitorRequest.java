/*
 *  This file was autogenerated by the Kinetica schema processor.
 *
 *  DO NOT EDIT DIRECTLY.
 */
package com.gpudb.protocol;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;

/**
 * A set of parameters for {@link
 * com.gpudb.GPUdb#alterTableMonitor(AlterTableMonitorRequest)
 * GPUdb.alterTableMonitor}.
 * <p>
 * Alters a table monitor previously created with {@link
 * com.gpudb.GPUdb#createTableMonitor(CreateTableMonitorRequest)
 * GPUdb.createTableMonitor}.
 */
public class AlterTableMonitorRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("AlterTableMonitorRequest")
            .namespace("com.gpudb")
            .fields()
                .name("topicId").type().stringType().noDefault()
                .name("monitorUpdatesMap").type().map().values().stringType().noDefault()
                .name("options").type().map().values().stringType().noDefault()
            .endRecord();

    /**
     * This method supports the Avro framework and is not intended to be called
     * directly by the user.
     *
     * @return The schema for the class.
     */
    public static Schema getClassSchema() {
        return schema$;
    }

    /**
     * A set of string constants for the {@link AlterTableMonitorRequest}
     * parameter {@link #getMonitorUpdatesMap() monitorUpdatesMap}.
     * <p>
     * Map containing the properties of the table monitor to be updated. Error
     * if empty.
     */
    public static final class MonitorUpdatesMap {
        /**
         * Updates the schema name.  If {@link MonitorUpdatesMap#SCHEMA_NAME
         * SCHEMA_NAME} doesn't exist, an error will be thrown. If {@link
         * MonitorUpdatesMap#SCHEMA_NAME SCHEMA_NAME} is empty, then the user's
         * default schema will be used.
         */
        public static final String SCHEMA_NAME = "schema_name";

        private MonitorUpdatesMap() {  }
    }

    private String topicId;
    private Map<String, String> monitorUpdatesMap;
    private Map<String, String> options;

    /**
     * Constructs an AlterTableMonitorRequest object with default parameters.
     */
    public AlterTableMonitorRequest() {
        topicId = "";
        monitorUpdatesMap = new LinkedHashMap<>();
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs an AlterTableMonitorRequest object with the specified
     * parameters.
     *
     * @param topicId  The topic ID returned by {@link
     *                 com.gpudb.GPUdb#createTableMonitor(CreateTableMonitorRequest)
     *                 GPUdb.createTableMonitor}.
     * @param monitorUpdatesMap  Map containing the properties of the table
     *                           monitor to be updated. Error if empty.
     *                           <ul>
     *                               <li>{@link MonitorUpdatesMap#SCHEMA_NAME
     *                                   SCHEMA_NAME}: Updates the schema name.
     *                                   If {@link
     *                                   MonitorUpdatesMap#SCHEMA_NAME
     *                                   SCHEMA_NAME} doesn't exist, an error
     *                                   will be thrown. If {@link
     *                                   MonitorUpdatesMap#SCHEMA_NAME
     *                                   SCHEMA_NAME} is empty, then the user's
     *                                   default schema will be used.
     *                           </ul>
     * @param options  Optional parameters.
     */
    public AlterTableMonitorRequest(String topicId, Map<String, String> monitorUpdatesMap, Map<String, String> options) {
        this.topicId = (topicId == null) ? "" : topicId;
        this.monitorUpdatesMap = (monitorUpdatesMap == null) ? new LinkedHashMap<String, String>() : monitorUpdatesMap;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * The topic ID returned by {@link
     * com.gpudb.GPUdb#createTableMonitor(CreateTableMonitorRequest)
     * GPUdb.createTableMonitor}.
     *
     * @return The current value of {@code topicId}.
     */
    public String getTopicId() {
        return topicId;
    }

    /**
     * The topic ID returned by {@link
     * com.gpudb.GPUdb#createTableMonitor(CreateTableMonitorRequest)
     * GPUdb.createTableMonitor}.
     *
     * @param topicId  The new value for {@code topicId}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AlterTableMonitorRequest setTopicId(String topicId) {
        this.topicId = (topicId == null) ? "" : topicId;
        return this;
    }

    /**
     * Map containing the properties of the table monitor to be updated. Error
     * if empty.
     * <ul>
     *     <li>{@link MonitorUpdatesMap#SCHEMA_NAME SCHEMA_NAME}: Updates the
     *         schema name.  If {@link MonitorUpdatesMap#SCHEMA_NAME
     *         SCHEMA_NAME} doesn't exist, an error will be thrown. If {@link
     *         MonitorUpdatesMap#SCHEMA_NAME SCHEMA_NAME} is empty, then the
     *         user's default schema will be used.
     * </ul>
     *
     * @return The current value of {@code monitorUpdatesMap}.
     */
    public Map<String, String> getMonitorUpdatesMap() {
        return monitorUpdatesMap;
    }

    /**
     * Map containing the properties of the table monitor to be updated. Error
     * if empty.
     * <ul>
     *     <li>{@link MonitorUpdatesMap#SCHEMA_NAME SCHEMA_NAME}: Updates the
     *         schema name.  If {@link MonitorUpdatesMap#SCHEMA_NAME
     *         SCHEMA_NAME} doesn't exist, an error will be thrown. If {@link
     *         MonitorUpdatesMap#SCHEMA_NAME SCHEMA_NAME} is empty, then the
     *         user's default schema will be used.
     * </ul>
     *
     * @param monitorUpdatesMap  The new value for {@code monitorUpdatesMap}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AlterTableMonitorRequest setMonitorUpdatesMap(Map<String, String> monitorUpdatesMap) {
        this.monitorUpdatesMap = (monitorUpdatesMap == null) ? new LinkedHashMap<String, String>() : monitorUpdatesMap;
        return this;
    }

    /**
     * Optional parameters.
     *
     * @return The current value of {@code options}.
     */
    public Map<String, String> getOptions() {
        return options;
    }

    /**
     * Optional parameters.
     *
     * @param options  The new value for {@code options}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AlterTableMonitorRequest setOptions(Map<String, String> options) {
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
        return this;
    }

    /**
     * This method supports the Avro framework and is not intended to be called
     * directly by the user.
     *
     * @return The schema object describing this class.
     */
    @Override
    public Schema getSchema() {
        return schema$;
    }

    /**
     * This method supports the Avro framework and is not intended to be called
     * directly by the user.
     *
     * @param index  the position of the field to get
     *
     * @return value of the field with the given index.
     *
     * @throws IndexOutOfBoundsException
     */
    @Override
    public Object get(int index) {
        switch (index) {
            case 0:
                return this.topicId;

            case 1:
                return this.monitorUpdatesMap;

            case 2:
                return this.options;

            default:
                throw new IndexOutOfBoundsException("Invalid index specified.");
        }
    }

    /**
     * This method supports the Avro framework and is not intended to be called
     * directly by the user.
     *
     * @param index  the position of the field to set
     * @param value  the value to set
     *
     * @throws IndexOutOfBoundsException
     */
    @Override
    @SuppressWarnings("unchecked")
    public void put(int index, Object value) {
        switch (index) {
            case 0:
                this.topicId = (String)value;
                break;

            case 1:
                this.monitorUpdatesMap = (Map<String, String>)value;
                break;

            case 2:
                this.options = (Map<String, String>)value;
                break;

            default:
                throw new IndexOutOfBoundsException("Invalid index specified.");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if( obj == this ) {
            return true;
        }

        if( (obj == null) || (obj.getClass() != this.getClass()) ) {
            return false;
        }

        AlterTableMonitorRequest that = (AlterTableMonitorRequest)obj;

        return ( this.topicId.equals( that.topicId )
                 && this.monitorUpdatesMap.equals( that.monitorUpdatesMap )
                 && this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "topicId" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.topicId ) );
        builder.append( ", " );
        builder.append( gd.toString( "monitorUpdatesMap" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.monitorUpdatesMap ) );
        builder.append( ", " );
        builder.append( gd.toString( "options" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.options ) );
        builder.append( "}" );

        return builder.toString();
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = (31 * hashCode) + this.topicId.hashCode();
        hashCode = (31 * hashCode) + this.monitorUpdatesMap.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
