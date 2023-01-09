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
 * com.gpudb.GPUdb#alterDirectory(AlterDirectoryRequest)}.
 * <p>
 * Alters an existing directory in <a href="../../../../../../tools/kifs/"
 * target="_top">KiFS</a>.
 */
public class AlterDirectoryRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("AlterDirectoryRequest")
            .namespace("com.gpudb")
            .fields()
                .name("directoryName").type().stringType().noDefault()
                .name("directoryUpdatesMap").type().map().values().stringType().noDefault()
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


    /**
     * Map containing the properties of the directory to be altered. Error if
     * empty.
     * <ul>
     *         <li> {@link
     * com.gpudb.protocol.AlterDirectoryRequest.DirectoryUpdatesMap#DATA_LIMIT
     * DATA_LIMIT}: The maximum capacity, in bytes, to apply to the directory.
     * Set to -1 to indicate no upper limit.
     * </ul>
     * A set of string constants for the parameter {@code directoryUpdatesMap}.
     */
    public static final class DirectoryUpdatesMap {

        /**
         * The maximum capacity, in bytes, to apply to the directory. Set to -1
         * to indicate no upper limit.
         */
        public static final String DATA_LIMIT = "data_limit";

        private DirectoryUpdatesMap() {  }
    }

    private String directoryName;
    private Map<String, String> directoryUpdatesMap;
    private Map<String, String> options;


    /**
     * Constructs an AlterDirectoryRequest object with default parameters.
     */
    public AlterDirectoryRequest() {
        directoryName = "";
        directoryUpdatesMap = new LinkedHashMap<>();
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs an AlterDirectoryRequest object with the specified
     * parameters.
     * 
     * @param directoryName  Name of the directory in KiFS to be altered.
     * @param directoryUpdatesMap  Map containing the properties of the
     *                             directory to be altered. Error if empty.
     *                             <ul>
     *                                     <li> {@link
     *                             com.gpudb.protocol.AlterDirectoryRequest.DirectoryUpdatesMap#DATA_LIMIT
     *                             DATA_LIMIT}: The maximum capacity, in bytes,
     *                             to apply to the directory. Set to -1 to
     *                             indicate no upper limit.
     *                             </ul>
     * @param options  Optional parameters.  The default value is an empty
     *                 {@link Map}.
     * 
     */
    public AlterDirectoryRequest(String directoryName, Map<String, String> directoryUpdatesMap, Map<String, String> options) {
        this.directoryName = (directoryName == null) ? "" : directoryName;
        this.directoryUpdatesMap = (directoryUpdatesMap == null) ? new LinkedHashMap<String, String>() : directoryUpdatesMap;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * 
     * @return Name of the directory in KiFS to be altered.
     * 
     */
    public String getDirectoryName() {
        return directoryName;
    }

    /**
     * 
     * @param directoryName  Name of the directory in KiFS to be altered.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public AlterDirectoryRequest setDirectoryName(String directoryName) {
        this.directoryName = (directoryName == null) ? "" : directoryName;
        return this;
    }

    /**
     * 
     * @return Map containing the properties of the directory to be altered.
     *         Error if empty.
     *         <ul>
     *                 <li> {@link
     *         com.gpudb.protocol.AlterDirectoryRequest.DirectoryUpdatesMap#DATA_LIMIT
     *         DATA_LIMIT}: The maximum capacity, in bytes, to apply to the
     *         directory. Set to -1 to indicate no upper limit.
     *         </ul>
     * 
     */
    public Map<String, String> getDirectoryUpdatesMap() {
        return directoryUpdatesMap;
    }

    /**
     * 
     * @param directoryUpdatesMap  Map containing the properties of the
     *                             directory to be altered. Error if empty.
     *                             <ul>
     *                                     <li> {@link
     *                             com.gpudb.protocol.AlterDirectoryRequest.DirectoryUpdatesMap#DATA_LIMIT
     *                             DATA_LIMIT}: The maximum capacity, in bytes,
     *                             to apply to the directory. Set to -1 to
     *                             indicate no upper limit.
     *                             </ul>
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public AlterDirectoryRequest setDirectoryUpdatesMap(Map<String, String> directoryUpdatesMap) {
        this.directoryUpdatesMap = (directoryUpdatesMap == null) ? new LinkedHashMap<String, String>() : directoryUpdatesMap;
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
    public AlterDirectoryRequest setOptions(Map<String, String> options) {
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
                return this.directoryName;

            case 1:
                return this.directoryUpdatesMap;

            case 2:
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
                this.directoryName = (String)value;
                break;

            case 1:
                this.directoryUpdatesMap = (Map<String, String>)value;
                break;

            case 2:
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

        AlterDirectoryRequest that = (AlterDirectoryRequest)obj;

        return ( this.directoryName.equals( that.directoryName )
                 && this.directoryUpdatesMap.equals( that.directoryUpdatesMap )
                 && this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "directoryName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.directoryName ) );
        builder.append( ", " );
        builder.append( gd.toString( "directoryUpdatesMap" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.directoryUpdatesMap ) );
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
        hashCode = (31 * hashCode) + this.directoryName.hashCode();
        hashCode = (31 * hashCode) + this.directoryUpdatesMap.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }

}
