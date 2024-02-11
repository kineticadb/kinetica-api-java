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
 * com.gpudb.GPUdb#createEnvironment(CreateEnvironmentRequest)
 * GPUdb.createEnvironment}.
 * <p>
 * Creates a new environment which can be used by <a
 * href="../../../../../../concepts/udf/" target="_top">user-defined
 * functions</a> (UDF).
 */
public class CreateEnvironmentRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("CreateEnvironmentRequest")
            .namespace("com.gpudb")
            .fields()
                .name("environmentName").type().stringType().noDefault()
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

    private String environmentName;
    private Map<String, String> options;

    /**
     * Constructs a CreateEnvironmentRequest object with default parameters.
     */
    public CreateEnvironmentRequest() {
        environmentName = "";
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs a CreateEnvironmentRequest object with the specified
     * parameters.
     *
     * @param environmentName  Name of the environment to be created.
     * @param options  Optional parameters. The default value is an empty
     *                 {@link Map}.
     */
    public CreateEnvironmentRequest(String environmentName, Map<String, String> options) {
        this.environmentName = (environmentName == null) ? "" : environmentName;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * Name of the environment to be created.
     *
     * @return The current value of {@code environmentName}.
     */
    public String getEnvironmentName() {
        return environmentName;
    }

    /**
     * Name of the environment to be created.
     *
     * @param environmentName  The new value for {@code environmentName}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public CreateEnvironmentRequest setEnvironmentName(String environmentName) {
        this.environmentName = (environmentName == null) ? "" : environmentName;
        return this;
    }

    /**
     * Optional parameters. The default value is an empty {@link Map}.
     *
     * @return The current value of {@code options}.
     */
    public Map<String, String> getOptions() {
        return options;
    }

    /**
     * Optional parameters. The default value is an empty {@link Map}.
     *
     * @param options  The new value for {@code options}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public CreateEnvironmentRequest setOptions(Map<String, String> options) {
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
                return this.environmentName;

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
                this.environmentName = (String)value;
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

        CreateEnvironmentRequest that = (CreateEnvironmentRequest)obj;

        return ( this.environmentName.equals( that.environmentName )
                 && this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "environmentName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.environmentName ) );
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
        hashCode = (31 * hashCode) + this.environmentName.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
