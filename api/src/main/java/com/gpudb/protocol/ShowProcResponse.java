/*
 *  This file was autogenerated by the GPUdb schema processor.
 *
 *  DO NOT EDIT DIRECTLY.
 */
package com.gpudb.protocol;

import java.nio.ByteBuffer;
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
 * com.gpudb.GPUdb#showProc(ShowProcRequest)}.
 */
public class ShowProcResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("ShowProcResponse")
            .namespace("com.gpudb")
            .fields()
                .name("procNames").type().array().items().stringType().noDefault()
                .name("executionModes").type().array().items().stringType().noDefault()
                .name("files").type().array().items().map().values().bytesType().noDefault()
                .name("commands").type().array().items().stringType().noDefault()
                .name("args").type().array().items().array().items().stringType().noDefault()
                .name("options").type().array().items().map().values().stringType().noDefault()
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


    /**
     * The execution modes of the procs named in {@code procNames}.
     * Supported values:
     * <ul>
     * </ul>
     * A set of string constants for the parameter {@code executionModes}.
     */
    public static final class ExecutionModes {

        /**
         * Distributed
         */
        public static final String DISTRIBUTED = "distributed";

        /**
         * Nondistributed
         */
        public static final String NONDISTRIBUTED = "nondistributed";

        private ExecutionModes() {  }
    }

    private List<String> procNames;
    private List<String> executionModes;
    private List<Map<String, ByteBuffer>> files;
    private List<String> commands;
    private List<List<String>> args;
    private List<Map<String, String>> options;
    private Map<String, String> info;


    /**
     * Constructs a ShowProcResponse object with default parameters.
     */
    public ShowProcResponse() {
    }

    /**
     * 
     * @return The proc names.
     * 
     */
    public List<String> getProcNames() {
        return procNames;
    }

    /**
     * 
     * @param procNames  The proc names.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public ShowProcResponse setProcNames(List<String> procNames) {
        this.procNames = (procNames == null) ? new ArrayList<String>() : procNames;
        return this;
    }

    /**
     * 
     * @return The execution modes of the procs named in {@code procNames}.
     *         Supported values:
     *         <ul>
     *         </ul>
     * 
     */
    public List<String> getExecutionModes() {
        return executionModes;
    }

    /**
     * 
     * @param executionModes  The execution modes of the procs named in {@code
     *                        procNames}.
     *                        Supported values:
     *                        <ul>
     *                        </ul>
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public ShowProcResponse setExecutionModes(List<String> executionModes) {
        this.executionModes = (executionModes == null) ? new ArrayList<String>() : executionModes;
        return this;
    }

    /**
     * 
     * @return Maps of the files that make up the procs named in {@code
     *         procNames}.
     * 
     */
    public List<Map<String, ByteBuffer>> getFiles() {
        return files;
    }

    /**
     * 
     * @param files  Maps of the files that make up the procs named in {@code
     *               procNames}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public ShowProcResponse setFiles(List<Map<String, ByteBuffer>> files) {
        this.files = (files == null) ? new ArrayList<Map<String, ByteBuffer>>() : files;
        return this;
    }

    /**
     * 
     * @return The commands (excluding arguments) that will be invoked when the
     *         procs named in {@code procNames} are executed.
     * 
     */
    public List<String> getCommands() {
        return commands;
    }

    /**
     * 
     * @param commands  The commands (excluding arguments) that will be invoked
     *                  when the procs named in {@code procNames} are executed.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public ShowProcResponse setCommands(List<String> commands) {
        this.commands = (commands == null) ? new ArrayList<String>() : commands;
        return this;
    }

    /**
     * 
     * @return Arrays of command-line arguments that will be passed to the
     *         procs named in {@code procNames} when executed.
     * 
     */
    public List<List<String>> getArgs() {
        return args;
    }

    /**
     * 
     * @param args  Arrays of command-line arguments that will be passed to the
     *              procs named in {@code procNames} when executed.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public ShowProcResponse setArgs(List<List<String>> args) {
        this.args = (args == null) ? new ArrayList<List<String>>() : args;
        return this;
    }

    /**
     * 
     * @return The optional parameters for the procs named in {@code
     *         procNames}.
     * 
     */
    public List<Map<String, String>> getOptions() {
        return options;
    }

    /**
     * 
     * @param options  The optional parameters for the procs named in {@code
     *                 procNames}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public ShowProcResponse setOptions(List<Map<String, String>> options) {
        this.options = (options == null) ? new ArrayList<Map<String, String>>() : options;
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
    public ShowProcResponse setInfo(Map<String, String> info) {
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
                return this.procNames;

            case 1:
                return this.executionModes;

            case 2:
                return this.files;

            case 3:
                return this.commands;

            case 4:
                return this.args;

            case 5:
                return this.options;

            case 6:
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
                this.procNames = (List<String>)value;
                break;

            case 1:
                this.executionModes = (List<String>)value;
                break;

            case 2:
                this.files = (List<Map<String, ByteBuffer>>)value;
                break;

            case 3:
                this.commands = (List<String>)value;
                break;

            case 4:
                this.args = (List<List<String>>)value;
                break;

            case 5:
                this.options = (List<Map<String, String>>)value;
                break;

            case 6:
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

        ShowProcResponse that = (ShowProcResponse)obj;

        return ( this.procNames.equals( that.procNames )
                 && this.executionModes.equals( that.executionModes )
                 && this.files.equals( that.files )
                 && this.commands.equals( that.commands )
                 && this.args.equals( that.args )
                 && this.options.equals( that.options )
                 && this.info.equals( that.info ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "procNames" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.procNames ) );
        builder.append( ", " );
        builder.append( gd.toString( "executionModes" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.executionModes ) );
        builder.append( ", " );
        builder.append( gd.toString( "files" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.files ) );
        builder.append( ", " );
        builder.append( gd.toString( "commands" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.commands ) );
        builder.append( ", " );
        builder.append( gd.toString( "args" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.args ) );
        builder.append( ", " );
        builder.append( gd.toString( "options" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.options ) );
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
        hashCode = (31 * hashCode) + this.procNames.hashCode();
        hashCode = (31 * hashCode) + this.executionModes.hashCode();
        hashCode = (31 * hashCode) + this.files.hashCode();
        hashCode = (31 * hashCode) + this.commands.hashCode();
        hashCode = (31 * hashCode) + this.args.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }

}
