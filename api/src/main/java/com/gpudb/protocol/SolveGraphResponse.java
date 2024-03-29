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
 * com.gpudb.GPUdb#solveGraph(SolveGraphRequest) GPUdb.solveGraph}.
 */
public class SolveGraphResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("SolveGraphResponse")
            .namespace("com.gpudb")
            .fields()
                .name("result").type().booleanType().noDefault()
                .name("resultPerDestinationNode").type().array().items().floatType().noDefault()
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

    private boolean result;
    private List<Float> resultPerDestinationNode;
    private Map<String, String> info;

    /**
     * Constructs a SolveGraphResponse object with default parameters.
     */
    public SolveGraphResponse() {
    }

    /**
     * Indicates a successful solution on all servers.
     *
     * @return The current value of {@code result}.
     */
    public boolean getResult() {
        return result;
    }

    /**
     * Indicates a successful solution on all servers.
     *
     * @param result  The new value for {@code result}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public SolveGraphResponse setResult(boolean result) {
        this.result = result;
        return this;
    }

    /**
     * Cost or Pagerank (based on solver type) for each destination node
     * requested. Only populated if 'export_solve_results' option is set to
     * true.
     *
     * @return The current value of {@code resultPerDestinationNode}.
     */
    public List<Float> getResultPerDestinationNode() {
        return resultPerDestinationNode;
    }

    /**
     * Cost or Pagerank (based on solver type) for each destination node
     * requested. Only populated if 'export_solve_results' option is set to
     * true.
     *
     * @param resultPerDestinationNode  The new value for {@code
     *                                  resultPerDestinationNode}.
     *
     * @return {@code this} to mimic the builder pattern.
     */
    public SolveGraphResponse setResultPerDestinationNode(List<Float> resultPerDestinationNode) {
        this.resultPerDestinationNode = (resultPerDestinationNode == null) ? new ArrayList<Float>() : resultPerDestinationNode;
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
    public SolveGraphResponse setInfo(Map<String, String> info) {
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
                return this.result;

            case 1:
                return this.resultPerDestinationNode;

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
                this.result = (Boolean)value;
                break;

            case 1:
                this.resultPerDestinationNode = (List<Float>)value;
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

        SolveGraphResponse that = (SolveGraphResponse)obj;

        return ( ( this.result == that.result )
                 && this.resultPerDestinationNode.equals( that.resultPerDestinationNode )
                 && this.info.equals( that.info ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "result" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.result ) );
        builder.append( ", " );
        builder.append( gd.toString( "resultPerDestinationNode" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.resultPerDestinationNode ) );
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
        hashCode = (31 * hashCode) + ((Boolean)this.result).hashCode();
        hashCode = (31 * hashCode) + this.resultPerDestinationNode.hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }
}
