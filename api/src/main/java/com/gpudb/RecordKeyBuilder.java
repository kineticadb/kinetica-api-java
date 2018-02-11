package com.gpudb;

import java.util.ArrayList;
import java.util.List;
import org.apache.avro.generic.IndexedRecord;

final class RecordKeyBuilder<T> {
    private static enum ColumnType {
        CHAR1,
        CHAR2,
        CHAR4,
        CHAR8,
        CHAR16,
        CHAR32,
        CHAR64,
        CHAR128,
        CHAR256,
        DATE,
        DATETIME,
        DECIMAL,
        DOUBLE,
        FLOAT,
        INT,
        INT8,
        INT16,
        IPV4,
        LONG,
        STRING,
        TIME,
        TIMESTAMP
    }

    private final TypeObjectMap<T> typeObjectMap;
    private final List<Integer> columns;
    private final List<String> columnNames;
    private final List<ColumnType> columnTypes;
    private final int bufferSize;

    public RecordKeyBuilder(boolean primaryKey, Type type) {
        this(primaryKey, type, null);
    }

    public RecordKeyBuilder(boolean primaryKey, TypeObjectMap<T> typeObjectMap) {
        this(primaryKey, typeObjectMap.getType(), typeObjectMap);
    }

    private RecordKeyBuilder(boolean primaryKey, Type type, TypeObjectMap<T> typeObjectMap) {
        this.typeObjectMap = typeObjectMap;
        columns = new ArrayList<>();
        columnNames = new ArrayList<>();
        columnTypes = new ArrayList<>();

        List<Type.Column> typeColumns = type.getColumns();
        boolean hasTimestamp = false;
        boolean hasX = false;
        boolean hasY = false;
        int trackIdColumn = -1;

        for (int i = 0; i < typeColumns.size(); i++) {
            Type.Column typeColumn = typeColumns.get(i);

            switch (typeColumn.getName()) {
                case "TRACKID":
                    trackIdColumn = i;
                    break;

                case "TIMESTAMP":
                    hasTimestamp = true;
                    break;

                case "x":
                    hasX = true;
                    break;

                case "y":
                    hasY = true;
                    break;
            }

            if (primaryKey && typeColumn.getProperties().contains(ColumnProperty.PRIMARY_KEY)) {
                columns.add(i);
            } else if (!primaryKey && typeColumn.getProperties().contains(ColumnProperty.SHARD_KEY)) {
                columns.add(i);
            }
        }

        if (!primaryKey && trackIdColumn != -1 && hasTimestamp && hasX && hasY) {
            if (columns.isEmpty()) {
                columns.add(trackIdColumn);
            } else if (columns.size() != 1 || columns.get(0) != trackIdColumn) {
                throw new IllegalArgumentException("Cannot have a shard key other than TRACKID.");
            }
        }

        if (columns.isEmpty()) {
            bufferSize = 0;
            return;
        }

        int size = 0;

        for (int i : columns) {
            Type.Column typeColumn = typeColumns.get(i);
            columnNames.add(typeColumn.getName());

            if (typeColumn.getType() == Double.class) {
                columnTypes.add(ColumnType.DOUBLE);
                size += 8;
            } else if (typeColumn.getType() == Float.class) {
                columnTypes.add(ColumnType.FLOAT);
                size += 4;
            } else if (typeColumn.getType() == Integer.class) {
                if (typeColumn.getProperties().contains(ColumnProperty.INT8)) {
                    columnTypes.add(ColumnType.INT8);
                    size += 1;
                } else if (typeColumn.getProperties().contains(ColumnProperty.INT16)) {
                    columnTypes.add(ColumnType.INT16);
                    size += 2;
                } else {
                    columnTypes.add(ColumnType.INT);
                    size += 4;
                }
            } else if (typeColumn.getType() == Long.class) {
                if (typeColumn.getProperties().contains(ColumnProperty.TIMESTAMP)) {
                    columnTypes.add(ColumnType.TIMESTAMP);
                    size += 8;
                } else {
                    columnTypes.add(ColumnType.LONG);
                    size += 8;
                }
            } else if (typeColumn.getType() == String.class) {
                if (typeColumn.getProperties().contains(ColumnProperty.CHAR1)) {
                    columnTypes.add(ColumnType.CHAR1);
                    size += 1;
                } else if (typeColumn.getProperties().contains(ColumnProperty.CHAR2)) {
                    columnTypes.add(ColumnType.CHAR2);
                    size += 2;
                } else if (typeColumn.getProperties().contains(ColumnProperty.CHAR4)) {
                    columnTypes.add(ColumnType.CHAR4);
                    size += 4;
                } else if (typeColumn.getProperties().contains(ColumnProperty.CHAR8)) {
                    columnTypes.add(ColumnType.CHAR8);
                    size += 8;
                } else if (typeColumn.getProperties().contains(ColumnProperty.CHAR16)) {
                    columnTypes.add(ColumnType.CHAR16);
                    size += 16;
                } else if (typeColumn.getProperties().contains(ColumnProperty.CHAR32)) {
                    columnTypes.add(ColumnType.CHAR32);
                    size += 32;
                } else if (typeColumn.getProperties().contains(ColumnProperty.CHAR64)) {
                    columnTypes.add(ColumnType.CHAR64);
                    size += 64;
                } else if (typeColumn.getProperties().contains(ColumnProperty.CHAR128)) {
                    columnTypes.add(ColumnType.CHAR128);
                    size += 128;
                } else if (typeColumn.getProperties().contains(ColumnProperty.CHAR256)) {
                    columnTypes.add(ColumnType.CHAR256);
                    size += 256;
                } else if (typeColumn.getProperties().contains(ColumnProperty.DATE)) {
                    columnTypes.add(ColumnType.DATE);
                    size += 4;
                } else if (typeColumn.getProperties().contains(ColumnProperty.DATETIME)) {
                    columnTypes.add(ColumnType.DATETIME);
                    size += 8;
                } else if (typeColumn.getProperties().contains(ColumnProperty.DECIMAL)) {
                    columnTypes.add(ColumnType.DECIMAL);
                    size += 8;
                } else if (typeColumn.getProperties().contains(ColumnProperty.IPV4)) {
                    columnTypes.add(ColumnType.IPV4);
                    size += 4;
                } else if (typeColumn.getProperties().contains(ColumnProperty.TIME)) {
                    columnTypes.add(ColumnType.TIME);
                    size += 4;
                } else {
                    columnTypes.add(ColumnType.STRING);
                    size += 8;
                }
            } else {
                throw new IllegalArgumentException("Cannot use column " + typeColumn.getName() + " as a key.");
            }
        }

        this.bufferSize = size;
    }

    private void addValue(RecordKey key, int column, Object value) {
        switch (columnTypes.get(column)) {
            case CHAR1:
                key.addChar((String)value, 1);
                break;

            case CHAR2:
                key.addChar((String)value, 2);
                break;

            case CHAR4:
                key.addChar((String)value, 4);
                break;

            case CHAR8:
                key.addChar((String)value, 8);
                break;

            case CHAR16:
                key.addChar((String)value, 16);
                break;

            case CHAR32:
                key.addChar((String)value, 32);
                break;

            case CHAR64:
                key.addChar((String)value, 64);
                break;

            case CHAR128:
                key.addChar((String)value, 128);
                break;

            case CHAR256:
                key.addChar((String)value, 256);
                break;

            case DATE:
                key.addDate((String)value);
                break;

            case DATETIME:
                key.addDateTime((String)value);
                break;

            case DECIMAL:
                key.addDecimal((String)value);
                break;

            case DOUBLE:
                key.addDouble((Double)value);
                break;

            case FLOAT:
                key.addFloat((Float)value);
                break;

            case INT:
                key.addInt((Integer)value);
                break;

            case INT8:
                key.addInt8((Integer)value);
                break;

            case INT16:
                key.addInt16((Integer)value);
                break;

            case IPV4:
                key.addIPv4((String)value);
                break;

            case LONG:
                key.addLong((Long)value);
                break;

            case STRING:
                key.addString((String)value);
                break;

            case TIME:
                key.addTime((String)value);
                break;

            case TIMESTAMP:
                key.addTimestamp((Long)value);
                break;
        }
    }

    public RecordKey build(T object) {
        if (bufferSize == 0) {
            return null;
        }

        IndexedRecord indexedRecord;

        if (typeObjectMap == null) {
            indexedRecord = (IndexedRecord)object;
        } else {
            indexedRecord = null;
        }

        RecordKey key = new RecordKey(bufferSize);

        for (int i = 0; i < columns.size(); i++) {
            if (indexedRecord != null) {
                addValue(key, i, indexedRecord.get(columns.get(i)));
            } else {
                addValue(key, i, typeObjectMap.get(object, columns.get(i)));
            }
        }

        key.computeHashes();
        return key;
    }

    public RecordKey build(List<Object> values) {
        if (bufferSize == 0) {
            return null;
        }

        if (columns.size() != values.size()) {
            throw new IllegalArgumentException("Incorrect number of key values specified.");
        }

        RecordKey key = new RecordKey(bufferSize);

        for (int i = 0; i < columns.size(); i++) {
            addValue(key, i, values.get(i));
        }

        key.computeHashes();
        return key;
    }

    public String buildExpression(List<Object> values) {
        if (bufferSize == 0) {
            return null;
        }

        if (columns.size() != values.size()) {
            throw new IllegalArgumentException("Incorrect number of key values specified.");
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < columns.size(); i++) {
            if (result.length() > 0) {
                result.append(" and ");
            }

            if (values.get(i) == null) {
                result.append("is_null(");
                result.append(columnNames.get(i));
                result.append(")");
                continue;
            }

            result.append(columnNames.get(i));
            result.append(" = ");

            switch (columnTypes.get(i)) {
                case CHAR1:
                case CHAR2:
                case CHAR4:
                case CHAR8:
                case CHAR16:
                case CHAR32:
                case CHAR64:
                case CHAR128:
                case CHAR256:
                case DATE:
                case DATETIME:
                case DECIMAL:
                case IPV4:
                case STRING:
                case TIME:
                    result.append("\"");
                    result.append(((String)values.get(i)).replace("\"", "\"\""));
                    result.append("\"");
                    break;

                case DOUBLE:
                    result.append(Double.toString((Double)values.get(i)));
                    break;

                case FLOAT:
                    result.append(Float.toString((Float)values.get(i)));
                    break;

                case INT:
                case INT8:
                case INT16:
                    result.append(Integer.toString((Integer)values.get(i)));
                    break;

                case LONG:
                case TIMESTAMP:
                    result.append(Long.toString((Long)values.get(i)));
                    break;
            }
        }

        return result.toString();
    }

    public boolean hasKey() {
        return !columns.isEmpty();
    }

    public boolean hasSameKey(RecordKeyBuilder<T> other) {
        return this.columns.equals(other.columns);
    }
}