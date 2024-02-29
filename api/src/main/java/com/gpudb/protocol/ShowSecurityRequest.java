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
 * com.gpudb.GPUdb#showSecurity(ShowSecurityRequest)}.
 * <p>
 * Shows security information relating to users and/or roles. If the caller is
 * not a system administrator, only information relating to the caller and
 * their roles is returned.
 */
public class ShowSecurityRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("ShowSecurityRequest")
            .namespace("com.gpudb")
            .fields()
                .name("names").type().array().items().stringType().noDefault()
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
     * com.gpudb.protocol.ShowSecurityRequest.Options#SHOW_CURRENT_USER
     * SHOW_CURRENT_USER}: If {@code true}, returns only security information
     * for the current user.
     * Supported values:
     * <ul>
     *         <li> {@link com.gpudb.protocol.ShowSecurityRequest.Options#TRUE
     * TRUE}
     *         <li> {@link com.gpudb.protocol.ShowSecurityRequest.Options#FALSE
     * FALSE}
     * </ul>
     * The default value is {@link
     * com.gpudb.protocol.ShowSecurityRequest.Options#FALSE FALSE}.
     * </ul>
     * The default value is an empty {@link Map}.
     * A set of string constants for the parameter {@code options}.
     */
    public static final class Options {

        /**
         * If {@code true}, returns only security information for the current
         * user.
         * Supported values:
         * <ul>
         *         <li> {@link
         * com.gpudb.protocol.ShowSecurityRequest.Options#TRUE TRUE}
         *         <li> {@link
         * com.gpudb.protocol.ShowSecurityRequest.Options#FALSE FALSE}
         * </ul>
         * The default value is {@link
         * com.gpudb.protocol.ShowSecurityRequest.Options#FALSE FALSE}.
         */
        public static final String SHOW_CURRENT_USER = "show_current_user";
        public static final String TRUE = "true";
        public static final String FALSE = "false";

        private Options() {  }
    }

    private List<String> names;
    private Map<String, String> options;


    /**
     * Constructs a ShowSecurityRequest object with default parameters.
     */
    public ShowSecurityRequest() {
        names = new ArrayList<>();
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs a ShowSecurityRequest object with the specified parameters.
     * 
     * @param names  A list of names of users and/or roles about which security
     *               information is requested. If none are provided,
     *               information about all users and roles will be returned.
     * @param options  Optional parameters.
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.ShowSecurityRequest.Options#SHOW_CURRENT_USER
     *                 SHOW_CURRENT_USER}: If {@code true}, returns only
     *                 security information for the current user.
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.ShowSecurityRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.ShowSecurityRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.ShowSecurityRequest.Options#FALSE
     *                 FALSE}.
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     * 
     */
    public ShowSecurityRequest(List<String> names, Map<String, String> options) {
        this.names = (names == null) ? new ArrayList<String>() : names;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * 
     * @return A list of names of users and/or roles about which security
     *         information is requested. If none are provided, information
     *         about all users and roles will be returned.
     * 
     */
    public List<String> getNames() {
        return names;
    }

    /**
     * 
     * @param names  A list of names of users and/or roles about which security
     *               information is requested. If none are provided,
     *               information about all users and roles will be returned.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public ShowSecurityRequest setNames(List<String> names) {
        this.names = (names == null) ? new ArrayList<String>() : names;
        return this;
    }

    /**
     * 
     * @return Optional parameters.
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.ShowSecurityRequest.Options#SHOW_CURRENT_USER
     *         SHOW_CURRENT_USER}: If {@code true}, returns only security
     *         information for the current user.
     *         Supported values:
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.ShowSecurityRequest.Options#TRUE TRUE}
     *                 <li> {@link
     *         com.gpudb.protocol.ShowSecurityRequest.Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link
     *         com.gpudb.protocol.ShowSecurityRequest.Options#FALSE FALSE}.
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
     *                 com.gpudb.protocol.ShowSecurityRequest.Options#SHOW_CURRENT_USER
     *                 SHOW_CURRENT_USER}: If {@code true}, returns only
     *                 security information for the current user.
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.ShowSecurityRequest.Options#TRUE
     *                 TRUE}
     *                         <li> {@link
     *                 com.gpudb.protocol.ShowSecurityRequest.Options#FALSE
     *                 FALSE}
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.ShowSecurityRequest.Options#FALSE
     *                 FALSE}.
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public ShowSecurityRequest setOptions(Map<String, String> options) {
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
                return this.names;

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
                this.names = (List<String>)value;
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

        ShowSecurityRequest that = (ShowSecurityRequest)obj;

        return ( this.names.equals( that.names )
                 && this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "names" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.names ) );
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
        hashCode = (31 * hashCode) + this.names.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }

}
