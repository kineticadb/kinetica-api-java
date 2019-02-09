/*
 *  This file was autogenerated by the GPUdb schema processor.
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
 * A set of results returned by {@link
 * com.gpudb.GPUdb#adminShowAlerts(AdminShowAlertsRequest)}.
 */
public class AdminShowAlertsResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("AdminShowAlertsResponse")
            .namespace("com.gpudb")
            .fields()
                .name("timestamps").type().array().items().stringType().noDefault()
                .name("types").type().array().items().stringType().noDefault()
                .name("params").type().array().items().map().values().stringType().noDefault()
                .name("info").type().map().values().stringType().noDefault()
            .endRecord();


    /**
     * This method supports the Avro framework and is not intended to be called
     * directly by the user.
     * 
     * @return  the schema for the class.
     * 
     */
    public static Schema getClassSchema() {
        return schema$;
    }

    private List<String> timestamps;
    private List<String> types;
    private List<Map<String, String>> params;
    private Map<String, String> info;


    /**
     * Constructs an AdminShowAlertsResponse object with default parameters.
     */
    public AdminShowAlertsResponse() {
    }

    /**
     * 
     * @return Timestamp for when the alert occurred, sorted from most recent
     *         to least recent. Each array entry corresponds with the entries
     *         at the same index in {@code types} and {@code params}.
     * 
     */
    public List<String> getTimestamps() {
        return timestamps;
    }

    /**
     * 
     * @param timestamps  Timestamp for when the alert occurred, sorted from
     *                    most recent to least recent. Each array entry
     *                    corresponds with the entries at the same index in
     *                    {@code types} and {@code params}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public AdminShowAlertsResponse setTimestamps(List<String> timestamps) {
        this.timestamps = (timestamps == null) ? new ArrayList<String>() : timestamps;
        return this;
    }

    /**
     * 
     * @return Type of system alert, sorted from most recent to least recent.
     *         Each array entry corresponds with the entries at the same index
     *         in {@code timestamps} and {@code params}.
     * 
     */
    public List<String> getTypes() {
        return types;
    }

    /**
     * 
     * @param types  Type of system alert, sorted from most recent to least
     *               recent. Each array entry corresponds with the entries at
     *               the same index in {@code timestamps} and {@code params}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public AdminShowAlertsResponse setTypes(List<String> types) {
        this.types = (types == null) ? new ArrayList<String>() : types;
        return this;
    }

    /**
     * 
     * @return Parameters for each alert, sorted from most recent to least
     *         recent. Each array entry corresponds with the entries at the
     *         same index in {@code timestamps} and {@code types}.
     * 
     */
    public List<Map<String, String>> getParams() {
        return params;
    }

    /**
     * 
     * @param params  Parameters for each alert, sorted from most recent to
     *                least recent. Each array entry corresponds with the
     *                entries at the same index in {@code timestamps} and
     *                {@code types}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public AdminShowAlertsResponse setParams(List<Map<String, String>> params) {
        this.params = (params == null) ? new ArrayList<Map<String, String>>() : params;
        return this;
    }

    /**
     * 
     * @return Additional information.
     * 
     */
    public Map<String, String> getInfo() {
        return info;
    }

    /**
     * 
     * @param info  Additional information.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public AdminShowAlertsResponse setInfo(Map<String, String> info) {
        this.info = (info == null) ? new LinkedHashMap<String, String>() : info;
        return this;
    }

    /**
     * This method supports the Avro framework and is not intended to be called
     * directly by the user.
     * 
     * @return the schema object describing this class.
     * 
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
     * 
     */
    @Override
    public Object get(int index) {
        switch (index) {
            case 0:
                return this.timestamps;

            case 1:
                return this.types;

            case 2:
                return this.params;

            case 3:
                return this.info;

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
     * 
     */
    @Override
    @SuppressWarnings("unchecked")
    public void put(int index, Object value) {
        switch (index) {
            case 0:
                this.timestamps = (List<String>)value;
                break;

            case 1:
                this.types = (List<String>)value;
                break;

            case 2:
                this.params = (List<Map<String, String>>)value;
                break;

            case 3:
                this.info = (Map<String, String>)value;
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

        AdminShowAlertsResponse that = (AdminShowAlertsResponse)obj;

        return ( this.timestamps.equals( that.timestamps )
                 && this.types.equals( that.types )
                 && this.params.equals( that.params )
                 && this.info.equals( that.info ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "timestamps" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.timestamps ) );
        builder.append( ", " );
        builder.append( gd.toString( "types" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.types ) );
        builder.append( ", " );
        builder.append( gd.toString( "params" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.params ) );
        builder.append( ", " );
        builder.append( gd.toString( "info" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.info ) );
        builder.append( "}" );

        return builder.toString();
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = (31 * hashCode) + this.timestamps.hashCode();
        hashCode = (31 * hashCode) + this.types.hashCode();
        hashCode = (31 * hashCode) + this.params.hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }

}