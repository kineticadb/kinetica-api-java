package com.gpudb;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.IntStream;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.OffsetTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeFormatterBuilder;
import org.threeten.bp.format.DateTimeParseException;
import org.threeten.bp.temporal.ChronoField;
import org.threeten.bp.temporal.TemporalAccessor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpudb.Type.Column.ColumnType;
import com.gpudb.util.json.JsonUtils;

/**
 * Abstract class that provides default implementations of most methods of
 * {@link Record}. Derived classes must at a minimum implement the
 * {@link #getType()}, {@link #get(int)}, and {@link #put(int, Object)} methods.
 */
public abstract class RecordBase implements Record {
    // Accepted time formats
    private final static transient String timeFormat =
        "HH:mm[:ss][.S[S[S]]][ ][XXX][Z][z][VV][x]";
    private final static transient DateTimeFormatter acceptedTimeFormat =
        new DateTimeFormatterBuilder().appendPattern( "HH:mm[:ss]" )
        .appendOptional( // optional fraction of second
                        new DateTimeFormatterBuilder()
                        .appendFraction( ChronoField.MICRO_OF_SECOND, 0, 6, true)
                        .toFormatter() )
        .appendOptional( // optional timezone related information
                        new DateTimeFormatterBuilder()
                        .appendPattern( "[ ][XXX][Z][z][VV][x]" )
                        .toFormatter() )
        .toFormatter();
    
    // Date patterns used around the world
    private final static transient String datePatternYMD = "yyyy[-][/][.]MM[-][/][.]dd";
    private final static transient String datePatternMDY = "MM[-][/][.]dd[-][/][.]yyyy";
    private final static transient String datePatternDMY = "dd[-][/][.]MM[-][/][.]yyyy";

    private final static transient DateTimeFormatter acceptedDateTimeFormats[] =
    { new DateTimeFormatterBuilder().appendPattern( datePatternYMD + "[ ]['T']" )
        .appendOptional( acceptedTimeFormat ).toFormatter(),
      new DateTimeFormatterBuilder().appendPattern( datePatternMDY + "[ ]['T']" )
        .appendOptional( acceptedTimeFormat ).toFormatter(),
      new DateTimeFormatterBuilder().appendPattern( datePatternDMY + "[ ]['T']" )
        .appendOptional( acceptedTimeFormat ).toFormatter()
    };

    // Kinetica-specific date, time, and datetime formats (can ingest with only
    // these formats)
    private final static transient DateTimeFormatter kineticaDateFormat =
        DateTimeFormatter.ofPattern( "yyyy-MM-dd" );
    private final static transient DateTimeFormatter kineticaTimeFormat =
        DateTimeFormatter.ofPattern( "HH:mm:ss.SSS" );
    private final static transient DateTimeFormatter kineticaDateTimeFormat =
        DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss.SSS" );

    private final class RecordEntry implements Map.Entry<String, Object> {
        private final int index;

        public RecordEntry(int index) {
            this.index = index;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }

            Map.Entry<?, ?> e = (Map.Entry)o;

            Object k1 = getKey();
            Object k2 = e.getKey();

            if (k1 == k2 || (k1 != null && k1.equals(k2))) {
                Object v1 = getValue();
                Object v2 = e.getValue();

                if (v1 == v2 || (v1 != null && v1.equals(v2))) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public String getKey() {
            return RecordBase.this.getType().getColumn(index).getName();
        }

        @Override
        public Object getValue() {
            return RecordBase.this.get(index);
        }

        @Override
        public int hashCode() {
            Object value = getValue();
            return (getKey().hashCode() ^ (value == null ? 0 : value.hashCode()));
        }

        @Override
        public Object setValue(Object value) {
            Object oldValue = RecordBase.this.get(index);
            RecordBase.this.put(index, value);
            return oldValue;
        }

        @Override
        public String toString() {
            GenericData gd = GenericData.get();
            return gd.toString(getKey()) + ":" + gd.toString(getValue());
        }
    }

    private final class RecordEntryIterator implements Iterator<Map.Entry<String, Object>> {
        private int pos = 0;

        @Override
        public boolean hasNext() {
            return pos < RecordBase.this.getType().getColumnCount();
        }

        @Override
        public Map.Entry<String, Object> next() {
            return new RecordEntry(pos++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private final class RecordEntrySet extends AbstractSet<Map.Entry<String, Object>> {
        @Override
        public boolean add(Map.Entry<String, Object> o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends Map.Entry<String, Object>> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Iterator<Map.Entry<String, Object>> iterator() {
            return new RecordEntryIterator();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return RecordBase.this.getType().getColumnCount();
        }
    }

    private final class RecordMap extends AbstractMap<String, Object> {
        private final RecordEntrySet entrySet = new RecordEntrySet();

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsKey(Object key) {
            if (key == null) {
                throw new NullPointerException("Key cannot be null.");
            }

            if (!(key instanceof String)) {
                throw new ClassCastException("Key must be a string.");
            }

            return RecordBase.this.getType().getColumn((String)key) != null;
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return entrySet;
        }

        @Override
        public Object get(Object key) {
            if (key == null) {
                throw new NullPointerException("Key cannot be null.");
            }

            if (!(key instanceof String)) {
                throw new ClassCastException("Key must be a string.");
            }

            int columnIndex = RecordBase.this.getType().getColumnIndex((String)key);

            if (columnIndex == -1) {
                return null;
            }

            return RecordBase.this.get(columnIndex);
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Object put(String key, Object value) {
            if (key == null) {
                throw new NullPointerException("Key cannot be null.");
            }

            int columnIndex = RecordBase.this.getType().getColumnIndexOrThrow(key);
            Object oldValue = RecordBase.this.get(columnIndex);
            RecordBase.this.put(columnIndex, value);
            return oldValue;
        }

        @Override
        public Object remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return RecordBase.this.getType().getColumnCount();
        }

        @Override
        public String toString() {
            return RecordBase.this.toString();
        }
    }

    @Override
    public Schema getSchema() {
        return getType().getSchema();
    }

    @Override
    public Object get(String name) {
        int index = getType().getColumnIndex(name);

        if (index == -1) {
            return null;
        }

        return get(index);
    }

     /**
     * For string columns with array property, return a native Java array of the appropriate type:
     * int[], long[], float[], double[], or String[].  For binary columns with vector property,
     * return float[].
     *
     * @param name   The name of the column.
     *
     * @throws GPUdbException              If an error occurs during the operation.
     * @throws JsonProcessingException
     * @throws JsonMappingException
     */
    public Object getArray(String name) throws Exception
    {
        int index = getType().getColumnIndexOrThrow(name);
        return getArray(index);
    }

    /**
     * For string columns with array property, return a native Java array of the appropriate type:
     * boolean[], int[], long[], float[], double[], or String[].
     * For binary columns with vector property, return float[].
     *
     * @param index  The index of the column.
     *
     * @throws GPUdbException              If an error occurs during the operation.
     * @throws JsonProcessingException
     * @throws JsonMappingException
     */
    public Object getArray(int index) throws Exception
    {
        Type.Column column = getType().getColumns().get(index);
        if (column.isVector())
            return getVector(index); // returns float[]

        ColumnType array_type = column.getArrayType();
        ObjectMapper om = new ObjectMapper();
        switch (array_type)
        {
        case BOOLEAN:
            return om.readValue(getString(index), boolean[].class);
        case INTEGER:
            return om.readValue(getString(index), int[].class);
        case LONG:
            return om.readValue(getString(index), long[].class);
        case FLOAT:
            return om.readValue(getString(index), float[].class);
        case DOUBLE:
            return om.readValue(getString(index), double[].class);
        case STRING:
            return om.readValue(getString(index), String[].class);
        default:
            throw new GPUdbException("Unknown array type: " + array_type);
        }
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

    /**
     * For string columns with JSON property, return a Java Map representing the JSON object.
     *
     * @param name  The name of the column.
     *
     * @throws GPUdbException              If an error occurs during the operation.
     * @throws JsonProcessingException
     * @throws JsonMappingException
     */
    public Map<String, Object> getJson(String name) throws Exception
    {
        int index = getType().getColumnIndexOrThrow(name);
        return getJson(index);
    }

    /**
     * For string columns with JSON property, return a Java Map representing the JSON object.
     *
     * @param index  The index of the column.
     *
     * @throws GPUdbException              If an error occurs during the operation.
     * @throws JsonProcessingException
     * @throws JsonMappingException
     */
    public Map<String, Object> getJson(int index) throws Exception
    {
        return new ObjectMapper().readValue(getString(index), new TypeReference<Map<String,Object>>(){});
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

     /**
     * For byte columns with a vector property, return a native Java array of float.
     *
     * @param name   The name of the column.
     * @param value  The value to be parsed (based on the given column's type).
     *
     * @throws GPUdbException              If an error occurs during the operation.
     * @throws JsonProcessingException
     * @throws JsonMappingException
     */
    public float[] getVector(String name) throws Exception
    {
        int index = getType().getColumnIndexOrThrow(name);
        return getVector(index);
    }

    /**
     * For byte columns with a vector property, return a native Java array of float.
     *
     * @param name   The name of the column.
     * @param value  The value to be parsed (based on the given column's type).
     *
     * @throws GPUdbException              If an error occurs during the operation.
     * @throws JsonProcessingException
     * @throws JsonMappingException
     */
    public float[] getVector(int index) throws Exception // Class<T> type
    {
        int dims = getType().getColumn(index).getVectorDimensions();
        if (dims < 0)
            throw new GPUdbException("Not a vector column");

        float[] fArray = new float[dims];
        ByteBuffer buff = getBytes(index);
        buff.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(fArray);

        return fArray;
    }

    @Override
    public void put(String name, Object value) {
        int index = getType().getColumnIndexOrThrow(name);
        put(index, value);
    }

     /**
     * For string columns with array property, parse the value parameter which may be a
     * String, List<?>, int[], Integer[], long[], Long[], float[], Float[], double[], Double[].
     * If the value is not of a relevant type, throw an error.
     *
     * @param name   The name of the column.
     * @param value  The value to be parsed (based on the given column's type).
     *
     * @throws GPUdbException  if an error occurs during the operation.
     */
    public void putArray(String name, Object value) throws GPUdbException {
        int index = getType().getColumnIndexOrThrow(name);
        putArray(index, value);
    }

     /**
     * For string columns with array property, parse the value parameter which may be a
     * String, List<?>, int[], Integer[], long[], Long[], float[], Float[], double[], Double[].
     * If the value is not of a relevant type, throw an error.
     *
     * @param index  The index of the column.
     * @param value  The value to be parsed (based on the given column's type).
     *
     * @throws GPUdbException  if an error occurs during the operation.
     */
    public void putArray(int index, Object value) throws GPUdbException {
        if (value == null)
            put(index, null);
        else if (value instanceof String)
            put(index, value);
        else
            put(index, JsonUtils.toJsonString(value));
    }

     /**
     * For Byte columns, parse the value parameter which may be a
     * ByteBuffer or byte[].
     * If the value is not of a relevant type, throw an error.
     *
     * @param name   The name of the column.
     * @param value  The value to be set.
     *
     * @throws GPUdbException  if an error occurs during the operation.
     */
    public void putBytes(String name, Object value) throws GPUdbException {
        int index = getType().getColumnIndexOrThrow(name);
        putBytes(index, value);
    }

     /**
     * For Byte columns, parse the value parameter which may be a
     * ByteBuffer or byte[].
     * If the value is not of a relevant type, throw an error.
     *
     * @param index  The index of the column.
     * @param value  The value to be set.
     *
     * @throws GPUdbException  if an error occurs during the operation.
     */
    public void putBytes(int index, Object value) throws GPUdbException {
        if ((value == null) || (value instanceof ByteBuffer))
            put(index, value);
        else if (value instanceof byte[])
            put(index, ByteBuffer.wrap((byte[])value));
        else
            throw new GPUdbException("Unsupported type for Bytes: " + value.getClass().getName());
    }

    /**
     * For string columns with date, time, or datetime property, parse the
     * string and convert to the appropriate Kinetica format using the system
     * timezone.  If the column is not of a relevant type, set the value
     * without any parsing (so that any string or other typed values can be set
     * using this method without resorting to first check the column's type
     * before calling it).
     *
     * Caveat is that due to string manipulation, this is considerably slower
     * than {@link #put}.  So, use this method only if you know that a
     * non-Kinetica date/time/datetime format is being used.
     *
     * @param name      The name of the column.
     * @param value     The value to be parsed (based on the given column's type).
     *
     * @throws GPUdbException  if an error occurs during the operation.
     */
    public void putDateTime(String name, Object value) throws GPUdbException {
        putDateTime( name, value, null );
    }

    /**
     * For string columns with date, time, or datetime property, parse the
     * string and convert to the appropriate Kinetica format using the given
     * timezone (system timezone if none given).  If the column is not of a
     * relevant type, set the value without any parsing (so that any string or
     * other typed values can be set using this method without resorting to
     * first check the column's type before calling it).
     *
     * Caveat is that due to string manipulation, this is considerably slower
     * than {@link #put}.  So, use this method only if you know that a
     * non-Kinetica date/time/datetime format is being used.
     *
     * @param name      The name of the column.
     * @param value     The value to be parsed (based on the given column's type).
     * @param timezone  Optional parameter specifying the timezone to use for
     *                  parsing the given value.  If null, the system timezone is
     *                  used.
     *
     * @throws GPUdbException  if an error occurs during the operation.
     */
    public void putDateTime(String name, Object value, TimeZone timezone) throws GPUdbException {
        int index = getType().getColumnIndexOrThrow(name);
        putDateTime(index, value, timezone);
    }

    /**
     * For string columns with date, time, or datetime property, parse the
     * string and convert to the appropriate Kinetica format using the system
     * timezone.  If the column is not of a relevant type, throw an error.
     *
     * Caveat is that due to string manipulation, this is considerably slower
     * than {@link #put}.  So, use this method only if you know that a
     * non-Kinetica date/time/datetime format is being used.
     *
     * @param index  The index of the column.
     * @param value  The value to be parsed (based on the given column's type).
     *
     * @throws GPUdbException  if an error occurs during the operation.
     */
    public void putDateTime(int index, Object value) throws GPUdbException {
        putDateTime( index, value, null );
    }

    /**
     * For string columns with date, time, or datetime property, parse the
     * string and convert to the appropriate Kinetica format using the given
     * timezone (system timezone if none given).  If the column is not of a
     * relevant type, set the value without any parsing (so that any string or
     * other typed values can be set using this method without resorting to
     * first check the column's type before calling it).
     *
     * Caveat is that due to string manipulation, this is considerably slower
     * than {@link #put}.  So, use this method only if you know that a
     * non-Kinetica date/time/datetime format is being used.
     *
     * The following patterns are accepted for date and datetime columns:
     * 1) yyyy[-][/][.]MM[-][/][.]dd[ ]['T'][HH:mm[:ss][.S[S][S][S][S][S]][ ][XXX][Z][z][VV][x]]
     * 2) MM[-][/][.]dd[-][/][.]yyyy[ ]['T'][HH:mm[:ss][.S[S][S][S][S][S]][ ][XXX][Z][z][VV][x]]
     * 3) dd[-][/][.]MM[-][/][.]yyyy[ ]['T'][HH:mm[:ss][.S[S][S][S][S][S]][ ][XXX][Z][z][VV][x]]
     * The following pattern is accepted by time-type columns:
     *   HH:mm[:ss][.S[S][S][S][S][S]][ ][XXX][Z][z][VV][x]
     *
     * In other words, the date component can be any of YMD, MDY, or DMY pattern
     * withh '-', '.', or '/' as the separator.  And, the time component must have
     * hours and minutes, but can optionally have seconds, fraction of a second
     * (up to six digits) and some form of a timezone identifier.
     *
     * @param index     The index of the column.
     * @param value     The value to be parsed (based on the given column's type).
     * @param timezone  Optional parameter specifying the timezone to use for
     *                  parsing the given value.  If null, the system timezone is
     *                  used.
     *
     * @throws GPUdbException  if an error occurs during the operation.
     */
    public void putDateTime(int index, Object value, TimeZone timezone) throws GPUdbException {
        Type.Column column = getType().getColumns().get( index );

        // Handle null values
        if (value == null) {
            put(index, value);
            return;
        }
        
        // Need a string column for this to work
        if ( column.getType() != String.class ) {
            throw new GPUdbException( "Need a string-type column; got "
                                      + column.getType().toString() );
        }

        // Use the system default timezone if none given
        if ( timezone == null ) {
            timezone = TimeZone.getDefault();
        }
        
        String stringValue = null;
        try {
            switch ( column.getColumnType() ) {
                
                case DATE: {
                    // Try to parse the date with one of the formats at a time
                    for ( int i = 0; i < acceptedDateTimeFormats.length; ++i ) {
                        try {
                            TemporalAccessor ta = acceptedDateTimeFormats[i].parseBest( (String)value,
                                                                                        ZonedDateTime.FROM,
                                                                                        LocalDateTime.FROM,
                                                                                        LocalDate.FROM );
                            // See if this format matched the input
                            if ( ta instanceof ZonedDateTime ) {
                                // Get the appropriate zone ID
                                ZoneId zone = (ZoneId)(ZoneOffset.ofTotalSeconds( timezone.getRawOffset() / 1000 ) );
                                // Parse the datetime local to the given time zone
                                // and then extract just the date
                                stringValue = ZonedDateTime.from( ta )
                                    .withZoneSameInstant( zone )
                                    .toLocalDateTime()
                                    .format( kineticaDateFormat );
                                break; // no need to try other formats
                            } else if ( ta instanceof LocalDateTime ) {
                                // Parse the datetime as is (since there is no
                                // timezone specified in the string itself) and
                                // then just extract the date
                                stringValue = LocalDateTime.from( ta ).format( kineticaDateFormat );
                                break; // no need to try other formats
                            } else if ( ta instanceof LocalDate ) {
                                // Parse the datetime as is (no time component
                                // given)
                                stringValue = LocalDate.from( ta ).format( kineticaDateFormat );
                                break; // no need to try other formats
                            }
                            // Didn't match this format; try the next one (next iteration)
                        } catch (DateTimeParseException ignore) {
                            // Just ignore any parse exception
                        }
                    }

                    if ( stringValue == null ) {
                        // Did not match any format
                        throw new GPUdbException("Value '" + (String)value + "' for column '"
                                                 + column.getName() + "' with sub-type "
                                                 + "'date' did not match any pattern");
                    } else {
                        // Value was successfully parsed
                        put(index, stringValue);
                    }
                    break;
                }

                case TIME: {
                    // Parse the input time string with the best matching format
                    TemporalAccessor ta = acceptedTimeFormat.parseBest( (String)value,
                                                                        OffsetTime.FROM,
                                                                        LocalTime.FROM );
                    if ( ta instanceof OffsetTime ) {
                        // Got an offset; but first parse the time as is
                        OffsetTime offsetTime = OffsetTime.from( ta );
                        // Get the time at the specified zone
                        stringValue = offsetTime
                            .withOffsetSameInstant( ZoneOffset.ofTotalSeconds( timezone.getRawOffset() / 1000 ) )
                            .toLocalTime()
                            .format( kineticaTimeFormat );
                    } else if ( ta instanceof LocalTime ) {
                        // Parse the time as is (since there is no timezone specified
                        // in the string itself)
                        stringValue = LocalTime.from( ta ).format( kineticaTimeFormat );
                    } else {
                        throw new GPUdbException("Value '" + (String)value + "' for column '"
                                                 + column.getName() + "' with sub-type "
                                                 + "'time' did not match any pattern");
                    }

                    // Value was successfully parsed
                    put(index, stringValue);
                    break;
                }

                case DATETIME: {
                    // Try to parse the datetime with one of the formats at a time
                    for ( int i = 0; i < acceptedDateTimeFormats.length; ++i ) {
                        try {
                            TemporalAccessor ta = acceptedDateTimeFormats[i].parseBest( (String)value,
                                                                                        ZonedDateTime.FROM,
                                                                                        LocalDateTime.FROM,
                                                                                        LocalDate.FROM );
                            // See if this format matched the input
                            if ( ta instanceof ZonedDateTime ) {
                                // Get the appropriate zone ID
                                ZoneId zone = (ZoneId)(ZoneOffset.ofTotalSeconds( timezone.getRawOffset() / 1000 ) );
                                // Parse the datetime local to the given time zone
                                stringValue = ZonedDateTime.from( ta )
                                    .withZoneSameInstant( zone )
                                    .toLocalDateTime()
                                    .format( kineticaDateTimeFormat );
                                break; // no need to try other formats
                            } else if ( ta instanceof LocalDateTime ) {
                                // Parse the datetime as is (since there is no timezone specified
                                // in the string itself)
                                stringValue = LocalDateTime.from( ta ).format( kineticaDateTimeFormat );
                                break; // no need to try other formats
                            } else if ( ta instanceof LocalDate ) {
                                // Parse the datetime as is (no time component given) and append
                                // a "beginning of the day" timestamp
                                stringValue = LocalDate.from( ta ).format( kineticaDateFormat )
                                    + " 00:00:00.000";
                                break; // no need to try other formats
                            }
                            // Didn't match this format; try the next format
                        } catch (DateTimeParseException ignore) {
                            // Just ignore any parse exception
                        }
                    }

                    if ( stringValue == null ) {
                        // Did not match any format
                        throw new GPUdbException("Value '" + (String)value + "' for column '"
                                                 + column.getName() + "' with sub-type "
                                                 + "'datetime' did not match any pattern");
                    } else {
                        // Value was successfully parsed
                        put(index, stringValue);
                    }
                    break;
                }

                default:
                    // For any other data types, just set the value
                    put(index, value);
                    break;
            }
        } catch ( DateTimeParseException ex ) {
            throw new GPUdbException ( "Failed to parse value '"
                                       + value.toString()
                                       + "' to a date/time/datetime value: "
                                       + ex.toString(),
                                       ex );
        }
    }   // end putDateTime

     /**
     * For String columns with json property, parse the value parameter which may be a
     * String, or Map<String, String>.
     * If the column is not of a relevant type, throw an error.
     *
     * @param name   The name of the column.
     * @param value  The value to be parsed (based on the given column's type).
     *
     * @throws GPUdbException  if an error occurs during the operation.
     */
    public void putJson(String name, Object value) throws GPUdbException {
        int index = getType().getColumnIndexOrThrow(name);
        putJson(index, value);
    }

     /**
     * For String columns with json property, parse the value parameter which may be a
     * String, or Map<String, String>.
     * If the column is not of a relevant type, throw an error.
     *
     * @param name   The name of the column.
     * @param value  The value to be parsed (based on the given column's type).
     *
     * @throws GPUdbException  if an error occurs during the operation.
     */
    public void putJson(int index, Object value) throws GPUdbException {
        if ((value == null) || (value instanceof String))
            put(index, value);
        else
            put(index, JsonUtils.toJsonString(value));
    }

     /**
     * For bytes columns with vector property, parse the value property which may be a
     * string, float[], Float[] or List<Float>.
     * If the column is not of a relevant type, throw an error.
     *
     * @param name   The name of the column.
     * @param value  The value to be parsed (based on the given column's type).
     *
     * @throws GPUdbException  if an error occurs during the operation.
     */
    public void putVector(String name, Object value) throws GPUdbException {
        int index = getType().getColumnIndexOrThrow(name);
        putVector(index, value);
    }

     /**
     * For bytes columns with vector property, parse the value parameter which may be a
     * string, float[], Float[] or List<Float>.
     * If the column is not of a relevant type, throw an error.
     *
     * @param index  The index of the column.
     * @param value  The value to be parsed (based on the given column's type).
     *
     * @throws GPUdbException  if an error occurs during the operation.
     */
    public void putVector(int index, Object value) throws GPUdbException {
        int dims = getType().getColumn(index).getVectorDimensions();
        if (dims < 0)
            throw new GPUdbException("Not a vector column");

        float[] fArray;
        if (value instanceof String)
        {
            // Its a vector datatyped field. Read the json array value
            try {
                fArray = new ObjectMapper().readValue(value.toString(), float[].class);
            } catch (JsonProcessingException e) {
                throw new GPUdbException("Error reading vector string", e);
            }
        }
        else if (value instanceof float[])
        {
            fArray = (float[])value;
        }
        else if ((value instanceof Float[]) || 
                    ((value instanceof List<?>) &&
                    (((List<?>)value).isEmpty() || (((List<?>)value).get(0) instanceof Float))))
        {
            Float[] fv;
            if (value instanceof Float[])
                fv = (Float[])value;
            else if (value instanceof List<?>)
                fv = ((List<?>)value).toArray(new Float[0]);
            else
                throw new GPUdbException("Unsupported type for vector: " + value.getClass().getName());

            fArray = new float[fv.length];
            IntStream.range(0, fv.length).forEach(i -> fArray[i] = fv[i]);
        }
        else
            throw new GPUdbException("Unsupported type for vector: " + value.getClass().getName());

        if (fArray.length != dims)
            throw new GPUdbException("Unmatched vector dimensions: found " + fArray.length + " but expected " + dims);

        byte[] buff = new byte[Float.BYTES * fArray.length];
        ByteBuffer.wrap(buff).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().put(fArray);

        putBytes(index, buff);
    }

    @Override
    public Map<String, Object> getDataMap() {
        return new RecordMap();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        RecordBase that = (RecordBase)obj;

        if (!that.getType().equals(this.getType())) {
            return false;
        }

        int columnCount = this.getType().getColumnCount();

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
        int columnCount = getType().getColumnCount();

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
        Type type = getType();
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