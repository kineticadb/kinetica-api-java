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
 * com.gpudb.GPUdb#adminAddRanks(AdminAddRanksRequest) GPUdb.adminAddRanks}.
 */
public class AdminAddRanksResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("AdminAddRanksResponse")
            .namespace("com.gpudb")
            .fields()
                .name("addedRanks").type().array().items().stringType().noDefault()
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

    private List<String> addedRanks;
    private Map<String, String> info;

    /**
     * Constructs an AdminAddRanksResponse object with default parameters.
     */
    public AdminAddRanksResponse() {
    }

    /**
     * The number assigned to each added rank, formatted as 'rankN', in the
     * same order as the ranks in {@link
     * com.gpudb.protocol.AdminAddRanksRequest#getHosts() hosts} and {@link
     * com.gpudb.protocol.AdminAddRanksRequest#getConfigParams() configParams}.
     *
     * @return The current value of {@code addedRanks}.
     */
    public List<String> getAddedRanks() {
        return addedRanks;
    }

    /**
     * The number assigned to each added rank, formatted as 'rankN', in the
     * same order as the ranks in {@link
     * com.gpudb.protocol.AdminAddRanksRequest#getHosts() hosts} and {@link
     * com.gpudb.protocol.AdminAddRanksRequest#getConfigParams() configParams}.
     *
     * @param addedRanks  The new value for {@code addedRanks}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public AdminAddRanksResponse setAddedRanks(List<String> addedRanks) {
        this.addedRanks = (addedRanks == null) ? new ArrayList<String>() : addedRanks;
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
    public AdminAddRanksResponse setInfo(Map<String, String> info) {
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
                return this.addedRanks;

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
                this.addedRanks = (List<String>)value;
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

        AdminAddRanksResponse that = (AdminAddRanksResponse)obj;

        return ( this.addedRanks.equals( that.addedRanks )
                 && this.info.equals( that.info ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "addedRanks" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.addedRanks ) );
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
        hashCode = (31 * hashCode) + this.addedRanks.hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }
}
