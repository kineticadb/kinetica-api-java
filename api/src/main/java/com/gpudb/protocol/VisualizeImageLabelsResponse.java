/*
 *  This file was autogenerated by the GPUdb schema processor.
 *
 *  DO NOT EDIT DIRECTLY.
 */
package com.gpudb.protocol;

import java.nio.ByteBuffer;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;


/**
 * A set of results returned by {@link
 * com.gpudb.GPUdb#visualizeImageLabels(VisualizeImageLabelsRequest)}.
 */
public class VisualizeImageLabelsResponse implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("VisualizeImageLabelsResponse")
            .namespace("com.gpudb")
            .fields()
                .name("width").type().doubleType().noDefault()
                .name("height").type().doubleType().noDefault()
                .name("bgColor").type().longType().noDefault()
                .name("imageData").type().bytesType().noDefault()
            .endRecord();


    /**
     * This method supports the Avro framework and is not intended to be called
     * directly by the user.
     * 
     * @return  the schema for the class.
     * 
     */
    public static Schema getClassSchema() {
        return schema$;
    }

    private double width;
    private double height;
    private long bgColor;
    private ByteBuffer imageData;


    /**
     * Constructs a VisualizeImageLabelsResponse object with default
     * parameters.
     */
    public VisualizeImageLabelsResponse() {
    }

    /**
     * 
     * @return Value of {@code width}.
     * 
     */
    public double getWidth() {
        return width;
    }

    /**
     * 
     * @param width  Value of {@code width}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public VisualizeImageLabelsResponse setWidth(double width) {
        this.width = width;
        return this;
    }

    /**
     * 
     * @return Value of {@code height}.
     * 
     */
    public double getHeight() {
        return height;
    }

    /**
     * 
     * @param height  Value of {@code height}.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public VisualizeImageLabelsResponse setHeight(double height) {
        this.height = height;
        return this;
    }

    /**
     * 
     * @return Background color of the output image, which is always 0
     *         (transparent).
     * 
     */
    public long getBgColor() {
        return bgColor;
    }

    /**
     * 
     * @param bgColor  Background color of the output image, which is always 0
     *                 (transparent).
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public VisualizeImageLabelsResponse setBgColor(long bgColor) {
        this.bgColor = bgColor;
        return this;
    }

    /**
     * 
     * @return Generated image data.
     * 
     */
    public ByteBuffer getImageData() {
        return imageData;
    }

    /**
     * 
     * @param imageData  Generated image data.
     * 
     * @return {@code this} to mimic the builder pattern.
     * 
     */
    public VisualizeImageLabelsResponse setImageData(ByteBuffer imageData) {
        this.imageData = (imageData == null) ? ByteBuffer.wrap( new byte[0] ) : imageData;
        return this;
    }

    /**
     * This method supports the Avro framework and is not intended to be called
     * directly by the user.
     * 
     * @return the schema object describing this class.
     * 
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
     * 
     */
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
     * 
     */
    @Override
    @SuppressWarnings("unchecked")
    public void put(int index, Object value) {
        switch (index) {
            case 0:
                this.width = (Double)value;
                break;

            case 1:
                this.height = (Double)value;
                break;

            case 2:
                this.bgColor = (Long)value;
                break;

            case 3:
                this.imageData = (ByteBuffer)value;
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

        VisualizeImageLabelsResponse that = (VisualizeImageLabelsResponse)obj;

        return ( ( (Double)this.width ).equals( (Double)that.width )
                 && ( (Double)this.height ).equals( (Double)that.height )
                 && ( this.bgColor == that.bgColor )
                 && this.imageData.equals( that.imageData ) );
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
        builder.append( "}" );

        return builder.toString();
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = (31 * hashCode) + ((Double)this.width).hashCode();
        hashCode = (31 * hashCode) + ((Double)this.height).hashCode();
        hashCode = (31 * hashCode) + ((Long)this.bgColor).hashCode();
        hashCode = (31 * hashCode) + this.imageData.hashCode();
        return hashCode;
    }

}