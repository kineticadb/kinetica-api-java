package com.gpudb;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.OffsetTime;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeFormatterBuilder;
import org.threeten.bp.format.DateTimeParseException;
import org.threeten.bp.temporal.ChronoField;
import org.threeten.bp.temporal.TemporalAccessor;


/**
 * An object that contains {@link Record} data based on a GPUdb {@link Type}
 * specified at runtime. GPUdb functions that return non-dynamic data will use
 * generic records by default in the absence of a specified type descriptor or
 * known type.
 */
public final class GenericRecord extends RecordBase implements Serializable  {
    private static final long serialVersionUID = 1L;

    private transient Type type;
    private transient Object[] values;

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

    
    /**
     * Creates a new generic record based on the specified GPUdb {@link Type}.
     * Note that generic records can also be created using {@link
     * Type#newInstance}.
     *
     * @param type  the GPUdb type
     */
    public GenericRecord(Type type) {
        this.type = type;
        values = new Object[type.getColumnCount()];
    }

    private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
        type = (Type)stream.readObject();
        values = new Object[type.getColumnCount()];

        for (int i = 0; i < values.length; i++) {
            values[i] = stream.readObject();
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.writeObject(type);

        for (Object obj : values) {
            stream.writeObject(obj);
        }
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Object get(int index) {
        return values[index];
    }

    @Override
    public void put(int index, Object value) {
        values[index] = value;
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
        int index = getType().getColumnIndex(name);

        if (index == -1) {
            throw new IllegalArgumentException("Field " + name + " does not exist.");
        }

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
        Type.Column column = type.getColumns().get( index );

        // Handle null values
        if (value == null) {
            values[index] = value;
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
                        values[index] = stringValue;
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
                    values[index] = stringValue;
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
                        values[index] = stringValue;
                    }
                    break;
                }

                case INTEGER:
                case LONG:
                case FLOAT:
                case DOUBLE:
                case STRING:
                case BYTES:
                case INT8:
                case INT16:
                case TIMESTAMP:
                case DECIMAL:
                case CHAR1:
                case CHAR2:
                case CHAR4:
                case CHAR8:
                case CHAR16:
                case CHAR32:
                case CHAR64:
                case CHAR128:
                case CHAR256:
                case IPV4:
                case WKT:
                case WKB:
                    // For any other data types, just set the value
                    values[index] = value;
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


}
