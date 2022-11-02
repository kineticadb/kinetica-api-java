package com.gpudb;

import com.gpudb.GPUdbBase.GPUdbExitException;
import com.gpudb.GPUdbBase.GPUdbUnauthorizedAccessException;
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
    private RecordKeyBuilder<T> shardKeyBuilder;
    private final boolean isMultiHeadEnabled;
    private boolean isWorkerLookupSupported;
    private boolean isTableReplicated;
    private final int dbHARingSize;
    private Map<String, String> options;
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

        // Keep track of how many times the DB client has switched HA clusters
        // in order to decide later if it's time to update the worker queues
        this.numClusterSwitches = gpudb.getNumClusterSwitches();

        // Keep track of which cluster we're using (helpful in knowing if an
        // HA failover has happened)
        this.currentHeadNodeURL = gpudb.getURL();

        // Set if user has provided rank URLs
        this.isMultiHeadEnabled = ( (this.workerList != null)
                                    && !this.workerList.isEmpty() );

        // If no rank URLs are provided, use the head rank
        this.isWorkerLookupSupported = this.isMultiHeadEnabled;

        // Check if the table is replicated or not; in case we can't figure it
        // out, we will pretend it is not
        try {
            // Check whether 'replicated' is in one of the response fields
            this.isTableReplicated = this.gpudb.showTable( this.tableName, null )
                .getTableDescriptions()
                .get(0)
                .contains( ShowTableResponse.TableDescriptions.REPLICATED );
        } catch ( GPUdbException ex ) {
            // Ignore any issue and carry on; it's ok if the table does not
            // exist yet.  Who knows when the user would instantiate this
            // object--quite possibly before creating the table.  So no worries.
        }

        // For replicated tables, we might have to use the head node instead of
        // a random worker, depended upon whether the database version supports
        // it.  So, we need to hardcode the versions that will have this change.
        //
        // Since isWorkerLookupSupported is true if MH is active (by this point
        // in init), just set it to false if the server can't support it for
        // replicated tables
        if ( this.isTableReplicated ) {
            // Check the server version
            GPUdbBase.GPUdbVersion serverVersion = this.gpudb.getServerVersion();
            if ( serverVersion == null ) {
                // Use head rank only if the server is too old or broken to
                // return its version number
                GPUdbLogger.warn( "Server returned a null version" );
                this.isWorkerLookupSupported = false;
            }
            else if ( serverVersion.isOlderThan( 7, 1, 3, 0 ) ) {
                // Anything newer than 7.1.2.0 can handle replicated tables
                // at the worker ranks for key lookup; [7.1.0.0, 7.1.2.0] can't.
                this.isWorkerLookupSupported = false;
            }
        }

        this.shardKeyBuilder = new RecordKeyBuilder<>(type, typeObjectMap);

        this.workerUrls = new ArrayList<>();

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

            // If ranks have not been assigned by updateWorkerQueues,
            // this is a randomly-sharded table; use head rank
            if (this.routingTable == null)
                this.isWorkerLookupSupported = false;
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
     * Force a high-availability cluster (inter-cluster) or ring-resiliency
     * (intra-cluster) failover over, as appropriate.  Check the health of the
     * cluster (either head node only, or head node and worker ranks, based on
     * the retriever configuration), and use it if healthy.  If no healthy cluster
     * is found, then throw an error.  Otherwise, stop at the first healthy cluster.
     *
     *
     * @returns whether a successful failover recovery happened.
     *
     * @throws GPUdbException if a successful failover could not be achieved.
     */
    private synchronized boolean forceFailover(URL oldURL, int currCountClusterSwitches)
        throws GPUdbException {
        GPUdbLogger.debug_with_info( "Forced failover begin..." );
        // The whole failover scenario needs to happen in a thread-safe
        // manner; since this happens only upon failure, it's ok to
        // synchronize the whole method

        // We'll need to know which URL we're using at the moment
        URL currURL = oldURL;

        // Try to fail over as many times as there are clusters
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

            // Check if we switched the rank-0 URL
            boolean didSwitchURL = !currURL.equals( oldURL );

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
                GPUdbLogger.debug_with_info( "Did we actually switch the URL? "
                                             + didSwitchURL );
                return didSwitchURL;
            }
            // else, this cluster is not healthy; try switching again
        }   // end for

        // If we get here, it means we've failed over across the whole HA ring at least
        // once (could be more times if other threads are causing failover, too)
        String errorMsg = ("HA failover could not find any healthy cluster (all GPUdb clusters with "
                           + "head nodes [" + currURL.toString()
                           + "] tried)");
        throw new GPUdbException( errorMsg );
    }   // end forceFailover


    /**
     * Updates the shard mapping based on the latest cluster configuration.
     * Also reconstructs the worker queues based on the new sharding.
     *
     * @return  whether the shard mapping was updated or not.
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

        // Flag for if the worker rank URLs need to be re-constructed when asked
        // for iff multi-head i/o is enabled and the caller asked for it.
        boolean reconstructWorkerURLS = ( doReconstructWorkerURLs
                                          && this.isMultiHeadEnabled );
        GPUdbLogger.debug_with_info( "Reconstruct worker URLs?: "
                                     + reconstructWorkerURLS );

        // The entire worker queue update process should happen in a thread-safe
        // manner; since this happens only upon failover, the time penalty for
        // single-threading this part is acceptable
        try {
            // Get the latest shard mapping information; note that this endpoint
            // call might trigger an N+1 or HA failover in the GPUdb object
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
                    if ( reconstructWorkerURLS )
                    {
                        // The caller needs to know if we ended up updating the
                        // worker rank URLs
                        return reconstructWorkerURLs();
                    }

                    // Not appropriate to update worker URLs; then no change
                    // has happened
                    GPUdbLogger.debug_with_info( "Returning false" );
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
                GPUdbLogger.debug_with_info( "Had connection failure: "
                                             + ex.getMessage() );
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
        if ( reconstructWorkerURLS )
        {
            reconstructWorkerURLs();
        }

        GPUdbLogger.debug_with_info( "Returning true" );
        return true; // the shard mapping was updated indeed
    }  // end updateWorkerQueues


    /**
     * Reconstructs the list of worker URLs.
     *
     * @returns whether we ended up reconstructing the worker queues or not.
     */
    private synchronized boolean reconstructWorkerURLs() throws GPUdbException {

        // Get the latest worker list (use whatever IP regex was used initially)
        if ( this.workerList == null )
            throw new GPUdbException( "No worker list exists!" );

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

        // Save the new URLs for future use
        this.workerUrls = newWorkerUrls;

        GPUdbLogger.debug_with_info( "Worker list was updated, returning true" );
        return true; // we did change the URLs!
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
        return !this.isWorkerLookupSupported;
    }

    /**
     * @return  whether this {@link RecordRetriever} object is using the worker
     *          ranks to do key lookups (true value), or doing simple record
     *          fetching (not the server's key lookup feature) (false value).
     *          Note that this will not reflect the non-worker lookup scenario
     *          where only an expression is supplied and the table is sharded.
     */
    public boolean isDoingWorkerLookup() {
        return this.isWorkerLookupSupported;
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
    public RecordRetriever<T> setOptions( Map<String, String> options ) {
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
     * Retrieves records with the given key values and filter expression from
     * the database using a direct-to-rank fast key lookup, if possible, and
     * falling back to a standard lookup via the head node, if not.
     * 
     * This method operates in four modes, depending on the parameters passed:
     * 
     * * keyValues only - attempts a direct-to-rank lookup for records matching
     *                    the given key values
     * * keyValues & expression - attempts a direct-to-rank lookup for records
     *                    matching the given key values, filtering them by the
     *                    given expression
     * * expression only - requests, via the head rank, all records in the table
     *                    matching the given filter expression
     * * neither - retrieves all records from the table via the head rank
     *
     * @param keyValues   the key values to use for the lookup; these must
     *                    correspond to either the explicit or implicit shard
     *                    key for sharded tables or the primary key of
     *                    replicated tables
     * @param expression  a filter expression that will be applied to the data
     *                    requested by the key values; if no key values are
     *                    specified this filter will be applied to all of the
     *                    data in the target table
     *
     * @return            a {@link com.gpudb.protocol.GetRecordsResponse} with
     *                    the requested records
     */
    public GetRecordsResponse<T> getByKey(List<Object> keyValues, String expression)
            throws GPUdbException {

        boolean doWorkerLookup = this.isWorkerLookupSupported;
        String compositeExpression = expression;
        boolean keyValuesSpecified = keyValues != null && !keyValues.isEmpty();

        if (!keyValuesSpecified) {
            // Use head rank if table is [randomly] sharded and no keys are given,
            //   or if the table is replicated and no expression (or keys) is given.
            if (!this.isTableReplicated || expression == null || expression.isEmpty())
                doWorkerLookup = false;
        } else {
            // Eliminate the case where key values are given, but the table has no
            //   key columns with which to associate them
            if (!shardKeyBuilder.hasKey())
                throw new IllegalArgumentException(
                        "Cannot associate the specified keyValues with columns, " +
                        "as the table has no primary or shard key."
                );

            // Since we have a keyed table, build the composite expression to be
            //   used for both sharded-with-PK/SK and replicated-with-PK tables;
            //   randomly-sharded & replicated-without-PK will only use expression
            String keyExpression = shardKeyBuilder.buildExpression( keyValues );

            // If the key expression exists, but the filter expression doesn't,
            //   use the key, otherwise if both exist, concatenate both
            if ( keyExpression != null && !keyExpression.isEmpty() )
                if ( expression == null || expression.isEmpty() )
                    compositeExpression = keyExpression;
                else
                    compositeExpression = ( "(" + keyExpression + ") and (" + expression + ")" );
        }

        // Create the options for the key lookup; first include general options
        Map<String, String> retrievalOptions = new HashMap<>( this.options );

        // Add the retrieval expression to the options, if any
        if ( compositeExpression != null && !compositeExpression.isEmpty() )
        {
            retrievalOptions.put(GetRecordsRequest.Options.EXPRESSION, compositeExpression);

            // If key values were specified, add fast index lookup option
            if ( keyValuesSpecified )
                retrievalOptions.put(
                        GetRecordsRequest.Options.FAST_INDEX_LOOKUP,
                        GetRecordsRequest.Options.TRUE
                );
        }


        GetRecordsRequest request = new GetRecordsRequest(tableName, 0, GPUdb.END_OF_SET, retrievalOptions);
        RawGetRecordsResponse response = new RawGetRecordsResponse();
        GetRecordsResponse<T> decodedResponse = new GetRecordsResponse<>();
        

        long retrievalAttemptTimestamp = new Timestamp( System.currentTimeMillis() ).getTime();
        URL currURL = getCurrentHeadNodeURL();
        int currentCountClusterSwitches = getCurrentClusterSwitchCount();

        try {
            if (!doWorkerLookup) {
                // Get from the head node
                GPUdbLogger.debug_with_info( "Retrieving records from rank-0 with <" + compositeExpression + ">" );
                response = gpudb.submitRequest("/get/records", request, response, false);
            } else {
                // Get the record(s) from a worker rank; whether from a random
                // or a specific one depends on a few things
                URL url;

                // If the table is replicated and it's determined that the
                // server supports it, use random worker rank for lookups
                if ( this.isTableReplicated ) {
                    url = this.workerUrls.get( ThreadLocalRandom.current().nextInt( this.workerUrls.size() ) );
                } else {
                    // Not a replicated table; so calculate the shard to figure
                    // out which worker rank contains the requested records
                    RecordKey shardKey;
                    try {
                        shardKey = shardKeyBuilder.build( keyValues );
                    } catch (GPUdbException ex) {
                        throw new GPUdbException( "Unable to calculate the shard value; please check data for unshardable values");
                    }

                    url = this.workerUrls.get( shardKey.route( this.routingTable ) );
                }

                GPUdbLogger.debug_with_info( "Retrieving records from <" + url.toString() + "> with <" + compositeExpression + ">" );
                response = gpudb.submitRequest(url, request, response, false);
            }

            // Check if shard re-balancing is under way at the server; if so,
            // we need to update the shard mapping
            if ( response.getInfo().get( "data_rerouted" ) == "true" )
                updateWorkerQueues( currentCountClusterSwitches );

            // Set up the decoded response
            decodedResponse.setTableName(  response.getTableName()  );
            decodedResponse.setTypeName(   response.getTypeName()   );
            decodedResponse.setTypeSchema( response.getTypeSchema() );

            // Decode the actual response
            if (typeObjectMap == null)
                decodedResponse.setData( gpudb.<T>decode(type, response.getRecordsBinary()) );
            else
                decodedResponse.setData( gpudb.<T>decode(typeObjectMap, response.getRecordsBinary()) );

            decodedResponse.setTotalNumberOfRecords(response.getTotalNumberOfRecords());
            decodedResponse.setHasMoreRecords(response.getHasMoreRecords());
        } catch ( GPUdbUnauthorizedAccessException ex ) {
            // Any permission related problem should get propagated
            throw ex;
        } catch ( GPUdbException ex ) {
            boolean didFailoverSucceed = false;
            if ( (ex instanceof GPUdbExitException) || ex.hadConnectionFailure() ) {
                GPUdbLogger.warn( "Caught EXIT exception or had other connection failure: " + ex.getMessage() );

                // We did encounter an HA failover trigger
                // Switch to a different, healthy cluster in the HA ring, if any
                try {
                    // Switch to a different, healthy cluster in the HA ring, if any
                    forceFailover( currURL, currentCountClusterSwitches );
                    didFailoverSucceed = true;
                } catch (GPUdbException ex2) {
                    // We've now tried all the HA clusters and circled back;
                    // propagate the error to the user
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause + "; " + ex2.getMessage(), true );
                }
            } else {
                // For debugging purposes only (can be very useful!)
                GPUdbLogger.debug_with_info( "Caught GPUdbException: " + ex.getMessage() );
            }
            GPUdbLogger.debug_with_info( "Did failover succeed? " + didFailoverSucceed );

            // Update the worker queues since we've failed over to a
            // different cluster
            GPUdbLogger.debug_with_info( "Updating worker queues" );
            boolean updatedWorkerQueues = updateWorkerQueues( currentCountClusterSwitches );
            GPUdbLogger.debug_with_info( "Did we update the worker queue? " + updatedWorkerQueues );
            boolean retry = false;
            synchronized ( this.shardUpdateTime ) {
                retry = ( didFailoverSucceed
                          || updatedWorkerQueues
                          || ( retrievalAttemptTimestamp < this.shardUpdateTime.longValue() ) );
            }
            GPUdbLogger.debug_with_info( "'retry' value: " + retry );
            if ( retry ) {
                // We need to try fetching the records again
                try {
                    // Don't use the modified expression;use the original one
                    return this.getByKey( keyValues, expression );
                } catch (Exception ex2) {
                    // Re-setting the exception since we may re-try again
                    throw new GPUdbException( ex2.getMessage() );
                }
            }
            throw new GPUdbException( ex.getMessage() );
        } catch (Exception ex) {
            GPUdbLogger.debug_with_info( "Caught java exception: " + ex.getMessage() );
            // Retrieval failed, but maybe due to shard mapping changes (due to
            // cluster reconfiguration)? Check if the mapping needs to be updated
            // or has been updated by another thread already after the
            // insertion was attempted
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
