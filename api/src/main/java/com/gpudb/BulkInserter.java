package com.gpudb;

import com.gpudb.GPUdbBase.GPUdbExitException;
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
    private final Object haFailoverLock;
    private final GPUdb gpudb;
    private final String tableName;
    private final TypeObjectMap<T> typeObjectMap;
    private final int batchSize;
    private final int dbHARingSize;
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
    private URL currentHeadNodeURL;
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

        haFailoverLock = new Object();
        
        this.gpudb = gpudb;
        this.tableName = tableName;
        this.typeObjectMap = typeObjectMap;
        this.workerList    = workers;

        // Initialize the shard version and update time
        this.shardVersion = 0;
        this.shardUpdateTime = new MutableLong();

        // We need to know how many clusters are in the HA ring (for failover
        // purposes)
        this.dbHARingSize = gpudb.getHARingSize();

        // Keep track of how many times the db client has switched HA clusters
        // in order to decide later if it's time to update the worker queues
        this.numClusterSwitches = gpudb.getNumClusterSwitches();

        // Keep track of which cluster we're using (helpful in knowing if an
        // HA failover has happened)
        this.currentHeadNodeURL = gpudb.getURL();
        
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
                    } else {
                        this.workerQueues.add(new WorkerQueue<T>( GPUdbBase.appendPathToURL(url, "/insert/records"),
                                                                  batchSize,
                                                                  this.hasPrimaryKey,
                                                                  this.updateOnExistingPk ) );
                    }
                }

                // Update the worker queues, if needed
                updateWorkerQueues( this.numClusterSwitches, false );

                this.numRanks = this.workerQueues.size();
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
     * Use the current head node URL in a thread-safe manner.
     */
    private URL getCurrentHeadNodeURL() {
        synchronized ( this.currentHeadNodeURL ) {
            return this.currentHeadNodeURL;
        }
    }
    
    /**
     * Sets the current head node URL in a thread-safe manner.
     */
    private void setCurrentHeadNodeURL(URL newCurrURL) {
        synchronized ( this.currentHeadNodeURL ) {
            this.currentHeadNodeURL = newCurrURL;
        }
    }
    
    /**
     * Use the current count of HA failover events in a thread-safe manner.
     */
    private int getCurrentClusterSwitchCount() {
        synchronized ( this.haFailoverLock ) {
            return this.numClusterSwitches;
        }
    }
    
    /**
     * Set the current count of HA failover events in a thread-safe manner.
     */
    private void setCurrentClusterSwitchCount(int value) {
        synchronized ( this.haFailoverLock ) {
            this.numClusterSwitches = value;
        }
    }
    

    /**
     * Force a high-availability cluster failover over.  Check the health of the
     * cluster (either head node only, or head node and worker ranks, based on
     * the retriever configuration), and use it if healthy.  If no healthy cluster
     * is found, then throw an error.  Otherwise, stop at the first healthy cluster.
     *
     * @throws GPUdbException if a successful failover could not be achieved.
     */
    private synchronized void forceHAFailover(URL currURL, int currCountClusterSwitches) throws GPUdbException {
        
        for (int i = 0; i < this.dbHARingSize; ++i) {
            // Try to switch to a new cluster
            try {
                this.gpudb.switchURL( currURL, currCountClusterSwitches );
            } catch (GPUdbBase.GPUdbHAUnavailableException ex ) {
                // Have tried all clusters; back to square 1
                throw ex;
            }

            // Update the reference points
            currURL                  = this.gpudb.getURL();
            currCountClusterSwitches = this.gpudb.getNumClusterSwitches();
            
            // We did switch to a different cluster; now check the health
            // of the cluster, starting with the head node
            if ( !this.gpudb.isKineticaRunning( currURL ) ) {
                continue; // try the next cluster because this head node is down
            }

            boolean isClusterHealthy = true;
            if ( this.isMultiHeadEnabled ) {
                // Obtain the worker rank addresses
                com.gpudb.WorkerList workerRanks;
                try {
                    workerRanks = new com.gpudb.WorkerList( this.gpudb,
                                                            this.workerList.getIpRegex() );
                } catch (GPUdbException ex) {
                    // Some problem occurred; move to the next cluster
                    continue;
                }
                
                // Check the health of all the worker ranks
                for ( URL workerRank : workerRanks) {
                    if ( !this.gpudb.isKineticaRunning( workerRank ) ) {
                        isClusterHealthy = false;
                    }
                }
            }

            if ( isClusterHealthy ) {
                // Save the healthy cluster's URL as the current head node URL
                this.setCurrentHeadNodeURL( currURL );
                this.setCurrentClusterSwitchCount( currCountClusterSwitches );
                return;
            }
        }   // end for loop

        // If we get here, it means we've failed over across the whole HA ring at least
        // once (could be more times if other threads are causing failover, too)
        String errorMsg = ("HA failover could not find any healthy cluster (all GPUdb clusters with "
                           + "head nodes [" + this.gpudb.getURLs().toString()
                           + "] tried)");
        throw new GPUdbException( errorMsg );
    }   // end forceHAFailover


    /**
     * Updates the shard mapping based on the latest cluster configuration.
     * Also reconstructs the worker queues based on the new sharding.
     *
     * @return  a bool indicating whether the shard mapping was updated or not.
     */
    private boolean updateWorkerQueues( int countClusterSwitches ) throws GPUdbException {
        return this.updateWorkerQueues( countClusterSwitches, true );
    }


    /**
     * Updates the worker queues and the shard mapping based on the latest
     * cluster configuration.   Optionally, also reconstructs the worker
     * queues based on the new sharding.
     *
     * @param countclusterSwitches  Integer keeping track of how many times HA
     *                              has happened.
     * @param doReconstructWorkerQueues  Boolean flag indicating if the worker
     *                                   queues ought to be re-built.
     *
     * @return  a bool indicating whether the shard mapping was updated or not.
     */
    private synchronized boolean updateWorkerQueues( int countClusterSwitches, boolean doReconstructWorkerQueues ) throws GPUdbException {
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
                if ( countClusterSwitches == _numClusterSwitches ) {
                    // Still using the same cluster; so no change needed
                    return false;
                }

                // Update the HA ring node switch counter
                this.setCurrentClusterSwitchCount( _numClusterSwitches );
            }
        
            // Save the new shard version
            this.shardVersion = newShardVersion;

            // Save when we're updating the mapping
            this.shardUpdateTime.setValue( new Timestamp( System.currentTimeMillis() ).getTime() );

            // Update the routing table
            this.routingTable = shardInfo.getRank();
        } catch (GPUdbException ex) {
            // Couldn't get the current shard assignment info; see if this is due
            // to cluster failure
            if ( ex.hadConnectionFailure() ) {
                // Could not update the worker queues because we can't connect
                // to the database
                return false;
            } else {
                // Unknown errors not handled here
                throw ex;
            }
        }

        // If we get here, then we may have done a cluster failover during
        // /admin/show/shards; so update the current head node url & count of
        // cluster switches
        this.setCurrentHeadNodeURL( this.gpudb.getURL() );
        this.setCurrentClusterSwitchCount( this.gpudb.getNumClusterSwitches() );
        
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
    private synchronized void reconstructWorkerQueues() throws GPUdbException { 

        // Using the worker ranks for multi-head ingestion; so need to rebuild
        // the worker queues
        // --------------------------------------------------------------------

        // Get the latest worker list (use whatever IP regex was used initially)
        com.gpudb.WorkerList newWorkerList = new com.gpudb.WorkerList( this.gpudb,
                                                                       this.workerList.getIpRegex() );
        if ( newWorkerList.equals( this.workerList ) ) {
            return; // nothing to do since the worker list did not change
        }

        // Update the worker list
        this.workerList = newWorkerList;

        // Create worker queues per worker URL
        List< WorkerQueue<T> > newWorkerQueues = new ArrayList<>();
        for ( URL url : this.workerList) {
            try {
                // Handle removed ranks
                if (url == null) {
                    newWorkerQueues.add( null );
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
        oldWorkerQueues = this.workerQueues;
        this.workerQueues = newWorkerQueues;
        
        // Re-queue any existing queued records
        for ( WorkerQueue<T> oldQueue : oldWorkerQueues ) {
            if ( oldQueue != null ) { // skipping removed ranks
                List<T> records = oldQueue.flush();
                this.insert( records );
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

        int retries = retryCount;
        
        try {
            RawInsertRecordsRequest request;

            if (typeObjectMap == null) {
                request = new RawInsertRecordsRequest(tableName, Avro.encode((List<? extends IndexedRecord>)queue, gpudb.getThreadCount(), gpudb.getExecutor()), options);
            } else {
                request = new RawInsertRecordsRequest(tableName, Avro.encode(typeObjectMap, queue, gpudb.getThreadCount(), gpudb.getExecutor()), options);
            }

            InsertRecordsResponse response = new InsertRecordsResponse();

            // int retries = retryCount;
            long insertionAttemptTimestamp = 0;
            URL currURL;
            int currentCountClusterSwitches;

            while (true) {
                // Save a snapshot of the state of the object pre-insertion attempt
                insertionAttemptTimestamp = new Timestamp( System.currentTimeMillis() ).getTime();
                currURL = getCurrentHeadNodeURL();
                currentCountClusterSwitches = getCurrentClusterSwitchCount();

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
                        updateWorkerQueues( currentCountClusterSwitches );
                    }

                    break; // out of the while loop
                } catch (GPUdbException ex) {
                    // If some connection issue occurred, we want to force an HA failover
                    if ( (ex instanceof GPUdbExitException)
                         || ex.hadConnectionFailure() ) {
                        // We did encounter an HA failover trigger
                        try {
                            // Switch to a different cluster in the HA ring, if any
                            forceHAFailover( currURL, currentCountClusterSwitches );
                        } catch (GPUdbException ex2) {
                            if (retries <= 0) {
                                // We've now tried all the HA clusters and circled back;
                                // propagate the error to the user, but only there
                                // are no more retries left
                                String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                                throw new GPUdbException( originalCause
                                                          + ex2.getMessage(), true );
                            }
                        }
                    }

                    // Update the worker queues since we've failed over to a
                    // different cluster
                    boolean updatedWorkerQueues = updateWorkerQueues( currentCountClusterSwitches );

                    boolean retry = false;
                    synchronized ( this.shardUpdateTime ) {
                        retry = ( updatedWorkerQueues
                                  || ( insertionAttemptTimestamp < this.shardUpdateTime.longValue() ) );
                    }
                    if ( retry ) {
                        // Now that we've switched to a different cluster, re-insert
                        // since no worker queue has these records any more (but the
                        // records may go to a worker queue different from the one
                        // they came from)
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
                            if (retries <= 0) {
                                throw ex2;
                            }
                        }
                    }

                    // If we still have retries left, then we'll go into the next
                    // iteration of the infinite while loop; otherwise, propagate
                    // the exception
                    if (retries > 0)  {
                        --retries;
                    } else {
                        // No more retries; propagate exception to user along with the
                        // failed queue of records
                        throw new InsertException( url, queue, ex.getMessage(), ex );
                    }
                } catch (Exception ex) {
                    // Insertion failed, but maybe due to shard mapping changes (due to
                    // cluster reconfiguration)? Check if the mapping needs to be updated
                    // or has been updated by another thread already after the
                    // insertion was attemtped
                    boolean updatedWorkerQueues = updateWorkerQueues( currentCountClusterSwitches );

                    boolean retry = false;
                    synchronized ( this.shardUpdateTime ) {
                        retry = ( updatedWorkerQueues
                                  || ( insertionAttemptTimestamp < this.shardUpdateTime.longValue() ) );
                    }
                    if ( retry ) {
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

                    // If we still have retries left, then we'll go into the next
                    // iteration of the infinite while loop; otherwise, propagate
                    // the exception
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
     * @throws GPUdbException if an error occurs while calculating shard/primary keys
     * @throws InsertException if an error occurs while inserting
     */
    public void insert(T record) throws InsertException {
        RecordKey primaryKey;
        RecordKey shardKey;

        try {
            if (primaryKeyBuilder != null) {
                primaryKey = primaryKeyBuilder.build( record );
            } else {
                primaryKey = null;
            }

            if (shardKeyBuilder != null) {
                shardKey = shardKeyBuilder.build( record );
            } else {
                shardKey = primaryKey;
            }
        } catch (GPUdbException ex) {
            List<T> queuedRecord = new ArrayList<>();
            queuedRecord.add( record );
            throw new InsertException( (URL)null, queuedRecord,
                                       "Unable to calculate shard/primary key; please check data for unshardable values" );
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
            flush(queue, workerQueue.getUrl(), false );
        }
    }   // end insert( single record )

    
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
                insert( records.get(i) );
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
