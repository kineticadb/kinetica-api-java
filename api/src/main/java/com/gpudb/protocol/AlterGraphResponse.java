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

public class AlterGraphResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("AlterGraphResponse")
            .namespace("com.gpudb")
            .fields()
                .name("action").type().stringType().noDefault()
                .name("actionArg").type().stringType().noDefault()
                .name("info").type().map().values().stringType().noDefault()
            .endRecord();

    public static Schema getClassSchema() {
        return schema$;
    }

    private String action;
    private String actionArg;
    private Map<String, String> info;

    public AlterGraphResponse() {
    }

    public String getAction() {
        return action;
    }

    public AlterGraphResponse setAction(String action) {
        this.action = (action == null) ? "" : action;
        return this;
    }

    public String getActionArg() {
        return actionArg;
    }

    public AlterGraphResponse setActionArg(String actionArg) {
        this.actionArg = (actionArg == null) ? "" : actionArg;
        return this;
    }

    public Map<String, String> getInfo() {
        return info;
    }

    public AlterGraphResponse setInfo(Map<String, String> info) {
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
                return this.action;

            case 1:
                return this.actionArg;

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
                this.action = (String)value;
                break;

            case 1:
                this.actionArg = (String)value;
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

        AlterGraphResponse that = (AlterGraphResponse)obj;

        return ( this.action.equals( that.action )
                 && this.actionArg.equals( that.actionArg )
                 && this.info.equals( that.info ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "action" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.action ) );
        builder.append( ", " );
        builder.append( gd.toString( "actionArg" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.actionArg ) );
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
        hashCode = (31 * hashCode) + this.action.hashCode();
        hashCode = (31 * hashCode) + this.actionArg.hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }
}