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
 * A set of results returned by {@link
 * com.gpudb.GPUdb#createJob(CreateJobRequest)}.
 */
public class CreateJobResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("CreateJobResponse")
            .namespace("com.gpudb")
            .fields()
                .name("jobId").type().longType().noDefault()
                .name("info").type().map().values().stringType().noDefault()
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
     * Additional information.
     * <ul>
     *         <li> {@link com.gpudb.protocol.CreateJobResponse.Info#JOB_TAG
     * JOB_TAG}: The job tag specified by the user or if unspecified by user, a
     * unique identifier generated internally for the job across clusters.
     * </ul>
     * The default value is an empty {@link Map}.
     * A set of string constants for the parameter {@code info}.
     */
    public static final class Info {

        /**
         * The job tag specified by the user or if unspecified by user, a
         * unique identifier generated internally for the job across clusters.
         */
        public static final String JOB_TAG = "job_tag";

        private Info() {  }
    }

    private long jobId;
    private Map<String, String> info;


    /**
     * Constructs a CreateJobResponse object with default parameters.
     */
    public CreateJobResponse() {
    }

    /**
     * 
     * @return An identifier for the job created by this call.
     * 
     */
    public long getJobId() {
        return jobId;
    }

    /**
     * 
     * @param jobId  An identifier for the job created by this call.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public CreateJobResponse setJobId(long jobId) {
        this.jobId = jobId;
        return this;
    }

    /**
     * 
     * @return Additional information.
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.CreateJobResponse.Info#JOB_TAG JOB_TAG}: The
     *         job tag specified by the user or if unspecified by user, a
     *         unique identifier generated internally for the job across
     *         clusters.
     *         </ul>
     *         The default value is an empty {@link Map}.
     * 
     */
    public Map<String, String> getInfo() {
        return info;
    }

    /**
     * 
     * @param info  Additional information.
     *              <ul>
     *                      <li> {@link
     *              com.gpudb.protocol.CreateJobResponse.Info#JOB_TAG JOB_TAG}:
     *              The job tag specified by the user or if unspecified by
     *              user, a unique identifier generated internally for the job
     *              across clusters.
     *              </ul>
     *              The default value is an empty {@link Map}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public CreateJobResponse setInfo(Map<String, String> info) {
        this.info = (info == null) ? new LinkedHashMap<String, String>() : info;
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
                return this.jobId;

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
     * 
     */
    @Override
    @SuppressWarnings("unchecked")
    public void put(int index, Object value) {
        switch (index) {
            case 0:
                this.jobId = (Long)value;
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

        CreateJobResponse that = (CreateJobResponse)obj;

        return ( ( this.jobId == that.jobId )
                 && this.info.equals( that.info ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "jobId" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.jobId ) );
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
        hashCode = (31 * hashCode) + ((Long)this.jobId).hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }

}
