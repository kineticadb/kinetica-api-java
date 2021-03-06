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
 * A set of parameters for {@link
 * com.gpudb.GPUdb#adminShowAlerts(AdminShowAlertsRequest)}.
 * <p>
 * Requests a list of the most recent alerts.
 * Returns lists of alert data, including timestamp and type.
 */
public class AdminShowAlertsRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("AdminShowAlertsRequest")
            .namespace("com.gpudb")
            .fields()
                .name("numAlerts").type().intType().noDefault()
                .name("options").type().map().values().stringType().noDefault()
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

    private int numAlerts;
    private Map<String, String> options;


    /**
     * Constructs an AdminShowAlertsRequest object with default parameters.
     */
    public AdminShowAlertsRequest() {
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs an AdminShowAlertsRequest object with the specified
     * parameters.
     * 
     * @param numAlerts  Number of most recent alerts to request. The response
     *                   will include up to {@code numAlerts} depending on how
     *                   many alerts there are in the system. A value of 0
     *                   returns all stored alerts.
     * @param options  Optional parameters.  The default value is an empty
     *                 {@link Map}.
     * 
     */
    public AdminShowAlertsRequest(int numAlerts, Map<String, String> options) {
        this.numAlerts = numAlerts;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * 
     * @return Number of most recent alerts to request. The response will
     *         include up to {@code numAlerts} depending on how many alerts
     *         there are in the system. A value of 0 returns all stored alerts.
     * 
     */
    public int getNumAlerts() {
        return numAlerts;
    }

    /**
     * 
     * @param numAlerts  Number of most recent alerts to request. The response
     *                   will include up to {@code numAlerts} depending on how
     *                   many alerts there are in the system. A value of 0
     *                   returns all stored alerts.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public AdminShowAlertsRequest setNumAlerts(int numAlerts) {
        this.numAlerts = numAlerts;
        return this;
    }

    /**
     * 
     * @return Optional parameters.  The default value is an empty {@link Map}.
     * 
     */
    public Map<String, String> getOptions() {
        return options;
    }

    /**
     * 
     * @param options  Optional parameters.  The default value is an empty
     *                 {@link Map}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public AdminShowAlertsRequest setOptions(Map<String, String> options) {
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
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
                return this.numAlerts;

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
     * 
     */
    @Override
    @SuppressWarnings("unchecked")
    public void put(int index, Object value) {
        switch (index) {
            case 0:
                this.numAlerts = (Integer)value;
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

        AdminShowAlertsRequest that = (AdminShowAlertsRequest)obj;

        return ( ( this.numAlerts == that.numAlerts )
                 && this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "numAlerts" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.numAlerts ) );
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
        hashCode = (31 * hashCode) + this.numAlerts;
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }

}
