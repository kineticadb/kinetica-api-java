package com.gisfederal.gpudb;

import com.gisfederal.gpudb.protocol.ShowTableResponse;
import com.gisfederal.gpudb.protocol.ShowTypesResponse;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.GenericData;

/**
 * Immutable collection of metadata about a GPUdb type.
 */
public final class Type {
    /**
     * Immutable collection of metadata about a column that is part of a GPUdb
     * type.
     */
    public static final class Column {
        private String name;
        private Class<?> type;
        private List<String> properties;

        /**
         * Creates a {@link Column} object with the specified metadata.
         *
         * @param name        the name of the column
         * @param type        the Java data type of the column.
         * @param properties  the list of properties that apply to the column;
         *                    defaults to none
         *
         * @throws IllegalArgumentException if {@code name} or any
         * {@code properties} are invalid, or if {@code type} is not one of the
         * following: {@link ByteBuffer}, {@link Double}, {@link Float},
         * {@link Integer}, {@link Long}, or {@link String}
         *
         * @see ColumnProperty
         */
        public Column(String name, Class<?> type, String... properties) {
            this(name, type, Arrays.asList(properties));
        }

        /**
         * Creates a {@link Column} object with the specified metadata.
         *
         * @param name        the name of the column
         * @param type        the Java data type of the column
         * @param properties  the list of properties that apply to the column;
         *                    defaults to none
         *
         * @throws IllegalArgumentException if {@code name} or any
         * {@code properties} are invalid, or if {@code type} is not one of the
         * following: {@link ByteBuffer}, {@link Double}, {@link Float},
         * {@link Integer}, {@link Long}, or {@link String}
         *
         * @see ColumnProperty
         */
        public Column(String name, Class<?> type, List<String> properties) {
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Name must not be empty.");
            }

            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);

                if (!(i > 0 && c >= '0' && c <= '9')
                        && !(c >= 'A' && c <= 'Z')
                        && !(c >= 'a' && c <= 'z')
                        && c != '_') {
                    throw new IllegalArgumentException("Invalid name specified.");
                }
            }

            if (type != ByteBuffer.class && type != Double.class
                    && type != Float.class && type != Integer.class
                    && type != Long.class && type != String.class) {
                throw new IllegalArgumentException("Column " + name + " must be of type ByteBuffer, Double, Float, Integer, Long or String.");
            }

            if (properties != null) {
                for (String property : properties) {
                    if (property == null) {
                        throw new IllegalArgumentException("Properties must not be null.");
                    }

                    if (property.isEmpty()) {
                        throw new IllegalArgumentException("Properties must not be empty.");
                    }
                }
            }

            this.name = name;
            this.type = type;
            this.properties = Collections.unmodifiableList(properties == null || properties.isEmpty() ? new ArrayList<String>() : new ArrayList<>(properties));
        }

        /**
         * Gets the name of the column.
         *
         * @return  the name of the column
         */
        public String getName() {
            return name;
        }

        /**
         * Gets the Java data type of the column.
         *
         * @return  the Java data type of the column
         */
        public Class<?> getType() {
            return type;
        }

        /**
         * Gets the list of properties that apply to the column.
         *
         * @return  the list of properties that apply to the column
         *
         * @see ColumnProperty
         */
        public List<String> getProperties() {
            return properties;
        }

        /**
         * Returns a string representation of the column metadata.
         *
         * @return  a string representation of the column metadata
         */
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("{");
            builder.append("\"name\": ").append(GenericData.get().toString(name)).append(", ");
            builder.append("\"type\": \"").append(type.getSimpleName()).append("\", ");
            builder.append("\"properties\": [");
            boolean output = false;

            for (String property : properties) {
                if (output) {
                    builder.append(", ");
                }

                builder.append(GenericData.get().toString(property));
                output = true;
            }

            builder.append("]}");
            return builder.toString();
        }
    }

    /**
     * Creates a {@link Type} object containing metadata for the GPUdb type of
     * an existing table in GPUdb. Note that this method makes a request to
     * GPUdb to obtain the metadata.
     *
     * @param gpudb      the {@link GPUdb} instance from which to obtain the
     *                   metadata
     * @param tableName  the name of the table in GPUdb
     * @return           the created {@link Type} object
     *
     * @throws GPUdbException if the table does not exist or is not homogeneous,
     * or if an error occurs during the request for metadata
     */
    public static Type fromTable(GPUdb gpudb, String tableName) throws GPUdbException {
        ShowTableResponse response = gpudb.showTable(tableName, null);

        if (response.getTypeIds().isEmpty()) {
            throw new GPUdbException("Table " + tableName + " does not exist.");
        }

        if (response.getTypeIds().size() > 1) {
            String typeId = response.getTypeIds().get(0);

            for (int i = 1; i < response.getTypeIds().size(); i++) {
                if (!response.getTypeIds().get(i).equals(typeId)) {
                    throw new GPUdbException("Table " + tableName + " is not homogeneous.");
                }
            }
        }

        return new Type(response.getTypeLabels().get(0), response.getTypeSchemas().get(0), response.getProperties().get(0));
    }

    /**
     * Creates a {@link Type} object containing metadata for an existing type in
     * GPUdb. Note that this method makes a request to GPUdb to obtain the
     * metadata.
     *
     * @param gpudb   the {@link GPUdb} instance from which to obtain the
     *                metadata
     * @param typeId  the type ID of the type in GPUdb
     * @return        the created {@link Type} object
     *
     * @throws GPUdbException if the type does not exist or if an error occurs
     * during the request for metadata
     */
    public static Type fromType(GPUdb gpudb, String typeId) throws GPUdbException {
        ShowTypesResponse response = gpudb.showTypes(typeId, null, null);

        if (response.getTypeIds().isEmpty()) {
            throw new GPUdbException("Type " + typeId + " does not exist.");
        }

        return new Type(response.getLabels().get(0), response.getTypeSchemas().get(0), response.getProperties().get(0));
    }

    private String label;
    private List<Column> columns;
    private Schema schema;

    /**
     * Creates a {@link Type} object with the specified metadata.
     *
     * @param label    a user-defined description string which can be used to
     *                 differentiate between data with otherwise identical
     *                 schemas
     * @param columns  the list of columns that the type comprises
     *
     * @throws IllegalArgumentException if no columns are specified
     */
    public Type(String label, Column... columns) {
        this(label, Arrays.asList(columns));
    }

    /**
     * Creates a {@link Type} object with the specified metadata.
     *
     * @param label    a user-defined description string which can be used to
     *                 differentiate between data with otherwise identical
     *                 schemas
     * @param columns  the list of columns that the type comprises
     *
     * @throws IllegalArgumentException if no columns are specified
     */
    public Type(String label, List<Column> columns) {
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("At least one column must be specified.");
        }

        this.label = label;
        this.columns = Collections.unmodifiableList(new ArrayList<>(columns));
        schema = Schema.createRecord("type_name", null, null, false);
        ArrayList<Field> fields = new ArrayList<>();

        for (Column column : columns) {
            Schema.Type fieldType;
            Class<?> columnType = column.getType();

            if (columnType == ByteBuffer.class) {
                fieldType = Schema.Type.BYTES;
            } else if (columnType == Double.class) {
                fieldType = Schema.Type.DOUBLE;
            } else if (columnType == Float.class) {
                fieldType = Schema.Type.FLOAT;
            } else if (columnType == Integer.class) {
                fieldType = Schema.Type.INT;
            } else if (columnType == Long.class) {
                fieldType = Schema.Type.LONG;
            } else if (columnType == String.class) {
                fieldType = Schema.Type.STRING;
            } else {
                throw new IllegalArgumentException("Column " + column.getName() + " must be of type ByteBuffer, Double, Float, Integer, Long or String.");
            }

            fields.add(new Field(column.getName(), Schema.create(fieldType), null, null));
        }

        schema.setFields(fields);
    }

    /**
     * Creates a {@link Type} object using data returned from the GPUdb
     * /show/table or /show/types endpoints.
     *
     * @param label       a user-defined description string which can be used to
     *                    differentiate between data with otherwise identical
     *                    schemas
     * @param typeSchema  the Avro record schema for the type
     * @param properties  a map of column names to lists of properties that
     *                    apply to those columns
     *
     * @throws IllegalArgumentException if {@code typeSchema} is invalid or
     * contains unsupported field types, or if any {@code properties} are
     * invalid
     */
    public Type(String label, String typeSchema, Map<String, List<String>> properties) {
        this.label = label;
        columns = new ArrayList<>();
        schema = new Schema.Parser().parse(typeSchema);

        if (schema.getType() != Schema.Type.RECORD) {
            throw new IllegalArgumentException("Schema must be of type record.");
        }

        for (Field field : schema.getFields()) {
            String fieldName = field.name();
            Class<?> columnType;
            Schema.Type fieldType = field.schema().getType();

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
                    throw new IllegalArgumentException("Field " + fieldName + " must be of type BYTES, DOUBLE, FLOAT, INT, LONG or STRING.");
            }

            columns.add(new Column(fieldName, columnType, properties.get(fieldName)));
        }

        columns = Collections.unmodifiableList(columns);
    }

    /**
     * Gets the user-defined description string which can be used to
     * differentiate between data with otherwise identical schemas.
     *
     * @return  the label string
     */
    public String getLabel() {
        return label;
    }

    /**
     * Gets the list of columns that the type comprises.
     *
     * @return  the list of columns that the type comprises
     */
    public List<Column> getColumns() {
        return columns;
    }

    /**
     * Gets the compiled Avro record schema for the type.
     *
     * @return  the compiled Avro record schema for the type
     */
    public Schema getSchema() {
        return schema;
    }

    /**
     * Creates a new instance of {@link GenericRecord} based on the schema for
     * the type.
     *
     * @return  a new {@link GenericRecord}
     */
    public Record newInstance() {
        return new GenericRecord(schema);
    }

    /**
     * Creates a type in GPUdb based on the metadata in the {@link Type} object
     * and returns the type ID for reference. If an identical type already
     * exists in GPUdb, the type ID of the existing type will be returned and
     * no new type will be created.
     *
     * @param gpudb  the {@link GPUdb} instance in which to create the type
     * @return       the type ID of the type in GPUdb
     *
     * @throws GPUdbException if an error occurs while creating the type
     */
    public String create(GPUdb gpudb) throws GPUdbException {
        LinkedHashMap<String, List<String>> properties = new LinkedHashMap<>();

        for (Column column : columns) {
            List<String> columnProperties = column.getProperties();

            if (!columnProperties.isEmpty()) {
                properties.put(column.getName(), columnProperties);
            }
        }

        return gpudb.createType(schema.toString(), label, properties, null).getTypeId();
    }

    /**
     * Returns a string representation of the type metadata.
     *
     * @return  a string representation of the type metadata
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append("\"label\": ").append(GenericData.get().toString(label)).append(", ");
        builder.append("\"columns\": [");
        boolean output = false;

        for (Column column : columns) {
            if (output) {
                builder.append(", ");
            }

            builder.append(column.toString());
            output = true;
        }

        builder.append("]}");
        return builder.toString();
    }
}