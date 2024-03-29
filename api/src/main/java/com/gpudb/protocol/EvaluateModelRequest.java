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

public class EvaluateModelRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("EvaluateModelRequest")
            .namespace("com.gpudb")
            .fields()
                .name("modelName").type().stringType().noDefault()
                .name("replicas").type().intType().noDefault()
                .name("deploymentMode").type().stringType().noDefault()
                .name("sourceTable").type().stringType().noDefault()
                .name("destinationTable").type().stringType().noDefault()
                .name("options").type().map().values().stringType().noDefault()
            .endRecord();

    public static Schema getClassSchema() {
        return schema$;
    }

    private String modelName;
    private int replicas;
    private String deploymentMode;
    private String sourceTable;
    private String destinationTable;
    private Map<String, String> options;

    public EvaluateModelRequest() {
        modelName = "";
        deploymentMode = "";
        sourceTable = "";
        destinationTable = "";
        options = new LinkedHashMap<>();
    }

    public EvaluateModelRequest(String modelName, int replicas, String deploymentMode, String sourceTable, String destinationTable, Map<String, String> options) {
        this.modelName = (modelName == null) ? "" : modelName;
        this.replicas = replicas;
        this.deploymentMode = (deploymentMode == null) ? "" : deploymentMode;
        this.sourceTable = (sourceTable == null) ? "" : sourceTable;
        this.destinationTable = (destinationTable == null) ? "" : destinationTable;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    public String getModelName() {
        return modelName;
    }

    public EvaluateModelRequest setModelName(String modelName) {
        this.modelName = (modelName == null) ? "" : modelName;
        return this;
    }

    public int getReplicas() {
        return replicas;
    }

    public EvaluateModelRequest setReplicas(int replicas) {
        this.replicas = replicas;
        return this;
    }

    public String getDeploymentMode() {
        return deploymentMode;
    }

    public EvaluateModelRequest setDeploymentMode(String deploymentMode) {
        this.deploymentMode = (deploymentMode == null) ? "" : deploymentMode;
        return this;
    }

    public String getSourceTable() {
        return sourceTable;
    }

    public EvaluateModelRequest setSourceTable(String sourceTable) {
        this.sourceTable = (sourceTable == null) ? "" : sourceTable;
        return this;
    }

    public String getDestinationTable() {
        return destinationTable;
    }

    public EvaluateModelRequest setDestinationTable(String destinationTable) {
        this.destinationTable = (destinationTable == null) ? "" : destinationTable;
        return this;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public EvaluateModelRequest setOptions(Map<String, String> options) {
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
                return this.replicas;

            case 2:
                return this.deploymentMode;

            case 3:
                return this.sourceTable;

            case 4:
                return this.destinationTable;

            case 5:
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
                this.replicas = (Integer)value;
                break;

            case 2:
                this.deploymentMode = (String)value;
                break;

            case 3:
                this.sourceTable = (String)value;
                break;

            case 4:
                this.destinationTable = (String)value;
                break;

            case 5:
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

        EvaluateModelRequest that = (EvaluateModelRequest)obj;

        return ( this.modelName.equals( that.modelName )
                 && ( this.replicas == that.replicas )
                 && this.deploymentMode.equals( that.deploymentMode )
                 && this.sourceTable.equals( that.sourceTable )
                 && this.destinationTable.equals( that.destinationTable )
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
        builder.append( gd.toString( "replicas" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.replicas ) );
        builder.append( ", " );
        builder.append( gd.toString( "deploymentMode" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.deploymentMode ) );
        builder.append( ", " );
        builder.append( gd.toString( "sourceTable" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.sourceTable ) );
        builder.append( ", " );
        builder.append( gd.toString( "destinationTable" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.destinationTable ) );
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
        hashCode = (31 * hashCode) + this.replicas;
        hashCode = (31 * hashCode) + this.deploymentMode.hashCode();
        hashCode = (31 * hashCode) + this.sourceTable.hashCode();
        hashCode = (31 * hashCode) + this.destinationTable.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
