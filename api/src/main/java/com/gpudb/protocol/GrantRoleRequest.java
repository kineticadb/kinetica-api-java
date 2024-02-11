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
 * A set of parameters for {@link com.gpudb.GPUdb#grantRole(GrantRoleRequest)
 * GPUdb.grantRole}.
 * <p>
 * Grants membership in a role to a user or role.
 */
public class GrantRoleRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("GrantRoleRequest")
            .namespace("com.gpudb")
            .fields()
                .name("role").type().stringType().noDefault()
                .name("member").type().stringType().noDefault()
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

    private String role;
    private String member;
    private Map<String, String> options;

    /**
     * Constructs a GrantRoleRequest object with default parameters.
     */
    public GrantRoleRequest() {
        role = "";
        member = "";
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs a GrantRoleRequest object with the specified parameters.
     *
     * @param role  Name of the role in which membership will be granted. Must
     *              be an existing role.
     * @param member  Name of the user or role that will be granted membership
     *                in {@code role}. Must be an existing user or role.
     * @param options  Optional parameters. The default value is an empty
     *                 {@link Map}.
     */
    public GrantRoleRequest(String role, String member, Map<String, String> options) {
        this.role = (role == null) ? "" : role;
        this.member = (member == null) ? "" : member;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * Name of the role in which membership will be granted. Must be an
     * existing role.
     *
     * @return The current value of {@code role}.
     */
    public String getRole() {
        return role;
    }

    /**
     * Name of the role in which membership will be granted. Must be an
     * existing role.
     *
     * @param role  The new value for {@code role}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public GrantRoleRequest setRole(String role) {
        this.role = (role == null) ? "" : role;
        return this;
    }

    /**
     * Name of the user or role that will be granted membership in {@link
     * #getRole() role}. Must be an existing user or role.
     *
     * @return The current value of {@code member}.
     */
    public String getMember() {
        return member;
    }

    /**
     * Name of the user or role that will be granted membership in {@link
     * #getRole() role}. Must be an existing user or role.
     *
     * @param member  The new value for {@code member}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public GrantRoleRequest setMember(String member) {
        this.member = (member == null) ? "" : member;
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
    public GrantRoleRequest setOptions(Map<String, String> options) {
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
                return this.role;

            case 1:
                return this.member;

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
                this.role = (String)value;
                break;

            case 1:
                this.member = (String)value;
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

        GrantRoleRequest that = (GrantRoleRequest)obj;

        return ( this.role.equals( that.role )
                 && this.member.equals( that.member )
                 && this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "role" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.role ) );
        builder.append( ", " );
        builder.append( gd.toString( "member" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.member ) );
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
        hashCode = (31 * hashCode) + this.role.hashCode();
        hashCode = (31 * hashCode) + this.member.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
