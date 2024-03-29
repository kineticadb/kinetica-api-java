/*
 *  This file was autogenerated by the Kinetica schema processor.
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
 * A set of results returned by {@link
 * com.gpudb.GPUdb#aggregateStatisticsByRange(AggregateStatisticsByRangeRequest)
 * GPUdb.aggregateStatisticsByRange}.
 */
public class AggregateStatisticsByRangeResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("AggregateStatisticsByRangeResponse")
            .namespace("com.gpudb")
            .fields()
                .name("stats").type().map().values().array().items().doubleType().noDefault()
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

    private Map<String, List<Double>> stats;
    private Map<String, String> info;

    /**
     * Constructs an AggregateStatisticsByRangeResponse object with default
     * parameters.
     */
    public AggregateStatisticsByRangeResponse() {
    }

    /**
     * A map with a key for each statistic in the stats input parameter having
     * a value that is a vector of the corresponding value-column bin
     * statistics. In a addition the key count has a value that is a histogram
     * of the binning-column.
     *
     * @return The current value of {@code stats}.
     */
    public Map<String, List<Double>> getStats() {
        return stats;
    }

    /**
     * A map with a key for each statistic in the stats input parameter having
     * a value that is a vector of the corresponding value-column bin
     * statistics. In a addition the key count has a value that is a histogram
     * of the binning-column.
     *
     * @param stats  The new value for {@code stats}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AggregateStatisticsByRangeResponse setStats(Map<String, List<Double>> stats) {
        this.stats = (stats == null) ? new LinkedHashMap<String, List<Double>>() : stats;
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
    public AggregateStatisticsByRangeResponse setInfo(Map<String, String> info) {
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
                return this.stats;

            case 1:
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
                this.stats = (Map<String, List<Double>>)value;
                break;

            case 1:
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

        AggregateStatisticsByRangeResponse that = (AggregateStatisticsByRangeResponse)obj;

        return ( this.stats.equals( that.stats )
                 && this.info.equals( that.info ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "stats" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.stats ) );
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
        hashCode = (31 * hashCode) + this.stats.hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }
}
