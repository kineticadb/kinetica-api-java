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
 * A set of parameters for {@link com.gpudb.GPUdb#showSchema(ShowSchemaRequest)
 * GPUdb.showSchema}.
 * <p>
 * Retrieves information about a <a href="../../../../../../concepts/schemas/"
 * target="_top">schema</a> (or all schemas), as specified in {@link
 * #getSchemaName() schemaName}.
 */
public class ShowSchemaRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("ShowSchemaRequest")
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
     * A set of string constants for the {@link ShowSchemaRequest} parameter
     * {@link #getOptions() options}.
     * <p>
     * Optional parameters.
     */
    public static final class Options {
        /**
         * If {@link Options#FALSE FALSE} will return an error if the provided
         * {@link #getSchemaName() schemaName} does not exist. If {@link
         * Options#TRUE TRUE} then it will return an empty result if the
         * provided {@link #getSchemaName() schemaName} does not exist.
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

        private Options() {  }
    }

    private String schemaName;
    private Map<String, String> options;

    /**
     * Constructs a ShowSchemaRequest object with default parameters.
     */
    public ShowSchemaRequest() {
        schemaName = "";
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs a ShowSchemaRequest object with the specified parameters.
     *
     * @param schemaName  Name of the schema for which to retrieve the
     *                    information. If blank, then info for all schemas is
     *                    returned.
     * @param options  Optional parameters.
     *                 <ul>
     *                     <li>{@link Options#NO_ERROR_IF_NOT_EXISTS
     *                         NO_ERROR_IF_NOT_EXISTS}: If {@link Options#FALSE
     *                         FALSE} will return an error if the provided
     *                         {@code schemaName} does not exist. If {@link
     *                         Options#TRUE TRUE} then it will return an empty
     *                         result if the provided {@code schemaName} does
     *                         not exist.
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
    public ShowSchemaRequest(String schemaName, Map<String, String> options) {
        this.schemaName = (schemaName == null) ? "" : schemaName;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * Name of the schema for which to retrieve the information. If blank, then
     * info for all schemas is returned.
     *
     * @return The current value of {@code schemaName}.
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * Name of the schema for which to retrieve the information. If blank, then
     * info for all schemas is returned.
     *
     * @param schemaName  The new value for {@code schemaName}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public ShowSchemaRequest setSchemaName(String schemaName) {
        this.schemaName = (schemaName == null) ? "" : schemaName;
        return this;
    }

    /**
     * Optional parameters.
     * <ul>
     *     <li>{@link Options#NO_ERROR_IF_NOT_EXISTS NO_ERROR_IF_NOT_EXISTS}:
     *         If {@link Options#FALSE FALSE} will return an error if the
     *         provided {@link #getSchemaName() schemaName} does not exist. If
     *         {@link Options#TRUE TRUE} then it will return an empty result if
     *         the provided {@link #getSchemaName() schemaName} does not exist.
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
     *         If {@link Options#FALSE FALSE} will return an error if the
     *         provided {@link #getSchemaName() schemaName} does not exist. If
     *         {@link Options#TRUE TRUE} then it will return an empty result if
     *         the provided {@link #getSchemaName() schemaName} does not exist.
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
    public ShowSchemaRequest setOptions(Map<String, String> options) {
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

        ShowSchemaRequest that = (ShowSchemaRequest)obj;

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
