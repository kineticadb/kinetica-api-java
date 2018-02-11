package com.gpudb;

import com.gpudb.protocol.AdminShowShardsRequest;
import com.gpudb.protocol.InsertRecordsRequest;
import com.gpudb.protocol.InsertRecordsResponse;
import com.gpudb.protocol.RawInsertRecordsRequest;
import java.net.MalformedURLException;
import java.net.URL;
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
     * @deprecated This class has been superceded by {@link
     * com.gpudb.WorkerList com.gpudb.WorkerList}.
     */
    @Deprecated
    public static final class WorkerList extends com.gpudb.WorkerList {
        private static final long serialVersionUID = 1L;

        /**
         * @deprecated This class has been superceded by {@link
         * com.gpudb.WorkerList com.gpudb.WorkerList}.
         */
        public WorkerList() {
            super();
        }

        /**
         * @deprecated This class has been superceded by {@link
         * com.gpudb.WorkerList com.gpudb.WorkerList}.
         */
        public WorkerList(GPUdb gpudb) throws GPUdbException {
            super(gpudb);
        }

        /**
         * @deprecated This class has been superceded by {@link
         * com.gpudb.WorkerList com.gpudb.WorkerList}.
         */
        public WorkerList(GPUdb gpudb, Pattern ipRegex) throws GPUdbException {
            super(gpudb, ipRegex);
        }

        /**
         * @deprecated This class has been superceded by {@link
         * com.gpudb.WorkerList com.gpudb.WorkerList}.
         */
        public WorkerList(GPUdb gpudb, String ipPrefix) throws GPUdbException {
            super(gpudb, ipPrefix);
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
    private volatile int retryCount;
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
    public BulkInserter(GPUdb gpudb, String tableName, Type type, int batchSize, Map<String, String> options, com.gpudb.WorkerList workers) throws GPUdbException {
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
    public BulkInserter(GPUdb gpudb, String tableName, TypeObjectMap<T> typeObjectMap, int batchSize, Map<String, String> options, com.gpudb.WorkerList workers) throws GPUdbException {
        this(gpudb, tableName, typeObjectMap.getType(), typeObjectMap, batchSize, options, workers);
    }

    private BulkInserter(GPUdb gpudb, String tableName, Type type, TypeObjectMap<T> typeObjectMap, int batchSize, Map<String, String> options, com.gpudb.WorkerList workers) throws GPUdbException {
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
     * Gets the number of times inserts into GPUdb will be retried in the event
     * of an error. After this many retries, {@link InsertException} will be
     * thrown.
     *
     * @return  the number of retries
     *
     * @see #setRetryCount(int)
     */
    public int getRetryCount() {
        return retryCount;
    }

    /**
     * Sets the number of times inserts into GPUdb will be retried in the event
     * of an error. After this many retries, {@link InsertException} will be
     * thrown.
     *
     * @param value  the number of retries
     *
     * @throws IllegalArgumentException if {@code value} is less than zero
     *
     * @see #getRetryCount()
     */
    public void setRetryCount(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Retry count must not be negative.");
        }

        retryCount = value;
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
            int retries = retryCount;

            while (true) {
                try {
                    if (url == null) {
                        gpudb.submitRequest("/insert/records", request, response, true);
                    } else {
                        gpudb.submitRequest(url, request, response, true);
                    }

                    break;
                } catch (Exception ex) {
                    if (retries > 0) {
                        retries--;
                    } else {
                        throw ex;
                    }
                }
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