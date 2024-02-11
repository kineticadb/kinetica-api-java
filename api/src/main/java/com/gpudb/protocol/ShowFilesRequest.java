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
 * A set of parameters for {@link com.gpudb.GPUdb#showFiles(ShowFilesRequest)
 * GPUdb.showFiles}.
 * <p>
 * Shows information about files in <a href="../../../../../../tools/kifs/"
 * target="_top">KiFS</a>. Can be used for individual files, or to show all
 * files in a given directory.
 */
public class ShowFilesRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("ShowFilesRequest")
            .namespace("com.gpudb")
            .fields()
                .name("paths").type().array().items().stringType().noDefault()
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

    private List<String> paths;
    private Map<String, String> options;

    /**
     * Constructs a ShowFilesRequest object with default parameters.
     */
    public ShowFilesRequest() {
        paths = new ArrayList<>();
        options = new LinkedHashMap<>();
    }

    /**
     * Constructs a ShowFilesRequest object with the specified parameters.
     *
     * @param paths  File paths to show. Each path can be a KiFS directory
     *               name, or a full path to a KiFS file. File paths may
     *               contain wildcard characters after the KiFS directory
     *               delimeter.  Accepted wildcard characters are asterisk (*)
     *               to represent any string of zero or more characters, and
     *               question mark (?) to indicate a single character.
     * @param options  Optional parameters. The default value is an empty
     *                 {@link Map}.
     */
    public ShowFilesRequest(List<String> paths, Map<String, String> options) {
        this.paths = (paths == null) ? new ArrayList<String>() : paths;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    /**
     * File paths to show. Each path can be a KiFS directory name, or a full
     * path to a KiFS file. File paths may contain wildcard characters after
     * the KiFS directory delimeter.
     * <p>
     * Accepted wildcard characters are asterisk (*) to represent any string of
     * zero or more characters, and question mark (?) to indicate a single
     * character.
     *
     * @return The current value of {@code paths}.
     */
    public List<String> getPaths() {
        return paths;
    }

    /**
     * File paths to show. Each path can be a KiFS directory name, or a full
     * path to a KiFS file. File paths may contain wildcard characters after
     * the KiFS directory delimeter.
     * <p>
     * Accepted wildcard characters are asterisk (*) to represent any string of
     * zero or more characters, and question mark (?) to indicate a single
     * character.
     *
     * @param paths  The new value for {@code paths}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public ShowFilesRequest setPaths(List<String> paths) {
        this.paths = (paths == null) ? new ArrayList<String>() : paths;
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
    public ShowFilesRequest setOptions(Map<String, String> options) {
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
                return this.paths;

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
                this.paths = (List<String>)value;
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

        ShowFilesRequest that = (ShowFilesRequest)obj;

        return ( this.paths.equals( that.paths )
                 && this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "paths" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.paths ) );
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
        hashCode = (31 * hashCode) + this.paths.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
