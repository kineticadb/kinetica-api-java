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
 * A set of results returned by {@link
 * com.gpudb.GPUdb#filterByArea(FilterByAreaRequest) GPUdb.filterByArea}.
 */
public class FilterByAreaResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("FilterByAreaResponse")
            .namespace("com.gpudb")
            .fields()
                .name("count").type().longType().noDefault()
                .name("info").type().map().values().stringType().noDefault()
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
     * A set of string constants for the {@link FilterByAreaResponse} parameter
     * {@link #getInfo() info}.
     * <p>
     * Additional information.
     */
    public static final class Info {
        /**
         * The fully qualified name of the view (i.e.&nbsp;including the
         * schema)
         */
        public static final String QUALIFIED_VIEW_NAME = "qualified_view_name";

        private Info() {  }
    }

    private long count;
    private Map<String, String> info;

    /**
     * Constructs a FilterByAreaResponse object with default parameters.
     */
    public FilterByAreaResponse() {
    }

    /**
     * The number of records passing the area filter.
     *
     * @return The current value of {@code count}.
     */
    public long getCount() {
        return count;
    }

    /**
     * The number of records passing the area filter.
     *
     * @param count  The new value for {@code count}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public FilterByAreaResponse setCount(long count) {
        this.count = count;
        return this;
    }

    /**
     * Additional information.
     * <ul>
     *     <li>{@link Info#QUALIFIED_VIEW_NAME QUALIFIED_VIEW_NAME}: The fully
     *         qualified name of the view (i.e. including the schema)
     * </ul>
     * The default value is an empty {@link Map}.
     *
     * @return The current value of {@code info}.
     */
    public Map<String, String> getInfo() {
        return info;
    }

    /**
     * Additional information.
     * <ul>
     *     <li>{@link Info#QUALIFIED_VIEW_NAME QUALIFIED_VIEW_NAME}: The fully
     *         qualified name of the view (i.e. including the schema)
     * </ul>
     * The default value is an empty {@link Map}.
     *
     * @param info  The new value for {@code info}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public FilterByAreaResponse setInfo(Map<String, String> info) {
        this.info = (info == null) ? new LinkedHashMap<String, String>() : info;
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

        FilterByAreaResponse that = (FilterByAreaResponse)obj;

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
