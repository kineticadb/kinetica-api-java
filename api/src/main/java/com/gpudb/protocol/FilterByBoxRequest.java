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
 * com.gpudb.GPUdb#filterByBox(FilterByBoxRequest)}.
 * <p>
 * Calculates how many objects within the given table lie in a
 * rectangular box. The operation is synchronous, meaning that a response will
 * not
 * be returned until all the objects are fully available. The response payload
 * provides the count of the resulting set. A new resultant set which satisfies
 * the
 * input NAI restriction specification is also created when a {@code viewName}
 * is
 * passed in as part of the input payload.
 */
public class FilterByBoxRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("FilterByBoxRequest")
            .namespace("com.gpudb")
            .fields()
                .name("tableName").type().stringType().noDefault()
                .name("viewName").type().stringType().noDefault()
                .name("xColumnName").type().stringType().noDefault()
                .name("minX").type().doubleType().noDefault()
                .name("maxX").type().doubleType().noDefault()
                .name("yColumnName").type().stringType().noDefault()
                .name("minY").type().doubleType().noDefault()
                .name("maxY").type().doubleType().noDefault()
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
     * com.gpudb.protocol.FilterByBoxRequest.Options#COLLECTION_NAME
     * COLLECTION_NAME}: [DEPRECATED--please specify the containing schema for
     * the view as part of {@code viewName} and use {@link
     * com.gpudb.GPUdb#createSchema(CreateSchemaRequest)} to create the schema
     * if non-existent]  Name of a schema for the newly created view. If the
     * schema is non-existent, it will be automatically created.
     * </ul>
     * The default value is an empty {@link Map}.
     * A set of string constants for the parameter {@code options}.
     */
    public static final class Options {

        /**
         * [DEPRECATED--please specify the containing schema for the view as
         * part of {@code viewName} and use {@link
         * com.gpudb.GPUdb#createSchema(CreateSchemaRequest)} to create the
         * schema if non-existent]  Name of a schema for the newly created
         * view. If the schema is non-existent, it will be automatically
         * created.
         */
        public static final String COLLECTION_NAME = "collection_name";

        private Options() {  }
    }

    private String tableName;
    private String viewName;
    private String xColumnName;
    private double minX;
    private double maxX;
    private String yColumnName;
    private double minY;
    private double maxY;
    private Map<String, String> options;


    /**
     * Constructs a FilterByBoxRequest object with default parameters.
     */
    public FilterByBoxRequest() {
        tableName = "";
        viewName = "";
        xColumnName = "";
        yColumnName = "";
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs a FilterByBoxRequest object with the specified parameters.
     * 
     * @param tableName  Name of the table on which the bounding box operation
     *                   will be performed, in [schema_name.]table_name format,
     *                   using standard <a
     *                   href="../../../../../concepts/tables.html#table-name-resolution"
     *                   target="_top">name resolution rules</a>.  Must be an
     *                   existing table.
     * @param viewName  If provided, then this will be the name of the view
     *                  containing the results, in [schema_name.]view_name
     *                  format, using standard <a
     *                  href="../../../../../concepts/tables.html#table-name-resolution"
     *                  target="_top">name resolution rules</a> and meeting <a
     *                  href="../../../../../concepts/tables.html#table-naming-criteria"
     *                  target="_top">table naming criteria</a>.  Must not be
     *                  an already existing table or view.  The default value
     *                  is ''.
     * @param xColumnName  Name of the column on which to perform the bounding
     *                     box query. Must be a valid numeric column.
     * @param minX  Lower bound for the column chosen by {@code xColumnName}.
     *              Must be less than or equal to {@code maxX}.
     * @param maxX  Upper bound for {@code xColumnName}.  Must be greater than
     *              or equal to {@code minX}.
     * @param yColumnName  Name of a column on which to perform the bounding
     *                     box query. Must be a valid numeric column.
     * @param minY  Lower bound for {@code yColumnName}. Must be less than or
     *              equal to {@code maxY}.
     * @param maxY  Upper bound for {@code yColumnName}. Must be greater than
     *              or equal to {@code minY}.
     * @param options  Optional parameters.
     *                 <ul>
     *                         <li> {@link
     *                 com.gpudb.protocol.FilterByBoxRequest.Options#COLLECTION_NAME
     *                 COLLECTION_NAME}: [DEPRECATED--please specify the
     *                 containing schema for the view as part of {@code
     *                 viewName} and use {@link
     *                 com.gpudb.GPUdb#createSchema(CreateSchemaRequest)} to
     *                 create the schema if non-existent]  Name of a schema for
     *                 the newly created view. If the schema is non-existent,
     *                 it will be automatically created.
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     * 
     */
    public FilterByBoxRequest(String tableName, String viewName, String xColumnName, double minX, double maxX, String yColumnName, double minY, double maxY, Map<String, String> options) {
        this.tableName = (tableName == null) ? "" : tableName;
        this.viewName = (viewName == null) ? "" : viewName;
        this.xColumnName = (xColumnName == null) ? "" : xColumnName;
        this.minX = minX;
        this.maxX = maxX;
        this.yColumnName = (yColumnName == null) ? "" : yColumnName;
        this.minY = minY;
        this.maxY = maxY;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * 
     * @return Name of the table on which the bounding box operation will be
     *         performed, in [schema_name.]table_name format, using standard <a
     *         href="../../../../../concepts/tables.html#table-name-resolution"
     *         target="_top">name resolution rules</a>.  Must be an existing
     *         table.
     * 
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * 
     * @param tableName  Name of the table on which the bounding box operation
     *                   will be performed, in [schema_name.]table_name format,
     *                   using standard <a
     *                   href="../../../../../concepts/tables.html#table-name-resolution"
     *                   target="_top">name resolution rules</a>.  Must be an
     *                   existing table.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public FilterByBoxRequest setTableName(String tableName) {
        this.tableName = (tableName == null) ? "" : tableName;
        return this;
    }

    /**
     * 
     * @return If provided, then this will be the name of the view containing
     *         the results, in [schema_name.]view_name format, using standard
     *         <a
     *         href="../../../../../concepts/tables.html#table-name-resolution"
     *         target="_top">name resolution rules</a> and meeting <a
     *         href="../../../../../concepts/tables.html#table-naming-criteria"
     *         target="_top">table naming criteria</a>.  Must not be an already
     *         existing table or view.  The default value is ''.
     * 
     */
    public String getViewName() {
        return viewName;
    }

    /**
     * 
     * @param viewName  If provided, then this will be the name of the view
     *                  containing the results, in [schema_name.]view_name
     *                  format, using standard <a
     *                  href="../../../../../concepts/tables.html#table-name-resolution"
     *                  target="_top">name resolution rules</a> and meeting <a
     *                  href="../../../../../concepts/tables.html#table-naming-criteria"
     *                  target="_top">table naming criteria</a>.  Must not be
     *                  an already existing table or view.  The default value
     *                  is ''.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public FilterByBoxRequest setViewName(String viewName) {
        this.viewName = (viewName == null) ? "" : viewName;
        return this;
    }

    /**
     * 
     * @return Name of the column on which to perform the bounding box query.
     *         Must be a valid numeric column.
     * 
     */
    public String getXColumnName() {
        return xColumnName;
    }

    /**
     * 
     * @param xColumnName  Name of the column on which to perform the bounding
     *                     box query. Must be a valid numeric column.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public FilterByBoxRequest setXColumnName(String xColumnName) {
        this.xColumnName = (xColumnName == null) ? "" : xColumnName;
        return this;
    }

    /**
     * 
     * @return Lower bound for the column chosen by {@code xColumnName}.  Must
     *         be less than or equal to {@code maxX}.
     * 
     */
    public double getMinX() {
        return minX;
    }

    /**
     * 
     * @param minX  Lower bound for the column chosen by {@code xColumnName}.
     *              Must be less than or equal to {@code maxX}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public FilterByBoxRequest setMinX(double minX) {
        this.minX = minX;
        return this;
    }

    /**
     * 
     * @return Upper bound for {@code xColumnName}.  Must be greater than or
     *         equal to {@code minX}.
     * 
     */
    public double getMaxX() {
        return maxX;
    }

    /**
     * 
     * @param maxX  Upper bound for {@code xColumnName}.  Must be greater than
     *              or equal to {@code minX}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public FilterByBoxRequest setMaxX(double maxX) {
        this.maxX = maxX;
        return this;
    }

    /**
     * 
     * @return Name of a column on which to perform the bounding box query.
     *         Must be a valid numeric column.
     * 
     */
    public String getYColumnName() {
        return yColumnName;
    }

    /**
     * 
     * @param yColumnName  Name of a column on which to perform the bounding
     *                     box query. Must be a valid numeric column.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public FilterByBoxRequest setYColumnName(String yColumnName) {
        this.yColumnName = (yColumnName == null) ? "" : yColumnName;
        return this;
    }

    /**
     * 
     * @return Lower bound for {@code yColumnName}. Must be less than or equal
     *         to {@code maxY}.
     * 
     */
    public double getMinY() {
        return minY;
    }

    /**
     * 
     * @param minY  Lower bound for {@code yColumnName}. Must be less than or
     *              equal to {@code maxY}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public FilterByBoxRequest setMinY(double minY) {
        this.minY = minY;
        return this;
    }

    /**
     * 
     * @return Upper bound for {@code yColumnName}. Must be greater than or
     *         equal to {@code minY}.
     * 
     */
    public double getMaxY() {
        return maxY;
    }

    /**
     * 
     * @param maxY  Upper bound for {@code yColumnName}. Must be greater than
     *              or equal to {@code minY}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public FilterByBoxRequest setMaxY(double maxY) {
        this.maxY = maxY;
        return this;
    }

    /**
     * 
     * @return Optional parameters.
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.FilterByBoxRequest.Options#COLLECTION_NAME
     *         COLLECTION_NAME}: [DEPRECATED--please specify the containing
     *         schema for the view as part of {@code viewName} and use {@link
     *         com.gpudb.GPUdb#createSchema(CreateSchemaRequest)} to create the
     *         schema if non-existent]  Name of a schema for the newly created
     *         view. If the schema is non-existent, it will be automatically
     *         created.
     *         </ul>
     *         The default value is an empty {@link Map}.
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
     *                 com.gpudb.protocol.FilterByBoxRequest.Options#COLLECTION_NAME
     *                 COLLECTION_NAME}: [DEPRECATED--please specify the
     *                 containing schema for the view as part of {@code
     *                 viewName} and use {@link
     *                 com.gpudb.GPUdb#createSchema(CreateSchemaRequest)} to
     *                 create the schema if non-existent]  Name of a schema for
     *                 the newly created view. If the schema is non-existent,
     *                 it will be automatically created.
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public FilterByBoxRequest setOptions(Map<String, String> options) {
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
                return this.xColumnName;

            case 3:
                return this.minX;

            case 4:
                return this.maxX;

            case 5:
                return this.yColumnName;

            case 6:
                return this.minY;

            case 7:
                return this.maxY;

            case 8:
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
                this.xColumnName = (String)value;
                break;

            case 3:
                this.minX = (Double)value;
                break;

            case 4:
                this.maxX = (Double)value;
                break;

            case 5:
                this.yColumnName = (String)value;
                break;

            case 6:
                this.minY = (Double)value;
                break;

            case 7:
                this.maxY = (Double)value;
                break;

            case 8:
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

        FilterByBoxRequest that = (FilterByBoxRequest)obj;

        return ( this.tableName.equals( that.tableName )
                 && this.viewName.equals( that.viewName )
                 && this.xColumnName.equals( that.xColumnName )
                 && ( (Double)this.minX ).equals( (Double)that.minX )
                 && ( (Double)this.maxX ).equals( (Double)that.maxX )
                 && this.yColumnName.equals( that.yColumnName )
                 && ( (Double)this.minY ).equals( (Double)that.minY )
                 && ( (Double)this.maxY ).equals( (Double)that.maxY )
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
        builder.append( gd.toString( "minX" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.minX ) );
        builder.append( ", " );
        builder.append( gd.toString( "maxX" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.maxX ) );
        builder.append( ", " );
        builder.append( gd.toString( "yColumnName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.yColumnName ) );
        builder.append( ", " );
        builder.append( gd.toString( "minY" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.minY ) );
        builder.append( ", " );
        builder.append( gd.toString( "maxY" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.maxY ) );
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
        hashCode = (31 * hashCode) + ((Double)this.minX).hashCode();
        hashCode = (31 * hashCode) + ((Double)this.maxX).hashCode();
        hashCode = (31 * hashCode) + this.yColumnName.hashCode();
        hashCode = (31 * hashCode) + ((Double)this.minY).hashCode();
        hashCode = (31 * hashCode) + ((Double)this.maxY).hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }

}
