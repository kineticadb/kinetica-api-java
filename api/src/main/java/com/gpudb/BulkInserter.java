package com.gpudb;

import com.gpudb.GPUdbBase.GPUdbExitException;
import com.gpudb.GPUdbBase.GPUdbVersion;
import com.gpudb.protocol.*;
import com.gpudb.util.json.JsonUtils;
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
import java.util.concurrent.atomic.AtomicBoolean;
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

    // The default number of times a batch is re-sent to the same rank
    private static final int DEFAULT_INSERTION_RETRY_COUNT = 7;

    //Number of seconds to wait for the thread pools (scheduler and worker) to terminate
    private static final int DEFAULT_THREADPOOL_TERMINATION_TIMEOUT = 30; //In seconds

    // Table members
    private final GPUdb gpudb;
    private final String tableName;
    private final TypeObjectMap<T> typeObjectMap;
    private final int batchSize;
    private final Map<String, String> options;
    private final boolean isJson;
    private final boolean isTableReplicated;
    private final boolean tableHasPrimaryKey;

    // Ingest processing members
    private final ExecutorService workerExecutorService;
    private ScheduledExecutorService timedFlushExecutorService;
    private boolean timedFlushExecutorServiceTerminated;
    private FlushOptions flushOptions;
    private GPUdbBase.JsonOptions jsonOptions;
    private volatile int maxRetries;
    private final boolean returnIndividualErrors;
    private final AtomicLong countInserted = new AtomicLong();
    private final AtomicLong countUpdated = new AtomicLong();
    private List<InsertException> errorList = new ArrayList<>();
    private List<InsertException> warningList = new ArrayList<>();
    private final Object errorListLock = new Object();
    private volatile boolean simulateErrorMode = false; // Simulate returnIndividualErrors after an error

    // Whether this BulkInserter has been closed; further requests are rejected.
    private final AtomicBoolean closed = new AtomicBoolean( false );
    private final Object closeMutex = new Object();

    // Sharding members
    private com.gpudb.WorkerList workerList;
    private final RecordKeyBuilder<T> shardKeyBuilder;
    private long shardVersion;
    private MutableLong shardUpdateTime;

    /**
     * Where records get routed, published as a single immutable value.
     * <p>
     * The destination of a record depends on three things that have to agree
     * with one another: whether to use the head node, the list of worker
     * queues, and the shard mapping.  Holding them in one immutable object
     * behind one {@code volatile} reference means a rebuild cannot be observed
     * part way through: a reader sees either the whole old state or the whole
     * new one, and gets the happens-before edge that makes the queues it finds
     * safe to use.
     * <p>
     * Every read on the insert path must take this reference <i>once</i> into a
     * local and use that local throughout.  Re-reading the field mid-decision
     * reintroduces exactly the inconsistency the snapshot exists to prevent.
     */
    private volatile Routing<T> routing;

    /**
     * The shard mapping most recently fetched from the server, which may not
     * have been published yet; see
     * {@link #updateWorkerQueues(boolean, boolean)}.  Only ever touched
     * while holding this object's monitor, and never read on the insert path.
     */
    private List<Integer> pendingRoutingTable;

    // HA members
    private URL currentHeadNodeURL;

    /**
     * The multi-head config the worker list was last built from, compared by
     * <b>identity</b> to detect that the connection's addresses have been
     * replaced -- by a move to another cluster, or by a fresh probe of the one
     * it is already on.
     */
    private GPUdbBase.MultiHeadSnapshot lastMultiHeadSnapshot;

    private final Object haFailoverLock;

    // Version that supports column defaults
    private static final GPUdbVersion DEFAULTS_VERSION = new GPUdbVersion(7, 2, 3, 18);


    /**
     * Creates a JSON {@link BulkInserter} with the specified parameters.
     * <br/>
     * This constructor will attempt to automatically configure multi-head
     * ingest, if available.
     * <br/>
     * It will also use default settings for the
     * {@link GPUdb#insertRecords(String, List, Map)} call.  Details can be
     * found at {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
     * 
     * @param gpudb        the {@link GPUdb} instance to insert records into
     * @param tableName    name of the table to insert records into
     * @param batchSize    the number of records to insert into GPUdb at a time
     *                     (records will queue until this number is reached);
     *                     for multi-head ingest, this value is per worker
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     *
     * @see com.gpudb.protocol.InsertRecordsRequest.Options
     */
    public BulkInserter(GPUdb gpudb,
                        String tableName,
                        int batchSize) throws GPUdbException {
        this(gpudb, tableName, null, null, batchSize, null, null, FlushOptions.defaultOptions(), GPUdbBase.JsonOptions.defaultOptions());
    }

    /**
     * Creates a JSON {@link BulkInserter} with the specified parameters.
     * <br/>
     * This constructor will attempt to automatically configure multi-head
     * ingest, if available.
     * 
     * @param gpudb        the {@link GPUdb} instance to insert records into
     * @param tableName    name of the table to insert records into
     * @param batchSize    the number of records to insert into GPUdb at a time
     *                     (records will queue until this number is reached);
     *                     for multi-head ingest, this value is per worker
     * @param options      optional parameters to pass to GPUdb while inserting
     *                     ({@code null} for no parameters)
     *                     <br/>
     *                     This is the same set of options as accepted by the
     *                     {@link GPUdb#insertRecords(String, List, Map)} call.
     *                     <br/>
     *                     The details can be found at
     *                     {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     *
     * @see com.gpudb.protocol.InsertRecordsRequest.Options
     */
    public BulkInserter(GPUdb gpudb,
                        String tableName,
                        int batchSize,
                        Map<String, String> options) throws GPUdbException {
        this(gpudb, tableName, null, null, batchSize, options, null, FlushOptions.defaultOptions(), GPUdbBase.JsonOptions.defaultOptions());
    }

    /**
     * Creates a JSON {@link BulkInserter} with the specified parameters.
     *
     * @param gpudb        the {@link GPUdb} instance to insert records into
     * @param tableName    name of the table to insert records into
     * @param batchSize    the number of records to insert into GPUdb at a time
     *                     (records will queue until this number is reached);
     *                     for multi-head ingest, this value is per worker
     * @param options      optional parameters to pass to GPUdb while inserting
     *                     ({@code null} for no parameters)
     *                     <br/>
     *                     This is the same set of options as accepted by the
     *                     {@link GPUdb#insertRecords(String, List, Map)} call.
     *                     <br/>
     *                     The details can be found at
     *                     {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
     * @param workers      worker list for multi-head ingest; use an empty worker
     *                     list ({@code new WorkerList()}) to disable multi-head
     *                     for this inserter, which leaves the connection's
     *                     fail-over intact and is not undone by a later rebuild
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     *
     * @see com.gpudb.protocol.InsertRecordsRequest.Options
     */
    public BulkInserter(GPUdb gpudb,
                        String tableName,
                        int batchSize,
                        Map<String, String> options,
                        com.gpudb.WorkerList workers) throws GPUdbException {
        this(gpudb, tableName, null, null, batchSize, options, workers, FlushOptions.defaultOptions(), GPUdbBase.JsonOptions.defaultOptions());
    }

    /**
     * Creates a JSON {@link BulkInserter} with the specified parameters.
     *
     * @param gpudb        the {@link GPUdb} instance to insert records into
     * @param tableName    name of the table to insert records into
     * @param batchSize    the number of records to insert into GPUdb at a time
     *                     (records will queue until this number is reached);
     *                     for multi-head ingest, this value is per worker
     * @param options      optional parameters to pass to GPUdb while inserting
     *                     ({@code null} for no parameters)
     *                     <br/>
     *                     This is the same set of options as accepted by the
     *                     {@link GPUdb#insertRecords(String, List, Map)} call.
     *                     <br/>
     *                     The details can be found at
     *                     {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
     * @param workers      worker list for multi-head ingest; use an empty worker
     *                     list ({@code new WorkerList()}) to disable multi-head
     *                     for this inserter, which leaves the connection's
     *                     fail-over intact and is not undone by a later rebuild
     * @param flushOptions {@link FlushOptions} to use for timed flush operation
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
                        int batchSize,
                        Map<String, String> options,
                        com.gpudb.WorkerList workers,
                        FlushOptions flushOptions) throws GPUdbException {
        this(gpudb, tableName, null, null, batchSize, options, workers, flushOptions, GPUdbBase.JsonOptions.defaultOptions());
    }

    /**
     * Creates a JSON {@link BulkInserter} with the specified parameters.
     *
     * @param gpudb        the {@link GPUdb} instance to insert records into
     * @param tableName    name of the table to insert records into
     * @param batchSize    the number of records to insert into GPUdb at a time
     *                     (records will queue until this number is reached);
     *                     for multi-head ingest, this value is per worker
     * @param options      optional parameters to pass to GPUdb while inserting
     *                     ({@code null} for no parameters)
     *                     <br/>
     *                     This is the same set of options as accepted by the
     *                     {@link GPUdb#insertRecords(String, List, Map)} call.
     *                     <br/>
     *                     The details can be found at
     *                     {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
     * @param workers      worker list for multi-head ingest; use an empty worker
     *                     list ({@code new WorkerList()}) to disable multi-head
     *                     for this inserter, which leaves the connection's
     *                     fail-over intact and is not undone by a later rebuild
     * @param jsonOptions  {@link GPUdbBase.JsonOptions} to use for JSON ingest
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     *
     * @see com.gpudb.protocol.InsertRecordsRequest.Options
     * @see FlushOptions
     * @see GPUdbBase.JsonOptions
     */
    public BulkInserter(GPUdb gpudb,
                        String tableName,
                        int batchSize,
                        Map<String, String> options,
                        com.gpudb.WorkerList workers,
                        GPUdbBase.JsonOptions jsonOptions) throws GPUdbException {
        this(gpudb, tableName, null, null, batchSize, options, workers, FlushOptions.defaultOptions(), jsonOptions);
    }

    /**
     * Creates a JSON {@link BulkInserter} with the specified parameters.
     *
     * @param gpudb        the {@link GPUdb} instance to insert records into
     * @param tableName    name of the table to insert records into
     * @param batchSize    the number of records to insert into GPUdb at a time
     *                     (records will queue until this number is reached);
     *                     for multi-head ingest, this value is per worker
     * @param options      optional parameters to pass to GPUdb while inserting
     *                     ({@code null} for no parameters)
     *                     <br/>
     *                     This is the same set of options as accepted by the
     *                     {@link GPUdb#insertRecords(String, List, Map)} call.
     *                     <br/>
     *                     The details can be found at
     *                     {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
     * @param workers      worker list for multi-head ingest; use an empty worker
     *                     list ({@code new WorkerList()}) to disable multi-head
     *                     for this inserter, which leaves the connection's
     *                     fail-over intact and is not undone by a later rebuild
     * @param flushOptions {@link FlushOptions} to use for timed flush operation
     * @param jsonOptions  {@link GPUdbBase.JsonOptions} to use for JSON ingest
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     *
     * @see com.gpudb.protocol.InsertRecordsRequest.Options
     * @see FlushOptions
     * @see GPUdbBase.JsonOptions
     */
    public BulkInserter(GPUdb gpudb,
                        String tableName,
                        int batchSize,
                        Map<String, String> options,
                        com.gpudb.WorkerList workers,
                        FlushOptions flushOptions,
                        GPUdbBase.JsonOptions jsonOptions) throws GPUdbException {
        this(gpudb, tableName, null, null, batchSize, options, workers, flushOptions, jsonOptions);
    }


    /**
     * Creates a {@link BulkInserter} with the specified parameters.
     * <br/>
     * This constructor will attempt to automatically configure multi-head
     * ingest, if available.
     * <br/>
     * It will also use default settings for the
     * {@link GPUdb#insertRecords(String, List, Map)} call.  Details can be
     * found at {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
     * 
     * @param gpudb        the {@link GPUdb} instance to insert records into
     * @param tableName    name of the table to insert records into
     * @param type         the {@link Type} of records being inserted
     * @param batchSize    the number of records to insert into GPUdb at a time
     *                     (records will queue until this number is reached);
     *                     for multi-head ingest, this value is per worker
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
                        int batchSize) throws GPUdbException {
        this(gpudb, tableName, type, null, batchSize, null, null, FlushOptions.defaultOptions(), null);
    }

    /**
     * Creates a {@link BulkInserter} with the specified parameters.
     * <br/>
     * This constructor will attempt to automatically configure multi-head
     * ingest, if available.
     * 
     * @param gpudb        the {@link GPUdb} instance to insert records into
     * @param tableName    name of the table to insert records into
     * @param type         the {@link Type} of records being inserted
     * @param batchSize    the number of records to insert into GPUdb at a time
     *                     (records will queue until this number is reached);
     *                     for multi-head ingest, this value is per worker
     * @param options      optional parameters to pass to GPUdb while inserting
     *                     ({@code null} for no parameters)
     *                     <br/>
     *                     This is the same set of options as accepted by the
     *                     {@link GPUdb#insertRecords(String, List, Map)} call.
     *                     <br/>
     *                     The details can be found at
     *                     {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
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
        this(gpudb, tableName, type, null, batchSize, options, null, FlushOptions.defaultOptions(), null);
    }

    /**
     * Creates a {@link BulkInserter} with the specified parameters.
     *
     * @param gpudb        the {@link GPUdb} instance to insert records into
     * @param tableName    name of the table to insert records into
     * @param type         the {@link Type} of records being inserted
     * @param batchSize    the number of records to insert into GPUdb at a time
     *                     (records will queue until this number is reached);
     *                     for multi-head ingest, this value is per worker
     * @param options      optional parameters to pass to GPUdb while inserting
     *                     ({@code null} for no parameters)
     *                     <br/>
     *                     This is the same set of options as accepted by the
     *                     {@link GPUdb#insertRecords(String, List, Map)} call.
     *                     <br/>
     *                     The details can be found at
     *                     {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
     * @param workers      worker list for multi-head ingest; use an empty worker
     *                     list ({@code new WorkerList()}) to disable multi-head
     *                     for this inserter, which leaves the connection's
     *                     fail-over intact and is not undone by a later rebuild
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
        this(gpudb, tableName, type, null, batchSize, options, workers, FlushOptions.defaultOptions(), null);
    }

    /**
     * Creates a {@link BulkInserter} with the specified parameters.
     *
     * @param gpudb        the {@link GPUdb} instance to insert records into
     * @param tableName    name of the table to insert records into
     * @param type         the {@link Type} of records being inserted
     * @param batchSize    the number of records to insert into GPUdb at a time
     *                     (records will queue until this number is reached);
     *                     for multi-head ingest, this value is per worker
     * @param options      optional parameters to pass to GPUdb while inserting
     *                     ({@code null} for no parameters)
     *                     <br/>
     *                     This is the same set of options as accepted by the
     *                     {@link GPUdb#insertRecords(String, List, Map)} call.
     *                     <br/>
     *                     The details can be found at
     *                     {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
     * @param workers      worker list for multi-head ingest; use an empty worker
     *                     list ({@code new WorkerList()}) to disable multi-head
     *                     for this inserter, which leaves the connection's
     *                     fail-over intact and is not undone by a later rebuild
     * @param flushOptions {@link FlushOptions} to use for timed flush operation
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
        this(gpudb, tableName, type, null, batchSize, options, workers, flushOptions, null);
    }

    /**
     * Creates a {@link BulkInserter} with the specified parameters.
     *
     * @param gpudb        the {@link GPUdb} instance to insert records into
     * @param tableName    name of the table to insert records into
     * @param type         the {@link Type} of records being inserted
     * @param batchSize    the number of records to insert into GPUdb at a time
     *                     (records will queue until this number is reached);
     *                     for multi-head ingest, this value is per worker
     * @param options      optional parameters to pass to GPUdb while inserting
     *                     ({@code null} for no parameters)
     *                     <br/>
     *                     This is the same set of options as accepted by the
     *                     {@link GPUdb#insertRecords(String, List, Map)} call.
     *                     <br/>
     *                     The details can be found at
     *                     {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
     * @param workers      worker list for multi-head ingest; use an empty worker
     *                     list ({@code new WorkerList()}) to disable multi-head
     *                     for this inserter, which leaves the connection's
     *                     fail-over intact and is not undone by a later rebuild
     * @param jsonOptions  {@link GPUdbBase.JsonOptions} to use for JSON ingest
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     *
     * @see com.gpudb.protocol.InsertRecordsRequest.Options
     * @see FlushOptions
     * @see GPUdbBase.JsonOptions
     */
    public BulkInserter(GPUdb gpudb,
                        String tableName,
                        Type type,
                        int batchSize,
                        Map<String, String> options,
                        com.gpudb.WorkerList workers,
                        GPUdbBase.JsonOptions jsonOptions) throws GPUdbException {
        this(gpudb, tableName, type, null, batchSize, options, workers, FlushOptions.defaultOptions(), null);
    }

    /**
     * Creates a {@link BulkInserter} with the specified parameters.
     *
     * @param gpudb        the {@link GPUdb} instance to insert records into
     * @param tableName    name of the table to insert records into
     * @param type         the {@link Type} of records being inserted
     * @param batchSize    the number of records to insert into GPUdb at a time
     *                     (records will queue until this number is reached);
     *                     for multi-head ingest, this value is per worker
     * @param options      optional parameters to pass to GPUdb while inserting
     *                     ({@code null} for no parameters)
     *                     <br/>
     *                     This is the same set of options as accepted by the
     *                     {@link GPUdb#insertRecords(String, List, Map)} call.
     *                     <br/>
     *                     The details can be found at
     *                     {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
     * @param workers      worker list for multi-head ingest; use an empty worker
     *                     list ({@code new WorkerList()}) to disable multi-head
     *                     for this inserter, which leaves the connection's
     *                     fail-over intact and is not undone by a later rebuild
     * @param flushOptions {@link FlushOptions} to use for timed flush operation
     * @param jsonOptions  {@link GPUdbBase.JsonOptions} to use for JSON ingest
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     *
     * @see com.gpudb.protocol.InsertRecordsRequest.Options
     * @see FlushOptions
     * @see GPUdbBase.JsonOptions
     */
    public BulkInserter(GPUdb gpudb,
                        String tableName,
                        Type type,
                        int batchSize,
                        Map<String, String> options,
                        com.gpudb.WorkerList workers,
                        FlushOptions flushOptions,
                        GPUdbBase.JsonOptions jsonOptions) throws GPUdbException {
        this(gpudb, tableName, type, null, batchSize, options, workers, flushOptions, null);
    }


    /**
     * Creates a {@link BulkInserter} with the specified parameters.
     * <br/>
     * This constructor will attempt to automatically configure multi-head
     * ingest, if available.
     * <br/>
     * It will also use default settings for the
     * {@link GPUdb#insertRecords(String, List, Map)} call.  Details can be
     * found at {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
     * 
     * @param gpudb         the {@link GPUdb} instance to insert records into
     * @param tableName     name of the table to insert records into
     * @param typeObjectMap the {@link TypeObjectMap} for the type of records
     *                      being inserted
     * @param batchSize     the number of records to insert into GPUdb at a time
     *                      (records will queue until this number is reached);
     *                      for multi-head ingest, this value is per worker
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
                        int batchSize) throws GPUdbException {
        this(gpudb, tableName, typeObjectMap.getType(), typeObjectMap, batchSize, null, null, FlushOptions.defaultOptions(), null);
    }

    /**
     * Creates a {@link BulkInserter} with the specified parameters.
     * <br/>
     * This constructor will attempt to automatically configure multi-head
     * ingest, if available.
     * 
     * @param gpudb         the {@link GPUdb} instance to insert records into
     * @param tableName     name of the table to insert records into
     * @param typeObjectMap the {@link TypeObjectMap} for the type of records
     *                      being inserted
     * @param batchSize     the number of records to insert into GPUdb at a time
     *                      (records will queue until this number is reached);
     *                      for multi-head ingest, this value is per worker
     * @param options       optional parameters to pass to GPUdb while inserting
     *                      ({@code null} for no parameters)
     *                      <br/>
     *                      This is the same set of options as accepted by the
     *                      {@link GPUdb#insertRecords(String, List, Map)} call.
     *                      <br/>
     *                      The details can be found at
     *                      {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
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
        this(gpudb, tableName, typeObjectMap.getType(), typeObjectMap, batchSize, options, null, FlushOptions.defaultOptions(), null);
    }

    /**
     * Creates a {@link BulkInserter} with the specified parameters.
     *
     * @param gpudb         the {@link GPUdb} instance to insert records into
     * @param tableName     name of the table to insert records into
     * @param typeObjectMap the {@link TypeObjectMap} for the type of records
     *                      being inserted
     * @param batchSize     the number of records to insert into GPUdb at a time
     *                      (records will queue until this number is reached);
     *                      for multi-head ingest, this value is per worker
     * @param options       optional parameters to pass to GPUdb while inserting
     *                      ({@code null} for no parameters)
     *                      <br/>
     *                      This is the same set of options as accepted by the
     *                      {@link GPUdb#insertRecords(String, List, Map)} call.
     *                      <br/>
     *                      The details can be found at
     *                      {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
     * @param workers       worker list for multi-head ingest; use an empty worker
     *                      list ({@code new WorkerList()}) to disable multi-head
     *                      for this inserter, which leaves the connection's
     *                      fail-over intact and is not undone by a later rebuild
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
        this(gpudb, tableName, typeObjectMap.getType(), typeObjectMap, batchSize, options, workers, FlushOptions.defaultOptions(), null);
    }

    /**
     * Creates a {@link BulkInserter} with the specified parameters.
     *
     * @param gpudb         the {@link GPUdb} instance to insert records into
     * @param tableName     name of the table to insert records into
     * @param typeObjectMap the {@link TypeObjectMap} for the type of records
     *                      being inserted
     * @param batchSize     the number of records to insert into GPUdb at a time
     *                      (records will queue until this number is reached);
     *                      for multi-head ingest, this value is per worker
     * @param options       optional parameters to pass to GPUdb while inserting
     *                      ({@code null} for no parameters)
     *                      <br/>
     *                      This is the same set of options as accepted by the
     *                      {@link GPUdb#insertRecords(String, List, Map)} call.
     *                      <br/>
     *                      The details can be found at
     *                      {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
     * @param workers       worker list for multi-head ingest; use an empty worker
     *                      list ({@code new WorkerList()}) to disable multi-head
     *                      for this inserter, which leaves the connection's
     *                      fail-over intact and is not undone by a later rebuild
     * @param flushOptions  {@link FlushOptions} to use for timed flush operation
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     *
     * @see com.gpudb.protocol.InsertRecordsRequest.Options
     * @see FlushOptions
     * @see GPUdbBase.JsonOptions
     */
    public BulkInserter(GPUdb gpudb,
                        String tableName,
                        TypeObjectMap<T> typeObjectMap,
                        int batchSize,
                        Map<String, String> options,
                        com.gpudb.WorkerList workers,
                        FlushOptions flushOptions) throws GPUdbException {
        this(gpudb, tableName, typeObjectMap.getType(), typeObjectMap, batchSize, options, workers, flushOptions, null);
    }

    /**
     * Creates a {@link BulkInserter} with the specified parameters.
     *
     * @param gpudb         the {@link GPUdb} instance to insert records into
     * @param tableName     name of the table to insert records into
     * @param typeObjectMap the {@link TypeObjectMap} for the type of records
     *                      being inserted
     * @param batchSize     the number of records to insert into GPUdb at a time
     *                      (records will queue until this number is reached);
     *                      for multi-head ingest, this value is per worker
     * @param options       optional parameters to pass to GPUdb while inserting
     *                      ({@code null} for no parameters)
     *                      <br/>
     *                      This is the same set of options as accepted by the
     *                      {@link GPUdb#insertRecords(String, List, Map)} call.
     *                      <br/>
     *                      The details can be found at
     *                      {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
     * @param workers       worker list for multi-head ingest; use an empty worker
     *                      list ({@code new WorkerList()}) to disable multi-head
     *                      for this inserter, which leaves the connection's
     *                      fail-over intact and is not undone by a later rebuild
     * @param jsonOptions   {@link GPUdbBase.JsonOptions} to use for JSON ingest
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     *
     * @see com.gpudb.protocol.InsertRecordsRequest.Options
     * @see FlushOptions
     * @see GPUdbBase.JsonOptions
     */
    public BulkInserter(GPUdb gpudb,
                        String tableName,
                        TypeObjectMap<T> typeObjectMap,
                        int batchSize,
                        Map<String, String> options,
                        com.gpudb.WorkerList workers,
                        GPUdbBase.JsonOptions jsonOptions) throws GPUdbException {
        this(gpudb, tableName, typeObjectMap.getType(), typeObjectMap, batchSize, options, workers, FlushOptions.defaultOptions(), null);
    }

    /**
     * Creates a {@link BulkInserter} with the specified parameters.
     *
     * @param gpudb         the {@link GPUdb} instance to insert records into
     * @param tableName     name of the table to insert records into
     * @param typeObjectMap the {@link TypeObjectMap} for the type of records
     *                      being inserted
     * @param batchSize     the number of records to insert into GPUdb at a time
     *                      (records will queue until this number is reached);
     *                      for multi-head ingest, this value is per worker
     * @param options       optional parameters to pass to GPUdb while inserting
     *                      ({@code null} for no parameters)
     *                      <br/>
     *                      This is the same set of options as accepted by the
     *                      {@link GPUdb#insertRecords(String, List, Map)} call.
     *                      <br/>
     *                      The details can be found at
     *                      {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
     * @param workers       worker list for multi-head ingest; use an empty worker
     *                      list ({@code new WorkerList()}) to disable multi-head
     *                      for this inserter, which leaves the connection's
     *                      fail-over intact and is not undone by a later rebuild
     * @param flushOptions  {@link FlushOptions} to use for timed flush operation
     * @param jsonOptions   {@link GPUdbBase.JsonOptions} to use for JSON ingest
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     *
     * @see com.gpudb.protocol.InsertRecordsRequest.Options
     * @see FlushOptions
     * @see GPUdbBase.JsonOptions
     */
    public BulkInserter(GPUdb gpudb,
                        String tableName,
                        TypeObjectMap<T> typeObjectMap,
                        int batchSize,
                        Map<String, String> options,
                        com.gpudb.WorkerList workers,
                        FlushOptions flushOptions,
                        GPUdbBase.JsonOptions jsonOptions) throws GPUdbException {
        this(gpudb, tableName, typeObjectMap.getType(), typeObjectMap, batchSize, options, workers, flushOptions, null);
    }


    /**
     * Creates a {@link BulkInserter} with the specified parameters.
     *
     * @param gpudb         the {@link GPUdb} instance to insert records into
     * @param tableName     name of the table to insert records into
     * @param type          the {@link Type} of records being inserted
     * @param typeObjectMap the {@link TypeObjectMap} for the type of records
     *                      being inserted
     * @param batchSize     the number of records to insert into GPUdb at a time
     *                      (records will queue until this number is reached);
     *                      for multi-head ingest, this value is per worker
     * @param options       optional parameters to pass to GPUdb while inserting
     *                      ({@code null} for no parameters)
     *                      <br/>
     *                      This is the same set of options as accepted by the
     *                      {@link GPUdb#insertRecords(String, List, Map)} call.
     *                      <br/>
     *                      The details can be found at
     *                      {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
     * @param workers       worker list for multi-head ingest; use an empty worker
     *                      list ({@code new WorkerList()}) to disable multi-head
     *                      for this inserter, which leaves the connection's
     *                      fail-over intact and is not undone by a later rebuild
     * @param flushOptions  {@link FlushOptions} to use for timed flush operation
     * @param jsonOptions   {@link GPUdbBase.JsonOptions} to use for JSON ingest
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     *
     * @see com.gpudb.protocol.InsertRecordsRequest.Options
     * @see FlushOptions
     * @see GPUdbBase.JsonOptions
     */
    private BulkInserter(GPUdb gpudb,
                         String tableName,
                         Type type,
                         TypeObjectMap<T> typeObjectMap,
                         int batchSize,
                         Map<String, String> options,
                         com.gpudb.WorkerList workers,
                         FlushOptions flushOptions,
                         GPUdbBase.JsonOptions jsonOptions
    ) throws GPUdbException {

        final boolean hasPermissions = hasPermissionsForTable(gpudb, tableName, options);
        if( !hasPermissions ) {
            String errorMsg = String.format("User %s doesn't have requisite permissions on the table %s to use BulkInserter", gpudb.getUsername(), tableName);
            throw new GPUdbException(errorMsg);
        }

        this.haFailoverLock = new Object();

        this.gpudb = gpudb;
        this.tableName = tableName;
        this.typeObjectMap = typeObjectMap;
        this.workerList    = workers;

        // Validate the batch size
        if (batchSize < 1) {
            throw new IllegalArgumentException("Batch size must be greater than zero.");
        }
        this.batchSize = batchSize;

        // By default, we will retry insertions once
        this.maxRetries = DEFAULT_INSERTION_RETRY_COUNT;

        this.options = (options == null) ? new HashMap<>() : new HashMap<>(options); // Make a (mutable) copy
        if ((type != null) && !DEFAULTS_VERSION.isNewerThan(gpudb.getServerVersion()))
            this.options.put("request_schema_str", type.getBaseDefinition());

        this.flushOptions = ( flushOptions == null ) ? FlushOptions.defaultOptions() : flushOptions;

        // Default is to set JSON validation to false.
        this.jsonOptions = jsonOptions;
        this.isJson = this.jsonOptions != null;

        // Initialize the thread pool for workers based on the resources
        // available on the system
        // N threads = N CPU * U CPU * (1 + W/C)
        // In this formula, NCPU is the number of cores, available through Runtime.getRuntime().availableProcessors()
        // U CPU is the target CPU use (between 0 and 1).
        // W/C is the ratio of wait time to compute time.
        this.workerExecutorService = Executors.newFixedThreadPool( Runtime
                .getRuntime()
                .availableProcessors() );

        // Initialize the shard version and update time
        this.shardVersion = 0;
        this.shardUpdateTime = new MutableLong();

        // Keep track of which cluster we're using (helpful in knowing if an
        // HA failover has happened)
        this.currentHeadNodeURL = gpudb.getURL();

        // Call /show/table to check table existence, replicated nature and
        // whether the table has a primary key
        ShowTableResponse tableInfo = null;
        try {
            tableInfo = GPUdbBase.ownRead( () -> gpudb.showTable(this.tableName, null) );
        }
        catch (GPUdbException ge) {
            throw new GPUdbException( "Table '" + this.tableName + "' does not exist!" );
        }
        List<String> tableDescriptions = tableInfo.getTableDescriptions().get(0);
        this.isTableReplicated = tableDescriptions.contains(ShowTableResponse.TableDescriptions.REPLICATED);
        this.tableHasPrimaryKey = hasPrimaryKey( tableInfo );

        // If no worker list is given, attempt to create one, by default, using
        // the given connection's work rank list
        if (this.workerList == null)
            this.workerList = new com.gpudb.WorkerList( this.gpudb.getCurrentMultiHeadSnapshot() );

        // Set if multi-head I/O is turned on at the server and rank URLs are accessible
        boolean isMultiHeadEnabled = ( (this.workerList != null) && !this.workerList.isEmpty() );

        // We should use the head node if multi-head is turned off at the server
        // or if we're working with a replicated table.  Routing states that rule;
        // it is needed here because it decides whether per-rank queues get built
        // at all, before any Routing exists to ask.
        boolean useHeadNode = Routing.useHeadNodeFor( isMultiHeadEnabled, this.isTableReplicated );

        this.shardKeyBuilder = (type == null ? null : new RecordKeyBuilder<>(type, typeObjectMap));

        List< WorkerQueue<T> > workerQueues = new ArrayList<>();

        try {

            // If we have multiple workers, then use those (unless the table
            // is replicated)
            if ( !useHeadNode ) {

                for (URL url : this.workerList) {
                    if (url == null) {
                        // Handle removed ranks
                        workerQueues.add( null );
                    } else {
                        URL insertURL = GPUdbBase.appendPathToURL( url, "/insert/records" );
                        workerQueues.add(new WorkerQueue<>(
                                this.gpudb,
                                insertURL,
                                this.tableName,
                                batchSize,
                                this.options,
                                (this.isJson ? this.jsonOptions : null),
                                this.typeObjectMap
                        ));
                    }
                }
            } else { // use the head node only for insertion
                URL insertURL = GPUdbBase.appendPathToURL( this.gpudb.getURL(), "/insert/records" );

                workerQueues.add(new WorkerQueue<>(
                        this.gpudb,
                        insertURL,
                        this.tableName,
                        this.batchSize,
                        this.options,
                        (this.isJson ? this.jsonOptions : null),
                        this.typeObjectMap
                ));
            }
        } catch (MalformedURLException ex) {
            throw new GPUdbException(ex.getMessage(), ex);
        }

        // Remember the initial multi-head state in order to detect changes to
        // the multi-head state after failover/failback/rebalance.
        this.lastMultiHeadSnapshot = this.gpudb.getCurrentMultiHeadSnapshot();

        // Publish the initial routing state as one value, before anything can
        // read it.  The shard mapping is not known yet; it is fetched below and
        // published as a snapshot of its own.
        this.routing = new Routing<>( isMultiHeadEnabled, this.isTableReplicated,
                                      workerQueues, null );

        if ( !useHeadNode ) {
            // Fetch the shard mapping that goes with these queues; don't
        	// rebuild the queues themselves
            updateWorkerQueues( false );
        }

        // The client only does its own "stop on error" emulation when the caller
        // asked for individual errors to be returned (so the server sends the
        // per-record errors it collects).
        boolean wantIndividualErrors = (options != null)
                && options.containsKey(InsertRecordsRequest.Options.RETURN_INDIVIDUAL_ERRORS)
                && options.get(InsertRecordsRequest.Options.RETURN_INDIVIDUAL_ERRORS)
                            .equals(InsertRecordsRequest.Options.TRUE);
        String errorHandling = (options != null) ? options.get("error_handling") : null;
        if (errorHandling != null && !errorHandling.isEmpty()) {
            this.returnIndividualErrors = wantIndividualErrors &&
                                          errorHandling.equalsIgnoreCase("abort");
        } else {
            this.returnIndividualErrors = wantIndividualErrors
                    && (!options.containsKey(InsertRecordsRequest.Options.ALLOW_PARTIAL_BATCH)
                        || !options.get(InsertRecordsRequest.Options.ALLOW_PARTIAL_BATCH)
                                .equals(InsertRecordsRequest.Options.TRUE));
        }

        // Create the scheduler only if the flush interval has been set to a valid value by the user.
        // The default value is -1 to indicate that automatic flush is not called for
        if( this.flushOptions.getFlushInterval() > 0 ) {
            GPUdbLogger.debug("Timed flush turned on, flush interval set to <" + this.flushOptions.getFlushInterval() + ">");
            this.timedFlushExecutorService = Executors.newSingleThreadScheduledExecutor();
            this.timedFlushExecutorService.scheduleWithFixedDelay(new TimedFlushTask(this),
                    this.flushOptions.getFlushInterval(),
                    this.flushOptions.getFlushInterval(),
                    TimeUnit.SECONDS);
            this.timedFlushExecutorServiceTerminated = false;
        } else {
            GPUdbLogger.debug("Timed flush turned off, flush interval set to negative value");
            this.timedFlushExecutorService = null;
            this.timedFlushExecutorServiceTerminated = true;
        }
    }



    /**
     * An exception that occurred during the insertion of records into GPUdb.
     */
    public static final class InsertException extends GPUdbException {
        private static final long serialVersionUID = 1L;

        private final URL url;
        private final transient List<?> records;

        InsertException(URL url, List<?> records, String message, Throwable cause) {
            super(message, cause);
            this.url = url;
            this.records = records;
        }

        InsertException(URL url, List<?> records, String message) {
            super(message);
            this.url = url;
            this.records = records;
        }

        public InsertException(String message) {
            super(message);
            this.url = null;
            this.records = null;
        }

        /**
         * Gets the URL that records were being inserted into when the exception
         * occurred, or {@code null} if multiple failover URLs all failed.
         *
         * @return  the URL
         */
        public URL getURL() {
            return this.url;
        }

        /**
         * Gets the list of records that was being inserted when the exception
         * occurred.
         *
         * @return  the list of records
         */
        public List<?> getRecords() {
            return this.records;
        }
    }

    /**
     * @deprecated This class has been superseded by {@link
     * com.gpudb.WorkerList com.gpudb.WorkerList}.
     */
    @Deprecated(since = "6.2.0", forRemoval = true)
    public static final class WorkerList extends com.gpudb.WorkerList {
        private static final long serialVersionUID = 1L;

        /**
         * @deprecated This class has been superseded by {@link
         * com.gpudb.WorkerList com.gpudb.WorkerList}.
         */
        @Deprecated(since = "6.2.0", forRemoval = true)
        public WorkerList() {
            super();
        }

        /**
         * @deprecated This class has been superseded by {@link
         * com.gpudb.WorkerList com.gpudb.WorkerList}.
         */
        @Deprecated(since = "6.2.0", forRemoval = true)
        public WorkerList(GPUdb gpudb) throws GPUdbException {
            super(gpudb);
        }

        /**
         * @deprecated This class has been superseded by {@link
         * com.gpudb.WorkerList com.gpudb.WorkerList}.
         */
        @Deprecated(since = "6.2.0", forRemoval = true)
        public WorkerList(GPUdb gpudb, Pattern ipRegex) throws GPUdbException {
            super(gpudb, ipRegex);
        }

        /**
         * @deprecated This class has been superseded by {@link
         * com.gpudb.WorkerList com.gpudb.WorkerList}.
         */
        @Deprecated(since = "6.2.0", forRemoval = true)
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
            return this.flushWhenFull;
        }

        /**
         * Sets the flag to set whether to flush when queues are full or not
         * @param flushWhenFull - boolean value to indicate whether to flush only full queues
         */
        public void setFlushWhenFull(boolean flushWhenFull) {
            this.flushWhenFull = flushWhenFull;
        }

        public int getFlushInterval() {
            return this.flushInterval;
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
            // Do nothing if this BulkInserter is closed
            if ( this.thisInserter.closed.get() )
                return;

            final LocalDateTime startTime = LocalDateTime.now();
            GPUdbLogger.debug(String.format("Timed flush started at : %s", startTime));
            try {
                if( this.thisInserter.getTimedFlushOptions().isFlushWhenFull())
                    this.thisInserter.flushFullQueues();
                else
                    this.thisInserter.flushAll();
                final LocalDateTime endTime = LocalDateTime.now();
                GPUdbLogger.debug(String.format("Timed flush completed in : %d seconds", Duration.between(startTime, endTime).getSeconds()));
            } catch (InsertException e) {
                GPUdbLogger.error(e.getMessage());
            }
        }
    }

    /**
     * The records queued for one rank, and the means of sending a batch of
     * them to it; a batch cut from the queue travels as a {@link BatchSend}.
     *
     * Those constructed with JSON options will be dedicated to ingesting JSON
     * records.
     */
    private static final class WorkerQueue<T> {
        private final GPUdb gpudb;
        
        /** Rank URL including insert records endpoint call path */
        private final URL insertUrl;

        private final String tableName;
        private final boolean isJson;

        // This is the same as the batchSize in BulkInserter class
        private final int capacity;
        List<T> queue;
        private final Object queueLock = new Object();
        private boolean retired = false; // guarded by queueLock
        private final TypeObjectMap<T> typeObjectMap;
        private final Map<String, String> options;

        private final GPUdbBase.JsonOptions jsonOptions;

        /**
         * Creates a queue for records to be inserted into the queue's
         * associated rank.  For head-node inserts, this queue will target
         * rank0; and for multi-head inserts, it will target some worker rank
         * between 1 and N.
         * 
         * @param gpudb  database connection to the target cluster
         * @param insertUrl  rank URL path to the endpoint for inserting records
         * @param tableName  name of the insert target table
         * @param capacity  number of records to queue before automatically
         *        ingesting the queued records and clearing the queue
         * @param options  optional parameters to pass to GPUdb while inserting
         *        <br/>
         *        This is the same set of options as accepted by the
         *        {@link GPUdb#insertRecords(String, List, Map)} call.
         *        <br/>
         *        The details can be found at
         *        {@link com.gpudb.protocol.InsertRecordsRequest.Options}.
         * @param jsonOptions  {@link GPUdbBase.JsonOptions} to use for JSON
         *        ingest only
         *        <br/>
         *        Note that a null here will trigger the non-JSON ingest path,
         *        and a non-null here will trigger the JSON ingest path.
         * @param typeObjectMap
         */
        public WorkerQueue(
                GPUdb gpudb,
                URL insertUrl,
                String tableName,
                int capacity,
                Map<String, String> options,
                GPUdbBase.JsonOptions jsonOptions,
                TypeObjectMap<T> typeObjectMap
        ) {
            this.gpudb         = gpudb;
            this.insertUrl     = insertUrl;
            this.tableName     = tableName;
            this.capacity      = capacity;
            this.options       = options;
            this.jsonOptions   = jsonOptions;
            this.typeObjectMap = typeObjectMap;
            this.isJson        = (this.jsonOptions != null);

            // Allow some extra room when allocating the memory since we may
            // sometimes go over the capacity before we flush
            this.queue = new ArrayList<>( (int)Math.round( capacity * 1.25 ) );
        }

        /*
         * Returns the currently queued records and re-initializes the queue
         * for new records.
         */
        public List<T> flush() {
            synchronized (this.queueLock) {
                List<T> oldQueue = this.queue;
                this.queue = new ArrayList<>(this.capacity);
                return oldQueue;
            }
        }

        /**
         * Queues the given record unless this queue has been retired by a
         * rebuild of the routing state, or the inserter has been closed.  Both
         * checks are made under the queue's lock, the same lock the retiring
         * rebuild and the closing drain take to empty it, so a record is
         * either queued before the queue was emptied for the last time, and
         * sent by whoever emptied it, or refused.
         * <p>
         * Where asked to, a record that fills the queue also cuts the full
         * batch, in the same critical section, as {@link #cut} would; so
         * queuing a record and learning that it completed a batch costs one
         * acquisition of the queue's lock, not two.
         *
         * @param record  the record to queue
         * @param closed  whether the inserter has been closed, or {@code null}
         *        for a record accepted before any close, which is queued
         *        regardless
         * @param cutIfFull  whether to cut the batch the record fills, which
         *        the caller then sends; a caller queuing many records cuts
         *        once at the end instead
         * @param maxRetries  how many times the batch may be re-sent, should
         *        one be cut
         * @param repeatable  whether a second copy of the batch is harmless
         * @return  {@code null} where the record was queued and no batch cut;
         *          the send of the batch it filled; or a refusal,
         *          {@link BatchSend#REFUSED_CLOSED} or
         *          {@link BatchSend#REFUSED_RETIRED}, compared by identity
         */
        @SuppressWarnings("unchecked")
        public BatchSend<T> offer(T record, AtomicBoolean closed, boolean cutIfFull, int maxRetries, boolean repeatable) {
            synchronized (this.queueLock) {
                if (this.retired)
                    return (BatchSend<T>) BatchSend.REFUSED_RETIRED;
                if (closed != null && closed.get())
                    return (BatchSend<T>) BatchSend.REFUSED_CLOSED;
                this.queue.add(record);
                if (cutIfFull && this.queue.size() >= this.capacity)
                    return cutLocked(maxRetries, repeatable);
                return null;
            }
        }

        /**
         * Cuts the queued records into a batch to send:  all of them, or only
         * a full batch's worth.
         *
         * @param onlyIfFull  whether to cut only when the queue is full
         * @param maxRetries  how many times the batch may be re-sent
         * @param repeatable  whether a second copy of the batch is harmless
         * @return  the send of the batch, or {@code null} where nothing was cut
         */
        public BatchSend<T> cut(boolean onlyIfFull, int maxRetries, boolean repeatable) {
            synchronized (this.queueLock) {
                if (this.queue.isEmpty() || (onlyIfFull && this.queue.size() < this.capacity))
                    return null;
                return cutLocked(maxRetries, repeatable);
            }
        }

        /** Takes the queued records as a batch to send; the queue's lock must be held. */
        private BatchSend<T> cutLocked(int maxRetries, boolean repeatable) {
            List<T> batch = this.queue;
            this.queue = new ArrayList<>( this.capacity );
            return new BatchSend<>(this, batch, maxRetries, repeatable);
        }

        /**
         * Sends a batch cut from this queue, by whichever path its records
         * take.
         */
        WorkerQueueInsertionResult<T> send(List<T> batch, int maxRetries, boolean repeatable) {
            RequestRetries retries = new RequestRetries( maxRetries, repeatable );
            return this.isJson ? handleJsonRecords(batch, retries) : handleRecordObjects(batch, retries);
        }

        /**
         * Takes the queued records for the last time, on a rebuild of the
         * routing state that replaces this queue.  A record offered after
         * this is refused, and its thread routes it again, so none is left
         * behind in a queue nothing flushes.
         *
         * @return  the records queued so far
         */
        public List<T> retire() {
            synchronized (this.queueLock) {
                this.retired = true;
                List<T> oldQueue = this.queue;
                this.queue = new ArrayList<>();
                return oldQueue;
            }
        }

        /**
         * Handle the insertion of the list of {@link RecordObject} derivatives
         * like {@link GenericRecord} or classes extending {@link RecordBase}.
         * 
         * @param queuedRecords  a batch of records to insert into the database
         *
         * @return - An instance of {@link WorkerQueueInsertionResult<T>}
         */
        private WorkerQueueInsertionResult<T> handleRecordObjects(List<T> queuedRecords, RequestRetries retries) {

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
                                                       this.options);

                recordsFailedEncoding = new LinkedHashMap<>( encodedRecords.getRight() );

            } catch (GPUdbException ex) {
                // Can't even attempt the insertion! Need to let the caller know.
                return new WorkerQueueInsertionResult<>(
                        this.insertUrl,
                        this.gpudb.getURL(),
                        null,
                        queuedRecords,
                        errors,
                        warnings,
                        Collections.emptyList(),
                        false,
                        false,
                        false,
                        this.gpudb.getNumClusterSwitches(),
                        0,
                        ex
                );
            }

            // Some flags for necessary follow-up work
            boolean doUpdateWorkers = false;
            boolean doFailover      = false;
            boolean isSuccess       = false;

            // Information that we will need to pass via the result object
            Exception exception   = null;
            List<T> failedRecords = null;
            List<InsertException> possiblyStored = Collections.emptyList();

            InsertRecordsResponse response = new InsertRecordsResponse();

            long insertionAttemptTimestamp = new Timestamp( System.currentTimeMillis() ).getTime();
            URL headRankURL = this.gpudb.getURL();
            int countClusterSwitches = this.gpudb.getNumClusterSwitches();

            try {
                final LocalDateTime startTime = LocalDateTime.now();

                GPUdbLogger.debug_with_info( "Inserting " + queuedRecords.size()
                                             + " records to rank at "
                                             + this.insertUrl.toString() );

                // // Note: The following debug is for developer debugging **ONLY**.
                // //       NEVER have this checked in uncommented since it will
                // //       slow down everything by printing the whole queue!
                // GPUdbLogger.debug_with_info( "Inserting records: "
                //                              + Arrays.toString( queue.toArray() ) );

                // Insert into the given worker rank
                response = this.gpudb.submitRequestRaw(this.insertUrl, request, response, true, this.gpudb.getTimeout(), retries);

                final LocalDateTime endTime = LocalDateTime.now();

                GPUdbLogger.debug_with_info(String.format("Insertion to rank %s completed in %d seconds",
                    this.insertUrl.toString(), Duration.between(startTime, endTime).getSeconds()
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

                possiblyStored = BulkInserter.possiblyStored( this.gpudb.getURL(), retries.getUncertainFailure(), queuedRecords, errors, warnings );

                // Check if shard re-balancing is under way at the server; if so,
                // we need to update the shard mapping
                if ( "true".equals( response.getInfo().get( GPUdbBase.RESPONSE_INFO_DATA_REROUTED ) ) ) {
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
            return new WorkerQueueInsertionResult<>(
                    this.insertUrl,
                    headRankURL,
                    response,
                    null,
                    failedRecords,
                    errors,
                    warnings,
                    possiblyStored,
                    isSuccess,
                    doUpdateWorkers,
                    doFailover,
                    countClusterSwitches,
                    insertionAttemptTimestamp,
                    exception
            );
        }

        /**
         * This method handles the insert of JSON records by using the method
         * {@link GPUdbBase#insertRecordsFromJson(String, String, GPUdbBase.JsonOptions, Map, Map)}
         *
         * @param queuedRecords  a batch of records to insert into the database
         *
         * @return - an instance of {@link WorkerQueueInsertionResult}
         */
        private WorkerQueueInsertionResult<T> handleJsonRecords(List<T> queuedRecords, RequestRetries retries) {

            // Some flags for necessary follow-up work
            boolean doUpdateWorkers = false;
            boolean doFailover      = false;
            boolean isSuccess       = false;

            // Information that we will need to pass via the result object
            Exception exception   = null;
            List<T> failedRecords = null;

            URL headRankURL = this.gpudb.getURL();
            long insertionAttemptTimestamp = new Timestamp( System.currentTimeMillis() ).getTime();

            Map<String, Object> insertRecordsFromJsonResponse = null;
            List<InsertException> possiblyStored = Collections.emptyList();

            @SuppressWarnings("unchecked")
            String jsonPayload = JsonUtils.toJsonArray((List<String>) queuedRecords);

            if( jsonPayload != null ) {
                try {
                    insertRecordsFromJsonResponse = this.gpudb.insertRecordsFromJson(
                            this.insertUrl,
                            jsonPayload,
                            this.tableName,
                            this.jsonOptions,
                            this.options,
                            retries
                    );

                    if (!(isSuccess = "OK".equalsIgnoreCase(insertRecordsFromJsonResponse.get("status").toString()))) {
                        failedRecords = queuedRecords;
                        exception = new InsertException(String.format(
                                "Error inserting <%d> JSON objects: %s",
                                failedRecords.size(),
                                insertRecordsFromJsonResponse.get("message")
                        ));
                    } else {
                        possiblyStored = BulkInserter.possiblyStored( this.gpudb.getURL(), retries.getUncertainFailure(),
                                queuedRecords, null, jsonReplyWarnings( insertRecordsFromJsonResponse ) );
                    }
                } catch (GPUdbException ex) {
                    // If some connection issue occurred, we want to force an HA failover
                    if ( (ex instanceof GPUdbExitException) || ex.hadConnectionFailure() ) {
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
            } else {
                failedRecords = queuedRecords;
                exception     = new InsertException(String.format(
                        "Error inserting <%d> JSON objects: they could not be combined into a JSON array",
                        failedRecords.size()));
            }

            return new WorkerQueueInsertionResult<>(
                    this.insertUrl,
                    headRankURL,
                    insertRecordsFromJsonResponse,
                    failedRecords,
                    null,
                    null,
                    possiblyStored,
                    isSuccess,
                    doUpdateWorkers,
                    doFailover,
                    0,
                    insertionAttemptTimestamp,
                    exception
            );
        }

        public URL getUrl() {
            return this.insertUrl;
        }

    }   // end class WorkerQueue


    /**
     * A batch of records cut from a queue, with how it is sent:  the one unit
     * handed to the worker pool, or sent on the calling thread.
     */
    private static final class BatchSend<T> implements Callable< WorkerQueueInsertionResult<T> > {

        /** A queue refused the record because the inserter is closed; see {@link WorkerQueue#offer}. */
        static final BatchSend<Object> REFUSED_CLOSED = new BatchSend<>( null, null, 0, false );
        /** A queue refused the record because a rebuild retired the queue; see {@link WorkerQueue#offer}. */
        static final BatchSend<Object> REFUSED_RETIRED = new BatchSend<>( null, null, 0, false );

        final WorkerQueue<T> queue;
        final List<T> records;
        final int maxRetries;
        final boolean repeatable;

        BatchSend(WorkerQueue<T> queue, List<T> records, int maxRetries, boolean repeatable) {
            this.queue      = queue;
            this.records    = records;
            this.maxRetries = maxRetries;
            this.repeatable = repeatable;
        }

        @Override
        public WorkerQueueInsertionResult<T> call() {
            return this.queue.send( this.records, this.maxRetries, this.repeatable );
        }
    }


    /**
     * An immutable snapshot of where records get routed.
     * <p>
     * Constructing one validates the relationship between its fields, so an
     * inconsistent routing state cannot be built, let alone published.  See
     * {@link BulkInserter#routing}.
     */
    private static final class Routing<T> {

        /** Whether records go to the head node rather than to worker ranks. */
        final boolean useHeadNode;

        /** Whether multi-head I/O is available for this inserter. */
        final boolean multiHeadEnabled;

        /**
         * The queue per destination; unmodifiable.  When {@link #useHeadNode}
         * is set this holds exactly one queue, for the head node.  Otherwise
         * entry {@code i} is worker rank {@code i + 1}, and a rank that has
         * been removed from the cluster keeps its slot as {@code null} so that
         * the indices stay aligned with {@link #routingTable}.
         */
        final List< WorkerQueue<T> > queues;

        /**
         * The shard-to-rank mapping; unmodifiable, and {@code null} when it is
         * not applicable (head-node routing) or not yet known.
         */
        final List<Integer> routingTable;

        /** Whether the table is replicated, kept so the rule can be re-applied. */
        final boolean tableReplicated;

        /**
         * The one statement of when routing goes to the head node: multi-head is
         * unusable, or the table is replicated.
         *
         * <p><b>The replicated case is a cost choice, not a safety requirement.</b>
         * A direct-to-rank insert on a replicated table is processed correctly,
         * but by being passed to the head rank for redistribution to the
         * workers.  Sending replicated table inserts directly to the head node
         * saves the extra redirection hit on the workers.
         */
        static boolean useHeadNodeFor( boolean multiHeadEnabled, boolean tableReplicated ) {
            return !multiHeadEnabled || tableReplicated;
        }

        Routing( boolean multiHeadEnabled,
                 boolean tableReplicated,
                 List< WorkerQueue<T> > queues,
                 List<Integer> routingTable ) {

            boolean doUseHeadNode = useHeadNodeFor( multiHeadEnabled, tableReplicated );

            if ( queues == null || queues.isEmpty() )
                throw new IllegalArgumentException( "Routing needs at least one queue" );

            // The insert path resolves head-node routing to queues.get( 0 ),
            // so this has to be the only queue there is.
            if ( doUseHeadNode && (queues.size() != 1) )
                throw new IllegalArgumentException(
                        "Head-node routing needs exactly one queue, got "
                        + queues.size() );

            if ( doUseHeadNode && (queues.get( 0 ) == null) )
                throw new IllegalArgumentException( "The head-node queue cannot be null" );

            this.useHeadNode      = doUseHeadNode;
            this.multiHeadEnabled = multiHeadEnabled;
            this.tableReplicated  = tableReplicated;
            this.queues           = Collections.unmodifiableList( new ArrayList<>( queues ) );
            this.routingTable     = (routingTable == null)
                                    ? null
                                    : Collections.unmodifiableList( new ArrayList<>( routingTable ) );
        }

        /**
         * Returns a copy of this routing state carrying a different shard
         * mapping.  The queues are shared, not rebuilt.
         */
        Routing<T> withRoutingTable( List<Integer> newRoutingTable ) {
            return new Routing<>( this.multiHeadEnabled, this.tableReplicated,
                                  this.queues, newRoutingTable );
        }

        @Override
        public String toString() {
            return "Routing{useHeadNode=" + this.useHeadNode
                   + ", multiHeadEnabled=" + this.multiHeadEnabled
                   + ", queues=" + this.queues.size()
                   + ", routingTable=" + ((this.routingTable == null)
                                          ? "null"
                                          : (this.routingTable.size() + " shards"))
                   + "}";
        }
    }   // end class Routing



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
        private final Map<String, Object> insertJsonResponse;
        private final List<T>   failedRecords;
        private final Exception failureException;
        private final boolean   didSucceed;
        private final boolean   doUpdateWorkers;
        private final boolean   doFailover;
        private final int       countClusterSwitches;
        private final long      insertionAttemptTimestamp;
        private final Map<Integer, Pair<String, T>> error_map;
        private final List<String> warnings;
        private final List<InsertException> possiblyStored;

        public WorkerQueueInsertionResult(URL workerUrl,
                                          URL headUrl,
                                          InsertRecordsResponse insertResponse,
                                          Map<String, Object> insertJsonResponse,
                                          List<T> failedRecords,
                                          Map<Integer, Pair<String, T>> error_map,
                                          List<String> warnings,
                                          List<InsertException> possiblyStored,
                                          boolean didSucceed,
                                          boolean doUpdateWorkers,
                                          boolean doFailover,
                                          int countClusterSwitches,
                                          long insertionAttemptTimestamp,
                                          Exception exception) {
            this.workerUrl        = workerUrl;
            this.headUrl          = headUrl;
            this.insertResponse   = insertResponse;
            this.insertJsonResponse = null;
            this.failedRecords    = failedRecords;
            this.error_map        = error_map;
            this.warnings         = warnings;
            this.possiblyStored   = possiblyStored;
            this.didSucceed       = didSucceed;
            this.doUpdateWorkers  = doUpdateWorkers;
            this.doFailover       = doFailover;
            this.failureException = exception;
            this.countClusterSwitches      = countClusterSwitches;
            this.insertionAttemptTimestamp = insertionAttemptTimestamp;
        }

        public WorkerQueueInsertionResult(URL workerUrl,
                                          URL headUrl,
                                          Map<String, Object> insertResponse,
                                          List<T> failedRecords,
                                          Map<Integer, Pair<String, T>> error_map,
                                          List<String> warnings,
                                          List<InsertException> possiblyStored,
                                          boolean didSucceed,
                                          boolean doUpdateWorkers,
                                          boolean doFailover,
                                          int countClusterSwitches,
                                          long insertionAttemptTimestamp,
                                          Exception exception) {
            this.workerUrl        = workerUrl;
            this.headUrl          = headUrl;
            this.insertResponse = null;
            this.insertJsonResponse   = insertResponse;
            this.failedRecords    = failedRecords;
            this.error_map        = error_map;
            this.warnings         = warnings;
            this.possiblyStored   = possiblyStored;
            this.didSucceed       = didSucceed;
            this.doUpdateWorkers  = doUpdateWorkers;
            this.doFailover       = doFailover;
            this.failureException = exception;
            this.countClusterSwitches      = countClusterSwitches;
            this.insertionAttemptTimestamp = insertionAttemptTimestamp;
        }

        public URL getHeadUrl() {
            return this.headUrl;
        }

        public URL getWorkerUrl() {
            return this.workerUrl;
        }

        public InsertRecordsResponse getInsertResponse() {
            return this.insertResponse;
        }

        /**
         * @return  the records a re-sent batch met as primary key collisions,
         *          which an earlier copy of the batch may have stored
         */
        public List<InsertException> getPossiblyStored() {
            return this.possiblyStored;
        }

        public Map<String, Object> getInsertJsonResponse() {
            return this.insertJsonResponse;
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


    /**
     * Retrieves options governing the timed flush ingest scheme.
     * 
     * @return the timed flush options as a {@link FlushOptions} instance
     */
    public FlushOptions getTimedFlushOptions() {
        return this.flushOptions;
    }

    /**
     * Retrieves options governing JSON ingest.
     * 
     * @return the JSON ingest options as a {@link GPUdbBase.JsonOptions} instance
     */
    public GPUdbBase.JsonOptions getJsonOptions() {
        return this.jsonOptions;
    }

    /**
     * This method could potentially result in two different scenarios
     * <ol>
     *   <li>
     *     It could start a timed flush thread if it was not already active
     *     when the BulkInserter was created.
     *   </li>
     *   <li>
     *     If the timed flush thread was already active then setting this to a
     *     new value will first terminate the existing thread; set the options
     *     to the new value and finally restart a new thread with the options
     *     passed in. This case could result in a delay since the thread needs
     *     to be cleaned up and restarted.
     *   </li>
     * </ol>
     *
     * @param flushOptions {@link FlushOptions} to use for timed flush operation
     * @throws GPUdbException in case an invalid timeout values is set in the
     *                          {@code flushOptions} parameter.
     * @throws IllegalStateException if this inserter has been closed
     */
    public void setTimedFlushOptions(FlushOptions flushOptions) throws GPUdbException {
        synchronized ( this.closeMutex ) {
            checkOpen();
            applyTimedFlushOptions( flushOptions );
        }
    }

    private void applyTimedFlushOptions(FlushOptions flushOptions) {
        this.flushOptions = (flushOptions == null) ? FlushOptions.defaultOptions() : flushOptions;

        // Timed flush already active, terminate first
        if( this.timedFlushExecutorService != null && !this.timedFlushExecutorServiceTerminated) {
            terminateTimedFlushExecutor();
            GPUdbLogger.debug_with_info("Timed flush executor service terminated ...");
        }

        // If valid flush interval has been passed in, re-create the timedFlush
        // service thread
        if( this.flushOptions.getFlushInterval() > 0) {

            // Restart with new FlushOptions value
            this.timedFlushExecutorService = Executors.newSingleThreadScheduledExecutor();
            this.timedFlushExecutorService.scheduleWithFixedDelay(new TimedFlushTask(this),
                this.flushOptions.getFlushInterval(),
                this.flushOptions.getFlushInterval(),
                TimeUnit.SECONDS);

            //reset the state to false after restart
            this.timedFlushExecutorServiceTerminated = this.timedFlushExecutorService.isTerminated();
            GPUdbLogger.debug("Timed flush restarted, flush interval set to <" + this.flushOptions.getFlushInterval() + ">");
        } else {
            GPUdbLogger.debug("Timed flush turned off, flush interval set to negative value");
        }

    }

    /**
     * This method could be used to set {@link GPUdbBase.JsonOptions} in case the user wants to
     * modify the defaults.
     *
     * @param jsonOptions {@link GPUdbBase.JsonOptions} to use for JSON ingest
     *
     * @see GPUdbBase.JsonOptions
     */
    public void setJsonOptions(GPUdbBase.JsonOptions jsonOptions) {
        this.jsonOptions = jsonOptions;
    }

    /**
     * Closes {@link BulkInserter} resources:
     * <ol>
     *   <li>Performs a final flush of any data yet to be ingested</li>
     *   <li>Terminates the timed flush mechanism, if needed</li>
     *   <li>Terminates the ingest executor service</li>
     * </ol>
     * <br/>
     * 
     * This method will be called automatically if the {@link BulkInserter} class is used in a
     * try-with-resources block. If not used that way it is mandatory to call this method
     * to initiate a smooth cleanup of the underlying resources.
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
     *     BulkInserter inserter = new BulkInserter&lt;&gt;(...)
     *     // Invoke some methods on the inserter
     *     //Explicitly call close() method
     *     inserter.close();
     * </pre>
     *
     * Once closed, the inserter accepts no more records:
     * {@link #insert(Object)}, {@link #insert(List)}, {@link #flush()} and
     * {@link #setTimedFlushOptions(FlushOptions)} throw
     * {@link IllegalStateException}.  Every record accepted before the close
     * is sent:  by this method, or, for a batch an insert on another thread
     * was already sending, by that insert.  Closing again does nothing.
     *
     * @throws InsertException - While doing the final flush
     */
    @Override
    public void close() throws InsertException {
        synchronized ( this.closeMutex ) {
            // Refuse records from here on; the queues check this flag under
            // their own locks, so the drain below takes every record queued
            // before it
            if ( !this.closed.compareAndSet( false, true ) )
                return;

            // Stop the timed flushes; one in progress finishes first
            terminateTimedFlushExecutor();

            // Call the flush method one last time
            InsertException ie = null;
            try {
                flushAll();
            } catch (InsertException e) {
                GPUdbLogger.error(e.getMessage());
                ie = e;
            }

            GPUdbLogger.debug_with_info("Terminating BulkInserter and cleaning up ...");

            // Terminate the worker thread pool.  A batch an insert on another
            // thread hands over after this is sent by that thread; see
            // flushQueues
            terminateWorkerThreadPool();

            if( ie != null ) {
                throw ie;
            }
        }
    }


    /**
     * @throws IllegalStateException if this inserter has been closed
     */
    private void checkOpen() {
        if ( this.closed.get() )
            throw closedException();
    }


    private IllegalStateException closedException() {
        return new IllegalStateException( "BulkInserter for table <" + this.tableName + "> is closed" );
    }

    /**
     * Checks whether the user has the requisite permissions on the table to
     * use {@link BulkInserter}. This method us used in the constructor and if
     * the permissions are not found to be adequate then the constructor throws
     * an exception and bails out.
     *
     * @param gpudb          the GPUdb instance to check access to the target table
     * @param tableName      the name of the table for which the BulkInserter is being created
     * @param insertOptions  the options to be passed to the
     *                       {@link GPUdb#insertRecords(String, List, Map)} call
     * @return {@code true} if the requisite permissions exist; {@code false} otherwise
     * @throws GPUdbException if the {@link GPUdb#hasPermission(String, String, String, String, Map)} call fails
     */
    private static boolean hasPermissionsForTable(final GPUdb gpudb, final String tableName, Map<String, String> insertOptions) throws GPUdbException {
        boolean insertPermissionOk = gpudb.hasPermission("", tableName, HasPermissionRequest.ObjectType.TABLE, HasPermissionRequest.Permission.INSERT, new HashMap<>()).getHasPermission();
        boolean updatePermissionOk = true;

        // If the user requests upserts, check update permission on the table
        //   Note: either options or the map value could be null
        if (insertOptions != null) {
            String updateOnExistingPk = insertOptions.get(InsertRecordsRequest.Options.UPDATE_ON_EXISTING_PK);
            if (InsertRecordsRequest.Options.TRUE.equalsIgnoreCase(updateOnExistingPk))
                updatePermissionOk = gpudb.hasPermission("", tableName, HasPermissionRequest.ObjectType.TABLE, HasPermissionRequest.Permission.UPDATE, new HashMap<>()).getHasPermission();
        }

        return (insertPermissionOk && updatePermissionOk);
    }

    private void terminateTimedFlushExecutor() {
        if( this.timedFlushExecutorService != null && !this.timedFlushExecutorService.isShutdown()) {
            this.timedFlushExecutorService.shutdown();
            try {
                if (!this.timedFlushExecutorService.awaitTermination( DEFAULT_THREADPOOL_TERMINATION_TIMEOUT, TimeUnit.SECONDS)) {
                    this.timedFlushExecutorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                this.timedFlushExecutorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            GPUdbLogger.debug_with_info("Terminated scheduler thread ...");
            this.timedFlushExecutorServiceTerminated = this.timedFlushExecutorService.isTerminated();
            this.timedFlushExecutorService = null;
        }
    }

    private void terminateWorkerThreadPool() {
        if( this.workerExecutorService != null && !this.workerExecutorService.isShutdown()) {
            this.workerExecutorService.shutdown();
            try {
                if (!this.workerExecutorService.awaitTermination( DEFAULT_THREADPOOL_TERMINATION_TIMEOUT, TimeUnit.SECONDS)) {
                    this.workerExecutorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                this.workerExecutorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            GPUdbLogger.debug_with_info("Terminated worker thread pool ...");
        }
    }


    /**
     * Use the current head node URL in a thread-safe manner, guarded by the HA
     * failover lock.
     */
    private URL getCurrentHeadNodeURL() {
        synchronized ( this.haFailoverLock ) {
            return this.currentHeadNodeURL;
        }
    }

    /**
     * Sets the current head node URL in a thread-safe manner, guarded by the HA
     * failover lock.
     */
    private void setCurrentHeadNodeURL(URL newCurrURL) {
        synchronized ( this.haFailoverLock ) {
            this.currentHeadNodeURL = newCurrURL;
        }
    }

    /**
     * Asks the connection to fail over to another cluster, and records
     * where it ended up.
     *
     * <p>The selection is entirely the connection's: {@code switchURL} walks the
     * HA ring, and returns the first one it has found usable.  This method
     * contributes the caller's vantage point -- the URL it was using and the
     * switch count it last saw -- which is what lets the connection tell a
     * first failover from a thread piggybacking on one already in progress.
     *
     * @param oldURL  the URL this object was using when the failure occurred
     * @param oldClusterSwitchCount  the connection's switch count as this object
     *                               last saw it, before the failing request
     *
     * @throws GPUdbException if a successful failover could not be achieved.
     */
    private synchronized void forceFailover(URL oldURL, int oldClusterSwitchCount) throws GPUdbException {
        this.gpudb.switchURL( oldURL, oldClusterSwitchCount );

        // Record where the connection ended up
        this.setCurrentHeadNodeURL( this.gpudb.getURL() );
    }   // end forceFailover


    /**
     * Updates the shard mapping based on the latest cluster configuration.
     * Also reconstructs the worker queues based on the new sharding.
     *
     * @return  a boolean indicating whether the shard mapping was updated.
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
     * @return  a boolean indicating whether the shard mapping was updated.
     */
    private synchronized boolean updateWorkerQueues( boolean doReconstructWorkerQueues ) throws GPUdbException {
        return updateWorkerQueues( doReconstructWorkerQueues, true );
    }


    /**
     * Updates the worker queues and the shard mapping based on the latest
     * cluster configuration.   Optionally, also reconstructs the worker
     * queues based on the new sharding.
     *
     * @param doReconstructWorkerQueues  Boolean flag indicating if the worker
     *                                   queues ought to be re-built.
     * @param publishShardMapping  Whether a newly fetched shard mapping should
     *                             be published on its own.  A caller that is
     *                             about to publish a complete routing state of
     *                             its own -- {@link #reconstructWorkerQueues()}
     *                             -- passes {@code false} and picks the mapping
     *                             up from {@link #pendingRoutingTable},
     *                             so that a new mapping is never published
     *                             alongside the queues it does not describe.
     *
     * @return  a boolean indicating whether the shard mapping was updated.
     */
    private synchronized boolean updateWorkerQueues( boolean doReconstructWorkerQueues,
                                                     boolean publishShardMapping ) throws GPUdbException {

        // Decide if the worker queues will need to be reconstructed (they will
        // only if multi-head is enabled, it is not a replicated table, and if
        // the user wants to)
        boolean reconstructWorkerQueues = ( doReconstructWorkerQueues
                                            && !this.routing.useHeadNode );

        // Whether the shard mapping has changed since the last snapshot.
        boolean shardMappingChanged = false;

        try {
            // Get the latest shard mapping information; note that this endpoint
            // call might trigger an HA failover in the GPUdb object
            AdminShowShardsResponse shardInfo = GPUdbBase.ownRead( () -> this.gpudb.adminShowShards(new AdminShowShardsRequest()) );

            // Get the shard version
            long newShardVersion = shardInfo.getVersion();

            shardMappingChanged = (this.shardVersion != newShardVersion);

            // No-op if the shard version hasn't changed (and it's not the first time)
            if (this.shardVersion == newShardVersion) {
                // Also check whether the connection moved to a different
                // cluster -- by fail-over or by fail-back -- since this object
                // last built its worker list.
                GPUdbBase.MultiHeadSnapshot currSnapshot =
                        this.gpudb.getCurrentMultiHeadSnapshot();
                if ( currSnapshot == this.lastMultiHeadSnapshot ) {
                    GPUdbLogger.debug_with_info( "Same cluster and shard version" );

                    if ( reconstructWorkerQueues )
                    {
                        // The caller needs to know if we ended up updating the
                        // queues
                        boolean didRecontructWorkerQueues = reconstructWorkerQueues( false );
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

                // Record the cluster now current, so the next call compares
                // against it rather than against the one left behind.
                this.lastMultiHeadSnapshot = currSnapshot;
            }

            // Save the new shard version
            this.shardVersion = newShardVersion;

            // Save when we're updating the mapping
            this.shardUpdateTime.setValue( new Timestamp( System.currentTimeMillis() ).getTime() );

            // Record the newly fetched shard mapping.  It is published here
            // only when the caller is not about to publish a routing state of
            // its own; see the publishShardMapping parameter.
            this.pendingRoutingTable = shardInfo.getRank();

            if ( publishShardMapping )
                this.routing = this.routing.withRoutingTable( this.pendingRoutingTable );
        } catch (GPUdbException ex) {
            // Couldn't get the current shard assignment info; see if this is due
            // to cluster failure
            if ( ex.hadConnectionFailure() ) {
                // Could not update the worker queues because we can't connect
                // to the database
                GPUdbLogger.debug_with_info( "Had connection failure: "
                                             + ex.getMessage() );
                return false;
            }
            
            // Unknown errors not handled here
            throw ex;
        }

        // If we get here, then we may have done a cluster failover during
        // /admin/show/shards; so update the current head node url
        this.setCurrentHeadNodeURL( this.gpudb.getURL() );

        // The worker queues need to be re-constructed when asked for
        // iff multi-head i/o is enabled and the table is not replicated
        if ( reconstructWorkerQueues )
        {
            reconstructWorkerQueues( shardMappingChanged );
        }

        GPUdbLogger.debug_with_info( "Returning true" );
        return true; // the shard mapping was updated indeed
    }  // end updateWorkerQueues


    /**
     * Reconstructs the worker queues and re-queues records in the old
     * queues.
     *
     * @param topologyMayHaveMoved  what this rebuild observed, not what it wants
     *                              done: {@code true} where the server reported
     *                              a shard mapping change, which can move rank
     *                              addresses without moving the connection, so
     *                              the connection re-acquires before answering;
     *                              {@code false} after a cluster change, which
     *                              the switch itself already probed
     *
     * @return  whether we ended up reconstructing the worker queues or not
     */
    private synchronized boolean reconstructWorkerQueues( boolean topologyMayHaveMoved )
            throws GPUdbException {

        // Using the worker ranks for multi-head ingestion; so need to rebuild
        // the worker queues
        // --------------------------------------------------------------------

        if (this.workerList.disablesMultiHead()) {
            GPUdbLogger.debug_with_info( "Worker list declines multi-head; not rebuilding" );
            return false;
        }


        // Ask the connection for the current cluster's addresses.
        GPUdbBase.MultiHeadSnapshot snapshot =
                this.gpudb.acquireMultiHeadSnapshot( topologyMayHaveMoved );

        // Adopt the addresses of whichever cluster the connection is on *now*.
        // This is how an inserter follows a failover: the connection switches,
        // and the next rebuild picks up the cluster it switched to.  Built from
        // the snapshot taken above rather than from a fresh read, so the
        // addresses and the reference recorded below cannot come from different
        // probes.
        com.gpudb.WorkerList newWorkerList = new com.gpudb.WorkerList( snapshot );

        GPUdbLogger.debug_with_info( "Current worker list: " + this.workerList.toString() );
        GPUdbLogger.debug_with_info( "New worker list:     " + newWorkerList.toString() );
        if ( newWorkerList.equals( this.workerList ) ) {
            GPUdbLogger.debug_with_info( "Worker list remained the same; returning false" );
            return false; // the worker list did not change
        }

        // Update the worker list
        this.workerList = newWorkerList;

        // Remember the exact answer these addresses came from -- not the
        // cluster.  The next updateWorkerQueues() compares by identity, so a
        // re-probe of the same cluster counts as a change; that is what makes
        // leaving a cluster and returning to it visible.
        this.lastMultiHeadSnapshot = snapshot;

        // Set if multi-head I/O is turned on at the server and rank URLs are accessible
        boolean isMultiHeadEnabled = ( (this.workerList != null) && !this.workerList.isEmpty() );

        // We should use the head node if multi-head is turned off at the server
        // or if we're working with a replicated table.  Routing states that rule;
        // it is needed here because it decides whether per-rank queues get built
        // at all, before any Routing exists to ask.
        boolean useHeadNode = Routing.useHeadNodeFor( isMultiHeadEnabled, this.isTableReplicated );

        // Everything below is built into locals and published in one go at the
        // end to avoid concurrency issues.
        List< WorkerQueue<T> > newWorkerQueues = new ArrayList<>();
        List<Integer>          newRoutingTable = null;

        try {
            if (!useHeadNode) {
                // Create worker queues per worker URL
                for ( URL url : this.workerList) {
                    // Handle removed ranks
                    if (url == null) {
                        newWorkerQueues.add( null );
                    }
                    else {
                        // Add a queue for a currently active rank
                        URL insertURL = GPUdbBase.appendPathToURL( url, "/insert/records" );
                        newWorkerQueues.add(new WorkerQueue<>(
                                this.gpudb,
                                insertURL,
                                this.tableName,
                                this.batchSize,
                                this.options,
                                (this.isJson ? this.jsonOptions : null),
                                this.typeObjectMap
                        ));
                    }
                }

                // Fetch the shard mapping that goes with the new queues, but
                // do not let it be published on its own -- it describes the new
                // set of ranks, and pairing it with the queues still in place
                // is the very mismatch this rebuild is avoiding.
                //
                // The fetch is a no-op when the shard version has not moved, in
                // which case it leaves pendingRoutingTable alone; clearing it
                // first is what distinguishes "nothing new was fetched" from a
                // mapping left over from an earlier fetch.  Nothing new means
                // the mapping currently in force still applies -- including
                // when that is null, which insert() reports rather than
                // guessing at.
                this.pendingRoutingTable = null;
                updateWorkerQueues( false, false );
                newRoutingTable = ( this.pendingRoutingTable != null )
                                  ? this.pendingRoutingTable
                                  : this.routing.routingTable;
            } else {
                URL insertURL = GPUdbBase.appendPathToURL( this.gpudb.getURL(), "/insert/records" );

                newWorkerQueues.add(new WorkerQueue<>(
                        this.gpudb,
                        insertURL,
                        this.tableName,
                        this.batchSize,
                        this.options,
                        (this.isJson ? this.jsonOptions : null),
                        this.typeObjectMap
                ));
            }
        } catch (MalformedURLException ex) {
            throw new GPUdbException(ex.getMessage(), ex);
        }

        // Publish the whole new routing state with one volatile write, so that
        // a concurrent insert() sees either all of the old state or all of the
        // new one.  Must happen before the re-queue loop below, which inserts
        // through this same routing state.
        List< WorkerQueue<T> > oldWorkerQueues = this.routing.queues;
        this.routing = new Routing<>( isMultiHeadEnabled, this.isTableReplicated,
                                      newWorkerQueues, newRoutingTable );

        // Re-queue any existing queued records.  They were accepted before
        // any close, so they are queued whether or not the inserter has been
        // closed since; and if it has, nothing else will flush the new queues
        // they land in, so flush them now.
        for ( WorkerQueue<T> oldQueue : oldWorkerQueues ) {
            if ( oldQueue != null ) { // skipping removed ranks
                List<T> records = oldQueue.retire();
                this.queueAndFlushFull( records, false );
            }
        }
        if ( this.closed.get() )
            flushAll();

        GPUdbLogger.debug_with_info( "Worker list was updated, returning true" );
        return true; // we did change the queues!
    }  // end reconstructWorkerQueues


    /**
     * Gets the GPUdb instance into which records will be inserted.
     *
     * @return  the GPUdb instance into which records will be inserted
     */
    public GPUdb getGPUdb() {
        return this.gpudb;
    }

    /**
     * Gets the name of the table into which records will be inserted.
     *
     * @return  the name of the table into which records will be inserted
     */
    public String getTableName() {
        return this.tableName;
    }

    /**
     * Gets the batch size (the number of records to insert into GPUdb at a
     * time). For multi-head ingest this value is per worker.
     *
     * @return  the batch size
     */
    public int getBatchSize() {
        return this.batchSize;
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
        return this.options;
    }

    public boolean isMultiHeadEnabled() {
        return this.routing.multiHeadEnabled;
    }

    /**
     * @return  whether this inserter's table has a primary key, so that its
     *          batches are re-sent where a second copy would otherwise be
     *          unsafe
     */
    boolean isTableWithPrimaryKey() {
        return this.tableHasPrimaryKey;
    }

    /**
     * Gets the number of times a batch is re-sent to the same cluster after a
     * failure that a second attempt may get past, such as a refused connection
     * or a busy server, before the inserter fails over to another cluster, or
     * throws {@link InsertException} where there is none.  A batch is given
     * this budget in place of the connection's
     * {@link GPUdbBase#getMaxRetries() maxRetries}, not in addition to it.
     * The default value is 7.
     *
     * @return  the number of retries
     *
     * @see #setMaxRetries(int)
     */
    public int getMaxRetries() {
        return this.maxRetries;
    }

    /**
     * Sets the number of times a batch is re-sent to the same cluster after a
     * failure that a second attempt may get past, such as a refused connection
     * or a busy server, before the inserter fails over to another cluster, or
     * throws {@link InsertException} where there is none.  A batch is given
     * this budget in place of the connection's
     * {@link GPUdbBase#getMaxRetries() maxRetries}, not in addition to it.
     * The default value is 7.
     *
     * @param value  the number of retries
     *
     * @throws IllegalArgumentException if {@code value} is less than zero
     *
     * @see #getMaxRetries()
     */
    public void setMaxRetries(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Max retries must not be negative.");
        }

        this.maxRetries = value;
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
    @Deprecated(since = "7.1.10", forRemoval = true)
    public int getRetryCount() {
        return this.maxRetries;
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
    @Deprecated(since = "7.1.10", forRemoval = true)
    public void setRetryCount(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Retry count must not be negative.");
        }

        this.maxRetries = value;
    }


    /**
     * Gets the number of records inserted into GPUdb. Excludes records that
     * are currently queued but not yet inserted and records not inserted due to
     * primary key conflicts.
     *
     * @return  the number of records inserted
     */
    public long getCountInserted() {
        return this.countInserted.get();
    }

    /**
     * Gets the number of records updated (instead of inserted) in GPUdb due to
     * primary key conflicts.
     *
     * @return  the number of records updated
     */
    public long getCountUpdated() {
        return this.countUpdated.get();
    }

    /**
     * Gets the list of errors received since the last call to getErrors().
     *
     * @return  list of InsertException objects
     */
    public List<InsertException> getErrors()
    {
        synchronized (this.errorListLock)
        {
            List<InsertException> copy = this.errorList; // Make a copy to return
            this.errorList = new ArrayList<>(); // Make a new list for the future
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
        synchronized (this.errorListLock)
        {
            List<InsertException> copy = this.warningList; // Make a copy to return
            this.warningList = new ArrayList<>(); // Make a new list for the future
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
     * @throws IllegalStateException if this inserter has been closed
     */
    public void flush() throws InsertException {
        checkOpen();
        flushAll();
    }


    /**
     * Flushes every queue, as {@link #flush()} does, without checking whether
     * this inserter is closed.
     */
    private void flushAll() throws InsertException {
        // Flush all queues, regardless of how full they are.  Note the last
        // parameter lets the called method know that the user is forcing this
        // flush; this is important for recursive calls.
        this.flushQueues( cutBatches( false ), true );
    }


    /**
     * Cuts a batch from every queue holding one:  all of each queue's records,
     * or only a full batch's worth.
     */
    private List<BatchSend<T>> cutBatches( boolean onlyIfFull ) {
        List<BatchSend<T>> batches = new ArrayList<>();
        for (WorkerQueue<T> workerQueue : this.routing.queues) {

            // Handle removed ranks
            if ( workerQueue == null)
                continue;

            BatchSend<T> batch = workerQueue.cut( onlyIfFull, this.maxRetries, this.tableHasPrimaryKey );
            if ( batch != null ) {
                GPUdbLogger.debug_with_info( "Cut a batch of " + batch.records.size() + " records for " + workerQueue.getUrl() );
                batches.add( batch );
            }
        }
        return batches;
    }


    /**
     * Flush only the queues that are already full.
     *
     * @throws InsertException if an error occurs while flushing
     */
    void flushFullQueues() throws InsertException {
        this.flushQueues( cutBatches( true ), false );
    }


    /**
     * Flush record batches queued for insert on each of the given workers.
     * There is an assumption that each of the WorkerQueues will have record
     * batches queued for insert.
     *
     * Each batch is sent with this inserter's own budget of re-sends to its
     * rank, spent by the connection's request layer, and marked repeatable
     * where the table has a primary key.  No batch is re-sent to the same
     * cluster here.  If any queue encounters a failover scenario,
     * trigger the failover mechanism to re-establish connection with the
     * server.  Then, re-insert all records that we failed to insert.
     * 
     * NOTE:  This method is also responsible for switching the connections and
     * queue configuration back to the primary cluster in the event of a
     * fail-back scenario.
     *
     * @param queues      the queues that we need to flush
     * @param forcedFlush boolean indicating if the user wants a forced flush
     *                    of all the records, so that records re-inserted
     *                    after a failover are flushed too
     *
     * @throws InsertException if an error occurs while flushing
     */
    private void flushQueues( List<BatchSend<T>> batches, boolean forcedFlush ) throws InsertException {

        GPUdbLogger.debug_with_info(String.format(
                "Begin flushQueues; <%d> batches", batches.size()
        ));

        if ( batches.isEmpty() ) {
            GPUdbLogger.debug_with_info( "Returning without further action" );
            return;
        }

        // Check if a fail-back has occurred, updating the cluster connections
        // and config, if so.  The rebuild takes this inserter's monitor, and a
        // fail-over, which runs under that monitor, records its new head URL
        // under haFailoverLock; so the URL is read under haFailoverLock and
        // the lock released before the rebuild is called, or the two could
        // each wait for the other's lock.
        if (getCurrentHeadNodeURL() != this.gpudb.getURL()) {
            try {
                updateWorkerQueues();
            }
            catch (GPUdbException e)
            {
                throw new InsertException(e.getMessage());
            }
        }

        // Create an execution completion service that will let each queue
        // work in an independent parallel thread.  We will consume the
        // result of the queues as they complete (in the order of completion).
        BlockingQueue<Future<WorkerQueueInsertionResult<T>>> completedQueues = new LinkedBlockingQueue<>();
        CompletionService<WorkerQueueInsertionResult<T>> queueService
            = new ExecutorCompletionService<>( this.workerExecutorService, completedQueues );

        // Which batch each pending result belongs to
        Map<Future<WorkerQueueInsertionResult<T>>, BatchSend<T>> inFlight = new IdentityHashMap<>();

        // Add all the given tasks to the service
        int countQueueSubmitted = 0;
        for (BatchSend<T> batch : batches) {

            // Nothing more is sent once a per-record error has stopped the
            // inserter; the batch is dropped
            if (this.simulateErrorMode)
                continue;

            // Submit each batch to the service.  A lone batch is sent on this
            // thread instead:  this flush waits for its result anyway, so a
            // worker thread would add a hand-off and, with the pool busy, a
            // wait for one, and no concurrency.  The pool is for the flushes
            // that send several queues' batches at once.
            if (batches.size() == 1) {
                sendOnThisThread( batch, completedQueues, inFlight );
            } else {
                try {
                    inFlight.put( queueService.submit( batch ), batch );
                } catch (RejectedExecutionException poolShutDown) {
                    // close() shut the pool down after this flush began.  The
                    // batch was accepted before the close, so send it here,
                    // and hand its result in as the pool would have.
                    GPUdbLogger.debug_with_info( "Worker pool shut down; inserting the batch for " + batch.queue.getUrl() + " on the calling thread" );
                    sendOnThisThread( batch, completedQueues, inFlight );
                }
            }
            ++countQueueSubmitted;
        }
        GPUdbLogger.debug_with_info( "# batches submitted: " + countQueueSubmitted );

        boolean doUpdateWorkers  = false;
        boolean doFailover       = false;
        boolean doRetryInsertion = false;
        int     latestCountClusterSwitches       = 0;
        long    latestInsertionAttemptTimestamp  = 0;
        List<URL> failedWorkerUrls       = new ArrayList<>();
        List<T> failedRecords            = new ArrayList<>();
        List<Exception> workerExceptions = new ArrayList<>();

        // Handle the results of the parallel insertions.  Aggregate results,
        // or set up the need for failover.
        for (int i = 0; i < countQueueSubmitted; ++i) {
            // Note that this take() will block until the next batch has
            // completed its work
            Future<WorkerQueueInsertionResult<T>> done;
            try {
                done = queueService.take();
            } catch ( InterruptedException ex ) {
                GPUdbLogger.debug_with_info(String.format("Thread interrupted : %s", ex.getMessage()));
                continue;
            }
            WorkerQueueInsertionResult<T> result;
            try {
                result = done.get();
            } catch ( ExecutionException ex ) {
                // Some error occurred in the task being executed; need to report
                final URL url = inFlight.get(done).queue.getUrl();
                GPUdbLogger.debug_with_info(String.format("Error in inserting data for queue with URL : %s : Error message # %s", url, ex.getMessage()));
                workerExceptions.add(ex);
                continue;
            } catch ( InterruptedException ex ) {
                GPUdbLogger.debug_with_info(String.format("Thread interrupted : %s", ex.getMessage()));
                continue;
            }

            // Handle the result, if any
            InsertRecordsResponse insertResponse = result.getInsertResponse();
            Map<String, Object> insertJsonResponse = result.getInsertJsonResponse();

            if ( result.getDidSucceed() ) {
                GPUdbLogger.debug_with_info( "Flush thread succeeded" );

                // Aggregate results of GenericRecord insertion
                if( insertResponse != null ) {
                    this.countInserted.addAndGet(insertResponse.getCountInserted());
                    this.countUpdated.addAndGet(insertResponse.getCountUpdated());

                    // Check if shard re-balancing is under way at the server; if so,
                    // we need to update the shard mapping
                    if ("true".equals(result.getInsertResponse().getInfo().get(GPUdbBase.RESPONSE_INFO_DATA_REROUTED))) {
                        doUpdateWorkers = true;
                    }
                }

                // Aggregate results of JSON insertion
                if( insertJsonResponse != null ) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseData = (Map<String, Object>) insertJsonResponse.get("data");
                    this.countInserted.addAndGet((Integer) responseData.get("count_inserted"));
                    this.countUpdated.addAndGet((Integer) responseData.get("count_updated"));
                }

                gatherErrorsFromInsertionResult(result);
            } else {
                // Something went wrong and the data was not inserted, whether
                // from a GenericRecord queue or a JSON one.  A JSON queue's
                // failure carries no response, so it is judged by its exception
                // alone, as a GenericRecord queue's is.  The request layer has
                // already re-sent the batch as often as the rules allow, so a
                // failure that calls for failing over is failed over; any other
                // is reported.
                Exception fe = result.getFailureException();
                if(fe instanceof GPUdbExitException || (fe instanceof GPUdbException && ((GPUdbException)fe).hadConnectionFailure())) {
                    GPUdbLogger.debug_with_info("Setting retry to true");
                    doRetryInsertion = true;
                }
                else {
                    throw new InsertException(getCurrentHeadNodeURL(), result.getFailedRecords(), fe.getMessage());
                }


                // Figure out went wrong with the insertion and what follow-up
                // steps need to be done, including saving the records that
                // could not be inserted.

                failedWorkerUrls.add(result.getWorkerUrl());

                List<T> workerFailedRecords = result.getFailedRecords();
                if (workerFailedRecords != null) {
                    failedRecords.addAll(workerFailedRecords);
                }

                if (result.getDoUpdateWorkers()) {
                    // We're only saving true values so that we don't
                    // accidentally override a true value based on a previous
                    // worker's result with a false value of this worker.  We
                    // are trying to find out if *any* worker has triggered a
                    // need for updating our worker list.
                    GPUdbLogger.debug_with_info("Setting update workers to true");
                    doUpdateWorkers = true;
                }

                if (result.getDoFailover()) {
                    // We're only saving true values so that we don't
                    // accidentally override a true value based on a previous
                    // worker's result with a false value of this worker.  We
                    // are trying to find out if *any* worker has triggered a
                    // need for updating our worker list.
                    GPUdbLogger.debug_with_info("Setting doFailover & updateWorkers to true");
                    doFailover = true;
                    // We need to update the worker if we have to fail over
                    doUpdateWorkers = true;
                }

                int workerCountClusterSwitches = result.getCountClusterSwitches();
                if (workerCountClusterSwitches > latestCountClusterSwitches) {
                    // We're only saving the largest value so that we know the
                    // latest cluster switch (in the gpudb object) that was
                    // encountered by the workers.
                    GPUdbLogger.debug_with_info("Changing # cluster switches from "
                            + latestCountClusterSwitches
                            + " to "
                            + workerCountClusterSwitches);
                    latestCountClusterSwitches = workerCountClusterSwitches;
                }

                long workerInsertionAttemptTimestamp = result.getInsertionAttemptTimestamp();
                if (workerInsertionAttemptTimestamp > latestInsertionAttemptTimestamp) {
                    // We're only saving the largest value so that we know the
                    // latest insertion attempt timestamp by the workers.
                    GPUdbLogger.debug_with_info("Changing insertion attempt time from "
                            + latestInsertionAttemptTimestamp
                            + " to "
                            + workerInsertionAttemptTimestamp);
                    latestInsertionAttemptTimestamp = workerInsertionAttemptTimestamp;
                }

                // Save any exception encountered by the worker
                if (workerFailedRecords != null)
                    GPUdbLogger.debug_with_info(String.format("Saving <%s> ingest worker failure exception with <%d> failed records", workerFailedRecords.getClass(), workerFailedRecords.size()));
                workerExceptions.add(result.getFailureException());
            }
        }  // end loop handling results

        URL currHeadUrl = getCurrentHeadNodeURL();

        // Concatenate all the error messages encountered by the workers, if any
        String originalCauses = "";
        if (doRetryInsertion) {
            StringBuilder builder = new StringBuilder();
            builder.append("[ ");
            int numFailedWorkers = failedWorkerUrls.size();
            for (int i = 0; i < numFailedWorkers; ++i) {
                URL workerUrl = failedWorkerUrls.get(i);
                Exception ex_ = workerExceptions.get(i);

                // Concatenate only non-null exceptions' messages
                if (ex_ != null) {
                    builder.append("worker URL " + workerUrl + ": ");
                    builder.append((ex_.getCause() == null)
                            ? ex_.toString() : ex_.getCause().toString());
                    builder.append("; ");
                }
            }
            builder.append(" ]");
            originalCauses = builder.toString();
            GPUdbLogger.warn("Original causes of failure from ranks: "
                    + originalCauses);
        }

        // With a single cluster there is nowhere to fail over to.  Let the
        // user know this and pass along the records that we could not insert.
        if (doRetryInsertion && this.gpudb.getHARingSize() == 1) {
            String message = "Insertion failed.  Original causes encountered by workers: "
                    + originalCauses;
            throw new InsertException(currHeadUrl, failedRecords, message);
        }

        // Failover if needed
        if (doFailover) {
            try {
                // Switch to a different cluster in the HA ring, if any
                GPUdbLogger.debug_with_info("Before calling forceFailover() "
                        + "with current head URL: "
                        + currHeadUrl);
                forceFailover(currHeadUrl, latestCountClusterSwitches);
            } catch (GPUdbException ex) {
                GPUdbLogger.debug_with_info("Failover failed with exception: "
                        + ex.getMessage());
                // We've now tried all the HA clusters and circled back;
                // propagate the error to the user.

                // Let the user know that there was a problem and which
                // records could not be inserted
                GPUdbException exception = new GPUdbException(ex.getMessage()
                        + ".  Original causes "
                        + " encountered by workers: "
                        + originalCauses,
                        true);
                throw new InsertException(currHeadUrl, failedRecords,
                        exception.getMessage(),
                        exception);
            }
        }   // end failover

        if (doUpdateWorkers) {
            // Update the workers because we either failed over or the shard
            // mapping has to be updated (due to added/removed ranks)
            try {
                GPUdbLogger.debug_with_info("Before calling updateWorkerQueues()");
                updateWorkerQueues();
            } catch (Exception ex) {
                GPUdbLogger.debug_with_info("updateWorkerQueues() failed with "
                        + "exception: "
                        + ex.getMessage());
                // Let the user know that there was a problem and which records
                // could not be inserted
                throw new InsertException(currHeadUrl, failedRecords,
                        ex.getMessage(), ex);
            }
        }

        if (doRetryInsertion) {
            // Re-insert the records that did not get ingested into the cluster
            // failed over to, where each batch starts with a full budget
            GPUdbLogger.debug_with_info("Retry insertion into new cluster");

            this.queueAndFlushFull(failedRecords, false);

            // If this is part of a user-initiated forced flush, the re-inserted
            // records must be flushed too, not left queued
            if (forcedFlush) {
                GPUdbLogger.debug_with_info("Before forced flush");
                flushAll();
                GPUdbLogger.debug_with_info("After forced flush");
            }

            GPUdbLogger.debug_with_info("Done retrying insertion into new cluster");
        }
    }   // end flushQueues

    /**
     * Sends a batch on the calling thread, handing its result in as a worker
     * thread would.
     */
    private static <T> void sendOnThisThread( BatchSend<T> batch,
                                              BlockingQueue<Future<WorkerQueueInsertionResult<T>>> completedQueues,
                                              Map<Future<WorkerQueueInsertionResult<T>>, BatchSend<T>> inFlight ) {
        FutureTask<WorkerQueueInsertionResult<T>> direct = new FutureTask<>( batch );
        direct.run();
        completedQueues.add( direct );
        inFlight.put( direct, batch );
    }


    private void gatherErrorsFromInsertionResult(WorkerQueueInsertionResult<T> result) {
        Map<Integer, Pair<String, T>> errors = result.getErrors();
        if (errors != null) {
            errors.forEach((key,entry) -> {
                this.simulateErrorMode |= this.returnIndividualErrors;
                List<T> records = new ArrayList<>();
                if (entry.getRight() != null)
                    records.add(entry.getRight());
                synchronized (this.errorListLock) {
                    String message = entry.getLeft();
                    message = message.substring( message.indexOf(":") + 1);
                    this.errorList.add(new InsertException(result.getHeadUrl(), records, message));
                }
            });
        }

        List<String> warnings = result.getWarnings();
        if (warnings != null) {
            synchronized (this.errorListLock) {
                warnings.forEach((entry) -> {
                    this.warningList.add(new InsertException(result.getHeadUrl(), null, entry));
                });
            }
        }

        List<InsertException> possiblyStored = result.getPossiblyStored();
        if (possiblyStored != null && !possiblyStored.isEmpty()) {
            synchronized (this.errorListLock) {
                this.warningList.addAll(possiblyStored);
            }
        }
    }


    /**
     * The warnings for the primary key collisions a batch met when it was
     * re-sent after a failure that leaves an earlier copy's outcome
     * unknown.  The earlier copy may have stored those records,
     * so they are reported as possibly stored rather than as rejected:
     * each record the reply names, or the batch as a whole where it names
     * none.  The per-record collisions are removed from {@code errors}
     * and the batch warnings from {@code warnings}.
     *
     * @param <T>  the type of the records
     * @param url  the URL of the cluster the batch was sent to
     * @param uncertainFailure  the failure the batch was re-sent after, as
     *        {@link RequestRetries#getUncertainFailure()} records it, or
     *        {@code null} where it was not re-sent so
     * @param batch  the records in the batch
     * @param errors  the per-record errors from the reply, by record index,
     *        each its message and its record
     * @param warnings  the batch warnings from the reply
     * @return  the warnings to report, or an empty list where the batch
     *          was not re-sent so or met no collisions
     */
    static <T> List<InsertException> possiblyStored( URL url,
                                                     String uncertainFailure,
                                                     List<T> batch,
                                                     Map<Integer, ? extends Map.Entry<String, T>> errors,
                                                     List<String> warnings ) {
        List<InsertException> possiblyStored = new ArrayList<>();
        if (uncertainFailure == null)
            return possiblyStored;

        String note = "may already have been stored by an earlier copy of the batch, "
                      + "which was re-sent after: " + uncertainFailure;

        if (errors != null) {
            for (Iterator<? extends Map.Entry<Integer, ? extends Map.Entry<String, T>>> it = errors.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Integer, ? extends Map.Entry<String, T>> error = it.next();
                if (!RetryRules.isPrimaryKeyCollision( error.getValue().getKey() ))
                    continue;
                it.remove();
                List<T> record = new ArrayList<>();
                if (error.getValue().getValue() != null)
                    record.add( error.getValue().getValue() );
                possiblyStored.add( new InsertException( url, record,
                        "Record " + error.getKey() + " " + note ) );
            }
        }

        if (warnings != null && warnings.removeIf( RetryRules::isPrimaryKeyCollision ))
            possiblyStored.add( new InsertException( url, new ArrayList<>( batch ),
                    "Records the server reported as already existing " + note ) );

        return possiblyStored;
    }


    /**
     * Parses the response to a JSON insert, extracting and returning any
     * warnings from it.
     * 
     * @return  the batch warnings of a JSON insert's reply, from the
     *          {@code info} map of its {@code data}
     */
    static List<String> jsonReplyWarnings( Map<String, Object> reply ) {
        List<String> warnings = new ArrayList<>();
        Object data = reply.get( "data" );
        if (!(data instanceof Map))
            return warnings;

        Object info = ((Map<?, ?>) data).get( "info" );
        if (!(info instanceof Map))
            return warnings;

        for (Map.Entry<?, ?> entry : ((Map<?, ?>) info).entrySet())
            if (String.valueOf( entry.getKey() ).toLowerCase().startsWith( "warning_" ))
                warnings.add( String.valueOf( entry.getValue() ) );
        return warnings;
    }


    /**
     * Whether a table has a primary key, from the column properties its
     * description lists.
     *
     * @param tableInfo  the table's description
     * @return  whether any of its columns is part of a primary key
     */
    static boolean hasPrimaryKey( ShowTableResponse tableInfo ) {
        List<Map<String, List<String>>> properties = tableInfo.getProperties();
        if (properties == null || properties.isEmpty() || properties.get(0) == null)
            return false;

        for (List<String> columnProperties : properties.get(0).values())
            if (columnProperties != null && columnProperties.contains( ColumnProperty.PRIMARY_KEY ))
                return true;
        return false;
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
     * @param refuseIfClosed  whether to refuse the record once this inserter
     *                        is closed:  so for a caller's record, not for one
     *                        accepted earlier and being re-queued
     *
     * @throws GPUdbException if an error occurs while calculating shard/primary keys
     * @throws InsertException if an error occurs while inserting
     * @throws IllegalStateException if the record is refused
     */
    private void insert(T record, boolean flushWhenFull, boolean refuseIfClosed) throws InsertException {
        // Don't accept new records if there was an error thrown already with returnIndividualErrors
        if (this.simulateErrorMode)
            return;

        WorkerQueue<T> workerQueue;
        BatchSend<T> cut;

        // Take the routing state once and route this record entirely from that
        // snapshot.  A rebuild running concurrently publishes a new one; this
        // record then goes wherever the state it was routed against said, which
        // is consistent, rather than to a destination assembled from both.  A
        // rebuild also retires the queues it replaces, so a record routed to
        // one of them after the rebuild emptied it is refused and routed again,
        // against the new state.
        for (int attempt = 0; ; ++attempt) {
            final Routing<T> routing = this.routing;

            if (routing.useHeadNode) {
                // Head-node routing.  Routing<T> guarantees the head-node queue is
                // the only queue, so index 0 is it.
                workerQueue = routing.queues.get(0);
            }
            else if (routing.queues.size() == 1) {
                // A single destination, so there is nothing to choose: skip the
                // shard key work.  Note this is worker rank 1 -- not the head node,
                // which the branch above handles.
                workerQueue = routing.queues.get(0);
            }
            else {
                if ((routing.routingTable == null) || routing.routingTable.isEmpty()) {
                    List<T> queuedRecord = new ArrayList<>();
                    queuedRecord.add(record);
                    throw new InsertException((URL) null, queuedRecord,
                            "No shard mapping is available for table '" + this.tableName
                            + "'; cannot route the record to a worker rank.  Maybe "
                            + "need to update the shard mapping.");
                }

                if( this.isJson ) {
                    // Handle JSON records
                    workerQueue = routing.queues.get(routing.routingTable.get(ThreadLocalRandom.current().nextInt(routing.routingTable.size())) - 1);
                } else {
                    //Handle GenericRecords or RecordObjects
                    RecordKey shardKey = null;

                    try {
                        shardKey = this.shardKeyBuilder.build(record);
                    } catch (Exception ex) {
                        List<T> queuedRecord = new ArrayList<>();
                        queuedRecord.add(record);
                        throw new InsertException((URL) null, queuedRecord,
                                "Unable to calculate shard/primary key; please check data for unshardable values");
                    }

                    if (shardKey == null)
                        workerQueue = routing.queues.get(routing.routingTable.get(ThreadLocalRandom.current().nextInt(routing.routingTable.size())) - 1);
                    else
                        workerQueue = routing.queues.get(shardKey.route(routing.routingTable));
                }
            }


            // Ensure that this is a valid worker queue (and not a previously removed rank)
            if (workerQueue == null) {
                List<T> queuedRecord = new ArrayList<>();
                queuedRecord.add(record);
                throw new InsertException((URL) null, queuedRecord,
                        "Attempted to insert into worker rank that has been removed!  Maybe need to update the shard mapping.");
            }

            // Insert the record into the queue, cutting the batch it fills
            // where this call is the one to send it
            cut = workerQueue.offer(record, refuseIfClosed ? this.closed : null, flushWhenFull,
                                    this.maxRetries, this.tableHasPrimaryKey);
            if (cut == BatchSend.REFUSED_CLOSED)
                throw closedException();
            if (cut != BatchSend.REFUSED_RETIRED)
                break;
            // Retired by a rebuild; route again, against the state it published
            if (attempt >= 100) {
                List<T> queuedRecord = new ArrayList<>();
                queuedRecord.add(record);
                throw new InsertException((URL) null, queuedRecord,
                        "The worker queues for table '" + this.tableName + "' kept being rebuilt; could not queue the record");
            }
        }

        // Send the batch this record filled, if it did
        if (cut != null) {
            this.flushQueues(Collections.singletonList(cut), false);
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
     * Note:  This version of {@link #insert(Object)} will result in sequential
     * batch inserts in a background thread.  Use {@link #insert(List)} to allow
     * multiple queues to reach their batch size and parallelize all of those
     * batch inserts at once.
     *
     * @param record  the record to insert
     *
     * @throws GPUdbException if an error occurs while calculating shard/primary keys
     * @throws InsertException if an error occurs while inserting
     * @throws IllegalStateException if this inserter has been closed
     */
    public void insert(T record) throws InsertException {
        checkOpen();
        // Do flush the queue when it is full.  Note that we are setting
        // flushWhenFull to true here to preserve backward compatibility.
        // Before version 7.0.19.1, the insertion and flushing in this class
        // was single threaded; so each time records got inserted to this class
        // (whether via this insert(single record) or the insert( many records ) )
        // method, we always flushed any queue that became full.  Now that we
        // are using background threads to flush the queues in parallel, if we
        // don't have this 'flushWhenFull' mechanism, the insert( single record )
        // method would _never_ flush any queue.  We can't change the method's
        // behavior that much; it would break existing client code potentially.
        // So, the insert( multiple records ) does not flush the queues upon
        // every individual insert (check that method's code; it passes false
        // where we pass true here for the 2nd parameter), this method does.
        if (this.isJson && this.jsonOptions.isValidateJson() && !JsonUtils.isValidJson((String) record))
            throw new InsertException("String passed in is Not a valid JSON record");

        this.insert( record, true, true );
    }   // end insert( single record )


    /**
     * Queues a list of records for insertion into GPUdb. If any queue reaches
     * the {@link #getBatchSize batch size}, all queues that have reached the
     * {@link #getBatchSize batch size} will have their records inserted into
     * the database before the method returns. If an error occurs while
     * inserting the records, they will no longer be in that queue or the
     * database; catch {@link InsertException} to get the list of records
     * that were being inserted (including any from the queue in question and
     * any remaining in the list not yet queued) if needed (for example, to
     * retry). Note that depending on the number of records, multiple calls to
     * the database may occur.
     *
     * @param records      the records to insert
     * @param refuseIfClosed  whether to refuse the records once this inserter
     *                        is closed; see {@link #insert(Object, boolean, boolean)}
     *
     * @throws InsertException if an error occurs while inserting
     * @throws IllegalStateException if a record is refused
     */
    @SuppressWarnings("unchecked")
    private void queueAndFlushFull(List<T> records, boolean refuseIfClosed) throws InsertException {
        // Don't accept new records if there was an error thrown already with returnIndividualErrors
        if (this.simulateErrorMode)
            return;

        for (int i = 0; i < records.size(); ++i) {
            try {
                // Do not flush after inserting this record (otherwise it
                // becomes essentially sequential flushing).  Parallel flushing
                // only occurs within this method by delaying the flush until
                // all records have been added, then flushing any/all queues
                // that happen to be full at the same time.
                insert( records.get(i), false, refuseIfClosed );
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
        this.flushFullQueues();
        GPUdbLogger.debug_with_info( "After flushing full queues" );
    }


    /**
     * Queues a list of records for insertion into GPUdb. If any queue reaches
     * the {@link #getBatchSize batch size}, all queues that have reached the
     * {@link #getBatchSize batch size} will have their records inserted into
     * the database before the method returns. If an error occurs while
     * inserting the records, they will no longer be in that queue or the
     * database; catch {@link InsertException} to get the list of records
     * that were being inserted (including any from the queue in question and
     * any remaining in the list not yet queued) if needed (for example, to
     * retry). Note that depending on the number of records, multiple calls to
     * the database may occur.
     *
     * Note:  This version of {@link #insert(List)} will result in parallelizing
     * the batch inserts in background threads.
     *
     * @param records  the records to insert
     *
     * @throws InsertException if an error occurs while inserting
     * @throws IllegalStateException if this inserter has been closed.  Where
     *         the close overlaps this call on another thread, the records
     *         queued before it are sent by the close, and the rest are refused.
     */
    @SuppressWarnings("unchecked")
    public void insert(List<T> records) throws InsertException {
        checkOpen();
        if (this.isJson && this.jsonOptions.isValidateJson() && !JsonUtils.isValidJson((List<String>) records))
            throw new InsertException("List passed in is not a list of valid JSON strings");

        this.queueAndFlushFull( records, true );
    }
}
