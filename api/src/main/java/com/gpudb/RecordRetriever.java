package com.gpudb;

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
    private int numRanks;
    private long shardVersion;
    private MutableLong shardUpdateTime;
    private int numClusterSwitches;
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
        this(gpudb, tableName, type, null, null);
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
        this(gpudb, tableName, type, null, workers);
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
    public RecordRetriever(GPUdb gpudb, String tableName, TypeObjectMap<T> typeObjectMap) throws GPUdbException {
        this(gpudb, tableName, typeObjectMap.getType(), typeObjectMap, null);
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
    public RecordRetriever(GPUdb gpudb, String tableName, TypeObjectMap<T> typeObjectMap, WorkerList workers) throws GPUdbException {
        this(gpudb, tableName, typeObjectMap.getType(), typeObjectMap, workers);
    }

    private RecordRetriever(GPUdb gpudb, String tableName, Type type, TypeObjectMap<T> typeObjectMap, WorkerList workers) throws GPUdbException {
        this.gpudb = gpudb;
        this.tableName = tableName;
        this.type = type;
        this.typeObjectMap = typeObjectMap;
        this.workerList    = workers;

        this.shardVersion = 0;
        this.shardUpdateTime = new MutableLong();

        // Keep track of how many times the db client has switched HA clusters
        // in order to decide later if it's time to update the worker queues
        this.numClusterSwitches = gpudb.getNumClusterSwitches();

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

        Map<String, String> options = new HashMap<>();
        options.put(GetRecordsRequest.Options.EXPRESSION, expression);
        options.put(GetRecordsRequest.Options.FAST_INDEX_LOOKUP, GetRecordsRequest.Options.TRUE);
        GetRecordsRequest request = new GetRecordsRequest(tableName, 0, GPUdb.END_OF_SET, options);
        RawGetRecordsResponse response = new RawGetRecordsResponse();

        GetRecordsResponse<T> decodedResponse = new GetRecordsResponse<>();

        long retrievalAttemptTimestamp = new Timestamp( System.currentTimeMillis() ).getTime();

        try {
            if ( this.isMultiHeadEnabled && (routingTable != null) ) {
                // Get from the worker rank
                RecordKey shardKey = shardKeyBuilder.build(keyValues);
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
                        return this.getByKey( keyValues, expression );
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

}  // class RecordRetriever
