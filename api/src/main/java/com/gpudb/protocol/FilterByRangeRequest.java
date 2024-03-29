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
 * com.gpudb.GPUdb#filterByRange(FilterByRangeRequest) GPUdb.filterByRange}.
 * <p>
 * Calculates which objects from a table have a column that is within the given
 * bounds. An object from the table identified by {@link #getTableName()
 * tableName} is added to the view {@link #getViewName() viewName} if its
 * column is within [{@link #getLowerBound() lowerBound}, {@link
 * #getUpperBound() upperBound}] (inclusive). The operation is synchronous. The
 * response provides a count of the number of objects which passed the bound
 * filter.  Although this functionality can also be accomplished with the
 * standard filter function, it is more efficient.
 * <p>
 * For track objects, the count reflects how many points fall within the given
 * bounds (which may not include all the track points of any given track).
 */
public class FilterByRangeRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("FilterByRangeRequest")
            .namespace("com.gpudb")
            .fields()
                .name("tableName").type().stringType().noDefault()
                .name("viewName").type().stringType().noDefault()
                .name("columnName").type().stringType().noDefault()
                .name("lowerBound").type().doubleType().noDefault()
                .name("upperBound").type().doubleType().noDefault()
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
     * A set of string constants for the {@link FilterByRangeRequest} parameter
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
         * com.gpudb.protocol.FilterByRangeResponse.Info#QUALIFIED_VIEW_NAME
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

        private Options() {  }
    }

    private String tableName;
    private String viewName;
    private String columnName;
    private double lowerBound;
    private double upperBound;
    private Map<String, String> options;

    /**
     * Constructs a FilterByRangeRequest object with default parameters.
     */
    public FilterByRangeRequest() {
        tableName = "";
        viewName = "";
        columnName = "";
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs a FilterByRangeRequest object with the specified parameters.
     *
     * @param tableName  Name of the table on which the filter by range
     *                   operation will be performed, in
     *                   [schema_name.]table_name format, using standard <a
     *                   href="../../../../../../concepts/tables/#table-name-resolution"
     *                   target="_top">name resolution rules</a>.  Must be an
     *                   existing table.
     * @param viewName  If provided, then this will be the name of the view
     *                  containing the results, in [schema_name.]view_name
     *                  format, using standard <a
     *                  href="../../../../../../concepts/tables/#table-name-resolution"
     *                  target="_top">name resolution rules</a> and meeting <a
     *                  href="../../../../../../concepts/tables/#table-naming-criteria"
     *                  target="_top">table naming criteria</a>.  Must not be
     *                  an already existing table or view. The default value is
     *                  ''.
     * @param columnName  Name of a column on which the operation would be
     *                    applied.
     * @param lowerBound  Value of the lower bound (inclusive).
     * @param upperBound  Value of the upper bound (inclusive).
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
     *                         com.gpudb.protocol.FilterByRangeResponse.Info#QUALIFIED_VIEW_NAME
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
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     */
    public FilterByRangeRequest(String tableName, String viewName, String columnName, double lowerBound, double upperBound, Map<String, String> options) {
        this.tableName = (tableName == null) ? "" : tableName;
        this.viewName = (viewName == null) ? "" : viewName;
        this.columnName = (columnName == null) ? "" : columnName;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * Name of the table on which the filter by range operation will be
     * performed, in [schema_name.]table_name format, using standard <a
     * href="../../../../../../concepts/tables/#table-name-resolution"
     * target="_top">name resolution rules</a>.  Must be an existing table.
     *
     * @return The current value of {@code tableName}.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Name of the table on which the filter by range operation will be
     * performed, in [schema_name.]table_name format, using standard <a
     * href="../../../../../../concepts/tables/#table-name-resolution"
     * target="_top">name resolution rules</a>.  Must be an existing table.
     *
     * @param tableName  The new value for {@code tableName}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public FilterByRangeRequest setTableName(String tableName) {
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
    public FilterByRangeRequest setViewName(String viewName) {
        this.viewName = (viewName == null) ? "" : viewName;
        return this;
    }

    /**
     * Name of a column on which the operation would be applied.
     *
     * @return The current value of {@code columnName}.
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * Name of a column on which the operation would be applied.
     *
     * @param columnName  The new value for {@code columnName}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public FilterByRangeRequest setColumnName(String columnName) {
        this.columnName = (columnName == null) ? "" : columnName;
        return this;
    }

    /**
     * Value of the lower bound (inclusive).
     *
     * @return The current value of {@code lowerBound}.
     */
    public double getLowerBound() {
        return lowerBound;
    }

    /**
     * Value of the lower bound (inclusive).
     *
     * @param lowerBound  The new value for {@code lowerBound}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public FilterByRangeRequest setLowerBound(double lowerBound) {
        this.lowerBound = lowerBound;
        return this;
    }

    /**
     * Value of the upper bound (inclusive).
     *
     * @return The current value of {@code upperBound}.
     */
    public double getUpperBound() {
        return upperBound;
    }

    /**
     * Value of the upper bound (inclusive).
     *
     * @param upperBound  The new value for {@code upperBound}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public FilterByRangeRequest setUpperBound(double upperBound) {
        this.upperBound = upperBound;
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
     *         com.gpudb.protocol.FilterByRangeResponse.Info#QUALIFIED_VIEW_NAME
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
     *         com.gpudb.protocol.FilterByRangeResponse.Info#QUALIFIED_VIEW_NAME
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
     * </ul>
     * The default value is an empty {@link Map}.
     *
     * @param options  The new value for {@code options}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public FilterByRangeRequest setOptions(Map<String, String> options) {
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
                return this.columnName;

            case 3:
                return this.lowerBound;

            case 4:
                return this.upperBound;

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
                this.columnName = (String)value;
                break;

            case 3:
                this.lowerBound = (Double)value;
                break;

            case 4:
                this.upperBound = (Double)value;
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

        FilterByRangeRequest that = (FilterByRangeRequest)obj;

        return ( this.tableName.equals( that.tableName )
                 && this.viewName.equals( that.viewName )
                 && this.columnName.equals( that.columnName )
                 && ( (Double)this.lowerBound ).equals( (Double)that.lowerBound )
                 && ( (Double)this.upperBound ).equals( (Double)that.upperBound )
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
        builder.append( gd.toString( "columnName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.columnName ) );
        builder.append( ", " );
        builder.append( gd.toString( "lowerBound" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.lowerBound ) );
        builder.append( ", " );
        builder.append( gd.toString( "upperBound" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.upperBound ) );
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
        hashCode = (31 * hashCode) + this.columnName.hashCode();
        hashCode = (31 * hashCode) + ((Double)this.lowerBound).hashCode();
        hashCode = (31 * hashCode) + ((Double)this.upperBound).hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
