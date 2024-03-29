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
 * A set of parameters for {@link com.gpudb.GPUdb#showTypes(ShowTypesRequest)
 * GPUdb.showTypes}.
 * <p>
 * Retrieves information for the specified data type ID or type label. For all
 * data types that match the input criteria, the database returns the type ID,
 * the type schema, the label (if available), and the type's column properties.
 */
public class ShowTypesRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("ShowTypesRequest")
            .namespace("com.gpudb")
            .fields()
                .name("typeId").type().stringType().noDefault()
                .name("label").type().stringType().noDefault()
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
     * A set of string constants for the {@link ShowTypesRequest} parameter
     * {@link #getOptions() options}.
     * <p>
     * Optional parameters.
     */
    public static final class Options {
        /**
         * When set to 'true', no join types will be included.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}
         *     <li>{@link Options#FALSE FALSE}
         * </ul>
         * The default value is {@link Options#FALSE FALSE}.
         */
        public static final String NO_JOIN_TYPES = "no_join_types";

        public static final String TRUE = "true";
        public static final String FALSE = "false";

        private Options() {  }
    }

    private String typeId;
    private String label;
    private Map<String, String> options;

    /**
     * Constructs a ShowTypesRequest object with default parameters.
     */
    public ShowTypesRequest() {
        typeId = "";
        label = "";
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs a ShowTypesRequest object with the specified parameters.
     *
     * @param typeId  Type Id returned in response to a call to {@link
     *                com.gpudb.GPUdb#createType(CreateTypeRequest)
     *                GPUdb.createType}.
     * @param label  Option string that was supplied by user in a call to
     *               {@link com.gpudb.GPUdb#createType(CreateTypeRequest)
     *               GPUdb.createType}.
     * @param options  Optional parameters.
     *                 <ul>
     *                     <li>{@link Options#NO_JOIN_TYPES NO_JOIN_TYPES}:
     *                         When set to 'true', no join types will be
     *                         included.
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
    public ShowTypesRequest(String typeId, String label, Map<String, String> options) {
        this.typeId = (typeId == null) ? "" : typeId;
        this.label = (label == null) ? "" : label;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * Type Id returned in response to a call to {@link
     * com.gpudb.GPUdb#createType(CreateTypeRequest) GPUdb.createType}.
     *
     * @return The current value of {@code typeId}.
     */
    public String getTypeId() {
        return typeId;
    }

    /**
     * Type Id returned in response to a call to {@link
     * com.gpudb.GPUdb#createType(CreateTypeRequest) GPUdb.createType}.
     *
     * @param typeId  The new value for {@code typeId}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public ShowTypesRequest setTypeId(String typeId) {
        this.typeId = (typeId == null) ? "" : typeId;
        return this;
    }

    /**
     * Option string that was supplied by user in a call to {@link
     * com.gpudb.GPUdb#createType(CreateTypeRequest) GPUdb.createType}.
     *
     * @return The current value of {@code label}.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Option string that was supplied by user in a call to {@link
     * com.gpudb.GPUdb#createType(CreateTypeRequest) GPUdb.createType}.
     *
     * @param label  The new value for {@code label}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public ShowTypesRequest setLabel(String label) {
        this.label = (label == null) ? "" : label;
        return this;
    }

    /**
     * Optional parameters.
     * <ul>
     *     <li>{@link Options#NO_JOIN_TYPES NO_JOIN_TYPES}: When set to 'true',
     *         no join types will be included.
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
     *     <li>{@link Options#NO_JOIN_TYPES NO_JOIN_TYPES}: When set to 'true',
     *         no join types will be included.
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
    public ShowTypesRequest setOptions(Map<String, String> options) {
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
                return this.typeId;

            case 1:
                return this.label;

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
     */
    @Override
    @SuppressWarnings("unchecked")
    public void put(int index, Object value) {
        switch (index) {
            case 0:
                this.typeId = (String)value;
                break;

            case 1:
                this.label = (String)value;
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

        ShowTypesRequest that = (ShowTypesRequest)obj;

        return ( this.typeId.equals( that.typeId )
                 && this.label.equals( that.label )
                 && this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "typeId" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.typeId ) );
        builder.append( ", " );
        builder.append( gd.toString( "label" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.label ) );
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
        hashCode = (31 * hashCode) + this.typeId.hashCode();
        hashCode = (31 * hashCode) + this.label.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
