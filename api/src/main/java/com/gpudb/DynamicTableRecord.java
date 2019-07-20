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

        // Get the type from the schema string and the data
        Type type = Type.fromDynamicSchema( schemaString, encodedData );

        return DynamicTableRecord.transpose( schemaString, encodedData, type );
    }

    @SuppressWarnings("unchecked")
    public static List<Record> transpose( String schemaString,
                                          ByteBuffer encodedData,
                                          Type type ) throws GPUdbException {

        // Get the schema to decode the data
        Schema schema;
        try {
            schema = new Schema.Parser().parse(schemaString);
        } catch (Exception ex) {
            throw new GPUdbException("Schema is invalid.", ex);
        }

        if (schema.getType() != Schema.Type.RECORD) {
            throw new GPUdbException("Schema must be of type record.");
        }

        // Decode the data
        IndexedRecord data = Avro.decode(schema, encodedData);
        int recordCount = ((List<?>)data.get(0)).size();

        // Extract the records from the decoded data
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
