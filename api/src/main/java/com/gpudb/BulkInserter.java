package com.gpudb;

import com.gpudb.protocol.AdminShowShardsRequest;
import com.gpudb.protocol.AdminShowShardsResponse;
import com.gpudb.protocol.InsertRecordsRequest;
import com.gpudb.protocol.InsertRecordsResponse;
import com.gpudb.protocol.RawInsertRecordsRequest;
import com.gpudb.protocol.ShowTableResponse;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import org.apache.avro.generic.IndexedRecord;
import org.apache.commons.lang3.mutable.MutableLong;

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

        private InsertException(URL url, List<?> records, String message) {
            super(message);
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

    // BulkInserter Members
    private final GPUdb gpudb;
    private final String tableName;
    private final TypeObjectMap<T> typeObjectMap;
    private final int batchSize;
    private final Map<String, String> options;
    private final RecordKeyBuilder<T> primaryKeyBuilder;
    private final RecordKeyBuilder<T> shardKeyBuilder;
    private final boolean isReplicatedTable;
    private final boolean isMultiHeadEnabled;
    private final boolean useHeadNode;
    private final boolean hasPrimaryKey;
    private final boolean updateOnExistingPk;
    private volatile int retryCount;
    private List<Integer> routingTable;
    private long shardVersion;
    private MutableLong shardUpdateTime;
    private int numClusterSwitches;
    private com.gpudb.WorkerList workerList;
    private List<WorkerQueue<T>> workerQueues;
    private int numRanks;
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
        this.workerList    = workers;

        this.shardVersion = 0;
        this.shardUpdateTime = new MutableLong();

        // Keep track of how many times the db client has switched HA clusters
        // in order to decide later if it's time to update the worker queues
        this.numClusterSwitches = gpudb.getNumClusterSwitches();
        
        // Validate that the table exists
        if ( !gpudb.hasTable( tableName, null ).getTableExists() ) {
            throw new GPUdbException( "Table '" + tableName + "' does not exist!" );
        }
            
        // Check if it is a replicated table (if so, then can't do
        // multi-head ingestion; will have to force rank-0 ingestion)
        this.isReplicatedTable = gpudb.showTable( tableName, null )
                                      .getTableDescriptions()
                                      .get( 0 )
                                      .contains( ShowTableResponse.TableDescriptions.REPLICATED );
            
        // Set if multi-head I/O is turned on at the server
        this.isMultiHeadEnabled = ( this.workerList != null && !this.workerList.isEmpty() );

        // We should use the head node if multi-head is turned off at the server
        // or if we're working with a replicated table
        this.useHeadNode = ( !this.isMultiHeadEnabled || this.isReplicatedTable);

        // Validate the batch size
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
        // Save whether this table has a primary key
        this.hasPrimaryKey = (this.primaryKeyBuilder != null);

        this.updateOnExistingPk = options != null
            && options.containsKey(InsertRecordsRequest.Options.UPDATE_ON_EXISTING_PK)
            && options.get(InsertRecordsRequest.Options.UPDATE_ON_EXISTING_PK).equals(InsertRecordsRequest.Options.TRUE);

        this.workerQueues = new ArrayList<>();

        try {

            // If we have multiple workers, then use those (unless the table
            // is replicated)
            if ( !this.useHeadNode ) {
                
                for (URL url : this.workerList) {
                    if (url == null) {
                        // Handle removed ranks
                        this.workerQueues.add( null );
                        // this.workerQueues.add( (WorkerQueue<>)null );
                    } else {
                        this.workerQueues.add(new WorkerQueue<T>( GPUdbBase.appendPathToURL(url, "/insert/records"),
                                                                  batchSize,
                                                                  this.hasPrimaryKey,
                                                                  this.updateOnExistingPk ) );
                    }
                }

                // Update the worker queues, if needed
                updateWorkerQueues( false );
                // routingTable = gpudb.adminShowShards(new AdminShowShardsRequest()).getRank();

                this.numRanks = this.workerQueues.size();
                // for (int i = 0; i < routingTable.size(); i++) {
                //     if (routingTable.get(i) > this.workerQueues.size()) {
                //         throw new IllegalArgumentException("Too few worker URLs specified.");
                //     }
                // }
            } else { // use the head node only for insertion
                if (gpudb.getURLs().size() == 1) {
                    this.workerQueues.add(new WorkerQueue<T>(GPUdbBase.appendPathToURL(gpudb.getURL(), "/insert/records"), batchSize, primaryKeyBuilder != null, updateOnExistingPk));
                } else {
                    this.workerQueues.add(new WorkerQueue<T>(null, batchSize, primaryKeyBuilder != null, updateOnExistingPk));
                }

                routingTable = null;
                this.numRanks = 1;
            }
        } catch (MalformedURLException ex) {
            throw new GPUdbException(ex.getMessage(), ex);
        }
    }


    /**
     * Updates the shard mapping based on the latest cluster configuration.
     * Also reconstructs the worker queues based on the new sharding.
     *
     * @return  a bool indicating whether the shard mapping was updated or not.
     */
    private boolean updateWorkerQueues() throws GPUdbException {
        return this.updateWorkerQueues( true );
    }


    /**
     * Updates the worker queues and the shard mapping based on the latest
     * cluster configuration.   Optionally, also reconstructs the worker
     * queues based on the new sharding.
     *
     * @param doReconstructWorkerQueues  Boolean flag indicating if the worker
     *                                   queues ought to be re-built.
     *
     * @return  a bool indicating whether the shard mapping was updated or not.
     */
    private boolean updateWorkerQueues( boolean doReconstructWorkerQueues ) throws GPUdbException {
        try {
            // Get the latest shard mapping information
            AdminShowShardsResponse shardInfo = gpudb.adminShowShards(new AdminShowShardsRequest());

            // Get the shard version
            long newShardVersion = shardInfo.getVersion();

            // No-op if the shard version hasn't changed (and it's not the first time)
            if (this.shardVersion == newShardVersion) {
                // Also check if the db client has failed over to a different HA
                // ring node
                int _numClusterSwitches = this.gpudb.getNumClusterSwitches();
                if ( this.numClusterSwitches == _numClusterSwitches ) {
                    // Still using the same cluster; so no change needed
                    return false;
                }

                // Update the HA ring node switch counter
                this.numClusterSwitches = _numClusterSwitches;
            }
        
            // Save the new shard version
            this.shardVersion = newShardVersion;

            // Save when we're updating the mapping
            synchronized ( this.shardUpdateTime ) {
                this.shardUpdateTime.setValue( new Timestamp( System.currentTimeMillis() ).getTime() );
            }

            // Update the routing table
            this.routingTable = shardInfo.getRank();
        } catch (GPUdbException ex) {
            // Couldn't get the current shard assignment info; see if this is due
            // to cluster failure
            if ( ex.hadConnectionFailure() ) {
                // Check if the db client has failed over to a different HA ring node
                int _numClusterSwitches = this.gpudb.getNumClusterSwitches();
                if ( this.numClusterSwitches == _numClusterSwitches ) {
                    return false; // nothing to do; some other problem occurred
                }

                // Update the HA ring node switch counter
                this.numClusterSwitches = _numClusterSwitches;
            } else {
                // Unknown errors not handled here
                throw ex;
            }
        }

        // The worker queues need to be re-constructed when asked for
        // iff multi-head i/o is enabled and the table is not replicated
        if ( doReconstructWorkerQueues
             && !this.useHeadNode )
        {
            reconstructWorkerQueues();
        }
        
        return true; // the shard mapping was updated indeed
    }  // end updateWorkerQueues


    /**
     * Reconstructs the worker queues and re-queues records in the old
     * queues.
     */
    private void reconstructWorkerQueues() throws GPUdbException {

        // Get the latest worker list (use whatever IP regex was used initially)
        if ( this.workerList == null )
            throw new GPUdbException( "No worker list exists!" );
        com.gpudb.WorkerList newWorkerList = new com.gpudb.WorkerList( this.gpudb,
                                                                       this.workerList.getIpRegex() );
        if ( newWorkerList.equals( this.workerList ) ) {
            return; // nothing to do since the worker list did not change
        }

        // Update the worker list
        synchronized ( this.workerList ) {
            this.workerList = newWorkerList;
        }

        // Create worker queues per worker URL
        List< WorkerQueue<T> > newWorkerQueues = new ArrayList<>();
        for ( URL url : this.workerList) {
            try {
                // Handle removed ranks
                if (url == null) {
                    newWorkerQueues.add( null );
                    // newWorkerQueues.add( (WorkerQueue<>)null );
                }
                else {
                    // Add a queue for a currently active rank
                    newWorkerQueues.add( new WorkerQueue<T>( GPUdbBase.appendPathToURL(url, "/insert/records"),
                                                             batchSize,
                                                             (this.primaryKeyBuilder != null),
                                                             this.updateOnExistingPk ) );
                }
            } catch (MalformedURLException ex) {
                throw new GPUdbException( ex.getMessage(), ex );
            } catch (Exception ex) {
                throw new GPUdbException( ex.getMessage(), ex );
            }
        }

        // Get the number of workers
        this.numRanks = newWorkerQueues.size();

        // Save the new queue for future use
        List< WorkerQueue<T> > oldWorkerQueues;
        synchronized( this.workerQueues ) {
            oldWorkerQueues = this.workerQueues;
            this.workerQueues = newWorkerQueues;
        }
        
        // Re-queue any existing queued records
        for ( WorkerQueue<T> oldQueue : oldWorkerQueues ) {
            if ( oldQueue != null ) { // skipping removed ranks
                synchronized ( oldQueue ) {
                    this.insert( oldQueue.flush() );
                }
            }
        }
    }  // end reconstructWorkerQueues


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

            // Handle removed ranks
            if ( workerQueue == null)
                continue;
            
            List<T> queue;

            synchronized (workerQueue) {
                queue = workerQueue.flush();
            }

            flush(queue, workerQueue.getUrl(), true);
        }
    }


    @SuppressWarnings("unchecked")
    private void flush(List<T> queue, URL url, boolean forcedFlush) throws InsertException {
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
            long insertionAttemptTimestamp = 0;
            
            while (true) {
                insertionAttemptTimestamp = new Timestamp( System.currentTimeMillis() ).getTime();
                try {
                    if (url == null) {
                        response = gpudb.submitRequest("/insert/records", request, response, true);
                    } else {
                        response = gpudb.submitRequest(url, request, response, true);
                    }

                    countInserted.addAndGet( response.getCountInserted() );
                    countUpdated.addAndGet(  response.getCountUpdated()  );

                    // Check if shard re-balancing is under way at the server; if so,
                    // we need to update the shard mapping
                    if ( "true".equals( response.getInfo().get( "data_rerouted" ) ) ) {
                        updateWorkerQueues();
                    }

                    break; // out of the while loop
                } catch (Exception ex) {
                    // Insertion failed, but maybe due to shard mapping changes (due to
                    // cluster reconfiguration)? Check if the mapping needs to be updated
                    // or has been updated by another thread already after the
                    // insertion was attemtped
                    boolean updatedWorkerQueues = updateWorkerQueues();
                    synchronized ( this.shardUpdateTime ) {
                        if ( updatedWorkerQueues
                             || ( insertionAttemptTimestamp < this.shardUpdateTime.longValue() ) ) {

                            // We need to try inserting the records again since no worker
                            // queue has these records any more (but the records may
                            // go to a worker queue different from the one they came from)
                            --retries;
                            try {
                                this.insert( queue );

                                // If the user intends a forceful flush, i.e. the public flush()
                                // was invoked, then make sure that the records get flushed
                                if ( forcedFlush ) {
                                    this.flush();
                                }

                                break; // out of the while loop
                            } catch (Exception ex2) {
                                // Re-setting the exception since we may re-try again
                                ex = ex2;
                            }
                        }
                    }
                    
                    if (retries > 0) {
                        --retries;
                    } else {
                        throw ex;
                    }
                }
            }
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

        if ( this.useHeadNode ) {
            workerQueue = workerQueues.get(0);
        } else if (shardKey == null) {
            workerQueue = workerQueues.get( routingTable.get( ThreadLocalRandom.current().nextInt( routingTable.size() ) ) - 1 );
        } else {
            workerQueue = workerQueues.get( shardKey.route( routingTable ) );
        }

        // Ensure that this is a valid worker queue (and not a previously removed rank)
        if (workerQueue == null) {
            List<T> queuedRecord = new ArrayList<>();
            queuedRecord.add( record );
            throw new InsertException( (URL)null, queuedRecord,
                                       "Attempted to insert into worker rank that has been removed!  Maybe need to update the shard mapping.");
        }
        
        List<T> queue;

        synchronized (workerQueue) {
            queue = workerQueue.insert(record, primaryKey);
        }

        if (queue != null) {
            flush(queue, workerQueue.getUrl(), false);
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
        for (int i = 0; i < records.size(); ++i) {
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
