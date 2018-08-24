/*
 *  This file was autogenerated by the GPUdb schema processor.
 *
 *  DO NOT EDIT DIRECTLY.
 */
package com.gpudb.protocol;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;


/**
 * A set of parameters for {@link
 * com.gpudb.GPUdb#filterByList(FilterByListRequest)}.
 * <p>
 * Calculates which records from a table have values in the given list for the
 * corresponding column. The operation is synchronous, meaning that a response
 * will not be returned until all the objects are fully available. The response
 * payload provides the count of the resulting set. A new resultant set (view)
 * which satisfies the input filter specification is also created if a {@code
 * viewName} is passed in as part of the request.
 * <p>
 * For example, if a type definition has the columns 'x' and 'y', then a filter
 * by list query with the column map {"x":["10.1", "2.3"], "y":["0.0", "-31.5",
 * "42.0"]} will return the count of all data points whose x and y values match
 * both in the respective x- and y-lists, e.g., "x = 10.1 and y = 0.0", "x =
 * 2.3 and y = -31.5", etc. However, a record with "x = 10.1 and y = -31.5" or
 * "x = 2.3 and y = 0.0" would not be returned because the values in the given
 * lists do not correspond.
 */
public class FilterByListRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("FilterByListRequest")
            .namespace("com.gpudb")
            .fields()
                .name("tableName").type().stringType().noDefault()
                .name("viewName").type().stringType().noDefault()
                .name("columnValuesMap").type().map().values().array().items().stringType().noDefault()
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
     * Optional parameters.
     * <ul>
     *         <li> {@link
     * com.gpudb.protocol.FilterByListRequest.Options#COLLECTION_NAME
     * COLLECTION_NAME}: Name of a collection which is to contain the newly
     * created view. If the collection provided is non-existent, the collection
     * will be automatically created. If empty, then the newly created view
     * will be top-level.
     *         <li> {@link
     * com.gpudb.protocol.FilterByListRequest.Options#FILTER_MODE FILTER_MODE}:
     * String indicating the filter mode, either 'in_list' or 'not_in_list'.
     * Supported values:
     * <ul>
     *         <li> {@link
     * com.gpudb.protocol.FilterByListRequest.Options#IN_LIST IN_LIST}: The
     * filter will match all items that are in the provided list(s).
     *         <li> {@link
     * com.gpudb.protocol.FilterByListRequest.Options#NOT_IN_LIST NOT_IN_LIST}:
     * The filter will match all items that are not in the provided list(s).
     * </ul>
     * The default value is {@link
     * com.gpudb.protocol.FilterByListRequest.Options#IN_LIST IN_LIST}.
     * </ul>
     * A set of string constants for the parameter {@code options}.
     */
    public static final class Options {

        /**
         * Name of a collection which is to contain the newly created view. If
         * the collection provided is non-existent, the collection will be
         * automatically created. If empty, then the newly created view will be
         * top-level.
         */
        public static final String COLLECTION_NAME = "collection_name";

        /**
         * String indicating the filter mode, either 'in_list' or
         * 'not_in_list'.
         * Supported values:
         * <ul>
         *         <li> {@link
         * com.gpudb.protocol.FilterByListRequest.Options#IN_LIST IN_LIST}: The
         * filter will match all items that are in the provided list(s).
         *         <li> {@link
         * com.gpudb.protocol.FilterByListRequest.Options#NOT_IN_LIST
         * NOT_IN_LIST}: The filter will match all items that are not in the
         * provided list(s).
         * </ul>
         * The default value is {@link
         * com.gpudb.protocol.FilterByListRequest.Options#IN_LIST IN_LIST}.
         */
        public static final String FILTER_MODE = "filter_mode";

        /**
         * The filter will match all items that are in the provided list(s).
         */
        public static final String IN_LIST = "in_list";

        /**
         * The filter will match all items that are not in the provided
         * list(s).
         */
        public static final String NOT_IN_LIST = "not_in_list";

        private Options() {  }
    }

    private String tableName;
    private String viewName;
    private Map<String, List<String>> columnValuesMap;
    private Map<String, String> options;


    /**
     * Constructs a FilterByListRequest object with default parameters.
     */
    public FilterByListRequest() {
        tableName = "";
        viewName = "";
        columnValuesMap = new LinkedHashMap<>();
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs a FilterByListRequest object with the specified parameters.
     * 
     * @param tableName  Name of the table to filter.  This may be the ID of a
     *                   collection, table or a result set (for chaining
     *                   queries). If filtering a collection, all child tables
     *                   where the filter expression is valid will be filtered;
     *                   the filtered result tables will then be placed in a
     *                   collection specified by {@code viewName}.
     * @param viewName  If provided, then this will be the name of the view
     *                  containing the results. Has the same naming
     *                  restrictions as <a
     *                  href="../../../../../concepts/tables.html"
     *                  target="_top">tables</a>.
     * @param columnValuesMap  List of values for the corresponding column in
     *                         the table
     * @param options  Optional parameters.
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.FilterByListRequest.Options#COLLECTION_NAME
     *                 COLLECTION_NAME}: Name of a collection which is to
     *                 contain the newly created view. If the collection
     *                 provided is non-existent, the collection will be
     *                 automatically created. If empty, then the newly created
     *                 view will be top-level.
     *                         <li> {@link
     *                 com.gpudb.protocol.FilterByListRequest.Options#FILTER_MODE
     *                 FILTER_MODE}: String indicating the filter mode, either
     *                 'in_list' or 'not_in_list'.
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.FilterByListRequest.Options#IN_LIST
     *                 IN_LIST}: The filter will match all items that are in
     *                 the provided list(s).
     *                         <li> {@link
     *                 com.gpudb.protocol.FilterByListRequest.Options#NOT_IN_LIST
     *                 NOT_IN_LIST}: The filter will match all items that are
     *                 not in the provided list(s).
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.FilterByListRequest.Options#IN_LIST
     *                 IN_LIST}.
     *                 </ul>
     * 
     */
    public FilterByListRequest(String tableName, String viewName, Map<String, List<String>> columnValuesMap, Map<String, String> options) {
        this.tableName = (tableName == null) ? "" : tableName;
        this.viewName = (viewName == null) ? "" : viewName;
        this.columnValuesMap = (columnValuesMap == null) ? new LinkedHashMap<String, List<String>>() : columnValuesMap;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * 
     * @return Name of the table to filter.  This may be the ID of a
     *         collection, table or a result set (for chaining queries). If
     *         filtering a collection, all child tables where the filter
     *         expression is valid will be filtered; the filtered result tables
     *         will then be placed in a collection specified by {@code
     *         viewName}.
     * 
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * 
     * @param tableName  Name of the table to filter.  This may be the ID of a
     *                   collection, table or a result set (for chaining
     *                   queries). If filtering a collection, all child tables
     *                   where the filter expression is valid will be filtered;
     *                   the filtered result tables will then be placed in a
     *                   collection specified by {@code viewName}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public FilterByListRequest setTableName(String tableName) {
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
    public FilterByListRequest setViewName(String viewName) {
        this.viewName = (viewName == null) ? "" : viewName;
        return this;
    }

    /**
     * 
     * @return List of values for the corresponding column in the table
     * 
     */
    public Map<String, List<String>> getColumnValuesMap() {
        return columnValuesMap;
    }

    /**
     * 
     * @param columnValuesMap  List of values for the corresponding column in
     *                         the table
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public FilterByListRequest setColumnValuesMap(Map<String, List<String>> columnValuesMap) {
        this.columnValuesMap = (columnValuesMap == null) ? new LinkedHashMap<String, List<String>>() : columnValuesMap;
        return this;
    }

    /**
     * 
     * @return Optional parameters.
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.FilterByListRequest.Options#COLLECTION_NAME
     *         COLLECTION_NAME}: Name of a collection which is to contain the
     *         newly created view. If the collection provided is non-existent,
     *         the collection will be automatically created. If empty, then the
     *         newly created view will be top-level.
     *                 <li> {@link
     *         com.gpudb.protocol.FilterByListRequest.Options#FILTER_MODE
     *         FILTER_MODE}: String indicating the filter mode, either
     *         'in_list' or 'not_in_list'.
     *         Supported values:
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.FilterByListRequest.Options#IN_LIST IN_LIST}:
     *         The filter will match all items that are in the provided
     *         list(s).
     *                 <li> {@link
     *         com.gpudb.protocol.FilterByListRequest.Options#NOT_IN_LIST
     *         NOT_IN_LIST}: The filter will match all items that are not in
     *         the provided list(s).
     *         </ul>
     *         The default value is {@link
     *         com.gpudb.protocol.FilterByListRequest.Options#IN_LIST IN_LIST}.
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
     *                         <li> {@link
     *                 com.gpudb.protocol.FilterByListRequest.Options#COLLECTION_NAME
     *                 COLLECTION_NAME}: Name of a collection which is to
     *                 contain the newly created view. If the collection
     *                 provided is non-existent, the collection will be
     *                 automatically created. If empty, then the newly created
     *                 view will be top-level.
     *                         <li> {@link
     *                 com.gpudb.protocol.FilterByListRequest.Options#FILTER_MODE
     *                 FILTER_MODE}: String indicating the filter mode, either
     *                 'in_list' or 'not_in_list'.
     *                 Supported values:
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.FilterByListRequest.Options#IN_LIST
     *                 IN_LIST}: The filter will match all items that are in
     *                 the provided list(s).
     *                         <li> {@link
     *                 com.gpudb.protocol.FilterByListRequest.Options#NOT_IN_LIST
     *                 NOT_IN_LIST}: The filter will match all items that are
     *                 not in the provided list(s).
     *                 </ul>
     *                 The default value is {@link
     *                 com.gpudb.protocol.FilterByListRequest.Options#IN_LIST
     *                 IN_LIST}.
     *                 </ul>
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public FilterByListRequest setOptions(Map<String, String> options) {
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
                return this.columnValuesMap;

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
                this.tableName = (String)value;
                break;

            case 1:
                this.viewName = (String)value;
                break;

            case 2:
                this.columnValuesMap = (Map<String, List<String>>)value;
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

        FilterByListRequest that = (FilterByListRequest)obj;

        return ( this.tableName.equals( that.tableName )
                 && this.viewName.equals( that.viewName )
                 && this.columnValuesMap.equals( that.columnValuesMap )
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
        builder.append( gd.toString( "columnValuesMap" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.columnValuesMap ) );
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
        hashCode = (31 * hashCode) + this.columnValuesMap.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }

}
