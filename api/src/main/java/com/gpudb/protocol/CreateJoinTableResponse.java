/*
 *  This file was autogenerated by the GPUdb schema processor.
 *
 *  DO NOT EDIT DIRECTLY.
 */
package com.gpudb.protocol;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;


/**
 * A set of results returned by {@link
 * com.gpudb.GPUdb#createJoinTable(CreateJoinTableRequest)}.
 */
public class CreateJoinTableResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("CreateJoinTableResponse")
            .namespace("com.gpudb")
            .fields()
                .name("joinTableName").type().stringType().noDefault()
                .name("count").type().longType().noDefault()
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

    private String joinTableName;
    private long count;


    /**
     * Constructs a CreateJoinTableResponse object with default parameters.
     */
    public CreateJoinTableResponse() {
    }

    /**
     * 
     * @return Value of {@code joinTableName}.
     * 
     */
    public String getJoinTableName() {
        return joinTableName;
    }

    /**
     * 
     * @param joinTableName  Value of {@code joinTableName}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public CreateJoinTableResponse setJoinTableName(String joinTableName) {
        this.joinTableName = (joinTableName == null) ? "" : joinTableName;
        return this;
    }

    /**
     * 
     * @return The number of records in the join table filtered by the given
     *         select expression.
     * 
     */
    public long getCount() {
        return count;
    }

    /**
     * 
     * @param count  The number of records in the join table filtered by the
     *               given select expression.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public CreateJoinTableResponse setCount(long count) {
        this.count = count;
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
                return this.joinTableName;

            case 1:
                return this.count;

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
                this.joinTableName = (String)value;
                break;

            case 1:
                this.count = (Long)value;
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

        CreateJoinTableResponse that = (CreateJoinTableResponse)obj;

        return ( this.joinTableName.equals( that.joinTableName )
                 && ( this.count == that.count ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "joinTableName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.joinTableName ) );
        builder.append( ", " );
        builder.append( gd.toString( "count" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.count ) );
        builder.append( "}" );

        return builder.toString();
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = (31 * hashCode) + this.joinTableName.hashCode();
        hashCode = (31 * hashCode) + ((Long)this.count).hashCode();
        return hashCode;
    }

}
