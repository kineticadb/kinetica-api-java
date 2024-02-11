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
 * com.gpudb.GPUdb#filterByArea(FilterByAreaRequest) GPUdb.filterByArea}.
 * <p>
 * Calculates which objects from a table are within a named area of interest
 * (NAI/polygon). The operation is synchronous, meaning that a response will
 * not be returned until all the matching objects are fully available. The
 * response payload provides the count of the resulting set. A new resultant
 * set (view) which satisfies the input NAI restriction specification is
 * created with the name {@link #getViewName() viewName} passed in as part of
 * the input.
 */
public class FilterByAreaRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("FilterByAreaRequest")
            .namespace("com.gpudb")
            .fields()
                .name("tableName").type().stringType().noDefault()
                .name("viewName").type().stringType().noDefault()
                .name("xColumnName").type().stringType().noDefault()
                .name("xVector").type().array().items().doubleType().noDefault()
                .name("yColumnName").type().stringType().noDefault()
                .name("yVector").type().array().items().doubleType().noDefault()
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
     * A set of string constants for the {@link FilterByAreaRequest} parameter
     * {@link #getOptions() options}.
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
         * com.gpudb.protocol.FilterByAreaResponse.Info#QUALIFIED_VIEW_NAME
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
         * schema for the newly created view. If the schema provided is
         * non-existent, it will be automatically created.
         */
        public static final String COLLECTION_NAME = "collection_name";

        private Options() {  }
    }

    private String tableName;
    private String viewName;
    private String xColumnName;
    private List<Double> xVector;
    private String yColumnName;
    private List<Double> yVector;
    private Map<String, String> options;

    /**
     * Constructs a FilterByAreaRequest object with default parameters.
     */
    public FilterByAreaRequest() {
        tableName = "";
        viewName = "";
        xColumnName = "";
        xVector = new ArrayList<>();
        yColumnName = "";
        yVector = new ArrayList<>();
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs a FilterByAreaRequest object with the specified parameters.
     *
     * @param tableName  Name of the table to filter, in
     *                   [schema_name.]table_name format, using standard <a
     *                   href="../../../../../../concepts/tables/#table-name-resolution"
     *                   target="_top">name resolution rules</a>.  This may be
     *                   the name of a table or a view (when chaining queries).
     * @param viewName  If provided, then this will be the name of the view
     *                  containing the results, in [schema_name.]view_name
     *                  format, using standard <a
     *                  href="../../../../../../concepts/tables/#table-name-resolution"
     *                  target="_top">name resolution rules</a> and meeting <a
     *                  href="../../../../../../concepts/tables/#table-naming-criteria"
     *                  target="_top">table naming criteria</a>.  Must not be
     *                  an already existing table or view. The default value is
     *                  ''.
     * @param xColumnName  Name of the column containing the x values to be
     *                     filtered.
     * @param xVector  List of x coordinates of the vertices of the polygon
     *                 representing the area to be filtered.
     * @param yColumnName  Name of the column containing the y values to be
     *                     filtered.
     * @param yVector  List of y coordinates of the vertices of the polygon
     *                 representing the area to be filtered.
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
     *                         com.gpudb.protocol.FilterByAreaResponse.Info#QUALIFIED_VIEW_NAME
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
     *                         created view. If the schema provided is
     *                         non-existent, it will be automatically created.
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     */
    public FilterByAreaRequest(String tableName, String viewName, String xColumnName, List<Double> xVector, String yColumnName, List<Double> yVector, Map<String, String> options) {
        this.tableName = (tableName == null) ? "" : tableName;
        this.viewName = (viewName == null) ? "" : viewName;
        this.xColumnName = (xColumnName == null) ? "" : xColumnName;
        this.xVector = (xVector == null) ? new ArrayList<Double>() : xVector;
        this.yColumnName = (yColumnName == null) ? "" : yColumnName;
        this.yVector = (yVector == null) ? new ArrayList<Double>() : yVector;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * Name of the table to filter, in [schema_name.]table_name format, using
     * standard <a
     * href="../../../../../../concepts/tables/#table-name-resolution"
     * target="_top">name resolution rules</a>.  This may be the name of a
     * table or a view (when chaining queries).
     *
     * @return The current value of {@code tableName}.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Name of the table to filter, in [schema_name.]table_name format, using
     * standard <a
     * href="../../../../../../concepts/tables/#table-name-resolution"
     * target="_top">name resolution rules</a>.  This may be the name of a
     * table or a view (when chaining queries).
     *
     * @param tableName  The new value for {@code tableName}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public FilterByAreaRequest setTableName(String tableName) {
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
    public FilterByAreaRequest setViewName(String viewName) {
        this.viewName = (viewName == null) ? "" : viewName;
        return this;
    }

    /**
     * Name of the column containing the x values to be filtered.
     *
     * @return The current value of {@code xColumnName}.
     */
    public String getXColumnName() {
        return xColumnName;
    }

    /**
     * Name of the column containing the x values to be filtered.
     *
     * @param xColumnName  The new value for {@code xColumnName}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public FilterByAreaRequest setXColumnName(String xColumnName) {
        this.xColumnName = (xColumnName == null) ? "" : xColumnName;
        return this;
    }

    /**
     * List of x coordinates of the vertices of the polygon representing the
     * area to be filtered.
     *
     * @return The current value of {@code xVector}.
     */
    public List<Double> getXVector() {
        return xVector;
    }

    /**
     * List of x coordinates of the vertices of the polygon representing the
     * area to be filtered.
     *
     * @param xVector  The new value for {@code xVector}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public FilterByAreaRequest setXVector(List<Double> xVector) {
        this.xVector = (xVector == null) ? new ArrayList<Double>() : xVector;
        return this;
    }

    /**
     * Name of the column containing the y values to be filtered.
     *
     * @return The current value of {@code yColumnName}.
     */
    public String getYColumnName() {
        return yColumnName;
    }

    /**
     * Name of the column containing the y values to be filtered.
     *
     * @param yColumnName  The new value for {@code yColumnName}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public FilterByAreaRequest setYColumnName(String yColumnName) {
        this.yColumnName = (yColumnName == null) ? "" : yColumnName;
        return this;
    }

    /**
     * List of y coordinates of the vertices of the polygon representing the
     * area to be filtered.
     *
     * @return The current value of {@code yVector}.
     */
    public List<Double> getYVector() {
        return yVector;
    }

    /**
     * List of y coordinates of the vertices of the polygon representing the
     * area to be filtered.
     *
     * @param yVector  The new value for {@code yVector}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public FilterByAreaRequest setYVector(List<Double> yVector) {
        this.yVector = (yVector == null) ? new ArrayList<Double>() : yVector;
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
     *         com.gpudb.protocol.FilterByAreaResponse.Info#QUALIFIED_VIEW_NAME
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
     *         of a schema for the newly created view. If the schema provided
     *         is non-existent, it will be automatically created.
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
     *         com.gpudb.protocol.FilterByAreaResponse.Info#QUALIFIED_VIEW_NAME
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
     *         of a schema for the newly created view. If the schema provided
     *         is non-existent, it will be automatically created.
     * </ul>
     * The default value is an empty {@link Map}.
     *
     * @param options  The new value for {@code options}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public FilterByAreaRequest setOptions(Map<String, String> options) {
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
                return this.xColumnName;

            case 3:
                return this.xVector;

            case 4:
                return this.yColumnName;

            case 5:
                return this.yVector;

            case 6:
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
                this.xColumnName = (String)value;
                break;

            case 3:
                this.xVector = (List<Double>)value;
                break;

            case 4:
                this.yColumnName = (String)value;
                break;

            case 5:
                this.yVector = (List<Double>)value;
                break;

            case 6:
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

        FilterByAreaRequest that = (FilterByAreaRequest)obj;

        return ( this.tableName.equals( that.tableName )
                 && this.viewName.equals( that.viewName )
                 && this.xColumnName.equals( that.xColumnName )
                 && this.xVector.equals( that.xVector )
                 && this.yColumnName.equals( that.yColumnName )
                 && this.yVector.equals( that.yVector )
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
        builder.append( gd.toString( "xColumnName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.xColumnName ) );
        builder.append( ", " );
        builder.append( gd.toString( "xVector" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.xVector ) );
        builder.append( ", " );
        builder.append( gd.toString( "yColumnName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.yColumnName ) );
        builder.append( ", " );
        builder.append( gd.toString( "yVector" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.yVector ) );
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
        hashCode = (31 * hashCode) + this.xColumnName.hashCode();
        hashCode = (31 * hashCode) + this.xVector.hashCode();
        hashCode = (31 * hashCode) + this.yColumnName.hashCode();
        hashCode = (31 * hashCode) + this.yVector.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
