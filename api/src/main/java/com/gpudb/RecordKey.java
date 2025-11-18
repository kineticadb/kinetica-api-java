package com.gpudb;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for building shard keys
 */
final class RecordKey {
    private static final Pattern DATE_REGEX = Pattern.compile("\\A(\\d{4})-(\\d{2})-(\\d{2})$");
    private static final Pattern DATETIME_REGEX = Pattern.compile("\\A(\\d{4})-(\\d{2})-(\\d{2}) (\\d{1,2}):(\\d{2}):(\\d{2})(?:\\.(\\d{3}))?$");
    private static final Date MIN_DATE = new Date(Long.MIN_VALUE);
    private static final Pattern IPV4_REGEX = Pattern.compile("\\A(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");
    private static final Pattern TIME_REGEX = Pattern.compile("\\A(\\d{1,2}):(\\d{2}):(\\d{2})(?:\\.(\\d{3}))?$");
    private static final Pattern UUID_REGEX = Pattern.compile("\\A([0-9a-fA-F]{8})-?([0-9a-fA-F]{4})-?([0-9a-fA-F]{4})-?([0-9a-fA-F]{4})-?([0-9a-fA-F]{4})([0-9a-fA-F]{8})$"); // Final group of 12 split into two sections 123e4567-e89b-12d3-a456-426614174000
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    
    private final ByteBuffer buffer;
    private int hashCode;
    private long routingHash;
    private boolean isValid;

    public RecordKey(int size) {
        this.buffer = ByteBuffer.allocate(size);
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
        this.isValid = true;
    }

    public void addBoolean(Boolean value) {
        if (value == null) {
            this.buffer.put((byte)0);
            return;
        }

        this.buffer.put((byte)(value ? 1 : 0));
    }

    public void addChar(String value, int length) {
        if (value == null) {
            for (int i = 0; i < length; i++) {
                this.buffer.put((byte)0);
            }

            return;
        }

        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        int count = bytes.length;

        if (count > length) {
            count = length;
        }

        for (int i = length; i > count; i--) {
            this.buffer.put((byte)0);
        }

        for (int i = count - 1; i >= 0; i--) {
            this.buffer.put(bytes[i]);
        }
    }

    public void addDate(String value) {
        if (value == null) {
            this.buffer.putInt(0);
            return;
        }

        Matcher matcher = DATE_REGEX.matcher(value);

        if (!matcher.matches()) {
            this.buffer.putInt(0);
            this.isValid = false;
            return;
        }

        int year;
        int month;
        int day;
        GregorianCalendar calendar;

        try {
            year = Integer.parseInt(matcher.group(1));
            month = Integer.parseInt(matcher.group(2));
            day = Integer.parseInt(matcher.group(3));
            calendar = new GregorianCalendar();
            calendar.setGregorianChange(MIN_DATE);
            calendar.set(year, month - 1, day);
        } catch (Exception ex) {
            this.buffer.putInt(0);
            this.isValid = false;
            return;
        }

        if (year < 1000 || year > 2900) {
            this.buffer.putInt(0);
            this.isValid = false;
            return;
        }

        this.buffer.putInt(((year - 1900) << 21)
                | (month << 17)
                | (day << 12)
                | (calendar.get(Calendar.DAY_OF_YEAR) << 3)
                | calendar.get(Calendar.DAY_OF_WEEK));
    }

    public void addDateTime(String value) {
        if (value == null) {
            this.buffer.putLong(0);
            return;
        }

        Matcher matcher = DATETIME_REGEX.matcher(value);

        if (!matcher.matches()) {
            this.buffer.putLong(0);
            this.isValid = false;
            return;
        }

        int year;
        int month;
        int day;
        int hour;
        int minute;
        int second;
        int millisecond;
        GregorianCalendar calendar;

        try {
            year = Integer.parseInt(matcher.group(1));
            month = Integer.parseInt(matcher.group(2));
            day = Integer.parseInt(matcher.group(3));
            hour = Integer.parseInt(matcher.group(4));
            minute = Integer.parseInt(matcher.group(5));
            second = Integer.parseInt(matcher.group(6));

            if (matcher.group(7) != null) {
                millisecond = Integer.parseInt(matcher.group(7));
            } else {
                millisecond = 0;
            }

            calendar = new GregorianCalendar();
            calendar.setGregorianChange(MIN_DATE);
            calendar.set(year, month - 1, day, hour, minute, second);
        } catch (Exception ex) {
            this.buffer.putLong(0);
            this.isValid = false;
            return;
        }

        if (year < 1000 || year > 2900) {
            this.buffer.putLong(0);
            this.isValid = false;
            return;
        }

        this.buffer.putLong(((long)(year - 1900) << 53)
                | ((long)month << 49)
                | ((long)day << 44)
                | ((long)hour << 39)
                | ((long)minute << 33)
                | ((long)second << 27)
                | (millisecond << 17)
                | (calendar.get(Calendar.DAY_OF_YEAR) << 8)
                | (calendar.get(Calendar.DAY_OF_WEEK) << 5));
    }

    /**
     * Add a decimal number to the buffer (can be null)--eight or twelve bytes.
     * Supports decimal(precision, scale). Dynamically determines whether to use
     * 8 or 12 bytes. Raises an error if value cannot fit in either.
     *
     * @param val String representation of decimal value (can be null)
     * @param precision Total number of digits
     * @param scale Number of digits after the decimal point
     * @throws IllegalArgumentException If value cannot fit in 8 or 12 bytes
     */
    public void addDecimal(String val, int precision, int scale) {
        // Validate precision
        if (precision < 1) {
            throw new IllegalArgumentException(
                    String.format("Precision must be a positive integer, got %d", precision));
        }

        // Validate scale
        if (scale < 0 || scale > precision) {
            throw new IllegalArgumentException(
                    String.format("Scale must be an integer between 0 and %d, got %d",
                            precision, scale));
        }

        this.buffer.order(ByteOrder.LITTLE_ENDIAN);

         // Parse decimal value
        BigInteger decimalValue = parseDecimalValue(val, precision, scale);

        if (decimalValue == null)
            decimalValue = BigInteger.ZERO;

        // Determine packing size based on column precision
        if (precision <= Type.Column.DECIMAL8_MAX_PRECISION) {
            // Value needs 8 bytes
            this.buffer.putLong(decimalValue.longValue());
        } else {
            // Value needs 12 bytes
            if (decimalValue.compareTo(BigInteger.ZERO) < 0) {
                // Negative value - convert to 96-bit two's complement
                decimalValue = BigInteger.ONE.shiftLeft(96).add(decimalValue);
            }

            long lowPart = decimalValue.and(new BigInteger("FFFFFFFFFFFFFFFF", 16)).longValue();
            int highPart = decimalValue.shiftRight(64).and(new BigInteger("FFFFFFFF", 16)).intValue();

            this.buffer.putLong(lowPart);
            this.buffer.putInt(highPart);
        }
    }

    /**
     * Parse a decimal value string into a BigInteger scaled by the given scale.
     *
     * @param val String representation of the decimal
     * @param precision Total number of digits
     * @param scale Number of digits after decimal point
     * @return BigInteger representation scaled by 10^scale, or null if invalid
     */
    private static BigInteger parseDecimalValue(String val, int precision, int scale) {
        BigInteger unscaled = null;
        
        if (val != null) {
            try {
                BigDecimal decimal = new BigDecimal(val);
    
                // Convert to unscaled value (BigInteger)
                unscaled = decimal.setScale(scale, RoundingMode.HALF_UP).unscaledValue();
    
                // Validate precision doesn't exceed specified precision
                int totalDigits = unscaled.abs().toString().length();
                if (totalDigits > precision && !isSupportedLegacyValue(decimal, precision, scale))
                    unscaled = null;

            } catch (NumberFormatException | ArithmeticException e) {}
        }
        return unscaled;
    }

    /**
     * Determine whether the given decimal is a value that fits in the default
     * decimal storage size prior to the release of the 12-byte decimal;
     * ultimately, these are decimals with 19 digits that can still fit in 
     * 8 bytes.
     *
     * @param decimal BigDecimal representation of the value to check
     * @param precision Total number of digits, as per the column type
     * @param scale Number of digits after decimal point, as per the column type
     * @return boolean Whether this decimal fits in the default legacy decimal
     *         storage size
     */
    private static boolean isSupportedLegacyValue(BigDecimal decimal, int precision, int scale) {
        return
                precision == Type.Column.DEFAULT_DECIMAL_PRECISION &&
                scale == Type.Column.DEFAULT_DECIMAL_SCALE &&
                decimal.compareTo(Type.Column.DEFAULT_DECIMAL_MIN) >= 0 &&
                decimal.compareTo(Type.Column.DEFAULT_DECIMAL_MAX) <= 0;
    }


    public void addDecimal(String value) {
        addDecimal(value, Type.Column.DEFAULT_DECIMAL_PRECISION, Type.Column.DEFAULT_DECIMAL_SCALE);
    }

    public void addDouble(Double value) {
        if (value == null) {
            this.buffer.putDouble(0.0);
            return;
        }

        this.buffer.putDouble(value);
    }

    public void addFloat(Float value) {
        if (value == null) {
            this.buffer.putFloat(0.0f);
            return;
        }

        this.buffer.putFloat(value);
    }

    public void addInt(Integer value) {
        if (value == null) {
            this.buffer.putInt(0);
            return;
        }

        this.buffer.putInt(value);
    }

    public void addInt8(Integer value) {
        if (value == null) {
            this.buffer.put((byte)0);
            return;
        }

        this.buffer.put((byte)(int)value);
    }

    public void addInt16(Integer value) {
        if (value == null) {
            this.buffer.putShort((short)0);
            return;
        }

        this.buffer.putShort((short)(int)value);
    }

    public void addIPv4(String value) {
        if (value == null) {
            this.buffer.putInt(0);
            return;
        }

        Matcher matcher = IPV4_REGEX.matcher(value);

        if (!matcher.matches()) {
            this.buffer.putInt(0);
            this.isValid = false;
            return;
        }

        int a;
        int b;
        int c;
        int d;

        try {
            a = Integer.parseInt(matcher.group(1));
            b = Integer.parseInt(matcher.group(2));
            c = Integer.parseInt(matcher.group(3));
            d = Integer.parseInt(matcher.group(4));
        } catch (Exception ex) {
            this.buffer.putInt(0);
            this.isValid = false;
            return;
        }

        if (a > 255 || b > 255 || c > 255 || d > 255) {
            this.buffer.putInt(0);
            this.isValid = false;
            return;
        }

        this.buffer.putInt((a << 24)
                | (b << 16)
                | (c << 8)
                | d);
    }

    public void addLong(Long value) {
        if (value == null) {
            this.buffer.putLong(0l);
            return;
        }

        this.buffer.putLong(value);
    }

    public void addString(String value) {
        if (value == null) {
            this.buffer.putLong(0l);
            return;
        }

        MurmurHash3.LongPair murmur = new MurmurHash3.LongPair();
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        MurmurHash3.murmurhash3_x64_128(bytes, 0, bytes.length, 10, murmur);
        this.buffer.putLong(murmur.val1);
    }

    public void addTime(String value) {
        if (value == null) {
            this.buffer.putInt(0);
            return;
        }

        Matcher matcher = TIME_REGEX.matcher(value);

        if (!matcher.matches()) {
            this.buffer.putInt(0);
            this.isValid = false;
            return;
        }

        int hour;
        int minute;
        int second;
        int millisecond;

        try {
            hour = Integer.parseInt(matcher.group(1));
            minute = Integer.parseInt(matcher.group(2));
            second = Integer.parseInt(matcher.group(3));

            if (matcher.group(4) != null) {
                millisecond = Integer.parseInt(matcher.group(4));
            } else {
                millisecond = 0;
            }
        } catch (Exception ex) {
            this.buffer.putInt(0);
            this.isValid = false;
            return;
        }

        if (hour > 23 || minute > 59 || second > 59) {
            this.buffer.putInt(0);
            this.isValid = false;
            return;
        }

        this.buffer.putInt((hour << 26)
                | (minute << 20)
                | (second << 14)
                | (millisecond << 4));
    }

    public void addTimestamp(Long value) {
        if (value == null) {
            this.buffer.putLong(0l);
            return;
        }

        GregorianCalendar calendar = new GregorianCalendar(UTC);
        calendar.setGregorianChange(MIN_DATE);
        calendar.setTimeInMillis(value);
        this.buffer.putLong(((long)(calendar.get(Calendar.YEAR) - 1900) << 53)
                | ((long)(calendar.get(Calendar.MONTH) + 1) << 49)
                | ((long)calendar.get(Calendar.DAY_OF_MONTH) << 44)
                | ((long)calendar.get(Calendar.HOUR_OF_DAY) << 39)
                | ((long)calendar.get(Calendar.MINUTE) << 33)
                | ((long)calendar.get(Calendar.SECOND) << 27)
                | (calendar.get(Calendar.MILLISECOND) << 17)
                | (calendar.get(Calendar.DAY_OF_YEAR) << 8)
                | (calendar.get(Calendar.DAY_OF_WEEK) << 5));
    }



    /**
     * Utility function parsing a given string into an unsigned long.
     * If possible, return true; return false otherwise.
     */
    public static boolean isUnsignedLong(String value) {
        int max_len = 20;

        // Need to have the correct number of characters
        int str_len = value.length();
        if ((str_len == 0) || (str_len > max_len)) {
            return false;
        }

        // Each character must be a digit
        for (int i = 0; i < str_len; ++i ) {
            if ( !Character.isDigit( value.charAt(i) ) ) {
                return false;
            }
        }
        return true;
        
    }
    
    public void addUlong(String value) throws GPUdbException {
        if (value == null) {
            this.buffer.putLong(0l);
            return;
        }

        // Verify if this is a proper unsigned long value
        if ( !isUnsignedLong( value ) )
        {
            throw new GPUdbException( "Unable to parse string value '" + value
                                      + "' as an unsigned long" );
        }

        // Convert the string to an unsigned long value
        byte[] ulong_bytes = new BigInteger( value ).abs().toByteArray();
        int byte_count = ulong_bytes.length;
        int ulong_size = 8;

        // For the max value of unsigned long, the bytes array will have an extra
        // byte to accommodate the sign bit.  We don't want the sign bit for sharding
        // anyway.
        int min_index = 0;
        if ( byte_count > ulong_size ) {
            min_index = (byte_count - ulong_size);
        }

        // Put in the unsigned long (which is in a big endian order)
        // while skipping any extra byte for the sign bit
        for (int i = (byte_count-1); i >= min_index; --i ) {
            this.buffer.put( ulong_bytes[ i ] );
        }
        // Need to pad with zeroes if less than size of a long
        for (int i = byte_count; i < ulong_size; ++i ) {
            this.buffer.put( (byte)0 );
        }
    }
    
    public void addUuid(String value) {
        if (value == null) {
            this.buffer.putLong(0);
            this.buffer.putLong(0);
            return;
        }

        Matcher matcher = UUID_REGEX.matcher(value);

        if (!matcher.matches()) {
            this.buffer.putLong(0);
            this.buffer.putLong(0);
            this.isValid = false;
            return;
        }

        long[] parts = new long[6];
        try {
            for (int p = 0; p < 6; ++p)
                parts[p] = Long.parseLong(matcher.group(p + 1), 16);
        } catch (Exception ex) {
            this.buffer.putLong(0);
            this.buffer.putLong(0);
            this.isValid = false;
            return;
        }

        // See: uuid_string_to_int128_t()
        this.buffer.putInt((int)parts[5]); // Last 8 hex digits of last group
        this.buffer.putShort((short)parts[4]); // First 4 hex digits of last group
        this.buffer.putShort((short)parts[3]); // Fourth group, 4 hex digits
        this.buffer.putShort((short)parts[2]); // Third group, 4 hex digits
        this.buffer.putShort((short)parts[1]); // Second group, 4 hex digits
        this.buffer.putInt((int)parts[0]); // First 8 hex digits
    }

    public void computeHashes() {
        MurmurHash3.LongPair murmur = new MurmurHash3.LongPair();
        MurmurHash3.murmurhash3_x64_128(this.buffer.array(), 0, this.buffer.capacity(), 10, murmur);
        this.routingHash = murmur.val1;
        this.hashCode = (int)(this.routingHash ^ (this.routingHash >>> 32));
        this.buffer.rewind();
    }

    public boolean isValid() {
        return this.isValid;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        return this.buffer.equals(((RecordKey)obj).buffer);
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    public int route(List<Integer> routingTable) {
        int index = Math.abs( (int) (this.routingHash % routingTable.size()) );
        return routingTable.get( index ) - 1;
    }
}
