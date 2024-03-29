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

public class ShowGraphGrammarResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("ShowGraphGrammarResponse")
            .namespace("com.gpudb")
            .fields()
                .name("result").type().booleanType().noDefault()
                .name("componentsJson").type().stringType().noDefault()
                .name("info").type().map().values().stringType().noDefault()
            .endRecord();

    public static Schema getClassSchema() {
        return schema$;
    }

    private boolean result;
    private String componentsJson;
    private Map<String, String> info;

    public ShowGraphGrammarResponse() {
    }

    public boolean getResult() {
        return result;
    }

    public ShowGraphGrammarResponse setResult(boolean result) {
        this.result = result;
        return this;
    }

    public String getComponentsJson() {
        return componentsJson;
    }

    public ShowGraphGrammarResponse setComponentsJson(String componentsJson) {
        this.componentsJson = (componentsJson == null) ? "" : componentsJson;
        return this;
    }

    public Map<String, String> getInfo() {
        return info;
    }

    public ShowGraphGrammarResponse setInfo(Map<String, String> info) {
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
                return this.componentsJson;

            case 2:
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
                this.componentsJson = (String)value;
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

        ShowGraphGrammarResponse that = (ShowGraphGrammarResponse)obj;

        return ( ( this.result == that.result )
                 && this.componentsJson.equals( that.componentsJson )
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
        builder.append( gd.toString( "componentsJson" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.componentsJson ) );
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
        hashCode = (31 * hashCode) + this.componentsJson.hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }
}
