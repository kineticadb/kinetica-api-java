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
 * A set of parameters for {@link com.gpudb.GPUdb#alterUser(AlterUserRequest)
 * GPUdb.alterUser}.
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
     * @return The schema for the class.
     */
    public static Schema getClassSchema() {
        return schema$;
    }

    /**
     * A set of string constants for the {@link AlterUserRequest} parameter
     * {@link #getAction() action}.
     * <p>
     * Modification operation to be applied to the user.
     */
    public static final class Action {
        /**
         * Is the user allowed to login.
         */
        public static final String SET_ACTIVATED = "set_activated";

        /**
         * User may login
         */
        public static final String TRUE = "true";

        /**
         * User may not login
         */
        public static final String FALSE = "false";

        /**
         * Sets the comment for an internal user.
         */
        public static final String SET_COMMENT = "set_comment";

        /**
         * Set the default_schema for an internal user. An empty string means
         * the user will have no default schema.
         */
        public static final String SET_DEFAULT_SCHEMA = "set_default_schema";

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
     *                    <li>{@link Action#SET_ACTIVATED SET_ACTIVATED}: Is
     *                        the user allowed to login.
     *                    <li>{@link Action#TRUE TRUE}: User may login
     *                    <li>{@link Action#FALSE FALSE}: User may not login
     *                    <li>{@link Action#SET_COMMENT SET_COMMENT}: Sets the
     *                        comment for an internal user.
     *                    <li>{@link Action#SET_DEFAULT_SCHEMA
     *                        SET_DEFAULT_SCHEMA}: Set the default_schema for
     *                        an internal user. An empty string means the user
     *                        will have no default schema.
     *                    <li>{@link Action#SET_PASSWORD SET_PASSWORD}: Sets
     *                        the password of the user. The user must be an
     *                        internal user.
     *                    <li>{@link Action#SET_RESOURCE_GROUP
     *                        SET_RESOURCE_GROUP}: Sets the resource group for
     *                        an internal user. The resource group must exist,
     *                        otherwise, an empty string assigns the user to
     *                        the default resource group.
     *                </ul>
     * @param value  The value of the modification, depending on {@code
     *               action}.
     * @param options  Optional parameters. The default value is an empty
     *                 {@link Map}.
     */
    public AlterUserRequest(String name, String action, String value, Map<String, String> options) {
        this.name = (name == null) ? "" : name;
        this.action = (action == null) ? "" : action;
        this.value = (value == null) ? "" : value;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * Name of the user to be altered. Must be an existing user.
     *
     * @return The current value of {@code name}.
     */
    public String getName() {
        return name;
    }

    /**
     * Name of the user to be altered. Must be an existing user.
     *
     * @param name  The new value for {@code name}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AlterUserRequest setName(String name) {
        this.name = (name == null) ? "" : name;
        return this;
    }

    /**
     * Modification operation to be applied to the user.
     * Supported values:
     * <ul>
     *     <li>{@link Action#SET_ACTIVATED SET_ACTIVATED}: Is the user allowed
     *         to login.
     *     <li>{@link Action#TRUE TRUE}: User may login
     *     <li>{@link Action#FALSE FALSE}: User may not login
     *     <li>{@link Action#SET_COMMENT SET_COMMENT}: Sets the comment for an
     *         internal user.
     *     <li>{@link Action#SET_DEFAULT_SCHEMA SET_DEFAULT_SCHEMA}: Set the
     *         default_schema for an internal user. An empty string means the
     *         user will have no default schema.
     *     <li>{@link Action#SET_PASSWORD SET_PASSWORD}: Sets the password of
     *         the user. The user must be an internal user.
     *     <li>{@link Action#SET_RESOURCE_GROUP SET_RESOURCE_GROUP}: Sets the
     *         resource group for an internal user. The resource group must
     *         exist, otherwise, an empty string assigns the user to the
     *         default resource group.
     * </ul>
     *
     * @return The current value of {@code action}.
     */
    public String getAction() {
        return action;
    }

    /**
     * Modification operation to be applied to the user.
     * Supported values:
     * <ul>
     *     <li>{@link Action#SET_ACTIVATED SET_ACTIVATED}: Is the user allowed
     *         to login.
     *     <li>{@link Action#TRUE TRUE}: User may login
     *     <li>{@link Action#FALSE FALSE}: User may not login
     *     <li>{@link Action#SET_COMMENT SET_COMMENT}: Sets the comment for an
     *         internal user.
     *     <li>{@link Action#SET_DEFAULT_SCHEMA SET_DEFAULT_SCHEMA}: Set the
     *         default_schema for an internal user. An empty string means the
     *         user will have no default schema.
     *     <li>{@link Action#SET_PASSWORD SET_PASSWORD}: Sets the password of
     *         the user. The user must be an internal user.
     *     <li>{@link Action#SET_RESOURCE_GROUP SET_RESOURCE_GROUP}: Sets the
     *         resource group for an internal user. The resource group must
     *         exist, otherwise, an empty string assigns the user to the
     *         default resource group.
     * </ul>
     *
     * @param action  The new value for {@code action}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AlterUserRequest setAction(String action) {
        this.action = (action == null) ? "" : action;
        return this;
    }

    /**
     * The value of the modification, depending on {@link #getAction() action}.
     *
     * @return The current value of {@code value}.
     */
    public String getValue() {
        return value;
    }

    /**
     * The value of the modification, depending on {@link #getAction() action}.
     *
     * @param value  The new value for {@code value}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AlterUserRequest setValue(String value) {
        this.value = (value == null) ? "" : value;
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
    public AlterUserRequest setOptions(Map<String, String> options) {
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
