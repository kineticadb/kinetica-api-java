package com.gpudb;

import com.gpudb.GPUdbBase.GPUdbExitException;
import com.gpudb.protocol.AdminShowShardsRequest;
import com.gpudb.protocol.AdminShowShardsResponse;
import com.gpudb.protocol.GetRecordsRequest;
import com.gpudb.protocol.GetRecordsResponse;
import com.gpudb.protocol.RawGetRecordsResponse;
import com.gpudb.protocol.ShowTableResponse;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.lang3.mutable.MutableLong;

/**
 * Object that permits efficient retrieval of records from GPUdb, with support
 * for multi-head access. {@code RecordRetriever} instances are thread safe and
 * may be used from any number of threads simultaneously.
 *
 * @param <T>  the type of object being retrieved
 */
public class RecordRetriever<T> {
    private final Object haFailoverLock;
    private final GPUdb gpudb;
    private final String tableName;
    private final Type type;
    private final TypeObjectMap<T> typeObjectMap;
    private final RecordKeyBuilder<T> shardKeyBuilder;
    private final boolean isMultiHeadEnabled;
    private final boolean useHeadRank;
    private final boolean useRandomWorker;
    private final boolean isTableReplicated;
    private final int dbHARingSize;
    private Map<String, String> options;
    private int numRanks;
    private long shardVersion;
    private MutableLong shardUpdateTime;
    private int numClusterSwitches;
    private URL currentHeadNodeURL;
    private com.gpudb.WorkerList workerList;
    private List<Integer> routingTable;
    private List<URL> workerUrls;

    /**
     * Creates a {@link RecordRetriever} with the specified parameters.
     *
     * @param gpudb      the GPUdb instance to retrieve records from
     * @param tableName  the table to retrieve records from
     * @param type       the type of records being retrieved
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     */
    public RecordRetriever(GPUdb gpudb, String tableName, Type type) throws GPUdbException {
        this(gpudb, tableName, type, null, null, null);
    }


    /**
     * Creates a {@link RecordRetriever} with the specified parameters.
     *
     * @param gpudb      the GPUdb instance to retrieve records from
     * @param tableName  the table to retrieve records from
     * @param type       the type of records being retrieved
     * @param options    optional parameters to pass to GPUdb while retrieving
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     */
    public RecordRetriever( GPUdb gpudb, String tableName, Type type,
                            Map<String, String> options )
        throws GPUdbException {
        this(gpudb, tableName, type, null, null, options);
    }


    /**
     * Creates a {@link RecordRetriever} with the specified parameters.
     *
     * @param gpudb      the GPUdb instance to retrieve records from
     * @param tableName  the table to retrieve records from
     * @param type       the type of records being retrieved
     * @param workers    worker list for multi-head retrieval ({@code null} to
     *                   disable multi-head retrieval)
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     */
    public RecordRetriever(GPUdb gpudb, String tableName, Type type,
                           WorkerList workers) throws GPUdbException {
        this(gpudb, tableName, type, null, workers, null);
    }


    /**
     * Creates a {@link RecordRetriever} with the specified parameters.
     *
     * @param gpudb      the GPUdb instance to retrieve records from
     * @param tableName  the table to retrieve records from
     * @param type       the type of records being retrieved
     * @param workers    worker list for multi-head retrieval ({@code null} to
     *                   disable multi-head retrieval)
     * @param options    optional parameters to pass to GPUdb while retrieving
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     */
    public RecordRetriever( GPUdb gpudb, String tableName, Type type,
                            WorkerList workers,
                            Map<String, String> options) throws GPUdbException {
        this(gpudb, tableName, type, null, workers, options);
    }


    /**
     * Creates a {@link RecordRetriever} with the specified parameters.
     *
     * @param gpudb          the GPUdb instance to retrieve records from
     * @param tableName      the table to retrieve records from
     * @param typeObjectMap  type object map for the type of records being
     *                       retrieved
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     */
    public RecordRetriever( GPUdb gpudb, String tableName,
                            TypeObjectMap<T> typeObjectMap)
        throws GPUdbException {
        this(gpudb, tableName, typeObjectMap.getType(), typeObjectMap, null, null);
    }


    /**
     * Creates a {@link RecordRetriever} with the specified parameters.
     *
     * @param gpudb          the GPUdb instance to retrieve records from
     * @param tableName      the table to retrieve records from
     * @param typeObjectMap  type object map for the type of records being
     *                       retrieved
     * @param options        optional parameters to pass to GPUdb while retrieving
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     */
    public RecordRetriever( GPUdb gpudb, String tableName,
                            TypeObjectMap<T> typeObjectMap,
                            Map<String, String> options )
        throws GPUdbException {
        this(gpudb, tableName, typeObjectMap.getType(), typeObjectMap, null, options);
    }



    /**
     * Creates a {@link RecordRetriever} with the specified parameters.
     *
     * @param gpudb          the GPUdb instance to retrieve records from
     * @param tableName      the table to retrieve records from
     * @param typeObjectMap  type object map for the type of records being
     *                       retrieved
     * @param workers        worker list for multi-head retrieval ({@code null}
     *                       to disable multi-head retrieval)
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     */
    public RecordRetriever( GPUdb gpudb, String tableName,
                            TypeObjectMap<T> typeObjectMap,
                            WorkerList workers) throws GPUdbException {
        this(gpudb, tableName, typeObjectMap.getType(), typeObjectMap, workers, null);
    }


    /**
     * Creates a {@link RecordRetriever} with the specified parameters.
     *
     * @param gpudb          the GPUdb instance to retrieve records from
     * @param tableName      the table to retrieve records from
     * @param typeObjectMap  type object map for the type of records being
     *                       retrieved
     * @param workers        worker list for multi-head retrieval ({@code null}
     *                       to disable multi-head retrieval)
     * @param options        optional parameters to pass to GPUdb while retrieving
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     */
    public RecordRetriever( GPUdb gpudb, String tableName,
                            TypeObjectMap<T> typeObjectMap,
                            WorkerList workers,
                            Map<String, String> options ) throws GPUdbException {
        this(gpudb, tableName, typeObjectMap.getType(), typeObjectMap, workers, options);
    }


    private RecordRetriever( GPUdb gpudb,
                             String tableName,
                             Type type,
                             TypeObjectMap<T> typeObjectMap,
                             WorkerList workers,
                             Map<String, String> options ) throws GPUdbException {

        haFailoverLock = new Object();

        this.gpudb = gpudb;
        this.tableName = tableName;
        this.type = type;
        this.typeObjectMap = typeObjectMap;
        this.workerList    = workers;

        this.shardVersion = 0;
        this.shardUpdateTime = new MutableLong();

        if (options != null) {
            this.options = new HashMap<>(options);
        } else {
            // We'll need to use at least the 'expressions' in the options
            this.options = new HashMap<>();
        }

        // We need to know how many clusters are in the HA ring (for failover
        // purposes)
        this.dbHARingSize = gpudb.getHARingSize();

        // Keep track of how many times the db client has switched HA clusters
        // in order to decide later if it's time to update the worker queues
        this.numClusterSwitches = gpudb.getNumClusterSwitches();

        // Keep track of which cluster we're using (helpful in knowing if an
        // HA failover has happened)
        this.currentHeadNodeURL = gpudb.getURL();

        // Check if the table is replicated or not; in case we can't figure it
        // out, we will pretend it is not
        boolean isTableReplicated = false;
        try {
            // Check whether 'replicated' is in one of the response fields
            isTableReplicated = this.gpudb.showTable( this.tableName, null )
                .getTableDescriptions()
                .get(0)
                .contains( ShowTableResponse.TableDescriptions.REPLICATED );
        } catch ( GPUdbException ex ) {
            // Ignore any issue and carry on; it's ok if the table does not
            // exist yet.  Who knows when the user would instantiate this
            // object--quite possibly before creating the table.  So no worries.
        }
        this.isTableReplicated = isTableReplicated;

        // For replicated tables, we might want to use a random worker instead
        // of the head node (completely depended upon the database version
        // running on the server.  So, we need to hardcode the versions that
        // will have this change.
        if ( isTableReplicated ) {
            // Check the server version
            GPUdbBase.GPUdbVersion serverVersion = this.gpudb.getServerVersion();
            if ( serverVersion == null ) {
                // When we can't know the server version, we won't risk
                // using random workers for replicated tables.  We will
                // just send the requests to the head node.
                this.useHeadRank     = true;
                this.useRandomWorker = false;
            } else if ( serverVersion.isNewerThan( 7, 1, 2, 0 ) ) {
                // Anything newer than 7.1.2.0 can handle replicated tables
                // at the worker ranks for key lookup; [7.1.0.0, 7.1.2.0) can't.
                this.useHeadRank     = false;
                this.useRandomWorker = true;
            } else if ( serverVersion.isNewerThan( 7, 0, 20, 2 ) ) {
                // Anything in [7.0.20.1, 7.1.0.0) can handle replicated tables
                // at the worker ranks for key lookup; but 7.0.20.0 can't.
                this.useHeadRank     = false;
                this.useRandomWorker = true;
            } else if ( serverVersion.isNewerThan( 7, 0, 19, 14 ) ) {
                // Anything in [7.0.19.3, 7.0.20.0) can handle replicated tables
                // at the worker ranks for key lookup
                this.useHeadRank     = false;
                this.useRandomWorker = true;
            } else {
                // All versions prior to 7.0.19.3 can NOT handle replicated
                // tables at the worker ranks for key lookup; must use the head
                // rank.
                this.useHeadRank     = true;
                this.useRandomWorker = false;
            }
        } else {
            // If not a replicated table, then this doesn't apply at all
            this.useHeadRank     = false;
            this.useRandomWorker = false;
        }

        RecordKeyBuilder<T> shardKeyBuilderTemp;

        if (typeObjectMap == null) {
            shardKeyBuilderTemp = new RecordKeyBuilder<>(false, type);

            if (!shardKeyBuilderTemp.hasKey()) {
                shardKeyBuilderTemp = new RecordKeyBuilder<>(true, type);
            }
        } else {
            shardKeyBuilderTemp = new RecordKeyBuilder<>(false, typeObjectMap);

            if (!shardKeyBuilderTemp.hasKey()) {
                shardKeyBuilderTemp = new RecordKeyBuilder<>(true, typeObjectMap);
            }
        }

        shardKeyBuilder = shardKeyBuilderTemp;

        this.workerUrls = new ArrayList<>();

        // Set if multi-head I/O is turned on at the server
        this.isMultiHeadEnabled = ( (this.workerList != null)
                                    && !this.workerList.isEmpty() );

        if ( this.isMultiHeadEnabled ) {
            try {
                for (URL url : workers) {
                    if (url == null) {
                        // Handle removed ranks
                        this.workerUrls.add( null );
                    } else { // add a URL for an active rank
                        this.workerUrls.add(GPUdbBase.appendPathToURL(url, "/get/records"));
                    }
                }
            } catch (MalformedURLException ex) {
                throw new GPUdbException(ex.getMessage(), ex);
            }

            // Update the worker queues, if needed
            updateWorkerQueues( this.numClusterSwitches, false );

            this.numRanks = this.workerUrls.size();

        } else {
            routingTable = null;
            this.numRanks = 1;
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
     * Use the current head node URL in a thread-safe manner.
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
        // The whole failover scenario needs to happen in a thread-safe
        // manner; since this happens only upon failure, it's ok to
        // synchronize the whole block
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

            boolean isClusterHealthy = true;

            // We did switch to a different cluster; now check the health
            // of the cluster, starting with the head node
            if ( !this.gpudb.isKineticaRunning( this.gpudb.getURL() ) ) {
                continue; // try the next cluster because this head node is down
            }

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
        }

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
     * Updates the shard mapping based on the latest cluster configuration.
     * Optionally, also reconstructs the worker queues based on the new sharding.
     *
     * @param doReconstructWorkerQueues  Boolean flag indicating if the worker
     *                                   queues ought to be re-built.
     *
     * @return  a bool indicating whether the shard mapping was updated or not.
     */
    private synchronized boolean updateWorkerQueues( int countClusterSwitches, boolean doReconstructWorkerURLs ) throws GPUdbException {
        // The entire worker queue update process should happen in a thread-safe
        // manner; since this happens only upon failover, the time penalty for
        // single-threading this part is acceptable
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

            // Save the new shard version and also when we're updating the mapping
            this.shardVersion = newShardVersion;

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
        // /admin/show/shards; so update the current head node url
        this.setCurrentHeadNodeURL( this.gpudb.getURL() );
        this.setCurrentClusterSwitchCount( this.gpudb.getNumClusterSwitches() );

        // The worker queues need to be re-constructed when asked for
        // iff multi-head i/o is enabled and the table is not replicated
        if ( doReconstructWorkerURLs
             && this.isMultiHeadEnabled )
        {
            reconstructWorkerURLs();
        }

        return true; // the shard mapping was updated indeed
    }  // end updateWorkerQueues


    /**
     * Reconstructs the list of worker URLs.
     */
    private synchronized void reconstructWorkerURLs() throws GPUdbException {

        // Get the latest worker list (use whatever IP regex was used initially)
        if ( this.workerList == null )
            throw new GPUdbException( "No worker list exists!" );
        com.gpudb.WorkerList newWorkerList = new com.gpudb.WorkerList( this.gpudb,
                                                                       this.workerList.getIpRegex() );
        if (newWorkerList == this.workerList) {
            return; // nothing to do since the worker list did not change
        }

        // Update the worker list
        this.workerList = newWorkerList;

        // Create worker queues per worker URL
        List<URL> newWorkerUrls = new ArrayList<>();
        for ( URL url : this.workerList) {
            try {
                // Handle removed ranks
                if (url == null) {
                    newWorkerUrls.add( null );
                }
                else {
                    // Add a queue for a currently active rank
                    newWorkerUrls.add( GPUdbBase.appendPathToURL(url, "/get/records") );
                }
            } catch (MalformedURLException ex) {
                throw new GPUdbException( ex.getMessage(), ex );
            } catch (Exception ex) {
                throw new GPUdbException( ex.getMessage(), ex );
            }
        }

        // Get the number of workers
        this.numRanks = newWorkerUrls.size();

        // Save the new queue for future use
        this.workerUrls = newWorkerUrls;
    }  // end reconstructWorkerURLs


    /**
     * Gets the GPUdb instance from which records will be retrieved.
     *
     * @return  the GPUdb instance from which records will be retrieved
     */
    public GPUdb getGPUdb() {
        return gpudb;
    }

    /**
     * Gets the name of the table from which records will be retrieved.
     *
     * @return  the name of the table from which records will be retrieved
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * @return  whether this {@link RecordRetriever} object is using the
     *          head-rank to do a simple record fetching (not utilizing
     *          the server's key lookup feature) (true value), or using
     *          multi-head (the worker ranks) for key lookup (false value).
     */
    public boolean isUsingHeadRank() {
        return this.useHeadRank;
    }

    /**
     * Gets the options currently used for the retriever methods.  Note
     * that any {@link com.gpudb.protocol.GetRecordsRequest.Options#EXPRESSION}
     * options will get overridden at the next {@link #getByKey} call with the
     * appropriate expression.
     *
     * @return  the options used during record retrieval
     *
     * @see #setOptions
     */
    public Map<String, String> getOptions() {
        return this.options;
    }


    /**
     * Sets the options to be used for the retriever methods.
     *
     * @param options  the options to be used during record retrieval
     *
     * @return         the current {@link RecordRetriever} instance
     *
     * @see com.gpudb.protocol.GetRecordsRequest.Options#EXPRESSION
     */
    public RecordRetriever setOptions( Map<String, String> options ) {
        // Set the options in a thread-safe manner
        synchronized (this.options) {
            if (options != null) {
                this.options = new HashMap<>(options);
            } else {
                // We'll need to use at least the 'expressions' in the options
                this.options = new HashMap<>();
            }
        }

        return this;
    }

    /**
     * Retrieves records for a given shard key, optionally further limited by an
     * additional expression. All records matching the key and satisfying the
     * expression will be returned, up to the system-defined limit. For
     * multi-head mode the request will be sent directly to the appropriate
     * worker.
     * <p>
     * All fields in both the shard key and the expression must have defined
     * attribute indexes, unless the shard key is also a primary key and all
     * referenced fields are in the primary key. The expression must be limited
     * to basic equality and inequality comparisons that can be evaluated using
     * the attribute indexes.
     * <p>
     * For replicated tables, the user may pass a pre-constructed expression via
     * the second parameter.  If both parameters are empty or null, then all the
     * records would be fetched.
     *
     * @param keyValues   list of values that make up the shard key; the values
     *                    must be in the same order as in the table and have the
     *                    correct data types, if given.  The user may provide
     *                    an empty or null list for replicated tables; in that
     *                    case, only the string expression would be used.
     * @param expression  additional filter to be applied, or {@code null} for
     *                    none.  For replicated tables, if neither is given,
     *                    all records would be fetched.
     * @return            {@link com.gpudb.protocol.GetRecordsResponse response}
     *                    object containing the results
     *
     * @throws GPUdbException if an error occurs during the operation
     */
    public GetRecordsResponse<T> getByKey(List<Object> keyValues, String expression)
        throws GPUdbException {

        String originalExpression = expression;

        // For replicated tables, we don't need any shard or primary keys for
        // looking up records.  But, for regular tables, must have at least some
        // primary or shard columns.  (For shard columns, attribute indexing is
        // needed, but will let the server check for that.)
        if ( !shardKeyBuilder.hasKey() && !this.isTableReplicated ) {
            // Table type does not have any primary or shard columns, nor are
            // we using the head rank or a random rank.  So, there's no way for
            // us to calculate which precise rank to send request to!
            String msg = ("Cannot get by key from unsharded regular "
                          + " (non-replicated) table.");
            throw new IllegalStateException( msg );
        }

        // Convert null expressions to empty an empty string so that we don't
        // need to keep checking for null later
        if ( expression == null ) {
            expression = "";
        }

        // Build the expression to be used for the lookup
        if ( shardKeyBuilder.hasKey() ) {
            // We have primary or shard columns; need to use the given key values
            // to create the filter expression.
            String keyExpression = "";
            if ( keyValues != null ) {
                keyExpression = shardKeyBuilder.buildExpression( keyValues );
            } else if ( !this.isTableReplicated ) {
                // It is ok for the user to bypass the keys and give a string
                // expression for replicated tables only.
                throw new GPUdbException( "For sharded tables, must provide the key values for key lookup." );
            }

            if ( expression.isEmpty() ) {
                // We are not dealing with the case where they keys were empty.
                // If both keys and the extra expression are empty, the user would
                // get ALL the records!
                expression = keyExpression;
            } else if ( !keyExpression.isEmpty() ){
                // Join the two non-empty expressions together
                expression = ( "(" + keyExpression + ") and (" + expression + ")" );
            }
        } else {
            // No primary or shard key in the table
            if ( ( (keyValues != null) && !keyValues.isEmpty() )
                 && this.isTableReplicated ) {
                // No primary keys for a replicated table; must give key values
                // via te second parameter, not keyValues (since we have no way
                // of knowing which columns the given values are for).
                String msg = ("For replicated tables without primary keys, please pass "
                              + "all key values in the 'expression' argument, not via "
                              + "the 'keyValues' argument.");
                throw new GPUdbException( msg );
            }
        }


        // Create the options for the key lookup; first include general options
        Map<String, String> retrievalOptions = new HashMap<>( this.options );
        // Add the retrieval expression to the options, if any
        if ( !expression.isEmpty() ) {
            retrievalOptions.put(GetRecordsRequest.Options.EXPRESSION, expression);

            // Additionally check if we need to add the fast index lookup
            // option.  Note that this option only applies if there is a
            // non-empty expression.  We could have an empty expression if no
            // key value nor any expression is provided by the user
            if ( shardKeyBuilder.hasKey() ) {
                retrievalOptions.put( GetRecordsRequest.Options.FAST_INDEX_LOOKUP,
                                      GetRecordsRequest.Options.TRUE );
            }
        }

        GetRecordsRequest request = new GetRecordsRequest(tableName, 0, GPUdb.END_OF_SET, retrievalOptions);
        RawGetRecordsResponse response = new RawGetRecordsResponse();

        GetRecordsResponse<T> decodedResponse = new GetRecordsResponse<>();

        // Save a snapshot of the state of the object pre-retrieval
        long retrievalAttemptTimestamp = new Timestamp( System.currentTimeMillis() ).getTime();
        URL currURL = getCurrentHeadNodeURL();
        int currentCountClusterSwitches = getCurrentClusterSwitchCount();

        try {
            if ( this.isMultiHeadEnabled && (routingTable != null)
                 && !this.useHeadRank ) {

                // Get the record(s) from a worker rank; whether from a random
                // or a specific one depends on a few things
                URL url;

                // Special condition for random worker rank: table is replicated
                // and the server version can handle key lookup queries for such
                // tables at the worker ranks
                if ( this.useRandomWorker ) {
                    // We don't need to waste time by calculating sharding; the
                    // data is available at all ranks!
                    url = workerUrls.get( ThreadLocalRandom.current().nextInt( workerUrls.size() ) );
                } else {
                    // Not a replicated table; so calculate the shard to figure
                    // out which worker rank contains the requested records
                    RecordKey shardKey;
                    try {
                        shardKey = shardKeyBuilder.build( keyValues );
                    } catch (GPUdbException ex) {
                        throw new GPUdbException( "Unable to calculate the shard value; please check data for unshardable values");
                    }

                    url = workerUrls.get( shardKey.route( routingTable ) );
                }

                // Get the records from the specified worker rank
                response = gpudb.submitRequest(url, request, response, false);
            } else {
                // Get from the head node
                response = gpudb.submitRequest("/get/records", request, response, false);
            }

            // Check if shard re-balancing is under way at the server; if so,
            // we need to update the shard mapping
            if ( response.getInfo().get( "data_rerouted" ) == "true" ) {
                updateWorkerQueues( currentCountClusterSwitches );
            }

            // Set up the decoded response
            decodedResponse.setTableName(  response.getTableName()  );
            decodedResponse.setTypeName(   response.getTypeName()   );
            decodedResponse.setTypeSchema( response.getTypeSchema() );

            // Decode the actual resposne
            if (typeObjectMap == null) {
                decodedResponse.setData( gpudb.<T>decode(type, response.getRecordsBinary()) );
            } else {
                decodedResponse.setData( gpudb.<T>decode(typeObjectMap, response.getRecordsBinary()) );
            }

            decodedResponse.setTotalNumberOfRecords(response.getTotalNumberOfRecords());
            decodedResponse.setHasMoreRecords(response.getHasMoreRecords());
        } catch (GPUdbException ex) {
            if ( (ex instanceof GPUdbExitException)
                 || ex.hadConnectionFailure() ) {
                // We did encounter an HA failover trigger
                // Switch to a different, healthy cluster in the HA ring, if any
                try {
                    // Switch to a different, healthy cluster in the HA ring, if any
                    forceHAFailover( currURL, currentCountClusterSwitches );
                } catch (GPUdbException ex2) {
                    // We've now tried all the HA clusters and circled back;
                    // propagate the error to the user
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause
                                              + ex2.getMessage(), true );
                }
            }

            // Update the worker queues since we've failed over to a
            // different cluster
            boolean updatedWorkerQueues = updateWorkerQueues( currentCountClusterSwitches );
            boolean retry = false;
            synchronized ( this.shardUpdateTime ) {
                retry = ( updatedWorkerQueues
                          || ( retrievalAttemptTimestamp < this.shardUpdateTime.longValue() ) );
            }
            if ( retry ) {
                // We need to try fetching the records again
                try {
                    // Don't use the modified expression;use the original one
                    return this.getByKey( keyValues, originalExpression );
                } catch (Exception ex2) {
                    // Re-setting the exception since we may re-try again
                    throw new GPUdbException( ex2.getMessage() );
                }
            }
            throw new GPUdbException( ex.getMessage() );
        } catch (Exception ex) {
            // Retrieval failed, but maybe due to shard mapping changes (due to
            // cluster reconfiguration)? Check if the mapping needs to be updated
            // or has been updated by another thread already after the
            // insertion was attemtped
            boolean updatedWorkerQueues = updateWorkerQueues( currentCountClusterSwitches );
            boolean retry = false;
            synchronized ( this.shardUpdateTime ) {
                retry = ( updatedWorkerQueues
                          || ( retrievalAttemptTimestamp < this.shardUpdateTime.longValue() ) );
            }
            if ( retry ) {
                // We need to try fetching the records again
                try {
                    return this.getByKey( keyValues, expression );
                } catch (Exception ex2) {
                    // Re-setting the exception since we may re-try again
                    throw new GPUdbException( ex2.getMessage() );
                }
            }
            throw new GPUdbException( ex.getMessage() );
        }

        return decodedResponse;
    }  // getByKey()


    /**
     * Note: If a regular retriever method is implemented (other than "by key"),
     *       then some changes would need to be made to the options.  Currently,
     *       `getByKey()` sets the `expressions` option and keeps it there since
     *       it will get overridden during the next `getByKey()` call.  However,
     *       if there is a method like `getAllRecords()` from the worker ranks
     *       directly, then such a saved expression in the options would change/
     *       limit the records fetched.  In that case, the options handling would
     *       have to be modified as necessary.  Also, currently, the `fast index
     *       lookup` option is always set in the constructor; that would not
     *       necessarily apply; we would need to take care of that as well.
     *
     *       The `setOptions()` method would have to be changed as well.
     */

}  // class RecordRetriever
