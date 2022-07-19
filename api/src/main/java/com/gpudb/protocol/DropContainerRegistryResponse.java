
/*
 *  This file was autogenerated by the GPUdb schema processor.
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


public class DropContainerRegistryResponse implements IndexedRecord {

    private static final Schema schema$ = SchemaBuilder
            .record("DropContainerRegistryResponse")
            .namespace("com.gpudb")
            .fields()
                .name("registryName").type().stringType().noDefault()
                .name("info").type().map().values().stringType().noDefault()
            .endRecord();


    public static Schema getClassSchema() {
        return schema$;
    }


    public static final class Info {

        public static final String KML_RESPONSE = "kml_response";


        private Info() {  }
    }


    private String registryName;
    private Map<String, String> info;


    public DropContainerRegistryResponse() {
    }

    public String getRegistryName() {
        return registryName;
    }

    public DropContainerRegistryResponse setRegistryName(String registryName) {
        this.registryName = (registryName == null) ? "" : registryName;
        return this;
    }

    public Map<String, String> getInfo() {
        return info;
    }

    public DropContainerRegistryResponse setInfo(Map<String, String> info) {
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
                return this.registryName;

            case 1:
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
                this.registryName = (String)value;
                break;

            case 1:
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

        DropContainerRegistryResponse that = (DropContainerRegistryResponse)obj;

        return ( this.registryName.equals( that.registryName )
                 && this.info.equals( that.info ) );
    }


    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "registryName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.registryName ) );
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
        hashCode = (31 * hashCode) + this.registryName.hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }


}