package com.gisfederal.gpudb;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.GenericData;

final class DynamicTableRecord implements Record {
    @SuppressWarnings("unchecked")
    public static final List<Record> transpose(String schemaString, ByteBuffer encodedData) throws GPUdbException {
        Schema schema;

        try {
            schema = new Schema.Parser().parse(schemaString);
        } catch (Exception ex) {
            throw new GPUdbException("Unable to parse schema.", ex);
        }

        if (schema.getType() != Schema.Type.RECORD) {
            throw new GPUdbException("Schema must be of type RECORD.");
        }

        Record data = Avro.decode(schema, encodedData);
        Schema newSchema = Schema.createRecord(schema.getName(), schema.getDoc(), schema.getNamespace(), false);
        int fieldCount = schema.getFields().size() - 1;
        List<String> expressions = (List<String>)data.get(fieldCount);
        List<String> names = new ArrayList<>();

        for (int i = 0; i < fieldCount; i++) {
            String name = expressions.get(i);
            boolean valid = true;

            for (int j = 0; j < name.length(); j++) {
                char c = name.charAt(j);

                if (!(j > 0 && c >= '0' && c <= '9')
                        && !(c >= 'A' && c <= 'Z')
                        && !(c >= 'a' && c <= 'z')
                        && c != '_') {
                    valid = false;
                    break;
                }
            }

            if (!valid) {
                name = "column_" + i;

                if (expressions.contains(name)) {
                    int j = 2;

                    do {
                        name = "column_" + i + "_" + j;
                        j++;
                    } while (expressions.contains(name));
                }
            }

            names.add(name);
        }

        List<Field> fields = new ArrayList<>();
        int recordCount = ((List<?>)data.get(0)).size();

        for (int i = 0; i < fieldCount; i++) {
            Field field = schema.getFields().get(i);

            if (field.schema().getType() != Schema.Type.ARRAY) {
                throw new GPUdbException("Field " + field.name() + " must be of type ARRAY.");
            }

            switch (field.schema().getElementType().getType()) {
                case BYTES:
                case DOUBLE:
                case FLOAT:
                case INT:
                case LONG:
                case STRING:
                    break;

                default:
                    throw new GPUdbException("Field " + field.name() + " must be of type BYTES, DOUBLE, FLOAT, INT, LONG or STRING.");
            }

            if (((List<?>)data.get(fields.size())).size() != recordCount) {
                throw new GPUdbException("Fields must all have the same number of entries.");
            }

            Field newField = new Field(names.get(i), field.schema().getElementType(), field.doc(), field.defaultValue(), field.order());
            newField.addProp("expression", expressions.get(field.pos()));
            fields.add(newField);
        }

        newSchema.setFields(fields);
        List<Record> result = new ArrayList<>();

        for (int i = 0; i < recordCount; i++) {
            result.add(new DynamicTableRecord(newSchema, data, i));
        }

        return result;
    }

    private final Schema schema;
    private final Record data;
    private final int recordIndex;

    private DynamicTableRecord(Schema schema, Record data, int recordIndex) {
        this.schema = schema;
        this.data = data;
        this.recordIndex = recordIndex;
    }

    @Override
    public Schema getSchema() {
        return schema;
    }

    @Override
    public Object get(int index) {
        return ((List<?>)data.get(index)).get(recordIndex);
    }

    @Override
    public Object get(String name) {
        Field field = schema.getField(name);

        if (field == null) {
            return null;
        }

        return ((List<?>)data.get(field.pos())).get(recordIndex);
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
        Field field = schema.getField(name);

        if (field == null) {
            throw new AvroRuntimeException("Not a valid schema field: " + name);
        }

        ((List)data.get(field.pos())).set(recordIndex, value);
    }

    @Override
    public String toString() {
        return GenericData.get().toString(this);
    }
}