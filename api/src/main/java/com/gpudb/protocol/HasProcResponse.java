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
 * A set of results returned by {@link com.gpudb.GPUdb#hasProc(HasProcRequest)
 * GPUdb.hasProc}.
 */
public class HasProcResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("HasProcResponse")
            .namespace("com.gpudb")
            .fields()
                .name("procName").type().stringType().noDefault()
                .name("procExists").type().booleanType().noDefault()
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
     * A set of string constants for the {@link HasProcResponse} parameter
     * {@link #getProcExists() procExists}.
     * <p>
     * Indicates whether the proc exists or not.
     */
    public static final class ProcExists {
        public static final String TRUE = "true";
        public static final String FALSE = "false";

        private ProcExists() {  }
    }

    private String procName;
    private boolean procExists;
    private Map<String, String> info;

    /**
     * Constructs a HasProcResponse object with default parameters.
     */
    public HasProcResponse() {
    }

    /**
     * Value of {@link com.gpudb.protocol.HasProcRequest#getProcName()
     * procName}
     *
     * @return The current value of {@code procName}.
     */
    public String getProcName() {
        return procName;
    }

    /**
     * Value of {@link com.gpudb.protocol.HasProcRequest#getProcName()
     * procName}
     *
     * @param procName  The new value for {@code procName}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public HasProcResponse setProcName(String procName) {
        this.procName = (procName == null) ? "" : procName;
        return this;
    }

    /**
     * Indicates whether the proc exists or not.
     * Supported values:
     * <ul>
     *     <li>{@link ProcExists#TRUE TRUE}
     *     <li>{@link ProcExists#FALSE FALSE}
     * </ul>
     *
     * @return The current value of {@code procExists}.
     */
    public boolean getProcExists() {
        return procExists;
    }

    /**
     * Indicates whether the proc exists or not.
     * Supported values:
     * <ul>
     *     <li>{@link ProcExists#TRUE TRUE}
     *     <li>{@link ProcExists#FALSE FALSE}
     * </ul>
     *
     * @param procExists  The new value for {@code procExists}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public HasProcResponse setProcExists(boolean procExists) {
        this.procExists = procExists;
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
    public HasProcResponse setInfo(Map<String, String> info) {
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
                return this.procName;

            case 1:
                return this.procExists;

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
                this.procName = (String)value;
                break;

            case 1:
                this.procExists = (Boolean)value;
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

        HasProcResponse that = (HasProcResponse)obj;

        return ( this.procName.equals( that.procName )
                 && ( this.procExists == that.procExists )
                 && this.info.equals( that.info ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "procName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.procName ) );
        builder.append( ", " );
        builder.append( gd.toString( "procExists" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.procExists ) );
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
        hashCode = (31 * hashCode) + this.procName.hashCode();
        hashCode = (31 * hashCode) + ((Boolean)this.procExists).hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }
}
