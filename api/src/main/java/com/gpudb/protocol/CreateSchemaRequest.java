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
 * A set of parameters for {@link
 * com.gpudb.GPUdb#createSchema(CreateSchemaRequest) GPUdb.createSchema}.
 * <p>
 * Creates a SQL-style <a href="../../../../../../concepts/schemas/"
 * target="_top">schema</a>. Schemas are containers for tables and views.
 * Multiple tables and views can be defined with the same name in different
 * schemas.
 */
public class CreateSchemaRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("CreateSchemaRequest")
            .namespace("com.gpudb")
            .fields()
                .name("schemaName").type().stringType().noDefault()
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

    /**
     * A set of string constants for the {@link CreateSchemaRequest} parameter
     * {@link #getOptions() options}.
     * <p>
     * Optional parameters.
     */
    public static final class Options {
        /**
         * If {@link Options#TRUE TRUE}, prevents an error from occurring if
         * the schema already exists.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}
         *     <li>{@link Options#FALSE FALSE}
         * </ul>
         * The default value is {@link Options#FALSE FALSE}.
         */
        public static final String NO_ERROR_IF_EXISTS = "no_error_if_exists";

        public static final String TRUE = "true";
        public static final String FALSE = "false";

        private Options() {  }
    }

    private String schemaName;
    private Map<String, String> options;

    /**
     * Constructs a CreateSchemaRequest object with default parameters.
     */
    public CreateSchemaRequest() {
        schemaName = "";
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs a CreateSchemaRequest object with the specified parameters.
     *
     * @param schemaName  Name of the schema to be created.  Has the same
     *                    naming restrictions as <a
     *                    href="../../../../../../concepts/tables/"
     *                    target="_top">tables</a>.
     * @param options  Optional parameters.
     *                 <ul>
     *                     <li>{@link Options#NO_ERROR_IF_EXISTS
     *                         NO_ERROR_IF_EXISTS}: If {@link Options#TRUE
     *                         TRUE}, prevents an error from occurring if the
     *                         schema already exists.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#TRUE TRUE}
     *                             <li>{@link Options#FALSE FALSE}
     *                         </ul>
     *                         The default value is {@link Options#FALSE
     *                         FALSE}.
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     */
    public CreateSchemaRequest(String schemaName, Map<String, String> options) {
        this.schemaName = (schemaName == null) ? "" : schemaName;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * Name of the schema to be created.  Has the same naming restrictions as
     * <a href="../../../../../../concepts/tables/" target="_top">tables</a>.
     *
     * @return The current value of {@code schemaName}.
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * Name of the schema to be created.  Has the same naming restrictions as
     * <a href="../../../../../../concepts/tables/" target="_top">tables</a>.
     *
     * @param schemaName  The new value for {@code schemaName}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public CreateSchemaRequest setSchemaName(String schemaName) {
        this.schemaName = (schemaName == null) ? "" : schemaName;
        return this;
    }

    /**
     * Optional parameters.
     * <ul>
     *     <li>{@link Options#NO_ERROR_IF_EXISTS NO_ERROR_IF_EXISTS}: If {@link
     *         Options#TRUE TRUE}, prevents an error from occurring if the
     *         schema already exists.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     * </ul>
     * The default value is an empty {@link Map}.
     *
     * @return The current value of {@code options}.
     */
    public Map<String, String> getOptions() {
        return options;
    }

    /**
     * Optional parameters.
     * <ul>
     *     <li>{@link Options#NO_ERROR_IF_EXISTS NO_ERROR_IF_EXISTS}: If {@link
     *         Options#TRUE TRUE}, prevents an error from occurring if the
     *         schema already exists.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     * </ul>
     * The default value is an empty {@link Map}.
     *
     * @param options  The new value for {@code options}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public CreateSchemaRequest setOptions(Map<String, String> options) {
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
                return this.schemaName;

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
                this.schemaName = (String)value;
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

        CreateSchemaRequest that = (CreateSchemaRequest)obj;

        return ( this.schemaName.equals( that.schemaName )
                 && this.options.equals( that.options ) );
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
        builder.append( gd.toString( "options" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.options ) );
        builder.append( "}" );

        return builder.toString();
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = (31 * hashCode) + this.schemaName.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}