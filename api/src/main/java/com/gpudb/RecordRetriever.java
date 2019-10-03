package com.gpudb;

import com.gpudb.GPUdbBase.GPUdbExitException;
import com.gpudb.protocol.AdminShowShardsRequest;
import com.gpudb.protocol.AdminShowShardsResponse;
import com.gpudb.protocol.GetRecordsRequest;
import com.gpudb.protocol.GetRecordsResponse;
import com.gpudb.protocol.RawGetRecordsResponse;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.mutable.MutableLong;

/**
 * Object that permits efficient retrieval of records from GPUdb, with support
 * for multi-head access. {@code RecordRetriever} instances are thread safe and
 * may be used from any number of threads simultaneously.
 *
 * @param <T>  the type of object being retrieved
 */
public class RecordRetriever<T> {
    private final GPUdb gpudb;
    private final String tableName;
    private final Type type;
    private final TypeObjectMap<T> typeObjectMap;
    private final RecordKeyBuilder<T> shardKeyBuilder;
    private final boolean isMultiHeadEnabled;
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
                            Map<String, String> options ) throws GPUdbException {
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
    public RecordRetriever(GPUdb gpudb, String tableName, Type type, WorkerList workers) throws GPUdbException {
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
                            TypeObjectMap<T> typeObjectMap) throws GPUdbException {
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
                            Map<String, String> options ) throws GPUdbException {
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
        // We will always need to use this for getByKey
        this.options.put(GetRecordsRequest.Options.FAST_INDEX_LOOKUP, GetRecordsRequest.Options.TRUE);
        
        // Keep track of how many times the db client has switched HA clusters
        // in order to decide later if it's time to update the worker queues
        this.numClusterSwitches = gpudb.getNumClusterSwitches();

        // Keep track of which cluster we're using (helpful in knowing if an
        // HA failover has happened)
        this.currentHeadNodeURL = gpudb.getURL();
        
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

        if (shardKeyBuilderTemp.hasKey()) {
            shardKeyBuilder = shardKeyBuilderTemp;
        } else {
            shardKeyBuilder = null;
        }

        this.workerUrls = new ArrayList<>();

        // Set if multi-head I/O is turned on at the server
        this.isMultiHeadEnabled = ( this.workerList != null && !this.workerList.isEmpty() );

        if ( this.isMultiHeadEnabled ) {
        // if (workers != null && !workers.isEmpty()) {
            try {
                for (URL url : workers) {
                    if (url == null) {
                        // Handle removed ranks
                        this.workerUrls.add( null );
                        // this.workerUrls.add( (URL)null );
                    } else { // add a URL for an active rank
                        this.workerUrls.add(GPUdbBase.appendPathToURL(url, "/get/records"));
                    }
                }
            } catch (MalformedURLException ex) {
                throw new GPUdbException(ex.getMessage(), ex);
            }

            // Update the worker queues, if needed
            updateWorkerQueues( false );
            // routingTable = gpudb.adminShowShards(new AdminShowShardsRequest()).getRank();

            this.numRanks = this.workerUrls.size();

            // for (int i = 0; i < routingTable.size(); i++) {
            //     if (routingTable.get(i) > workers.size()) {
            //         throw new IllegalArgumentException("Too few worker URLs specified.");
            //     }
            // }
        } else {
            routingTable = null;
            this.numRanks = 1;
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
    private void forceHAFailover() throws GPUdbException {
        while (true) {
            // Try to switch to a new cluster
            try {
                // this.gpudb.switchURL( this.currentHeadNodeURL );
                synchronized ( this.currentHeadNodeURL ) {
                    this.gpudb.switchURL( this.currentHeadNodeURL );
                }
            } catch (GPUdbBase.GPUdbHAUnavailableException ex ) {
                // Have tried all clusters; back to square 1
                throw ex;
            }

            
            // We did switch to a different cluster; now check the health
            // of the cluster, starting with the head node
            if ( !this.gpudb.isKineticaRunning( this.gpudb.getURL() ) ) {
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
                synchronized ( this.currentHeadNodeURL ) {
                    this.currentHeadNodeURL = this.gpudb.getURL();
                }
                return;
            }
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
     * Updates the shard mapping based on the latest cluster configuration.
     * Optionally, also reconstructs the worker queues based on the new sharding.
     *
     * @param doReconstructWorkerQueues  Boolean flag indicating if the worker
     *                                   queues ought to be re-built.
     *
     * @return  a bool indicating whether the shard mapping was updated or not.
     */
    private boolean updateWorkerQueues( boolean doReconstructWorkerURLs ) throws GPUdbException {
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
        
            // Save the new shard version and also when we're updating the mapping
            this.shardVersion = newShardVersion;

            synchronized ( this.shardUpdateTime ) {
                this.shardUpdateTime.setValue( new Timestamp( System.currentTimeMillis() ).getTime() );
            }

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
    private void reconstructWorkerURLs() throws GPUdbException {

        // Get the latest worker list (use whatever IP regex was used initially)
        if ( this.workerList == null )
            throw new GPUdbException( "No worker list exists!" );
        com.gpudb.WorkerList newWorkerList = new com.gpudb.WorkerList( this.gpudb,
                                                                       this.workerList.getIpRegex() );
        if (newWorkerList == this.workerList) {
            return; // nothing to do since the worker list did not change
        }

        // Update the worker list
        synchronized ( this.workerList ) {
            this.workerList = newWorkerList;
        }

        // Create worker queues per worker URL
        List<URL> newWorkerUrls = new ArrayList<>();
        for ( URL url : this.workerList) {
            try {
                // Handle removed ranks
                if (url == null) {
                    newWorkerUrls.add( null );
                    // newWorkerUrls.add( (URL)null );
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
        synchronized ( this.workerUrls ) {
            this.workerUrls = newWorkerUrls;
        }
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
     * Gets the options currently used for the retriever methods.  Note
     * that any {@link GetRecordsRequest.Options.EXPRESSION} options will
     * get overridden at the next {@link getByKey} call with the appropriate
     * expression.
     *
     * @return  the options used during record retrieval
     *
     * @see setOptions
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
     * @see com.gpudb.protocol.GetRecordsRequest.Options.EXPRESSION
     */
    public RecordRetriever setOptions( Map<String, String> options ) {
        if (options != null) {
            this.options = new HashMap<>(options);
        } else {
            // We'll need to use at least the 'expressions' in the options
            this.options = new HashMap<>();
        }
        // We will always need to use this for getByKey
        this.options.put(GetRecordsRequest.Options.FAST_INDEX_LOOKUP, GetRecordsRequest.Options.TRUE);

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
     *
     * @param keyValues   list of values that make up the shard key; the values
     *                    must be in the same order as in the table and have the
     *                    correct data types
     * @param expression  additional filter to be applied, or {@code null} for
     *                    none
     * @return            {@link com.gpudb.protocol.GetRecordsResponse response}
     *                    object containing the results
     *
     * @throws GPUdbException if an error occurs during the operation
     */
    public GetRecordsResponse<T> getByKey(List<Object> keyValues, String expression) throws GPUdbException {
        if (shardKeyBuilder == null) {
            throw new IllegalStateException("Cannot get by key from unsharded table.");
        }

        if (expression == null || expression.isEmpty()) {
            expression = shardKeyBuilder.buildExpression(keyValues);
        } else {
            expression = "(" + shardKeyBuilder.buildExpression(keyValues) + ") and (" + expression + ")";
        }

        // Update the options with the correct expression
        this.options.put(GetRecordsRequest.Options.EXPRESSION, expression);
        // // Currently being set in the private constructor
        // this.options.put(GetRecordsRequest.Options.FAST_INDEX_LOOKUP, GetRecordsRequest.Options.TRUE);

        GetRecordsRequest request = new GetRecordsRequest(tableName, 0, GPUdb.END_OF_SET, this.options);
        RawGetRecordsResponse response = new RawGetRecordsResponse();

        GetRecordsResponse<T> decodedResponse = new GetRecordsResponse<>();

        long retrievalAttemptTimestamp = new Timestamp( System.currentTimeMillis() ).getTime();

        try {
            if ( this.isMultiHeadEnabled && (routingTable != null) ) {
                // Get from the worker rank
                RecordKey shardKey;
                try {
                    shardKey = shardKeyBuilder.build(keyValues);
                } catch (GPUdbException ex) {
                    throw new GPUdbException( "Unable to calculate the shard value; please check data for unshardable values");
                }
                    
                URL url = workerUrls.get(shardKey.route(routingTable));
                response = gpudb.submitRequest(url, request, response, false);
            } else {
                // Get from the head node
                response = gpudb.submitRequest("/get/records", request, response, false);
            }

            // Check if shard re-balancing is under way at the server; if so,
            // we need to update the shard mapping
            if ( response.getInfo().get( "data_rerouted" ) == "true" ) {
                updateWorkerQueues();
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
                    forceHAFailover();
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
            boolean updatedWorkerQueues = updateWorkerQueues();
            synchronized ( this.shardUpdateTime ) {
                if ( updatedWorkerQueues
                     || ( retrievalAttemptTimestamp < this.shardUpdateTime.longValue() ) ) {

                    // We need to try fetching the records again
                    try {
                        GetRecordsResponse<T> records = this.getByKey( keyValues, expression );
                        return records;
                    } catch (Exception ex2) {
                        // Re-setting the exception since we may re-try again
                        throw new GPUdbException( ex2.getMessage() );
                    }
                }
            }
            throw new GPUdbException( ex.getMessage() );
        } catch (Exception ex) {
            // Retrieval failed, but maybe due to shard mapping changes (due to
            // cluster reconfiguration)? Check if the mapping needs to be updated
            // or has been updated by another thread already after the
            // insertion was attemtped
            boolean updatedWorkerQueues = updateWorkerQueues();
            synchronized ( this.shardUpdateTime ) {
                if ( updatedWorkerQueues
                     || ( retrievalAttemptTimestamp < this.shardUpdateTime.longValue() ) ) {

                    // We need to try fetching the records again
                    try {
                        GetRecordsResponse<T> records = this.getByKey( keyValues, expression );
                        return records;
                    } catch (Exception ex2) {
                        // Re-setting the exception since we may re-try again
                        throw new GPUdbException( ex2.getMessage() );
                    }
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
