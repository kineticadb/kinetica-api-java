package com.gpudb;

import com.gpudb.protocol.AdminShowShardsRequest;
import com.gpudb.protocol.InsertRecordsRequest;
import com.gpudb.protocol.InsertRecordsResponse;
import com.gpudb.protocol.RawInsertRecordsRequest;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.avro.generic.IndexedRecord;

/**
 * Object that manages the insertion into GPUdb of large numbers of records in
 * bulk, with automatic batch management and support for multi-head ingest.
 * {@code BulkInserter} instances are thread safe and may be used from any
 * number of threads simultaneously. Use the {@link #insert(Object)} and
 * {@link #insert(List)} methods to queue records for insertion, and the
 * {@link #flush} method to ensure that all queued records have been inserted.
 *
 * @param <T>  the type of object being inserted
 */
public class BulkInserter<T> {
    /**
     * An exception that occurred during the insertion of records into GPUdb.
     */
    public static final class InsertException extends GPUdbException {
        private static final long serialVersionUID = 1L;

        private final URL url;
        private final transient List<?> records;

        private InsertException(URL url, List<?> records, String message, Throwable cause) {
            super(message, cause);
            this.url = url;
            this.records = records;
        }

        /**
         * Gets the URL that records were being inserted into when the exception
         * occurred, or {@code null} if multiple failover URLs all failed.
         *
         * @return  the URL
         */
        public URL getURL() {
            return url;
        }

        /**
         * Gets the list of records that was being inserted when the exception
         * occurred.
         *
         * @return  the list of records
         */
        public List<?> getRecords() {
            return records;
        }
    }

    /**
     * A list of worker URLs to use for multi-head ingest.
     */
    public static final class WorkerList extends ArrayList<URL> {
        private static final long serialVersionUID = 1L;

        /**
         * Creates an empty {@link WorkerList} that can be populated manually
         * with worker URLs to support multi-head ingest. Note that worker URLs
         * must be added in rank order, starting with rank 1, and all worker
         * ranks must be included; otherwise insertion may fail for certain
         * data types.
         */
        public WorkerList() {
        }

        /**
         * Creates a {@link WorkerList} and automatically populates it with the
         * worker URLs from GPUdb to support multi-head ingest. (If the
         * specified GPUdb instance has multi-head ingest disabled, the worker
         * list will be empty and multi-head ingest will not be used.) Note that
         * in some cases, workers may be configured to use more than one IP
         * address, not all of which may be accessible to the client; this
         * constructor uses the first IP returned by the server for each worker.
         * To override this behavior, use one of the alternate constructors that
         * accepts an {@link #BulkInserter.WorkerList(GPUdb, Pattern) IP regex}
         * or an {@link #BulkInserter.WorkerList(GPUdb, String) IP prefix}.
         *
         * @param gpudb    the {@link GPUdb} instance from which to obtain the
         *                 worker URLs
         *
         * @throws GPUdbException if an error occurs during the request for
         * worker URLs
         */
        public WorkerList(GPUdb gpudb) throws GPUdbException {
            this(gpudb, (Pattern)null);
        }

        /**
         * Creates a {@link WorkerList} and automatically populates it with the
         * worker URLs from GPUdb to support multi-head ingest. (If the
         * specified GPUdb instance has multi-head ingest disabled, the worker
         * list will be empty and multi-head ingest will not be used.) Note that
         * in some cases, workers may be configured to use more than one IP
         * address, not all of which may be accessible to the client; the
         * optional {@code ipRegex} parameter can be used in such cases to
         * filter for an IP range that is accessible, e.g., a regex of
         * {@code "192\.168\..*"} will use worker IP addresses in the 192.168.*
         * range.
         *
         * @param gpudb    the {@link GPUdb} instance from which to obtain the
         *                 worker URLs
         * @param ipRegex  optional IP regex to match
         *
         * @throws GPUdbException if an error occurs during the request for
         * worker URLs or no IP addresses matching the IP regex could be found
         * for one or more workers
         */
        public WorkerList(GPUdb gpudb, Pattern ipRegex) throws GPUdbException {
            Map<String, String> systemProperties = gpudb.showSystemProperties(GPUdb.options()).getPropertyMap();
            String protocol = gpudb.getURL().getProtocol();

            String s = systemProperties.get("conf.enable_worker_http_servers");

            if (s == null) {
                throw new GPUdbException("Missing value for conf.enable_worker_http_servers.");
            }

            if (s.equals("FALSE")) {
                return;
            }

            if (gpudb.getURLs().size() > 1) {
                throw new GPUdbException("Multi-head ingest not supported with failover URLs.");
            }

            s = systemProperties.get("conf.worker_http_server_urls");

            if (s != null) {
                String[] urlLists = s.split(";");

                for (int i = 1; i < urlLists.length; i++) {
                    String[] urls = urlLists[i].split(",");
                    boolean found = false;

                    for (String url : urls) {
                        boolean match;

                        if (ipRegex != null) {
                            match = ipRegex.matcher(url).matches();
                        } else {
                            match = true;
                        }

                        if (match) {
                            try {
                                add(new URL(protocol + "://" + url));
                            } catch (MalformedURLException ex) {
                                throw new GPUdbException(ex.getMessage(), ex);
                            }

                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        throw new GPUdbException("No matching IP found for worker " + i + ".");
                    }
                }
            } else {
                s = systemProperties.get("conf.worker_http_server_ips");

                if (s == null) {
                    throw new GPUdbException("Missing value for conf.worker_http_server_ips.");
                }

                String[] ipLists = s.split(";");

                s = systemProperties.get("conf.worker_http_server_ports");

                if (s == null) {
                    throw new GPUdbException("Missing value for conf.worker_http_server_ports.");
                }

                String[] ports = s.split(";");

                if (ipLists.length != ports.length) {
                    throw new GPUdbException("Inconsistent number of values for conf.worker_http_server_ips and conf.worker_http_server_ports.");
                }

                for (int i = 1; i < ipLists.length; i++) {
                    String[] ips = ipLists[i].split(",");
                    boolean found = false;

                    for (String ip : ips) {
                        boolean match;

                        if (ipRegex != null) {
                            match = ipRegex.matcher(ip).matches();
                        } else {
                            match = true;
                        }

                        if (match) {
                            try {
                                add(new URL(protocol + "://" + ip + ":" + ports[i]));
                            } catch (MalformedURLException ex) {
                                throw new GPUdbException(ex.getMessage(), ex);
                            }

                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        throw new GPUdbException("No matching IP found for worker " + i + ".");
                    }
                }
            }

            if (isEmpty()) {
                throw new GPUdbException("No worker HTTP servers found.");
            }
        }

        /**
         * Creates a {@link WorkerList} and automatically populates it with the
         * worker URLs from GPUdb to support multi-head ingest. (If the
         * specified GPUdb instance has multi-head ingest disabled, the worker
         * list will be empty and multi-head ingest will not be used.) Note that
         * in some cases, workers may be configured to use more than one IP
         * address, not all of which may be accessible to the client; the
         * optional {@code ipprefix} parameter can be used in such cases to
         * filter for an IP range that is accessible, e.g., a prefix of
         * {@code "192.168."} will use worker IP addresses in the 192.168.*
         * range.
         *
         * @param gpudb     the {@link GPUdb} instance from which to obtain the
         *                  worker URLs
         * @param ipPrefix  optional IP prefix to match
         *
         * @throws GPUdbException if an error occurs during the request for
         * worker URLs or no IP addresses matching the IP prefix could be found
         * for one or more workers
         */
        public WorkerList(GPUdb gpudb, String ipPrefix) throws GPUdbException {
            this(gpudb, (ipPrefix == null) ? null
                    : Pattern.compile(Pattern.quote(ipPrefix) + ".*"));
        }
    }

    private static final class RecordKey {
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

    private static final class RecordKeyBuilder<T> {
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
                Object value;

                if (indexedRecord != null) {
                    value = indexedRecord.get(columns.get(i));
                } else {
                    value = typeObjectMap.get(object, columns.get(i));
                }

                switch (columnTypes.get(i)) {
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

            key.computeHashes();
            return key;
        }

        public boolean hasKey() {
            return !columns.isEmpty();
        }

        public boolean hasSameKey(RecordKeyBuilder<T> other) {
            return this.columns.equals(other.columns);
        }
    }

    private static final class WorkerQueue<T> {
        private final URL url;
        private final int capacity;
        private final boolean hasPrimaryKey;
        private final boolean updateOnExistingPk;
        private List<T> queue;
        private Map<RecordKey, Integer> primaryKeyMap;

        public WorkerQueue(URL url, int capacity, boolean hasPrimaryKey, boolean updateOnExistingPk) {
            this.url = url;
            this.capacity = capacity;
            this.hasPrimaryKey = hasPrimaryKey;
            this.updateOnExistingPk = updateOnExistingPk;
            queue = new ArrayList<>(capacity);

            if (hasPrimaryKey) {
                primaryKeyMap = new HashMap<>(Math.round(capacity / 0.75f) + 1, 0.75f);
            }
        }

        public List<T> flush() {
            List<T> oldQueue = queue;
            queue = new ArrayList<>(capacity);

            if (primaryKeyMap != null) {
                primaryKeyMap.clear();
            }

            return oldQueue;
        }

        public List<T> insert(T record, RecordKey key) {
            if (hasPrimaryKey && key.isValid()) {
                if (updateOnExistingPk) {
                    Integer keyIndex = primaryKeyMap.get(key);

                    if (keyIndex != null) {
                        queue.set(keyIndex, record);
                    } else {
                        queue.add(record);
                        primaryKeyMap.put(key, queue.size() - 1);
                    }
                } else {
                    if (primaryKeyMap.containsKey(key)) {
                        return null;
                    }

                    queue.add(record);
                    primaryKeyMap.put(key, queue.size() - 1);
                }
            } else {
                queue.add(record);
            }

            if (queue.size() == capacity) {
                return flush();
            } else {
                return null;
            }
        }

        public URL getUrl() {
            return url;
        }
    }

    private final GPUdb gpudb;
    private final String tableName;
    private final TypeObjectMap<T> typeObjectMap;
    private final int batchSize;
    private final Map<String, String> options;
    private final RecordKeyBuilder<T> primaryKeyBuilder;
    private final RecordKeyBuilder<T> shardKeyBuilder;
    private final List<Integer> routingTable;
    private final List<WorkerQueue<T>> workerQueues;
    private final AtomicLong countInserted = new AtomicLong();
    private final AtomicLong countUpdated = new AtomicLong();

    /**
     * Creates a {@link BulkInserter} with the specified parameters.
     *
     * @param gpudb      the GPUdb instance to insert records into
     * @param tableName  the table to insert records into
     * @param type       the type of records being inserted
     * @param batchSize  the number of records to insert into GPUdb at a time
     *                   (records will queue until this number is reached)
     * @param options    optional parameters to pass to GPUdb while inserting
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     *
     * @see com.gpudb.protocol.InsertRecordsRequest.Options
     */
    public BulkInserter(GPUdb gpudb, String tableName, Type type, int batchSize, Map<String, String> options) throws GPUdbException {
        this(gpudb, tableName, type, null, batchSize, options, null);
    }

    /**
     * Creates a {@link BulkInserter} with the specified parameters.
     *
     * @param gpudb      the GPUdb instance to insert records into
     * @param tableName  name of the table to insert records into
     * @param type       the type of records being inserted
     * @param batchSize  the number of records to insert into GPUdb at a time
     *                   (records will queue until this number is reached); for
     *                   multi-head ingest, this value is per worker
     * @param options    optional parameters to pass to GPUdb while inserting
     *                   ({@code null} for no parameters)
     * @param workers    worker list for multi-head ingest ({@code null} to
     *                   disable multi-head ingest)
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     *
     * @see com.gpudb.protocol.InsertRecordsRequest.Options
     */
    public BulkInserter(GPUdb gpudb, String tableName, Type type, int batchSize, Map<String, String> options, WorkerList workers) throws GPUdbException {
        this(gpudb, tableName, type, null, batchSize, options, workers);
    }

    /**
     * Creates a {@link BulkInserter} with the specified parameters.
     *
     * @param gpudb          the GPUdb instance to insert records into
     * @param tableName      name of the table to insert records into
     * @param typeObjectMap  type object map for the type of records being
     *                       inserted
     * @param batchSize      the number of records to insert into GPUdb at a
     *                       time (records will queue until this number is
     *                       reached)
     * @param options        optional parameters to pass to GPUdb while
     *                       inserting ({@code null} for no parameters)
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     *
     * @see com.gpudb.protocol.InsertRecordsRequest.Options
     */
    public BulkInserter(GPUdb gpudb, String tableName, TypeObjectMap<T> typeObjectMap, int batchSize, Map<String, String> options) throws GPUdbException {
        this(gpudb, tableName, typeObjectMap.getType(), typeObjectMap, batchSize, options, null);
    }

    /**
     * Creates a {@link BulkInserter} with the specified parameters.
     *
     * @param gpudb          the GPUdb instance to insert records into
     * @param tableName      name of the table to insert records into
     * @param typeObjectMap  type object map for the type of records being
     *                       inserted
     * @param batchSize      the number of records to insert into GPUdb at a
     *                       time (records will queue until this number is
     *                       reached); for multi-head ingest, this value is per
     *                       worker
     * @param options        optional parameters to pass to GPUdb while
     *                       inserting ({@code null} for no parameters)
     * @param workers        worker list for multi-head ingest ({@code null} to
     *                       disable multi-head ingest)
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     *
     * @see com.gpudb.protocol.InsertRecordsRequest.Options
     */
    public BulkInserter(GPUdb gpudb, String tableName, TypeObjectMap<T> typeObjectMap, int batchSize, Map<String, String> options, WorkerList workers) throws GPUdbException {
        this(gpudb, tableName, typeObjectMap.getType(), typeObjectMap, batchSize, options, workers);
    }

    private BulkInserter(GPUdb gpudb, String tableName, Type type, TypeObjectMap<T> typeObjectMap, int batchSize, Map<String, String> options, WorkerList workers) throws GPUdbException {
        this.gpudb = gpudb;
        this.tableName = tableName;
        this.typeObjectMap = typeObjectMap;

        if (batchSize < 1) {
            throw new IllegalArgumentException("Batch size must be greater than zero.");
        }

        this.batchSize = batchSize;

        if (options != null) {
            this.options = Collections.unmodifiableMap(new HashMap<>(options));
        } else {
            this.options = null;
        }

        RecordKeyBuilder<T> primaryKeyBuilderTemp;
        RecordKeyBuilder<T> shardKeyBuilderTemp;

        if (typeObjectMap == null) {
            primaryKeyBuilderTemp = new RecordKeyBuilder<>(true, type);
            shardKeyBuilderTemp = new RecordKeyBuilder<>(false, type);
        } else {
            primaryKeyBuilderTemp = new RecordKeyBuilder<>(true, typeObjectMap);
            shardKeyBuilderTemp = new RecordKeyBuilder<>(false, typeObjectMap);
        }

        if (primaryKeyBuilderTemp.hasKey()) {
            primaryKeyBuilder = primaryKeyBuilderTemp;

            if (shardKeyBuilderTemp.hasKey() && !shardKeyBuilderTemp.hasSameKey(primaryKeyBuilderTemp)) {
                shardKeyBuilder = shardKeyBuilderTemp;
            } else {
                shardKeyBuilder = null;
            }
        } else {
            primaryKeyBuilder = null;

            if (shardKeyBuilderTemp.hasKey()) {
                shardKeyBuilder = shardKeyBuilderTemp;
            } else {
                shardKeyBuilder = null;
            }
        }

        boolean updateOnExistingPk = options != null
                && options.containsKey(InsertRecordsRequest.Options.UPDATE_ON_EXISTING_PK)
                && options.get(InsertRecordsRequest.Options.UPDATE_ON_EXISTING_PK).equals(InsertRecordsRequest.Options.TRUE);

        this.workerQueues = new ArrayList<>();

        try {
            if (workers != null && !workers.isEmpty()) {
                for (URL url : workers) {
                    this.workerQueues.add(new WorkerQueue<T>(GPUdbBase.appendPathToURL(url, "/insert/records"), batchSize, primaryKeyBuilder != null, updateOnExistingPk));
                }

                routingTable = gpudb.adminShowShards(new AdminShowShardsRequest()).getRank();

                for (int i = 0; i < routingTable.size(); i++) {
                    if (routingTable.get(i) > this.workerQueues.size()) {
                        throw new IllegalArgumentException("Too few worker URLs specified.");
                    }
                }
            } else {
                if (gpudb.getURLs().size() == 1) {
                    this.workerQueues.add(new WorkerQueue<T>(GPUdbBase.appendPathToURL(gpudb.getURL(), "/insert/records"), batchSize, primaryKeyBuilder != null, updateOnExistingPk));
                } else {
                    this.workerQueues.add(new WorkerQueue<T>(null, batchSize, primaryKeyBuilder != null, updateOnExistingPk));
                }

                routingTable = null;
            }
        } catch (MalformedURLException ex) {
            throw new GPUdbException(ex.getMessage(), ex);
        }
    }

    /**
     * Gets the GPUdb instance into which records will be inserted.
     *
     * @return  the GPUdb instance into which records will be inserted
     */
    public GPUdb getGPUdb() {
        return gpudb;
    }

    /**
     * Gets the name of the table into which records will be inserted.
     *
     * @return  the name of the table into which records will be inserted
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Gets the batch size (the number of records to insert into GPUdb at a
     * time). For multi-head ingest this value is per worker.
     *
     * @return  the batch size
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Gets the optional parameters that will be passed to GPUdb while
     * inserting.
     *
     * @return  the optional parameters that will be passed to GPUdb while
     *          inserting
     *
     * @see com.gpudb.protocol.InsertRecordsRequest.Options
     */
    public Map<String, String> getOptions() {
        return options;
    }

    /**
     * Gets the number of records inserted into GPUdb. Excludes records that
     * are currently queued but not yet inserted and records not inserted due to
     * primary key conflicts.
     *
     * @return  the number of records inserted
     */
    public long getCountInserted() {
        return countInserted.get();
    }

    /**
     * Gets the number of records updated (instead of inserted) in GPUdb due to
     * primary key conflicts.
     *
     * @return  the number of records updated
     */
    public long getCountUpdated() {
        return countUpdated.get();
    }

    /**
     * Ensures that any queued records are inserted into GPUdb. If an error
     * occurs while inserting the records from any queue, the records will no
     * longer be in that queue nor in GPUdb; catch {@link InsertException} to
     * get the list of records that were being inserted if needed (for example,
     * to retry). Other queues may also still contain unflushed records if
     * this occurs.
     *
     * @throws InsertException if an error occurs while inserting
     */
    public void flush() throws InsertException {
        for (WorkerQueue<T> workerQueue : workerQueues) {
            List<T> queue;

            synchronized (workerQueue) {
                queue = workerQueue.flush();
            }

            flush(queue, workerQueue.getUrl());
        }
    }

    @SuppressWarnings("unchecked")
    private void flush(List<T> queue, URL url) throws InsertException {
        if (queue.isEmpty()) {
            return;
        }

        try {
            RawInsertRecordsRequest request;

            if (typeObjectMap == null) {
                request = new RawInsertRecordsRequest(tableName, Avro.encode((List<? extends IndexedRecord>)queue, gpudb.getThreadCount(), gpudb.getExecutor()), options);
            } else {
                request = new RawInsertRecordsRequest(tableName, Avro.encode(typeObjectMap, queue, gpudb.getThreadCount(), gpudb.getExecutor()), options);
            }

            InsertRecordsResponse response = new InsertRecordsResponse();

            if (url == null) {
                gpudb.submitRequest("/insert/records", request, response, true);
            } else {
                gpudb.submitRequest(url, request, response, true);
            }

            countInserted.addAndGet(response.getCountInserted());
            countUpdated.addAndGet(response.getCountUpdated());
        } catch (GPUdbBase.SubmitException ex) {
            throw new InsertException(ex.getURL(), queue, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new InsertException(url, queue, ex.getMessage(), ex);
        }
    }

    /**
     * Queues a record for insertion into GPUdb. If the queue reaches the
     * {@link #getBatchSize batch size}, all records in the queue will be
     * inserted into GPUdb before the method returns. If an error occurs while
     * inserting the records, the records will no longer be in the queue nor in
     * GPUdb; catch {@link InsertException} to get the list of records that were
     * being inserted if needed (for example, to retry).
     *
     * @param record  the record to insert
     *
     * @throws InsertException if an error occurs while inserting
     */
    public void insert(T record) throws InsertException {
        RecordKey primaryKey;
        RecordKey shardKey;

        if (primaryKeyBuilder != null) {
            primaryKey = primaryKeyBuilder.build(record);
        } else {
            primaryKey = null;
        }

        if (shardKeyBuilder != null) {
            shardKey = shardKeyBuilder.build(record);
        } else {
            shardKey = primaryKey;
        }

        WorkerQueue<T> workerQueue;

        if (routingTable == null) {
            workerQueue = workerQueues.get(0);
        } else if (shardKey == null) {
            workerQueue = workerQueues.get(routingTable.get(ThreadLocalRandom.current().nextInt(routingTable.size())) - 1);
        } else {
            workerQueue = workerQueues.get(shardKey.route(routingTable));
        }

        List<T> queue;

        synchronized (workerQueue) {
            queue = workerQueue.insert(record, primaryKey);
        }

        if (queue != null) {
            flush(queue, workerQueue.getUrl());
        }
    }

    /**
     * Queues a list of records for insertion into GPUdb. If any queue reaches
     * the {@link #getBatchSize batch size}, all records in that queue will be
     * inserted into GPUdb before the method returns. If an error occurs while
     * inserting the queued records, the records will no longer be in that queue
     * nor in GPUdb; catch {@link InsertException} to get the list of records
     * that were being inserted (including any from the queue in question and
     * any remaining in the list not yet queued) if needed (for example, to
     * retry). Note that depending on the number of records, multiple calls to
     * GPUdb may occur.
     *
     * @param records  the records to insert
     *
     * @throws InsertException if an error occurs while inserting
     */
    @SuppressWarnings("unchecked")
    public void insert(List<T> records) throws InsertException {
        for (int i = 0; i < records.size(); i++) {
            try {
                insert(records.get(i));
            } catch (InsertException ex) {
                List<T> queue = (List<T>)ex.getRecords();

                for (int j = i + 1; j < records.size(); j++) {
                    queue.add(records.get(j));
                }

                throw ex;
            }
        }
    }
}