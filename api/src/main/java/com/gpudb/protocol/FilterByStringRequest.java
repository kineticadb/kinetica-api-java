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
 * com.gpudb.GPUdb#filterByString(FilterByStringRequest) GPUdb.filterByString}.
 * <p>
 * Calculates which objects from a table or view match a string expression for
 * the given string columns. Setting {@link Options#CASE_SENSITIVE
 * CASE_SENSITIVE} can modify case sensitivity in matching for all modes except
 * {@link Mode#SEARCH SEARCH}. For {@link Mode#SEARCH SEARCH} mode details and
 * limitations, see <a href="../../../../../../concepts/full_text_search/"
 * target="_top">Full Text Search</a>.
 */
public class FilterByStringRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("FilterByStringRequest")
            .namespace("com.gpudb")
            .fields()
                .name("tableName").type().stringType().noDefault()
                .name("viewName").type().stringType().noDefault()
                .name("expression").type().stringType().noDefault()
                .name("mode").type().stringType().noDefault()
                .name("columnNames").type().array().items().stringType().noDefault()
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
     * A set of string constants for the {@link FilterByStringRequest}
     * parameter {@link #getMode() mode}.
     * <p>
     * The string filtering mode to apply. See below for details.
     */
    public static final class Mode {
        /**
         * Full text search query with wildcards and boolean operators. Note
         * that for this mode, no column can be specified in {@link
         * #getColumnNames() columnNames}; all string columns of the table that
         * have text search enabled will be searched.
         */
        public static final String SEARCH = "search";

        /**
         * Exact whole-string match (accelerated).
         */
        public static final String EQUALS = "equals";

        /**
         * Partial substring match (not accelerated).  If the column is a
         * string type (non-charN) and the number of records is too large, it
         * will return 0.
         */
        public static final String CONTAINS = "contains";

        /**
         * Strings that start with the given expression (not accelerated). If
         * the column is a string type (non-charN) and the number of records is
         * too large, it will return 0.
         */
        public static final String STARTS_WITH = "starts_with";

        /**
         * Full regular expression search (not accelerated). If the column is a
         * string type (non-charN) and the number of records is too large, it
         * will return 0.
         */
        public static final String REGEX = "regex";

        private Mode() {  }
    }

    /**
     * A set of string constants for the {@link FilterByStringRequest}
     * parameter {@link #getOptions() options}.
     * <p>
     * Optional parameters.
     */
    public static final class Options {
        /**
         * If {@link Options#TRUE TRUE}, a unique temporary table name will be
         * generated in the sys_temp schema and used in place of {@link
         * #getViewName() viewName}. This is always allowed even if the caller
         * does not have permission to create tables. The generated name is
         * returned in {@link
         * com.gpudb.protocol.FilterByStringResponse.Info#QUALIFIED_VIEW_NAME
         * QUALIFIED_VIEW_NAME}.
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
         * [DEPRECATED--please specify the containing schema for the view as
         * part of {@link #getViewName() viewName} and use {@link
         * com.gpudb.GPUdb#createSchema(CreateSchemaRequest)
         * GPUdb.createSchema} to create the schema if non-existent]  Name of a
         * schema for the newly created view. If the schema is non-existent, it
         * will be automatically created.
         */
        public static final String COLLECTION_NAME = "collection_name";

        /**
         * If {@link Options#FALSE FALSE} then string filtering will ignore
         * case. Does not apply to {@link Mode#SEARCH SEARCH} mode.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}
         *     <li>{@link Options#FALSE FALSE}
         * </ul>
         * The default value is {@link Options#TRUE TRUE}.
         */
        public static final String CASE_SENSITIVE = "case_sensitive";

        private Options() {  }
    }

    private String tableName;
    private String viewName;
    private String expression;
    private String mode;
    private List<String> columnNames;
    private Map<String, String> options;

    /**
     * Constructs a FilterByStringRequest object with default parameters.
     */
    public FilterByStringRequest() {
        tableName = "";
        viewName = "";
        expression = "";
        mode = "";
        columnNames = new ArrayList<>();
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs a FilterByStringRequest object with the specified parameters.
     *
     * @param tableName  Name of the table on which the filter operation will
     *                   be performed, in [schema_name.]table_name format,
     *                   using standard <a
     *                   href="../../../../../../concepts/tables/#table-name-resolution"
     *                   target="_top">name resolution rules</a>.  Must be an
     *                   existing table or view.
     * @param viewName  If provided, then this will be the name of the view
     *                  containing the results, in [schema_name.]view_name
     *                  format, using standard <a
     *                  href="../../../../../../concepts/tables/#table-name-resolution"
     *                  target="_top">name resolution rules</a> and meeting <a
     *                  href="../../../../../../concepts/tables/#table-naming-criteria"
     *                  target="_top">table naming criteria</a>.  Must not be
     *                  an already existing table or view. The default value is
     *                  ''.
     * @param expression  The expression with which to filter the table.
     * @param mode  The string filtering mode to apply. See below for details.
     *              Supported values:
     *              <ul>
     *                  <li>{@link Mode#SEARCH SEARCH}: Full text search query
     *                      with wildcards and boolean operators. Note that for
     *                      this mode, no column can be specified in {@code
     *                      columnNames}; all string columns of the table that
     *                      have text search enabled will be searched.
     *                  <li>{@link Mode#EQUALS EQUALS}: Exact whole-string
     *                      match (accelerated).
     *                  <li>{@link Mode#CONTAINS CONTAINS}: Partial substring
     *                      match (not accelerated).  If the column is a string
     *                      type (non-charN) and the number of records is too
     *                      large, it will return 0.
     *                  <li>{@link Mode#STARTS_WITH STARTS_WITH}: Strings that
     *                      start with the given expression (not accelerated).
     *                      If the column is a string type (non-charN) and the
     *                      number of records is too large, it will return 0.
     *                  <li>{@link Mode#REGEX REGEX}: Full regular expression
     *                      search (not accelerated). If the column is a string
     *                      type (non-charN) and the number of records is too
     *                      large, it will return 0.
     *              </ul>
     * @param columnNames  List of columns on which to apply the filter.
     *                     Ignored for {@link Mode#SEARCH SEARCH} mode.
     * @param options  Optional parameters.
     *                 <ul>
     *                     <li>{@link Options#CREATE_TEMP_TABLE
     *                         CREATE_TEMP_TABLE}: If {@link Options#TRUE
     *                         TRUE}, a unique temporary table name will be
     *                         generated in the sys_temp schema and used in
     *                         place of {@code viewName}. This is always
     *                         allowed even if the caller does not have
     *                         permission to create tables. The generated name
     *                         is returned in {@link
     *                         com.gpudb.protocol.FilterByStringResponse.Info#QUALIFIED_VIEW_NAME
     *                         QUALIFIED_VIEW_NAME}.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#TRUE TRUE}
     *                             <li>{@link Options#FALSE FALSE}
     *                         </ul>
     *                         The default value is {@link Options#FALSE
     *                         FALSE}.
     *                     <li>{@link Options#COLLECTION_NAME COLLECTION_NAME}:
     *                         [DEPRECATED--please specify the containing
     *                         schema for the view as part of {@code viewName}
     *                         and use {@link
     *                         com.gpudb.GPUdb#createSchema(CreateSchemaRequest)
     *                         GPUdb.createSchema} to create the schema if
     *                         non-existent]  Name of a schema for the newly
     *                         created view. If the schema is non-existent, it
     *                         will be automatically created.
     *                     <li>{@link Options#CASE_SENSITIVE CASE_SENSITIVE}:
     *                         If {@link Options#FALSE FALSE} then string
     *                         filtering will ignore case. Does not apply to
     *                         {@link Mode#SEARCH SEARCH} mode.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#TRUE TRUE}
     *                             <li>{@link Options#FALSE FALSE}
     *                         </ul>
     *                         The default value is {@link Options#TRUE TRUE}.
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     */
    public FilterByStringRequest(String tableName, String viewName, String expression, String mode, List<String> columnNames, Map<String, String> options) {
        this.tableName = (tableName == null) ? "" : tableName;
        this.viewName = (viewName == null) ? "" : viewName;
        this.expression = (expression == null) ? "" : expression;
        this.mode = (mode == null) ? "" : mode;
        this.columnNames = (columnNames == null) ? new ArrayList<String>() : columnNames;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * Name of the table on which the filter operation will be performed, in
     * [schema_name.]table_name format, using standard <a
     * href="../../../../../../concepts/tables/#table-name-resolution"
     * target="_top">name resolution rules</a>.  Must be an existing table or
     * view.
     *
     * @return The current value of {@code tableName}.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Name of the table on which the filter operation will be performed, in
     * [schema_name.]table_name format, using standard <a
     * href="../../../../../../concepts/tables/#table-name-resolution"
     * target="_top">name resolution rules</a>.  Must be an existing table or
     * view.
     *
     * @param tableName  The new value for {@code tableName}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public FilterByStringRequest setTableName(String tableName) {
        this.tableName = (tableName == null) ? "" : tableName;
        return this;
    }

    /**
     * If provided, then this will be the name of the view containing the
     * results, in [schema_name.]view_name format, using standard <a
     * href="../../../../../../concepts/tables/#table-name-resolution"
     * target="_top">name resolution rules</a> and meeting <a
     * href="../../../../../../concepts/tables/#table-naming-criteria"
     * target="_top">table naming criteria</a>.  Must not be an already
     * existing table or view. The default value is ''.
     *
     * @return The current value of {@code viewName}.
     */
    public String getViewName() {
        return viewName;
    }

    /**
     * If provided, then this will be the name of the view containing the
     * results, in [schema_name.]view_name format, using standard <a
     * href="../../../../../../concepts/tables/#table-name-resolution"
     * target="_top">name resolution rules</a> and meeting <a
     * href="../../../../../../concepts/tables/#table-naming-criteria"
     * target="_top">table naming criteria</a>.  Must not be an already
     * existing table or view. The default value is ''.
     *
     * @param viewName  The new value for {@code viewName}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public FilterByStringRequest setViewName(String viewName) {
        this.viewName = (viewName == null) ? "" : viewName;
        return this;
    }

    /**
     * The expression with which to filter the table.
     *
     * @return The current value of {@code expression}.
     */
    public String getExpression() {
        return expression;
    }

    /**
     * The expression with which to filter the table.
     *
     * @param expression  The new value for {@code expression}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public FilterByStringRequest setExpression(String expression) {
        this.expression = (expression == null) ? "" : expression;
        return this;
    }

    /**
     * The string filtering mode to apply. See below for details.
     * Supported values:
     * <ul>
     *     <li>{@link Mode#SEARCH SEARCH}: Full text search query with
     *         wildcards and boolean operators. Note that for this mode, no
     *         column can be specified in {@link #getColumnNames()
     *         columnNames}; all string columns of the table that have text
     *         search enabled will be searched.
     *     <li>{@link Mode#EQUALS EQUALS}: Exact whole-string match
     *         (accelerated).
     *     <li>{@link Mode#CONTAINS CONTAINS}: Partial substring match (not
     *         accelerated).  If the column is a string type (non-charN) and
     *         the number of records is too large, it will return 0.
     *     <li>{@link Mode#STARTS_WITH STARTS_WITH}: Strings that start with
     *         the given expression (not accelerated). If the column is a
     *         string type (non-charN) and the number of records is too large,
     *         it will return 0.
     *     <li>{@link Mode#REGEX REGEX}: Full regular expression search (not
     *         accelerated). If the column is a string type (non-charN) and the
     *         number of records is too large, it will return 0.
     * </ul>
     *
     * @return The current value of {@code mode}.
     */
    public String getMode() {
        return mode;
    }

    /**
     * The string filtering mode to apply. See below for details.
     * Supported values:
     * <ul>
     *     <li>{@link Mode#SEARCH SEARCH}: Full text search query with
     *         wildcards and boolean operators. Note that for this mode, no
     *         column can be specified in {@link #getColumnNames()
     *         columnNames}; all string columns of the table that have text
     *         search enabled will be searched.
     *     <li>{@link Mode#EQUALS EQUALS}: Exact whole-string match
     *         (accelerated).
     *     <li>{@link Mode#CONTAINS CONTAINS}: Partial substring match (not
     *         accelerated).  If the column is a string type (non-charN) and
     *         the number of records is too large, it will return 0.
     *     <li>{@link Mode#STARTS_WITH STARTS_WITH}: Strings that start with
     *         the given expression (not accelerated). If the column is a
     *         string type (non-charN) and the number of records is too large,
     *         it will return 0.
     *     <li>{@link Mode#REGEX REGEX}: Full regular expression search (not
     *         accelerated). If the column is a string type (non-charN) and the
     *         number of records is too large, it will return 0.
     * </ul>
     *
     * @param mode  The new value for {@code mode}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public FilterByStringRequest setMode(String mode) {
        this.mode = (mode == null) ? "" : mode;
        return this;
    }

    /**
     * List of columns on which to apply the filter. Ignored for {@link
     * Mode#SEARCH SEARCH} mode.
     *
     * @return The current value of {@code columnNames}.
     */
    public List<String> getColumnNames() {
        return columnNames;
    }

    /**
     * List of columns on which to apply the filter. Ignored for {@link
     * Mode#SEARCH SEARCH} mode.
     *
     * @param columnNames  The new value for {@code columnNames}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public FilterByStringRequest setColumnNames(List<String> columnNames) {
        this.columnNames = (columnNames == null) ? new ArrayList<String>() : columnNames;
        return this;
    }

    /**
     * Optional parameters.
     * <ul>
     *     <li>{@link Options#CREATE_TEMP_TABLE CREATE_TEMP_TABLE}: If {@link
     *         Options#TRUE TRUE}, a unique temporary table name will be
     *         generated in the sys_temp schema and used in place of {@link
     *         #getViewName() viewName}. This is always allowed even if the
     *         caller does not have permission to create tables. The generated
     *         name is returned in {@link
     *         com.gpudb.protocol.FilterByStringResponse.Info#QUALIFIED_VIEW_NAME
     *         QUALIFIED_VIEW_NAME}.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     *     <li>{@link Options#COLLECTION_NAME COLLECTION_NAME}:
     *         [DEPRECATED--please specify the containing schema for the view
     *         as part of {@link #getViewName() viewName} and use {@link
     *         com.gpudb.GPUdb#createSchema(CreateSchemaRequest)
     *         GPUdb.createSchema} to create the schema if non-existent]  Name
     *         of a schema for the newly created view. If the schema is
     *         non-existent, it will be automatically created.
     *     <li>{@link Options#CASE_SENSITIVE CASE_SENSITIVE}: If {@link
     *         Options#FALSE FALSE} then string filtering will ignore case.
     *         Does not apply to {@link Mode#SEARCH SEARCH} mode.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#TRUE TRUE}.
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
     *         #getViewName() viewName}. This is always allowed even if the
     *         caller does not have permission to create tables. The generated
     *         name is returned in {@link
     *         com.gpudb.protocol.FilterByStringResponse.Info#QUALIFIED_VIEW_NAME
     *         QUALIFIED_VIEW_NAME}.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     *     <li>{@link Options#COLLECTION_NAME COLLECTION_NAME}:
     *         [DEPRECATED--please specify the containing schema for the view
     *         as part of {@link #getViewName() viewName} and use {@link
     *         com.gpudb.GPUdb#createSchema(CreateSchemaRequest)
     *         GPUdb.createSchema} to create the schema if non-existent]  Name
     *         of a schema for the newly created view. If the schema is
     *         non-existent, it will be automatically created.
     *     <li>{@link Options#CASE_SENSITIVE CASE_SENSITIVE}: If {@link
     *         Options#FALSE FALSE} then string filtering will ignore case.
     *         Does not apply to {@link Mode#SEARCH SEARCH} mode.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#TRUE TRUE}.
     * </ul>
     * The default value is an empty {@link Map}.
     *
     * @param options  The new value for {@code options}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public FilterByStringRequest setOptions(Map<String, String> options) {
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
                return this.tableName;

            case 1:
                return this.viewName;

            case 2:
                return this.expression;

            case 3:
                return this.mode;

            case 4:
                return this.columnNames;

            case 5:
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
                this.tableName = (String)value;
                break;

            case 1:
                this.viewName = (String)value;
                break;

            case 2:
                this.expression = (String)value;
                break;

            case 3:
                this.mode = (String)value;
                break;

            case 4:
                this.columnNames = (List<String>)value;
                break;

            case 5:
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

        FilterByStringRequest that = (FilterByStringRequest)obj;

        return ( this.tableName.equals( that.tableName )
                 && this.viewName.equals( that.viewName )
                 && this.expression.equals( that.expression )
                 && this.mode.equals( that.mode )
                 && this.columnNames.equals( that.columnNames )
                 && this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "tableName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.tableName ) );
        builder.append( ", " );
        builder.append( gd.toString( "viewName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.viewName ) );
        builder.append( ", " );
        builder.append( gd.toString( "expression" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.expression ) );
        builder.append( ", " );
        builder.append( gd.toString( "mode" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.mode ) );
        builder.append( ", " );
        builder.append( gd.toString( "columnNames" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.columnNames ) );
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
        hashCode = (31 * hashCode) + this.tableName.hashCode();
        hashCode = (31 * hashCode) + this.viewName.hashCode();
        hashCode = (31 * hashCode) + this.expression.hashCode();
        hashCode = (31 * hashCode) + this.mode.hashCode();
        hashCode = (31 * hashCode) + this.columnNames.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
