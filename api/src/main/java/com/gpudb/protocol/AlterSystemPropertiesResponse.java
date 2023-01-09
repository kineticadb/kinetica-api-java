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
 * com.gpudb.GPUdb#alterSystemProperties(AlterSystemPropertiesRequest)}.
 */
public class AlterSystemPropertiesResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("AlterSystemPropertiesResponse")
            .namespace("com.gpudb")
            .fields()
                .name("updatedPropertiesMap").type().map().values().stringType().noDefault()
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

    private Map<String, String> updatedPropertiesMap;
    private Map<String, String> info;


    /**
     * Constructs an AlterSystemPropertiesResponse object with default
     * parameters.
     */
    public AlterSystemPropertiesResponse() {
    }

    /**
     * 
     * @return Map of values updated; for speed tests, a map of values measured
     *         to the measurement
     * 
     */
    public Map<String, String> getUpdatedPropertiesMap() {
        return updatedPropertiesMap;
    }

    /**
     * 
     * @param updatedPropertiesMap  Map of values updated; for speed tests, a
     *                              map of values measured to the measurement
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public AlterSystemPropertiesResponse setUpdatedPropertiesMap(Map<String, String> updatedPropertiesMap) {
        this.updatedPropertiesMap = (updatedPropertiesMap == null) ? new LinkedHashMap<String, String>() : updatedPropertiesMap;
        return this;
    }

    /**
     * 
     * @return Additional information.
     * 
     */
    public Map<String, String> getInfo() {
        return info;
    }

    /**
     * 
     * @param info  Additional information.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public AlterSystemPropertiesResponse setInfo(Map<String, String> info) {
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
                return this.updatedPropertiesMap;

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
                this.updatedPropertiesMap = (Map<String, String>)value;
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

        AlterSystemPropertiesResponse that = (AlterSystemPropertiesResponse)obj;

        return ( this.updatedPropertiesMap.equals( that.updatedPropertiesMap )
                 && this.info.equals( that.info ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "updatedPropertiesMap" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.updatedPropertiesMap ) );
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
        hashCode = (31 * hashCode) + this.updatedPropertiesMap.hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }

}
