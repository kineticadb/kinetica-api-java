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

public class AlterModelRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("AlterModelRequest")
            .namespace("com.gpudb")
            .fields()
                .name("modelName").type().stringType().noDefault()
                .name("action").type().stringType().noDefault()
                .name("value").type().stringType().noDefault()
                .name("options").type().map().values().stringType().noDefault()
            .endRecord();

    public static Schema getClassSchema() {
        return schema$;
    }

    public static final class Action {
        public static final String CONTAINER = "container";
        public static final String REGISTRY = "registry";
        public static final String REFRESH = "refresh";
        public static final String STOP_DEPLOYMENT = "stop_deployment";

        private Action() {  }
    }

    private String modelName;
    private String action;
    private String value;
    private Map<String, String> options;

    public AlterModelRequest() {
        modelName = "";
        action = "";
        value = "";
        options = new LinkedHashMap<>();
    }

    public AlterModelRequest(String modelName, String action, String value, Map<String, String> options) {
        this.modelName = (modelName == null) ? "" : modelName;
        this.action = (action == null) ? "" : action;
        this.value = (value == null) ? "" : value;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    public String getModelName() {
        return modelName;
    }

    public AlterModelRequest setModelName(String modelName) {
        this.modelName = (modelName == null) ? "" : modelName;
        return this;
    }

    public String getAction() {
        return action;
    }

    public AlterModelRequest setAction(String action) {
        this.action = (action == null) ? "" : action;
        return this;
    }

    public String getValue() {
        return value;
    }

    public AlterModelRequest setValue(String value) {
        this.value = (value == null) ? "" : value;
        return this;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public AlterModelRequest setOptions(Map<String, String> options) {
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
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
                return this.modelName;

            case 1:
                return this.action;

            case 2:
                return this.value;

            case 3:
                return this.options;

            default:
                throw new IndexOutOfBoundsException("Invalid index specified.");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void put(int index, Object value) {
        switch (index) {
            case 0:
                this.modelName = (String)value;
                break;

            case 1:
                this.action = (String)value;
                break;

            case 2:
                this.value = (String)value;
                break;

            case 3:
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

        AlterModelRequest that = (AlterModelRequest)obj;

        return ( this.modelName.equals( that.modelName )
                 && this.action.equals( that.action )
                 && this.value.equals( that.value )
                 && this.options.equals( that.options ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "modelName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.modelName ) );
        builder.append( ", " );
        builder.append( gd.toString( "action" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.action ) );
        builder.append( ", " );
        builder.append( gd.toString( "value" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.value ) );
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
        hashCode = (31 * hashCode) + this.modelName.hashCode();
        hashCode = (31 * hashCode) + this.action.hashCode();
        hashCode = (31 * hashCode) + this.value.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
