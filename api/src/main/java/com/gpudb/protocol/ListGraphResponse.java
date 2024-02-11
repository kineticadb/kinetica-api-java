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

public class ListGraphResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("ListGraphResponse")
            .namespace("com.gpudb")
            .fields()
                .name("result").type().booleanType().noDefault()
                .name("graphNames").type().array().items().stringType().noDefault()
                .name("numNodes").type().array().items().longType().noDefault()
                .name("numEdges").type().array().items().longType().noDefault()
                .name("info").type().map().values().stringType().noDefault()
            .endRecord();

    public static Schema getClassSchema() {
        return schema$;
    }

    private boolean result;
    private List<String> graphNames;
    private List<Long> numNodes;
    private List<Long> numEdges;
    private Map<String, String> info;

    public ListGraphResponse() {
    }

    public boolean getResult() {
        return result;
    }

    public ListGraphResponse setResult(boolean result) {
        this.result = result;
        return this;
    }

    public List<String> getGraphNames() {
        return graphNames;
    }

    public ListGraphResponse setGraphNames(List<String> graphNames) {
        this.graphNames = (graphNames == null) ? new ArrayList<String>() : graphNames;
        return this;
    }

    public List<Long> getNumNodes() {
        return numNodes;
    }

    public ListGraphResponse setNumNodes(List<Long> numNodes) {
        this.numNodes = (numNodes == null) ? new ArrayList<Long>() : numNodes;
        return this;
    }

    public List<Long> getNumEdges() {
        return numEdges;
    }

    public ListGraphResponse setNumEdges(List<Long> numEdges) {
        this.numEdges = (numEdges == null) ? new ArrayList<Long>() : numEdges;
        return this;
    }

    public Map<String, String> getInfo() {
        return info;
    }

    public ListGraphResponse setInfo(Map<String, String> info) {
        this.info = (info == null) ? new LinkedHashMap<String, String>() : info;
        return this;
    }

    @Override
    public Schema getSchema() {
        return schema$;
    }

    @Override
    public Object get(int index) {
        switch (index) {
            case 0:
                return this.result;

            case 1:
                return this.graphNames;

            case 2:
                return this.numNodes;

            case 3:
                return this.numEdges;

            case 4:
                return this.info;

            default:
                throw new IndexOutOfBoundsException("Invalid index specified.");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void put(int index, Object value) {
        switch (index) {
            case 0:
                this.result = (Boolean)value;
                break;

            case 1:
                this.graphNames = (List<String>)value;
                break;

            case 2:
                this.numNodes = (List<Long>)value;
                break;

            case 3:
                this.numEdges = (List<Long>)value;
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

        ListGraphResponse that = (ListGraphResponse)obj;

        return ( ( this.result == that.result )
                 && this.graphNames.equals( that.graphNames )
                 && this.numNodes.equals( that.numNodes )
                 && this.numEdges.equals( that.numEdges )
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
        builder.append( gd.toString( "graphNames" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.graphNames ) );
        builder.append( ", " );
        builder.append( gd.toString( "numNodes" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.numNodes ) );
        builder.append( ", " );
        builder.append( gd.toString( "numEdges" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.numEdges ) );
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
        hashCode = (31 * hashCode) + this.graphNames.hashCode();
        hashCode = (31 * hashCode) + this.numNodes.hashCode();
        hashCode = (31 * hashCode) + this.numEdges.hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }
}
