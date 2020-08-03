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
 * A set of parameters for {@link
 * com.gpudb.GPUdb#alterTableMetadata(AlterTableMetadataRequest)}.
 * <p>
 * Updates (adds or changes) metadata for tables. The metadata key and
 * values must both be strings. This is an easy way to annotate whole tables
 * rather
 * than single records within tables.  Some examples of metadata are owner of
 * the
 * table, table creation timestamp etc.
 */
public class AlterTableMetadataRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("AlterTableMetadataRequest")
            .namespace("com.gpudb")
            .fields()
                .name("tableNames").type().array().items().stringType().noDefault()
                .name("metadataMap").type().map().values().stringType().noDefault()
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

    private List<String> tableNames;
    private Map<String, String> metadataMap;
    private Map<String, String> options;


    /**
     * Constructs an AlterTableMetadataRequest object with default parameters.
     */
    public AlterTableMetadataRequest() {
        tableNames = new ArrayList<>();
        metadataMap = new LinkedHashMap<>();
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs an AlterTableMetadataRequest object with the specified
     * parameters.
     * 
     * @param tableNames  Names of the tables whose metadata will be updated,
     *                    in [schema_name.]table_name format, using standard <a
     *                    href="../../../../../concepts/tables.html#table-name-resolution"
     *                    target="_top">name resolution rules</a>.  All
     *                    specified tables must exist, or an error will be
     *                    returned.
     * @param metadataMap  A map which contains the metadata of the tables that
     *                     are to be updated. Note that only one map is
     *                     provided for all the tables; so the change will be
     *                     applied to every table. If the provided map is
     *                     empty, then all existing metadata for the table(s)
     *                     will be cleared.
     * @param options  Optional parameters.  The default value is an empty
     *                 {@link Map}.
     * 
     */
    public AlterTableMetadataRequest(List<String> tableNames, Map<String, String> metadataMap, Map<String, String> options) {
        this.tableNames = (tableNames == null) ? new ArrayList<String>() : tableNames;
        this.metadataMap = (metadataMap == null) ? new LinkedHashMap<String, String>() : metadataMap;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * 
     * @return Names of the tables whose metadata will be updated, in
     *         [schema_name.]table_name format, using standard <a
     *         href="../../../../../concepts/tables.html#table-name-resolution"
     *         target="_top">name resolution rules</a>.  All specified tables
     *         must exist, or an error will be returned.
     * 
     */
    public List<String> getTableNames() {
        return tableNames;
    }

    /**
     * 
     * @param tableNames  Names of the tables whose metadata will be updated,
     *                    in [schema_name.]table_name format, using standard <a
     *                    href="../../../../../concepts/tables.html#table-name-resolution"
     *                    target="_top">name resolution rules</a>.  All
     *                    specified tables must exist, or an error will be
     *                    returned.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public AlterTableMetadataRequest setTableNames(List<String> tableNames) {
        this.tableNames = (tableNames == null) ? new ArrayList<String>() : tableNames;
        return this;
    }

    /**
     * 
     * @return A map which contains the metadata of the tables that are to be
     *         updated. Note that only one map is provided for all the tables;
     *         so the change will be applied to every table. If the provided
     *         map is empty, then all existing metadata for the table(s) will
     *         be cleared.
     * 
     */
    public Map<String, String> getMetadataMap() {
        return metadataMap;
    }

    /**
     * 
     * @param metadataMap  A map which contains the metadata of the tables that
     *                     are to be updated. Note that only one map is
     *                     provided for all the tables; so the change will be
     *                     applied to every table. If the provided map is
     *                     empty, then all existing metadata for the table(s)
     *                     will be cleared.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public AlterTableMetadataRequest setMetadataMap(Map<String, String> metadataMap) {
        this.metadataMap = (metadataMap == null) ? new LinkedHashMap<String, String>() : metadataMap;
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
    public AlterTableMetadataRequest setOptions(Map<String, String> options) {
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
                return this.tableNames;

            case 1:
                return this.metadataMap;

            case 2:
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
                this.tableNames = (List<String>)value;
                break;

            case 1:
                this.metadataMap = (Map<String, String>)value;
                break;

            case 2:
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

        AlterTableMetadataRequest that = (AlterTableMetadataRequest)obj;

        return ( this.tableNames.equals( that.tableNames )
                 && this.metadataMap.equals( that.metadataMap )
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
        builder.append( gd.toString( "metadataMap" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.metadataMap ) );
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
        hashCode = (31 * hashCode) + this.metadataMap.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }

}
