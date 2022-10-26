package com.gpudb;

import java.util.ArrayList;
import java.util.List;
import org.apache.avro.generic.IndexedRecord;

final class RecordKeyBuilder<T> {

    private final TypeObjectMap<T> typeObjectMap;
    private final List<Integer> columns;
    private final List<String> columnNames;
    private final List<Type.Column.ColumnType> columnTypes;
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

        // If no explicit shard key is defined, assume the PK is the SK
        this.columns = (!skColumns.isEmpty()) ? skColumns : pkColumns;
        
        this.hasPrimaryKey = !pkColumns.isEmpty();
        
        if (this.columns.isEmpty()) {
            this.bufferSize = 0;
            return;
        }

        int size = 0;

        for (int i : columns) {
            Type.Column column = typeColumns.get(i);
            this.columnNames.add( column.getName() );

            switch ( column.getColumnType() ) {
                case DOUBLE:
                    this.columnTypes.add( Type.Column.ColumnType.DOUBLE );
                    size += 8;
                    break;
                case FLOAT:
                    this.columnTypes.add( Type.Column.ColumnType.FLOAT );
                    size += 4;
                    break;
                case INTEGER:
                    this.columnTypes.add( Type.Column.ColumnType.INTEGER );
                    size += 4;
                    break;
                case INT8:
                    this.columnTypes.add( Type.Column.ColumnType.INT8 );
                    size += 1;
                    break;
                case INT16:
                    this.columnTypes.add( Type.Column.ColumnType.INT16 );
                    size += 2;
                    break;
                case LONG:
                    this.columnTypes.add( Type.Column.ColumnType.LONG );
                    size += 8;
                    break;
                case TIMESTAMP:
                    this.columnTypes.add( Type.Column.ColumnType.TIMESTAMP );
                    size += 8;
                    break;
                case CHAR1:
                    this.columnTypes.add( Type.Column.ColumnType.CHAR1 );
                    size += 1;
                    break;
                case CHAR2:
                    this.columnTypes.add( Type.Column.ColumnType.CHAR2 );
                    size += 2;
                    break;
                case CHAR4:
                    this.columnTypes.add( Type.Column.ColumnType.CHAR4 );
                    size += 4;
                    break;
                case CHAR8:
                    this.columnTypes.add( Type.Column.ColumnType.CHAR8 );
                    size += 8;
                    break;
                case CHAR16:
                    this.columnTypes.add( Type.Column.ColumnType.CHAR16 );
                    size += 16;
                    break;
                case CHAR32:
                    this.columnTypes.add( Type.Column.ColumnType.CHAR32 );
                    size += 32;
                    break;
                case CHAR64:
                    this.columnTypes.add( Type.Column.ColumnType.CHAR64 );
                    size += 64;
                    break;
                case CHAR128:
                    this.columnTypes.add( Type.Column.ColumnType.CHAR128 );
                    size += 128;
                    break;
                case CHAR256:
                    this.columnTypes.add( Type.Column.ColumnType.CHAR256 );
                    size += 256;
                    break;
                case DATE:
                    this.columnTypes.add( Type.Column.ColumnType.DATE );
                    size += 4;
                    break;
                case DATETIME:
                    this.columnTypes.add( Type.Column.ColumnType.DATETIME );
                    size += 8;
                    break;
                case DECIMAL:
                    this.columnTypes.add( Type.Column.ColumnType.DECIMAL );
                    size += 8;
                    break;
                case TIME:
                    this.columnTypes.add( Type.Column.ColumnType.TIME );
                    size += 4;
                    break;
                case IPV4:
                    this.columnTypes.add( Type.Column.ColumnType.IPV4 );
                    size += 4;
                    break;
                case ULONG:
                    this.columnTypes.add( Type.Column.ColumnType.ULONG );
                    size += 8;
                    break;
                case STRING:
                    this.columnTypes.add( Type.Column.ColumnType.STRING );
                    size += 8;
                    break;
                default:
                    // WKT, WKB, and bytes are not allowed for sharding
                    throw new IllegalArgumentException(
                            "Cannot use column <" + column.getName() + "> as a key; " +
                            "type/property <" + column.getType() + ">"
                    );
            }
        }

        this.bufferSize = size;
    }

    private void addValue(RecordKey key, int column, Object value) throws GPUdbException {
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
        }
    }

    public RecordKey build(T object) throws GPUdbException {
        if (this.bufferSize == 0) {
            return null;
        }

        IndexedRecord indexedRecord;

        if (typeObjectMap == null) {
            indexedRecord = (IndexedRecord)object;
        } else {
            indexedRecord = null;
        }

        RecordKey key = new RecordKey(this.bufferSize);

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

        if (columns.size() != values.size()) {
            throw new IllegalArgumentException("Incorrect number of key values specified.");
        }

        RecordKey key = new RecordKey(this.bufferSize);

        for (int i = 0; i < columns.size(); i++) {
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

        if (columns.size() != values.size()) {
            throw new IllegalArgumentException(
                    "Incorrect number of key values specified: " +
                    "need <" + columns.size() + ">, got <" + values.size() + ">"
            );
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

                case ULONG:
                    String value = (String)values.get(i);
                    // Need to verify if the string is an actual unsigned long value
                    if ( !RecordKey.isUnsignedLong( value ) )
                    {
                        throw new GPUdbException( "Unable to parse string value '" + value
                                                  + "' as an unsigned long while building expression" );
                    }

                    // For querying purposes, unsigned long values are treated
                    // as numbers, not strings
                    result.append( value );
                    break;

                case DOUBLE:
                    result.append(Double.toString((Double)values.get(i)));
                    break;

                case FLOAT:
                    result.append(Float.toString((Float)values.get(i)));
                    break;

                case INTEGER:
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

    public boolean hasPrimaryKey() {
        return hasPrimaryKey;
    }

    public boolean hasSameKey(RecordKeyBuilder<T> other) {
        return this.columns.equals(other.columns);
    }
}
