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
 * A set of parameters for {@link com.gpudb.GPUdb#hasRole(HasRoleRequest)
 * GPUdb.hasRole}.
 * <p>
 * Checks if the specified user has the specified role.
 */
public class HasRoleRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("HasRoleRequest")
            .namespace("com.gpudb")
            .fields()
                .name("principal").type().stringType().noDefault()
                .name("role").type().stringType().noDefault()
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
     * A set of string constants for the {@link HasRoleRequest} parameter
     * {@link #getOptions() options}.
     * <p>
     * Optional parameters.
     */
    public static final class Options {
        /**
         * If {@link Options#FALSE FALSE} will return an error if the provided
         * {@link #getRole() role} does not exist or is blank. If {@link
         * Options#TRUE TRUE} then it will return {@link
         * com.gpudb.protocol.HasRoleResponse.HasRole#FALSE FALSE} for {@link
         * com.gpudb.protocol.HasRoleResponse#getHasRole() hasRole}.
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
         * If {@link Options#FALSE FALSE} will search recursively if the {@link
         * #getPrincipal() principal} is a member of {@link #getRole() role}.
         * If {@link Options#TRUE TRUE} then {@link #getPrincipal() principal}
         * must directly be a member of {@link #getRole() role}.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}
         *     <li>{@link Options#FALSE FALSE}
         * </ul>
         * The default value is {@link Options#FALSE FALSE}.
         */
        public static final String ONLY_DIRECT = "only_direct";

        private Options() {  }
    }

    private String principal;
    private String role;
    private Map<String, String> options;

    /**
     * Constructs a HasRoleRequest object with default parameters.
     */
    public HasRoleRequest() {
        principal = "";
        role = "";
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs a HasRoleRequest object with the specified parameters.
     *
     * @param principal  Name of the user for which role membersih is being
     *                   checked. Must be an existing user. If blank, will use
     *                   the current user. The default value is ''.
     * @param role  Name of role to check for membership.
     * @param options  Optional parameters.
     *                 <ul>
     *                     <li>{@link Options#NO_ERROR_IF_NOT_EXISTS
     *                         NO_ERROR_IF_NOT_EXISTS}: If {@link Options#FALSE
     *                         FALSE} will return an error if the provided
     *                         {@code role} does not exist or is blank. If
     *                         {@link Options#TRUE TRUE} then it will return
     *                         {@link
     *                         com.gpudb.protocol.HasRoleResponse.HasRole#FALSE
     *                         FALSE} for {@link
     *                         com.gpudb.protocol.HasRoleResponse#getHasRole()
     *                         hasRole}.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#TRUE TRUE}
     *                             <li>{@link Options#FALSE FALSE}
     *                         </ul>
     *                         The default value is {@link Options#FALSE
     *                         FALSE}.
     *                     <li>{@link Options#ONLY_DIRECT ONLY_DIRECT}: If
     *                         {@link Options#FALSE FALSE} will search
     *                         recursively if the {@code principal} is a member
     *                         of {@code role}.  If {@link Options#TRUE TRUE}
     *                         then {@code principal} must directly be a member
     *                         of {@code role}.
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
    public HasRoleRequest(String principal, String role, Map<String, String> options) {
        this.principal = (principal == null) ? "" : principal;
        this.role = (role == null) ? "" : role;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * Name of the user for which role membersih is being checked. Must be an
     * existing user. If blank, will use the current user. The default value is
     * ''.
     *
     * @return The current value of {@code principal}.
     */
    public String getPrincipal() {
        return principal;
    }

    /**
     * Name of the user for which role membersih is being checked. Must be an
     * existing user. If blank, will use the current user. The default value is
     * ''.
     *
     * @param principal  The new value for {@code principal}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public HasRoleRequest setPrincipal(String principal) {
        this.principal = (principal == null) ? "" : principal;
        return this;
    }

    /**
     * Name of role to check for membership.
     *
     * @return The current value of {@code role}.
     */
    public String getRole() {
        return role;
    }

    /**
     * Name of role to check for membership.
     *
     * @param role  The new value for {@code role}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public HasRoleRequest setRole(String role) {
        this.role = (role == null) ? "" : role;
        return this;
    }

    /**
     * Optional parameters.
     * <ul>
     *     <li>{@link Options#NO_ERROR_IF_NOT_EXISTS NO_ERROR_IF_NOT_EXISTS}:
     *         If {@link Options#FALSE FALSE} will return an error if the
     *         provided {@link #getRole() role} does not exist or is blank. If
     *         {@link Options#TRUE TRUE} then it will return {@link
     *         com.gpudb.protocol.HasRoleResponse.HasRole#FALSE FALSE} for
     *         {@link com.gpudb.protocol.HasRoleResponse#getHasRole() hasRole}.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     *     <li>{@link Options#ONLY_DIRECT ONLY_DIRECT}: If {@link Options#FALSE
     *         FALSE} will search recursively if the {@link #getPrincipal()
     *         principal} is a member of {@link #getRole() role}.  If {@link
     *         Options#TRUE TRUE} then {@link #getPrincipal() principal} must
     *         directly be a member of {@link #getRole() role}.
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
     *         provided {@link #getRole() role} does not exist or is blank. If
     *         {@link Options#TRUE TRUE} then it will return {@link
     *         com.gpudb.protocol.HasRoleResponse.HasRole#FALSE FALSE} for
     *         {@link com.gpudb.protocol.HasRoleResponse#getHasRole() hasRole}.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     *     <li>{@link Options#ONLY_DIRECT ONLY_DIRECT}: If {@link Options#FALSE
     *         FALSE} will search recursively if the {@link #getPrincipal()
     *         principal} is a member of {@link #getRole() role}.  If {@link
     *         Options#TRUE TRUE} then {@link #getPrincipal() principal} must
     *         directly be a member of {@link #getRole() role}.
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
    public HasRoleRequest setOptions(Map<String, String> options) {
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
                return this.principal;

            case 1:
                return this.role;

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
                this.principal = (String)value;
                break;

            case 1:
                this.role = (String)value;
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

        HasRoleRequest that = (HasRoleRequest)obj;

        return ( this.principal.equals( that.principal )
                 && this.role.equals( that.role )
                 && this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "principal" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.principal ) );
        builder.append( ", " );
        builder.append( gd.toString( "role" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.role ) );
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
        hashCode = (31 * hashCode) + this.principal.hashCode();
        hashCode = (31 * hashCode) + this.role.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}