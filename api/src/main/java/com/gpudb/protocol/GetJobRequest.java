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
 * A set of parameters for {@link com.gpudb.GPUdb#getJob(GetJobRequest)
 * GPUdb.getJob}.
 * <p>
 * Get the status and result of asynchronously running job.  See the {@link
 * com.gpudb.GPUdb#createJob(CreateJobRequest) GPUdb.createJob} for starting an
 * asynchronous job.  Some fields of the response are filled only after the
 * submitted job has finished execution.
 */
public class GetJobRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("GetJobRequest")
            .namespace("com.gpudb")
            .fields()
                .name("jobId").type().longType().noDefault()
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
     * A set of string constants for the {@link GetJobRequest} parameter {@link
     * #getOptions() options}.
     * <p>
     * Optional parameters.
     */
    public static final class Options {
        /**
         * Job tag returned in call to create the job
         */
        public static final String JOB_TAG = "job_tag";

        private Options() {  }
    }

    private long jobId;
    private Map<String, String> options;

    /**
     * Constructs a GetJobRequest object with default parameters.
     */
    public GetJobRequest() {
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs a GetJobRequest object with the specified parameters.
     *
     * @param jobId  A unique identifier for the job whose status and result is
     *               to be fetched.
     * @param options  Optional parameters.
     *                 <ul>
     *                     <li>{@link Options#JOB_TAG JOB_TAG}: Job tag
     *                         returned in call to create the job
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     */
    public GetJobRequest(long jobId, Map<String, String> options) {
        this.jobId = jobId;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * A unique identifier for the job whose status and result is to be
     * fetched.
     *
     * @return The current value of {@code jobId}.
     */
    public long getJobId() {
        return jobId;
    }

    /**
     * A unique identifier for the job whose status and result is to be
     * fetched.
     *
     * @param jobId  The new value for {@code jobId}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public GetJobRequest setJobId(long jobId) {
        this.jobId = jobId;
        return this;
    }

    /**
     * Optional parameters.
     * <ul>
     *     <li>{@link Options#JOB_TAG JOB_TAG}: Job tag returned in call to
     *         create the job
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
     *     <li>{@link Options#JOB_TAG JOB_TAG}: Job tag returned in call to
     *         create the job
     * </ul>
     * The default value is an empty {@link Map}.
     *
     * @param options  The new value for {@code options}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public GetJobRequest setOptions(Map<String, String> options) {
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
                return this.jobId;

            case 1:
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
                this.jobId = (Long)value;
                break;

            case 1:
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

        GetJobRequest that = (GetJobRequest)obj;

        return ( ( this.jobId == that.jobId )
                 && this.options.equals( that.options ) );
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
        builder.append( gd.toString( "options" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.options ) );
        builder.append( "}" );

        return builder.toString();
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = (31 * hashCode) + ((Long)this.jobId).hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
