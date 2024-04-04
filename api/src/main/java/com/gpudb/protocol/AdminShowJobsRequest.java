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
 * com.gpudb.GPUdb#adminShowJobs(AdminShowJobsRequest) GPUdb.adminShowJobs}.
 * <p>
 * Get a list of the current jobs in GPUdb.
 */
public class AdminShowJobsRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("AdminShowJobsRequest")
            .namespace("com.gpudb")
            .fields()
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
     * A set of string constants for the {@link AdminShowJobsRequest} parameter
     * {@link #getOptions() options}.
     * <p>
     * Optional parameters.
     */
    public static final class Options {
        /**
         * If {@link Options#TRUE TRUE}, then the completed async jobs are also
         * included in the response. By default, once the async jobs are
         * completed they are no longer included in the jobs list.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}
         *     <li>{@link Options#FALSE FALSE}
         * </ul>
         * The default value is {@link Options#FALSE FALSE}.
         */
        public static final String SHOW_ASYNC_JOBS = "show_async_jobs";

        public static final String TRUE = "true";
        public static final String FALSE = "false";

        /**
         * If {@link Options#TRUE TRUE}, then information is also returned from
         * worker ranks. By default only status from the head rank is returned.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}
         *     <li>{@link Options#FALSE FALSE}
         * </ul>
         */
        public static final String SHOW_WORKER_INFO = "show_worker_info";

        private Options() {  }
    }

    private Map<String, String> options;

    /**
     * Constructs an AdminShowJobsRequest object with default parameters.
     */
    public AdminShowJobsRequest() {
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs an AdminShowJobsRequest object with the specified parameters.
     *
     * @param options  Optional parameters.
     *                 <ul>
     *                     <li>{@link Options#SHOW_ASYNC_JOBS SHOW_ASYNC_JOBS}:
     *                         If {@link Options#TRUE TRUE}, then the completed
     *                         async jobs are also included in the response. By
     *                         default, once the async jobs are completed they
     *                         are no longer included in the jobs list.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#TRUE TRUE}
     *                             <li>{@link Options#FALSE FALSE}
     *                         </ul>
     *                         The default value is {@link Options#FALSE
     *                         FALSE}.
     *                     <li>{@link Options#SHOW_WORKER_INFO
     *                         SHOW_WORKER_INFO}: If {@link Options#TRUE TRUE},
     *                         then information is also returned from worker
     *                         ranks. By default only status from the head rank
     *                         is returned.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#TRUE TRUE}
     *                             <li>{@link Options#FALSE FALSE}
     *                         </ul>
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     */
    public AdminShowJobsRequest(Map<String, String> options) {
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * Optional parameters.
     * <ul>
     *     <li>{@link Options#SHOW_ASYNC_JOBS SHOW_ASYNC_JOBS}: If {@link
     *         Options#TRUE TRUE}, then the completed async jobs are also
     *         included in the response. By default, once the async jobs are
     *         completed they are no longer included in the jobs list.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     *     <li>{@link Options#SHOW_WORKER_INFO SHOW_WORKER_INFO}: If {@link
     *         Options#TRUE TRUE}, then information is also returned from
     *         worker ranks. By default only status from the head rank is
     *         returned.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
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
     *     <li>{@link Options#SHOW_ASYNC_JOBS SHOW_ASYNC_JOBS}: If {@link
     *         Options#TRUE TRUE}, then the completed async jobs are also
     *         included in the response. By default, once the async jobs are
     *         completed they are no longer included in the jobs list.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     *     <li>{@link Options#SHOW_WORKER_INFO SHOW_WORKER_INFO}: If {@link
     *         Options#TRUE TRUE}, then information is also returned from
     *         worker ranks. By default only status from the head rank is
     *         returned.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     * </ul>
     * The default value is an empty {@link Map}.
     *
     * @param options  The new value for {@code options}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AdminShowJobsRequest setOptions(Map<String, String> options) {
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

        AdminShowJobsRequest that = (AdminShowJobsRequest)obj;

        return ( this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "options" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.options ) );
        builder.append( "}" );

        return builder.toString();
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
