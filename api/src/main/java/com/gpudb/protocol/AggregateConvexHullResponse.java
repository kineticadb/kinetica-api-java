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
 * A set of results returned by {@link
 * com.gpudb.GPUdb#aggregateConvexHull(AggregateConvexHullRequest)
 * GPUdb.aggregateConvexHull}.
 */
public class AggregateConvexHullResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("AggregateConvexHullResponse")
            .namespace("com.gpudb")
            .fields()
                .name("xVector").type().array().items().doubleType().noDefault()
                .name("yVector").type().array().items().doubleType().noDefault()
                .name("count").type().intType().noDefault()
                .name("isValid").type().booleanType().noDefault()
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

    private List<Double> xVector;
    private List<Double> yVector;
    private int count;
    private boolean isValid;
    private Map<String, String> info;

    /**
     * Constructs an AggregateConvexHullResponse object with default
     * parameters.
     */
    public AggregateConvexHullResponse() {
    }

    /**
     * Array of x coordinates of the resulting convex set.
     *
     * @return The current value of {@code xVector}.
     */
    public List<Double> getXVector() {
        return xVector;
    }

    /**
     * Array of x coordinates of the resulting convex set.
     *
     * @param xVector  The new value for {@code xVector}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AggregateConvexHullResponse setXVector(List<Double> xVector) {
        this.xVector = (xVector == null) ? new ArrayList<Double>() : xVector;
        return this;
    }

    /**
     * Array of y coordinates of the resulting convex set.
     *
     * @return The current value of {@code yVector}.
     */
    public List<Double> getYVector() {
        return yVector;
    }

    /**
     * Array of y coordinates of the resulting convex set.
     *
     * @param yVector  The new value for {@code yVector}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AggregateConvexHullResponse setYVector(List<Double> yVector) {
        this.yVector = (yVector == null) ? new ArrayList<Double>() : yVector;
        return this;
    }

    /**
     * Count of the number of points in the convex set.
     *
     * @return The current value of {@code count}.
     */
    public int getCount() {
        return count;
    }

    /**
     * Count of the number of points in the convex set.
     *
     * @param count  The new value for {@code count}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AggregateConvexHullResponse setCount(int count) {
        this.count = count;
        return this;
    }

    /**
     * @return The current value of {@code isValid}.
     */
    public boolean getIsValid() {
        return isValid;
    }

    /**
     * @param isValid  The new value for {@code isValid}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AggregateConvexHullResponse setIsValid(boolean isValid) {
        this.isValid = isValid;
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
    public AggregateConvexHullResponse setInfo(Map<String, String> info) {
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
                return this.xVector;

            case 1:
                return this.yVector;

            case 2:
                return this.count;

            case 3:
                return this.isValid;

            case 4:
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
                this.xVector = (List<Double>)value;
                break;

            case 1:
                this.yVector = (List<Double>)value;
                break;

            case 2:
                this.count = (Integer)value;
                break;

            case 3:
                this.isValid = (Boolean)value;
                break;

            case 4:
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

        AggregateConvexHullResponse that = (AggregateConvexHullResponse)obj;

        return ( this.xVector.equals( that.xVector )
                 && this.yVector.equals( that.yVector )
                 && ( this.count == that.count )
                 && ( this.isValid == that.isValid )
                 && this.info.equals( that.info ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "xVector" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.xVector ) );
        builder.append( ", " );
        builder.append( gd.toString( "yVector" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.yVector ) );
        builder.append( ", " );
        builder.append( gd.toString( "count" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.count ) );
        builder.append( ", " );
        builder.append( gd.toString( "isValid" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.isValid ) );
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
        hashCode = (31 * hashCode) + this.xVector.hashCode();
        hashCode = (31 * hashCode) + this.yVector.hashCode();
        hashCode = (31 * hashCode) + this.count;
        hashCode = (31 * hashCode) + ((Boolean)this.isValid).hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }
}
