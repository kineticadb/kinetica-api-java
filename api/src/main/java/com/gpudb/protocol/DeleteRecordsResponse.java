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
 * A set of results returned by {@link
 * com.gpudb.GPUdb#deleteRecords(DeleteRecordsRequest) GPUdb.deleteRecords}.
 */
public class DeleteRecordsResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("DeleteRecordsResponse")
            .namespace("com.gpudb")
            .fields()
                .name("countDeleted").type().longType().noDefault()
                .name("countsDeleted").type().array().items().longType().noDefault()
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

    private long countDeleted;
    private List<Long> countsDeleted;
    private Map<String, String> info;

    /**
     * Constructs a DeleteRecordsResponse object with default parameters.
     */
    public DeleteRecordsResponse() {
    }

    /**
     * Total number of records deleted across all expressions.
     *
     * @return The current value of {@code countDeleted}.
     */
    public long getCountDeleted() {
        return countDeleted;
    }

    /**
     * Total number of records deleted across all expressions.
     *
     * @param countDeleted  The new value for {@code countDeleted}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public DeleteRecordsResponse setCountDeleted(long countDeleted) {
        this.countDeleted = countDeleted;
        return this;
    }

    /**
     * Total number of records deleted per expression.
     *
     * @return The current value of {@code countsDeleted}.
     */
    public List<Long> getCountsDeleted() {
        return countsDeleted;
    }

    /**
     * Total number of records deleted per expression.
     *
     * @param countsDeleted  The new value for {@code countsDeleted}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public DeleteRecordsResponse setCountsDeleted(List<Long> countsDeleted) {
        this.countsDeleted = (countsDeleted == null) ? new ArrayList<Long>() : countsDeleted;
        return this;
    }

    /**
     * Additional information.
     *
     * @return The current value of {@code info}.
     */
    public Map<String, String> getInfo() {
        return info;
    }

    /**
     * Additional information.
     *
     * @param info  The new value for {@code info}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public DeleteRecordsResponse setInfo(Map<String, String> info) {
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
                return this.countDeleted;

            case 1:
                return this.countsDeleted;

            case 2:
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
                this.countDeleted = (Long)value;
                break;

            case 1:
                this.countsDeleted = (List<Long>)value;
                break;

            case 2:
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

        DeleteRecordsResponse that = (DeleteRecordsResponse)obj;

        return ( ( this.countDeleted == that.countDeleted )
                 && this.countsDeleted.equals( that.countsDeleted )
                 && this.info.equals( that.info ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "countDeleted" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.countDeleted ) );
        builder.append( ", " );
        builder.append( gd.toString( "countsDeleted" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.countsDeleted ) );
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
        hashCode = (31 * hashCode) + ((Long)this.countDeleted).hashCode();
        hashCode = (31 * hashCode) + this.countsDeleted.hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }
}
