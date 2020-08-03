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
 * A set of parameters for {@link
 * com.gpudb.GPUdb#getRecordsBySeriesRaw(GetRecordsBySeriesRequest)}.
 * <p>
 * Retrieves the complete series/track records from the given
 * {@code worldTableName} based on the partial track information contained in
 * the {@code tableName}.
 * <p>
 * This operation supports paging through the data via the {@code offset} and
 * {@code limit} parameters.
 * <p>
 * In contrast to {@link com.gpudb.GPUdb#getRecordsRaw(GetRecordsRequest)} this
 * returns records grouped by
 * series/track. So if {@code offset} is 0 and {@code limit} is 5 this
 * operation
 * would return the first 5 series/tracks in {@code tableName}. Each
 * series/track
 * will be returned sorted by their TIMESTAMP column.
 */
public class GetRecordsBySeriesRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("GetRecordsBySeriesRequest")
            .namespace("com.gpudb")
            .fields()
                .name("tableName").type().stringType().noDefault()
                .name("worldTableName").type().stringType().noDefault()
                .name("offset").type().intType().noDefault()
                .name("limit").type().intType().noDefault()
                .name("encoding").type().stringType().noDefault()
                .name("options").type().map().values().stringType().noDefault()
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
     * Specifies the encoding for returned records; either {@code binary} or
     * {@code json}.
     * Supported values:
     * <ul>
     *         <li> {@link
     * com.gpudb.protocol.GetRecordsBySeriesRequest.Encoding#BINARY BINARY}
     *         <li> {@link
     * com.gpudb.protocol.GetRecordsBySeriesRequest.Encoding#JSON JSON}
     * </ul>
     * The default value is {@link
     * com.gpudb.protocol.GetRecordsBySeriesRequest.Encoding#BINARY BINARY}.
     * A set of string constants for the parameter {@code encoding}.
     */
    public static final class Encoding {
        public static final String BINARY = "binary";
        public static final String JSON = "json";

        private Encoding() {  }
    }

    private String tableName;
    private String worldTableName;
    private int offset;
    private int limit;
    private String encoding;
    private Map<String, String> options;


    /**
     * Constructs a GetRecordsBySeriesRequest object with default parameters.
     */
    public GetRecordsBySeriesRequest() {
        tableName = "";
        worldTableName = "";
        encoding = Encoding.BINARY;
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs a GetRecordsBySeriesRequest object with the specified
     * parameters.
     * 
     * @param tableName  Name of the table or view for which series/tracks will
     *                   be fetched, in [schema_name.]table_name format, using
     *                   standard <a
     *                   href="../../../../../concepts/tables.html#table-name-resolution"
     *                   target="_top">name resolution rules</a>.
     * @param worldTableName  Name of the table containing the complete
     *                        series/track information to be returned for the
     *                        tracks present in the {@code tableName}, in
     *                        [schema_name.]table_name format, using standard
     *                        <a
     *                        href="../../../../../concepts/tables.html#table-name-resolution"
     *                        target="_top">name resolution rules</a>.
     *                        Typically this is used when retrieving
     *                        series/tracks from a view (which contains partial
     *                        series/tracks) but the user wants to retrieve the
     *                        entire original series/tracks. Can be blank.
     * @param offset  A positive integer indicating the number of initial
     *                series/tracks to skip (useful for paging through the
     *                results).  The default value is 0.The minimum allowed
     *                value is 0. The maximum allowed value is MAX_INT.
     * @param limit  A positive integer indicating the maximum number of
     *               series/tracks to be returned. Or END_OF_SET (-9999) to
     *               indicate that the max number of results should be
     *               returned.  The default value is 250.
     * @param options  Optional parameters.  The default value is an empty
     *                 {@link Map}.
     * 
     */
    public GetRecordsBySeriesRequest(String tableName, String worldTableName, int offset, int limit, Map<String, String> options) {
        this.tableName = (tableName == null) ? "" : tableName;
        this.worldTableName = (worldTableName == null) ? "" : worldTableName;
        this.offset = offset;
        this.limit = limit;
        this.encoding = Encoding.BINARY;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * Constructs a GetRecordsBySeriesRequest object with the specified
     * parameters.
     * 
     * @param tableName  Name of the table or view for which series/tracks will
     *                   be fetched, in [schema_name.]table_name format, using
     *                   standard <a
     *                   href="../../../../../concepts/tables.html#table-name-resolution"
     *                   target="_top">name resolution rules</a>.
     * @param worldTableName  Name of the table containing the complete
     *                        series/track information to be returned for the
     *                        tracks present in the {@code tableName}, in
     *                        [schema_name.]table_name format, using standard
     *                        <a
     *                        href="../../../../../concepts/tables.html#table-name-resolution"
     *                        target="_top">name resolution rules</a>.
     *                        Typically this is used when retrieving
     *                        series/tracks from a view (which contains partial
     *                        series/tracks) but the user wants to retrieve the
     *                        entire original series/tracks. Can be blank.
     * @param offset  A positive integer indicating the number of initial
     *                series/tracks to skip (useful for paging through the
     *                results).  The default value is 0.The minimum allowed
     *                value is 0. The maximum allowed value is MAX_INT.
     * @param limit  A positive integer indicating the maximum number of
     *               series/tracks to be returned. Or END_OF_SET (-9999) to
     *               indicate that the max number of results should be
     *               returned.  The default value is 250.
     * @param encoding  Specifies the encoding for returned records; either
     *                  {@code binary} or {@code json}.
     *                  Supported values:
     *                  <ul>
     *                          <li> {@link
     *                  com.gpudb.protocol.GetRecordsBySeriesRequest.Encoding#BINARY
     *                  BINARY}
     *                          <li> {@link
     *                  com.gpudb.protocol.GetRecordsBySeriesRequest.Encoding#JSON
     *                  JSON}
     *                  </ul>
     *                  The default value is {@link
     *                  com.gpudb.protocol.GetRecordsBySeriesRequest.Encoding#BINARY
     *                  BINARY}.
     * @param options  Optional parameters.  The default value is an empty
     *                 {@link Map}.
     * 
     */
    public GetRecordsBySeriesRequest(String tableName, String worldTableName, int offset, int limit, String encoding, Map<String, String> options) {
        this.tableName = (tableName == null) ? "" : tableName;
        this.worldTableName = (worldTableName == null) ? "" : worldTableName;
        this.offset = offset;
        this.limit = limit;
        this.encoding = (encoding == null) ? Encoding.BINARY : encoding;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * 
     * @return Name of the table or view for which series/tracks will be
     *         fetched, in [schema_name.]table_name format, using standard <a
     *         href="../../../../../concepts/tables.html#table-name-resolution"
     *         target="_top">name resolution rules</a>.
     * 
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * 
     * @param tableName  Name of the table or view for which series/tracks will
     *                   be fetched, in [schema_name.]table_name format, using
     *                   standard <a
     *                   href="../../../../../concepts/tables.html#table-name-resolution"
     *                   target="_top">name resolution rules</a>.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public GetRecordsBySeriesRequest setTableName(String tableName) {
        this.tableName = (tableName == null) ? "" : tableName;
        return this;
    }

    /**
     * 
     * @return Name of the table containing the complete series/track
     *         information to be returned for the tracks present in the {@code
     *         tableName}, in [schema_name.]table_name format, using standard
     *         <a
     *         href="../../../../../concepts/tables.html#table-name-resolution"
     *         target="_top">name resolution rules</a>.  Typically this is used
     *         when retrieving series/tracks from a view (which contains
     *         partial series/tracks) but the user wants to retrieve the entire
     *         original series/tracks. Can be blank.
     * 
     */
    public String getWorldTableName() {
        return worldTableName;
    }

    /**
     * 
     * @param worldTableName  Name of the table containing the complete
     *                        series/track information to be returned for the
     *                        tracks present in the {@code tableName}, in
     *                        [schema_name.]table_name format, using standard
     *                        <a
     *                        href="../../../../../concepts/tables.html#table-name-resolution"
     *                        target="_top">name resolution rules</a>.
     *                        Typically this is used when retrieving
     *                        series/tracks from a view (which contains partial
     *                        series/tracks) but the user wants to retrieve the
     *                        entire original series/tracks. Can be blank.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public GetRecordsBySeriesRequest setWorldTableName(String worldTableName) {
        this.worldTableName = (worldTableName == null) ? "" : worldTableName;
        return this;
    }

    /**
     * 
     * @return A positive integer indicating the number of initial
     *         series/tracks to skip (useful for paging through the results).
     *         The default value is 0.The minimum allowed value is 0. The
     *         maximum allowed value is MAX_INT.
     * 
     */
    public int getOffset() {
        return offset;
    }

    /**
     * 
     * @param offset  A positive integer indicating the number of initial
     *                series/tracks to skip (useful for paging through the
     *                results).  The default value is 0.The minimum allowed
     *                value is 0. The maximum allowed value is MAX_INT.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public GetRecordsBySeriesRequest setOffset(int offset) {
        this.offset = offset;
        return this;
    }

    /**
     * 
     * @return A positive integer indicating the maximum number of
     *         series/tracks to be returned. Or END_OF_SET (-9999) to indicate
     *         that the max number of results should be returned.  The default
     *         value is 250.
     * 
     */
    public int getLimit() {
        return limit;
    }

    /**
     * 
     * @param limit  A positive integer indicating the maximum number of
     *               series/tracks to be returned. Or END_OF_SET (-9999) to
     *               indicate that the max number of results should be
     *               returned.  The default value is 250.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public GetRecordsBySeriesRequest setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * 
     * @return Specifies the encoding for returned records; either {@code
     *         binary} or {@code json}.
     *         Supported values:
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.GetRecordsBySeriesRequest.Encoding#BINARY
     *         BINARY}
     *                 <li> {@link
     *         com.gpudb.protocol.GetRecordsBySeriesRequest.Encoding#JSON JSON}
     *         </ul>
     *         The default value is {@link
     *         com.gpudb.protocol.GetRecordsBySeriesRequest.Encoding#BINARY
     *         BINARY}.
     * 
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * 
     * @param encoding  Specifies the encoding for returned records; either
     *                  {@code binary} or {@code json}.
     *                  Supported values:
     *                  <ul>
     *                          <li> {@link
     *                  com.gpudb.protocol.GetRecordsBySeriesRequest.Encoding#BINARY
     *                  BINARY}
     *                          <li> {@link
     *                  com.gpudb.protocol.GetRecordsBySeriesRequest.Encoding#JSON
     *                  JSON}
     *                  </ul>
     *                  The default value is {@link
     *                  com.gpudb.protocol.GetRecordsBySeriesRequest.Encoding#BINARY
     *                  BINARY}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public GetRecordsBySeriesRequest setEncoding(String encoding) {
        this.encoding = (encoding == null) ? Encoding.BINARY : encoding;
        return this;
    }

    /**
     * 
     * @return Optional parameters.  The default value is an empty {@link Map}.
     * 
     */
    public Map<String, String> getOptions() {
        return options;
    }

    /**
     * 
     * @param options  Optional parameters.  The default value is an empty
     *                 {@link Map}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public GetRecordsBySeriesRequest setOptions(Map<String, String> options) {
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
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
                return this.tableName;

            case 1:
                return this.worldTableName;

            case 2:
                return this.offset;

            case 3:
                return this.limit;

            case 4:
                return this.encoding;

            case 5:
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
     * 
     */
    @Override
    @SuppressWarnings("unchecked")
    public void put(int index, Object value) {
        switch (index) {
            case 0:
                this.tableName = (String)value;
                break;

            case 1:
                this.worldTableName = (String)value;
                break;

            case 2:
                this.offset = (Integer)value;
                break;

            case 3:
                this.limit = (Integer)value;
                break;

            case 4:
                this.encoding = (String)value;
                break;

            case 5:
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

        GetRecordsBySeriesRequest that = (GetRecordsBySeriesRequest)obj;

        return ( this.tableName.equals( that.tableName )
                 && this.worldTableName.equals( that.worldTableName )
                 && ( this.offset == that.offset )
                 && ( this.limit == that.limit )
                 && this.encoding.equals( that.encoding )
                 && this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "tableName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.tableName ) );
        builder.append( ", " );
        builder.append( gd.toString( "worldTableName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.worldTableName ) );
        builder.append( ", " );
        builder.append( gd.toString( "offset" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.offset ) );
        builder.append( ", " );
        builder.append( gd.toString( "limit" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.limit ) );
        builder.append( ", " );
        builder.append( gd.toString( "encoding" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.encoding ) );
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
        hashCode = (31 * hashCode) + this.tableName.hashCode();
        hashCode = (31 * hashCode) + this.worldTableName.hashCode();
        hashCode = (31 * hashCode) + this.offset;
        hashCode = (31 * hashCode) + this.limit;
        hashCode = (31 * hashCode) + this.encoding.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }

}
