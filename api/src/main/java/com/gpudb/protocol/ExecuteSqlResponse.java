/*
 *  This file was autogenerated by the Kinetica schema processor.
 *
 *  DO NOT EDIT DIRECTLY.
 */
package com.gpudb.protocol;

import com.gpudb.Record;
import com.gpudb.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.avro.generic.GenericData;

/**
 * A set of results returned by {@link
 * com.gpudb.GPUdb#executeSql(ExecuteSqlRequest) GPUdb.executeSql}.
 */
public class ExecuteSqlResponse {
    /**
     * A set of string constants for the {@link ExecuteSqlResponse} parameter
     * {@link #getHasMoreRecords() hasMoreRecords}.
     * <p>
     * Too many records. Returned a partial set.
     */
    public static final class HasMoreRecords {
        public static final String TRUE = "true";
        public static final String FALSE = "false";

        private HasMoreRecords() {  }
    }

    /**
     * A set of string constants for the {@link ExecuteSqlResponse} parameter
     * {@link #getInfo() info}.
     * <p>
     * Additional information.
     */
    public static final class Info {
        /**
         * Number of records in the final table
         */
        public static final String COUNT = "count";

        private Info() {  }
    }

    private long countAffected;
    private List<Record> data;
    private long totalNumberOfRecords;
    private boolean hasMoreRecords;
    private String pagingTable;
    private Map<String, String> info;
    private Type dataType;

    /**
     * Constructs an ExecuteSqlResponse object with default parameters.
     */
    public ExecuteSqlResponse() {
    }

    /**
     * The number of objects/records affected.
     *
     * @return The current value of {@code countAffected}.
     */
    public long getCountAffected() {
        return countAffected;
    }

    /**
     * The number of objects/records affected.
     *
     * @param countAffected  The new value for {@code countAffected}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public ExecuteSqlResponse setCountAffected(long countAffected) {
        this.countAffected = countAffected;
        return this;
    }

    /**
     * Avro binary encoded response.
     *
     * @return The current value of {@code data}.
     */
    public List<Record> getData() {
        return data;
    }

    /**
     * Avro binary encoded response.
     *
     * @param data  The new value for {@code data}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public ExecuteSqlResponse setData(List<Record> data) {
        this.data = (data == null) ? new ArrayList<Record>() : data;
        return this;
    }

    /**
     * Total/Filtered number of records.
     *
     * @return The current value of {@code totalNumberOfRecords}.
     */
    public long getTotalNumberOfRecords() {
        return totalNumberOfRecords;
    }

    /**
     * Total/Filtered number of records.
     *
     * @param totalNumberOfRecords  The new value for {@code
     *                              totalNumberOfRecords}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public ExecuteSqlResponse setTotalNumberOfRecords(long totalNumberOfRecords) {
        this.totalNumberOfRecords = totalNumberOfRecords;
        return this;
    }

    /**
     * Too many records. Returned a partial set.
     * Supported values:
     * <ul>
     *     <li>{@code true}
     *     <li>{@code false}
     * </ul>
     *
     * @return The current value of {@code hasMoreRecords}.
     */
    public boolean getHasMoreRecords() {
        return hasMoreRecords;
    }

    /**
     * Too many records. Returned a partial set.
     * Supported values:
     * <ul>
     *     <li>{@code true}
     *     <li>{@code false}
     * </ul>
     *
     * @param hasMoreRecords  The new value for {@code hasMoreRecords}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public ExecuteSqlResponse setHasMoreRecords(boolean hasMoreRecords) {
        this.hasMoreRecords = hasMoreRecords;
        return this;
    }

    /**
     * Name of the table that has the result records of the query. Valid, when
     * {@link #getHasMoreRecords() hasMoreRecords} is {@link
     * HasMoreRecords#TRUE TRUE}
     *
     * @return The current value of {@code pagingTable}.
     */
    public String getPagingTable() {
        return pagingTable;
    }

    /**
     * Name of the table that has the result records of the query. Valid, when
     * {@link #getHasMoreRecords() hasMoreRecords} is {@link
     * HasMoreRecords#TRUE TRUE}
     *
     * @param pagingTable  The new value for {@code pagingTable}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public ExecuteSqlResponse setPagingTable(String pagingTable) {
        this.pagingTable = (pagingTable == null) ? "" : pagingTable;
        return this;
    }

    /**
     * Additional information.
     * <ul>
     *     <li>{@link Info#COUNT COUNT}: Number of records in the final table
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
     *     <li>{@link Info#COUNT COUNT}: Number of records in the final table
     * </ul>
     * The default value is an empty {@link Map}.
     *
     * @param info  The new value for {@code info}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public ExecuteSqlResponse setInfo(Map<String, String> info) {
        this.info = (info == null) ? new LinkedHashMap<String, String>() : info;
        return this;
    }

    /**
     * The {@link Type} object containing the type of the dynamically generated
     * data.
     *
     * @return The current value of {@code dataType}.
     */
    public Type getDataType() {
        return dataType;
    }

    /**
     * The {@link Type} object containing the type of the dynamically generated
     * data.
     *
     * @param dataType  The new value for {@code dataType}.
     *
     * @return {@code this} to mimic the builder pattern.
     *
     */
    public ExecuteSqlResponse setDataType(Type dataType) {
        this.dataType = dataType;
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

        ExecuteSqlResponse that = (ExecuteSqlResponse)obj;

        return ( ( this.countAffected == that.countAffected )
                 && this.data.equals( that.data )
                 && ( this.totalNumberOfRecords == that.totalNumberOfRecords )
                 && ( this.hasMoreRecords == that.hasMoreRecords )
                 && this.pagingTable.equals( that.pagingTable )
                 && this.info.equals( that.info )
                 && this.dataType.equals( that.dataType ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "countAffected" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.countAffected ) );
        builder.append( ", " );
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
        builder.append( ", " );
        builder.append( gd.toString( "pagingTable" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.pagingTable ) );
        builder.append( ", " );
        builder.append( gd.toString( "info" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.info ) );
        builder.append( ", " );
        builder.append( gd.toString( "dataType" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.dataType ) );
        builder.append( "}" );

        return builder.toString();
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = (31 * hashCode) + ((Long)this.countAffected).hashCode();
        hashCode = (31 * hashCode) + this.data.hashCode();
        hashCode = (31 * hashCode) + ((Long)this.totalNumberOfRecords).hashCode();
        hashCode = (31 * hashCode) + ((Boolean)this.hasMoreRecords).hashCode();
        hashCode = (31 * hashCode) + this.pagingTable.hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        hashCode = (31 * hashCode) + this.dataType.hashCode();
        return hashCode;
    }
}
