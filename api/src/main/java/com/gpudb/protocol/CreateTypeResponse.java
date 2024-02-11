/*
 *  This file was autogenerated by the Kinetica schema processor.
 *
 *  DO NOT EDIT DIRECTLY.
 */
package com.gpudb.protocol;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;

/**
 * A set of results returned by {@link
 * com.gpudb.GPUdb#createType(CreateTypeRequest) GPUdb.createType}.
 */
public class CreateTypeResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("CreateTypeResponse")
            .namespace("com.gpudb")
            .fields()
                .name("typeId").type().stringType().noDefault()
                .name("typeDefinition").type().stringType().noDefault()
                .name("label").type().stringType().noDefault()
                .name("properties").type().map().values().array().items().stringType().noDefault()
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

    private String typeId;
    private String typeDefinition;
    private String label;
    private Map<String, List<String>> properties;
    private Map<String, String> info;

    /**
     * Constructs a CreateTypeResponse object with default parameters.
     */
    public CreateTypeResponse() {
    }

    /**
     * An identifier representing the created type. This type_id can be used in
     * subsequent calls to {@link
     * com.gpudb.GPUdb#createTable(CreateTableRequest) create a table}
     *
     * @return The current value of {@code typeId}.
     */
    public String getTypeId() {
        return typeId;
    }

    /**
     * An identifier representing the created type. This type_id can be used in
     * subsequent calls to {@link
     * com.gpudb.GPUdb#createTable(CreateTableRequest) create a table}
     *
     * @param typeId  The new value for {@code typeId}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public CreateTypeResponse setTypeId(String typeId) {
        this.typeId = (typeId == null) ? "" : typeId;
        return this;
    }

    /**
     * Value of {@link com.gpudb.protocol.CreateTypeRequest#getTypeDefinition()
     * typeDefinition}.
     *
     * @return The current value of {@code typeDefinition}.
     */
    public String getTypeDefinition() {
        return typeDefinition;
    }

    /**
     * Value of {@link com.gpudb.protocol.CreateTypeRequest#getTypeDefinition()
     * typeDefinition}.
     *
     * @param typeDefinition  The new value for {@code typeDefinition}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public CreateTypeResponse setTypeDefinition(String typeDefinition) {
        this.typeDefinition = (typeDefinition == null) ? "" : typeDefinition;
        return this;
    }

    /**
     * Value of {@link com.gpudb.protocol.CreateTypeRequest#getLabel() label}.
     *
     * @return The current value of {@code label}.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Value of {@link com.gpudb.protocol.CreateTypeRequest#getLabel() label}.
     *
     * @param label  The new value for {@code label}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public CreateTypeResponse setLabel(String label) {
        this.label = (label == null) ? "" : label;
        return this;
    }

    /**
     * Value of {@link com.gpudb.protocol.CreateTypeRequest#getProperties()
     * properties}.
     *
     * @return The current value of {@code properties}.
     */
    public Map<String, List<String>> getProperties() {
        return properties;
    }

    /**
     * Value of {@link com.gpudb.protocol.CreateTypeRequest#getProperties()
     * properties}.
     *
     * @param properties  The new value for {@code properties}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public CreateTypeResponse setProperties(Map<String, List<String>> properties) {
        this.properties = (properties == null) ? new LinkedHashMap<String, List<String>>() : properties;
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
    public CreateTypeResponse setInfo(Map<String, String> info) {
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
                return this.typeId;

            case 1:
                return this.typeDefinition;

            case 2:
                return this.label;

            case 3:
                return this.properties;

            case 4:
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
                this.typeId = (String)value;
                break;

            case 1:
                this.typeDefinition = (String)value;
                break;

            case 2:
                this.label = (String)value;
                break;

            case 3:
                this.properties = (Map<String, List<String>>)value;
                break;

            case 4:
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

        CreateTypeResponse that = (CreateTypeResponse)obj;

        return ( this.typeId.equals( that.typeId )
                 && this.typeDefinition.equals( that.typeDefinition )
                 && this.label.equals( that.label )
                 && this.properties.equals( that.properties )
                 && this.info.equals( that.info ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "typeId" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.typeId ) );
        builder.append( ", " );
        builder.append( gd.toString( "typeDefinition" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.typeDefinition ) );
        builder.append( ", " );
        builder.append( gd.toString( "label" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.label ) );
        builder.append( ", " );
        builder.append( gd.toString( "properties" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.properties ) );
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
        hashCode = (31 * hashCode) + this.typeId.hashCode();
        hashCode = (31 * hashCode) + this.typeDefinition.hashCode();
        hashCode = (31 * hashCode) + this.label.hashCode();
        hashCode = (31 * hashCode) + this.properties.hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }
}
