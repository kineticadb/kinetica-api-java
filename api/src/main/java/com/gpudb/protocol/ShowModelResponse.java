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

public class ShowModelResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("ShowModelResponse")
            .namespace("com.gpudb")
            .fields()
                .name("modelNames").type().array().items().stringType().noDefault()
                .name("entityIds").type().array().items().intType().noDefault()
                .name("inputSchemas").type().array().items().stringType().noDefault()
                .name("outputSchemas").type().array().items().stringType().noDefault()
                .name("registryList").type().array().items().stringType().noDefault()
                .name("containerList").type().array().items().stringType().noDefault()
                .name("runFunctionList").type().array().items().stringType().noDefault()
                .name("deployments").type().array().items().stringType().noDefault()
                .name("options").type().array().items().map().values().stringType().noDefault()
                .name("info").type().map().values().stringType().noDefault()
            .endRecord();

    public static Schema getClassSchema() {
        return schema$;
    }

    public static final class Info {
        public static final String KML_RESPONSE = "kml_response";

        private Info() {  }
    }

    private List<String> modelNames;
    private List<Integer> entityIds;
    private List<String> inputSchemas;
    private List<String> outputSchemas;
    private List<String> registryList;
    private List<String> containerList;
    private List<String> runFunctionList;
    private List<String> deployments;
    private List<Map<String, String>> options;
    private Map<String, String> info;

    public ShowModelResponse() {
    }

    public List<String> getModelNames() {
        return modelNames;
    }

    public ShowModelResponse setModelNames(List<String> modelNames) {
        this.modelNames = (modelNames == null) ? new ArrayList<String>() : modelNames;
        return this;
    }

    public List<Integer> getEntityIds() {
        return entityIds;
    }

    public ShowModelResponse setEntityIds(List<Integer> entityIds) {
        this.entityIds = (entityIds == null) ? new ArrayList<Integer>() : entityIds;
        return this;
    }

    public List<String> getInputSchemas() {
        return inputSchemas;
    }

    public ShowModelResponse setInputSchemas(List<String> inputSchemas) {
        this.inputSchemas = (inputSchemas == null) ? new ArrayList<String>() : inputSchemas;
        return this;
    }

    public List<String> getOutputSchemas() {
        return outputSchemas;
    }

    public ShowModelResponse setOutputSchemas(List<String> outputSchemas) {
        this.outputSchemas = (outputSchemas == null) ? new ArrayList<String>() : outputSchemas;
        return this;
    }

    public List<String> getRegistryList() {
        return registryList;
    }

    public ShowModelResponse setRegistryList(List<String> registryList) {
        this.registryList = (registryList == null) ? new ArrayList<String>() : registryList;
        return this;
    }

    public List<String> getContainerList() {
        return containerList;
    }

    public ShowModelResponse setContainerList(List<String> containerList) {
        this.containerList = (containerList == null) ? new ArrayList<String>() : containerList;
        return this;
    }

    public List<String> getRunFunctionList() {
        return runFunctionList;
    }

    public ShowModelResponse setRunFunctionList(List<String> runFunctionList) {
        this.runFunctionList = (runFunctionList == null) ? new ArrayList<String>() : runFunctionList;
        return this;
    }

    public List<String> getDeployments() {
        return deployments;
    }

    public ShowModelResponse setDeployments(List<String> deployments) {
        this.deployments = (deployments == null) ? new ArrayList<String>() : deployments;
        return this;
    }

    public List<Map<String, String>> getOptions() {
        return options;
    }

    public ShowModelResponse setOptions(List<Map<String, String>> options) {
        this.options = (options == null) ? new ArrayList<Map<String, String>>() : options;
        return this;
    }

    public Map<String, String> getInfo() {
        return info;
    }

    public ShowModelResponse setInfo(Map<String, String> info) {
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
                return this.modelNames;

            case 1:
                return this.entityIds;

            case 2:
                return this.inputSchemas;

            case 3:
                return this.outputSchemas;

            case 4:
                return this.registryList;

            case 5:
                return this.containerList;

            case 6:
                return this.runFunctionList;

            case 7:
                return this.deployments;

            case 8:
                return this.options;

            case 9:
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
                this.modelNames = (List<String>)value;
                break;

            case 1:
                this.entityIds = (List<Integer>)value;
                break;

            case 2:
                this.inputSchemas = (List<String>)value;
                break;

            case 3:
                this.outputSchemas = (List<String>)value;
                break;

            case 4:
                this.registryList = (List<String>)value;
                break;

            case 5:
                this.containerList = (List<String>)value;
                break;

            case 6:
                this.runFunctionList = (List<String>)value;
                break;

            case 7:
                this.deployments = (List<String>)value;
                break;

            case 8:
                this.options = (List<Map<String, String>>)value;
                break;

            case 9:
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

        ShowModelResponse that = (ShowModelResponse)obj;

        return ( this.modelNames.equals( that.modelNames )
                 && this.entityIds.equals( that.entityIds )
                 && this.inputSchemas.equals( that.inputSchemas )
                 && this.outputSchemas.equals( that.outputSchemas )
                 && this.registryList.equals( that.registryList )
                 && this.containerList.equals( that.containerList )
                 && this.runFunctionList.equals( that.runFunctionList )
                 && this.deployments.equals( that.deployments )
                 && this.options.equals( that.options )
                 && this.info.equals( that.info ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "modelNames" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.modelNames ) );
        builder.append( ", " );
        builder.append( gd.toString( "entityIds" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.entityIds ) );
        builder.append( ", " );
        builder.append( gd.toString( "inputSchemas" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.inputSchemas ) );
        builder.append( ", " );
        builder.append( gd.toString( "outputSchemas" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.outputSchemas ) );
        builder.append( ", " );
        builder.append( gd.toString( "registryList" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.registryList ) );
        builder.append( ", " );
        builder.append( gd.toString( "containerList" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.containerList ) );
        builder.append( ", " );
        builder.append( gd.toString( "runFunctionList" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.runFunctionList ) );
        builder.append( ", " );
        builder.append( gd.toString( "deployments" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.deployments ) );
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
        hashCode = (31 * hashCode) + this.modelNames.hashCode();
        hashCode = (31 * hashCode) + this.entityIds.hashCode();
        hashCode = (31 * hashCode) + this.inputSchemas.hashCode();
        hashCode = (31 * hashCode) + this.outputSchemas.hashCode();
        hashCode = (31 * hashCode) + this.registryList.hashCode();
        hashCode = (31 * hashCode) + this.containerList.hashCode();
        hashCode = (31 * hashCode) + this.runFunctionList.hashCode();
        hashCode = (31 * hashCode) + this.deployments.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }
}
