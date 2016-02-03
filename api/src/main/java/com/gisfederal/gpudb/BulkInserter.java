package com.gisfederal.gpudb;

import com.gisfederal.gpudb.protocol.InsertRecordsResponse;
import com.gisfederal.gpudb.protocol.RawInsertRecordsRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
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
 * @param <T>  the type of object being inserted.
 */
public class BulkInserter<T> {
    /**
     * A list of worker URLs and other configuration information to use for
     * multi-head ingest.
     */
    public static final class WorkerList extends ArrayList<URL> {
        private static final long serialVersionUID = 1L;

        private int tomsPerRank;

        /**
         * Creates a {@link WorkerList} and automatically populates it with the
         * necessary information from GPUdb to support multi-head ingest. (If
         * the specified GPUdb instance has multi-head ingest disabled, the
         * worker list will be empty and multi-head ingest will not be used.)
         * Note that in some cases, workers may be configured to use more than
         * one IP address, not all of which may be accessible to the client;
         * this constructor uses the first IP returned by the server for each
         * worker. To override this behavior, use one of the alternate
         * constructors that accepts an {@link #BulkInserter.WorkerList(GPUdb,
         * Pattern) IP regex} or an {@link #BulkInserter.WorkerList(GPUdb,
         * String) IP prefix}.
         *
         * @param gpudb    the {@link GPUdb} instance from which to obtain the
         *                 information
         *
         * @throws GPUdbException if an error occurs during the request for
         * information
         */
        public WorkerList(GPUdb gpudb) throws GPUdbException {
            this(gpudb, (Pattern)null);
        }

        /**
         * Creates a {@link WorkerList} and automatically populates it with the
         * necessary information from GPUdb to support multi-head ingest. (If
         * the specified GPUdb instance has multi-head ingest disabled, the
         * worker list will be empty and multi-head ingest will not be used.)
         * Note that in some cases, workers may be configured to use more than
         * one IP address, not all of which may be accessible to the client; the
         * optional {@code ipRegex} parameter can be used in such cases to
         * filter for an IP range that is accessible, e.g., a regex of
         * {@code "192\.168\..*"} will use worker IP addresses in the 192.168.*
         * range.
         *
         * @param gpudb    the {@link GPUdb} instance from which to obtain the
         *                 information
         * @param ipRegex  optional IP regex to match
         *
         * @throws GPUdbException if an error occurs during the request for
         * information or no IP addresses matching the IP regex could be found
         * for one or more workers
         */
        public WorkerList(GPUdb gpudb, Pattern ipRegex) throws GPUdbException {
            Map<String, String> systemProperties = gpudb.showSystemProperties(GPUdb.options()).getPropertyMap();
            String s = systemProperties.get("conf.toms_per_rank");

            if (s == null) {
                throw new GPUdbException("Missing value for conf.toms_per_rank.");
            }

            try {
                tomsPerRank = Integer.parseInt(s);
            } catch (Exception ex) {
                tomsPerRank = 0;
            }

            if (tomsPerRank < 1) {
                throw new GPUdbException("Invalid value for conf.toms_per_rank.");
            }

            s = systemProperties.get("conf.enable_worker_http_servers");

            if (s == null) {
                throw new GPUdbException("Missing value for conf.enable_worker_http_servers.");
            }

            if (s.equals("FALSE")) {
                return;
            }

            s = systemProperties.get("conf.worker_http_server_ips");

            if (s == null) {
                throw new GPUdbException("Missing value for conf.worker_http_server_ips.");
            }

            String[] ipLists = s.split(",");

            s = systemProperties.get("conf.worker_http_server_ports");

            if (s == null) {
                throw new GPUdbException("Missing value for conf.worker_http_server_ports.");
            }

            String[] ports = s.split(",");

            if (ipLists.length != ports.length) {
                throw new GPUdbException("Inconsistent number of values for conf.worker_http_server_ips and conf.worker_http_server_ports.");
            }

            for (int i = 1; i < ipLists.length; i++) {
                String[] ips = ipLists[i].split(" ");
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
                            add(new URL("http://" + ip + ":" + ports[i]));
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

            if (isEmpty()) {
                throw new GPUdbException("No worker HTTP servers found.");
            }
        }

        /**
         * Creates a {@link WorkerList} and automatically populates it with the
         * necessary information from GPUdb to support multi-head ingest. (If
         * the specified GPUdb instance has multi-head ingest disabled, the
         * worker list will be empty and multi-head ingest will not be used.)
         * Note that in some cases, workers may be configured to use more than
         * one IP address, not all of which may be accessible to the client; the
         * optional {@code ipprefix} parameter can be used in such cases to
         * filter for an IP range that is accessible, e.g., a prefix of
         * {@code "192.168."} will use worker IP addresses in the 192.168.*
         * range.
         *
         * @param gpudb     the {@link GPUdb} instance from which to obtain the
         *                  information
         * @param ipPrefix  optional IP prefix to match
         *
         * @throws GPUdbException if an error occurs during the request for
         * information or no IP addresses matching the IP prefix could be found
         * for one or more workers
         */
        public WorkerList(GPUdb gpudb, String ipPrefix) throws GPUdbException {
            this(gpudb, (ipPrefix == null) ? null
                    : Pattern.compile(Pattern.quote(ipPrefix) + ".*"));
        }

        /**
         * Creates an empty {@link WorkerList} that can be populated manually
         * with worker URLs to support multi-head ingest. Note that worker URLs
         * must be added in rank order, starting with rank 1, and all worker
         * ranks must be included; otherwise insertion may fail for certain
         * data types.
         *
         * @param tomsPerRank  the number of TOMs per rank (from the GPUdb
         *                     configuration settings)
         */
        public WorkerList(int tomsPerRank) {
            if (tomsPerRank < 1) {
                throw new IllegalArgumentException("Must be at least 1 TOM per rank.");
            }

            this.tomsPerRank = tomsPerRank;
        }

        /**
         * Adds a worker URL string to the list. Note that worker URLs must be
         * added in rank order, starting with rank 1, and all worker ranks must
         * be included; otherwise insertion may fail for certain data types.
         *
         * @param url  the URL to add
         *
         * @return     {@code true}
         *
         * @throws GPUdbException if an invalid URL is specified
         */
        public boolean add(String url) throws GPUdbException {
            try {
                return add(new URL(url));
            } catch (MalformedURLException ex) {
                throw new GPUdbException(ex.getMessage(), ex);
            }
        }

        /**
         * Adds a worker URL to the list. Note that worker URLs must be added in
         * rank order, starting with rank 1, and all worker ranks must be
         * included; otherwise insertion may fail for certain data types.
         *
         * @param url  the URL to add
         *
         * @return     {@code true}
         */
        @Override
        public boolean add(URL url) {
            return super.add(url);
        }

        /**
         * Gets the number of TOMs per rank.
         *
         * @return  the number of TOMs per rank
         */
        public int getTomsPerRank() {
            return tomsPerRank;
        }

        /**
         * Sets the number of TOMs per rank.
         *
         * @param value  the number of TOMs per rank (from the GPUdb
         *               configuration settings)
         */
        public void setTomsPerRank(int value) {
            if (value < 1) {
                throw new IllegalArgumentException("Must be at least 1 TOM per rank.");
            }

            tomsPerRank = value;
        }
    }

    private static final class RecordRouter<T> {
        private static enum ColumnType {
            CHAR1,
            CHAR2,
            CHAR4,
            CHAR8,
            CHAR16,
            DOUBLE,
            FLOAT,
            INT,
            INT8,
            INT16,
            LONG,
            STRING
        }

        private static final class RoutingHashBuilder {
            private static final Charset UTF8 = Charset.forName("UTF-8");

            private final ByteBuffer buffer;

            public RoutingHashBuilder(int size) {
                buffer = ByteBuffer.allocate(size);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
            }

            public void addChar(String value, int length) {
                byte[] bytes = value.getBytes(UTF8);
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

            public void addDouble(double value) {
                buffer.putDouble(value);
            }

            public void addFloat(float value) {
                buffer.putFloat(value);
            }

            public void addInt(int value) {
                buffer.putInt(value);
            }

            public void addInt8(int value) {
                buffer.put((byte)value);
            }

            public void addInt16(int value) {
                buffer.putShort((short)value);
            }

            public void addLong(long value) {
                buffer.putLong(value);
            }

            public void addString(String value) {
                MurmurHash3.LongPair murmur = new MurmurHash3.LongPair();
                byte[] bytes = value.getBytes(UTF8);
                MurmurHash3.murmurhash3_x64_128(bytes, 0, bytes.length, 10, murmur);
                buffer.putLong(murmur.val1);
            }

            public long getHash() {
                MurmurHash3.LongPair murmur = new MurmurHash3.LongPair();
                MurmurHash3.murmurhash3_x64_128(buffer.array(), 0, buffer.capacity(), 10, murmur);
                return murmur.val1;
            }
        }

        private final TypeObjectMap typeObjectMap;
        private final int numRanks;
        private final int tomsPerRank;
        private final List<Integer> columns;
        private final List<ColumnType> columnTypes;
        private final int bufferSize;

        public RecordRouter(Type type, int numRanks, int tomsPerRank) {
            this(type, null, numRanks, tomsPerRank);
        }

        public RecordRouter(TypeObjectMap typeObjectMap, int numRanks, int tomsPerRank) {
            this(typeObjectMap.getType(), typeObjectMap, numRanks, tomsPerRank);
        }

        private RecordRouter(Type type, TypeObjectMap typeObjectMap, int numRanks, int tomsPerRank) {
            this.typeObjectMap = typeObjectMap;
            this.numRanks = numRanks;
            this.tomsPerRank = tomsPerRank;
            columns = new ArrayList<>();
            columnTypes = new ArrayList<>();

            if (numRanks == 1) {
                this.bufferSize = 0;
                return;
            }

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

                if (typeColumn.getProperties().contains(ColumnProperty.PRIMARY_KEY)) {
                    columns.add(i);
                }
            }

            if (trackIdColumn != -1 && hasTimestamp && hasX && hasY) {
                columns.add(trackIdColumn);
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
                    columnTypes.add(ColumnType.LONG);
                    size += 8;
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
                    } else if (typeColumn.getProperties().contains(ColumnProperty.IPV4)) {
                        throw new IllegalArgumentException("Cannot perform routing on column " + typeColumn.getName() + ".");
                    } else {
                        columnTypes.add(ColumnType.STRING);
                        size += 8;
                    }
                } else {
                    throw new IllegalArgumentException("Cannot perform routing on column " + typeColumn.getName() + ".");
                }
            }

            this.bufferSize = size;
        }

        public int routeObject(T object) {
            if (bufferSize == 0) {
                return ThreadLocalRandom.current().nextInt(numRanks);
            }

            IndexedRecord indexedRecord;

            if (typeObjectMap == null) {
                indexedRecord = (IndexedRecord)object;
            } else {
                indexedRecord = null;
            }

            RoutingHashBuilder builder = new RoutingHashBuilder(bufferSize);

            for (int i = 0; i < columns.size(); i++) {
                Object value;

                if (indexedRecord != null) {
                    value = indexedRecord.get(columns.get(i));
                } else {
                    value = typeObjectMap.get(object, i);
                }

                switch (columnTypes.get(i)) {
                    case CHAR1:
                        builder.addChar((String)value, 1);
                        break;

                    case CHAR2:
                        builder.addChar((String)value, 2);
                        break;

                    case CHAR4:
                        builder.addChar((String)value, 4);
                        break;

                    case CHAR8:
                        builder.addChar((String)value, 8);
                        break;

                    case CHAR16:
                        builder.addChar((String)value, 16);
                        break;

                    case DOUBLE:
                        builder.addDouble((Double)value);
                        break;

                    case FLOAT:
                        builder.addFloat((Float)value);
                        break;

                    case INT:
                        builder.addInt((Integer)value);
                        break;

                    case INT8:
                        builder.addInt8((Integer)value);
                        break;

                    case INT16:
                        builder.addInt16((Integer)value);
                        break;

                    case LONG:
                        builder.addLong((Long)value);
                        break;

                    case STRING:
                        builder.addString((String)value);
                        break;
                }
            }

            return Math.abs((int)(builder.getHash() % (numRanks * tomsPerRank))) / tomsPerRank;
        }
    }

    private static final class Worker<T> {
        private final URL url;
        private final int capacity;
        private List<T> queue;

        public Worker(URL url, int capacity) {
            this.url = url;
            this.capacity = capacity;
            queue = new ArrayList<>(capacity);
        }

        public List<T> getQueue() {
            return queue;
        }

        public URL getUrl() {
            return url;
        }

        public List<T> replaceQueue() {
            List<T> oldQueue = queue;
            queue = new ArrayList<>(capacity);
            return oldQueue;
        }
    }

    private final GPUdb gpudb;
    private final String tableName;
    private final TypeObjectMap typeObjectMap;
    private final int batchSize;
    private final Map<String, String> options;
    private final List<Worker<T>> workers;
    private final RecordRouter<T> router;
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
     * @see com.gisfederal.gpudb.protocol.InsertRecordsRequest.Options
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
     * @see com.gisfederal.gpudb.protocol.InsertRecordsRequest.Options
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
     * @see com.gisfederal.gpudb.protocol.InsertRecordsRequest.Options
     */
    public BulkInserter(GPUdb gpudb, String tableName, TypeObjectMap typeObjectMap, int batchSize, Map<String, String> options) throws GPUdbException {
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
     * @see com.gisfederal.gpudb.protocol.InsertRecordsRequest.Options
     */
    public BulkInserter(GPUdb gpudb, String tableName, TypeObjectMap typeObjectMap, int batchSize, Map<String, String> options, WorkerList workers) throws GPUdbException {
        this(gpudb, tableName, typeObjectMap.getType(), typeObjectMap, batchSize, options, workers);
    }

    private BulkInserter(GPUdb gpudb, String tableName, Type type, TypeObjectMap typeObjectMap, int batchSize, Map<String, String> options, WorkerList workers) throws GPUdbException {
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

        this.workers = new ArrayList<>();
        int numRanks;
        int tomsPerRank;

        if (workers != null && !workers.isEmpty()) {
            try {
                for (URL url : workers) {
                    this.workers.add(new Worker<T>(new URL(url, "/insert/records"), batchSize));
                }
            } catch (MalformedURLException ex) {
                throw new GPUdbException(ex.getMessage(), ex);
            }

            numRanks = workers.size();
            tomsPerRank = workers.getTomsPerRank();
        } else {
            try {
                this.workers.add(new Worker<T>(new URL(gpudb.getURL(), "/insert/records"), batchSize));
            } catch (MalformedURLException ex) {
                throw new GPUdbException(ex.getMessage(), ex);
            }

            numRanks = 1;
            tomsPerRank = 1;
        }

        if (typeObjectMap == null) {
            router = new RecordRouter<>(type, numRanks, tomsPerRank);
        } else {
            router = new RecordRouter<>(typeObjectMap, numRanks, tomsPerRank);
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
     * @see com.gisfederal.gpudb.protocol.InsertRecordsRequest.Options
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
     * Ensures that any queued records are inserted into GPUdb.
     *
     * @throws GPUdbException if an error occurs while inserting
     */
    public void flush() throws GPUdbException {
        for (Worker<T> worker : workers) {
            List<T> queue;

            synchronized (worker) {
                queue = worker.replaceQueue();
            }

            flush(queue, worker.getUrl());
        }
    }

    @SuppressWarnings("unchecked")
    private void flush(List<T> queue, URL url) throws GPUdbException {
        if (queue.isEmpty()) {
            return;
        }

        RawInsertRecordsRequest request;

        if (typeObjectMap == null) {
            request = new RawInsertRecordsRequest(tableName, Avro.encode((List<? extends IndexedRecord>)queue, gpudb.getThreadCount(), gpudb.getExecutor()), options);
        } else {
            request = new RawInsertRecordsRequest(tableName, Avro.encode(typeObjectMap, queue, gpudb.getThreadCount(), gpudb.getExecutor()), options);
        }

        InsertRecordsResponse response = new InsertRecordsResponse();
        gpudb.submitRequest(url, request, response, true);
        countInserted.addAndGet(response.getCountInserted());
        countUpdated.addAndGet(response.getCountUpdated());
    }

    /**
     * Queues a record for insertion into GPUdb. If the queue reaches the
     * {@link #getBatchSize batch size}, all records in the queue will be
     * inserted into GPUdb before the method returns.
     *
     * @param record  the record to insert
     *
     * @throws GPUdbException if an error occurs while inserting
     */
    public void insert(T record) throws GPUdbException {
        Worker<T> worker = workers.get(router.routeObject(record));
        List<T> queue;
        boolean flush = false;

        synchronized (worker) {
            queue = worker.getQueue();
            queue.add(record);

            if (queue.size() == batchSize) {
                worker.replaceQueue();
                flush = true;
            }
        }

        if (flush) {
            flush(queue, worker.getUrl());
        }
    }

    /**
     * Queues a list of records for insertion into GPUdb. If any queue reaches
     * the {@link #getBatchSize batch size}, all records in the queue will be
     * inserted into GPUdb before the method returns. Note that depending on the
     * number of records, multiple calls to GPUdb may occur.
     *
     * @param records  the records to insert
     *
     * @throws GPUdbException if an error occurs while inserting
     */
    public void insert(List<T> records) throws GPUdbException {
        for (T record : records) {
            insert(record);
        }
    }
}