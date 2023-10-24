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
 * com.gpudb.GPUdb#grantPermissionProc(GrantPermissionProcRequest)}.
 * <p>
 * Grants a proc-level permission to a user or role.
 */
public class GrantPermissionProcRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("GrantPermissionProcRequest")
            .namespace("com.gpudb")
            .fields()
                .name("name").type().stringType().noDefault()
                .name("permission").type().stringType().noDefault()
                .name("procName").type().stringType().noDefault()
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
     * Permission to grant to the user or role.
     * Supported values:
     * <ul>
     *         <li> {@link
     * com.gpudb.protocol.GrantPermissionProcRequest.Permission#PROC_ADMIN
     * PROC_ADMIN}: Admin access to the proc.
     *         <li> {@link
     * com.gpudb.protocol.GrantPermissionProcRequest.Permission#PROC_EXECUTE
     * PROC_EXECUTE}: Execute access to the proc.
     * </ul>
     * A set of string constants for the parameter {@code permission}.
     */
    public static final class Permission {

        /**
         * Admin access to the proc.
         */
        public static final String PROC_ADMIN = "proc_admin";

        /**
         * Execute access to the proc.
         */
        public static final String PROC_EXECUTE = "proc_execute";

        private Permission() {  }
    }

    private String name;
    private String permission;
    private String procName;
    private Map<String, String> options;


    /**
     * Constructs a GrantPermissionProcRequest object with default parameters.
     */
    public GrantPermissionProcRequest() {
        name = "";
        permission = "";
        procName = "";
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs a GrantPermissionProcRequest object with the specified
     * parameters.
     * 
     * @param name  Name of the user or role to which the permission will be
     *              granted. Must be an existing user or role.
     * @param permission  Permission to grant to the user or role.
     *                    Supported values:
     *                    <ul>
     *                            <li> {@link
     *                    com.gpudb.protocol.GrantPermissionProcRequest.Permission#PROC_ADMIN
     *                    PROC_ADMIN}: Admin access to the proc.
     *                            <li> {@link
     *                    com.gpudb.protocol.GrantPermissionProcRequest.Permission#PROC_EXECUTE
     *                    PROC_EXECUTE}: Execute access to the proc.
     *                    </ul>
     * @param procName  Name of the proc to which the permission grants access.
     *                  Must be an existing proc, or an empty string to grant
     *                  access to all procs.
     * @param options  Optional parameters.  The default value is an empty
     *                 {@link Map}.
     * 
     */
    public GrantPermissionProcRequest(String name, String permission, String procName, Map<String, String> options) {
        this.name = (name == null) ? "" : name;
        this.permission = (permission == null) ? "" : permission;
        this.procName = (procName == null) ? "" : procName;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * 
     * @return Name of the user or role to which the permission will be
     *         granted. Must be an existing user or role.
     * 
     */
    public String getName() {
        return name;
    }

    /**
     * 
     * @param name  Name of the user or role to which the permission will be
     *              granted. Must be an existing user or role.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public GrantPermissionProcRequest setName(String name) {
        this.name = (name == null) ? "" : name;
        return this;
    }

    /**
     * 
     * @return Permission to grant to the user or role.
     *         Supported values:
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.GrantPermissionProcRequest.Permission#PROC_ADMIN
     *         PROC_ADMIN}: Admin access to the proc.
     *                 <li> {@link
     *         com.gpudb.protocol.GrantPermissionProcRequest.Permission#PROC_EXECUTE
     *         PROC_EXECUTE}: Execute access to the proc.
     *         </ul>
     * 
     */
    public String getPermission() {
        return permission;
    }

    /**
     * 
     * @param permission  Permission to grant to the user or role.
     *                    Supported values:
     *                    <ul>
     *                            <li> {@link
     *                    com.gpudb.protocol.GrantPermissionProcRequest.Permission#PROC_ADMIN
     *                    PROC_ADMIN}: Admin access to the proc.
     *                            <li> {@link
     *                    com.gpudb.protocol.GrantPermissionProcRequest.Permission#PROC_EXECUTE
     *                    PROC_EXECUTE}: Execute access to the proc.
     *                    </ul>
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public GrantPermissionProcRequest setPermission(String permission) {
        this.permission = (permission == null) ? "" : permission;
        return this;
    }

    /**
     * 
     * @return Name of the proc to which the permission grants access. Must be
     *         an existing proc, or an empty string to grant access to all
     *         procs.
     * 
     */
    public String getProcName() {
        return procName;
    }

    /**
     * 
     * @param procName  Name of the proc to which the permission grants access.
     *                  Must be an existing proc, or an empty string to grant
     *                  access to all procs.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public GrantPermissionProcRequest setProcName(String procName) {
        this.procName = (procName == null) ? "" : procName;
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
    public GrantPermissionProcRequest setOptions(Map<String, String> options) {
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
                return this.permission;

            case 2:
                return this.procName;

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
                this.permission = (String)value;
                break;

            case 2:
                this.procName = (String)value;
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

        GrantPermissionProcRequest that = (GrantPermissionProcRequest)obj;

        return ( this.name.equals( that.name )
                 && this.permission.equals( that.permission )
                 && this.procName.equals( that.procName )
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
        builder.append( gd.toString( "permission" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.permission ) );
        builder.append( ", " );
        builder.append( gd.toString( "procName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.procName ) );
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
        hashCode = (31 * hashCode) + this.permission.hashCode();
        hashCode = (31 * hashCode) + this.procName.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }

}
