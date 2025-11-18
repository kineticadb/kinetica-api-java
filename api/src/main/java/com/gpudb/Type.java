package com.gpudb;

import com.gpudb.protocol.ShowTableResponse;
import com.gpudb.protocol.ShowTypesResponse;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.GenericData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Immutable collection of metadata about a GPUdb type.
 */
public final class Type implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Immutable collection of metadata about a column that is part of a GPUdb
     * type.
     */
    public static final class Column implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * Converts a value ({@link BigDecimal} | {@link Double} | {@link Float} | {@link String} | {@link Number})
         * to a {@link String} value
         *
         * @param value - the value to convert
         * @param scale - the decimal scale to use
         * @return - a {@link String} value for the input decimal value suitably scaled
         */
        static String convertDecimalValue(Object value, int scale) {
            String result = null;
            BigDecimal bd;

            if ( value instanceof String) {
                result = (String) value;
            } else if (value != null) {
                if (value instanceof BigDecimal) {
                    bd = (BigDecimal) value;
                } else if (value instanceof Double) {
                    bd = BigDecimal.valueOf((Double) value);
                } else if (value instanceof Float) {
                    bd = BigDecimal.valueOf((Float) value);
                } else if (value instanceof Number) {
                    // Handle primitive wrappers (double, float) and other Number types
                    bd = new BigDecimal(value.toString());
                } else {
                    throw new IllegalArgumentException(
                            "Unsupported type: " + value.getClass().getName() +
                                    ". Expected Double, Float, or BigDecimal."
                    );
                }
                bd = bd.setScale(scale, RoundingMode.UNNECESSARY);
                result = bd.toPlainString().replaceAll("0+$", "");
            }

            return result;
        }

        /** Converts a ({@link Boolean} | {@link Number} etc) to a {#boolean} value
         * @param value - the {@link Object} to boolean
         * @return - a Boolean value
         */
        static Boolean convertBooleanValue(Object value) {
            Boolean result = null;
            if (value != null) {
                if (value instanceof Boolean) {
                    result = (Boolean) value;
                } else if (value instanceof Number) {
                    result = ((Number) value).intValue() != 0;
                } else {
                    result = Boolean.parseBoolean(value.toString());
                }
            }
            return result;
        }


        /**
         * An enumeration of base types for column (excluding
         * any property-related subtypes).  This is synonymous
         * to using the Class type for the column, but the enumeration
         * provides the ability to use switches instead of if statements
         * for checking which type the column is.
         */
        public static enum ColumnBaseType {
            // Basic types
            INTEGER,
            LONG,
            FLOAT,
            DOUBLE,
            STRING,
            BYTES
        }


        /**
         * An enumeration of all the Kinetica column types (including
         * sub-types that are determined by the properties used).
         * Note that multiple column types will map to a single base
         * type ({@link ColumnBaseType}).
         */
        public static enum ColumnType {
            // Basic types
            INTEGER,
            LONG,
            FLOAT,
            DOUBLE,
            STRING,
            BYTES,
            // Integer sub-types
            BOOLEAN,
            INT8,
            INT16,
            // Long sub-types
            TIMESTAMP,
            // String sub-types
            ARRAY,
            DATE,
            DATETIME,
            TIME,
            DECIMAL,
            CHAR1,
            CHAR2,
            CHAR4,
            CHAR8,
            CHAR16,
            CHAR32,
            CHAR64,
            CHAR128,
            CHAR256,
            IPV4,
            JSON,
            ULONG,
            UUID,
            WKT,
            // Bytes sub-types
            VECTOR,
            WKB
        }

        public static final int DEFAULT_DECIMAL_PRECISION = 18;
        public static final int DEFAULT_DECIMAL_SCALE = 4;
        public static final BigDecimal DEFAULT_DECIMAL_MIN = new BigDecimal(Long.MIN_VALUE).movePointLeft(DEFAULT_DECIMAL_SCALE);
        public static final BigDecimal DEFAULT_DECIMAL_MAX = new BigDecimal(Long.MAX_VALUE).movePointLeft(DEFAULT_DECIMAL_SCALE);
        public static final int DECIMAL8_MAX_PRECISION = 18;
        private static final Pattern decimalPattern = Pattern.compile(ColumnProperty.DECIMAL + "\\((\\d+),(\\d+)\\)");

        private transient String name;
        private transient Class<?> type;
        private transient boolean isNullable;
        private transient List<String> properties;
        private transient ColumnType columnType;
        private transient ColumnBaseType columnBaseType;
        private transient int precision;
        private transient int scale;

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
            this.name = name;
            this.type = type;
            this.properties = properties == null ? new ArrayList<>() : new ArrayList<>(properties);
            init();
        }

        private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
            this.name = (String)stream.readObject();
            this.type = (Class<?>)stream.readObject();
            int propertyCount = stream.readInt();
            this.properties = new ArrayList<>(propertyCount);

            for (int i = 0; i < propertyCount; i++) {
                this.properties.add((String)stream.readObject());
            }

            init();
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            stream.writeObject(this.name);
            stream.writeObject(this.type);
            stream.writeInt(this.properties.size());

            for (String property : this.properties) {
                stream.writeObject(property);
            }
        }

        private void setDecimalInfo(String propLower) {
            /**
             * Find precision and scale from a decimal(x,y) property.
             */
            Matcher matcher = decimalPattern.matcher(propLower);

            if (matcher.matches()) {
                this.precision = Integer.parseInt(matcher.group(1));
                this.scale = Integer.parseInt(matcher.group(2));
            } else {
                this.precision = DEFAULT_DECIMAL_PRECISION;
                this.scale = DEFAULT_DECIMAL_SCALE;
            }
        }

        private void init() {
            if (this.name.isEmpty()) {
                throw new IllegalArgumentException("Name must not be empty.");
            }

            if (this.type != ByteBuffer.class && this.type != Double.class
                    && this.type != Float.class && this.type != Integer.class
                    && this.type != Long.class && this.type != String.class) {
                throw new IllegalArgumentException("Column " + this.name + " must be of type ByteBuffer, Double, Float, Integer, Long or String.");
            }

            for (String property : this.properties) {
                if (property == null) {
                    throw new IllegalArgumentException("Properties must not be null.");
                }

                if (property.isEmpty()) {
                    throw new IllegalArgumentException("Properties must not be empty.");
                }

                if (!this.isNullable && property.equals(ColumnProperty.NULLABLE)) {
                    this.isNullable = true;
                }
            }

            this.properties = Collections.unmodifiableList(this.properties);

            // Set the column type enumerations
            // --------------------------------
            // First the base types
            if( this.type == Integer.class) {
                this.columnType = ColumnType.INTEGER;
                this.columnBaseType = ColumnBaseType.INTEGER;
            } else if( this.type == Long.class) {
                this.columnType = ColumnType.LONG;
                this.columnBaseType = ColumnBaseType.LONG;
            } else if( this.type == Double.class) {
                this.columnType = ColumnType.DOUBLE;
                this.columnBaseType = ColumnBaseType.DOUBLE;
            } else if( this.type == Float.class) {
                this.columnType = ColumnType.FLOAT;
                this.columnBaseType = ColumnBaseType.FLOAT;
            } else if( this.type == String.class ) {
                this.columnType = ColumnType.STRING;
                this.columnBaseType = ColumnBaseType.STRING;
            } else if( this.type == ByteBuffer.class ) {
                this.columnType = ColumnType.BYTES;
                this.columnBaseType = ColumnBaseType.BYTES;
            }
            // Then, any sub-type based on properties
            if ( this.properties.contains( ColumnProperty.BOOLEAN ) ) {
                this.columnType = ColumnType.BOOLEAN;
            } else if ( this.properties.contains( ColumnProperty.INT8 ) ) {
                this.columnType = ColumnType.INT8;
            } else if ( this.properties.contains( ColumnProperty.INT16 ) ) {
                this.columnType = ColumnType.INT16;
            } else if ( this.properties.contains( ColumnProperty.TIMESTAMP ) ) {
                this.columnType = ColumnType.TIMESTAMP;
            } else if ( this.properties.contains( ColumnProperty.DATE ) ) {
                this.columnType = ColumnType.DATE;
            } else if ( this.properties.contains( ColumnProperty.TIME ) ) {
                this.columnType = ColumnType.TIME;
            } else if ( this.properties.contains( ColumnProperty.DATETIME ) ) {
                this.columnType = ColumnType.DATETIME;
            } else if ( this.properties.contains( ColumnProperty.CHAR1 ) ) {
                this.columnType = ColumnType.CHAR1;
            } else if ( this.properties.contains( ColumnProperty.CHAR2 ) ) {
                this.columnType = ColumnType.CHAR2;
            } else if ( this.properties.contains( ColumnProperty.CHAR4 ) ) {
                this.columnType = ColumnType.CHAR4;
            } else if ( this.properties.contains( ColumnProperty.CHAR8 ) ) {
                this.columnType = ColumnType.CHAR8;
            } else if ( this.properties.contains( ColumnProperty.CHAR16 ) ) {
                this.columnType = ColumnType.CHAR16;
            } else if ( this.properties.contains( ColumnProperty.CHAR32 ) ) {
                this.columnType = ColumnType.CHAR32;
            } else if ( this.properties.contains( ColumnProperty.CHAR64 ) ) {
                this.columnType = ColumnType.CHAR64;
            } else if ( this.properties.contains( ColumnProperty.CHAR128 ) ) {
                this.columnType = ColumnType.CHAR128;
            } else if ( this.properties.contains( ColumnProperty.CHAR256 ) ) {
                this.columnType = ColumnType.CHAR256;
            } else if ( this.properties.contains( ColumnProperty.IPV4 ) ) {
                this.columnType = ColumnType.IPV4;
            } else if ( this.properties.contains( ColumnProperty.JSON ) ) {
                this.columnType = ColumnType.JSON;
            } else if ( this.properties.contains( ColumnProperty.ULONG ) ) {
                this.columnType = ColumnType.ULONG;
            } else if ( this.properties.contains( ColumnProperty.UUID ) ) {
                this.columnType = ColumnType.UUID;
            } else if ( this.properties.contains( ColumnProperty.WKT ) ) {
                // Decide if it's WKT or WKB based on the base type
                if ( this.columnType == ColumnType.STRING ) {
                    this.columnType = ColumnType.WKT;
                } else if ( this.columnType == ColumnType.BYTES ) {
                    this.columnType = ColumnType.WKB;
                }
            } else {
                // Need to look through the properties list for startsWith Array or Vector
                for (String prop : this.properties)
                {
                    if (prop.startsWith(ColumnProperty.ARRAY)) {
                        this.columnType = ColumnType.ARRAY;
                        break;
                    } else if (prop.startsWith(ColumnProperty.VECTOR)) {
                        this.columnType = ColumnType.VECTOR;
                        break;
                    } else if (prop.startsWith(ColumnProperty.DECIMAL)) {
                        this.columnType = ColumnType.DECIMAL;
                        setDecimalInfo(prop.toLowerCase());
                        break;
                    }
                }
            }

        }   // end init

        /**
         * Gets the name of the column.
         *
         * @return  the name of the column
         */
        public String getName() {
            return this.name;
        }

        /**
         * Gets the Java data type of the column.
         *
         * @return  the Java data type of the column
         */
        public Class<?> getType() {
            return this.type;
        }

        /**
         * Gets the enumeration of the *base* type of the column.  This is far
         * more efficient than using {@link #getType()} and then comparing
         * it to various Java classes, e.g. Integer.class.  With this
         * enumeration, switch statements can be used to do different things for
         * different column types.  This enumeration is preferred when the
         * same thing needs to be done for all sub-types of a given base type.
         * For example, string has a lot of sub-types; rather than having to
         * group multiple case statements (and ensuring that no applicable
         * enumeration is missed) for {@link ColumnType}, this enumeration
         * can be used to cover all string cases.
         *
         * @return  the enumeration representing the *base* type of the column
         */
        public ColumnBaseType getColumnBaseType() {
            return this.columnBaseType;
        }

        /**
         * Gets the enumeration of the type of the column.  This is far
         * more efficient than using {@link #hasProperty(String)} to
         * check for given column properties.  With this enumeration,
         * switch statements can be used to do different things for
         * different column types.
         *
         * @return  the enumeration representing the type of the column
         */
        public ColumnType getColumnType() {
            return this.columnType;
        }

        public boolean isDecimal() {
            return this.columnType == ColumnType.DECIMAL;
        }

        public int getDecimalPrecision() {
            return this.precision;
        }

        public int getDecimalScale() {
            return this.scale;
        }

        /**
         * Gets whether the column is nullable.
         *
         * @return  whether the column is nullable
         */
        public boolean isNullable() {
            return this.isNullable;
        }

        /**
         * Determine if the column is an Array type
         *
         * @return True if the column is an Array
         */
        public boolean isArray()
        {
            for (String prop: getProperties())
                if (prop.startsWith(ColumnProperty.ARRAY))
                    return true;

            return false;
        }

        /**
         * Get the sub-type of the array
         *
         * @return The ColumnBaseType of the array sub-type.  Returns null if column is not an Array
         */
        public ColumnType getArrayType() throws GPUdbException
        {
            for (String prop: getProperties())
            {
                if (prop.startsWith(ColumnProperty.ARRAY))
                {
                    int open_index = prop.indexOf("(");
                    int close_index = prop.lastIndexOf(")");
                    if ((open_index >=0 ) && (close_index > open_index))
                    {
                        int comma_index = prop.indexOf(",", open_index);
                        if ((comma_index > open_index) && (comma_index < close_index))
                            close_index = comma_index;
                        String sub_type = prop.substring(open_index + 1, close_index).toLowerCase();
                        if (sub_type.startsWith("int"))
                            return ColumnType.INTEGER;
                        else if (sub_type.startsWith("bool"))
                            return ColumnType.BOOLEAN;
                        else if (sub_type.equals("long"))
                            return ColumnType.LONG;
                        else if (sub_type.equals("float"))
                            return ColumnType.FLOAT;
                        else if (sub_type.equals("double"))
                            return ColumnType.DOUBLE;
                        else if (sub_type.equals("string"))
                            return ColumnType.STRING;
                        else if (sub_type.equals("ulong"))
                            return ColumnType.ULONG;

                        throw new GPUdbException("Unknown array type: " + sub_type);
                    }
                }
            }

            return null;
        }

        /**
         * Gets the list of properties that apply to the column.
         *
         * @return  the list of properties that apply to the column
         *
         * @see ColumnProperty
         */
        public List<String> getProperties() {
            return this.properties;
        }

        /**
         * Determine if the column is a Vector type
         *
         * @return True if the column is a Vector type
         */
        public boolean isVector()
        {
            for (String prop: getProperties())
                if (prop.startsWith(ColumnProperty.VECTOR))
                    return true;

            return false;
        }

        /**
         * Get vector datatype dimensions, -1 if not a vector
         *
         * @return Size of the vector, -1 if not a vector
         */
        public int getVectorDimensions()
        {
            int dims = -1;
            for (String prop: getProperties())
            {
                if (prop.startsWith(ColumnProperty.VECTOR))
                {
                    int open_index = prop.indexOf("(");
                    int close_index = prop.lastIndexOf(")");
                    if ((open_index >=0 ) && (close_index > open_index))
                        dims = Integer.parseInt(prop.substring(open_index + 1, close_index));
                    break;
                }
            }

            return dims;
        }

        /**
         * Checks if the given property applies to the column.
         *
         * @return  boolean indicating whether the given property
         *          exists in the column's properties.
         *
         * @see ColumnProperty
         */
        public boolean hasProperty( String property ) {
            if (property == null)
                return false;
            return this.properties.contains( property );
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }

            Column that = (Column)obj;

            return that.name.equals(this.name)
                    && that.type == this.type
                    && that.properties.equals(this.properties);
        }

        @Override
        public int hashCode() {
            return (this.name.hashCode() * 31 + this.type.hashCode()) * 31 + this.properties.hashCode();
        }

        @Override
        public String toString() {
            GenericData gd = GenericData.get();
            StringBuilder builder = new StringBuilder();
            builder.append("{");
            builder.append("\"name\":").append(gd.toString(this.name)).append(",");
            builder.append("\"type\":\"").append(this.type.getSimpleName()).append("\",");
            builder.append("\"properties\":[");
            boolean output = false;

            for (String property : this.properties) {
                if (output) {
                    builder.append(",");
                }

                builder.append(gd.toString(property));
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
     * Creates a {@link Type} object containing metadata for an existing type
     * in GPUdb. Note that this method makes a request to GPUdb to obtain the
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


    /**
     * Creates a {@link Type} object with the specified dynamic schema metadata and
     * encoded dynamic table data.
     *
     * @param schemaString   a String object containing the dynamic schema
     * @param encodedData    the binary encoded data that contains metadata on
     *                       the dynamic schema
     * @return               the created {@link Type} object
     *
     * @throws GPUdbException if an error occurs during the processing of the metadata
     */
    public static Type fromDynamicSchema(String schemaString, ByteBuffer encodedData) throws GPUdbException {
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

        // The schema string will have, in addition to the dynamically generated records' type,
        // two more fields ('column_headers' and 'column_datatypes')
        int fieldCount = schema.getFields().size() - 2;
        // The field/expression names are stored in the second to last element
        // of the returned data
        @SuppressWarnings("unchecked")
        List<String> expressions = (List<String>)data.get(fieldCount);
        // The field types are stored in the very last element/list of the
        // returned data
        @SuppressWarnings("unchecked")
        List<String> types = (List<String>)data.get(fieldCount + 1);

        // The encoded response is structured column-based; so any element's
        // size (i.e. list size) gives the total number of records
        int recordCount = ((List<?>)data.get(0)).size();

        // The number of columns given in the encoded data must match
        // the given schema string
        if (expressions.size() < fieldCount)
        {
            throw new GPUdbException("Every field must have a corresponding expression.");
        }

        // The number of types must match the number fields given in the schema
        if (types.size() < fieldCount)
        {
            throw new GPUdbException("Every field must have a corresponding type.");
        }

        List<Type.Column> columns = new ArrayList<>();

        // Extract the column information from the given schema and the encoded data
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
                case ColumnProperty.BOOLEAN:
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
                case ColumnProperty.DATETIME:
                case ColumnProperty.INT8:
                case ColumnProperty.INT16:
                case ColumnProperty.IPV4:
                case ColumnProperty.JSON:
                case ColumnProperty.TIME:
                case ColumnProperty.TIMESTAMP:
                case ColumnProperty.ULONG:
                case ColumnProperty.UUID:
                case ColumnProperty.WKT:
                    columnProperties.add(type);
                    break;

                case "geometry":
                    // WKT types could be returned as 'geometry' by the server
                    columnProperties.add( ColumnProperty.WKT );
                    break;

                default:
                    if (type.startsWith(ColumnProperty.ARRAY))
                        columnProperties.add(type);
                    else if (type.startsWith(ColumnProperty.VECTOR))
                        columnProperties.add(type);
                    else if (type.startsWith(ColumnProperty.DECIMAL))
                        columnProperties.add(type);
            }

            columns.add(new Type.Column(name, columnType, columnProperties));
        }   // end for loop

        // Create the type from the extracted column information
        return new Type("", columns);
    }


    private transient String label;
    private transient List<Column> columns;
    private transient Map<String, Integer> columnMap;
    private transient Schema schema;

    /**
     * Creates a {@link Type} object with the specified column metadata and an
     * empty type label.
     *
     * @param columns  the list of columns that the type comprises
     *
     * @throws IllegalArgumentException if no columns are specified
     */
    public Type(Column... columns) {
        this("", Arrays.asList(columns));
    }

    /**
     * Creates a {@link Type} object with the specified column metadata and an
     * empty type label.
     *
     * @param columns  the list of columns that the type comprises
     *
     * @throws IllegalArgumentException if no columns are specified
     */
    public Type(List<Column> columns) {
        this("", columns);
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
        this.label = label;
        this.columns = columns == null ? new ArrayList<>() : new ArrayList<>(columns);
        init();
    }

    private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
        this.label = (String)stream.readObject();
        int columnCount = stream.readInt();
        this.columns = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            this.columns.add((Column)stream.readObject());
        }

        init();
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.writeObject(this.label);
        stream.writeInt(this.columns.size());

        for (Column column : this.columns) {
            stream.writeObject(column);
        }
    }

    private void init() {
        if (this.columns.isEmpty()) {
            throw new IllegalArgumentException("At least one column must be specified.");
        }

        this.columnMap = new HashMap<>();

        for (int i = 0; i < this.columns.size(); i++) {
            Column column = this.columns.get(i);
            String columnName = column.getName();

            if (this.columnMap.containsKey(columnName)) {
                throw new IllegalArgumentException("Duplicate column name " + columnName+ " specified.");
            }

            this.columnMap.put(columnName, i);
        }

        this.columns = Collections.unmodifiableList(this.columns);
        createSchema();
    }

    /**
     * Creates a {@link Type} object with column metadata from an Avro record
     * schema and an empty type label.
     *
     * @param typeSchema  the Avro record schema for the type
     *
     * @throws IllegalArgumentException if {@code typeSchema} is invalid or
     * contains unsupported field types
     */
    public Type(String typeSchema) {
        this("", typeSchema, null);
    }

    /**
     * Creates a {@link Type} object with metadata in the format returned from
     * the GPUdb /show/table or /show/types endpoints.
     *
     * @param label       a user-defined description string which can be used to
     *                    differentiate between data with otherwise identical
     *                    schemas
     * @param typeSchema  the Avro record schema for the type
     * @param properties  an optional map of column names to lists of properties
     *                    that apply to those columns
     *
     * @throws IllegalArgumentException if {@code typeSchema} is invalid or
     * contains unsupported field types, or if any {@code properties} are
     * invalid
     */
    public Type(String label, String typeSchema, Map<String, List<String>> properties) {
        this.label = label;
        this.columns = new ArrayList<>();
        this.columnMap = new HashMap<>();
        JsonNode root;

        try {
            root = MAPPER.readTree(typeSchema);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Schema is invalid.", ex);
        }

        JsonNode rootType = root.get("type");

        if (rootType == null || !"record".equals(rootType.textValue())) {
            throw new IllegalArgumentException("Schema must be of type record.");
        }

        JsonNode fields = root.get("fields");

        if (fields == null || !fields.isArray() || fields.isEmpty()) {
            throw new IllegalArgumentException("Schema has no fields.");
        }

        Iterator<JsonNode> fieldIterator = fields.elements();

        while (fieldIterator.hasNext()) {
            JsonNode field = fieldIterator.next();

            if (!field.isObject()) {
                throw new IllegalArgumentException("Schema has invalid field.");
            }

            JsonNode fieldName = field.get("name");

            if (fieldName == null || !fieldName.isTextual()) {
                throw new IllegalArgumentException("Schema has unnamed field.");
            }

            String columnName = fieldName.textValue();

            if (this.columnMap.containsKey(columnName)) {
                 throw new IllegalArgumentException("Duplicate field name " + columnName + ".");
            }

            JsonNode fieldType = field.get("type");

            if (fieldType == null) {
                throw new IllegalArgumentException("Field " + columnName + " has no type.");
            }

            if (!fieldType.isTextual() && !fieldType.isArray()) {
                throw new IllegalArgumentException("Field " + columnName + " has invalid type.");
            }

            String fieldTypeString = null;
            boolean isNullable = false;

            if (fieldType.isTextual()) {
                fieldTypeString = fieldType.textValue();
            } else {
                Iterator<JsonNode> fieldTypeIterator = fieldType.elements();

                while (fieldTypeIterator.hasNext()) {
                    JsonNode fieldTypeElement = fieldTypeIterator.next();
                    boolean valid = false;

                    if (fieldTypeElement.isTextual()) {
                        String fieldTypeElementString = fieldTypeElement.textValue();

                        if (fieldTypeString != null && fieldTypeElementString.equals("null")) {
                            valid = true;
                            isNullable = true;
                        } else if (fieldTypeString == null) {
                            fieldTypeString = fieldTypeElementString;
                            valid = true;
                        }
                    }

                    if (!valid) {
                        throw new IllegalArgumentException("Field " + columnName + " has invalid type.");
                    }
                }

                if (fieldTypeString == null) {
                    throw new IllegalArgumentException("Field " + columnName + " has invalid type.");
                }
            }

            Class<?> columnType;

            switch (fieldTypeString) {
                case "bytes":
                    columnType = ByteBuffer.class;
                    break;

                case "double":
                    columnType = Double.class;
                    break;

                case "float":
                    columnType = Float.class;
                    break;

                case "int":
                    columnType = Integer.class;
                    break;

                case "long":
                    columnType = Long.class;
                    break;

                case "string":
                    columnType = String.class;
                    break;

                default:
                    throw new IllegalArgumentException("Field " + columnName + " must be of type bytes, double, float, int, long or string.");
            }

            List<String> columnProperties = properties != null ? properties.get(columnName) : null;

            if (isNullable) {
                if (columnProperties == null) {
                    columnProperties = new ArrayList<>();
                    columnProperties.add(ColumnProperty.NULLABLE);
                } else if (!columnProperties.contains(ColumnProperty.NULLABLE)) {
                    columnProperties = new ArrayList<>(columnProperties);
                    columnProperties.add(ColumnProperty.NULLABLE);
                }
            }

            this.columns.add(new Column(columnName, columnType, columnProperties));
            this.columnMap.put(columnName, this.columns.size() - 1);
        }

        this.columns = Collections.unmodifiableList(this.columns);
        createSchema();
    }

    private void createSchema() {
        this.schema = Schema.createRecord("type_name", null, null, false);
        List<Field> fields = new ArrayList<>();
        HashSet<String> fieldNames = new HashSet<>();

        for (int i = 0; i < this.columns.size(); i++) {
            Column column = this.columns.get(i);
            String columnName = column.getName();
            StringBuilder fieldNameBuilder = new StringBuilder(columnName);

            for (int j = 0; j < fieldNameBuilder.length(); j++) {
                char c = fieldNameBuilder.charAt(j);

                if (!(j > 0 && c >= '0' && c <= '9')
                        && !(c >= 'A' && c <= 'Z')
                        && !(c >= 'a' && c <= 'z')
                        && c != '_') {
                    fieldNameBuilder.setCharAt(j, '_');
                }
            }

            String fieldName;

            for (int n = 1; ; n++) {
                fieldName = fieldNameBuilder.toString() + (n > 1 ? "_" + n : "");
                Integer columnIndex = this.columnMap.get(fieldName);

                if ((columnIndex == null || columnIndex == i)
                        && !fieldNames.contains(fieldName)) {
                    break;
                }
            }

            Class<?> columnType = column.getType();
            Schema fieldSchema;

            if (columnType == ByteBuffer.class) {
                fieldSchema = Schema.create(Schema.Type.BYTES);
            } else if (columnType == Double.class) {
                fieldSchema = Schema.create(Schema.Type.DOUBLE);
            } else if (columnType == Float.class) {
                fieldSchema = Schema.create(Schema.Type.FLOAT);
            } else if (columnType == Integer.class) {
                fieldSchema = Schema.create(Schema.Type.INT);
            } else if (columnType == Long.class) {
                fieldSchema = Schema.create(Schema.Type.LONG);
            } else if (columnType == String.class) {
                fieldSchema = Schema.create(Schema.Type.STRING);
            } else {
                throw new IllegalArgumentException("Column " + columnName + " must be of type ByteBuffer, Double, Float, Integer, Long or String.");
            }

            if (column.isNullable()) {
                fieldSchema = Schema.createUnion(fieldSchema, Schema.create(Schema.Type.NULL));
            }

            Field field = new Field(fieldName, fieldSchema, null, (Object)null);
            fields.add(field);
            fieldNames.add(fieldName);
        }

        this.schema.setFields(fields);
    }

    /**
     * Gets the user-defined description string which can be used to
     * differentiate between data with otherwise identical schemas.
     *
     * @return  the label string
     */
    public String getLabel() {
        return this.label;
    }

    /**
     * Gets the list of columns that the type comprises.
     *
     * @return  the list of columns that the type comprises
     */
    public List<Column> getColumns() {
        return this.columns;
    }

    /**
     * Gets the column with the specified index.
     *
     * @param index  the column index
     * @return       the column with the specified index
     *
     * @throws IndexOutOfBoundsException if the specified index is out of range
     */
    public Column getColumn(int index) {
        return this.columns.get(index);
    }

    /**
     * Gets the column with the specified name.
     *
     * @param name  the column name
     * @return      the column with the specified name, or null if no such
     *              column exists
     */
    public Column getColumn(String name) {
        Integer index = this.columnMap.get(name);
        return index == null ? null : this.columns.get(index);
    }

    /**
     * Gets the number of columns.
     *
     * @return  the number of columns
     */
    public int getColumnCount() {
        return this.columns.size();
    }

    /**
     * Gets the index of the column with the specified name.
     *
     * @param name  the column name
     * @return      the index of the column with the specified name, or -1 if no
     *              such column exists
     */
    public int getColumnIndex(String name) {
        Integer result = this.columnMap.get(name);
        return result == null ? -1 : result;
    }

    /**
     * Internal helper for getColumnIndex() for the specified name which will throw if name not present.
     *
     * @param name  the column name
     * @return      the index of the column with the specified name
     *
     * @throws IllegalArgumentException if column name does not exist
     */
    int getColumnIndexOrThrow(String name) throws IllegalArgumentException {
        Integer result = this.columnMap.get(name);

        if (result == null)
            throw new IllegalArgumentException("Field '" + name + "'' does not exist.");

        return result;
    }

    /**
     * Gets the Avro record schema for the type.
     *
     * @return  the Avro record schema for the type
     */
    public Schema getSchema() {
        return this.schema;
    }

    /**
     * Creates a new {@link Record} based on the type.
     *
     * @return  a new {@link Record}
     */
    public Record newInstance() {
        return new GenericRecord(this);
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
        ObjectNode root = MAPPER.createObjectNode();
        root.put("type", "record");
        root.put("name", "type_name");
        ArrayNode fields = MAPPER.createArrayNode();
        LinkedHashMap<String, List<String>> properties = new LinkedHashMap<>();

        for (Column column : this.columns) {
            ObjectNode field = MAPPER.createObjectNode();
            String columnName = column.getName();
            field.put("name", columnName);
            Class<?> columnType = column.getType();
            String columnTypeString;

            if (columnType == ByteBuffer.class) {
                columnTypeString = "bytes";
            } else if (columnType == Double.class) {
                columnTypeString = "double";
            } else if (columnType == Float.class) {
                columnTypeString = "float";
            } else if (columnType == Integer.class) {
                columnTypeString = "int";
            } else if (columnType == Long.class) {
                columnTypeString = "long";
            } else if (columnType == String.class) {
                columnTypeString = "string";
            } else {
                throw new IllegalArgumentException("Column " + columnName + " must be of type ByteBuffer, Double, Float, Integer, Long or String.");
            }

            if (column.isNullable())
            {
                ArrayNode fieldArray = MAPPER.createArrayNode();
                fieldArray.add(columnTypeString);
                fieldArray.add("null");
                field.set("type", fieldArray);
            }
            else
            {
                field.put("type", columnTypeString);
            }

            fields.add(field);

            List<String> columnProperties = column.getProperties();

            if (!columnProperties.isEmpty()) {
                properties.put(columnName, columnProperties);
            }
        }

        root.set("fields", fields);
        return gpudb.createType(root.toString(), this.label, properties, null).getTypeId();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        Type that = (Type)obj;

        return that.label.equals(this.label)
                && that.columns.equals(this.columns);
    }

    @Override
    public int hashCode() {
        return this.label.hashCode() * 31 + this.columns.hashCode();
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append("\"label\":").append(gd.toString(this.label)).append(",");
        builder.append("\"columns\":[");
        boolean output = false;

        for (Column column : this.columns) {
            if (output) {
                builder.append(",");
            }

            builder.append(column.toString());
            output = true;
        }

        builder.append("]}");
        return builder.toString();
    }
}
