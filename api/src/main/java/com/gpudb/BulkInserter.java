package com.gpudb;

import com.gpudb.GPUdbBase.GPUdbExitException;
import com.gpudb.protocol.*;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.commons.lang3.tuple.Pair;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

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
public class BulkInserter<T> implements AutoCloseable {

    // The default number of times insertions will be re-attempted
    private static final int DEFAULT_INSERTION_RETRY_COUNT = 3;

    //Number of seconds to wait for the thread pools (scheduler and worker) to terminate
    private static final int DEFAULT_THREADPOOL_TERMINATION_TIMEOUT = 30; //In seconds

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
     * @deprecated This class has been superseded by {@link
     * com.gpudb.WorkerList com.gpudb.WorkerList}.
     */
    @Deprecated
    public static final class WorkerList extends com.gpudb.WorkerList {
        private static final long serialVersionUID = 1L;

        /**
         * @deprecated This class has been superseded by {@link
         * com.gpudb.WorkerList com.gpudb.WorkerList}.
         */
        public WorkerList() {
            super();
        }

        /**
         * @deprecated This class has been superseded by {@link
         * com.gpudb.WorkerList com.gpudb.WorkerList}.
         */
        public WorkerList(GPUdb gpudb) throws GPUdbException {
            super(gpudb);
        }

        /**
         * @deprecated This class has been superseded by {@link
         * com.gpudb.WorkerList com.gpudb.WorkerList}.
         */
        public WorkerList(GPUdb gpudb, Pattern ipRegex) throws GPUdbException {
            super(gpudb, ipRegex);
        }

        /**
         * @deprecated This class has been superseded by {@link
         * com.gpudb.WorkerList com.gpudb.WorkerList}.
         */
        public WorkerList(GPUdb gpudb, String ipPrefix) throws GPUdbException {
            super(gpudb, ipPrefix);
        }
    }

    /**
     * This class facilitates customizing the behavior of automatic flush in {@link BulkInserter}
     * The default value of the 'flushInterval' is set to a negative value to indicate that
     * the automatic flush feature is not needed. If the default values are passed in for the
     * {@link FlushOptions} instance to the constructors it is mandatory to call the method
     * {@link #flush()} or {@link #close()} so that the records are actually saved to the table.
     *
     * @see #flush()
     */
    public static final class FlushOptions {

        public static final int NO_PERIODIC_FLUSH = -1;
        public static final boolean FLUSH_WHEN_FULL = true;

        private int flushInterval; // in seconds
        private boolean flushWhenFull;

        /**
         * This method returns an instance of {@link FlushOptions} with default values.
         * @return - a new instance of {@link FlushOptions} class
         */
        public static FlushOptions defaultOptions() {
            return new FlushOptions();
        }

        /**
         * Default constructor
         */
        public FlushOptions() {
            this.flushInterval = NO_PERIODIC_FLUSH;
            this.flushWhenFull = FLUSH_WHEN_FULL;
        }

        /**
         * Constructor with all members
         * @param flushWhenFull - boolean value indicating whether to flush only full queues
         * @param flushInterval - the time interval in seconds to execute flush
         */
        public FlushOptions(boolean flushWhenFull, int flushInterval) {
            this.flushWhenFull = flushWhenFull;
            this.flushInterval = flushInterval < 0 ? NO_PERIODIC_FLUSH : flushInterval;
        }

        public boolean isFlushWhenFull() {
            return flushWhenFull;
        }

        /**
         * Sets the flag to set whether to flush when queues are full or not
         * @param flushWhenFull - boolean value to indicate whether to flush only full queues
         */
        public void setFlushWhenFull(boolean flushWhenFull) {
            this.flushWhenFull = flushWhenFull;
        }

        public int getFlushInterval() {
            return flushInterval;
        }

        /**
         * Sets the flush interval
         * @param flushInterval - time in seconds
         */
        public void setFlushInterval(int flushInterval) {
            this.flushInterval = flushInterval;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FlushOptions that = (FlushOptions) o;
            return getFlushInterval() == that.getFlushInterval() && isFlushWhenFull() == that.isFlushWhenFull();
        }

        @Override
        public int hashCode() {
            return Objects.hash(getFlushInterval(), isFlushWhenFull());
        }
    }

    /**
     * This is a {@link Runnable} class that is executed by the internal scheduler to perform the automatic flushes
     * periodically.
     */
    private final class TimedFlushTask implements Runnable {

        private final BulkInserter<T> thisInserter;

        public TimedFlushTask( BulkInserter<T> thisInserter ) {
            this.thisInserter = thisInserter;
        }

        @Override
        public void run() {
            final LocalDateTime startTime = LocalDateTime.now();
            GPUdbLogger.debug(String.format("Timed flush started at : %s", startTime));
            try {
                if( thisInserter.getTimedFlushOptions().isFlushWhenFull())
                    thisInserter.flushFullQueues( thisInserter.getRetryCount() );
                else
                    thisInserter.flush();
                final LocalDateTime endTime = LocalDateTime.now();
                GPUdbLogger.debug(String.format("Timed flush completed in : %d seconds", Duration.between(startTime, endTime).getSeconds()));
            } catch (InsertException e) {
                GPUdbLogger.error(e.getMessage());
            }
        }
    }

    /**
     * A callable class that stores a list of records to insert and can also insert
     * those records in its call() method.
     */
    private static final class WorkerQueue<T> implements Callable< WorkerQueueInsertionResult<T> > {
        private final GPUdb gpudb;
        private final URL url;
        private final String tableName;

        // This is the same as the batchSize in BulkInserter class
        private final int capacity;
        private List<T> queue;
        private final TypeObjectMap<T> typeObjectMap;
        private final Map<String, String> options;

        public WorkerQueue( GPUdb gpudb, URL url, String tableName,
                            int capacity,
                            int retryCount,
                            Map<String, String> options,
                            TypeObjectMap<T> typeObjectMap ) {
            this.gpudb     = gpudb;
            this.url       = url;
            this.tableName = tableName;
            this.capacity           = capacity;
            this.options            = options;
            this.typeObjectMap      = typeObjectMap;

            // Allow some extra room when allocating the memory since we may
            // sometimes go over the capacity before we flush
            queue = new ArrayList<>( (int)Math.round( capacity * 1.25 ) );

        }

        /*
         * Returns the currently queued records and re-initializes the queue
         * for new records.
         */
        public List<T> flush() {
            List<T> oldQueue = queue;
            queue = new ArrayList<>(capacity);

            return oldQueue;
        }

        /**
         * Insert the given record into the queue.
         */
        public boolean insert(T record) {
            queue.add( record );
            return true;
        }


        /**
         * Returns if the queue is full (based on the capacity).
         */
        public boolean isQueueFull() {
            return queue.size() >= capacity;
        }


        /**
         * Inserts the records in the queue.  Returns a {@link
         * WorkerQueueInsertionResult} object
         * containing the result of the insertion, or null if no
         * insertion was attempted.
         */
        @Override
        public WorkerQueueInsertionResult<T> call() throws Exception {
            // Get the currently queued records (we shall try inserting them)
            List<T> queuedRecords = this.queue;
            this.queue = new ArrayList<>( capacity );

            // If nothing to insert, return a null object for the response
            if ( queuedRecords.isEmpty() ) {
                GPUdbLogger.debug_with_info( "0 records in the queue; nothing to insert" );
                return null;
            }

            // First, encode the records to create the request packet
            RawInsertRecordsRequest request;

            // A TreeMap will keep the entries sorted by the key.
            // The key is an integer which is the original index of
            // the records as obtained from the list of encoded records
            // returned by the 'Avro.encode' method.
            Map<Integer, Pair<String, T>> errors = new TreeMap<>();
            List<String> warnings = new ArrayList<>();

            // This map stores the records which have failed client side encoding
            // and has been instantiated as a 'LinkedHashMap' which preserves
            // the insertion order.
            Map<String, T> recordsFailedEncoding = null;

            try {
                // This is a 'Pair' of the list of correctly encoded records
                // and a Map of an actual error message to the record which
                // has failed Avro encoding.
                final Pair<ArrayList<ByteBuffer>, Map<String, T>> encodedRecords;

                if ( this.typeObjectMap == null ) {
                    encodedRecords = Avro.encode( queuedRecords,
                                                  this.gpudb.getThreadCount(),
                                                  this.gpudb.getExecutor() );

                } else {
                    encodedRecords = Avro.encode( this.typeObjectMap,
                                                  queuedRecords,
                                                  this.gpudb.getThreadCount(),
                                                  this.gpudb.getExecutor() );
                }
                request = new RawInsertRecordsRequest( this.tableName,
                                                       encodedRecords.getLeft(),
                                                       options);

                recordsFailedEncoding = new LinkedHashMap<>( encodedRecords.getRight() );

            } catch (GPUdbException ex) {
                // Can't even attempt the insertion! Need to let the caller know.
                return new WorkerQueueInsertionResult<>( url, this.gpudb.getURL(),
                                                          null,
                                                          queuedRecords,
                                                          errors, warnings, false, false, false,
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
                final LocalDateTime startTime = LocalDateTime.now();

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
                final LocalDateTime endTime = LocalDateTime.now();

                GPUdbLogger.debug_with_info(String.format("Insertion to rank %s completed in %d seconds",
                    (url == null ? "head rank" : url.toString()), Duration.between(startTime, endTime).getSeconds()
                ));

                Map<String, String> info = response.getInfo();

                // Peek into the errors from the map. This map contains
                // several incorrect error messages returned by the server
                // for those cases where we had passed empty ByteBuffers in
                // places of records which had failed Avro encoding.
                info.entrySet().stream().filter( x -> x.getKey().toLowerCase().startsWith("error_"))
                    .forEach( x -> {
                            int index = Integer.parseInt(x.getKey().substring(6));
                            errors.put(index, Pair.of(x.getKey().substring(6) + ":" + x.getValue(), queuedRecords.get(index)));
                    });

                // Add the warnings
                info.entrySet().stream().filter( x -> x.getKey().toLowerCase().startsWith("warning_"))
                    .forEach( x -> {
                        warnings.add(x.getValue());
                    });

                // Now we traverse the Map of records which failed Avro
                // encoding and update our warnings map with the actual Avro
                // failure message for those records for which we had sent in
                // am empty ByteBuffer to the server.
                recordsFailedEncoding.forEach((key, value) -> {
                    int index = Integer.parseInt(key.substring(0,key.indexOf(":")));
                    errors.put( index, Pair.of( key, value ));
                });

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
            return new WorkerQueueInsertionResult<>( url, headRankURL, response,
                                                      failedRecords,
                                                      errors,
                                                      warnings,
                                                      isSuccess,
                                                      doUpdateWorkers,
                                                      doFailover,
                                                      countClusterSwitches,
                                                      insertionAttemptTimestamp,
                                                      exception );
        }  // end call

        // Clear queue without sending
        public void clear() {
            queue.clear();
        }

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
        private final Map<Integer, Pair<String, T>> error_map;
        private final List<String> warnings;

        public WorkerQueueInsertionResult(URL workerUrl,
                                          URL headUrl,
                                          InsertRecordsResponse insertResponse,
                                          List<T> failedRecords,
                                          Map<Integer, Pair<String, T>> error_map,
                                          List<String> warnings,
                                          boolean didSucceed,
                                          boolean doUpdateWorkers,
                                          boolean doFailover,
                                          int countClusterSwitches,
                                          long insertionAttemptTimestamp,
                                          Exception exception) {
            this.workerUrl        = workerUrl;
            this.headUrl          = headUrl;
            this.insertResponse   = insertResponse;
            this.failedRecords    = failedRecords;
            this.error_map        = error_map;
            this.warnings         = warnings;
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

        public InsertRecordsResponse getInsertResponse() {
            return this.insertResponse;
        }

        public List<T> getFailedRecords() {
            return this.failedRecords;
        }

        public Map<Integer, Pair<String, T>> getErrors() {
            return this.error_map;
        }

        public List<String> getWarnings() {
            return this.warnings;
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
    private final RecordKeyBuilder<T> shardKeyBuilder;
    private final ExecutorService workerExecutorService;
    private ScheduledExecutorService timedFlushExecutorService;
    private boolean timedFlushExecutorServiceTerminated;

    private FlushOptions flushOptions;
    private final boolean isReplicatedTable;
    private final boolean multiHeadEnabled;
    private final boolean useHeadNode;
    private final boolean returnIndividualErrors;
    private boolean simulateErrorMode = false; // Simulate returnIndividualErrors after an error
    private volatile int retryCount;
    private List<Integer> routingTable;
    private long shardVersion;
    private MutableLong shardUpdateTime;
    private int numClusterSwitches;
    private URL currentHeadNodeURL;
    private com.gpudb.WorkerList workerList;
    private List<WorkerQueue<T>> workerQueues;
    private final AtomicLong countInserted = new AtomicLong();
    private final AtomicLong countUpdated = new AtomicLong();
    private List<InsertException> error_list = new ArrayList<>();
    private List<InsertException> warning_list = new ArrayList<>();
    private final Object error_list_lock = new Object();

    /**
     * Creates a {@link BulkInserter} with the specified parameters.
     *
     * This constructor could be used to obtain an instance of the BulkInserter
     * class when the API user chooses not to use the multi-head ingestion
     * facility even if it is turned on, on the server.

     * @param gpudb      the GPUdb instance to insert records into
     * @param tableName  the table to insert records into
     * @param type       the type of records being inserted
     * @param batchSize  the number of records to insert into GPUdb at a time
     *                   (records will queue until this number is reached)
     * @param options    optional parameters to pass to GPUdb while inserting<br>
     *                   This is the same set of options as accepted by the
     *                   {@link GPUdb#insertRecords(String, List, Map)} call.<br>
     *                   The details can be found at
     *                   {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     *
     * @see com.gpudb.protocol.InsertRecordsRequest.Options
     */
    public BulkInserter(GPUdb gpudb,
                        String tableName,
                        Type type,
                        int batchSize,
                        Map<String, String> options) throws GPUdbException {
        this(gpudb, tableName, type, null, batchSize, options, null, FlushOptions.defaultOptions());
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
     *                   ({@code null} for no parameters)<br>
     *                   This is the same set of options as accepted by the
     *                   {@link GPUdb#insertRecords(String, List, Map)} call.<br>
     *                   The details can be found at
     *                   {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
     * @param workers    worker list for multi-head ingest ({@code null} to
     *                   disable multi-head ingest)
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     *
     * @see com.gpudb.protocol.InsertRecordsRequest.Options
     */
    public BulkInserter(GPUdb gpudb,
                        String tableName,
                        Type type,
                        int batchSize,
                        Map<String, String> options,
                        com.gpudb.WorkerList workers) throws GPUdbException {
        this(gpudb, tableName, type, null, batchSize, options, workers, FlushOptions.defaultOptions());
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
     *                   ({@code null} for no parameters)<br>
     *                   This is the same set of options as accepted by the
     *                   {@link GPUdb#insertRecords(String, List, Map)} call.<br>
     *                   The details can be found at
     *                   {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
     * @param workers    worker list for multi-head ingest ({@code null} to
     *                   disable multi-head ingest)
     * @param flushOptions - instance of {@link FlushOptions} class
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     *
     * @see com.gpudb.protocol.InsertRecordsRequest.Options
     * @see FlushOptions
     */
    public BulkInserter(GPUdb gpudb,
                        String tableName,
                        Type type,
                        int batchSize,
                        Map<String, String> options,
                        com.gpudb.WorkerList workers,
                        FlushOptions flushOptions) throws GPUdbException {
        this(gpudb, tableName, type, null, batchSize, options, workers, flushOptions);
    }

    /**
     * Creates a {@link BulkInserter} with the specified parameters.
     *
     * This constructor could be used to obtain an instance of the BulkInserter
     * class when the API user chooses not to use the multi-head ingestion
     * facility even if it is turned on, on the server.
     *
     * @param gpudb          the GPUdb instance to insert records into
     * @param tableName      name of the table to insert records into
     * @param typeObjectMap  type object map for the type of records being
     *                       inserted
     * @param batchSize      the number of records to insert into GPUdb at a
     *                       time (records will queue until this number is
     *                       reached)
     * @param options        optional parameters to pass to GPUdb while
     *                       inserting ({@code null} for no parameters)<br>
     *                       This is the same set of options as accepted by the
     *                       {@link GPUdb#insertRecords(String, List, Map)} call.<br>
     *                       The details can be found at
     *                       {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     *
     * @see com.gpudb.protocol.InsertRecordsRequest.Options
     */
    public BulkInserter(GPUdb gpudb,
                        String tableName,
                        TypeObjectMap<T> typeObjectMap,
                        int batchSize,
                        Map<String, String> options) throws GPUdbException {
        this(gpudb, tableName, typeObjectMap.getType(), typeObjectMap, batchSize, options, null, FlushOptions.defaultOptions());
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
     *                       inserting ({@code null} for no parameters)<br>
     *                       This is the same set of options as accepted by the
     *                       {@link GPUdb#insertRecords(String, List, Map)} call.<br>
     *                       The details can be found at
     *                       {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
     * @param workers        worker list for multi-head ingest ({@code null} to
     *                       disable multi-head ingest)
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     *
     * @see com.gpudb.protocol.InsertRecordsRequest.Options
     */
    public BulkInserter(GPUdb gpudb,
                        String tableName,
                        TypeObjectMap<T> typeObjectMap,
                        int batchSize,
                        Map<String, String> options,
                        com.gpudb.WorkerList workers) throws GPUdbException {
        this(gpudb, tableName, typeObjectMap.getType(), typeObjectMap, batchSize, options, workers, FlushOptions.defaultOptions());
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
     *                       inserting ({@code null} for no parameters)<br>
     *                       This is the same set of options as accepted by the
     *                       {@link GPUdb#insertRecords(String, List, Map)} call.<br>
     *                       The details can be found at
     *                       {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
     * @param workers        worker list for multi-head ingest ({@code null} to
     *                       disable multi-head ingest)
     * @param flushOptions - instance of timed flush options {@link FlushOptions}
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     *
     * @see com.gpudb.protocol.InsertRecordsRequest.Options
     * @see FlushOptions
     */
    public BulkInserter(GPUdb gpudb,
                        String tableName,
                        TypeObjectMap<T> typeObjectMap,
                        int batchSize,
                        Map<String, String> options,
                        com.gpudb.WorkerList workers,
                        FlushOptions flushOptions) throws GPUdbException {
        this(gpudb, tableName, typeObjectMap.getType(), typeObjectMap, batchSize, options, workers, flushOptions);
    }

    private BulkInserter(GPUdb gpudb,
                         String tableName,
                         Type type,
                         TypeObjectMap<T> typeObjectMap,
                         int batchSize,
                         Map<String, String> options,
                         com.gpudb.WorkerList workers,
                         FlushOptions flushOptions) throws GPUdbException {

        boolean hasPermissions = hasPermissionsForTable(gpudb, tableName, options);
        if( !hasPermissions ) {
            String errorMsg = String.format("User %s doesn't have requisite permissions on the table %s to use BulkInserter", gpudb.getUsername(), tableName);
            GPUdbLogger.error(errorMsg);
            throw new GPUdbException(errorMsg);
        }

        haFailoverLock = new Object();

        this.gpudb = gpudb;
        this.tableName = tableName;
        this.typeObjectMap = typeObjectMap;
        this.workerList    = workers;
        this.flushOptions = ( flushOptions == null ) ? FlushOptions.defaultOptions() : flushOptions;

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
        this.multiHeadEnabled = ( this.workerList != null && !this.workerList.isEmpty() );

        // We should use the head node if multi-head is turned off at the server
        // or if we're working with a replicated table
        this.useHeadNode = ( !this.multiHeadEnabled || this.isReplicatedTable);

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

        this.shardKeyBuilder = new RecordKeyBuilder<>(type, typeObjectMap);

        // If caller specified RETURN_INDIVIDUAL_ERRORS without ALLOW_PARTIAL_BATCH
        // then we will need to do our own error handling
        this.returnIndividualErrors = (options != null)
                && options.containsKey(InsertRecordsRequest.Options.RETURN_INDIVIDUAL_ERRORS)
                && options.get(InsertRecordsRequest.Options.RETURN_INDIVIDUAL_ERRORS)
                            .equals(InsertRecordsRequest.Options.TRUE)
                && (!options.containsKey(InsertRecordsRequest.Options.ALLOW_PARTIAL_BATCH)
                    || !options.get(InsertRecordsRequest.Options.ALLOW_PARTIAL_BATCH)
                            .equals(InsertRecordsRequest.Options.TRUE));

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
                        this.workerQueues.add(new WorkerQueue<>( this.gpudb,
                                                                  insertURL,
                                                                  this.tableName,
                                                                  batchSize,
                                                                  retryCount,
                                                                  this.options,
                                                                  this.typeObjectMap ) );
                    }
                }

                // Update the worker queues, if needed
                updateWorkerQueues( this.numClusterSwitches, false );
            } else { // use the head node only for insertion
                URL insertURL = null;
                if (gpudb.getURLs().size() == 1) {
                    insertURL = GPUdbBase.appendPathToURL( gpudb.getURL(),
                                                           "/insert/records" );
                }

                this.workerQueues.add( new WorkerQueue<>( this.gpudb, insertURL,
                                                           this.tableName,
                                                           batchSize,
                                                           retryCount,
                                                           this.options,
                                                           this.typeObjectMap ) );
                routingTable = null;
            }
        } catch (MalformedURLException ex) {
            throw new GPUdbException(ex.getMessage(), ex);
        }

        // Create the scheduler only if the flush interval has been set to a valid value by the user.
        // The default value is -1 to indicate that automatic flush is not called for
        if( this.flushOptions.getFlushInterval() > 0 ) {
            GPUdbLogger.debug("Timed flush turned on, flush interval set to <" + this.flushOptions.getFlushInterval() + ">");
            timedFlushExecutorService = Executors.newSingleThreadScheduledExecutor();
            timedFlushExecutorService.scheduleWithFixedDelay(new TimedFlushTask(this),
                    this.flushOptions.getFlushInterval(),
                    this.flushOptions.getFlushInterval(),
                    TimeUnit.SECONDS);
            timedFlushExecutorServiceTerminated = false;
        } else {
            GPUdbLogger.debug("Timed flush turned off, flush interval set to negative value");
            this.timedFlushExecutorService = null;
            timedFlushExecutorServiceTerminated = true;
        }
    }

    /**
     * Returns the {@link FlushOptions} instance value, useful for debugging.
     * @return - the {@link FlushOptions} instance
     */
    public FlushOptions getTimedFlushOptions() {
        return flushOptions;
    }

    /**
     * This method could potentially result in two different scenarios
     * 1. It could start a timed flush thread if it was not already active
     *    when the BulkInserter was created.
     * 2. If the timed flush thread was already active then setting this to a
     *    new value will first terminate the existing thread; set the options
     *    to the new value and finally restart a new thread with the options
     *    passed in. This case could result in a delay since the thread needs
     *    to be cleaned up and restarted.
     *
     * @param flushOptions - an instance of the {@link FlushOptions} class
     * @throws GPUdbException - in case an invalid timeout values is set in the
     *                          flushOptions parameter.
     */
    public void setTimedFlushOptions(FlushOptions flushOptions) throws GPUdbException {
        flushOptions = flushOptions == null ? FlushOptions.defaultOptions() : flushOptions;

        // Timed flush already active, terminate first
        if( timedFlushExecutorService != null && !timedFlushExecutorServiceTerminated) {
            terminateTimedFlushExecutor();
            GPUdbLogger.debug_with_info("Timed flush executor service terminated ...");
        }

        // If valid flush interval has been passed in, re-create the timedFlush
        // service thread
        if( flushOptions.getFlushInterval() > 0) {

            this.flushOptions = flushOptions;

            // Restart with new FlushOptions value
            timedFlushExecutorService = Executors.newSingleThreadScheduledExecutor();
            timedFlushExecutorService.scheduleWithFixedDelay(new TimedFlushTask(this),
                this.flushOptions.getFlushInterval(),
                this.flushOptions.getFlushInterval(),
                TimeUnit.SECONDS);

            //reset the state to false after restart
            timedFlushExecutorServiceTerminated = timedFlushExecutorService.isTerminated();
            GPUdbLogger.debug("Timed flush restarted, flush interval set to <" + this.flushOptions.getFlushInterval() + ">");
        } else {
            GPUdbLogger.debug("Timed flush turned off, flush interval set to negative value");
        }


    }

    /**
     * This method will be called automatically if the {@link BulkInserter} class is used in a
     * try-with-resources block. If not used that way it is mandatory to call this method
     * to initiate a smooth cleanup of the underlying resources. This method will terminate
     * the internal scheduler threads and call flush so that all pending updates to the database
     * are handled properly.
     *
     * <pre>
     *     try( BulkInserter inserter = new BulkInserter(...) ) {
     *         // Do something with the BulkInserter instance
     *         // inserter.{some_method}
     *     }
     *     // Here the close method of the BulkInserter class will be called
     *     // automatically
     * </pre>
     *
     * or
     *
     * <pre>
     *     BulkInserter inserter = new BulkInserter<>(...)
     *     // Invoke some methods on the inserter
     *     //Explicitly call close() method
     *     inserter.close();
     * </pre>
     *
     * @throws InsertException - While doing the final flush
     */
    @Override
    public void close() throws InsertException {
        // Call the flush method one last time
        InsertException ie = null;
        try {
            this.flush();
        } catch (InsertException e) {
            GPUdbLogger.error(e.getMessage());
            ie = e;
        }

        GPUdbLogger.debug_with_info("Terminating BulkInserter and cleaning up ...");

        //Terminate the scheduler thread
        terminateTimedFlushExecutor();

        // Terminate the worker thread pool
        terminateWorkerThreadPool();

        if( ie != null ) {
            throw ie;
        }
    }

    /**
     * Checks whether the user has the requisite permissions on the table to
     * use BulkInserter. This method us used in the constructor and if the
     * permissions are not found to be adequate then the constructor throws
     * an exception and bails out.
     *
     * @param gpudb     - the GPUdb instance
     * @param tableName - the name of the table for which the BulkInserter is being created
     * @param options - The options to be passed to "/insert/records" endpoint.
     * @return - true (in case the insert and optional update permissions exist) or false
     * @throws GPUdbException - in case the {@link GPUdb#hasPermission(String, String, String, String, Map)}  call fails
     */
    private boolean hasPermissionsForTable(final GPUdb gpudb, final String tableName, Map<String, String> options) throws GPUdbException {
        boolean insertPermissionOk = gpudb.hasPermission("", tableName, HasPermissionRequest.ObjectType.TABLE, HasPermissionRequest.Permission.INSERT, new HashMap<String, String>()).getHasPermission();
        boolean updatePermissionOk = true;

        // If the user requests upserts, check update permission on the table
        //   Note: either options or the map value could be null
        if (options != null) {
            String updateOnExistingPk = options.get(InsertRecordsRequest.Options.UPDATE_ON_EXISTING_PK);
            if (InsertRecordsRequest.Options.TRUE.equalsIgnoreCase(updateOnExistingPk))
                updatePermissionOk = gpudb.hasPermission("", tableName, HasPermissionRequest.ObjectType.TABLE, HasPermissionRequest.Permission.UPDATE, new HashMap<String, String>()).getHasPermission();
        }

        return (insertPermissionOk && updatePermissionOk);
    }

    private void terminateTimedFlushExecutor() {
        if( timedFlushExecutorService != null && !timedFlushExecutorService.isShutdown()) {
            timedFlushExecutorService.shutdown();
            try {
                if (!timedFlushExecutorService.awaitTermination( DEFAULT_THREADPOOL_TERMINATION_TIMEOUT, TimeUnit.SECONDS)) {
                    timedFlushExecutorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                timedFlushExecutorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            GPUdbLogger.debug_with_info("Terminated scheduler thread ...");
            timedFlushExecutorServiceTerminated = timedFlushExecutorService.isTerminated();
            timedFlushExecutorService = null;
        }
    }

    private void terminateWorkerThreadPool() {
        if( this.workerExecutorService != null && !workerExecutorService.isShutdown()) {
            workerExecutorService.shutdown();
            try {
                if (!workerExecutorService.awaitTermination( DEFAULT_THREADPOOL_TERMINATION_TIMEOUT, TimeUnit.SECONDS)) {
                    workerExecutorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                workerExecutorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            GPUdbLogger.debug_with_info("Terminated worker thread pool ...");
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
            } catch (GPUdbBase.GPUdbHAUnavailableException | GPUdbBase.GPUdbFailoverDisabledException ex ) {
                // Have tried all clusters; back to square 1
                throw ex;
            } // Failover is disabled


            // Update the reference points
            currURL                  = this.gpudb.getURL();
            currCountClusterSwitches = this.gpudb.getNumClusterSwitches();

            // We did switch to a different cluster; now check the health
            // of the cluster, starting with the head node
            if ( !this.gpudb.isKineticaRunning( currURL ) ) {
                continue; // try the next cluster because this head node is down
            }

            boolean isClusterHealthy = true;
            if ( this.multiHeadEnabled) {
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
     * @param countClusterSwitches  Integer keeping track of how many times HA
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
                                                             retryCount,
                                                             this.options,
                                                             this.typeObjectMap ) );
                }
            } catch (Exception ex) {
                throw new GPUdbException( ex.getMessage(), ex );
            }
        }

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

    public boolean isMultiHeadEnabled() throws GPUdbException {
        return multiHeadEnabled;
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
     * Gets the list of errors received since the last call to getErrors().
     *
     * @return  list of InsertException objects
     */
    public List<InsertException> getErrors()
    {
        synchronized (error_list_lock)
        {
            List<InsertException> copy = error_list; // Make a copy to return
            error_list = new ArrayList<>(); // Make a new list for the future
            return copy;
        }
    }

    /**
     * Gets the list of warnings received since the last call to getWarnings().
     *
     * @return  list of InsertException objects
     */
    public List<InsertException> getWarnings()
    {
        synchronized (error_list_lock)
        {
            List<InsertException> copy = warning_list; // Make a copy to return
            warning_list = new ArrayList<>(); // Make a new list for the future
            return copy;
        }
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
        // retry based on user configuration.  Note the last param
        // lets the called method know that the user is forcing this flush;
        // this is important for recursive calls.
        this.flush( this.retryCount );
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
    private void flush( int retryCount ) throws InsertException {
        // Flush all queues, regardless of how full they are.  Also, we will
        // retry based on user configuration.  Note the last param
        // lets the called method know that the user is forcing this flush;
        // this is important for recursive calls.
        this.flushQueues( this.workerQueues, retryCount, true );
    }


    /**
     * Flush the records of a given worker queue.
     */
    private void flush( WorkerQueue<T> workerQueue ) throws InsertException {
        // Flush only the given single queue.  Also, we will retry based on user
        // configuration.
        List<WorkerQueue<T>> workerQueues = new ArrayList<>();
        workerQueues.add( workerQueue );
        this.flushQueues( workerQueues, this.retryCount, false );
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
            if ( workerQueue == null) {
                continue;
            }

            // We will flush only full queues
            if ( workerQueue.isQueueFull() ) {
                GPUdbLogger.debug_with_info( "Adding full queue for "
                                             + workerQueue.getUrl() );
                fullQueues.add( workerQueue );
            }

        }

        GPUdbLogger.debug_with_info( "Before calling flushQueues()" );
        this.flushQueues( fullQueues, retryCount, false );
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
     * @param forcedFlush boolean indicating if the user wants a forced flush
     *                    of all the records.  Useful in error cases when
     *                    insertion retries happen.
     *
     * @throws InsertException if an error occurs while flushing
     */
    private void flushQueues( List<WorkerQueue<T>> queues, int retryCount,
                              boolean forcedFlush )
        throws InsertException {
        GPUdbLogger.debug_with_info( "Begin; # queues " + queues.size()
                                     + "; retryCount: " + retryCount );
        // Let the user know that we ran out of retries
        if ( ( retryCount < 0 ) || (queues.size() == 0) ) {
            // Retry count of 0 means try once but do no retry
            GPUdbLogger.debug_with_info( "Returning without further action" );
            return;
        }

        // Create an execution completion service that will let each queue
        // work in an independent parallel thread.  We will consume the
        // result of the queues as they complete (in the order of completion).
        CompletionService<WorkerQueueInsertionResult<T>> queueService
            = new ExecutorCompletionService<>( this.workerExecutorService );

        // Add all the given tasks to the service
        int countQueueSubmitted = 0;
        for (WorkerQueue<T> workerQueue : queues) {

            // Handle removed ranks
            if ( (workerQueue == null) || workerQueue.queue.isEmpty() ) {
                GPUdbLogger.debug_with_info( "Skipping null/empty worker queue" );
                continue;
            }

            if (simulateErrorMode) {
                workerQueue.clear();
                continue;
            }

            // Submit each extant worker queue to the service
            queueService.submit( workerQueue );
            ++countQueueSubmitted;
        }
        GPUdbLogger.debug_with_info( "# queues submitted: " + countQueueSubmitted );

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
                if (ex instanceof ExecutionException ) {
                    // Some error occurred in the task being executed
                    // Need to report
                    final URL url = queues.get(i).getUrl();
                    GPUdbLogger.debug_with_info(String.format("Error in inserting data for queue with URL : %s : Error message # %s", url, ((ExecutionException) ex).getMessage()));
                    workerExceptions.add(ex);
                }
                if( ex instanceof InterruptedException ) {
                    GPUdbLogger.debug_with_info(String.format("Thread interrupted : %s", ((InterruptedException) ex).getMessage()));
                }
                continue;
            }

            // Empty queues would return null results
            if ( result ==  null ) {
                continue;
            }

            // Handle the result, if any
            if ( result.getDidSucceed() ) {
                GPUdbLogger.debug_with_info( "Flush thread succeeded" );
                // The insertion for this queue succeeded; aggregate the results
                countInserted.addAndGet( result
                                         .getInsertResponse()
                                         .getCountInserted() );
                countUpdated.addAndGet(  result
                                         .getInsertResponse()
                                         .getCountUpdated()  );

                gatherErrorsFromInsertionResult( result );

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
                if( result.getFailureException().getMessage().equalsIgnoreCase("access denied")) {
                    // No point retrying anymore, we won't succeed until the permissions
                    // are available.
                    throw new InsertException(getCurrentHeadNodeURL(), result.getFailedRecords(), String.format("No permissions on table %s for inserting/updating records", tableName));
                }

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
            GPUdbLogger.warn( "Original causes of failure from ranks: "
                              + originalCauses );
        }


        // This is the scenario where we are re-trying without attempting a
        // failover. We would recurse till the set retryCount becomes 0 and
        // subsequently attempt an HA failover.
        if ( doRetryInsertion && retryCount > 0 ) {
            // We need to re-attempt inserting the records that did not get
            // ingested.
            GPUdbLogger.debug_with_info( "Retry insertion" );

            GPUdbLogger.debug_with_info( "Decreasing retry count from: "
                + retryCount );
            --retryCount;
            GPUdbLogger.debug_with_info( "retryCount: " + retryCount );

            // Retry insertion of the failed records (recursive call to our
            // private insertion with the retry count decreased to halt
            // the recursion as needed
            boolean couldRetry = this.insert( failedRecords, retryCount );

            if ( couldRetry ) {
                // If this is part of a user-initiated forced flush, then we
                // need to call the flush method again (otherwise, failed records
                // may have been queued but not actually inserted).
                if ( forcedFlush ) {
                    GPUdbLogger.debug_with_info( "Before forced flush" );
                    this.flush( retryCount );
                    GPUdbLogger.debug_with_info( "After forced flush" );
                }
                return;
            }
            GPUdbLogger.debug_with_info( "End flushQueues" );
        }

        // We ran out of chances to retry.  Let the user know this and
        // pass along the records that we could not insert.
        if( doRetryInsertion && retryCount <= 0 && this.gpudb.getHARingSize() == 1 ) {
            String message = ("Insertion failed; ran out of retries.  "
                + "Original causes encountered by workers: "
                + originalCauses);
            throw new InsertException( currHeadUrl, failedRecords, message );
        }

        // Failover if needed
        if ( doFailover ) {
            try {
                // Switch to a different cluster in the HA ring, if any
                // TODO: Check which head node url needs to be used here
                GPUdbLogger.debug_with_info( "Before calling forceFailover() "
                                             + "with current head URL: "
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
                GPUdbLogger.debug_with_info( "Before calling updateWorkerQueues()" );
                updateWorkerQueues( latestCountClusterSwitches );
            } catch (Exception ex) {
                GPUdbLogger.debug_with_info( "updateWorkerQueues() failed with "
                                             + "exception: "
                                             + ex.getMessage() );
                // Let the user know that there was a problem and which records
                // could not be inserted
                throw new InsertException( currHeadUrl, failedRecords,
                                           ex.getMessage(), ex);
            }
        }

        // Here we reset the local retrycount variable to what was set as the
        // value of the instance variable retryCount. This block will retry
        // insertion after an HA failover has taken place, so it will start with
        // original retryCount once more into the new cluster.
        retryCount = this.retryCount;
        if ( doRetryInsertion ) {
            // We need to re-attempt inserting the records that did not get
            // ingested.
            GPUdbLogger.debug_with_info( "Retry insertion" );

            GPUdbLogger.debug_with_info( "retryCount: " + retryCount );

            // Retry insertion of the failed records (recursive call to our
            // private insertion with the retry count decreased to halt
            // the recursion as needed
            this.insert( failedRecords, retryCount );

            GPUdbLogger.debug_with_info( "End flushQueues" );
        }
    }   // end flushQueues

    private void gatherErrorsFromInsertionResult(WorkerQueueInsertionResult<T> result) {
        Map<Integer, Pair<String, T>> errors = result.getErrors();
        if (errors != null) {
            errors.forEach((key,entry) -> {
                simulateErrorMode |= returnIndividualErrors;
                List<T> records = new ArrayList<>();
                if (entry.getRight() != null)
                    records.add(entry.getRight());
                synchronized (error_list_lock) {
                    String message = entry.getLeft();
                    message = message.substring( message.indexOf(":") + 1);
                    error_list.add(new InsertException(result.headUrl, records, message));
                }
            });
        }

        List<String> warnings = result.getWarnings();
        if (warnings != null) {
            synchronized (error_list_lock) {
                warnings.forEach((entry) -> {
                    warning_list.add(new InsertException(result.headUrl, null, entry));
                });
            }
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
     * @param flushWhenFull  boolean flag indicating if the queue should be flushed
     *                       if it is full after inserting this record.
     *
     * @throws GPUdbException if an error occurs while calculating shard/primary keys
     * @throws InsertException if an error occurs while inserting
     */
    private void insert(T record, boolean flushWhenFull) throws InsertException {
        RecordKey shardKey = null;

        // Don't accept new records if there was an error thrown already with returnIndividualErrors
        if (simulateErrorMode)
            return;

        try {
            shardKey = this.shardKeyBuilder.build(record);
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
            workerQueue.insert(record);

            // Flush the queue if it is full
            if ( flushWhenFull && workerQueue.isQueueFull() ) {
                this.flush( workerQueue );
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
        // Don't accept new records if there was an error thrown already with returnIndividualErrors
        if (simulateErrorMode)
            return true;

        // Let the user know that we ran out of retries
        if ( retryCount < 0 ) {
            // Retry count of 0 means try once but do no retry
            GPUdbLogger.debug_with_info( "retryCount: " + retryCount
                                         + "; returning false" );
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
        GPUdbLogger.debug_with_info( "Before flushing full queues" );
        this.flushFullQueues( retryCount );
        GPUdbLogger.debug_with_info( "After flushing full queues" );

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
    public void insert(List<T> records) throws InsertException {
        // Try to insert all the records with the alotted retry count
        this.insert( records, this.retryCount );
    }
}
