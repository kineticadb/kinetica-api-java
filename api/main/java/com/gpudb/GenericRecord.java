package com.gpudb;

import java.nio.ByteBuffer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

/**
 * An object that contains {@link Record} data based on an Avro schema specified
 * at runtime. GPUdb functions that return non-dynamic data will use generic
 * records by default in the absence of a specified type descriptor or known
 * type.
 */
public class GenericRecord extends GenericData.Record implements Record {
    /**
     * Creates a new generic record based on the specified Avro schema. Note
     * that generic records can also be created using {@link Type#newInstance},
     * which will supply the appropriate schema automatically.
     *
     * @param schema  the Avro schema
     */
    public GenericRecord(Schema schema) {
        super(schema);
    }

    /**
     * Returns the value of the specified field cast to a {@link ByteBuffer}.
     * If the field is not of the correct type an exception will be thrown.
     *
     * @param index  the index of the field
     * @return       the value of the field
     *
     * @throws ClassCastException if the field is not of the correct type
     *
     * @throws IndexOutOfBoundsException if the specified index is not valid
     */
    @Override
    public ByteBuffer getBytes(int index)
    {
        return (ByteBuffer)get(index);
    }

    /**
     * Returns the value of the specified field cast to a {@link ByteBuffer}.
     * If the field is not of the correct type an exception will be thrown.
     *
     * @param name  the name of the field
     * @return      the value of the field
     *
     * @throws ClassCastException if the field is not of the correct type
     *
     * @throws GPUdbRuntimeException if no field exists with the specified name
     */
    @Override
    public ByteBuffer getBytes(String name)
    {
        return (ByteBuffer)get(name);
    }

    /**
     * Returns the value of the specified field cast to a {@link Double}.
     * If the field is not of the correct type an exception will be thrown.
     *
     * @param index  the index of the field
     * @return       the value of the field
     *
     * @throws ClassCastException if the field is not of the correct type
     *
     * @throws IndexOutOfBoundsException if the specified index is not valid
     */
    @Override
    public Double getDouble(int index)
    {
        return (Double)get(index);
    }

    /**
     * Returns the value of the specified field cast to a {@link Double}.
     * If the field is not of the correct type an exception will be thrown.
     *
     * @param name  the name of the field
     * @return      the value of the field
     *
     * @throws ClassCastException if the field is not of the correct type
     *
     * @throws GPUdbRuntimeException if no field exists with the specified name
     */
    @Override
    public Double getDouble(String name)
    {
        return (Double)get(name);
    }

    /**
     * Returns the value of the specified field cast to a {@link Float}.
     * If the field is not of the correct type an exception will be thrown.
     *
     * @param index  the index of the field
     * @return       the value of the field
     *
     * @throws ClassCastException if the field is not of the correct type
     *
     * @throws IndexOutOfBoundsException if the specified index is not valid
     */
    @Override
    public Float getFloat(int index)
    {
        return (Float)get(index);
    }

    /**
     * Returns the value of the specified field cast to a {@link Float}.
     * If the field is not of the correct type an exception will be thrown.
     *
     * @param name  the name of the field
     * @return      the value of the field
     *
     * @throws ClassCastException if the field is not of the correct type
     *
     * @throws GPUdbRuntimeException if no field exists with the specified name
     */
    @Override
    public Float getFloat(String name)
    {
        return (Float)get(name);
    }

    /**
     * Returns the value of the specified field cast to a {@link Integer}.
     * If the field is not of the correct type an exception will be thrown.
     *
     * @param index  the index of the field
     * @return       the value of the field
     *
     * @throws ClassCastException if the field is not of the correct type
     *
     * @throws IndexOutOfBoundsException if the specified index is not valid
     */
    @Override
    public Integer getInt(int index)
    {
        return (Integer)get(index);
    }

    /**
     * Returns the value of the specified field cast to a {@link Integer}.
     * If the field is not of the correct type an exception will be thrown.
     *
     * @param name  the name of the field
     * @return      the value of the field
     *
     * @throws ClassCastException if the field is not of the correct type
     *
     * @throws GPUdbRuntimeException if no field exists with the specified name
     */
    @Override
    public Integer getInt(String name)
    {
        return (Integer)get(name);
    }

    /**
     * Returns the value of the specified field cast to a {@link Long}.
     * If the field is not of the correct type an exception will be thrown.
     *
     * @param index  the index of the field
     * @return       the value of the field
     *
     * @throws ClassCastException if the field is not of the correct type
     *
     * @throws IndexOutOfBoundsException if the specified index is not valid
     */
    @Override
    public Long getLong(int index)
    {
        return (Long)get(index);
    }

    /**
     * Returns the value of the specified field cast to a {@link Long}.
     * If the field is not of the correct type an exception will be thrown.
     *
     * @param name  the name of the field
     * @return      the value of the field
     *
     * @throws ClassCastException if the field is not of the correct type
     *
     * @throws GPUdbRuntimeException if no field exists with the specified name
     */
    @Override
    public Long getLong(String name)
    {
        return (Long)get(name);
    }

    /**
     * Returns the value of the specified field cast to a {@link String}.
     * If the field is not of the correct type an exception will be thrown.
     *
     * @param index  the index of the field
     * @return       the value of the field
     *
     * @throws ClassCastException if the field is not of the correct type
     *
     * @throws IndexOutOfBoundsException if the specified index is not valid
     */
    @Override
    public String getString(int index)
    {
        return (String)get(index);
    }

    /**
     * Returns the value of the specified field cast to a {@link String}.
     * If the field is not of the correct type an exception will be thrown.
     *
     * @param name  the name of the field
     * @return      the value of the field
     *
     * @throws ClassCastException if the field is not of the correct type
     *
     * @throws GPUdbRuntimeException if no field exists with the specified name
     */
    @Override
    public String getString(String name)
    {
        return (String)get(name);
    }
}