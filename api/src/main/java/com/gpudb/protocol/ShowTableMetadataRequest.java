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
 * com.gpudb.GPUdb#showTableMetadata(ShowTableMetadataRequest)
 * GPUdb.showTableMetadata}.
 * <p>
 * Retrieves the user provided metadata for the specified tables.
 */
public class ShowTableMetadataRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("ShowTableMetadataRequest")
            .namespace("com.gpudb")
            .fields()
                .name("tableNames").type().array().items().stringType().noDefault()
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

    private List<String> tableNames;
    private Map<String, String> options;

    /**
     * Constructs a ShowTableMetadataRequest object with default parameters.
     */
    public ShowTableMetadataRequest() {
        tableNames = new ArrayList<>();
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs a ShowTableMetadataRequest object with the specified
     * parameters.
     *
     * @param tableNames  Names of tables whose metadata will be fetched, in
     *                    [schema_name.]table_name format, using standard <a
     *                    href="../../../../../../concepts/tables/#table-name-resolution"
     *                    target="_top">name resolution rules</a>.  All
     *                    provided tables must exist, or an error is returned.
     * @param options  Optional parameters. The default value is an empty
     *                 {@link Map}.
     */
    public ShowTableMetadataRequest(List<String> tableNames, Map<String, String> options) {
        this.tableNames = (tableNames == null) ? new ArrayList<String>() : tableNames;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * Names of tables whose metadata will be fetched, in
     * [schema_name.]table_name format, using standard <a
     * href="../../../../../../concepts/tables/#table-name-resolution"
     * target="_top">name resolution rules</a>.  All provided tables must
     * exist, or an error is returned.
     *
     * @return The current value of {@code tableNames}.
     */
    public List<String> getTableNames() {
        return tableNames;
    }

    /**
     * Names of tables whose metadata will be fetched, in
     * [schema_name.]table_name format, using standard <a
     * href="../../../../../../concepts/tables/#table-name-resolution"
     * target="_top">name resolution rules</a>.  All provided tables must
     * exist, or an error is returned.
     *
     * @param tableNames  The new value for {@code tableNames}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public ShowTableMetadataRequest setTableNames(List<String> tableNames) {
        this.tableNames = (tableNames == null) ? new ArrayList<String>() : tableNames;
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
    public ShowTableMetadataRequest setOptions(Map<String, String> options) {
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
                return this.tableNames;

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
                this.tableNames = (List<String>)value;
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

        ShowTableMetadataRequest that = (ShowTableMetadataRequest)obj;

        return ( this.tableNames.equals( that.tableNames )
                 && this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "tableNames" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.tableNames ) );
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
        hashCode = (31 * hashCode) + this.tableNames.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
