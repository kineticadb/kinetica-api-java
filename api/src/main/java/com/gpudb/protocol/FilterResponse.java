/*
 *  This file was autogenerated by the GPUdb schema processor.
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
 * A set of results returned by {@link com.gpudb.GPUdb#filter(FilterRequest)}.
 */
public class FilterResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("FilterResponse")
            .namespace("com.gpudb")
            .fields()
                .name("count").type().longType().noDefault()
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


    /**
     * Additional information.
     * <ul>
     *         <li> {@link
     * com.gpudb.protocol.FilterResponse.Info#QUALIFIED_VIEW_NAME
     * QUALIFIED_VIEW_NAME}: The fully qualified name of the view (i.e.
     * including the schema)
     * </ul>
     * The default value is an empty {@link Map}.
     * A set of string constants for the parameter {@code info}.
     */
    public static final class Info {

        /**
         * The fully qualified name of the view (i.e. including the schema)
         */
        public static final String QUALIFIED_VIEW_NAME = "qualified_view_name";

        private Info() {  }
    }

    private long count;
    private Map<String, String> info;


    /**
     * Constructs a FilterResponse object with default parameters.
     */
    public FilterResponse() {
    }

    /**
     * 
     * @return The number of records that matched the given select expression.
     * 
     */
    public long getCount() {
        return count;
    }

    /**
     * 
     * @param count  The number of records that matched the given select
     *               expression.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public FilterResponse setCount(long count) {
        this.count = count;
        return this;
    }

    /**
     * 
     * @return Additional information.
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.FilterResponse.Info#QUALIFIED_VIEW_NAME
     *         QUALIFIED_VIEW_NAME}: The fully qualified name of the view (i.e.
     *         including the schema)
     *         </ul>
     *         The default value is an empty {@link Map}.
     * 
     */
    public Map<String, String> getInfo() {
        return info;
    }

    /**
     * 
     * @param info  Additional information.
     *              <ul>
     *                      <li> {@link
     *              com.gpudb.protocol.FilterResponse.Info#QUALIFIED_VIEW_NAME
     *              QUALIFIED_VIEW_NAME}: The fully qualified name of the view
     *              (i.e. including the schema)
     *              </ul>
     *              The default value is an empty {@link Map}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public FilterResponse setInfo(Map<String, String> info) {
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
                return this.count;

            case 1:
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
                this.count = (Long)value;
                break;

            case 1:
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

        FilterResponse that = (FilterResponse)obj;

        return ( ( this.count == that.count )
                 && this.info.equals( that.info ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "count" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.count ) );
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
        hashCode = (31 * hashCode) + ((Long)this.count).hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }

}
