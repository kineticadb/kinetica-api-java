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
 * A set of parameters for {@link com.gpudb.GPUdb#dropSchema(DropSchemaRequest)
 * GPUdb.dropSchema}.
 * <p>
 * Drops an existing SQL-style <a href="../../../../../../concepts/schemas/"
 * target="_top">schema</a>, specified in {@link #getSchemaName() schemaName}.
 */
public class DropSchemaRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("DropSchemaRequest")
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
     * A set of string constants for the {@link DropSchemaRequest} parameter
     * {@link #getOptions() options}.
     * <p>
     * Optional parameters.
     */
    public static final class Options {
        /**
         * If {@link Options#TRUE TRUE} and if the schema specified in {@link
         * #getSchemaName() schemaName} does not exist, no error is returned.
         * If {@link Options#FALSE FALSE} and if the schema specified in {@link
         * #getSchemaName() schemaName} does not exist, then an error is
         * returned.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}
         *     <li>{@link Options#FALSE FALSE}
         * </ul>
         * The default value is {@link Options#FALSE FALSE}.
         */
        public static final String NO_ERROR_IF_NOT_EXISTS = "no_error_if_not_exists";

        public static final String TRUE = "true";
        public static final String FALSE = "false";

        /**
         * If {@link Options#TRUE TRUE}, all tables within the schema will be
         * dropped. If {@link Options#FALSE FALSE}, the schema will be dropped
         * only if empty.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}
         *     <li>{@link Options#FALSE FALSE}
         * </ul>
         * The default value is {@link Options#FALSE FALSE}.
         */
        public static final String CASCADE = "cascade";

        private Options() {  }
    }

    private String schemaName;
    private Map<String, String> options;

    /**
     * Constructs a DropSchemaRequest object with default parameters.
     */
    public DropSchemaRequest() {
        schemaName = "";
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs a DropSchemaRequest object with the specified parameters.
     *
     * @param schemaName  Name of the schema to be dropped. Must be an existing
     *                    schema.
     * @param options  Optional parameters.
     *                 <ul>
     *                     <li>{@link Options#NO_ERROR_IF_NOT_EXISTS
     *                         NO_ERROR_IF_NOT_EXISTS}: If {@link Options#TRUE
     *                         TRUE} and if the schema specified in {@code
     *                         schemaName} does not exist, no error is
     *                         returned. If {@link Options#FALSE FALSE} and if
     *                         the schema specified in {@code schemaName} does
     *                         not exist, then an error is returned.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#TRUE TRUE}
     *                             <li>{@link Options#FALSE FALSE}
     *                         </ul>
     *                         The default value is {@link Options#FALSE
     *                         FALSE}.
     *                     <li>{@link Options#CASCADE CASCADE}: If {@link
     *                         Options#TRUE TRUE}, all tables within the schema
     *                         will be dropped. If {@link Options#FALSE FALSE},
     *                         the schema will be dropped only if empty.
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
    public DropSchemaRequest(String schemaName, Map<String, String> options) {
        this.schemaName = (schemaName == null) ? "" : schemaName;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * Name of the schema to be dropped. Must be an existing schema.
     *
     * @return The current value of {@code schemaName}.
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * Name of the schema to be dropped. Must be an existing schema.
     *
     * @param schemaName  The new value for {@code schemaName}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public DropSchemaRequest setSchemaName(String schemaName) {
        this.schemaName = (schemaName == null) ? "" : schemaName;
        return this;
    }

    /**
     * Optional parameters.
     * <ul>
     *     <li>{@link Options#NO_ERROR_IF_NOT_EXISTS NO_ERROR_IF_NOT_EXISTS}:
     *         If {@link Options#TRUE TRUE} and if the schema specified in
     *         {@link #getSchemaName() schemaName} does not exist, no error is
     *         returned. If {@link Options#FALSE FALSE} and if the schema
     *         specified in {@link #getSchemaName() schemaName} does not exist,
     *         then an error is returned.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     *     <li>{@link Options#CASCADE CASCADE}: If {@link Options#TRUE TRUE},
     *         all tables within the schema will be dropped. If {@link
     *         Options#FALSE FALSE}, the schema will be dropped only if empty.
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
     *     <li>{@link Options#NO_ERROR_IF_NOT_EXISTS NO_ERROR_IF_NOT_EXISTS}:
     *         If {@link Options#TRUE TRUE} and if the schema specified in
     *         {@link #getSchemaName() schemaName} does not exist, no error is
     *         returned. If {@link Options#FALSE FALSE} and if the schema
     *         specified in {@link #getSchemaName() schemaName} does not exist,
     *         then an error is returned.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     *     <li>{@link Options#CASCADE CASCADE}: If {@link Options#TRUE TRUE},
     *         all tables within the schema will be dropped. If {@link
     *         Options#FALSE FALSE}, the schema will be dropped only if empty.
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
    public DropSchemaRequest setOptions(Map<String, String> options) {
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

        DropSchemaRequest that = (DropSchemaRequest)obj;

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
