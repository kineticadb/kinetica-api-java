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
 * A set of results returned by {@link
 * com.gpudb.GPUdb#showSystemProperties(ShowSystemPropertiesRequest)
 * GPUdb.showSystemProperties}.
 */
public class ShowSystemPropertiesResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("ShowSystemPropertiesResponse")
            .namespace("com.gpudb")
            .fields()
                .name("propertyMap").type().map().values().stringType().noDefault()
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

    /**
     * A set of string constants for the {@link ShowSystemPropertiesResponse}
     * parameter {@link #getPropertyMap() propertyMap}.
     * <p>
     * A map of server configuration parameters and version information.
     */
    public static final class PropertyMap {
        /**
         * Boolean value indicating whether the system is configured for
         * multi-head ingestion.
         * Supported values:
         * <ul>
         *     <li>{@link PropertyMap#TRUE TRUE}: Indicates that the system is
         *         configured for multi-head ingestion.
         *     <li>{@link PropertyMap#FALSE FALSE}: Indicates that the system
         *         is NOT configured for multi-head ingestion.
         * </ul>
         */
        public static final String CONF_ENABLE_WORKER_HTTP_SERVERS = "conf.enable_worker_http_servers";

        /**
         * Indicates that the system is configured for multi-head ingestion.
         */
        public static final String TRUE = "TRUE";

        /**
         * Indicates that the system is NOT configured for multi-head
         * ingestion.
         */
        public static final String FALSE = "FALSE";

        /**
         * Semicolon (';') separated string of IP addresses of all the
         * ingestion-enabled worker heads of the system.
         */
        public static final String CONF_WORKER_HTTP_SERVER_IPS = "conf.worker_http_server_ips";

        /**
         * Semicolon (';') separated string of the port numbers of all the
         * ingestion-enabled worker ranks of the system.
         */
        public static final String CONF_WORKER_HTTP_SERVER_PORTS = "conf.worker_http_server_ports";

        /**
         * The host manager port number (an integer value).
         */
        public static final String CONF_HM_HTTP_PORT = "conf.hm_http_port";

        /**
         * Flag indicating whether high availability (HA) is set up (a boolean
         * value).
         */
        public static final String CONF_ENABLE_HA = "conf.enable_ha";

        /**
         * A comma-separated string of high availability (HA) ring node URLs.
         * If HA is not set up, then an empty string.
         */
        public static final String CONF_HA_RING_HEAD_NODES = "conf.ha_ring_head_nodes";

        private PropertyMap() {  }
    }

    private Map<String, String> propertyMap;
    private Map<String, String> info;

    /**
     * Constructs a ShowSystemPropertiesResponse object with default
     * parameters.
     */
    public ShowSystemPropertiesResponse() {
    }

    /**
     * A map of server configuration parameters and version information.
     * <ul>
     *     <li>{@link PropertyMap#CONF_ENABLE_WORKER_HTTP_SERVERS
     *         CONF_ENABLE_WORKER_HTTP_SERVERS}: Boolean value indicating
     *         whether the system is configured for multi-head ingestion.
     *         Supported values:
     *         <ul>
     *             <li>{@link PropertyMap#TRUE TRUE}: Indicates that the system
     *                 is configured for multi-head ingestion.
     *             <li>{@link PropertyMap#FALSE FALSE}: Indicates that the
     *                 system is NOT configured for multi-head ingestion.
     *         </ul>
     *     <li>{@link PropertyMap#CONF_WORKER_HTTP_SERVER_IPS
     *         CONF_WORKER_HTTP_SERVER_IPS}: Semicolon (';') separated string
     *         of IP addresses of all the ingestion-enabled worker heads of the
     *         system.
     *     <li>{@link PropertyMap#CONF_WORKER_HTTP_SERVER_PORTS
     *         CONF_WORKER_HTTP_SERVER_PORTS}: Semicolon (';') separated string
     *         of the port numbers of all the ingestion-enabled worker ranks of
     *         the system.
     *     <li>{@link PropertyMap#CONF_HM_HTTP_PORT CONF_HM_HTTP_PORT}: The
     *         host manager port number (an integer value).
     *     <li>{@link PropertyMap#CONF_ENABLE_HA CONF_ENABLE_HA}: Flag
     *         indicating whether high availability (HA) is set up (a boolean
     *         value).
     *     <li>{@link PropertyMap#CONF_HA_RING_HEAD_NODES
     *         CONF_HA_RING_HEAD_NODES}: A comma-separated string of high
     *         availability (HA) ring node URLs.  If HA is not set up, then an
     *         empty string.
     * </ul>
     *
     * @return The current value of {@code propertyMap}.
     */
    public Map<String, String> getPropertyMap() {
        return propertyMap;
    }

    /**
     * A map of server configuration parameters and version information.
     * <ul>
     *     <li>{@link PropertyMap#CONF_ENABLE_WORKER_HTTP_SERVERS
     *         CONF_ENABLE_WORKER_HTTP_SERVERS}: Boolean value indicating
     *         whether the system is configured for multi-head ingestion.
     *         Supported values:
     *         <ul>
     *             <li>{@link PropertyMap#TRUE TRUE}: Indicates that the system
     *                 is configured for multi-head ingestion.
     *             <li>{@link PropertyMap#FALSE FALSE}: Indicates that the
     *                 system is NOT configured for multi-head ingestion.
     *         </ul>
     *     <li>{@link PropertyMap#CONF_WORKER_HTTP_SERVER_IPS
     *         CONF_WORKER_HTTP_SERVER_IPS}: Semicolon (';') separated string
     *         of IP addresses of all the ingestion-enabled worker heads of the
     *         system.
     *     <li>{@link PropertyMap#CONF_WORKER_HTTP_SERVER_PORTS
     *         CONF_WORKER_HTTP_SERVER_PORTS}: Semicolon (';') separated string
     *         of the port numbers of all the ingestion-enabled worker ranks of
     *         the system.
     *     <li>{@link PropertyMap#CONF_HM_HTTP_PORT CONF_HM_HTTP_PORT}: The
     *         host manager port number (an integer value).
     *     <li>{@link PropertyMap#CONF_ENABLE_HA CONF_ENABLE_HA}: Flag
     *         indicating whether high availability (HA) is set up (a boolean
     *         value).
     *     <li>{@link PropertyMap#CONF_HA_RING_HEAD_NODES
     *         CONF_HA_RING_HEAD_NODES}: A comma-separated string of high
     *         availability (HA) ring node URLs.  If HA is not set up, then an
     *         empty string.
     * </ul>
     *
     * @param propertyMap  The new value for {@code propertyMap}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public ShowSystemPropertiesResponse setPropertyMap(Map<String, String> propertyMap) {
        this.propertyMap = (propertyMap == null) ? new LinkedHashMap<String, String>() : propertyMap;
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
    public ShowSystemPropertiesResponse setInfo(Map<String, String> info) {
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
                return this.propertyMap;

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
                this.propertyMap = (Map<String, String>)value;
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

        ShowSystemPropertiesResponse that = (ShowSystemPropertiesResponse)obj;

        return ( this.propertyMap.equals( that.propertyMap )
                 && this.info.equals( that.info ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "propertyMap" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.propertyMap ) );
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
        hashCode = (31 * hashCode) + this.propertyMap.hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }
}
