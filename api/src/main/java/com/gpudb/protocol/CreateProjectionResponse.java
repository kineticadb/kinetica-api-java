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
 * com.gpudb.GPUdb#createProjection(CreateProjectionRequest)
 * GPUdb.createProjection}.
 */
public class CreateProjectionResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("CreateProjectionResponse")
            .namespace("com.gpudb")
            .fields()
                .name("projectionName").type().stringType().noDefault()
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
     * A set of string constants for the {@link CreateProjectionResponse}
     * parameter {@link #getInfo() info}.
     * <p>
     * Additional information.
     */
    public static final class Info {
        /**
         * Number of records in the final table
         */
        public static final String COUNT = "count";

        /**
         * The fully qualified name of the projection (i.e.&nbsp;including the
         * schema).
         */
        public static final String QUALIFIED_PROJECTION_NAME = "qualified_projection_name";

        private Info() {  }
    }

    private String projectionName;
    private Map<String, String> info;

    /**
     * Constructs a CreateProjectionResponse object with default parameters.
     */
    public CreateProjectionResponse() {
    }

    /**
     * Value of {@link
     * com.gpudb.protocol.CreateProjectionRequest#getProjectionName()
     * projectionName}.
     *
     * @return The current value of {@code projectionName}.
     */
    public String getProjectionName() {
        return projectionName;
    }

    /**
     * Value of {@link
     * com.gpudb.protocol.CreateProjectionRequest#getProjectionName()
     * projectionName}.
     *
     * @param projectionName  The new value for {@code projectionName}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public CreateProjectionResponse setProjectionName(String projectionName) {
        this.projectionName = (projectionName == null) ? "" : projectionName;
        return this;
    }

    /**
     * Additional information.
     * <ul>
     *     <li>{@link Info#COUNT COUNT}: Number of records in the final table
     *     <li>{@link Info#QUALIFIED_PROJECTION_NAME
     *         QUALIFIED_PROJECTION_NAME}: The fully qualified name of the
     *         projection (i.e. including the schema).
     * </ul>
     * The default value is an empty {@link Map}.
     *
     * @return The current value of {@code info}.
     */
    public Map<String, String> getInfo() {
        return info;
    }

    /**
     * Additional information.
     * <ul>
     *     <li>{@link Info#COUNT COUNT}: Number of records in the final table
     *     <li>{@link Info#QUALIFIED_PROJECTION_NAME
     *         QUALIFIED_PROJECTION_NAME}: The fully qualified name of the
     *         projection (i.e. including the schema).
     * </ul>
     * The default value is an empty {@link Map}.
     *
     * @param info  The new value for {@code info}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public CreateProjectionResponse setInfo(Map<String, String> info) {
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
                return this.projectionName;

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
                this.projectionName = (String)value;
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

        CreateProjectionResponse that = (CreateProjectionResponse)obj;

        return ( this.projectionName.equals( that.projectionName )
                 && this.info.equals( that.info ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "projectionName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.projectionName ) );
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
        hashCode = (31 * hashCode) + this.projectionName.hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }
}
