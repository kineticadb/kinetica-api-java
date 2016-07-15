package com.gpudb;

import java.nio.ByteBuffer;
import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;

/**
 * Interface for objects that contain record data. Includes methods for getting
 * and setting fields by index or name and communicating with the Avro
 * framework.
 */
public interface Record extends IndexedRecord {
    /**
     * Returns the GPUdb {@link Type} of the record.
     *
     * @return  the GPUdb type
     */
    Type getType();

    /**
     * Returns the Avro record schema of the record.
     *
     * @return  the Avro record schema of the record
     */
    @Override
    Schema getSchema();

    /**
     * Returns the value of the specified field.
     *
     * @param index  the index of the field
     * @return       the value of the field
     *
     * @throws IndexOutOfBoundsException if the specified index is not valid
     */
    @Override
    Object get(int index);

    /**
     * Returns the value of the specified field.
     *
     * @param name  the name of the field
     * @return      the value of the field, or {@code null} if no field with the
     *              specified name exists
     */
    Object get(String name);

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
    ByteBuffer getBytes(int index);

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
    ByteBuffer getBytes(String name);

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
    Double getDouble(int index);

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
    Double getDouble(String name);

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
    Float getFloat(int index);

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
    Float getFloat(String name);

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
    Integer getInt(int index);

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
    Integer getInt(String name);

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
    Long getLong(int index);

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
    Long getLong(String name);

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
    String getString(int index);

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
    String getString(String name);

    /**
     * Sets the value of the specified field.
     *
     * @param index  the index of the field
     * @param value  the new value
     *
     * @throws IndexOutOfBoundsException if the specified index is not valid
     */
    @Override
    void put(int index, Object value);

    /**
     * Sets the value of the specified field.
     *
     * @param name   the name of the field
     * @param value  the new value
     *
     * @throws GPUdbRuntimeException if no field exists with the specified name
     */
    void put(String name, Object value);
}