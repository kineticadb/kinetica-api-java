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
 * com.gpudb.GPUdb#showSchema(ShowSchemaRequest)}.
 * <p>
 * Retrieves information about a <a href="../../../../../../concepts/schemas/"
 * target="_top">schema</a> (or all schemas), as specified in {@code
 * schemaName}.
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
     * @return  the schema for the class.
     * 
     */
    public static Schema getClassSchema() {
        return schema$;
    }


    /**
     * Optional parameters.
     * <ul>
     *         <li> {@link
     * com.gpudb.protocol.ShowSchemaRequest.Options#NO_ERROR_IF_NOT_EXISTS
     * NO_ERROR_IF_NOT_EXISTS}: If {@code false} will return an error if the
     * provided {@code schemaName} does not exist. If {@code true} then it will
     * return an empty result if the provided {@code schemaName} does not
     * exist.
     * Supported values:
     * <ul>
     *         <li> {@link com.gpudb.protocol.ShowSchemaRequest.Options#TRUE
     * TRUE}
     *         <li> {@link com.gpudb.protocol.ShowSchemaRequest.Options#FALSE
     * FALSE}
     * </ul>
     * The default value is {@link
     * com.gpudb.protocol.ShowSchemaRequest.Options#FALSE FALSE}.
     * </ul>
     * The default value is an empty {@link Map}.
     * A set of string constants for the parameter {@code options}.
     */
    public static final class Options {

        /**
         * If {@code false} will return an error if the provided {@code
         * schemaName} does not exist. If {@code true} then it will return an
         * empty result if the provided {@code schemaName} does not exist.
         * Supported values:
         * <ul>
         *         <li> {@link
         * com.gpudb.protocol.ShowSchemaRequest.Options#TRUE TRUE}
         *         <li> {@link
         * com.gpudb.protocol.ShowSchemaRequest.Options#FALSE FALSE}
         * </ul>
         * The default value is {@link
         * com.gpudb.protocol.ShowSchemaRequest.Options#FALSE FALSE}.
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
     *                         <li> {@link
     *                 com.gpudb.protocol.ShowSchemaRequest.Options#NO_ERROR_IF_NOT_EXISTS
     *                 NO_ERROR_IF_NOT_EXISTS}: If {@code false} will return an
     *                 error if the provided {@code schemaName} does not exist.
     *                 If {@code true} then it will return an empty result if
     *                 the provided {@code schemaName} does not exist.
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.ShowSchemaRequest.Options#TRUE TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.ShowSchemaRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.ShowSchemaRequest.Options#FALSE
     *                 FALSE}.
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     * 
     */
    public ShowSchemaRequest(String schemaName, Map<String, String> options) {
        this.schemaName = (schemaName == null) ? "" : schemaName;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * 
     * @return Name of the schema for which to retrieve the information. If
     *         blank, then info for all schemas is returned.
     * 
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * 
     * @param schemaName  Name of the schema for which to retrieve the
     *                    information. If blank, then info for all schemas is
     *                    returned.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public ShowSchemaRequest setSchemaName(String schemaName) {
        this.schemaName = (schemaName == null) ? "" : schemaName;
        return this;
    }

    /**
     * 
     * @return Optional parameters.
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.ShowSchemaRequest.Options#NO_ERROR_IF_NOT_EXISTS
     *         NO_ERROR_IF_NOT_EXISTS}: If {@code false} will return an error
     *         if the provided {@code schemaName} does not exist. If {@code
     *         true} then it will return an empty result if the provided {@code
     *         schemaName} does not exist.
     *         Supported values:
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.ShowSchemaRequest.Options#TRUE TRUE}
     *                 <li> {@link
     *         com.gpudb.protocol.ShowSchemaRequest.Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link
     *         com.gpudb.protocol.ShowSchemaRequest.Options#FALSE FALSE}.
     *         </ul>
     *         The default value is an empty {@link Map}.
     * 
     */
    public Map<String, String> getOptions() {
        return options;
    }

    /**
     * 
     * @param options  Optional parameters.
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.ShowSchemaRequest.Options#NO_ERROR_IF_NOT_EXISTS
     *                 NO_ERROR_IF_NOT_EXISTS}: If {@code false} will return an
     *                 error if the provided {@code schemaName} does not exist.
     *                 If {@code true} then it will return an empty result if
     *                 the provided {@code schemaName} does not exist.
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.ShowSchemaRequest.Options#TRUE TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.ShowSchemaRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.ShowSchemaRequest.Options#FALSE
     *                 FALSE}.
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public ShowSchemaRequest setOptions(Map<String, String> options) {
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
     * 
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