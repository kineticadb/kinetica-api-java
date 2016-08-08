package com.gpudb;

/**
 * An object that contains {@link Record} data based on a GPUdb {@link Type}
 * specified at runtime. GPUdb functions that return non-dynamic data will use
 * generic records by default in the absence of a specified type descriptor or
 * known type.
 */
public final class GenericRecord extends RecordBase {
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