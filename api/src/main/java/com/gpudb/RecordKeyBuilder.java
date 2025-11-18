package com.gpudb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.generic.IndexedRecord;
import org.apache.commons.lang3.tuple.Pair;

final class RecordKeyBuilder<T> {

    private final TypeObjectMap<T> typeObjectMap;
    private final List<Integer> columns;
    private final List<String> columnNames;
    private final List<Type.Column.ColumnType> columnTypes;
    private final Map<Integer, Pair<Integer, Integer>> decimalSizes = new HashMap<>();
    private final int bufferSize;
    private final boolean hasPrimaryKey;

    /*
     * @deprecated  As of version 7.1.8, this method should not be called by a
     * client.  It is intended to be used internally by the
     * {@link RecordRetriever}, but that class now uses the
     * {@link #RecordKeyBuilder(Type, TypeObjectMap<T>) method instead.
     * This method will be removed in version 7.2.0.0.
     */
    @Deprecated public RecordKeyBuilder(boolean primaryKey, Type type) {
        this(type, null);
    }

    /*
     * @deprecated  As of version 7.1.8, this method should not be called by a
     * client.  It is intended to be used internally by the
     * {@link RecordRetriever}, but that class now uses the
     * {@link RecordKeyBuilder(Type, TypeObjectMap<T>) method instead.
     * This method will be removed in version 7.2.0.0.
     */
    @Deprecated public RecordKeyBuilder(boolean primaryKey, TypeObjectMap<T> typeObjectMap) {
        this(typeObjectMap.getType(), typeObjectMap);
    }

    /**
     * Creates a {@link RecordKeyBuilder} with the specified table type.
     * This holds the shard/primary key column positions, names, & types.
     * It also provides the ability to generate a {@link RecordKey} for
     * determining the rank from which to request data, and the key expression
     * used to make that request.
     *
     * @param type       the type of records being retrieved
     * @param typeObjectMap  type object map for the type of records being
     *                       retrieved
     *
     * @throws IllegalArgumentException if the specified table type contains a
     *         column marked as a shard key whose type is incompatible with
     *         being a shard key
     */
    protected RecordKeyBuilder(Type type, TypeObjectMap<T> typeObjectMap) {
        this.typeObjectMap = typeObjectMap;
        List<Integer> pkColumns = new ArrayList<>();
        List<Integer> skColumns = new ArrayList<>();
        this.columnNames = new ArrayList<>();
        this.columnTypes = new ArrayList<>();

        // Build a list of the PK & SK columns in the table
        List<Type.Column> typeColumns = type.getColumns();
        for (int i = 0; i < typeColumns.size(); i++) {
            List<String> columnProps = typeColumns.get(i).getProperties();

            // Accumulate explicit SK & PK columns; stop accumulating PK columns
            //   if an SK is found and at least one PK column is found--the PK
            //   list needs at least one column so that hasPrimaryKey can be set
            //   appropriately below by checking the PK list's non-emptiness
            if (columnProps.contains(ColumnProperty.SHARD_KEY))
                skColumns.add(i);
            if ((pkColumns.isEmpty() || skColumns.isEmpty()) && columnProps.contains(ColumnProperty.PRIMARY_KEY))
                pkColumns.add(i);
        }
        int nonShardedColumns = typeColumns.size() - skColumns.size();

        // If no explicit shard key is defined, assume the PK is the SK
        this.columns = (!skColumns.isEmpty()) ? skColumns : pkColumns;
        
        this.hasPrimaryKey = !pkColumns.isEmpty();
        
        if (this.columns.isEmpty()) {
            this.bufferSize = 0;
            return;
        }

        int size = 0;

        for (int i : this.columns) {
            Type.Column column = typeColumns.get(i);
            this.columnNames.add( column.getName() );

            Type.Column.ColumnType columnType = column.getColumnType();
            switch ( columnType ) {
                case BOOLEAN:
                case CHAR1:
                case INT8:
                    size += 1;
                    break;

                case CHAR2:
                case INT16:
                    size += 2;
                    break;

                case CHAR4:
                case DATE:
                case FLOAT:
                case INTEGER:
                case IPV4:
                case TIME:
                    size += 4;
                    break;

                case CHAR8:
                case DATETIME:
                case DOUBLE:
                case LONG:
                case STRING:
                case TIMESTAMP:
                case ULONG:
                    size += 8;
                    break;
                case DECIMAL:
                    int precision = column.getDecimalPrecision();
                    this.decimalSizes.putIfAbsent(i - nonShardedColumns, Pair.of(precision, column.getDecimalScale()));
                    size += precision > 18 ? 12 : 8;
                    break;
                case CHAR16:
                case UUID:
                    size += 16;
                    break;
                case CHAR32:
                    size += 32;
                    break;
                case CHAR64:
                    size += 64;
                    break;
                case CHAR128:
                    size += 128;
                    break;
                case CHAR256:
                    size += 256;
                    break;

                // The following are not allowed as shard keys
                case ARRAY:
                case JSON:
                case VECTOR:
                case WKB:
                case WKT:
                default:
                    throw new IllegalArgumentException(
                            "Cannot use column <" + column.getName() + "> as a key; " +
                            "type/property <" + column.getType() + ">"
                    );
            }

            this.columnTypes.add( columnType );
        }

        this.bufferSize = size;
    }

    private void addValue(RecordKey key, int column, Object value) throws GPUdbException {
        Type.Column.ColumnType columnType = this.columnTypes.get(column);
        switch (columnType) {
            case BOOLEAN:
                Boolean b = Type.Column.convertBooleanValue(value);
                key.addBoolean(b);
                break;

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
                // Convert the value to String if it is not already a String
                //Allow Double, Float or BigDecimal
                int precision = this.decimalSizes.get(column).getLeft();
                int scale = this.decimalSizes.get(column).getRight();
                String convertedValue = Type.Column.convertDecimalValue(value, scale);
                key.addDecimal(convertedValue, precision, scale);
                break;

            case DOUBLE:
                key.addDouble((Double)value);
                break;

            case FLOAT:
                key.addFloat((Float)value);
                break;

            case INTEGER:
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

            case ULONG:
                key.addUlong((String)value);
                break;
            
            case UUID:
                key.addUuid((String)value);
                break;

            // The following are not allowed as shard keys
            case ARRAY:
            case JSON:
            case VECTOR:
            case WKB:
            case WKT:
            default:
                throw new IllegalArgumentException(
                        "Cannot use column <" + column + "> as a key; " +
                        "type/property <" + this.columnTypes.get(column) + ">"
                );
            }
    }

    public RecordKey build(T object) throws GPUdbException {
        if (this.bufferSize == 0) {
            return null;
        }

        IndexedRecord indexedRecord;

        if (this.typeObjectMap == null) {
            indexedRecord = (IndexedRecord)object;
        } else {
            indexedRecord = null;
        }

        RecordKey key = new RecordKey(this.bufferSize);

        for (int i = 0; i < this.columns.size(); i++) {
            if (indexedRecord != null) {
                addValue(key, i, indexedRecord.get(this.columns.get(i)));
            } else {
                addValue(key, i, this.typeObjectMap.get(object, this.columns.get(i)));
            }
        }

        key.computeHashes();
        return key;
    }

    /**
     * Builds a {@link RecordKey} object, which is used by a
     * {@link RecordRetriever} to identify the shard on which the requested
     * records exist.
     *
     * @param values   the key values corresponding to this table's key columns,
     *                 used to calculate the shard of the records
     *
     * @return         a {@link RecordKey} instance for this table, which can be
     *                 used to determine the rank from which to request data
     */
    public RecordKey build(List<Object> values) throws GPUdbException {
        if (this.bufferSize == 0) {
            return null;
        }

        if (this.columns.size() != values.size()) {
            throw new IllegalArgumentException("Incorrect number of key values specified.");
        }

        RecordKey key = new RecordKey(this.bufferSize);

        for (int i = 0; i < this.columns.size(); i++) {
            addValue(key, i, values.get(i));
        }

        key.computeHashes();
        return key;
    }

    /**
     * Builds and returns a filter expression, associating the given key values
     * with the table's key columns.
     *
     * @param values   the key values corresponding to this table's key columns,
     *                 which will be used to build a filter expression
     *                 identifying the records
     *
     * @return         a filter expression identifying the records with the
     *                 given values
     */
    public String buildExpression(List<Object> values) throws GPUdbException {
        if (this.bufferSize == 0) {
            return null;
        }

        if (this.columns.size() != values.size()) {
            throw new IllegalArgumentException(
                    "Incorrect number of key values specified: " +
                    "need <" + this.columns.size() + ">, got <" + values.size() + ">"
            );
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < this.columns.size(); i++) {
            if (result.length() > 0) {
                result.append(" and ");
            }

            Object value = values.get(i);
            if (value == null) {
                result.append("is_null(");
                result.append(this.columnNames.get(i));
                result.append(")");
                continue;
            }

            result.append(this.columnNames.get(i));
            result.append(" = ");

            switch (this.columnTypes.get(i)) {
                case ARRAY:
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
                case JSON:
                case STRING:
                case TIME:
                case UUID:
                case VECTOR:
                    result.append("\"");
                    result.append(((String)value).replace("\"", "\"\""));
                    result.append("\"");
                    break;

                case ULONG:
                    // Need to verify if the string is an actual unsigned long value
                    if ( !RecordKey.isUnsignedLong( value.toString() ) )
                    {
                        throw new GPUdbException( "Unable to parse string value '" + value.toString()
                                                  + "' as an unsigned long while building expression" );
                    }

                    // For querying purposes, unsigned long values are treated
                    // as numbers, not strings
                    result.append( value.toString() );
                    break;

                case DOUBLE:
                    result.append(Double.toString((Double)value));
                    break;

                case FLOAT:
                    result.append(Float.toString((Float)value));
                    break;

                case BOOLEAN:
                    if (value instanceof Boolean)
                        result.append((Boolean)value ? "1" : "0");
                    else if (value instanceof Number)
                        result.append(((Number) value).intValue() != 0 ? "1" : "0");
                    else
                        result.append(!value.equals(0) ? "1" : "0");
                    break;

                case INTEGER:
                case INT8:
                case INT16:
                    result.append(Integer.toString((Integer)value));
                    break;

                case LONG:
                case TIMESTAMP:
                    result.append(Long.toString((Long)value));
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Cannot use column <" + this.columnNames.get(i) + "> as a key; " +
                            "type/property <" + this.columnTypes.get(i) + ">"
                    );
            }
        }

        return result.toString();
    }

    public boolean hasKey() {
        return !this.columns.isEmpty();
    }

    public boolean hasPrimaryKey() {
        return this.hasPrimaryKey;
    }

    public boolean hasSameKey(RecordKeyBuilder<T> other) {
        return this.columns.equals(other.columns);
    }
}
