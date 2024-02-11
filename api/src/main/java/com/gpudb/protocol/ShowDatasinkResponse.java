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
 * com.gpudb.GPUdb#showDatasink(ShowDatasinkRequest) GPUdb.showDatasink}.
 */
public class ShowDatasinkResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("ShowDatasinkResponse")
            .namespace("com.gpudb")
            .fields()
                .name("datasinkNames").type().array().items().stringType().noDefault()
                .name("destinationTypes").type().array().items().stringType().noDefault()
                .name("additionalInfo").type().array().items().map().values().stringType().noDefault()
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
     * A set of string constants for the {@link ShowDatasinkResponse} parameter
     * {@link #getAdditionalInfo() additionalInfo}.
     * <p>
     * Additional information about the respective data sinks in {@link
     * #getDatasinkNames() datasinkNames}.
     */
    public static final class AdditionalInfo {
        /**
         * Destination for the output data in 'destination_type://path[:port]'
         * format
         */
        public static final String DESTINATION = "destination";

        /**
         * Kafka topic if the data sink type is a Kafka broker
         */
        public static final String KAFKA_TOPIC_NAME = "kafka_topic_name";

        /**
         * Name of the remote system user
         */
        public static final String USER_NAME = "user_name";

        private AdditionalInfo() {  }
    }

    private List<String> datasinkNames;
    private List<String> destinationTypes;
    private List<Map<String, String>> additionalInfo;
    private Map<String, String> info;

    /**
     * Constructs a ShowDatasinkResponse object with default parameters.
     */
    public ShowDatasinkResponse() {
    }

    /**
     * The data sink names.
     *
     * @return The current value of {@code datasinkNames}.
     */
    public List<String> getDatasinkNames() {
        return datasinkNames;
    }

    /**
     * The data sink names.
     *
     * @param datasinkNames  The new value for {@code datasinkNames}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public ShowDatasinkResponse setDatasinkNames(List<String> datasinkNames) {
        this.datasinkNames = (datasinkNames == null) ? new ArrayList<String>() : datasinkNames;
        return this;
    }

    /**
     * The destination type of the data sinks named in {@link
     * #getDatasinkNames() datasinkNames}.
     *
     * @return The current value of {@code destinationTypes}.
     */
    public List<String> getDestinationTypes() {
        return destinationTypes;
    }

    /**
     * The destination type of the data sinks named in {@link
     * #getDatasinkNames() datasinkNames}.
     *
     * @param destinationTypes  The new value for {@code destinationTypes}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public ShowDatasinkResponse setDestinationTypes(List<String> destinationTypes) {
        this.destinationTypes = (destinationTypes == null) ? new ArrayList<String>() : destinationTypes;
        return this;
    }

    /**
     * Additional information about the respective data sinks in {@link
     * #getDatasinkNames() datasinkNames}.
     * <ul>
     *     <li>{@link AdditionalInfo#DESTINATION DESTINATION}: Destination for
     *         the output data in 'destination_type://path[:port]' format
     *     <li>{@link AdditionalInfo#KAFKA_TOPIC_NAME KAFKA_TOPIC_NAME}: Kafka
     *         topic if the data sink type is a Kafka broker
     *     <li>{@link AdditionalInfo#USER_NAME USER_NAME}: Name of the remote
     *         system user
     * </ul>
     *
     * @return The current value of {@code additionalInfo}.
     */
    public List<Map<String, String>> getAdditionalInfo() {
        return additionalInfo;
    }

    /**
     * Additional information about the respective data sinks in {@link
     * #getDatasinkNames() datasinkNames}.
     * <ul>
     *     <li>{@link AdditionalInfo#DESTINATION DESTINATION}: Destination for
     *         the output data in 'destination_type://path[:port]' format
     *     <li>{@link AdditionalInfo#KAFKA_TOPIC_NAME KAFKA_TOPIC_NAME}: Kafka
     *         topic if the data sink type is a Kafka broker
     *     <li>{@link AdditionalInfo#USER_NAME USER_NAME}: Name of the remote
     *         system user
     * </ul>
     *
     * @param additionalInfo  The new value for {@code additionalInfo}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public ShowDatasinkResponse setAdditionalInfo(List<Map<String, String>> additionalInfo) {
        this.additionalInfo = (additionalInfo == null) ? new ArrayList<Map<String, String>>() : additionalInfo;
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
    public ShowDatasinkResponse setInfo(Map<String, String> info) {
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
                return this.datasinkNames;

            case 1:
                return this.destinationTypes;

            case 2:
                return this.additionalInfo;

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
                this.datasinkNames = (List<String>)value;
                break;

            case 1:
                this.destinationTypes = (List<String>)value;
                break;

            case 2:
                this.additionalInfo = (List<Map<String, String>>)value;
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

        ShowDatasinkResponse that = (ShowDatasinkResponse)obj;

        return ( this.datasinkNames.equals( that.datasinkNames )
                 && this.destinationTypes.equals( that.destinationTypes )
                 && this.additionalInfo.equals( that.additionalInfo )
                 && this.info.equals( that.info ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "datasinkNames" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.datasinkNames ) );
        builder.append( ", " );
        builder.append( gd.toString( "destinationTypes" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.destinationTypes ) );
        builder.append( ", " );
        builder.append( gd.toString( "additionalInfo" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.additionalInfo ) );
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
        hashCode = (31 * hashCode) + this.datasinkNames.hashCode();
        hashCode = (31 * hashCode) + this.destinationTypes.hashCode();
        hashCode = (31 * hashCode) + this.additionalInfo.hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }
}
