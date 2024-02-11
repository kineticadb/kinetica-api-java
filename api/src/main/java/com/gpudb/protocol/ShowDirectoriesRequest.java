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
 * com.gpudb.GPUdb#showDirectories(ShowDirectoriesRequest)
 * GPUdb.showDirectories}.
 * <p>
 * Shows information about directories in <a
 * href="../../../../../../tools/kifs/" target="_top">KiFS</a>. Can be used to
 * show a single directory, or all directories.
 */
public class ShowDirectoriesRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("ShowDirectoriesRequest")
            .namespace("com.gpudb")
            .fields()
                .name("directoryName").type().stringType().noDefault()
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

    private String directoryName;
    private Map<String, String> options;

    /**
     * Constructs a ShowDirectoriesRequest object with default parameters.
     */
    public ShowDirectoriesRequest() {
        directoryName = "";
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs a ShowDirectoriesRequest object with the specified
     * parameters.
     *
     * @param directoryName  The KiFS directory name to show. If empty, shows
     *                       all directories. The default value is ''.
     * @param options  Optional parameters. The default value is an empty
     *                 {@link Map}.
     */
    public ShowDirectoriesRequest(String directoryName, Map<String, String> options) {
        this.directoryName = (directoryName == null) ? "" : directoryName;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * The KiFS directory name to show. If empty, shows all directories. The
     * default value is ''.
     *
     * @return The current value of {@code directoryName}.
     */
    public String getDirectoryName() {
        return directoryName;
    }

    /**
     * The KiFS directory name to show. If empty, shows all directories. The
     * default value is ''.
     *
     * @param directoryName  The new value for {@code directoryName}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public ShowDirectoriesRequest setDirectoryName(String directoryName) {
        this.directoryName = (directoryName == null) ? "" : directoryName;
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
    public ShowDirectoriesRequest setOptions(Map<String, String> options) {
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
                return this.directoryName;

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
                this.directoryName = (String)value;
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

        ShowDirectoriesRequest that = (ShowDirectoriesRequest)obj;

        return ( this.directoryName.equals( that.directoryName )
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
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
