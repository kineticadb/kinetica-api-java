package com.gpudb;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;

final class DynamicTableRecord implements Record {
    @SuppressWarnings("unchecked")
    public static final List<Record> transpose(String schemaString, ByteBuffer encodedData) throws GPUdbException {
        Schema schema;

        try {
            schema = new Schema.Parser().parse(schemaString);
        } catch (Exception ex) {
            throw new GPUdbException("Schema is invalid.", ex);
        }

        if (schema.getType() != Schema.Type.RECORD) {
            throw new GPUdbException("Schema must be of type record.");
        }

        IndexedRecord data = Avro.decode(schema, encodedData);
        int fieldCount = schema.getFields().size() - 1;
        int recordCount = ((List<?>)data.get(0)).size();
        List<String> expressions = (List<String>)data.get(fieldCount);
        List<Type.Column> columns = new ArrayList<>();

        for (int i = 0; i < fieldCount; i++) {
            Field field = schema.getFields().get(i);

            if (field.schema().getType() != Schema.Type.ARRAY) {
                throw new GPUdbException("Field " + field.name() + " must be of type array.");
            }

            Class<?> columnType;

            switch (field.schema().getElementType().getType()) {
                case BYTES:
                    columnType = ByteBuffer.class;
                    break;

                case DOUBLE:
                    columnType = Double.class;
                    break;

                case FLOAT:
                    columnType = Float.class;
                    break;

                case INT:
                    columnType = Integer.class;
                    break;

                case LONG:
                    columnType = Long.class;
                    break;

                case STRING:
                    columnType = String.class;
                    break;

                default:
                    throw new GPUdbException("Field " + field.name() + " must be of type bytes, double, float, int, long or string.");
            }

            if (((List<?>)data.get(i)).size() != recordCount) {
                throw new GPUdbException("Fields must all have the same number of elements.");
            }

            columns.add(new Type.Column(expressions.get(i), columnType));
        }

        Type type = new Type("", columns);
        List<Record> result = new ArrayList<>();

        for (int i = 0; i < recordCount; i++) {
            result.add(new DynamicTableRecord(type, data, i));
        }

        return result;
    }

    private final Type type;
    private final IndexedRecord data;
    private final int recordIndex;

    private DynamicTableRecord(Type type, IndexedRecord data, int recordIndex) {
        this.type = type;
        this.data = data;
        this.recordIndex = recordIndex;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Schema getSchema() {
        return type.getSchema();
    }

    @Override
    public Object get(int index) {
        return ((List<?>)data.get(index)).get(recordIndex);
    }

    @Override
    public Object get(String name) {
        int columnIndex = type.getColumnIndex(name);

        if (columnIndex == -1) {
            return null;
        }

        return ((List<?>)data.get(columnIndex)).get(recordIndex);
    }

    @Override
    public ByteBuffer getBytes(int index)
    {
        return (ByteBuffer)get(index);
    }

    @Override
    public ByteBuffer getBytes(String name)
    {
        return (ByteBuffer)get(name);
    }

    @Override
    public Double getDouble(int index)
    {
        return (Double)get(index);
    }

    @Override
    public Double getDouble(String name)
    {
        return (Double)get(name);
    }

    @Override
    public Float getFloat(int index)
    {
        return (Float)get(index);
    }

    @Override
    public Float getFloat(String name)
    {
        return (Float)get(name);
    }

    @Override
    public Integer getInt(int index)
    {
        return (Integer)get(index);
    }

    @Override
    public Integer getInt(String name)
    {
        return (Integer)get(name);
    }

    @Override
    public Long getLong(int index)
    {
        return (Long)get(index);
    }

    @Override
    public Long getLong(String name)
    {
        return (Long)get(name);
    }

    @Override
    public String getString(int index)
    {
        return (String)get(index);
    }

    @Override
    public String getString(String name)
    {
        return (String)get(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void put(int index, Object value) {
        ((List)data.get(index)).set(recordIndex, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void put(String name, Object value) {
        int index = type.getColumnIndex(name);

        if (index == -1) {
            throw new GPUdbRuntimeException("Field " + name + " does not exist.");
        }

        ((List)data.get(index)).set(recordIndex, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        DynamicTableRecord that = (DynamicTableRecord)obj;

        if (!that.type.equals(this.type)) {
            return false;
        }

        int columnCount = this.type.getColumnCount();

        for (int i = 0; i < columnCount; i++) {
            if (!Objects.equals(that.get(i), this.get(i))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        int columnCount = type.getColumnCount();

        for (int i = 0; i < columnCount; i++) {
            hashCode = 31 * hashCode;
            Object value = get(i);

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
        int columnCount = type.getColumnCount();

        for (int i = 0; i < columnCount; i++) {
            if (i > 0) {
                builder.append(",");
            }

            builder.append(gd.toString(type.getColumn(i).getName()));
            builder.append(":");
            builder.append(gd.toString(get(i)));
        }

        builder.append("}");
        return builder.toString();
    }
}