package com.gpudb;

import java.math.BigDecimal;
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

final class RecordKey {
    private static final Pattern DATE_REGEX = Pattern.compile("\\A(\\d{4})-(\\d{2})-(\\d{2})$");
    private static final Pattern DATETIME_REGEX = Pattern.compile("\\A(\\d{4})-(\\d{2})-(\\d{2}) (\\d{1,2}):(\\d{2}):(\\d{2})(?:\\.(\\d{3}))?$");
    private static final Pattern DECIMAL_REGEX = Pattern.compile("\\A\\s*[+-]?(\\d+(\\.\\d{0,4})?|\\.\\d{1,4})$");
    private static final Date MIN_DATE = new Date(Long.MIN_VALUE);
    private static final Pattern IPV4_REGEX = Pattern.compile("\\A(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");
    private static final Pattern TIME_REGEX = Pattern.compile("\\A(\\d{1,2}):(\\d{2}):(\\d{2})(?:\\.(\\d{3}))?$");
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private final ByteBuffer buffer;
    private int hashCode;
    private long routingHash;
    private boolean isValid;

    public RecordKey(int size) {
        buffer = ByteBuffer.allocate(size);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        isValid = true;
    }

    public void addChar(String value, int length) {
        if (value == null) {
            for (int i = 0; i < length; i++) {
                buffer.put((byte)0);
            }

            return;
        }

        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        int count = bytes.length;

        if (count > length) {
            count = length;
        }

        for (int i = length; i > count; i--) {
            buffer.put((byte)0);
        }

        for (int i = count - 1; i >= 0; i--) {
            buffer.put(bytes[i]);
        }
    }

    public void addDate(String value) {
        if (value == null) {
            buffer.putInt(0);
            return;
        }

        Matcher matcher = DATE_REGEX.matcher(value);

        if (!matcher.matches()) {
            buffer.putInt(0);
            isValid = false;
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
            buffer.putInt(0);
            isValid = false;
            return;
        }

        if (year < 1000 || year > 2900) {
            buffer.putInt(0);
            isValid = false;
            return;
        }

        buffer.putInt(((year - 1900) << 21)
                | (month << 17)
                | (day << 12)
                | (calendar.get(Calendar.DAY_OF_YEAR) << 3)
                | calendar.get(Calendar.DAY_OF_WEEK));
    }

    public void addDateTime(String value) {
        if (value == null) {
            buffer.putLong(0);
            return;
        }

        Matcher matcher = DATETIME_REGEX.matcher(value);

        if (!matcher.matches()) {
            buffer.putLong(0);
            isValid = false;
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
            buffer.putLong(0);
            isValid = false;
            return;
        }

        if (year < 1000 || year > 2900) {
            buffer.putLong(0);
            isValid = false;
            return;
        }

        buffer.putLong(((long)(year - 1900) << 53)
                | ((long)month << 49)
                | ((long)day << 44)
                | ((long)hour << 39)
                | ((long)minute << 33)
                | ((long)second << 27)
                | (millisecond << 17)
                | (calendar.get(Calendar.DAY_OF_YEAR) << 8)
                | (calendar.get(Calendar.DAY_OF_WEEK) << 5));
    }

    public void addDecimal(String value) {
        if (value == null) {
            buffer.putLong(0l);
            return;
        }

        Matcher matcher = DECIMAL_REGEX.matcher(value);

        if (!matcher.matches()) {
            buffer.putLong(0l);
            isValid = false;
            return;
        }

        try {
            int i = 0;

            while (i < value.length() && Character.isWhitespace(value.charAt(i))) {
                i++;
            }

            buffer.putLong(new BigDecimal(value.substring(i)).movePointRight(4).setScale(0, BigDecimal.ROUND_UNNECESSARY).longValueExact());
        } catch (Exception ex) {
            buffer.putLong(0l);
            isValid = false;
        }
    }

    public void addDouble(Double value) {
        if (value == null) {
            buffer.putDouble(0.0);
            return;
        }

        buffer.putDouble(value);
    }

    public void addFloat(Float value) {
        if (value == null) {
            buffer.putFloat(0.0f);
            return;
        }

        buffer.putFloat(value);
    }

    public void addInt(Integer value) {
        if (value == null) {
            buffer.putInt(0);
            return;
        }

        buffer.putInt(value);
    }

    public void addInt8(Integer value) {
        if (value == null) {
            buffer.put((byte)0);
            return;
        }

        buffer.put((byte)(int)value);
    }

    public void addInt16(Integer value) {
        if (value == null) {
            buffer.putShort((short)0);
            return;
        }

        buffer.putShort((short)(int)value);
    }

    public void addIPv4(String value) {
        if (value == null) {
            buffer.putInt(0);
            return;
        }

        Matcher matcher = IPV4_REGEX.matcher(value);

        if (!matcher.matches()) {
            buffer.putInt(0);
            isValid = false;
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
            buffer.putInt(0);
            isValid = false;
            return;
        }

        if (a > 255 || b > 255 || c > 255 || d > 255) {
            buffer.putInt(0);
            isValid = false;
            return;
        }

        buffer.putInt((a << 24)
                | (b << 16)
                | (c << 8)
                | d);
    }

    public void addLong(Long value) {
        if (value == null) {
            buffer.putLong(0l);
            return;
        }

        buffer.putLong(value);
    }

    public void addString(String value) {
        if (value == null) {
            buffer.putLong(0l);
            return;
        }

        MurmurHash3.LongPair murmur = new MurmurHash3.LongPair();
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        MurmurHash3.murmurhash3_x64_128(bytes, 0, bytes.length, 10, murmur);
        buffer.putLong(murmur.val1);
    }

    public void addTime(String value) {
        if (value == null) {
            buffer.putInt(0);
            return;
        }

        Matcher matcher = TIME_REGEX.matcher(value);

        if (!matcher.matches()) {
            buffer.putInt(0);
            isValid = false;
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
            buffer.putInt(0);
            isValid = false;
            return;
        }

        if (hour > 23 || minute > 59 || second > 59) {
            buffer.putInt(0);
            isValid = false;
            return;
        }

        buffer.putInt((hour << 26)
                | (minute << 20)
                | (second << 14)
                | (millisecond << 4));
    }

    public void addTimestamp(Long value) {
        if (value == null) {
            buffer.putLong(0l);
            return;
        }

        GregorianCalendar calendar = new GregorianCalendar(UTC);
        calendar.setGregorianChange(MIN_DATE);
        calendar.setTimeInMillis(value);
        buffer.putLong(((long)(calendar.get(Calendar.YEAR) - 1900) << 53)
                | ((long)(calendar.get(Calendar.MONTH) + 1) << 49)
                | ((long)calendar.get(Calendar.DAY_OF_MONTH) << 44)
                | ((long)calendar.get(Calendar.HOUR_OF_DAY) << 39)
                | ((long)calendar.get(Calendar.MINUTE) << 33)
                | ((long)calendar.get(Calendar.SECOND) << 27)
                | (calendar.get(Calendar.MILLISECOND) << 17)
                | (calendar.get(Calendar.DAY_OF_YEAR) << 8)
                | (calendar.get(Calendar.DAY_OF_WEEK) << 5));
    }

    public void computeHashes() {
        MurmurHash3.LongPair murmur = new MurmurHash3.LongPair();
        MurmurHash3.murmurhash3_x64_128(buffer.array(), 0, buffer.capacity(), 10, murmur);
        routingHash = murmur.val1;
        hashCode = (int)(routingHash ^ (routingHash >>> 32));
        buffer.rewind();
    }

    public boolean isValid() {
        return isValid;
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
        return hashCode;
    }

    public int route(List<Integer> routingTable) {
        return routingTable.get(Math.abs((int)(routingHash % routingTable.size()))) - 1;
    }
}