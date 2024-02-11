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
 * com.gpudb.GPUdb#adminRemoveHost(AdminRemoveHostRequest)
 * GPUdb.adminRemoveHost}.
 * <p>
 * Removes a host from an existing cluster. If the host to be removed has any
 * ranks running on it, the ranks must be removed using {@link
 * com.gpudb.GPUdb#adminRemoveRanks(AdminRemoveRanksRequest)
 * GPUdb.adminRemoveRanks} or manually switched over to a new host using {@link
 * com.gpudb.GPUdb#adminSwitchover(AdminSwitchoverRequest)
 * GPUdb.adminSwitchover} prior to host removal. If the host to be removed has
 * the graph server or SQL planner running on it, these must be manually
 * switched over to a new host using {@link
 * com.gpudb.GPUdb#adminSwitchover(AdminSwitchoverRequest)
 * GPUdb.adminSwitchover}.
 */
public class AdminRemoveHostRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("AdminRemoveHostRequest")
            .namespace("com.gpudb")
            .fields()
                .name("host").type().stringType().noDefault()
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
     * A set of string constants for the {@link AdminRemoveHostRequest}
     * parameter {@link #getOptions() options}.
     * <p>
     * Optional parameters.
     */
    public static final class Options {
        /**
         * If set to {@link Options#TRUE TRUE}, only validation checks will be
         * performed. No host is removed.
         * Supported values:
         * <ul>
         *     <li>{@link Options#TRUE TRUE}
         *     <li>{@link Options#FALSE FALSE}
         * </ul>
         * The default value is {@link Options#FALSE FALSE}.
         */
        public static final String DRY_RUN = "dry_run";

        public static final String TRUE = "true";
        public static final String FALSE = "false";

        private Options() {  }
    }

    private String host;
    private Map<String, String> options;

    /**
     * Constructs an AdminRemoveHostRequest object with default parameters.
     */
    public AdminRemoveHostRequest() {
        host = "";
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs an AdminRemoveHostRequest object with the specified
     * parameters.
     *
     * @param host  Identifies the host this applies to. Can be the host
     *              address, or formatted as 'hostN' where N is the host number
     *              as specified in gpudb.conf
     * @param options  Optional parameters.
     *                 <ul>
     *                     <li>{@link Options#DRY_RUN DRY_RUN}: If set to
     *                         {@link Options#TRUE TRUE}, only validation
     *                         checks will be performed. No host is removed.
     *                         Supported values:
     *                         <ul>
     *                             <li>{@link Options#TRUE TRUE}
     *                             <li>{@link Options#FALSE FALSE}
     *                         </ul>
     *                         The default value is {@link Options#FALSE
     *                         FALSE}.
     *                 </ul>
     *                 The default value is an empty {@link Map}.
     */
    public AdminRemoveHostRequest(String host, Map<String, String> options) {
        this.host = (host == null) ? "" : host;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * Identifies the host this applies to. Can be the host address, or
     * formatted as 'hostN' where N is the host number as specified in
     * gpudb.conf
     *
     * @return The current value of {@code host}.
     */
    public String getHost() {
        return host;
    }

    /**
     * Identifies the host this applies to. Can be the host address, or
     * formatted as 'hostN' where N is the host number as specified in
     * gpudb.conf
     *
     * @param host  The new value for {@code host}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AdminRemoveHostRequest setHost(String host) {
        this.host = (host == null) ? "" : host;
        return this;
    }

    /**
     * Optional parameters.
     * <ul>
     *     <li>{@link Options#DRY_RUN DRY_RUN}: If set to {@link Options#TRUE
     *         TRUE}, only validation checks will be performed. No host is
     *         removed.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
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
     *     <li>{@link Options#DRY_RUN DRY_RUN}: If set to {@link Options#TRUE
     *         TRUE}, only validation checks will be performed. No host is
     *         removed.
     *         Supported values:
     *         <ul>
     *             <li>{@link Options#TRUE TRUE}
     *             <li>{@link Options#FALSE FALSE}
     *         </ul>
     *         The default value is {@link Options#FALSE FALSE}.
     * </ul>
     * The default value is an empty {@link Map}.
     *
     * @param options  The new value for {@code options}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AdminRemoveHostRequest setOptions(Map<String, String> options) {
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
                return this.host;

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
                this.host = (String)value;
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

        AdminRemoveHostRequest that = (AdminRemoveHostRequest)obj;

        return ( this.host.equals( that.host )
                 && this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "host" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.host ) );
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
        hashCode = (31 * hashCode) + this.host.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}