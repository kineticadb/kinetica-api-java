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
 * com.gpudb.GPUdb#filterByString(FilterByStringRequest)}.
 * <p>
 * Calculates which objects from a table, collection, or view match a string
 * expression for the given string columns. The options 'case_sensitive' can be
 * used to modify the behavior for all modes except 'search'. For 'search' mode
 * details and limitations, see <a
 * href="../../../../../concepts/full_text_search.html" target="_top">Full Text
 * Search</a>.
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
     * @return  the schema for the class.
     * 
     */
    public static Schema getClassSchema() {
        return schema$;
    }


    /**
     * The string filtering mode to apply. See below for details. Values:
     * search, equals, contains, starts_with, regex.

     * A set of string constants for the parameter {@code mode}.
     */
    public static final class Mode {

        /**
         * Full text search query with wildcards and boolean operators. Note
         * that for this mode, no column can be specified in {@code
         * columnNames}; all string columns of the table that have text search
         * enabled will be searched.
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
     * Optional parameters.
     * <ul>
     *         <li> case_sensitive: If 'false' then string filtering will
     * ignore case. Does not apply to 'search' mode. Values: true, false.
     * <p>
     * </ul>
     * A set of string constants for the parameter {@code options}.
     */
    public static final class Options {

        /**
         * If 'false' then string filtering will ignore case. Does not apply to
         * 'search' mode. Values: true, false.
         */
        public static final String CASE_SENSITIVE = "case_sensitive";
        public static final String TRUE = "true";
        public static final String FALSE = "false";

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
     *                   be performed.  Must be an existing table, collection
     *                   or view.
     * @param viewName  If provided, then this will be the name of the view
     *                  containing the results. Has the same naming
     *                  restrictions as <a
     *                  href="../../../../../concepts/tables.html"
     *                  target="_top">tables</a>.
     * @param expression  The expression with which to filter the table.
     * @param mode  The string filtering mode to apply. See below for details.
     *              Values: search, equals, contains, starts_with, regex.
     * @param columnNames  List of columns on which to apply the filter.
     *                     Ignored for 'search' mode.
     * @param options  Optional parameters.
     *                 <ul>
     *                         <li> case_sensitive: If 'false' then string
     *                 filtering will ignore case. Does not apply to 'search'
     *                 mode. Values: true, false.
     *                 </ul>
     * 
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
     * 
     * @return Name of the table on which the filter operation will be
     *         performed.  Must be an existing table, collection or view.
     * 
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * 
     * @param tableName  Name of the table on which the filter operation will
     *                   be performed.  Must be an existing table, collection
     *                   or view.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public FilterByStringRequest setTableName(String tableName) {
        this.tableName = (tableName == null) ? "" : tableName;
        return this;
    }

    /**
     * 
     * @return If provided, then this will be the name of the view containing
     *         the results. Has the same naming restrictions as <a
     *         href="../../../../../concepts/tables.html"
     *         target="_top">tables</a>.
     * 
     */
    public String getViewName() {
        return viewName;
    }

    /**
     * 
     * @param viewName  If provided, then this will be the name of the view
     *                  containing the results. Has the same naming
     *                  restrictions as <a
     *                  href="../../../../../concepts/tables.html"
     *                  target="_top">tables</a>.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public FilterByStringRequest setViewName(String viewName) {
        this.viewName = (viewName == null) ? "" : viewName;
        return this;
    }

    /**
     * 
     * @return The expression with which to filter the table.
     * 
     */
    public String getExpression() {
        return expression;
    }

    /**
     * 
     * @param expression  The expression with which to filter the table.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public FilterByStringRequest setExpression(String expression) {
        this.expression = (expression == null) ? "" : expression;
        return this;
    }

    /**
     * 
     * @return The string filtering mode to apply. See below for details.
     *         Values: search, equals, contains, starts_with, regex.
     * 
     */
    public String getMode() {
        return mode;
    }

    /**
     * 
     * @param mode  The string filtering mode to apply. See below for details.
     *              Values: search, equals, contains, starts_with, regex.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public FilterByStringRequest setMode(String mode) {
        this.mode = (mode == null) ? "" : mode;
        return this;
    }

    /**
     * 
     * @return List of columns on which to apply the filter. Ignored for
     *         'search' mode.
     * 
     */
    public List<String> getColumnNames() {
        return columnNames;
    }

    /**
     * 
     * @param columnNames  List of columns on which to apply the filter.
     *                     Ignored for 'search' mode.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public FilterByStringRequest setColumnNames(List<String> columnNames) {
        this.columnNames = (columnNames == null) ? new ArrayList<String>() : columnNames;
        return this;
    }

    /**
     * 
     * @return Optional parameters.
     *         <ul>
     *                 <li> case_sensitive: If 'false' then string filtering
     *         will ignore case. Does not apply to 'search' mode. Values: true,
     *         false.
     *         </ul>
     * 
     */
    public Map<String, String> getOptions() {
        return options;
    }

    /**
     * 
     * @param options  Optional parameters.
     *                 <ul>
     *                         <li> case_sensitive: If 'false' then string
     *                 filtering will ignore case. Does not apply to 'search'
     *                 mode. Values: true, false.
     *                 </ul>
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public FilterByStringRequest setOptions(Map<String, String> options) {
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
     * 
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
