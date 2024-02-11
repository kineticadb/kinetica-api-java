/*
 *  This file was autogenerated by the Kinetica schema processor.
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
 * com.gpudb.GPUdb#createJoinTable(CreateJoinTableRequest)
 * GPUdb.createJoinTable}.
 * <p>
 * Creates a table that is the result of a SQL JOIN.
 * <p>
 * For join details and examples see: <a
 * href="../../../../../../concepts/joins/" target="_top">Joins</a>.  For
 * limitations, see <a
 * href="../../../../../../concepts/joins/#limitations-cautions"
 * target="_top">Join Limitations and Cautions</a>.
 */
public class CreateJoinTableRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("CreateJoinTableRequest")
            .namespace("com.gpudb")
            .fields()
                .name("joinTableName").type().stringType().noDefault()
                .name("tableNames").type().array().items().stringType().noDefault()
                .name("columnNames").type().array().items().stringType().noDefault()
                .name("expressions").type().array().items().stringType().noDefault()
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
     * A set of string constants for the {@link CreateJoinTableRequest}
     * parameter {@link #getOptions() options}.
     * <p>
     * Optional parameters.
     */
    public static final class Options {
        /**
         * If {@link Options#TRUE TRUE}, a unique temporary table name will be
         * generated in the sys_temp schema and used in place of {@link
         * #getJoinTableName() joinTableName}. This is always allowed even if
         * the caller does not have permission to create tables. The generated
         * name is returned in {@link
         * com.gpudb.protocol.CreateJoinTableResponse.Info#QUALIFIED_JOIN_TABLE_NAME
         * QUALIFIED_JOIN_TABLE_NAME}.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}
         *     <li>{@link Options#FALSE FALSE}
         * </ul>
         * The default value is {@link Options#FALSE FALSE}.
         */
        public static final String CREATE_TEMP_TABLE = "create_temp_table";

        public static final String TRUE = "true";
        public static final String FALSE = "false";

        /**
         * [DEPRECATED--please specify the containing schema for the join as
         * part of {@link #getJoinTableName() joinTableName} and use {@link
         * com.gpudb.GPUdb#createSchema(CreateSchemaRequest)
         * GPUdb.createSchema} to create the schema if non-existent]  Name of a
         * schema for the join. If the schema is non-existent, it will be
         * automatically created. The default value is ''.
         */
        public static final String COLLECTION_NAME = "collection_name";

        /**
         * No longer used.
         */
        public static final String MAX_QUERY_DIMENSIONS = "max_query_dimensions";

        /**
         * Use more memory to speed up the joining of tables.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}
         *     <li>{@link Options#FALSE FALSE}
         * </ul>
         * The default value is {@link Options#FALSE FALSE}.
         */
        public static final String OPTIMIZE_LOOKUPS = "optimize_lookups";

        /**
         * The <a href="../../../../../../rm/concepts/#tier-strategies"
         * target="_top">tier strategy</a> for the table and its columns.
         */
        public static final String STRATEGY_DEFINITION = "strategy_definition";

        /**
         * Sets the <a href="../../../../../../concepts/ttl/"
         * target="_top">TTL</a> of the join table specified in {@link
         * #getJoinTableName() joinTableName}.
         */
        public static final String TTL = "ttl";

        /**
         * view this projection is part of. The default value is ''.
         */
        public static final String VIEW_ID = "view_id";

        /**
         * Return a count of 0 for the join table for logging and for {@link
         * com.gpudb.GPUdb#showTable(ShowTableRequest) GPUdb.showTable};
         * optimization needed for large overlapped equi-join stencils. The
         * default value is 'false'.
         */
        public static final String NO_COUNT = "no_count";

        /**
         * Maximum number of records per joined-chunk for this table. Defaults
         * to the gpudb.conf file chunk size
         */
        public static final String CHUNK_SIZE = "chunk_size";

        private Options() {  }
    }

    private String joinTableName;
    private List<String> tableNames;
    private List<String> columnNames;
    private List<String> expressions;
    private Map<String, String> options;

    /**
     * Constructs a CreateJoinTableRequest object with default parameters.
     */
    public CreateJoinTableRequest() {
        joinTableName = "";
        tableNames = new ArrayList<>();
        columnNames = new ArrayList<>();
        expressions = new ArrayList<>();
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs a CreateJoinTableRequest object with the specified
     * parameters.
     *
     * @param joinTableName  Name of the join table to be created, in
     *                       [schema_name.]table_name format, using standard <a
     *                       href="../../../../../../concepts/tables/#table-name-resolution"
     *                       target="_top">name resolution rules</a> and
     *                       meeting <a
     *                       href="../../../../../../concepts/tables/#table-naming-criteria"
     *                       target="_top">table naming criteria</a>.
     * @param tableNames  The list of table names composing the join, each in
     *                    [schema_name.]table_name format, using standard <a
     *                    href="../../../../../../concepts/tables/#table-name-resolution"
     *                    target="_top">name resolution rules</a>.  Corresponds
     *                    to a SQL statement FROM clause.
     * @param columnNames  List of member table columns or column expressions
     *                     to be included in the join. Columns can be prefixed
     *                     with 'table_id.column_name', where 'table_id' is the
     *                     table name or alias.  Columns can be aliased via the
     *                     syntax 'column_name as alias'. Wild cards '*' can be
     *                     used to include all columns across member tables or
     *                     'table_id.*' for all of a single table's columns.
     *                     Columns and column expressions composing the join
     *                     must be uniquely named or aliased--therefore, the
     *                     '*' wild card cannot be used if column names aren't
     *                     unique across all tables.
     * @param expressions  An optional list of expressions to combine and
     *                     filter the joined tables.  Corresponds to a SQL
     *                     statement WHERE clause. For details see: <a
     *                     href="../../../../../../concepts/expressions/"
     *                     target="_top">expressions</a>. The default value is
     *                     an empty {@link List}.
     * @param options  Optional parameters.
     *                 <ul>
     *                     <li>{@link Options#CREATE_TEMP_TABLE
     *                         CREATE_TEMP_TABLE}: If {@link Options#TRUE
     *                         TRUE}, a unique temporary table name will be
     *                         generated in the sys_temp schema and used in
     *                         place of {@code joinTableName}. This is always
     *                         allowed even if the caller does not have
     *                         permission to create tables. The generated name
     *                         is returned in {@link
     *                         com.gpudb.protocol.CreateJoinTableResponse.Info#QUALIFIED_JOIN_TABLE_NAME
     *                         QUALIFIED_JOIN_TABLE_NAME}.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#TRUE TRUE}
     *                             <li>{@link Options#FALSE FALSE}
     *                         </ul>
     *                         The default value is {@link Options#FALSE
     *                         FALSE}.
     *                     <li>{@link Options#COLLECTION_NAME COLLECTION_NAME}:
     *                         [DEPRECATED--please specify the containing
     *                         schema for the join as part of {@code
     *                         joinTableName} and use {@link
     *                         com.gpudb.GPUdb#createSchema(CreateSchemaRequest)
     *                         GPUdb.createSchema} to create the schema if
     *                         non-existent]  Name of a schema for the join. If
     *                         the schema is non-existent, it will be
     *                         automatically created. The default value is ''.
     *                     <li>{@link Options#MAX_QUERY_DIMENSIONS
     *                         MAX_QUERY_DIMENSIONS}: No longer used.
     *                     <li>{@link Options#OPTIMIZE_LOOKUPS
     *                         OPTIMIZE_LOOKUPS}: Use more memory to speed up
     *                         the joining of tables.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#TRUE TRUE}
     *                             <li>{@link Options#FALSE FALSE}
     *                         </ul>
     *                         The default value is {@link Options#FALSE
     *                         FALSE}.
     *                     <li>{@link Options#STRATEGY_DEFINITION
     *                         STRATEGY_DEFINITION}: The <a
     *                         href="../../../../../../rm/concepts/#tier-strategies"
     *                         target="_top">tier strategy</a> for the table
     *                         and its columns.
     *                     <li>{@link Options#TTL TTL}: Sets the <a
     *                         href="../../../../../../concepts/ttl/"
     *                         target="_top">TTL</a> of the join table
     *                         specified in {@code joinTableName}.
     *                     <li>{@link Options#VIEW_ID VIEW_ID}: view this
     *                         projection is part of. The default value is ''.
     *                     <li>{@link Options#NO_COUNT NO_COUNT}: Return a
     *                         count of 0 for the join table for logging and
     *                         for {@link
     *                         com.gpudb.GPUdb#showTable(ShowTableRequest)
     *                         GPUdb.showTable}; optimization needed for large
     *                         overlapped equi-join stencils. The default value
     *                         is 'false'.
     *                     <li>{@link Options#CHUNK_SIZE CHUNK_SIZE}: Maximum
     *                         number of records per joined-chunk for this
     *                         table. Defaults to the gpudb.conf file chunk
     *                         size
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     */
    public CreateJoinTableRequest(String joinTableName, List<String> tableNames, List<String> columnNames, List<String> expressions, Map<String, String> options) {
        this.joinTableName = (joinTableName == null) ? "" : joinTableName;
        this.tableNames = (tableNames == null) ? new ArrayList<String>() : tableNames;
        this.columnNames = (columnNames == null) ? new ArrayList<String>() : columnNames;
        this.expressions = (expressions == null) ? new ArrayList<String>() : expressions;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * Name of the join table to be created, in [schema_name.]table_name
     * format, using standard <a
     * href="../../../../../../concepts/tables/#table-name-resolution"
     * target="_top">name resolution rules</a> and meeting <a
     * href="../../../../../../concepts/tables/#table-naming-criteria"
     * target="_top">table naming criteria</a>.
     *
     * @return The current value of {@code joinTableName}.
     */
    public String getJoinTableName() {
        return joinTableName;
    }

    /**
     * Name of the join table to be created, in [schema_name.]table_name
     * format, using standard <a
     * href="../../../../../../concepts/tables/#table-name-resolution"
     * target="_top">name resolution rules</a> and meeting <a
     * href="../../../../../../concepts/tables/#table-naming-criteria"
     * target="_top">table naming criteria</a>.
     *
     * @param joinTableName  The new value for {@code joinTableName}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public CreateJoinTableRequest setJoinTableName(String joinTableName) {
        this.joinTableName = (joinTableName == null) ? "" : joinTableName;
        return this;
    }

    /**
     * The list of table names composing the join, each in
     * [schema_name.]table_name format, using standard <a
     * href="../../../../../../concepts/tables/#table-name-resolution"
     * target="_top">name resolution rules</a>.  Corresponds to a SQL statement
     * FROM clause.
     *
     * @return The current value of {@code tableNames}.
     */
    public List<String> getTableNames() {
        return tableNames;
    }

    /**
     * The list of table names composing the join, each in
     * [schema_name.]table_name format, using standard <a
     * href="../../../../../../concepts/tables/#table-name-resolution"
     * target="_top">name resolution rules</a>.  Corresponds to a SQL statement
     * FROM clause.
     *
     * @param tableNames  The new value for {@code tableNames}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public CreateJoinTableRequest setTableNames(List<String> tableNames) {
        this.tableNames = (tableNames == null) ? new ArrayList<String>() : tableNames;
        return this;
    }

    /**
     * List of member table columns or column expressions to be included in the
     * join. Columns can be prefixed with 'table_id.column_name', where
     * 'table_id' is the table name or alias.  Columns can be aliased via the
     * syntax 'column_name as alias'. Wild cards '*' can be used to include all
     * columns across member tables or 'table_id.*' for all of a single table's
     * columns.  Columns and column expressions composing the join must be
     * uniquely named or aliased--therefore, the '*' wild card cannot be used
     * if column names aren't unique across all tables.
     *
     * @return The current value of {@code columnNames}.
     */
    public List<String> getColumnNames() {
        return columnNames;
    }

    /**
     * List of member table columns or column expressions to be included in the
     * join. Columns can be prefixed with 'table_id.column_name', where
     * 'table_id' is the table name or alias.  Columns can be aliased via the
     * syntax 'column_name as alias'. Wild cards '*' can be used to include all
     * columns across member tables or 'table_id.*' for all of a single table's
     * columns.  Columns and column expressions composing the join must be
     * uniquely named or aliased--therefore, the '*' wild card cannot be used
     * if column names aren't unique across all tables.
     *
     * @param columnNames  The new value for {@code columnNames}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public CreateJoinTableRequest setColumnNames(List<String> columnNames) {
        this.columnNames = (columnNames == null) ? new ArrayList<String>() : columnNames;
        return this;
    }

    /**
     * An optional list of expressions to combine and filter the joined tables.
     * Corresponds to a SQL statement WHERE clause. For details see: <a
     * href="../../../../../../concepts/expressions/"
     * target="_top">expressions</a>. The default value is an empty {@link
     * List}.
     *
     * @return The current value of {@code expressions}.
     */
    public List<String> getExpressions() {
        return expressions;
    }

    /**
     * An optional list of expressions to combine and filter the joined tables.
     * Corresponds to a SQL statement WHERE clause. For details see: <a
     * href="../../../../../../concepts/expressions/"
     * target="_top">expressions</a>. The default value is an empty {@link
     * List}.
     *
     * @param expressions  The new value for {@code expressions}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public CreateJoinTableRequest setExpressions(List<String> expressions) {
        this.expressions = (expressions == null) ? new ArrayList<String>() : expressions;
        return this;
    }

    /**
     * Optional parameters.
     * <ul>
     *     <li>{@link Options#CREATE_TEMP_TABLE CREATE_TEMP_TABLE}: If {@link
     *         Options#TRUE TRUE}, a unique temporary table name will be
     *         generated in the sys_temp schema and used in place of {@link
     *         #getJoinTableName() joinTableName}. This is always allowed even
     *         if the caller does not have permission to create tables. The
     *         generated name is returned in {@link
     *         com.gpudb.protocol.CreateJoinTableResponse.Info#QUALIFIED_JOIN_TABLE_NAME
     *         QUALIFIED_JOIN_TABLE_NAME}.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     *     <li>{@link Options#COLLECTION_NAME COLLECTION_NAME}:
     *         [DEPRECATED--please specify the containing schema for the join
     *         as part of {@link #getJoinTableName() joinTableName} and use
     *         {@link com.gpudb.GPUdb#createSchema(CreateSchemaRequest)
     *         GPUdb.createSchema} to create the schema if non-existent]  Name
     *         of a schema for the join. If the schema is non-existent, it will
     *         be automatically created. The default value is ''.
     *     <li>{@link Options#MAX_QUERY_DIMENSIONS MAX_QUERY_DIMENSIONS}: No
     *         longer used.
     *     <li>{@link Options#OPTIMIZE_LOOKUPS OPTIMIZE_LOOKUPS}: Use more
     *         memory to speed up the joining of tables.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     *     <li>{@link Options#STRATEGY_DEFINITION STRATEGY_DEFINITION}: The <a
     *         href="../../../../../../rm/concepts/#tier-strategies"
     *         target="_top">tier strategy</a> for the table and its columns.
     *     <li>{@link Options#TTL TTL}: Sets the <a
     *         href="../../../../../../concepts/ttl/" target="_top">TTL</a> of
     *         the join table specified in {@link #getJoinTableName()
     *         joinTableName}.
     *     <li>{@link Options#VIEW_ID VIEW_ID}: view this projection is part
     *         of. The default value is ''.
     *     <li>{@link Options#NO_COUNT NO_COUNT}: Return a count of 0 for the
     *         join table for logging and for {@link
     *         com.gpudb.GPUdb#showTable(ShowTableRequest) GPUdb.showTable};
     *         optimization needed for large overlapped equi-join stencils. The
     *         default value is 'false'.
     *     <li>{@link Options#CHUNK_SIZE CHUNK_SIZE}: Maximum number of records
     *         per joined-chunk for this table. Defaults to the gpudb.conf file
     *         chunk size
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
     *     <li>{@link Options#CREATE_TEMP_TABLE CREATE_TEMP_TABLE}: If {@link
     *         Options#TRUE TRUE}, a unique temporary table name will be
     *         generated in the sys_temp schema and used in place of {@link
     *         #getJoinTableName() joinTableName}. This is always allowed even
     *         if the caller does not have permission to create tables. The
     *         generated name is returned in {@link
     *         com.gpudb.protocol.CreateJoinTableResponse.Info#QUALIFIED_JOIN_TABLE_NAME
     *         QUALIFIED_JOIN_TABLE_NAME}.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     *     <li>{@link Options#COLLECTION_NAME COLLECTION_NAME}:
     *         [DEPRECATED--please specify the containing schema for the join
     *         as part of {@link #getJoinTableName() joinTableName} and use
     *         {@link com.gpudb.GPUdb#createSchema(CreateSchemaRequest)
     *         GPUdb.createSchema} to create the schema if non-existent]  Name
     *         of a schema for the join. If the schema is non-existent, it will
     *         be automatically created. The default value is ''.
     *     <li>{@link Options#MAX_QUERY_DIMENSIONS MAX_QUERY_DIMENSIONS}: No
     *         longer used.
     *     <li>{@link Options#OPTIMIZE_LOOKUPS OPTIMIZE_LOOKUPS}: Use more
     *         memory to speed up the joining of tables.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     *     <li>{@link Options#STRATEGY_DEFINITION STRATEGY_DEFINITION}: The <a
     *         href="../../../../../../rm/concepts/#tier-strategies"
     *         target="_top">tier strategy</a> for the table and its columns.
     *     <li>{@link Options#TTL TTL}: Sets the <a
     *         href="../../../../../../concepts/ttl/" target="_top">TTL</a> of
     *         the join table specified in {@link #getJoinTableName()
     *         joinTableName}.
     *     <li>{@link Options#VIEW_ID VIEW_ID}: view this projection is part
     *         of. The default value is ''.
     *     <li>{@link Options#NO_COUNT NO_COUNT}: Return a count of 0 for the
     *         join table for logging and for {@link
     *         com.gpudb.GPUdb#showTable(ShowTableRequest) GPUdb.showTable};
     *         optimization needed for large overlapped equi-join stencils. The
     *         default value is 'false'.
     *     <li>{@link Options#CHUNK_SIZE CHUNK_SIZE}: Maximum number of records
     *         per joined-chunk for this table. Defaults to the gpudb.conf file
     *         chunk size
     * </ul>
     * The default value is an empty {@link Map}.
     *
     * @param options  The new value for {@code options}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public CreateJoinTableRequest setOptions(Map<String, String> options) {
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
                return this.joinTableName;

            case 1:
                return this.tableNames;

            case 2:
                return this.columnNames;

            case 3:
                return this.expressions;

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
                this.joinTableName = (String)value;
                break;

            case 1:
                this.tableNames = (List<String>)value;
                break;

            case 2:
                this.columnNames = (List<String>)value;
                break;

            case 3:
                this.expressions = (List<String>)value;
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

        CreateJoinTableRequest that = (CreateJoinTableRequest)obj;

        return ( this.joinTableName.equals( that.joinTableName )
                 && this.tableNames.equals( that.tableNames )
                 && this.columnNames.equals( that.columnNames )
                 && this.expressions.equals( that.expressions )
                 && this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "joinTableName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.joinTableName ) );
        builder.append( ", " );
        builder.append( gd.toString( "tableNames" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.tableNames ) );
        builder.append( ", " );
        builder.append( gd.toString( "columnNames" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.columnNames ) );
        builder.append( ", " );
        builder.append( gd.toString( "expressions" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.expressions ) );
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
        hashCode = (31 * hashCode) + this.joinTableName.hashCode();
        hashCode = (31 * hashCode) + this.tableNames.hashCode();
        hashCode = (31 * hashCode) + this.columnNames.hashCode();
        hashCode = (31 * hashCode) + this.expressions.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
