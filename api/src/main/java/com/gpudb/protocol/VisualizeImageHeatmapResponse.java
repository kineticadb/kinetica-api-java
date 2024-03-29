/*
 *  This file was autogenerated by the Kinetica schema processor.
 *
 *  DO NOT EDIT DIRECTLY.
 */
package com.gpudb.protocol;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;

public class VisualizeImageHeatmapResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("VisualizeImageHeatmapResponse")
            .namespace("com.gpudb")
            .fields()
                .name("width").type().intType().noDefault()
                .name("height").type().intType().noDefault()
                .name("bgColor").type().longType().noDefault()
                .name("imageData").type().bytesType().noDefault()
                .name("info").type().map().values().stringType().noDefault()
            .endRecord();

    public static Schema getClassSchema() {
        return schema$;
    }

    private int width;
    private int height;
    private long bgColor;
    private ByteBuffer imageData;
    private Map<String, String> info;

    public VisualizeImageHeatmapResponse() {
    }

    public int getWidth() {
        return width;
    }

    public VisualizeImageHeatmapResponse setWidth(int width) {
        this.width = width;
        return this;
    }

    public int getHeight() {
        return height;
    }

    public VisualizeImageHeatmapResponse setHeight(int height) {
        this.height = height;
        return this;
    }

    public long getBgColor() {
        return bgColor;
    }

    public VisualizeImageHeatmapResponse setBgColor(long bgColor) {
        this.bgColor = bgColor;
        return this;
    }

    public ByteBuffer getImageData() {
        return imageData;
    }

    public VisualizeImageHeatmapResponse setImageData(ByteBuffer imageData) {
        this.imageData = (imageData == null) ? ByteBuffer.wrap( new byte[0] ) : imageData;
        return this;
    }

    public Map<String, String> getInfo() {
        return info;
    }

    public VisualizeImageHeatmapResponse setInfo(Map<String, String> info) {
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
                return this.width;

            case 1:
                return this.height;

            case 2:
                return this.bgColor;

            case 3:
                return this.imageData;

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
                this.width = (Integer)value;
                break;

            case 1:
                this.height = (Integer)value;
                break;

            case 2:
                this.bgColor = (Long)value;
                break;

            case 3:
                this.imageData = (ByteBuffer)value;
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

        VisualizeImageHeatmapResponse that = (VisualizeImageHeatmapResponse)obj;

        return ( ( this.width == that.width )
                 && ( this.height == that.height )
                 && ( this.bgColor == that.bgColor )
                 && this.imageData.equals( that.imageData )
                 && this.info.equals( that.info ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "width" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.width ) );
        builder.append( ", " );
        builder.append( gd.toString( "height" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.height ) );
        builder.append( ", " );
        builder.append( gd.toString( "bgColor" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.bgColor ) );
        builder.append( ", " );
        builder.append( gd.toString( "imageData" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.imageData ) );
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
        hashCode = (31 * hashCode) + this.width;
        hashCode = (31 * hashCode) + this.height;
        hashCode = (31 * hashCode) + ((Long)this.bgColor).hashCode();
        hashCode = (31 * hashCode) + this.imageData.hashCode();
        hashCode = (31 * hashCode) + this.info.hashCode();
        return hashCode;
    }
}
