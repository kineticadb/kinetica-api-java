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
 * A set of parameters for {@link com.gpudb.GPUdb#alterUser(AlterUserRequest)}.
 * <p>
 * Alters a user.
 */
public class AlterUserRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("AlterUserRequest")
            .namespace("com.gpudb")
            .fields()
                .name("name").type().stringType().noDefault()
                .name("action").type().stringType().noDefault()
                .name("value").type().stringType().noDefault()
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
     * Modification operation to be applied to the user.
     * Supported values:
     * <ul>
     *         <li> {@link
     * com.gpudb.protocol.AlterUserRequest.Action#SET_PASSWORD SET_PASSWORD}:
     * Sets the password of the user. The user must be an internal user.
     *         <li> {@link
     * com.gpudb.protocol.AlterUserRequest.Action#SET_RESOURCE_GROUP
     * SET_RESOURCE_GROUP}: Sets the resource group for an internal user. The
     * resource group must exist, otherwise, an empty string assigns the user
     * to the default resource group.
     *         <li> {@link
     * com.gpudb.protocol.AlterUserRequest.Action#SET_DEFAULT_SCHEMA
     * SET_DEFAULT_SCHEMA}: Set the default_schema for an internal user. An
     * empty string means the user will have no default schema.
     * </ul>
     * A set of string constants for the parameter {@code action}.
     */
    public static final class Action {

        /**
         * Sets the password of the user. The user must be an internal user.
         */
        public static final String SET_PASSWORD = "set_password";

        /**
         * Sets the resource group for an internal user. The resource group
         * must exist, otherwise, an empty string assigns the user to the
         * default resource group.
         */
        public static final String SET_RESOURCE_GROUP = "set_resource_group";

        /**
         * Set the default_schema for an internal user. An empty string means
         * the user will have no default schema.
         */
        public static final String SET_DEFAULT_SCHEMA = "set_default_schema";

        private Action() {  }
    }

    private String name;
    private String action;
    private String value;
    private Map<String, String> options;


    /**
     * Constructs an AlterUserRequest object with default parameters.
     */
    public AlterUserRequest() {
        name = "";
        action = "";
        value = "";
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs an AlterUserRequest object with the specified parameters.
     * 
     * @param name  Name of the user to be altered. Must be an existing user.
     * @param action  Modification operation to be applied to the user.
     *                Supported values:
     *                <ul>
     *                        <li> {@link
     *                com.gpudb.protocol.AlterUserRequest.Action#SET_PASSWORD
     *                SET_PASSWORD}: Sets the password of the user. The user
     *                must be an internal user.
     *                        <li> {@link
     *                com.gpudb.protocol.AlterUserRequest.Action#SET_RESOURCE_GROUP
     *                SET_RESOURCE_GROUP}: Sets the resource group for an
     *                internal user. The resource group must exist, otherwise,
     *                an empty string assigns the user to the default resource
     *                group.
     *                        <li> {@link
     *                com.gpudb.protocol.AlterUserRequest.Action#SET_DEFAULT_SCHEMA
     *                SET_DEFAULT_SCHEMA}: Set the default_schema for an
     *                internal user. An empty string means the user will have
     *                no default schema.
     *                </ul>
     * @param value  The value of the modification, depending on {@code
     *               action}.
     * @param options  Optional parameters.  The default value is an empty
     *                 {@link Map}.
     * 
     */
    public AlterUserRequest(String name, String action, String value, Map<String, String> options) {
        this.name = (name == null) ? "" : name;
        this.action = (action == null) ? "" : action;
        this.value = (value == null) ? "" : value;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * 
     * @return Name of the user to be altered. Must be an existing user.
     * 
     */
    public String getName() {
        return name;
    }

    /**
     * 
     * @param name  Name of the user to be altered. Must be an existing user.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public AlterUserRequest setName(String name) {
        this.name = (name == null) ? "" : name;
        return this;
    }

    /**
     * 
     * @return Modification operation to be applied to the user.
     *         Supported values:
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.AlterUserRequest.Action#SET_PASSWORD
     *         SET_PASSWORD}: Sets the password of the user. The user must be
     *         an internal user.
     *                 <li> {@link
     *         com.gpudb.protocol.AlterUserRequest.Action#SET_RESOURCE_GROUP
     *         SET_RESOURCE_GROUP}: Sets the resource group for an internal
     *         user. The resource group must exist, otherwise, an empty string
     *         assigns the user to the default resource group.
     *                 <li> {@link
     *         com.gpudb.protocol.AlterUserRequest.Action#SET_DEFAULT_SCHEMA
     *         SET_DEFAULT_SCHEMA}: Set the default_schema for an internal
     *         user. An empty string means the user will have no default
     *         schema.
     *         </ul>
     * 
     */
    public String getAction() {
        return action;
    }

    /**
     * 
     * @param action  Modification operation to be applied to the user.
     *                Supported values:
     *                <ul>
     *                        <li> {@link
     *                com.gpudb.protocol.AlterUserRequest.Action#SET_PASSWORD
     *                SET_PASSWORD}: Sets the password of the user. The user
     *                must be an internal user.
     *                        <li> {@link
     *                com.gpudb.protocol.AlterUserRequest.Action#SET_RESOURCE_GROUP
     *                SET_RESOURCE_GROUP}: Sets the resource group for an
     *                internal user. The resource group must exist, otherwise,
     *                an empty string assigns the user to the default resource
     *                group.
     *                        <li> {@link
     *                com.gpudb.protocol.AlterUserRequest.Action#SET_DEFAULT_SCHEMA
     *                SET_DEFAULT_SCHEMA}: Set the default_schema for an
     *                internal user. An empty string means the user will have
     *                no default schema.
     *                </ul>
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public AlterUserRequest setAction(String action) {
        this.action = (action == null) ? "" : action;
        return this;
    }

    /**
     * 
     * @return The value of the modification, depending on {@code action}.
     * 
     */
    public String getValue() {
        return value;
    }

    /**
     * 
     * @param value  The value of the modification, depending on {@code
     *               action}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public AlterUserRequest setValue(String value) {
        this.value = (value == null) ? "" : value;
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
    public AlterUserRequest setOptions(Map<String, String> options) {
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
                return this.name;

            case 1:
                return this.action;

            case 2:
                return this.value;

            case 3:
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
                this.name = (String)value;
                break;

            case 1:
                this.action = (String)value;
                break;

            case 2:
                this.value = (String)value;
                break;

            case 3:
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

        AlterUserRequest that = (AlterUserRequest)obj;

        return ( this.name.equals( that.name )
                 && this.action.equals( that.action )
                 && this.value.equals( that.value )
                 && this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "name" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.name ) );
        builder.append( ", " );
        builder.append( gd.toString( "action" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.action ) );
        builder.append( ", " );
        builder.append( gd.toString( "value" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.value ) );
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
        hashCode = (31 * hashCode) + this.name.hashCode();
        hashCode = (31 * hashCode) + this.action.hashCode();
        hashCode = (31 * hashCode) + this.value.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }

}
