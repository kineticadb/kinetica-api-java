/*
 *  This file was autogenerated by the Kinetica schema processor.
 *
 *  DO NOT EDIT DIRECTLY.
 */
package com.gpudb.protocol;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;

/**
 * A set of results returned by {@link
 * com.gpudb.GPUdb#executeSqlRaw(ExecuteSqlRequest) GPUdb.executeSqlRaw}.
 */
public class RawExecuteSqlResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("RawExecuteSqlResponse")
            .namespace("com.gpudb")
            .fields()
                .name("countAffected").type().longType().noDefault()
                .name("responseSchemaStr").type().stringType().noDefault()
                .name("binaryEncodedResponse").type().bytesType().noDefault()
                .name("jsonEncodedResponse").type().stringType().noDefault()
                .name("totalNumberOfRecords").type().longType().noDefault()
                .name("hasMoreRecords").type().booleanType().noDefault()
                .name("pagingTable").type().stringType().noDefault()
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
     * A set of string constants for the {@link RawExecuteSqlResponse}
     * parameter {@link #getHasMoreRecords() hasMoreRecords}.
     * <p>
     * Too many records. Returned a partial set.
     */
    public static final class HasMoreRecords {
        public static final String TRUE = "true";
        public static final String FALSE = "false";

        private HasMoreRecords() {  }
    }

    /**
     * A set of string constants for the {@link RawExecuteSqlResponse}
     * parameter {@link #getInfo() info}.
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
    private String responseSchemaStr;
    private ByteBuffer binaryEncodedResponse;
    private String jsonEncodedResponse;
    private long totalNumberOfRecords;
    private boolean hasMoreRecords;
    private String pagingTable;
    private Map<String, String> info;

    /**
     * Constructs a RawExecuteSqlResponse object with default parameters.
     */
    public RawExecuteSqlResponse() {
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
    public RawExecuteSqlResponse setCountAffected(long countAffected) {
        this.countAffected = countAffected;
        return this;
    }

    /**
     * Avro schema of {@link #getBinaryEncodedResponse() binaryEncodedResponse}
     * or {@link #getJsonEncodedResponse() jsonEncodedResponse}.
     *
     * @return The current value of {@code responseSchemaStr}.
     */
    public String getResponseSchemaStr() {
        return responseSchemaStr;
    }

    /**
     * Avro schema of {@link #getBinaryEncodedResponse() binaryEncodedResponse}
     * or {@link #getJsonEncodedResponse() jsonEncodedResponse}.
     *
     * @param responseSchemaStr  The new value for {@code responseSchemaStr}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public RawExecuteSqlResponse setResponseSchemaStr(String responseSchemaStr) {
        this.responseSchemaStr = (responseSchemaStr == null) ? "" : responseSchemaStr;
        return this;
    }

    /**
     * Avro binary encoded response.
     *
     * @return The current value of {@code binaryEncodedResponse}.
     */
    public ByteBuffer getBinaryEncodedResponse() {
        return binaryEncodedResponse;
    }

    /**
     * Avro binary encoded response.
     *
     * @param binaryEncodedResponse  The new value for {@code
     *                               binaryEncodedResponse}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public RawExecuteSqlResponse setBinaryEncodedResponse(ByteBuffer binaryEncodedResponse) {
        this.binaryEncodedResponse = (binaryEncodedResponse == null) ? ByteBuffer.wrap( new byte[0] ) : binaryEncodedResponse;
        return this;
    }

    /**
     * Avro JSON encoded response.
     *
     * @return The current value of {@code jsonEncodedResponse}.
     */
    public String getJsonEncodedResponse() {
        return jsonEncodedResponse;
    }

    /**
     * Avro JSON encoded response.
     *
     * @param jsonEncodedResponse  The new value for {@code
     *                             jsonEncodedResponse}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public RawExecuteSqlResponse setJsonEncodedResponse(String jsonEncodedResponse) {
        this.jsonEncodedResponse = (jsonEncodedResponse == null) ? "" : jsonEncodedResponse;
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
    public RawExecuteSqlResponse setTotalNumberOfRecords(long totalNumberOfRecords) {
        this.totalNumberOfRecords = totalNumberOfRecords;
        return this;
    }

    /**
     * Too many records. Returned a partial set.
     * Supported values:
     * <ul>
     *     <li>{@link HasMoreRecords#TRUE TRUE}
     *     <li>{@link HasMoreRecords#FALSE FALSE}
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
     *     <li>{@link HasMoreRecords#TRUE TRUE}
     *     <li>{@link HasMoreRecords#FALSE FALSE}
     * </ul>
     *
     * @param hasMoreRecords  The new value for {@code hasMoreRecords}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public RawExecuteSqlResponse setHasMoreRecords(boolean hasMoreRecords) {
        this.hasMoreRecords = hasMoreRecords;
        return this;
    }

    /**
     * Name of the table that has the result records of the query. Valid, when
     * {@link #getHasMoreRecords() hasMoreRecords} is {@link
     * HasMoreRecords#TRUE TRUE} (Subject to config.paging_tables_enabled)
     *
     * @return The current value of {@code pagingTable}.
     */
    public String getPagingTable() {
        return pagingTable;
    }

    /**
     * Name of the table that has the result records of the query. Valid, when
     * {@link #getHasMoreRecords() hasMoreRecords} is {@link
     * HasMoreRecords#TRUE TRUE} (Subject to config.paging_tables_enabled)
     *
     * @param pagingTable  The new value for {@code pagingTable}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public RawExecuteSqlResponse setPagingTable(String pagingTable) {
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
    public RawExecuteSqlResponse setInfo(Map<String, String> info) {
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
                return this.countAffected;

            case 1:
                return this.responseSchemaStr;

            case 2:
                return this.binaryEncodedResponse;

            case 3:
                return this.jsonEncodedResponse;

            case 4:
                return this.totalNumberOfRecords;

            case 5:
                return this.hasMoreRecords;

            case 6:
                return this.pagingTable;

            case 7:
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
                this.countAffected = (Long)value;
                break;

            case 1:
                this.responseSchemaStr = (String)value;
                break;

            case 2:
                this.binaryEncodedResponse = (ByteBuffer)value;
                break;

            case 3:
                this.jsonEncodedResponse = (String)value;
                break;

            case 4:
                this.totalNumberOfRecords = (Long)value;
                break;

            case 5:
                this.hasMoreRecords = (Boolean)value;
                break;

            case 6:
                this.pagingTable = (String)value;
                break;

            case 7:
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

        RawExecuteSqlResponse that = (RawExecuteSqlResponse)obj;

        return ( ( this.countAffected == that.countAffected )
                 && this.responseSchemaStr.equals( that.responseSchemaStr )
                 && this.binaryEncodedResponse.equals( that.binaryEncodedResponse )
                 && this.jsonEncodedResponse.equals( that.jsonEncodedResponse )
                 && ( this.totalNumberOfRecords == that.totalNumberOfRecords )
                 && ( this.hasMoreRecords == that.hasMoreRecords )
                 && this.pagingTable.equals( that.pagingTable )
                 && this.info.equals( that.info ) );
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
        builder.append( gd.toString( "responseSchemaStr" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.responseSchemaStr ) );
        builder.append( ", " );
        builder.append( gd.toString( "binaryEncodedResponse" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.binaryEncodedResponse ) );
        builder.append( ", " );
        builder.append( gd.toString( "jsonEncodedResponse" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.jsonEncodedResponse ) );
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
        builder.append( "}" );

        return builder.toString();
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = (31 * hashCode) + ((Long)this.countAffected).hashCode();
        hashCode = (31 * hashCode) + this.responseSchemaStr.hashCode();
        hashCode = (31 * hashCode) + this.binaryEncodedResponse.hashCode();
        hashCode = (31 * hashCode) + this.jsonEncodedResponse.hashCode();
        hashCode = (31 * hashCode) + ((Long)this.totalNumberOfRecords).hashCode();
        hashCode = (31 * hashCode) + ((Boolean)this.hasMoreRecords).hashCode();
        hashCode = (31 * hashCode) + this.pagingTable.hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }
}
