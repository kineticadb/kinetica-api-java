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
 * com.gpudb.GPUdb#hasSchema(HasSchemaRequest) GPUdb.hasSchema}.
 */
public class HasSchemaResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("HasSchemaResponse")
            .namespace("com.gpudb")
            .fields()
                .name("schemaName").type().stringType().noDefault()
                .name("schemaExists").type().booleanType().noDefault()
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
     * A set of string constants for the {@link HasSchemaResponse} parameter
     * {@link #getSchemaExists() schemaExists}.
     * <p>
     * Indicates whether the schema exists or not.
     */
    public static final class SchemaExists {
        public static final String TRUE = "true";
        public static final String FALSE = "false";

        private SchemaExists() {  }
    }

    private String schemaName;
    private boolean schemaExists;
    private Map<String, String> info;

    /**
     * Constructs a HasSchemaResponse object with default parameters.
     */
    public HasSchemaResponse() {
    }

    /**
     * Value of {@link com.gpudb.protocol.HasSchemaRequest#getSchemaName()
     * schemaName}
     *
     * @return The current value of {@code schemaName}.
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * Value of {@link com.gpudb.protocol.HasSchemaRequest#getSchemaName()
     * schemaName}
     *
     * @param schemaName  The new value for {@code schemaName}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public HasSchemaResponse setSchemaName(String schemaName) {
        this.schemaName = (schemaName == null) ? "" : schemaName;
        return this;
    }

    /**
     * Indicates whether the schema exists or not.
     * Supported values:
     * <ul>
     *     <li>{@code true}
     *     <li>{@code false}
     * </ul>
     *
     * @return The current value of {@code schemaExists}.
     */
    public boolean getSchemaExists() {
        return schemaExists;
    }

    /**
     * Indicates whether the schema exists or not.
     * Supported values:
     * <ul>
     *     <li>{@code true}
     *     <li>{@code false}
     * </ul>
     *
     * @param schemaExists  The new value for {@code schemaExists}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public HasSchemaResponse setSchemaExists(boolean schemaExists) {
        this.schemaExists = schemaExists;
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
    public HasSchemaResponse setInfo(Map<String, String> info) {
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
                return this.schemaName;

            case 1:
                return this.schemaExists;

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
                this.schemaName = (String)value;
                break;

            case 1:
                this.schemaExists = (Boolean)value;
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

        HasSchemaResponse that = (HasSchemaResponse)obj;

        return ( this.schemaName.equals( that.schemaName )
                 && ( this.schemaExists == that.schemaExists )
                 && this.info.equals( that.info ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "schemaName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.schemaName ) );
        builder.append( ", " );
        builder.append( gd.toString( "schemaExists" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.schemaExists ) );
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
        hashCode = (31 * hashCode) + this.schemaName.hashCode();
        hashCode = (31 * hashCode) + ((Boolean)this.schemaExists).hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }
}
