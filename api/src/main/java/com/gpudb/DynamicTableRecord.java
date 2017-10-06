package com.gpudb;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.IndexedRecord;

final class DynamicTableRecord extends RecordBase implements Serializable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    public static List<Record> transpose(String schemaString, ByteBuffer encodedData) throws GPUdbException {
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
        int fieldCount = schema.getFields().size() - 2;
        int recordCount = ((List<?>)data.get(0)).size();
        List<String> expressions = (List<String>)data.get(fieldCount);
        List<String> types = (List<String>)data.get(fieldCount + 1);

        if (expressions.size() < fieldCount)
        {
            throw new GPUdbException("Every field must have a corresponding expression.");
        }

        if (types.size() < fieldCount)
        {
            throw new GPUdbException("Every field must have a corresponding type.");
        }

        List<Type.Column> columns = new ArrayList<>();

        for (int i = 0; i < fieldCount; i++) {
            Field field = schema.getFields().get(i);

            if (field.schema().getType() != Schema.Type.ARRAY) {
                throw new GPUdbException("Field " + field.name() + " must be of type array.");
            }

            Schema fieldSchema = field.schema().getElementType();
            Schema.Type fieldType = fieldSchema.getType();
            List<String> columnProperties = new ArrayList<>();

            if (fieldType == Schema.Type.UNION) {
                List<Schema> fieldUnionTypes = fieldSchema.getTypes();

                if (fieldUnionTypes.size() == 2 && fieldUnionTypes.get(1).getType() == Schema.Type.NULL) {
                    fieldType = fieldUnionTypes.get(0).getType();
                    columnProperties.add(ColumnProperty.NULLABLE);
                } else {
                    throw new GPUdbException("Field " + field.name() + " has invalid type.");
                }
            }

            Class<?> columnType;

            switch (fieldType) {
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

            String name = expressions.get(i);

            for (int j = 0; j < i; j++) {
                if (name.equals(columns.get(j).getName())) {
                    for (int n = 2; ; n++) {
                        String tempName = name + "_" + n;
                        boolean found = false;

                        for (int k = 0; k < i; k++) {
                            if (tempName.equals(columns.get(k).getName())) {
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            for (int k = i + 1; k < fieldCount; k++) {
                                if (tempName.equals(expressions.get(k))) {
                                    found = true;
                                    break;
                                }
                            }
                        }

                        if (!found) {
                            name = tempName;
                            break;
                        }
                    }
                }
            }

            String type = types.get(i);

            switch (type) {
                case ColumnProperty.CHAR1:
                case ColumnProperty.CHAR2:
                case ColumnProperty.CHAR4:
                case ColumnProperty.CHAR8:
                case ColumnProperty.CHAR16:
                case ColumnProperty.CHAR32:
                case ColumnProperty.CHAR64:
                case ColumnProperty.CHAR128:
                case ColumnProperty.CHAR256:
                case ColumnProperty.DATE:
                case ColumnProperty.DECIMAL:
                case ColumnProperty.INT8:
                case ColumnProperty.INT16:
                case ColumnProperty.IPV4:
                case ColumnProperty.TIME:
                case ColumnProperty.TIMESTAMP:
                    columnProperties.add(type);
            }

            columns.add(new Type.Column(name, columnType, columnProperties));
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

    private Object writeReplace() throws ObjectStreamException {
        GenericRecord gr = new GenericRecord(type);
        int columnCount = type.getColumnCount();

        for (int i = 0; i < columnCount; i++) {
            gr.put(i, get(i));
        }

        return gr;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Object get(int index) {
        return ((List<?>)data.get(index)).get(recordIndex);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void put(int index, Object value) {
        ((List)data.get(index)).set(recordIndex, value);
    }
}