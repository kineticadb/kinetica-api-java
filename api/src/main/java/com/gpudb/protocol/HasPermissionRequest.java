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
 * A set of parameters for {@link
 * com.gpudb.GPUdb#hasPermission(HasPermissionRequest) GPUdb.hasPermission}.
 * <p>
 * Checks if the specified user has the specified permission on the specified
 * object.
 */
public class HasPermissionRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("HasPermissionRequest")
            .namespace("com.gpudb")
            .fields()
                .name("principal").type().stringType().noDefault()
                .name("object").type().stringType().noDefault()
                .name("objectType").type().stringType().noDefault()
                .name("permission").type().stringType().noDefault()
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
     * A set of string constants for the {@link HasPermissionRequest} parameter
     * {@link #getObjectType() objectType}.
     * <p>
     * The type of object being checked
     */
    public static final class ObjectType {
        /**
         * Context
         */
        public static final String CONTEXT = "context";

        /**
         * Credential
         */
        public static final String CREDENTIAL = "credential";

        /**
         * Data Sink
         */
        public static final String DATASINK = "datasink";

        /**
         * Data Source
         */
        public static final String DATASOURCE = "datasource";

        /**
         * KiFS File Directory
         */
        public static final String DIRECTORY = "directory";

        /**
         * A Graph object
         */
        public static final String GRAPH = "graph";

        /**
         * UDF Procedure
         */
        public static final String PROC = "proc";

        /**
         * Schema
         */
        public static final String SCHEMA = "schema";

        /**
         * SQL Procedure
         */
        public static final String SQL_PROC = "sql_proc";

        /**
         * System-level access
         */
        public static final String SYSTEM = "system";

        /**
         * Database Table
         */
        public static final String TABLE = "table";

        /**
         * Table monitor
         */
        public static final String TABLE_MONITOR = "table_monitor";

        private ObjectType() {  }
    }

    /**
     * A set of string constants for the {@link HasPermissionRequest} parameter
     * {@link #getPermission() permission}.
     * <p>
     * Permission to check for.
     */
    public static final class Permission {
        /**
         * Full read/write and administrative access on the object.
         */
        public static final String ADMIN = "admin";

        /**
         * Connect access on the given data source or data sink.
         */
        public static final String CONNECT = "connect";

        /**
         * Ability to create new objects of this type.
         */
        public static final String CREATE = "create";

        /**
         * Delete rows from tables.
         */
        public static final String DELETE = "delete";

        /**
         * Ability to Execute the Procedure object.
         */
        public static final String EXECUTE = "execute";

        /**
         * Insert access to tables.
         */
        public static final String INSERT = "insert";

        /**
         * Ability to read, list and use the object.
         */
        public static final String READ = "read";

        /**
         * Update access to the table.
         */
        public static final String UPDATE = "update";

        /**
         * Access to administer users and roles that do not have system_admin
         * permission.
         */
        public static final String USER_ADMIN = "user_admin";

        /**
         * Access to write, change and delete objects.
         */
        public static final String WRITE = "write";

        private Permission() {  }
    }

    /**
     * A set of string constants for the {@link HasPermissionRequest} parameter
     * {@link #getOptions() options}.
     * <p>
     * Optional parameters.
     */
    public static final class Options {
        /**
         * If {@link Options#FALSE FALSE} will return an error if the provided
         * {@link #getObject() object} does not exist or is blank. If {@link
         * Options#TRUE TRUE} then it will return {@link
         * com.gpudb.protocol.HasPermissionResponse.HasPermission#FALSE FALSE}
         * for {@link
         * com.gpudb.protocol.HasPermissionResponse#getHasPermission()
         * hasPermission}.
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

    private String principal;
    private String object;
    private String objectType;
    private String permission;
    private Map<String, String> options;

    /**
     * Constructs a HasPermissionRequest object with default parameters.
     */
    public HasPermissionRequest() {
        principal = "";
        object = "";
        objectType = "";
        permission = "";
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs a HasPermissionRequest object with the specified parameters.
     *
     * @param principal  Name of the user for which the permission is being
     *                   checked. Must be an existing user. If blank, will use
     *                   the current user. The default value is ''.
     * @param object  Name of object to check for the requested permission.  It
     *                is recommended to use a fully-qualified name when
     *                possible.
     * @param objectType  The type of object being checked.
     *                    Supported values:
     *                    <ul>
     *                        <li>{@link ObjectType#CONTEXT CONTEXT}: Context
     *                        <li>{@link ObjectType#CREDENTIAL CREDENTIAL}:
     *                            Credential
     *                        <li>{@link ObjectType#DATASINK DATASINK}: Data
     *                            Sink
     *                        <li>{@link ObjectType#DATASOURCE DATASOURCE}:
     *                            Data Source
     *                        <li>{@link ObjectType#DIRECTORY DIRECTORY}: KiFS
     *                            File Directory
     *                        <li>{@link ObjectType#GRAPH GRAPH}: A Graph
     *                            object
     *                        <li>{@link ObjectType#PROC PROC}: UDF Procedure
     *                        <li>{@link ObjectType#SCHEMA SCHEMA}: Schema
     *                        <li>{@link ObjectType#SQL_PROC SQL_PROC}: SQL
     *                            Procedure
     *                        <li>{@link ObjectType#SYSTEM SYSTEM}:
     *                            System-level access
     *                        <li>{@link ObjectType#TABLE TABLE}: Database
     *                            Table
     *                        <li>{@link ObjectType#TABLE_MONITOR
     *                            TABLE_MONITOR}: Table monitor
     *                    </ul>
     * @param permission  Permission to check for.
     *                    Supported values:
     *                    <ul>
     *                        <li>{@link Permission#ADMIN ADMIN}: Full
     *                            read/write and administrative access on the
     *                            object.
     *                        <li>{@link Permission#CONNECT CONNECT}: Connect
     *                            access on the given data source or data sink.
     *                        <li>{@link Permission#CREATE CREATE}: Ability to
     *                            create new objects of this type.
     *                        <li>{@link Permission#DELETE DELETE}: Delete rows
     *                            from tables.
     *                        <li>{@link Permission#EXECUTE EXECUTE}: Ability
     *                            to Execute the Procedure object.
     *                        <li>{@link Permission#INSERT INSERT}: Insert
     *                            access to tables.
     *                        <li>{@link Permission#READ READ}: Ability to
     *                            read, list and use the object.
     *                        <li>{@link Permission#UPDATE UPDATE}: Update
     *                            access to the table.
     *                        <li>{@link Permission#USER_ADMIN USER_ADMIN}:
     *                            Access to administer users and roles that do
     *                            not have system_admin permission.
     *                        <li>{@link Permission#WRITE WRITE}: Access to
     *                            write, change and delete objects.
     *                    </ul>
     * @param options  Optional parameters.
     *                 <ul>
     *                     <li>{@link Options#NO_ERROR_IF_NOT_EXISTS
     *                         NO_ERROR_IF_NOT_EXISTS}: If {@link Options#FALSE
     *                         FALSE} will return an error if the provided
     *                         {@code object} does not exist or is blank. If
     *                         {@link Options#TRUE TRUE} then it will return
     *                         {@link
     *                         com.gpudb.protocol.HasPermissionResponse.HasPermission#FALSE
     *                         FALSE} for {@link
     *                         com.gpudb.protocol.HasPermissionResponse#getHasPermission()
     *                         hasPermission}.
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
    public HasPermissionRequest(String principal, String object, String objectType, String permission, Map<String, String> options) {
        this.principal = (principal == null) ? "" : principal;
        this.object = (object == null) ? "" : object;
        this.objectType = (objectType == null) ? "" : objectType;
        this.permission = (permission == null) ? "" : permission;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * Name of the user for which the permission is being checked. Must be an
     * existing user. If blank, will use the current user. The default value is
     * ''.
     *
     * @return The current value of {@code principal}.
     */
    public String getPrincipal() {
        return principal;
    }

    /**
     * Name of the user for which the permission is being checked. Must be an
     * existing user. If blank, will use the current user. The default value is
     * ''.
     *
     * @param principal  The new value for {@code principal}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public HasPermissionRequest setPrincipal(String principal) {
        this.principal = (principal == null) ? "" : principal;
        return this;
    }

    /**
     * Name of object to check for the requested permission.  It is recommended
     * to use a fully-qualified name when possible.
     *
     * @return The current value of {@code object}.
     */
    public String getObject() {
        return object;
    }

    /**
     * Name of object to check for the requested permission.  It is recommended
     * to use a fully-qualified name when possible.
     *
     * @param object  The new value for {@code object}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public HasPermissionRequest setObject(String object) {
        this.object = (object == null) ? "" : object;
        return this;
    }

    /**
     * The type of object being checked.
     * Supported values:
     * <ul>
     *     <li>{@link ObjectType#CONTEXT CONTEXT}: Context
     *     <li>{@link ObjectType#CREDENTIAL CREDENTIAL}: Credential
     *     <li>{@link ObjectType#DATASINK DATASINK}: Data Sink
     *     <li>{@link ObjectType#DATASOURCE DATASOURCE}: Data Source
     *     <li>{@link ObjectType#DIRECTORY DIRECTORY}: KiFS File Directory
     *     <li>{@link ObjectType#GRAPH GRAPH}: A Graph object
     *     <li>{@link ObjectType#PROC PROC}: UDF Procedure
     *     <li>{@link ObjectType#SCHEMA SCHEMA}: Schema
     *     <li>{@link ObjectType#SQL_PROC SQL_PROC}: SQL Procedure
     *     <li>{@link ObjectType#SYSTEM SYSTEM}: System-level access
     *     <li>{@link ObjectType#TABLE TABLE}: Database Table
     *     <li>{@link ObjectType#TABLE_MONITOR TABLE_MONITOR}: Table monitor
     * </ul>
     *
     * @return The current value of {@code objectType}.
     */
    public String getObjectType() {
        return objectType;
    }

    /**
     * The type of object being checked.
     * Supported values:
     * <ul>
     *     <li>{@link ObjectType#CONTEXT CONTEXT}: Context
     *     <li>{@link ObjectType#CREDENTIAL CREDENTIAL}: Credential
     *     <li>{@link ObjectType#DATASINK DATASINK}: Data Sink
     *     <li>{@link ObjectType#DATASOURCE DATASOURCE}: Data Source
     *     <li>{@link ObjectType#DIRECTORY DIRECTORY}: KiFS File Directory
     *     <li>{@link ObjectType#GRAPH GRAPH}: A Graph object
     *     <li>{@link ObjectType#PROC PROC}: UDF Procedure
     *     <li>{@link ObjectType#SCHEMA SCHEMA}: Schema
     *     <li>{@link ObjectType#SQL_PROC SQL_PROC}: SQL Procedure
     *     <li>{@link ObjectType#SYSTEM SYSTEM}: System-level access
     *     <li>{@link ObjectType#TABLE TABLE}: Database Table
     *     <li>{@link ObjectType#TABLE_MONITOR TABLE_MONITOR}: Table monitor
     * </ul>
     *
     * @param objectType  The new value for {@code objectType}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public HasPermissionRequest setObjectType(String objectType) {
        this.objectType = (objectType == null) ? "" : objectType;
        return this;
    }

    /**
     * Permission to check for.
     * Supported values:
     * <ul>
     *     <li>{@link Permission#ADMIN ADMIN}: Full read/write and
     *         administrative access on the object.
     *     <li>{@link Permission#CONNECT CONNECT}: Connect access on the given
     *         data source or data sink.
     *     <li>{@link Permission#CREATE CREATE}: Ability to create new objects
     *         of this type.
     *     <li>{@link Permission#DELETE DELETE}: Delete rows from tables.
     *     <li>{@link Permission#EXECUTE EXECUTE}: Ability to Execute the
     *         Procedure object.
     *     <li>{@link Permission#INSERT INSERT}: Insert access to tables.
     *     <li>{@link Permission#READ READ}: Ability to read, list and use the
     *         object.
     *     <li>{@link Permission#UPDATE UPDATE}: Update access to the table.
     *     <li>{@link Permission#USER_ADMIN USER_ADMIN}: Access to administer
     *         users and roles that do not have system_admin permission.
     *     <li>{@link Permission#WRITE WRITE}: Access to write, change and
     *         delete objects.
     * </ul>
     *
     * @return The current value of {@code permission}.
     */
    public String getPermission() {
        return permission;
    }

    /**
     * Permission to check for.
     * Supported values:
     * <ul>
     *     <li>{@link Permission#ADMIN ADMIN}: Full read/write and
     *         administrative access on the object.
     *     <li>{@link Permission#CONNECT CONNECT}: Connect access on the given
     *         data source or data sink.
     *     <li>{@link Permission#CREATE CREATE}: Ability to create new objects
     *         of this type.
     *     <li>{@link Permission#DELETE DELETE}: Delete rows from tables.
     *     <li>{@link Permission#EXECUTE EXECUTE}: Ability to Execute the
     *         Procedure object.
     *     <li>{@link Permission#INSERT INSERT}: Insert access to tables.
     *     <li>{@link Permission#READ READ}: Ability to read, list and use the
     *         object.
     *     <li>{@link Permission#UPDATE UPDATE}: Update access to the table.
     *     <li>{@link Permission#USER_ADMIN USER_ADMIN}: Access to administer
     *         users and roles that do not have system_admin permission.
     *     <li>{@link Permission#WRITE WRITE}: Access to write, change and
     *         delete objects.
     * </ul>
     *
     * @param permission  The new value for {@code permission}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public HasPermissionRequest setPermission(String permission) {
        this.permission = (permission == null) ? "" : permission;
        return this;
    }

    /**
     * Optional parameters.
     * <ul>
     *     <li>{@link Options#NO_ERROR_IF_NOT_EXISTS NO_ERROR_IF_NOT_EXISTS}:
     *         If {@link Options#FALSE FALSE} will return an error if the
     *         provided {@link #getObject() object} does not exist or is blank.
     *         If {@link Options#TRUE TRUE} then it will return {@link
     *         com.gpudb.protocol.HasPermissionResponse.HasPermission#FALSE
     *         FALSE} for {@link
     *         com.gpudb.protocol.HasPermissionResponse#getHasPermission()
     *         hasPermission}.
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
     *         provided {@link #getObject() object} does not exist or is blank.
     *         If {@link Options#TRUE TRUE} then it will return {@link
     *         com.gpudb.protocol.HasPermissionResponse.HasPermission#FALSE
     *         FALSE} for {@link
     *         com.gpudb.protocol.HasPermissionResponse#getHasPermission()
     *         hasPermission}.
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
    public HasPermissionRequest setOptions(Map<String, String> options) {
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
                return this.object;

            case 2:
                return this.objectType;

            case 3:
                return this.permission;

            case 4:
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
                this.object = (String)value;
                break;

            case 2:
                this.objectType = (String)value;
                break;

            case 3:
                this.permission = (String)value;
                break;

            case 4:
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

        HasPermissionRequest that = (HasPermissionRequest)obj;

        return ( this.principal.equals( that.principal )
                 && this.object.equals( that.object )
                 && this.objectType.equals( that.objectType )
                 && this.permission.equals( that.permission )
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
        builder.append( gd.toString( "object" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.object ) );
        builder.append( ", " );
        builder.append( gd.toString( "objectType" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.objectType ) );
        builder.append( ", " );
        builder.append( gd.toString( "permission" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.permission ) );
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
        hashCode = (31 * hashCode) + this.object.hashCode();
        hashCode = (31 * hashCode) + this.objectType.hashCode();
        hashCode = (31 * hashCode) + this.permission.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
