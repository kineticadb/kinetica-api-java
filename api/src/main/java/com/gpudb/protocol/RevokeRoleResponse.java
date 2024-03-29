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
 * A set of results returned by {@link
 * com.gpudb.GPUdb#revokeRole(RevokeRoleRequest) GPUdb.revokeRole}.
 */
public class RevokeRoleResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("RevokeRoleResponse")
            .namespace("com.gpudb")
            .fields()
                .name("role").type().stringType().noDefault()
                .name("member").type().stringType().noDefault()
                .name("info").type().map().values().stringType().noDefault()
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
    private Map<String, String> info;

    /**
     * Constructs a RevokeRoleResponse object with default parameters.
     */
    public RevokeRoleResponse() {
    }

    /**
     * Value of {@link com.gpudb.protocol.RevokeRoleRequest#getRole() role}.
     *
     * @return The current value of {@code role}.
     */
    public String getRole() {
        return role;
    }

    /**
     * Value of {@link com.gpudb.protocol.RevokeRoleRequest#getRole() role}.
     *
     * @param role  The new value for {@code role}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public RevokeRoleResponse setRole(String role) {
        this.role = (role == null) ? "" : role;
        return this;
    }

    /**
     * Value of {@link com.gpudb.protocol.RevokeRoleRequest#getMember()
     * member}.
     *
     * @return The current value of {@code member}.
     */
    public String getMember() {
        return member;
    }

    /**
     * Value of {@link com.gpudb.protocol.RevokeRoleRequest#getMember()
     * member}.
     *
     * @param member  The new value for {@code member}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public RevokeRoleResponse setMember(String member) {
        this.member = (member == null) ? "" : member;
        return this;
    }

    /**
     * Additional information.
     *
     * @return The current value of {@code info}.
     */
    public Map<String, String> getInfo() {
        return info;
    }

    /**
     * Additional information.
     *
     * @param info  The new value for {@code info}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public RevokeRoleResponse setInfo(Map<String, String> info) {
        this.info = (info == null) ? new LinkedHashMap<String, String>() : info;
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
                return this.info;

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
                this.info = (Map<String, String>)value;
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

        RevokeRoleResponse that = (RevokeRoleResponse)obj;

        return ( this.role.equals( that.role )
                 && this.member.equals( that.member )
                 && this.info.equals( that.info ) );
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
        builder.append( gd.toString( "info" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.info ) );
        builder.append( "}" );

        return builder.toString();
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = (31 * hashCode) + this.role.hashCode();
        hashCode = (31 * hashCode) + this.member.hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }
}
