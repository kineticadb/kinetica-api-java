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
 * A set of parameters for {@link com.gpudb.GPUdb#alterTier(AlterTierRequest)
 * GPUdb.alterTier}.
 * <p>
 * Alters properties of an exisiting <a
 * href="../../../../../../rm/concepts/#storage-tiers" target="_top">tier</a>
 * to facilitate <a href="../../../../../../rm/concepts/"
 * target="_top">resource management</a>.
 * <p>
 * To disable <a href="../../../../../../rm/concepts/#watermark-based-eviction"
 * target="_top">watermark-based eviction</a>, set both {@link
 * Options#HIGH_WATERMARK HIGH_WATERMARK} and {@link Options#LOW_WATERMARK
 * LOW_WATERMARK} to 100.
 */
public class AlterTierRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("AlterTierRequest")
            .namespace("com.gpudb")
            .fields()
                .name("name").type().stringType().noDefault()
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
     * A set of string constants for the {@link AlterTierRequest} parameter
     * {@link #getOptions() options}.
     * <p>
     * Optional parameters.
     */
    public static final class Options {
        /**
         * Maximum size in bytes this tier may hold at once.
         */
        public static final String CAPACITY = "capacity";

        /**
         * Threshold of usage of this tier's resource that once exceeded, will
         * trigger watermark-based eviction from this tier. The minimum allowed
         * value is '0'. The maximum allowed value is '100'.
         */
        public static final String HIGH_WATERMARK = "high_watermark";

        /**
         * Threshold of resource usage that once fallen below after crossing
         * the {@link Options#HIGH_WATERMARK HIGH_WATERMARK}, will cease
         * watermark-based eviction from this tier. The minimum allowed value
         * is '0'. The maximum allowed value is '100'.
         */
        public static final String LOW_WATERMARK = "low_watermark";

        /**
         * Timeout in seconds for reading from or writing to this resource.
         * Applies to cold storage tiers only.
         */
        public static final String WAIT_TIMEOUT = "wait_timeout";

        /**
         * If {@link Options#TRUE TRUE} the system configuration will be
         * written to disk upon successful application of this request. This
         * will commit the changes from this request and any additional
         * in-memory modifications.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}
         *     <li>{@link Options#FALSE FALSE}
         * </ul>
         * The default value is {@link Options#TRUE TRUE}.
         */
        public static final String PERSIST = "persist";

        public static final String TRUE = "true";
        public static final String FALSE = "false";

        /**
         * Apply the requested change only to a specific rank. The minimum
         * allowed value is '0'. The maximum allowed value is '10000'.
         */
        public static final String RANK = "rank";

        private Options() {  }
    }

    private String name;
    private Map<String, String> options;

    /**
     * Constructs an AlterTierRequest object with default parameters.
     */
    public AlterTierRequest() {
        name = "";
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs an AlterTierRequest object with the specified parameters.
     *
     * @param name  Name of the tier to be altered. Must be an existing tier
     *              group name.
     * @param options  Optional parameters.
     *                 <ul>
     *                     <li>{@link Options#CAPACITY CAPACITY}: Maximum size
     *                         in bytes this tier may hold at once.
     *                     <li>{@link Options#HIGH_WATERMARK HIGH_WATERMARK}:
     *                         Threshold of usage of this tier's resource that
     *                         once exceeded, will trigger watermark-based
     *                         eviction from this tier. The minimum allowed
     *                         value is '0'. The maximum allowed value is
     *                         '100'.
     *                     <li>{@link Options#LOW_WATERMARK LOW_WATERMARK}:
     *                         Threshold of resource usage that once fallen
     *                         below after crossing the {@link
     *                         Options#HIGH_WATERMARK HIGH_WATERMARK}, will
     *                         cease watermark-based eviction from this tier.
     *                         The minimum allowed value is '0'. The maximum
     *                         allowed value is '100'.
     *                     <li>{@link Options#WAIT_TIMEOUT WAIT_TIMEOUT}:
     *                         Timeout in seconds for reading from or writing
     *                         to this resource. Applies to cold storage tiers
     *                         only.
     *                     <li>{@link Options#PERSIST PERSIST}: If {@link
     *                         Options#TRUE TRUE} the system configuration will
     *                         be written to disk upon successful application
     *                         of this request. This will commit the changes
     *                         from this request and any additional in-memory
     *                         modifications.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#TRUE TRUE}
     *                             <li>{@link Options#FALSE FALSE}
     *                         </ul>
     *                         The default value is {@link Options#TRUE TRUE}.
     *                     <li>{@link Options#RANK RANK}: Apply the requested
     *                         change only to a specific rank. The minimum
     *                         allowed value is '0'. The maximum allowed value
     *                         is '10000'.
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     */
    public AlterTierRequest(String name, Map<String, String> options) {
        this.name = (name == null) ? "" : name;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * Name of the tier to be altered. Must be an existing tier group name.
     *
     * @return The current value of {@code name}.
     */
    public String getName() {
        return name;
    }

    /**
     * Name of the tier to be altered. Must be an existing tier group name.
     *
     * @param name  The new value for {@code name}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AlterTierRequest setName(String name) {
        this.name = (name == null) ? "" : name;
        return this;
    }

    /**
     * Optional parameters.
     * <ul>
     *     <li>{@link Options#CAPACITY CAPACITY}: Maximum size in bytes this
     *         tier may hold at once.
     *     <li>{@link Options#HIGH_WATERMARK HIGH_WATERMARK}: Threshold of
     *         usage of this tier's resource that once exceeded, will trigger
     *         watermark-based eviction from this tier. The minimum allowed
     *         value is '0'. The maximum allowed value is '100'.
     *     <li>{@link Options#LOW_WATERMARK LOW_WATERMARK}: Threshold of
     *         resource usage that once fallen below after crossing the {@link
     *         Options#HIGH_WATERMARK HIGH_WATERMARK}, will cease
     *         watermark-based eviction from this tier. The minimum allowed
     *         value is '0'. The maximum allowed value is '100'.
     *     <li>{@link Options#WAIT_TIMEOUT WAIT_TIMEOUT}: Timeout in seconds
     *         for reading from or writing to this resource. Applies to cold
     *         storage tiers only.
     *     <li>{@link Options#PERSIST PERSIST}: If {@link Options#TRUE TRUE}
     *         the system configuration will be written to disk upon successful
     *         application of this request. This will commit the changes from
     *         this request and any additional in-memory modifications.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#TRUE TRUE}.
     *     <li>{@link Options#RANK RANK}: Apply the requested change only to a
     *         specific rank. The minimum allowed value is '0'. The maximum
     *         allowed value is '10000'.
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
     *     <li>{@link Options#CAPACITY CAPACITY}: Maximum size in bytes this
     *         tier may hold at once.
     *     <li>{@link Options#HIGH_WATERMARK HIGH_WATERMARK}: Threshold of
     *         usage of this tier's resource that once exceeded, will trigger
     *         watermark-based eviction from this tier. The minimum allowed
     *         value is '0'. The maximum allowed value is '100'.
     *     <li>{@link Options#LOW_WATERMARK LOW_WATERMARK}: Threshold of
     *         resource usage that once fallen below after crossing the {@link
     *         Options#HIGH_WATERMARK HIGH_WATERMARK}, will cease
     *         watermark-based eviction from this tier. The minimum allowed
     *         value is '0'. The maximum allowed value is '100'.
     *     <li>{@link Options#WAIT_TIMEOUT WAIT_TIMEOUT}: Timeout in seconds
     *         for reading from or writing to this resource. Applies to cold
     *         storage tiers only.
     *     <li>{@link Options#PERSIST PERSIST}: If {@link Options#TRUE TRUE}
     *         the system configuration will be written to disk upon successful
     *         application of this request. This will commit the changes from
     *         this request and any additional in-memory modifications.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#TRUE TRUE}.
     *     <li>{@link Options#RANK RANK}: Apply the requested change only to a
     *         specific rank. The minimum allowed value is '0'. The maximum
     *         allowed value is '10000'.
     * </ul>
     * The default value is an empty {@link Map}.
     *
     * @param options  The new value for {@code options}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AlterTierRequest setOptions(Map<String, String> options) {
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
                return this.name;

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
                this.name = (String)value;
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

        AlterTierRequest that = (AlterTierRequest)obj;

        return ( this.name.equals( that.name )
                 && this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "name" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.name ) );
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
        hashCode = (31 * hashCode) + this.name.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
