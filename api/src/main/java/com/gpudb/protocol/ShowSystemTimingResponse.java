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
 * com.gpudb.GPUdb#showSystemTiming(ShowSystemTimingRequest)
 * GPUdb.showSystemTiming}.
 */
public class ShowSystemTimingResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("ShowSystemTimingResponse")
            .namespace("com.gpudb")
            .fields()
                .name("endpoints").type().array().items().stringType().noDefault()
                .name("timeInMs").type().array().items().floatType().noDefault()
                .name("jobids").type().array().items().stringType().noDefault()
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

    private List<String> endpoints;
    private List<Float> timeInMs;
    private List<String> jobids;
    private Map<String, String> info;

    /**
     * Constructs a ShowSystemTimingResponse object with default parameters.
     */
    public ShowSystemTimingResponse() {
    }

    /**
     * List of recently called endpoints, most recent first.
     *
     * @return The current value of {@code endpoints}.
     */
    public List<String> getEndpoints() {
        return endpoints;
    }

    /**
     * List of recently called endpoints, most recent first.
     *
     * @param endpoints  The new value for {@code endpoints}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public ShowSystemTimingResponse setEndpoints(List<String> endpoints) {
        this.endpoints = (endpoints == null) ? new ArrayList<String>() : endpoints;
        return this;
    }

    /**
     * List of time (in ms) of the recent requests.
     *
     * @return The current value of {@code timeInMs}.
     */
    public List<Float> getTimeInMs() {
        return timeInMs;
    }

    /**
     * List of time (in ms) of the recent requests.
     *
     * @param timeInMs  The new value for {@code timeInMs}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public ShowSystemTimingResponse setTimeInMs(List<Float> timeInMs) {
        this.timeInMs = (timeInMs == null) ? new ArrayList<Float>() : timeInMs;
        return this;
    }

    /**
     * List of the internal job ids for the recent requests.
     *
     * @return The current value of {@code jobids}.
     */
    public List<String> getJobids() {
        return jobids;
    }

    /**
     * List of the internal job ids for the recent requests.
     *
     * @param jobids  The new value for {@code jobids}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public ShowSystemTimingResponse setJobids(List<String> jobids) {
        this.jobids = (jobids == null) ? new ArrayList<String>() : jobids;
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
    public ShowSystemTimingResponse setInfo(Map<String, String> info) {
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
                return this.endpoints;

            case 1:
                return this.timeInMs;

            case 2:
                return this.jobids;

            case 3:
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
                this.endpoints = (List<String>)value;
                break;

            case 1:
                this.timeInMs = (List<Float>)value;
                break;

            case 2:
                this.jobids = (List<String>)value;
                break;

            case 3:
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

        ShowSystemTimingResponse that = (ShowSystemTimingResponse)obj;

        return ( this.endpoints.equals( that.endpoints )
                 && this.timeInMs.equals( that.timeInMs )
                 && this.jobids.equals( that.jobids )
                 && this.info.equals( that.info ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "endpoints" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.endpoints ) );
        builder.append( ", " );
        builder.append( gd.toString( "timeInMs" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.timeInMs ) );
        builder.append( ", " );
        builder.append( gd.toString( "jobids" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.jobids ) );
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
        hashCode = (31 * hashCode) + this.endpoints.hashCode();
        hashCode = (31 * hashCode) + this.timeInMs.hashCode();
        hashCode = (31 * hashCode) + this.jobids.hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }
}
