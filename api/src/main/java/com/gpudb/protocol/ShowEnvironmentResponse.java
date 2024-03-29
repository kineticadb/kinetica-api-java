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
 * com.gpudb.GPUdb#showEnvironment(ShowEnvironmentRequest)
 * GPUdb.showEnvironment}.
 */
public class ShowEnvironmentResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("ShowEnvironmentResponse")
            .namespace("com.gpudb")
            .fields()
                .name("environmentNames").type().array().items().stringType().noDefault()
                .name("packages").type().array().items().array().items().stringType().noDefault()
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

    private List<String> environmentNames;
    private List<List<String>> packages;
    private Map<String, String> info;

    /**
     * Constructs a ShowEnvironmentResponse object with default parameters.
     */
    public ShowEnvironmentResponse() {
    }

    /**
     * A list of all credential names.
     *
     * @return The current value of {@code environmentNames}.
     */
    public List<String> getEnvironmentNames() {
        return environmentNames;
    }

    /**
     * A list of all credential names.
     *
     * @param environmentNames  The new value for {@code environmentNames}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public ShowEnvironmentResponse setEnvironmentNames(List<String> environmentNames) {
        this.environmentNames = (environmentNames == null) ? new ArrayList<String>() : environmentNames;
        return this;
    }

    /**
     * Information about the installed packages in the respective environments
     * in {@link #getEnvironmentNames() environmentNames}.
     *
     * @return The current value of {@code packages}.
     */
    public List<List<String>> getPackages() {
        return packages;
    }

    /**
     * Information about the installed packages in the respective environments
     * in {@link #getEnvironmentNames() environmentNames}.
     *
     * @param packages  The new value for {@code packages}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public ShowEnvironmentResponse setPackages(List<List<String>> packages) {
        this.packages = (packages == null) ? new ArrayList<List<String>>() : packages;
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
    public ShowEnvironmentResponse setInfo(Map<String, String> info) {
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
                return this.environmentNames;

            case 1:
                return this.packages;

            case 2:
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
                this.environmentNames = (List<String>)value;
                break;

            case 1:
                this.packages = (List<List<String>>)value;
                break;

            case 2:
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

        ShowEnvironmentResponse that = (ShowEnvironmentResponse)obj;

        return ( this.environmentNames.equals( that.environmentNames )
                 && this.packages.equals( that.packages )
                 && this.info.equals( that.info ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "environmentNames" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.environmentNames ) );
        builder.append( ", " );
        builder.append( gd.toString( "packages" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.packages ) );
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
        hashCode = (31 * hashCode) + this.environmentNames.hashCode();
        hashCode = (31 * hashCode) + this.packages.hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }
}
