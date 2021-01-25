package com.gpudb;

import com.gpudb.GPUdbBase.GPUdbExitException;
import com.gpudb.GPUdbBase.GPUdbUnauthorizedAccessException;
import com.gpudb.protocol.AdminShowShardsRequest;
import com.gpudb.protocol.AdminShowShardsResponse;
import com.gpudb.protocol.InsertRecordsRequest;
import com.gpudb.protocol.InsertRecordsResponse;
import com.gpudb.protocol.RawInsertRecordsRequest;
import com.gpudb.protocol.ShowTableResponse;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

    // The default number of times insertions will be re-attempted
    private static final int DEFAULT_INSERTION_RETRY_COUNT = 0;
    // private static final int DEFAULT_INSERTION_RETRY_COUNT = 1;


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


    /**
     * A callable class that stores a list of records to insert and can also insert
     * those records in its call() method.
     */
    private static final class WorkerQueue<T> implements Callable {
        private final GPUdb gpudb;
        private final URL url;
        private final String tableName;
        private final int capacity;
        private final boolean hasPrimaryKey;
        private final boolean updateOnExistingPk;
        private List<T> queue;
        private Map<RecordKey, Integer> primaryKeyMap;
        private final TypeObjectMap<T> typeObjectMap;
        private final Map<String, String> options;

        public WorkerQueue( GPUdb gpudb, URL url, String tableName,
                            int capacity,
                            boolean hasPrimaryKey, boolean updateOnExistingPk,
                            Map<String, String> options,
                            TypeObjectMap<T> typeObjectMap ) {
            this.gpudb     = gpudb;
            this.url       = url;
            this.tableName = tableName;
            this.capacity           = capacity;
            this.hasPrimaryKey      = hasPrimaryKey;
            this.updateOnExistingPk = updateOnExistingPk;
            this.options            = options;
            this.typeObjectMap      = typeObjectMap;

            // Allow some extra room when allocating the memory since we may
            // sometimes go over the capacity before we flush
            queue = new ArrayList<>( (int)Math.round( capacity * 1.25 ) );

            if (hasPrimaryKey) {
                primaryKeyMap = new HashMap<>(Math.round(capacity / 0.75f) + 1, 0.75f);
            }
        }

        /*
         * Returns the currently queued records and re-initializes the queue
         * for new records.
         */
        public List<T> flush() {
            List<T> oldQueue = queue;
            queue = new ArrayList<>(capacity);

            if (primaryKeyMap != null) {
                primaryKeyMap.clear();
            }

            return oldQueue;
        }

        /**
         * Insert the given record.  Return true if the record
         * is inserted into the queue, and false if it hasn't
         * (based on primary key conflicts and user options).
         */
        public boolean insert(T record, RecordKey key) {
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
                        return false;
                    }

                    queue.add(record);
                    primaryKeyMap.put(key, queue.size() - 1);
                }
            } else {
                queue.add(record);
            }

            return true;
        }


        /**
         * Returns if the queue is full (based on the capacity).
         */
        public boolean isQueueFull() {
            if (queue.size() >= capacity) {
                return true;
            } else {
                return false;
            }
        }


        /**
         * Inserts the records in the queue.  Returns a {@link
         * com.gpudb.GPUdbBase.WorkerQueueInsertionResult} object
         * containing the result of the insertion, or null if no
         * insertion was attempted.
         */
        @Override
        public WorkerQueueInsertionResult<T> call() throws Exception {
            // Get the currently queued records (we shall try inserting them)
            List<T> queuedRecords = this.queue;
            this.queue = new ArrayList<>( capacity );

            // Clear out the map of records to primary index (used for the
            // purpose of updating existing primary keys)
            if (primaryKeyMap != null) {
                primaryKeyMap.clear();
            }

            // If nothing to insert, return a null object for the response
            if ( queuedRecords.isEmpty() ) {
                GPUdbLogger.debug_with_info( "0 records in the queue; nothing to insert" );
                return null;
            }

            // First, encode the records to create the request packet
            RawInsertRecordsRequest request;
            try {
                List<ByteBuffer> encodedRecords;
                if ( this.typeObjectMap == null ) {
                    encodedRecords = Avro.encode( (List<? extends IndexedRecord>)queuedRecords,
                                                  this.gpudb.getThreadCount(),
                                                  this.gpudb.getExecutor() );
                } else {
                    encodedRecords = Avro.encode( this.typeObjectMap,
                                                  queuedRecords,
                                                  this.gpudb.getThreadCount(),
                                                  this.gpudb.getExecutor() );
                }
                request = new RawInsertRecordsRequest( this.tableName,
                                                       encodedRecords,
                                                       options);
            } catch (Exception ex) {
                // Can't even attempt the insertion! Need to let the caller know.
                return new WorkerQueueInsertionResult<T>( url, this.gpudb.getURL(),
                                                          null,
                                                          queuedRecords,
                                                          false, false, false,
                                                          this.gpudb.getNumClusterSwitches(),
                                                          0, ex );
            }

            // Some flags for necessary follow-up work
            boolean doUpdateWorkers = false;
            boolean doFailover      = false;
            boolean isSuccess       = false;

            // Information that we will need to pass via the result object
            Exception exception   = null;
            List<T> failedRecords = null;

            InsertRecordsResponse response = new InsertRecordsResponse();

            long insertionAttemptTimestamp = new Timestamp( System.currentTimeMillis() ).getTime();
            URL headRankURL = this.gpudb.getURL();
            int countClusterSwitches = this.gpudb.getNumClusterSwitches();

            try {
                if (url == null) {
                    GPUdbLogger.debug_with_info( "Inserting " + queuedRecords.size()
                                                 + " records to rank-0" );
                    response = this.gpudb.submitRequest("/insert/records", request, response, true);
                } else {
                    GPUdbLogger.debug_with_info( "Inserting " + queuedRecords.size()
                                                 + " records to rank at "
                                                 + url.toString() );

                    // // Note: The following debug is for developer debugging **ONLY**.
                    // //       NEVER have this checked in uncommented since it will
                    // //       slow down everything by printing the whole queue!
                    // GPUdbLogger.debug_with_info( "Inserting records: "
                    //                              + Arrays.toString( queue.toArray() ) );

                    // Insert into the given worker rank
                    response = this.gpudb.submitRequest(url, request, response, true);
                }

                // Check if shard re-balancing is under way at the server; if so,
                // we need to update the shard mapping
                if ( "true".equals( response.getInfo().get( "data_rerouted" ) ) ) {
                    doUpdateWorkers = true;
                }

                // Insertion worked!
                isSuccess = true;
            } catch (GPUdbException ex) {
                // If some connection issue occurred, we want to force an HA failover
                if ( (ex instanceof GPUdbExitException)
                     || ex.hadConnectionFailure() ) {
                    // We did encounter an HA failover trigger
                    doFailover = true;
                }

                // Need to pass the records that we couldn't insert and the
                // exception we caught for further analysis down the road
                failedRecords = queuedRecords;
                // Note that the unauthorized exception is handled here as well
                exception     = ex;
            } catch (Exception ex) {
                // Need to pass the records that we couldn't insert and the
                // exception we caught for further analysis down the road
                failedRecords = queuedRecords;
                exception     = ex;
            }

            // Package the response nicely with all relevant information
            return new WorkerQueueInsertionResult<T>( url, headRankURL, response,
                                                      failedRecords,
                                                      isSuccess,
                                                      doUpdateWorkers,
                                                      doFailover,
                                                      countClusterSwitches,
                                                      insertionAttemptTimestamp,
                                                      exception );
        }  // end call

        public URL getUrl() {
            return url;
        }
    }   // end class WorkerQueue



    /**
     * A container for storing all relevant results for an attempted insertion
     * of records.  Will contain an /insert/records response, any record
     * that failed to be inserted, and some boolean flags around success and
     * other follow-up work that may need to be done.  Also contains any
     * encountered exception.
     */
    private static final class WorkerQueueInsertionResult<T> {
        private final URL workerUrl;
        private final URL headUrl;
        private final InsertRecordsResponse insertResponse;
        private final List<T>   failedRecords;
        private final Exception failureException;
        private final boolean   didSucceed;
        private final boolean   doUpdateWorkers;
        private final boolean   doFailover;
        private final int       countClusterSwitches;
        private final long      insertionAttemptTimestamp;

        public WorkerQueueInsertionResult( URL workerUrl,
                                           URL headUrl,
                                           InsertRecordsResponse insertResponse,
                                           List<T>   failedRecords,
                                           boolean   didSucceed,
                                           boolean   doUpdateWorkers,
                                           boolean   doFailover,
                                           int       countClusterSwitches,
                                           long      insertionAttemptTimestamp,
                                           Exception exception ) {
            this.workerUrl        = workerUrl;
            this.headUrl          = headUrl;
            this.insertResponse   = insertResponse;
            this.failedRecords    = failedRecords;
            this.didSucceed       = didSucceed;
            this.doUpdateWorkers  = doUpdateWorkers;
            this.doFailover       = doFailover;
            this.failureException = exception;
            this.countClusterSwitches      = countClusterSwitches;
            this.insertionAttemptTimestamp = insertionAttemptTimestamp;
        }

        public URL getWorkerUrl() {
            return this.workerUrl;
        }

        public URL getHeadUrl() {
            return this.headUrl;
        }

        public InsertRecordsResponse getInsertResponse() {
            return this.insertResponse;
        }

        public List<T> getFailedRecords() {
            return this.failedRecords;
        }

        public Exception getFailureException() {
            return this.failureException;
        }

        public boolean getDidSucceed() {
            return this.didSucceed;
        }

        public boolean getDoUpdateWorkers() {
            return this.doUpdateWorkers;
        }

        public boolean getDoFailover() {
            return this.doFailover;
        }

        public int getCountClusterSwitches() {
            return this.countClusterSwitches;
        }

        public long getInsertionAttemptTimestamp() {
            return this.insertionAttemptTimestamp;
        }

    }  // end class WorkerQueueInsertionResult


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
    private final ExecutorService workerExecutorService;
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

        // Initialize the thread pool for workers based on the resources
        // available on the system
        workerExecutorService = Executors.newFixedThreadPool( Runtime
                                                              .getRuntime()
                                                              .availableProcessors() );

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

        // By default, we will retry insertions once
        this.retryCount = DEFAULT_INSERTION_RETRY_COUNT;

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

        // And wheter we need to update records with existing primary keys
        this.updateOnExistingPk = ( (options != null)
                                    && options.containsKey( InsertRecordsRequest
                                                            .Options
                                                            .UPDATE_ON_EXISTING_PK )
                                    && options.get( InsertRecordsRequest
                                                    .Options
                                                    .UPDATE_ON_EXISTING_PK )
                                               .equals( InsertRecordsRequest
                                                        .Options
                                                        .TRUE ) );

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
                        URL insertURL = GPUdbBase.appendPathToURL( url,
                                                                   "/insert/records" );
                        this.workerQueues.add(new WorkerQueue<T>( this.gpudb,
                                                                  insertURL,
                                                                  this.tableName,
                                                                  batchSize,
                                                                  this.hasPrimaryKey,
                                                                  this.updateOnExistingPk,
                                                                  this.options,
                                                                  this.typeObjectMap ) );
                    }
                }

                // Update the worker queues, if needed
                updateWorkerQueues( this.numClusterSwitches, false );

                this.numRanks = this.workerQueues.size();
            } else { // use the head node only for insertion
                URL insertURL = null;
                if (gpudb.getURLs().size() == 1) {
                    insertURL = GPUdbBase.appendPathToURL( gpudb.getURL(),
                                                           "/insert/records" );
                }

                this.workerQueues.add( new WorkerQueue<T>( this.gpudb, insertURL,
                                                           this.tableName,
                                                           batchSize,
                                                           (primaryKeyBuilder != null),
                                                           updateOnExistingPk,
                                                           this.options,
                                                           this.typeObjectMap ) );
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
     * Force a high-availability cluster (inter-cluster) or ring-resiliency
     * (intra-cluster) failover over, as appropriate.  Check the health of the
     * cluster (either head node only, or head node and worker ranks, based on
     * the retriever configuration), and use it if healthy.  If no healthy cluster
     * is found, then throw an error.  Otherwise, stop at the first healthy cluster.
     *
     * @throws GPUdbException if a successful failover could not be achieved.
     */
    private synchronized void forceFailover(URL currURL, int currCountClusterSwitches) throws GPUdbException {

        for (int i = 0; i < this.dbHARingSize; ++i) {
            // Try to switch to a new cluster
            try {
                GPUdbLogger.debug_with_info( "Forced HA failover attempt #" + i );
                this.gpudb.switchURL( currURL, currCountClusterSwitches );
            } catch (GPUdbBase.GPUdbHAUnavailableException ex ) {
                // Have tried all clusters; back to square 1
                throw ex;
            } catch (GPUdbBase.GPUdbFailoverDisabledException ex) {
                // Failover is disabled
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
    }   // end forceFailover


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

        // Decide if the worker queues will need to be reconstructed (they will
        // only if multi-head is enabled, it is not a replicated table, and if
        // the user wants to)
        boolean reconstructWorkerQueues = ( doReconstructWorkerQueues
                                            && !this.useHeadNode );

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
                    GPUdbLogger.debug_with_info( "# cluster switches and shard versions the same" );

                    // Still using the same cluster; but may have done an N+1
                    // failover
                    if ( reconstructWorkerQueues )
                    {
                        // The caller needs to know if we ended up updating the
                        // queues
                        boolean didRecontructWorkerQueues = reconstructWorkerQueues();
                        GPUdbLogger.debug_with_info( "Returning reconstruct "
                                                     + "worker queue return value: "
                                                     + didRecontructWorkerQueues );
                        return didRecontructWorkerQueues;
                    }
                    // Not appropriate to update worker queues; then no change
                    // has happened
                    GPUdbLogger.debug_with_info( "Returning false" );
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
                GPUdbLogger.debug_with_info( "Had connection failure: "
                                             + ex.getMessage() );
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
        if ( reconstructWorkerQueues )
        {
            reconstructWorkerQueues();
        }

        GPUdbLogger.debug_with_info( "Returning true" );
        return true; // the shard mapping was updated indeed
    }  // end updateWorkerQueues


    /**
     * Reconstructs the worker queues and re-queues records in the old
     * queues.
     *
     * @returns whether we ended up reconstructing the worker queues or not.
     */
    private synchronized boolean reconstructWorkerQueues() throws GPUdbException {

        // Using the worker ranks for multi-head ingestion; so need to rebuild
        // the worker queues
        // --------------------------------------------------------------------

        // Get the latest worker list (use whatever IP regex was used initially)
        com.gpudb.WorkerList newWorkerList = new com.gpudb.WorkerList( this.gpudb,
                                                                       this.workerList.getIpRegex() );
        GPUdbLogger.debug_with_info( "Current worker list: " + this.workerList.toString() );
        GPUdbLogger.debug_with_info( "New worker list:     " + newWorkerList.toString() );
        if ( newWorkerList.equals( this.workerList ) ) {
            GPUdbLogger.debug_with_info( "Worker list remained the same; returning false" );
            return false; // the worker list did not change
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
                    URL insertURL = GPUdbBase.appendPathToURL( url, "/insert/records" );
                    newWorkerQueues.add( new WorkerQueue<T>( this.gpudb, insertURL,
                                                             this.tableName,
                                                             batchSize,
                                                             (this.primaryKeyBuilder != null),
                                                             this.updateOnExistingPk,
                                                             this.options,
                                                             this.typeObjectMap ) );
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

        GPUdbLogger.debug_with_info( "Worker list was updated, returning true" );
        return true; // we did change the queues!
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
        // Flush all queues, regardless of how full they are.  Also, we will
        // retry based on user configuration.
        this.flushQueues( this.workerQueues, this.retryCount );
    }


    /**
     * Flush the records of a given worker queue.
     */
    private void flush( WorkerQueue<T> workerQueue ) throws InsertException {
        // Flush only the given single queue.  Also, we will retry based on user
        // configuration.
        List<WorkerQueue<T>> workerQueues = new ArrayList<>();
        workerQueues.add( workerQueue );
        this.flushQueues( workerQueues, this.retryCount );
    }


    /**
     * Flush only the queues that are already full.
     *
     * @param retryCount  the number of times we have left to retry inserting
     *
     * @throws InsertException if an error occurs while flushing
     */
    private void flushFullQueues( int retryCount ) throws InsertException {
        List<WorkerQueue<T>> fullQueues = new ArrayList<>();

        for (WorkerQueue<T> workerQueue : this.workerQueues) {

            // Handle removed ranks
            if ( workerQueue == null)
                continue;

            // We will flush only full queues
            if ( workerQueue.isQueueFull() ) {
                fullQueues.add( workerQueue );
            }

        }

        this.flushQueues( fullQueues, retryCount );
    }


    /**
     * Flush only the queues that are already full.
     *
     * If any queue encounters a failover scenario, trigger the failover
     * mechanism to re-establish connection with the server.  Then, re-insert
     * all records that we failed to insert.
     *
     * @param queues      the queues that we need to flush
     * @param retryCount  the number of times we have left to retry inserting
     *
     * @throws InsertException if an error occurs while flushing
     */
    private void flushQueues( List<WorkerQueue<T>> queues, int retryCount )
        throws InsertException {

        // Create an execution completion service that will let each queue
        // work in an independent parallel thread.  We will consume the
        // result of the queues as they complete (in the order of completion).
        CompletionService<WorkerQueueInsertionResult<T>> queueService
            = new ExecutorCompletionService<>( this.workerExecutorService );
		List< Future< WorkerQueueInsertionResult<T> >> futureResults
            = new ArrayList<>();

        // Add all the given tasks to the service
        int countQueueSubmitted = 0;
        for (WorkerQueue<T> workerQueue : queues) {

            // Handle removed ranks
            if ( workerQueue == null)
                continue;

            // Submit each extant worker queue to the service and also
            // to the list of future results
            futureResults.add( queueService.submit( workerQueue ) );
            ++countQueueSubmitted;
        }

        boolean doUpdateWorkers  = false;
        boolean doFailover       = false;
        boolean doRetryInsertion = false;
        int     latestCountClusterSwitches       = 0;
        long    latestInsertionAttemptTimestamp  = 0;
        List<URL> failedWorkerUrls       = new ArrayList<>();
        List<T> failedRecords            = new ArrayList<>();
        List<Exception> workerExceptions = new ArrayList<>();

        // Handle the results of the parallel insertions.  Aggregate results,
        // or set up the need for retry or failover.
        for (int i = 0; i < countQueueSubmitted; ++i) {
            // Note that this take() will block until the next queue has
            // completed its work
            WorkerQueueInsertionResult<T> result;
            try {
                result = queueService.take().get();
            } catch ( ExecutionException
                      | java.lang.InterruptedException ex ) {
                // Something interrupted the execution of the threads.
                // TODO: Is this the best way to handle it?
                continue;
            }

            // Empty queues would return null results
            if ( result ==  null ) {
                continue;
            }

            // Handle the result, if any
            if ( result.getDidSucceed() ) {
                // The insertion for this queue succeeded; aggregate the results
                countInserted.addAndGet( result
                                         .getInsertResponse()
                                         .getCountInserted() );
                countUpdated.addAndGet(  result
                                         .getInsertResponse()
                                         .getCountUpdated()  );

                // Check if shard re-balancing is under way at the server; if so,
                // we need to update the shard mapping
                if ( "true".equals( result
                                    .getInsertResponse()
                                    .getInfo()
                                    .get( "data_rerouted" ) ) ) {
                    doUpdateWorkers = true;
                }
            } else {
                // Something went wrong and the data was not inserted.
                GPUdbLogger.debug_with_info( "Setting retry to true" );
                doRetryInsertion = true;

                // Figure out went wrong with the insertion and what follow-up
                // steps need to be done, including saving the records that
                // could not be inserted.

                failedWorkerUrls.add( result.getWorkerUrl() );

                List<T> workerFailedRecords = result.getFailedRecords();
                if ( workerFailedRecords != null ) {
                    failedRecords.addAll( workerFailedRecords );
                }

                if ( result.getDoUpdateWorkers() ) {
                    // We're only saving true values so that we don't
                    // accidentally override a true value based on a previous
                    // worker's result with a false value of this worker.  We
                    // are trying to find out if *any* worker has triggered a
                    // need for updating our worker list.
                    GPUdbLogger.debug_with_info( "Setting update workers to true" );
                    doUpdateWorkers = true;
                }

                if ( result.getDoFailover() ) {
                    // We're only saving true values so that we don't
                    // accidentally override a true value based on a previous
                    // worker's result with a false value of this worker.  We
                    // are trying to find out if *any* worker has triggered a
                    // need for updating our worker list.
                    GPUdbLogger.debug_with_info( "Setting doFailover & updateWorkers to true" );
                    doFailover = true;
                    // We need to update the worker if we have to fail over
                    doUpdateWorkers = true;
                }

                int workerCountClusterSwitches = result.getCountClusterSwitches();
                if ( workerCountClusterSwitches > latestCountClusterSwitches ) {
                    // We're only saving the largest value so that we know the
                    // latest cluster switch (in the gpudb object) that was
                    // encountered by the workers.
                    GPUdbLogger.debug_with_info( "Changing # cluster switches from "
                                                 + latestCountClusterSwitches
                                                 + " to "
                                                 + workerCountClusterSwitches );
                    latestCountClusterSwitches = workerCountClusterSwitches;
                }

                long workerInsertionAttemptTimestamp = result.getInsertionAttemptTimestamp();
                if ( workerInsertionAttemptTimestamp > latestInsertionAttemptTimestamp ) {
                    // We're only saving the largest value so that we know the
                    // latest insertion attempt timestamp by the workers.
                    GPUdbLogger.debug_with_info( "Changing insertion attempt time from "
                                                 + latestInsertionAttemptTimestamp
                                                 + " to "
                                                 + workerInsertionAttemptTimestamp );
                    latestInsertionAttemptTimestamp = workerInsertionAttemptTimestamp;
                }

                // Save any exception encountered by the worker
                GPUdbLogger.debug_with_info( "Saving worker failure exception " );
                workerExceptions.add( result.getFailureException() );
            }  // end if
        }  // end loop handling results

        // TODO: Figure out if this is the URL that should be passed via the
        // insert exception
        URL currHeadUrl = getCurrentHeadNodeURL();

        // Concatenate all the error messages encountered by the workers, if any
        String originalCauses = "";
        if ( doRetryInsertion ) {
            StringBuilder builder = new StringBuilder();
            builder.append( "[ " );
            int numFailedWorkers = failedWorkerUrls.size();
            for ( int i = 0; i < numFailedWorkers; ++i ) {
                URL workerUrl = failedWorkerUrls.get( i );
                Exception ex_ = workerExceptions.get( i );

                // Concatenate only non-null exceptions' messages
                if ( ex_ != null ) {
                    builder.append( "worker URL " + workerUrl + ": ");
                    builder.append( (ex_.getCause() == null)
                                    ? ex_.toString() : ex_.getCause().toString() );
                    builder.append( "; " );
                }
            }
            builder.append( " ]" );
            originalCauses = builder.toString();
            GPUdbLogger.debug_with_info( "Original causes of failure from ranks: "
                                         + originalCauses );
        }

        // Failover if needed
        if ( doFailover ) {
            try {
                // Switch to a different cluster in the HA ring, if any
                // TODO: Check which head node url needs to be used here
                GPUdbLogger.debug_with_info( "Before calling forceFailovre() "
                                             + "with current head url: "
                                             + currHeadUrl );
                forceFailover( currHeadUrl, latestCountClusterSwitches );
            } catch (GPUdbException ex) {
                GPUdbLogger.debug_with_info( "Failover failed with exception: "
                                             + ex.getMessage() );
                // We've now tried all the HA clusters and circled back;
                // propagate the error to the user.

                // Let the user know that there was a problem and which
                // records could not be inserted
                GPUdbException exception = new GPUdbException( ex.getMessage()
                                                               + ".  Original causes "
                                                               + " encountered by workers: "
                                                               + originalCauses,
                                                               true );
                throw new InsertException( currHeadUrl, failedRecords,
                                           exception.getMessage(),
                                           exception );
            }
        }   // end failover

        if ( doUpdateWorkers ) {
            // Update the workers because we either failed over or the shard
            // mapping has to be updated (due to added/removed ranks)
            try {
                GPUdbLogger.debug_with_info( "Before calling updateWorkerQueeus()" );
                updateWorkerQueues( latestCountClusterSwitches );
            } catch (Exception ex) {
                GPUdbLogger.debug_with_info( "updateWorkerQueeus() failed with "
                                             + "exception: "
                                             + ex.getMessage() );
                // Let the user know that there was a problem and which records
                // could not be inserted
                throw new InsertException( currHeadUrl, failedRecords,
                                           ex.getMessage(), ex);
            }
        }

        if ( doRetryInsertion ) {
            // We need to re-attempt inserting the records that did not get
            // ingested.
            GPUdbLogger.debug_with_info( "Retry insertion" );

            // For a failover scenario, we won't count the next trial as a
            // a true retry since.  Note that without this check, failover
            // doesn't happen properly since we run out of retries.
            if ( !doFailover ) {
                // Not a failover scenario; so the insertion failure happened
                // for some other reason.  Count the next attempt as a retry.
                GPUdbLogger.debug_with_info( "Decreasing retry count from: "
                                             + retryCount );
                --retryCount;
            }

            // Retry insertion of the failed records (recursive call to our
            // private insertion with the retry count decreased to halt
            // the recursion as needed
            GPUdbLogger.debug_with_info( "Before retry" );
            boolean couldRetry = this.insert( failedRecords, retryCount );
            GPUdbLogger.debug_with_info( "After retry; success?: " + couldRetry );
            if ( !couldRetry ) {
                // We ran out of chances to retry.  Let the user know this and
                // pass along the records that we could not insert.
                String message = ("Insertion failed; ran out of retries.  "
                                  + "Original causes encountered by workers: "
                                  + originalCauses );
                throw new InsertException( currHeadUrl, failedRecords, message );

            }
        }
    }   // end flushQueues



    /**
     * Queues a record for insertion into GPUdb. If the queue reaches the
     * {@link #getBatchSize batch size}, all records in the queue will be
     * inserted into GPUdb before the method returns. If an error occurs while
     * inserting the records, the records will no longer be in the queue nor in
     * GPUdb; catch {@link InsertException} to get the list of records that were
     * being inserted if needed (for example, to retry).
     *
     * @param record  the record to insert
     * @param flushWhenFull  boolean flag indicating if the queue should be flushed
     *                       if it is full after inserting this record.
     *
     * @throws GPUdbException if an error occurs while calculating shard/primary keys
     * @throws InsertException if an error occurs while inserting
     */
    private void insert(T record, boolean flushWhenFull) throws InsertException {
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

        // Insert the record into the queue
        synchronized (workerQueue) {
            workerQueue.insert(record, primaryKey);

            // Flush the queue if it is full
            if ( flushWhenFull && workerQueue.isQueueFull() ) {
                this.flush( workerQueue );
                // this.flushFullQueues();
            }
        }
    }   // end private insert( single record, flush when full flag )


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
        // Do flush the queue when it is full.  Note that we are setting
        // flushWhenFull to true here to preserve backward compatibility.
        // Before version 7.0.19.1, the insertion and flushing in this class
        // was single threaded; so each time records got inserted to this class
        // (whether via this insert(single record) or the insert( many records ) )
        // method, we always flushed any queue that became full.  Now that we
        // are using background threads to flush the queues paralelly, if we
        // don't have this 'flushWhenFull' mechanism, the insert( single record )
        // method would _never_ flush any queue.  We can't change the method's
        // behavior that much; it would break existing client code potentially.
        // So, the insert( multiple records ) does not flush the queues upon
        // every individual insert (check that method's code; it passes false
        // where we pass true here for the 2nd parameter), this method does.
        this.insert( record, true );
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
     * If no retries are left, then returns false indicating it could not
     * attempt insertion.  Otherwise, returns true upon successful insertion.
     *
     * @param records      the records to insert
     * @param retryCount   the number of times we have left to retry insertion
     *                     the given list of records
     *
     * @throws InsertException if an error occurs while inserting
     */
    @SuppressWarnings("unchecked")
    private boolean insert(List<T> records, int retryCount ) throws InsertException {
        // Let the user know that we ran out of retries
        if ( retryCount < 0 ) {
            // Retry count of 0 means try once but do no retry
            return false;
        }

        for (int i = 0; i < records.size(); ++i) {
            try {
                // Do not flush after inserting this record (otherwise it
                // becomes essentially sequential flushing)
                insert( records.get(i), false );
            } catch (InsertException ex) {
                List<T> queue = (List<T>)ex.getRecords();

                synchronized ( queue ) {
                    for (int j = i + 1; j < records.size(); j++) {
                        queue.add(records.get(j));
                    }
                }

                throw ex;
            }
        }

        // Flush all the queues that are full in parallel
        this.flushFullQueues( retryCount );

        // We succeeded in inserting all the records!
        return true;
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
        // Try to insert all the records with the alotted retry count
        this.insert( records, this.retryCount );
    }
}
