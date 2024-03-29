/*
 *  This file was autogenerated by the Kinetica schema processor.
 *
 *  DO NOT EDIT DIRECTLY.
 */
package com.gpudb.protocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;

/**
 * A set of parameters for {@link
 * com.gpudb.GPUdb#showTableMonitors(ShowTableMonitorsRequest)
 * GPUdb.showTableMonitors}.
 * <p>
 * Show table monitors and their properties. Table monitors are created using
 * {@link com.gpudb.GPUdb#createTableMonitor(CreateTableMonitorRequest)
 * GPUdb.createTableMonitor}.
 * Returns detailed information about existing table monitors.
 */
public class ShowTableMonitorsRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("ShowTableMonitorsRequest")
            .namespace("com.gpudb")
            .fields()
                .name("monitorIds").type().array().items().stringType().noDefault()
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

    private List<String> monitorIds;
    private Map<String, String> options;

    /**
     * Constructs a ShowTableMonitorsRequest object with default parameters.
     */
    public ShowTableMonitorsRequest() {
        monitorIds = new ArrayList<>();
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs a ShowTableMonitorsRequest object with the specified
     * parameters.
     *
     * @param monitorIds  List of monitors to be shown. An empty list or a
     *                    single entry with an empty string returns all table
     *                    monitors.
     * @param options  Optional parameters. The default value is an empty
     *                 {@link Map}.
     */
    public ShowTableMonitorsRequest(List<String> monitorIds, Map<String, String> options) {
        this.monitorIds = (monitorIds == null) ? new ArrayList<String>() : monitorIds;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * List of monitors to be shown. An empty list or a single entry with an
     * empty string returns all table monitors.
     *
     * @return The current value of {@code monitorIds}.
     */
    public List<String> getMonitorIds() {
        return monitorIds;
    }

    /**
     * List of monitors to be shown. An empty list or a single entry with an
     * empty string returns all table monitors.
     *
     * @param monitorIds  The new value for {@code monitorIds}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public ShowTableMonitorsRequest setMonitorIds(List<String> monitorIds) {
        this.monitorIds = (monitorIds == null) ? new ArrayList<String>() : monitorIds;
        return this;
    }

    /**
     * Optional parameters. The default value is an empty {@link Map}.
     *
     * @return The current value of {@code options}.
     */
    public Map<String, String> getOptions() {
        return options;
    }

    /**
     * Optional parameters. The default value is an empty {@link Map}.
     *
     * @param options  The new value for {@code options}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public ShowTableMonitorsRequest setOptions(Map<String, String> options) {
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
                return this.monitorIds;

            case 1:
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
                this.monitorIds = (List<String>)value;
                break;

            case 1:
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

        ShowTableMonitorsRequest that = (ShowTableMonitorsRequest)obj;

        return ( this.monitorIds.equals( that.monitorIds )
                 && this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "monitorIds" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.monitorIds ) );
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
        hashCode = (31 * hashCode) + this.monitorIds.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
