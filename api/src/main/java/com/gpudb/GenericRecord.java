package com.gpudb;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * An object that contains {@link Record} data based on a GPUdb {@link Type}
 * specified at runtime. GPUdb functions that return non-dynamic data will use
 * generic records by default in the absence of a specified type descriptor or
 * known type.
 */
public final class GenericRecord extends RecordBase implements Serializable  {
    private static final long serialVersionUID = 1L;

    private transient Type type;
    private transient Object[] values;

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

    private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
        type = (Type)stream.readObject();
        values = new Object[type.getColumnCount()];

        for (int i = 0; i < values.length; i++) {
            values[i] = stream.readObject();
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.writeObject(type);

        for (Object obj : values) {
            stream.writeObject(obj);
        }
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Object get(int index) {
        return values[index];
    }

    @Override
    public void put(int index, Object value) {
        values[index] = value;
    }
}