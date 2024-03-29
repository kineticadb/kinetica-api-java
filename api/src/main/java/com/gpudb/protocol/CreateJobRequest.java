/*
 *  This file was autogenerated by the Kinetica schema processor.
 *
 *  DO NOT EDIT DIRECTLY.
 */
package com.gpudb.protocol;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;

/**
 * A set of parameters for {@link com.gpudb.GPUdb#createJob(CreateJobRequest)
 * GPUdb.createJob}.
 * <p>
 * Create a job which will run asynchronously. The response returns a job ID,
 * which can be used to query the status and result of the job. The status and
 * the result of the job upon completion can be requested by {@link
 * com.gpudb.GPUdb#getJob(GetJobRequest) GPUdb.getJob}.
 */
public class CreateJobRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("CreateJobRequest")
            .namespace("com.gpudb")
            .fields()
                .name("endpoint").type().stringType().noDefault()
                .name("requestEncoding").type().stringType().noDefault()
                .name("data").type().bytesType().noDefault()
                .name("dataStr").type().stringType().noDefault()
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
     * A set of string constants for the {@link CreateJobRequest} parameter
     * {@link #getRequestEncoding() requestEncoding}.
     * <p>
     * The encoding of the request payload for the job.
     */
    public static final class RequestEncoding {
        public static final String BINARY = "binary";
        public static final String JSON = "json";
        public static final String SNAPPY = "snappy";

        private RequestEncoding() {  }
    }

    /**
     * A set of string constants for the {@link CreateJobRequest} parameter
     * {@link #getOptions() options}.
     * <p>
     * Optional parameters.
     */
    public static final class Options {
        /**
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}
         *     <li>{@link Options#FALSE FALSE}
         * </ul>
         */
        public static final String REMOVE_JOB_ON_COMPLETE = "remove_job_on_complete";

        public static final String TRUE = "true";
        public static final String FALSE = "false";

        /**
         * Tag to use for submitted job. The same tag could be used on backup
         * cluster to retrieve response for the job. Tags can use letter,
         * numbers, '_' and '-'
         */
        public static final String JOB_TAG = "job_tag";

        private Options() {  }
    }

    private String endpoint;
    private String requestEncoding;
    private ByteBuffer data;
    private String dataStr;
    private Map<String, String> options;

    /**
     * Constructs a CreateJobRequest object with default parameters.
     */
    public CreateJobRequest() {
        endpoint = "";
        requestEncoding = "";
        data = ByteBuffer.wrap( new byte[0] );
        dataStr = "";
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs a CreateJobRequest object with the specified parameters.
     *
     * @param endpoint  Indicates which endpoint to execute, e.g.
     *                  '/alter/table'.
     * @param requestEncoding  The encoding of the request payload for the job.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link RequestEncoding#BINARY BINARY}
     *                             <li>{@link RequestEncoding#JSON JSON}
     *                             <li>{@link RequestEncoding#SNAPPY SNAPPY}
     *                         </ul>
     *                         The default value is {@link
     *                         RequestEncoding#BINARY BINARY}.
     * @param data  Binary-encoded payload for the job to be run
     *              asynchronously.  The payload must contain the relevant
     *              input parameters for the endpoint indicated in {@code
     *              endpoint}.  Please see the documentation for the
     *              appropriate endpoint to see what values must (or can) be
     *              specified.  If this parameter is used, then {@code
     *              requestEncoding} must be {@link RequestEncoding#BINARY
     *              BINARY} or {@link RequestEncoding#SNAPPY SNAPPY}.
     * @param dataStr  JSON-encoded payload for the job to be run
     *                 asynchronously.  The payload must contain the relevant
     *                 input parameters for the endpoint indicated in {@code
     *                 endpoint}.  Please see the documentation for the
     *                 appropriate endpoint to see what values must (or can) be
     *                 specified.  If this parameter is used, then {@code
     *                 requestEncoding} must be {@link RequestEncoding#JSON
     *                 JSON}.
     * @param options  Optional parameters.
     *                 <ul>
     *                     <li>{@link Options#REMOVE_JOB_ON_COMPLETE
     *                         REMOVE_JOB_ON_COMPLETE}:
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#TRUE TRUE}
     *                             <li>{@link Options#FALSE FALSE}
     *                         </ul>
     *                     <li>{@link Options#JOB_TAG JOB_TAG}: Tag to use for
     *                         submitted job. The same tag could be used on
     *                         backup cluster to retrieve response for the job.
     *                         Tags can use letter, numbers, '_' and '-'
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     */
    public CreateJobRequest(String endpoint, String requestEncoding, ByteBuffer data, String dataStr, Map<String, String> options) {
        this.endpoint = (endpoint == null) ? "" : endpoint;
        this.requestEncoding = (requestEncoding == null) ? "" : requestEncoding;
        this.data = (data == null) ? ByteBuffer.wrap( new byte[0] ) : data;
        this.dataStr = (dataStr == null) ? "" : dataStr;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * Indicates which endpoint to execute, e.g.&nbsp;'/alter/table'.
     *
     * @return The current value of {@code endpoint}.
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Indicates which endpoint to execute, e.g.&nbsp;'/alter/table'.
     *
     * @param endpoint  The new value for {@code endpoint}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public CreateJobRequest setEndpoint(String endpoint) {
        this.endpoint = (endpoint == null) ? "" : endpoint;
        return this;
    }

    /**
     * The encoding of the request payload for the job.
     * Supported values:
     * <ul>
     *     <li>{@link RequestEncoding#BINARY BINARY}
     *     <li>{@link RequestEncoding#JSON JSON}
     *     <li>{@link RequestEncoding#SNAPPY SNAPPY}
     * </ul>
     * The default value is {@link RequestEncoding#BINARY BINARY}.
     *
     * @return The current value of {@code requestEncoding}.
     */
    public String getRequestEncoding() {
        return requestEncoding;
    }

    /**
     * The encoding of the request payload for the job.
     * Supported values:
     * <ul>
     *     <li>{@link RequestEncoding#BINARY BINARY}
     *     <li>{@link RequestEncoding#JSON JSON}
     *     <li>{@link RequestEncoding#SNAPPY SNAPPY}
     * </ul>
     * The default value is {@link RequestEncoding#BINARY BINARY}.
     *
     * @param requestEncoding  The new value for {@code requestEncoding}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public CreateJobRequest setRequestEncoding(String requestEncoding) {
        this.requestEncoding = (requestEncoding == null) ? "" : requestEncoding;
        return this;
    }

    /**
     * Binary-encoded payload for the job to be run asynchronously.  The
     * payload must contain the relevant input parameters for the endpoint
     * indicated in {@link #getEndpoint() endpoint}.  Please see the
     * documentation for the appropriate endpoint to see what values must (or
     * can) be specified.  If this parameter is used, then {@link
     * #getRequestEncoding() requestEncoding} must be {@link
     * RequestEncoding#BINARY BINARY} or {@link RequestEncoding#SNAPPY SNAPPY}.
     *
     * @return The current value of {@code data}.
     */
    public ByteBuffer getData() {
        return data;
    }

    /**
     * Binary-encoded payload for the job to be run asynchronously.  The
     * payload must contain the relevant input parameters for the endpoint
     * indicated in {@link #getEndpoint() endpoint}.  Please see the
     * documentation for the appropriate endpoint to see what values must (or
     * can) be specified.  If this parameter is used, then {@link
     * #getRequestEncoding() requestEncoding} must be {@link
     * RequestEncoding#BINARY BINARY} or {@link RequestEncoding#SNAPPY SNAPPY}.
     *
     * @param data  The new value for {@code data}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public CreateJobRequest setData(ByteBuffer data) {
        this.data = (data == null) ? ByteBuffer.wrap( new byte[0] ) : data;
        return this;
    }

    /**
     * JSON-encoded payload for the job to be run asynchronously.  The payload
     * must contain the relevant input parameters for the endpoint indicated in
     * {@link #getEndpoint() endpoint}.  Please see the documentation for the
     * appropriate endpoint to see what values must (or can) be specified.  If
     * this parameter is used, then {@link #getRequestEncoding()
     * requestEncoding} must be {@link RequestEncoding#JSON JSON}.
     *
     * @return The current value of {@code dataStr}.
     */
    public String getDataStr() {
        return dataStr;
    }

    /**
     * JSON-encoded payload for the job to be run asynchronously.  The payload
     * must contain the relevant input parameters for the endpoint indicated in
     * {@link #getEndpoint() endpoint}.  Please see the documentation for the
     * appropriate endpoint to see what values must (or can) be specified.  If
     * this parameter is used, then {@link #getRequestEncoding()
     * requestEncoding} must be {@link RequestEncoding#JSON JSON}.
     *
     * @param dataStr  The new value for {@code dataStr}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public CreateJobRequest setDataStr(String dataStr) {
        this.dataStr = (dataStr == null) ? "" : dataStr;
        return this;
    }

    /**
     * Optional parameters.
     * <ul>
     *     <li>{@link Options#REMOVE_JOB_ON_COMPLETE REMOVE_JOB_ON_COMPLETE}:
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *     <li>{@link Options#JOB_TAG JOB_TAG}: Tag to use for submitted job.
     *         The same tag could be used on backup cluster to retrieve
     *         response for the job. Tags can use letter, numbers, '_' and '-'
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
     *     <li>{@link Options#REMOVE_JOB_ON_COMPLETE REMOVE_JOB_ON_COMPLETE}:
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *     <li>{@link Options#JOB_TAG JOB_TAG}: Tag to use for submitted job.
     *         The same tag could be used on backup cluster to retrieve
     *         response for the job. Tags can use letter, numbers, '_' and '-'
     * </ul>
     * The default value is an empty {@link Map}.
     *
     * @param options  The new value for {@code options}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public CreateJobRequest setOptions(Map<String, String> options) {
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
                return this.endpoint;

            case 1:
                return this.requestEncoding;

            case 2:
                return this.data;

            case 3:
                return this.dataStr;

            case 4:
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
                this.endpoint = (String)value;
                break;

            case 1:
                this.requestEncoding = (String)value;
                break;

            case 2:
                this.data = (ByteBuffer)value;
                break;

            case 3:
                this.dataStr = (String)value;
                break;

            case 4:
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

        CreateJobRequest that = (CreateJobRequest)obj;

        return ( this.endpoint.equals( that.endpoint )
                 && this.requestEncoding.equals( that.requestEncoding )
                 && this.data.equals( that.data )
                 && this.dataStr.equals( that.dataStr )
                 && this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "endpoint" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.endpoint ) );
        builder.append( ", " );
        builder.append( gd.toString( "requestEncoding" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.requestEncoding ) );
        builder.append( ", " );
        builder.append( gd.toString( "data" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.data ) );
        builder.append( ", " );
        builder.append( gd.toString( "dataStr" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.dataStr ) );
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
        hashCode = (31 * hashCode) + this.endpoint.hashCode();
        hashCode = (31 * hashCode) + this.requestEncoding.hashCode();
        hashCode = (31 * hashCode) + this.data.hashCode();
        hashCode = (31 * hashCode) + this.dataStr.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
