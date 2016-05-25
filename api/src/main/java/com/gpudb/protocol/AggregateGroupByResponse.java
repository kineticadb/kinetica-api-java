/*
 *  This file was autogenerated by the GPUdb schema processor.
 *
 *  DO NOT EDIT DIRECTLY.
 */
package com.gpudb.protocol;

import com.gpudb.Record;
import java.util.ArrayList;
import java.util.List;
import org.apache.avro.generic.GenericData;


/**
 * A set of results returned by {@link
 * com.gpudb.GPUdb#aggregateGroupBy(AggregateGroupByRequest)}.
 */
public class AggregateGroupByResponse {
    private List<Record> data;
    private long totalNumberOfRecords;
    private boolean hasMoreRecords;


    /**
     * Constructs an AggregateGroupByResponse object with default parameters.
     */
    public AggregateGroupByResponse() {
    }

    /**
     * 
     * @return Avro binary encoded response.
     * 
     */
    public List<Record> getData() {
        return data;
    }

    /**
     * 
     * @param data  Avro binary encoded response.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public AggregateGroupByResponse setData(List<Record> data) {
        this.data = (data == null) ? new ArrayList<Record>() : data;
        return this;
    }

    /**
     * 
     * @return Total/Filtered number of records.
     * 
     */
    public long getTotalNumberOfRecords() {
        return totalNumberOfRecords;
    }

    /**
     * 
     * @param totalNumberOfRecords  Total/Filtered number of records.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public AggregateGroupByResponse setTotalNumberOfRecords(long totalNumberOfRecords) {
        this.totalNumberOfRecords = totalNumberOfRecords;
        return this;
    }

    /**
     * 
     * @return Too many records. Returned a partial set.
     * 
     */
    public boolean getHasMoreRecords() {
        return hasMoreRecords;
    }

    /**
     * 
     * @param hasMoreRecords  Too many records. Returned a partial set.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public AggregateGroupByResponse setHasMoreRecords(boolean hasMoreRecords) {
        this.hasMoreRecords = hasMoreRecords;
        return this;
    }
    @Override
    public boolean equals(Object obj) {
        if( obj == this ) {
            return true;
        }

        if( (obj == null) || (obj.getClass() != this.getClass()) ) {
            return false;
        }

        AggregateGroupByResponse that = (AggregateGroupByResponse)obj;

        return ( this.data.equals( that.data )
                 && ( this.totalNumberOfRecords == that.totalNumberOfRecords )
                 && ( this.hasMoreRecords == that.hasMoreRecords ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "data" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.data ) );
        builder.append( ", " );
        builder.append( gd.toString( "totalNumberOfRecords" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.totalNumberOfRecords ) );
        builder.append( ", " );
        builder.append( gd.toString( "hasMoreRecords" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.hasMoreRecords ) );
        builder.append( "}" );

        return builder.toString();
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = (31 * hashCode) + this.data.hashCode();
        hashCode = (31 * hashCode) + ((Long)this.totalNumberOfRecords).hashCode();
        hashCode = (31 * hashCode) + ((Boolean)this.hasMoreRecords).hashCode();
        return hashCode;
    }

}
