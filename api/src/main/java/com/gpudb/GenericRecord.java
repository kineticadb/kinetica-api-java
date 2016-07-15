package com.gpudb;

import java.nio.ByteBuffer;
import java.util.Objects;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

/**
 * An object that contains {@link Record} data based on a GPUdb {@link Type}
 * specified at runtime. GPUdb functions that return non-dynamic data will use
 * generic records by default in the absence of a specified type descriptor or
 * known type.
 */
public final class GenericRecord implements Record {
    private final Type type;
    private final Object[] values;

    /**
     * Creates a new generic record based on the specified GPUdb {@link Type}.
     * Note that generic records can also be created using {@link
     * Type#newInstance}.
     *
     * @param type  the GPUdb type
     */
    public GenericRecord(Type type) {
        this.type = type;
        values = new Object[type.getColumnCount()];
    }

    /**
     * Returns the GPUdb {@link Type} of the record.
     *
     * @return  the GPUdb type
     */
    @Override
    public Type getType() {
        return type;
    }

    /**
     * Returns the Avro record schema of the record.
     *
     * @return  the Avro record schema of the record
     */
    @Override
    public Schema getSchema() {
        return type.getSchema();
    }

    /**
     * Returns the value of the specified field.
     *
     * @param index  the index of the field
     * @return       the value of the field
     *
     * @throws IndexOutOfBoundsException if the specified index is not valid
     */
    @Override
    public Object get(int index) {
        return values[index];
    }

    /**
     * Returns the value of the specified field.
     *
     * @param name  the name of the field
     * @return      the value of the field, or {@code null} if no field with the
     *              specified name exists
     */
    @Override
    public Object get(String name) {
        int index = type.getColumnIndex(name);

        if (index == -1) {
            return null;
        }

        return values[index];
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

    /**
     * Sets the value of the specified field.
     *
     * @param index  the index of the field
     * @param value  the new value
     *
     * @throws IndexOutOfBoundsException if the specified index is not valid
     */
    @Override
    public void put(int index, Object value) {
        values[index] = value;
    }

    /**
     * Sets the value of the specified field.
     *
     * @param name   the name of the field
     * @param value  the new value
     *
     * @throws GPUdbRuntimeException if no field exists with the specified name
     */
    @Override
    public void put(String name, Object value) {
        int index = type.getColumnIndex(name);

        if (index == -1) {
            throw new GPUdbRuntimeException("Field " + name + " does not exist.");
        }

        values[index] = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        GenericRecord that = (GenericRecord)obj;

        if (!that.type.equals(this.type)) {
            return false;
        }

        for (int i = 0; i < this.values.length; i++) {
            if (!Objects.equals(that.values[i], this.values[i])) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;

        for (Object value : values) {
            hashCode = 31 * hashCode;

            if (value != null) {
                hashCode += value.hashCode();
            }
        }

        return hashCode;
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append("{");

        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(",");
            }

            builder.append(gd.toString(type.getColumn(i).getName()));
            builder.append(":");
            builder.append(gd.toString(values[i]));
        }

        builder.append("}");
        return builder.toString();
    }
}