package com.gpudb;

import com.gpudb.GPUdbBase.GPUdbExitException;
import com.gpudb.GPUdbBase.GPUdbUnauthorizedAccessException;
import com.gpudb.protocol.AdminShowShardsRequest;
import com.gpudb.protocol.AdminShowShardsResponse;
import com.gpudb.protocol.GetRecordsByColumnRequest;
import com.gpudb.protocol.GetRecordsByColumnResponse;
import com.gpudb.protocol.GetRecordsRequest;
import com.gpudb.protocol.GetRecordsResponse;
import com.gpudb.protocol.RawGetRecordsByColumnResponse;
import com.gpudb.protocol.RawGetRecordsResponse;
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
import org.apache.commons.lang3.mutable.MutableLong;

/**
 * Object that permits efficient retrieval of records from GPUdb, with support
 * for multi-head access. {@code RecordRetriever} instances are thread safe and
 * may be used from any number of threads simultaneously.
 *
 * @param <T>  the type of object being retrieved
 */
public class RecordRetriever<T> {

    // Table members
    private final GPUdb gpudb;
    private final String tableName;
    private final Type type;
    private final TypeObjectMap<T> typeObjectMap;
    private volatile Map<String,String> options;
    private boolean tableReplicated;

    /**
     * The multi-head config the worker list was last built from, compared by
     * <b>identity</b> to detect that the connection's addresses have been
     * replaced -- by a move to another cluster, or by a fresh probe of the one
     * it is already on.
     */
    private GPUdbBase.MultiHeadSnapshot lastMultiHeadSnapshot;



    // Sharding members
    private com.gpudb.WorkerList workerList;

    /**
     * Where lookups get routed, published as a single immutable value.
     *
     * <p>The destination of a lookup depends on several things that must agree:
     * whether multi-head is usable at all, the worker URL per rank, which of
     * those slots hold a live worker, and the shard mapping.  Holding them in
     * one immutable object behind one {@code volatile} reference means a
     * rebuild cannot be observed part way through: a reader sees either the
     * whole old state or the whole new one, and gets the happens-before edge
     * that makes what it finds safe to use.
     *
     * <p>Every read on the lookup path must take this reference <i>once</i>
     * into a local and use that local throughout.  Re-reading the field
     * mid-decision reintroduces exactly the inconsistency the snapshot exists
     * to prevent.
     *
     * <p>Mirrors {@code BulkInserter.routing}; see {@link Routing} for where
     * the two intentionally differ.
     */
    private volatile Routing routing;

    /**
     * The shard mapping most recently fetched from the server, which may not
     * have been published yet.  Only ever touched while holding this object's
     * monitor, and never read on the lookup path.
     */
    private List<Integer> pendingRoutingTable;
    private final RecordKeyBuilder<T> shardKeyBuilder;
    private long shardVersion;
    private MutableLong shardUpdateTime;
    
    // HA members
    private int numClusterSwitches;
    private URL currentHeadNodeURL;
    private URL lastUsedUrl;
    private final Object haFailoverLock;


    /**
     * Creates a {@link RecordRetriever} with the specified parameters.
     * <br/>
     * It will use default settings for the
     * {@link GPUdb#getRecords(String, long, long, Map)} call supporting
     * {@link #getByKey(List, String)} and the
     * {@link GPUdb#getRecordsByColumn(String, List, long, long, Map)} call
     * supporting {@link #getColumnsByKey(List, List, String)}.
     * <br/>
     * Details can be found at
     * {@link com.gpudb.protocol.GetRecordsRequest.Options} and
     * {@link com.gpudb.protocol.GetRecordsByColumnRequest.Options},
     * respectively.
     *
     * @param gpudb      the {@link GPUdb} instance to retrieve records from
     * @param tableName  the table to retrieve records from
     * @param type       the {@link Type} of records being retrieved
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
     * @param gpudb      the {@link GPUdb} instance to retrieve records from
     * @param tableName  the table to retrieve records from
     * @param type       the {@link Type} of records being retrieved
     * @param options    optional parameters to pass to GPUdb while retrieving
     *                   ({@code null} for no parameters)
     *                   <br/>
     *                   This is the same set of options as accepted by the
     *                   {@link GPUdb#getRecords(String, long, long, Map)} and
     *                   {@link GPUdb#getRecordsByColumn(String, List, long, long, Map)}
     *                   calls.
     *                   <br/>
     *                   The details can be found at
     *                   {@link com.gpudb.protocol.GetRecordsRequest.Options} and
     *                   {@link com.gpudb.protocol.GetRecordsByColumnRequest.Options}.
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
     * @param gpudb      the {@link GPUdb} instance to retrieve records from
     * @param tableName  the table to retrieve records from
     * @param type       the {@link Type} of records being retrieved
     * @param workers    worker list for multi-head retrieval; pass an empty
     *                   list ({@code new WorkerList()}) to disable multi-head
     *                   for this retriever, which leaves the connection's
     *                   fail-over intact and is not undone by a later rebuild.
     *                   Passing {@code null} does <i>not</i> disable it -- the
     *                   list is then derived from the connection's own worker
     *                   addresses, which is the default behavior
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
     * @param gpudb      the {@link GPUdb} instance to retrieve records from
     * @param tableName  the table to retrieve records from
     * @param type       the {@link Type} of records being retrieved
     * @param workers    worker list for multi-head retrieval; pass an empty
     *                   list ({@code new WorkerList()}) to disable multi-head
     *                   for this retriever, which leaves the connection's
     *                   fail-over intact and is not undone by a later rebuild.
     *                   Passing {@code null} does <i>not</i> disable it -- the
     *                   list is then derived from the connection's own worker
     *                   addresses, which is the default behavior
     * @param options    optional parameters to pass to GPUdb while retrieving
     *                   ({@code null} for no parameters)
     *                   <br/>
     *                   This is the same set of options as accepted by the
     *                   {@link GPUdb#getRecords(String, long, long, Map)} and
     *                   {@link GPUdb#getRecordsByColumn(String, List, long, long, Map)}
     *                   calls.
     *                   <br/>
     *                   The details can be found at
     *                   {@link com.gpudb.protocol.GetRecordsRequest.Options} and
     *                   {@link com.gpudb.protocol.GetRecordsByColumnRequest.Options}.
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
     * @param gpudb          the {@link GPUdb} instance to retrieve records from
     * @param tableName      the table to retrieve records from
     * @param typeObjectMap  the {@link TypeObjectMap} for the type of records
     *                       being retrieved
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
     * @param gpudb          the {@link GPUdb} instance to retrieve records from
     * @param tableName      the table to retrieve records from
     * @param typeObjectMap  the {@link TypeObjectMap} for the type of records
     *                       being retrieved
     * @param options        optional parameters to pass to GPUdb while retrieving
     *                       ({@code null} for no parameters)
     *                       <br/>
     *                       This is the same set of options as accepted by the
     *                       {@link GPUdb#getRecords(String, long, long, Map)} and
     *                       {@link GPUdb#getRecordsByColumn(String, List, long, long, Map)}
     *                       calls.
     *                       <br/>
     *                       The details can be found at
     *                       {@link com.gpudb.protocol.GetRecordsRequest.Options} and
     *                       {@link com.gpudb.protocol.GetRecordsByColumnRequest.Options}.
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
     * @param gpudb          the {@link GPUdb} instance to retrieve records from
     * @param tableName      the table to retrieve records from
     * @param typeObjectMap  the {@link TypeObjectMap} for the type of records
     *                       being retrieved
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
     * @param gpudb          the {@link GPUdb} instance to retrieve records from
     * @param tableName      the table to retrieve records from
     * @param typeObjectMap  the {@link TypeObjectMap} for the type of records
     *                       being retrieved
     * @param workers        worker list for multi-head retrieval ({@code null}
     *                       to disable multi-head retrieval)
     * @param options        optional parameters to pass to GPUdb while retrieving
     *                       ({@code null} for no parameters)
     *                       <br/>
     *                       This is the same set of options as accepted by the
     *                       {@link GPUdb#getRecords(String, long, long, Map)} and
     *                       {@link GPUdb#getRecordsByColumn(String, List, long, long, Map)}
     *                       calls.
     *                       <br/>
     *                       The details can be found at
     *                       {@link com.gpudb.protocol.GetRecordsRequest.Options} and
     *                       {@link com.gpudb.protocol.GetRecordsByColumnRequest.Options}.
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


    /**
     * Creates a {@link RecordRetriever} with the specified parameters.
     *
     * @param gpudb          the {@link GPUdb} instance to retrieve records from
     * @param tableName      the table to retrieve records from
     * @param type           the {@link Type} of records being retrieved
     * @param typeObjectMap  the {@link TypeObjectMap} for the type of records
     *                       being retrieved
     * @param workers        worker list for multi-head retrieval ({@code null}
     *                       to disable multi-head retrieval)
     * @param options        optional parameters to pass to GPUdb while retrieving
     *                       ({@code null} for no parameters)
     *                       <br/>
     *                       This is the same set of options as accepted by the
     *                       {@link GPUdb#getRecords(String, long, long, Map)} and
     *                       {@link GPUdb#getRecordsByColumn(String, List, long, long, Map)}
     *                       calls.
     *                       <br/>
     *                       The details can be found at
     *                       {@link com.gpudb.protocol.GetRecordsRequest.Options} and
     *                       {@link com.gpudb.protocol.GetRecordsByColumnRequest.Options}.
     *
     * @throws GPUdbException if a configuration error occurs
     *
     * @throws IllegalArgumentException if an invalid parameter is specified
     */
    private RecordRetriever( GPUdb gpudb,
                             String tableName,
                             Type type,
                             TypeObjectMap<T> typeObjectMap,
                             WorkerList workers,
                             Map<String, String> options ) throws GPUdbException {

        this.haFailoverLock = new Object();

        this.gpudb = gpudb;
        this.tableName = tableName;
        this.type = type;
        this.typeObjectMap = typeObjectMap;
        this.workerList    = workers;

        if (options != null) {
            this.options = new HashMap<>(options);
        } else {
            // We'll need to use at least the 'expressions' in the options
            this.options = new HashMap<>();
        }

        // Initialize the shard version and update time
        this.shardVersion = 0;
        this.shardUpdateTime = new MutableLong();

        // Keep track of how many times the DB client has switched HA clusters
        // in order to decide later if it's time to update the worker queues
        this.numClusterSwitches = gpudb.getNumClusterSwitches();

        // Keep track of which cluster we're using (helpful in knowing if an
        // HA failover has happened)
        this.currentHeadNodeURL = gpudb.getURL();

        // Check if the table is replicated or not; in case we can't figure it
        // out, we will pretend it is not
        try {
            // Check whether 'replicated' is in one of the response fields
            this.tableReplicated = this.gpudb.showTable( this.tableName, null )
                .getTableDescriptions()
                .get(0)
                .contains( ShowTableResponse.TableDescriptions.REPLICATED );
        } catch ( GPUdbException ex ) {
            // Ignore any issue and carry on; it's OK if the table does not
            // exist yet.  Who knows when the user would instantiate this
            // object--quite possibly before creating the table.  So no worries.
        }

        // If no worker list is given, use the rank URLs the connection has
        // already resolved, which is the route BulkInserter takes.
        //
        // Those URLs were filtered through the user's hostname regex when the
        // cluster was discovered (GPUdbBase.getRankURLs), and they already carry
        // the null placeholders for ranks removed from the cluster.  Building
        // from them therefore honors setHostnameRegex -- and it avoids a
        // second /show/system/properties round trip that could itself fail.
        if (this.workerList == null) {
            this.workerList = new WorkerList( this.gpudb.getCurrentMultiHeadSnapshot() );

            if (this.workerList.isEmpty())
                GPUdbLogger.info("No worker rank URLs available for record retrieval; using head node instead.");
        }

        // Multi-head lookups are usable if they are turned on at the server and
        // the rank URLs are reachable from this client
        boolean isMultiHeadEnabled = ( (this.workerList != null) && !this.workerList.isEmpty() );

        this.shardKeyBuilder = new RecordKeyBuilder<>(type, typeObjectMap);

        List<URL> workerUrls = new ArrayList<>();

        if ( isMultiHeadEnabled ) {
            try {
                for (URL url : this.workerList) {
                    if (url == null) {
                        // Handle removed ranks
                        workerUrls.add( null );
                    } else { // add a URL for an active rank
                        workerUrls.add(GPUdbBase.appendPathToURL(url, "/get/records"));
                    }
                }
            } catch (MalformedURLException ex) {
                throw new GPUdbException(ex.getMessage(), ex);
            }
        }

        // Remember the initial multi-head state in order to detect changes to
        // the multi-head state after failover/failback/rebalance.
        this.lastMultiHeadSnapshot = this.gpudb.getCurrentMultiHeadSnapshot();

        // Publish the initial routing state as one value, before anything can
        // read it.  The shard mapping is not known yet; it is fetched below and
        // published as a snapshot of its own.  The live-worker indices are
        // derived inside Routing from the URL list, so the two cannot disagree.
        this.routing = new Routing( isMultiHeadEnabled, workerUrls, null );

        if ( isMultiHeadEnabled ) {
            // Fetch the shard mapping that goes with these URLs
            updateWorkerQueues( false );
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
        this.setCurrentClusterSwitchCount( this.gpudb.getNumClusterSwitches() );
    }   // end forceFailover


    /**
     * Updates the shard mapping based on the latest cluster configuration.
     * Also reconstructs the worker queues based on the new sharding.
     *
     * @return  whether the shard mapping was updated or not.
     */
    private boolean updateWorkerQueues() throws GPUdbException {
        return this.updateWorkerQueues( true );
    }


    /**
     * Updates the shard mapping based on the latest cluster configuration.
     * Optionally, also reconstructs the worker queues based on the new sharding.
     *
     * @param doReconstructWorkerURLs  Boolean flag indicating if the worker
     *                                   queues ought to be re-built.
     *
     * @return  a boolean indicating whether the shard mapping was updated.
     */
    private synchronized boolean updateWorkerQueues( boolean doReconstructWorkerURLs ) throws GPUdbException {
        return updateWorkerQueues( doReconstructWorkerURLs, true );
    }


    /**
     * Updates the shard mapping and, optionally, reconstructs the worker rank
     * URLs.
     *
     * @param doReconstructWorkerURLs  whether the worker URLs should be rebuilt
     * @param publishShardMapping  whether a newly fetched shard mapping should
     *                             be published on its own.  A caller about to
     *                             publish a complete routing state of its own
     *                             -- {@link #reconstructWorkerURLs()} -- passes
     *                             {@code false} and takes the mapping from
     *                             {@link #pendingRoutingTable}, so a new mapping
     *                             is never published alongside URLs it does not
     *                             describe.
     *
     * @return  whether the shard mapping was updated.
     */
    private synchronized boolean updateWorkerQueues( boolean doReconstructWorkerURLs,
                                                     boolean publishShardMapping ) throws GPUdbException {

        // Flag for if the worker rank URLs need to be re-constructed when asked
        // for iff multi-head i/o is enabled and the caller asked for it.
        boolean reconstructWorkerURLS = ( doReconstructWorkerURLs
                                          && this.routing.multiHeadEnabled );
        GPUdbLogger.debug_with_info( "Reconstruct worker URLs?: "
                                     + reconstructWorkerURLS );

        // Whether the shard mapping has changed since the last snapshot.
        boolean shardMappingChanged = false;

        try {
            // Get the latest shard mapping information; note that this endpoint
            // call might trigger an HA failover in the GPUdb object
            AdminShowShardsResponse shardInfo = this.gpudb.adminShowShards(new AdminShowShardsRequest());

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

                    if ( reconstructWorkerURLS )
                    {
                        // The caller needs to know if we ended up updating the
                        // worker rank URLs
                        return reconstructWorkerURLs( false );
                    }

                    // Not appropriate to update worker URLs; then no change
                    // has happened
                    GPUdbLogger.debug_with_info( "Returning false" );
                    return false;
                }

                // Record the cluster now current, so the next call compares
                // against it rather than against the one left behind.
                this.lastMultiHeadSnapshot = currSnapshot;
                this.setCurrentClusterSwitchCount( this.gpudb.getNumClusterSwitches() );
            }

            // Save the new shard version and also when we're updating the mapping
            this.shardVersion = newShardVersion;

            this.shardUpdateTime.setValue( new Timestamp( System.currentTimeMillis() ).getTime() );

            // Record the newly fetched shard mapping.  It is published here only
            // when the caller is not about to publish a routing state of its
            // own; see the publishShardMapping parameter.
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
        this.setCurrentClusterSwitchCount( this.gpudb.getNumClusterSwitches() );

        // The worker queues need to be re-constructed when asked for
        // iff multi-head i/o is enabled and the table is not replicated
        if ( reconstructWorkerURLS )
        {
            reconstructWorkerURLs( shardMappingChanged );
        }

        GPUdbLogger.debug_with_info( "Returning true" );
        return true; // the shard mapping was updated indeed
    }  // end updateWorkerQueues


    /**
     * Reconstructs the list of worker URLs.
     *
     * @param topologyMayHaveMoved  what this rebuild observed, not what it wants
     *                              done: {@code true} where the server reported
     *                              a shard mapping change, which can move rank
     *                              addresses without moving the connection, so
     *                              the connection re-acquires before answering;
     *                              {@code false} after a cluster change, which
     *                              the switch itself already probed
     *
     * @return  whether we ended up reconstructing the worker URLs or not
     */
    private synchronized boolean reconstructWorkerURLs( boolean topologyMayHaveMoved )
            throws GPUdbException {

        if ( this.workerList == null )
            throw new GPUdbException( "No worker list exists!" );

        if ( this.workerList.disablesMultiHead() ) {
            GPUdbLogger.debug_with_info( "Worker list declines multi-head; not rebuilding" );
            return false;
        }


        // Ask the connection for the current cluster's addresses.
        GPUdbBase.MultiHeadSnapshot snapshot =
                this.gpudb.acquireMultiHeadSnapshot( topologyMayHaveMoved );


        // Adopt the addresses of whichever cluster the connection is on now;
        // see the matching note in BulkInserter.reconstructWorkerQueues().
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

        // Recompute whether multi-head is still usable.
        boolean isMultiHeadEnabled = ( (this.workerList != null) && !this.workerList.isEmpty() );

        // Create a URL per worker rank
        List<URL> workerUrls = new ArrayList<>();
        for ( URL url : this.workerList) {
            try {
                // Handle removed ranks
                if (url == null) {
                    workerUrls.add( null );
                }
                else {
                    // Add a queue for a currently active rank
                    workerUrls.add( GPUdbBase.appendPathToURL(url, "/get/records") );
                }
            } catch (MalformedURLException ex) {
                throw new GPUdbException( ex.getMessage(), ex );
            } catch (Exception ex) {
                throw new GPUdbException( ex.getMessage(), ex );
            }
        }

        // Refresh the shard mapping for the new URL set without letting it be
        // published on its own, so it is never paired with the URLs still in
        // place.  The fetch is a no-op when the shard version has not moved, in
        // which case it leaves pendingRoutingTable alone; clearing it first is
        // what distinguishes "nothing new was fetched" from a mapping left over
        // from an earlier fetch.
        List<Integer> newRoutingTable = this.routing.routingTable;
        if ( isMultiHeadEnabled ) {
            this.pendingRoutingTable = null;
            updateWorkerQueues( false, false );
            if ( this.pendingRoutingTable != null )
                newRoutingTable = this.pendingRoutingTable;
        }

        // Publish the whole new routing state with one volatile write, so a
        // concurrent lookup sees either all of the old state or all of the new
        // one.  The live-worker indices are derived inside Routing from the URL
        // list it is given, so they cannot lag behind it.
        this.routing = new Routing( isMultiHeadEnabled, workerUrls, newRoutingTable );

        GPUdbLogger.debug_with_info( "Worker list was updated, returning true" );
        return true; // we did change the URLs!
    }  // end reconstructWorkerURLs


    /**
     * Returns the indices of the slots of the given worker URL list that hold
     * a live worker.
     *
     * A rank that has been removed from the cluster keeps its slot in the
     * worker list--as {@code null}--so that the worker indices produced by the
     * server's routing table stay aligned with the rank numbering.  Such a
     * slot holds no URL, so it must never be handed a lookup.
     *
     * @param workerUrls  the worker URL list, holes included
     *
     * @return the indices of the slots holding a live worker
     */
    private static List<Integer> computeLiveWorkerIndices( List<URL> workerUrls ) {
        List<Integer> liveIndices = new ArrayList<>();

        for ( int i = 0; i < workerUrls.size(); ++i ) {
            if ( workerUrls.get( i ) != null )
                liveIndices.add( i );
        }

        return liveIndices;
    }  // end computeLiveWorkerIndices


    /**
     * An immutable snapshot of where lookups get routed.
     *
     * <p>The counterpart of {@code BulkInserter.Routing}, with two deliberate
     * differences:
     *
     * <ul>
     *   <li>There is no {@code useHeadNode}.  {@code BulkInserter} sends a
     *       replicated table's records to the head node; retrieval instead
     *       picks any live worker, since every worker holds the whole table.
     *       So for retrieval "not using multi-head" is exactly
     *       {@code !multiHeadEnabled}, and a second flag could only disagree
     *       with the first.</li>
     *   <li>{@code liveWorkerIndices} is <i>derived</i> here rather than passed
     *       in and validated.  It holds indices <i>into</i> {@code workerUrls},
     *       so computing it from that list inside the constructor makes a
     *       mismatched pair unconstructible rather than merely rejected.</li>
     * </ul>
     */
    private static final class Routing {

        /** Whether multi-head lookups are usable for this retriever. */
        final boolean multiHeadEnabled;

        /**
         * The {@code /get/records} URL per rank; unmodifiable.  Entry {@code i}
         * is rank {@code i + 1}, and a rank removed from the cluster keeps its
         * slot as {@code null} so the indices stay aligned with the rank
         * numbering the shard routing table refers to.
         */
        final List<URL> workerUrls;

        /**
         * The indices of {@link #workerUrls} holding a live worker;
         * unmodifiable.  Derived from {@code workerUrls}, never supplied.
         */
        final List<Integer> liveWorkerIndices;

        /**
         * The shard-to-rank mapping; unmodifiable, and {@code null} when not
         * yet known.
         */
        final List<Integer> routingTable;

        Routing( boolean multiHeadEnabled, List<URL> workerUrls, List<Integer> routingTable ) {
            this.multiHeadEnabled = multiHeadEnabled;
            this.workerUrls = Collections.unmodifiableList(
                    new ArrayList<URL>( (workerUrls == null) ? new ArrayList<URL>() : workerUrls ) );
            this.liveWorkerIndices = Collections.unmodifiableList(
                    computeLiveWorkerIndices( this.workerUrls ) );
            this.routingTable = (routingTable == null)
                                ? null
                                : Collections.unmodifiableList( new ArrayList<Integer>( routingTable ) );
        }

        /**
         * Returns a copy carrying a different shard mapping.  The URLs are
         * unchanged, so the derived live-worker indices come out identical.
         */
        Routing withRoutingTable( List<Integer> newRoutingTable ) {
            return new Routing( this.multiHeadEnabled, this.workerUrls, newRoutingTable );
        }

        @Override
        public String toString() {
            return "Routing{multiHeadEnabled=" + this.multiHeadEnabled
                   + ", workerUrls=" + this.workerUrls.size()
                   + ", live=" + this.liveWorkerIndices.size()
                   + ", routingTable=" + ((this.routingTable == null)
                                          ? "null"
                                          : (this.routingTable.size() + " shards"))
                   + "}";
        }
    }   // end class Routing


    /**
     * Returns the URL of a randomly chosen <b>live</b> worker rank, for
     * lookups that are not routed by a shard key (i.e. on replicated tables,
     * where every live rank holds the whole table).
     *
     * @return the URL of a randomly chosen live worker rank
     *
     * @throws GPUdbException if no rank slot holds a live worker
     */
    private URL getRandomLiveWorkerUrl( Routing routing ) throws GPUdbException {
        if ( (routing.liveWorkerIndices == null) || routing.liveWorkerIndices.isEmpty() ) {
            throw new GPUdbException( "No live worker rank is available for "
                                      + "record retrieval; all "
                                      + routing.workerUrls.size()
                                      + " worker rank slot(s) are empty "
                                      + "(removed ranks)" );
        }

        int liveIndex = routing.liveWorkerIndices.get(
                ThreadLocalRandom.current().nextInt( routing.liveWorkerIndices.size() ) );

        return routing.workerUrls.get( liveIndex );
    }  // end getRandomLiveWorkerUrl


    /**
     * Returns the URL of the worker rank at the given index.
     *
     * Validates both ends of the contract: an index past the end of the worker
     * list, and an index naming the empty slot that a removed rank leaves
     * behind so that routing-table indices stay aligned with the rank
     * numbering.
     *
     * @param workerIndex  the index of the worker rank, as produced by the
     *                     shard routing table
     *
     * @return the URL of the worker rank at the given index
     *
     * @throws GPUdbException if the index does not name a live worker
     */
    private URL getLiveWorkerUrl( Routing routing, int workerIndex ) throws GPUdbException {
        if ( (workerIndex < 0) || (workerIndex >= routing.workerUrls.size()) ) {
            throw new GPUdbException( "Sharded worker index is out of bound: "
                                      + workerIndex + " (# worker ranks "
                                      + routing.workerUrls.size() + ")" );
        }

        URL url = routing.workerUrls.get( workerIndex );

        if ( url == null ) {
            throw new GPUdbException( "Worker rank with index " + workerIndex
                                      + " has been removed from the cluster; "
                                      + "it cannot serve records (# worker "
                                      + "ranks " + routing.workerUrls.size()
                                      + "); the shard mapping may need to be "
                                      + "updated" );
        }

        return url;
    }  // end getLiveWorkerUrl


    /**
     * Gets the GPUdb instance from which records will be retrieved.
     *
     * @return  the GPUdb instance from which records will be retrieved
     */
    public GPUdb getGPUdb() {
        return this.gpudb;
    }

    /**
     * Gets the name of the table from which records will be retrieved.
     *
     * @return  the name of the table from which records will be retrieved
     */
    public String getTableName() {
        return this.tableName;
    }

    /**
     * @return  whether this {@link RecordRetriever} object is using the
     *          head-rank to do a simple record fetching (not utilizing
     *          the server's key lookup feature) (true value), or using
     *          multi-head (the worker ranks) for key lookup (false value).
     */
    public boolean isUsingHeadRank() {
        return !this.routing.multiHeadEnabled;
    }

    /**
     * @return  whether this {@link RecordRetriever} object is using the worker
     *          ranks to do key lookups (true value), or doing simple record
     *          fetching (not the server's key lookup feature) (false value).
     *          Note that this will not reflect the non-worker lookup scenario
     *          where only an expression is supplied and the table is sharded.
     */
    public boolean isDoingWorkerLookup() {
        return this.routing.multiHeadEnabled;
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
        // The field is volatile and is only ever REPLACED, never mutated in
        // place, so the reference write publishes the map safely and no lock is
        // needed.
        {
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
     * <br/>
     * This method operates in four modes, depending on the parameters passed:
     * <ul>
     *   <li> keyValues only -
     *            attempts a direct-to-rank lookup for records
     *            matching the given key values
     *   </li>
     *   <li> keyValues and expression -
     *            attempts a direct-to-rank lookup for records matching the
     *            given key values, filtering them by the given expression
     *   </li>
     *   <li> expression only -
     *            requests, via the head rank, all records in the table matching
     *            the given filter expression
     *   </li>
     *   <li> neither -
     *            retrieves all records from the table via the head rank
     *   </li>
     * </ul>
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
        return getByKey(keyValues, expression, 0);
    }

    /**
     * Retrieves records with the given key values and filter expression from
     * the database using a direct-to-rank fast key lookup, if possible, and
     * falling back to a standard lookup via the head node, if not.  Returns
     * records in the overall result set starting from the given {@code offset}.
     * <br/>
     * This method operates in four modes, depending on the parameters passed:
     * <ul>
     *   <li> keyValues only -
     *            attempts a direct-to-rank lookup for records
     *            matching the given key values
     *   </li>
     *   <li> keyValues and expression -
     *            attempts a direct-to-rank lookup for records matching the
     *            given key values, filtering them by the given expression
     *   </li>
     *   <li> expression only -
     *            requests, via the head rank, all records in the table matching
     *            the given filter expression
     *   </li>
     *   <li> neither -
     *            retrieves all records from the table via the head rank
     *   </li>
     * </ul>
     *
     * @param keyValues   the key values to use for the lookup; these must
     *                    correspond to either the explicit or implicit shard
     *                    key for sharded tables or the primary key of
     *                    replicated tables
     * @param expression  a filter expression that will be applied to the data
     *                    requested by the key values; if no key values are
     *                    specified this filter will be applied to all of the
     *                    data in the target table
     * @param offset      offset of the record(s) within the result set to
     *                    return
     *
     * @return            a {@link com.gpudb.protocol.GetRecordsResponse} with
     *                    the requested records
     */
    public GetRecordsResponse<T> getByKey(List<Object> keyValues, String expression,
        long offset) throws GPUdbException {

        // Take the routing state once and route this lookup entirely from that
        // snapshot.  A rebuild running concurrently publishes a new one; this
        // lookup then goes wherever the state it was routed against said, which
        // is consistent, rather than to a destination assembled from both.
        final Routing routing = this.routing;
        boolean doWorkerLookup = routing.multiHeadEnabled;
        String compositeExpression = expression;
        boolean keyValuesSpecified = keyValues != null && !keyValues.isEmpty();

        if (offset < 1)
            this.lastUsedUrl = null;

        if (!keyValuesSpecified) {
            // Use head rank if table is [randomly] sharded and no keys are given,
            //   or if the table is replicated and no expression (or keys) is given.
            if (!this.tableReplicated || expression == null || expression.isEmpty())
                doWorkerLookup = false;
        } else {
            // Eliminate the case where key values are given, but the table has no
            //   key columns with which to associate them
            if (!this.shardKeyBuilder.hasKey())
                throw new IllegalArgumentException(
                        "Cannot associate the specified keyValues with columns, " +
                        "as the table has no primary or shard key."
                );

            // Since we have a keyed table, build the composite expression to be
            //   used for both sharded-with-PK/SK and replicated-with-PK tables;
            //   randomly-sharded & replicated-without-PK will only use expression
            String keyExpression = this.shardKeyBuilder.buildExpression( keyValues );

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

        GetRecordsRequest request = new GetRecordsRequest(this.tableName, offset, GPUdbBase.END_OF_SET,
            retrievalOptions);
        RawGetRecordsResponse response = new RawGetRecordsResponse();
        GetRecordsResponse<T> decodedResponse = new GetRecordsResponse<>();
        
        long retrievalAttemptTimestamp = new Timestamp( System.currentTimeMillis() ).getTime();
        URL currURL = getCurrentHeadNodeURL();
        int currentCountClusterSwitches = getCurrentClusterSwitchCount();

        try {
            if (!doWorkerLookup) {
                // Get from the head node
                GPUdbLogger.debug_with_info( "Retrieving records from rank-0 with <" + compositeExpression + ">" );
                response = this.gpudb.submitRequest("/get/records", request, response, false);
            } else {
                // Get the record(s) from a worker rank; whether from a random
                // or a specific one depends on a few things
                URL url;

                // If the table is replicated and it's determined that the
                // server supports it, use random worker rank for lookups
                if ( this.tableReplicated ) {
                    // For replicated tables, use the same worker for new pages (i.e., offset > 0)
                    // in case the data is in a different order on different workers
                    if (this.lastUsedUrl != null)
                        url = this.lastUsedUrl;
                    else {
                        url = getRandomLiveWorkerUrl( routing );
                        this.lastUsedUrl = url; // Remember for next time
                    }
                } else {
                    // Not a replicated table; so calculate the shard to figure
                    // out which worker rank contains the requested records
                    RecordKey shardKey;
                    try {
                        shardKey = this.shardKeyBuilder.build( keyValues );
                    } catch (Exception ex) {
                        throw new GPUdbException( "Unable to calculate the shard value; please check data for unshardable values: " + ex.getMessage(), ex );
                    }

                    // Routing by key requires the shard mapping.  If the
                    // server could not supply one, surface it.
                    if ( (routing.routingTable == null) || routing.routingTable.isEmpty() ) {
                        throw new GPUdbException( "No shard mapping is available "
                                + "for table '" + this.tableName + "'; cannot route "
                                + "the lookup to a worker rank." );
                    }

                    url = getLiveWorkerUrl( routing, shardKey.route( routing.routingTable ) );
                }

                GPUdbLogger.debug_with_info( "Retrieving records from <" + url.toString() + "> with <" + compositeExpression + ">" );
                response = this.gpudb.submitRequest(url, request, response, false);
            }

            // Check if shard re-balancing is under way at the server; if so,
            // we need to update the shard mapping
            if ( "true".equals( response.getInfo().get( GPUdbBase.RESPONSE_INFO_DATA_REROUTED ) ) )
                updateWorkerQueues();

            // Set up the decoded response
            decodedResponse.setTableName(  response.getTableName()  );
            decodedResponse.setTypeName(   response.getTypeName()   );
            decodedResponse.setTypeSchema( response.getTypeSchema() );

            // Decode the actual response
            if (this.typeObjectMap == null)
                decodedResponse.setData( this.gpudb.<T>decode(this.type, response.getRecordsBinary()) );
            else
                decodedResponse.setData( this.gpudb.<T>decode(this.typeObjectMap, response.getRecordsBinary()) );

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
                    throw new GPUdbException( originalCause + "; " + ex2.getMessage(), ex, true );
                }
            } else {
                // For debugging purposes only (can be very useful!)
                GPUdbLogger.debug_with_info( "Caught GPUdbException: " + ex.getMessage() );
            }
            GPUdbLogger.debug_with_info( "Did failover succeed? " + didFailoverSucceed );

            // Update the worker queues since we've failed over to a
            // different cluster
            GPUdbLogger.debug_with_info( "Updating worker queues" );

            // A failure to rebuild must not displace the error we are
            // recovering from; record it and carry on to the retry decision
            // with the queues left unchanged.
            boolean updatedWorkerQueues = false;
            try {
                updatedWorkerQueues = updateWorkerQueues();
            } catch ( Exception rebuildEx ) {
                GPUdbLogger.warn( "Could not update the worker queues while recovering from <"
                                  + ex.getMessage() + ">: " + rebuildEx.getMessage() );
            }
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
                    // Keep the original failure.  It is the diagnosis; the
                    // retry's own failure is usually a consequence of it, and
                    // replacing it loses the only useful message.
                    throw new GPUdbException( ex.getMessage()
                                              + "; the retry after recovery also failed: "
                                              + ex2.getMessage(), ex );
                }
            }
            throw new GPUdbException( ex.getMessage(), ex );
        } catch (Exception ex) {
            GPUdbLogger.debug_with_info( "Caught java exception: " + ex.getMessage() );
            // Retrieval failed, but maybe due to shard mapping changes (due to
            // cluster reconfiguration)? Check if the mapping needs to be updated
            // or has been updated by another thread already after the
            // insertion was attempted
            // A failure to rebuild must not displace the error we are
            // recovering from; record it and carry on with the queues left
            // unchanged.
            boolean updatedWorkerQueues = false;
            try {
                updatedWorkerQueues = updateWorkerQueues();
            } catch ( Exception rebuildEx ) {
                GPUdbLogger.warn( "Could not update the worker queues while recovering from <"
                                  + ex.getMessage() + ">: " + rebuildEx.getMessage() );
            }

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
                    // Keep the original failure.  It is the diagnosis; the
                    // retry's own failure is usually a consequence of it, and
                    // replacing it loses the only useful message.
                    throw new GPUdbException( ex.getMessage()
                                              + "; the retry after recovery also failed: "
                                              + ex2.getMessage(), ex );
                }
            }
            throw new GPUdbException( ex.getMessage(), ex );
        }

        return decodedResponse;
    }  // getByKey()

    
    /**
     * Retrieves records with the given key values and filter expression from
     * the database using a direct-to-rank fast key lookup, if possible, and
     * falling back to a standard lookup via the head node, if not.
     * <br/>
     * This method operates in four modes, depending on the parameters passed:
     * <ul>
     *   <li> keyValues only -
     *            attempts a direct-to-rank lookup for records
     *            matching the given key values
     *   </li>
     *   <li> keyValues and expression -
     *            attempts a direct-to-rank lookup for records matching the
     *            given key values, filtering them by the given expression
     *   </li>
     *   <li> expression only -
     *            requests, via the head rank, all records in the table matching
     *            the given filter expression
     *   </li>
     *   <li> neither -
     *            retrieves all records from the table via the head rank
     *   </li>
     * </ul>
     *
     * @param columns     The requested columns (which can include expressions)
     *                    being requested.  May use "*" for all columns.
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
    public GetRecordsByColumnResponse getColumnsByKey(List<String> columns, List<Object> keyValues,
        String expression) throws GPUdbException {
        return getColumnsByKey(columns, keyValues, expression, 0);
    }

    /**
     * Retrieves records with the given key values and filter expression from
     * the database using a direct-to-rank fast key lookup, if possible, and
     * falling back to a standard lookup via the head node, if not.  Returns
     * records in the overall result set starting from the given {@code offset}.
     * <br/>
     * This method operates in four modes, depending on the parameters passed:
     * <ul>
     *   <li> keyValues only -
     *            attempts a direct-to-rank lookup for records
     *            matching the given key values
     *   </li>
     *   <li> keyValues and expression -
     *            attempts a direct-to-rank lookup for records matching the
     *            given key values, filtering them by the given expression
     *   </li>
     *   <li> expression only -
     *            requests, via the head rank, all records in the table matching
     *            the given filter expression
     *   </li>
     *   <li> neither -
     *            retrieves all records from the table via the head rank
     *   </li>
     * </ul>
     *
     * @param columns     The requested columns (which can include expressions)
     *                    being requested.  May use "*" for all columns.
     * @param keyValues   the key values to use for the lookup; these must
     *                    correspond to either the explicit or implicit shard
     *                    key for sharded tables or the primary key of
     *                    replicated tables
     * @param expression  a filter expression that will be applied to the data
     *                    requested by the key values; if no key values are
     *                    specified this filter will be applied to all of the
     *                    data in the target table
     * @param offset      offset of the record(s) within the result set to
     *                    return
     *
     * @return            a {@link com.gpudb.protocol.GetRecordsResponse} with
     *                    the requested records
     */
    public GetRecordsByColumnResponse getColumnsByKey(List<String> columns, List<Object> keyValues,
        String expression, long offset) throws GPUdbException {

        // Take the routing state once and route this lookup entirely from that
        // snapshot.  A rebuild running concurrently publishes a new one; this
        // lookup then goes wherever the state it was routed against said, which
        // is consistent, rather than to a destination assembled from both.
        final Routing routing = this.routing;
        boolean doWorkerLookup = routing.multiHeadEnabled;
        String compositeExpression = expression;
        boolean keyValuesSpecified = keyValues != null && !keyValues.isEmpty();

        if (offset < 1)
            this.lastUsedUrl = null;

        if (!keyValuesSpecified)
        {
            // Use head rank if table is [randomly] sharded and no keys are given,
            //   or if the table is replicated and no expression (or keys) is given.
            if (!this.tableReplicated)
                doWorkerLookup = false;
        } else {
            // Eliminate the case where key values are given, but the table has no
            //   key columns with which to associate them
            if (!this.shardKeyBuilder.hasKey())
                throw new IllegalArgumentException(
                        "Cannot associate the specified keyValues with columns, " +
                        "as the table has no primary or shard key."
                );

            // Since we have a keyed table, build the composite expression to be
            //   used for both sharded-with-PK/SK and replicated-with-PK tables;
            //   randomly-sharded & replicated-without-PK will only use expression
            String keyExpression = this.shardKeyBuilder.buildExpression( keyValues );

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
        }

        GetRecordsByColumnRequest request = new GetRecordsByColumnRequest(this.tableName, columns,
            offset, GPUdbBase.END_OF_SET, retrievalOptions);
        RawGetRecordsByColumnResponse response = new RawGetRecordsByColumnResponse();
        GetRecordsByColumnResponse decodedResponse = new GetRecordsByColumnResponse();

        long retrievalAttemptTimestamp = new Timestamp( System.currentTimeMillis() ).getTime();
        URL currURL = getCurrentHeadNodeURL();
        int currentCountClusterSwitches = getCurrentClusterSwitchCount();

        try {
            if (!doWorkerLookup) {
                // Get from the head node
                GPUdbLogger.debug_with_info( "Retrieving records from rank-0 with <" + compositeExpression + ">" );
                response = this.gpudb.submitRequest("/get/records/bycolumn", request, response, false);
            } else {
                // Get the record(s) from a worker rank; whether from a random
                // or a specific one depends on a few things
                URL url;

                // If the table is replicated and it's determined that the
                // server supports it, use random worker rank for lookups
                if ( this.tableReplicated )
                {
                    // For replicated tables, use the same worker for new pages (i.e., offset > 0)
                    // in case the data is in a different order on different workers
                    if (this.lastUsedUrl != null)
                        url = this.lastUsedUrl;
                    else {
                        url = getRandomLiveWorkerUrl( routing );
                        this.lastUsedUrl = url; // Remember for next time
                    }
                } else {
                    // Not a replicated table; so calculate the shard to figure
                    // out which worker rank contains the requested records
                    RecordKey shardKey;
                    try {
                        shardKey = this.shardKeyBuilder.build( keyValues );
                    } catch (Exception ex) {
                        throw new GPUdbException( "Unable to calculate the shard value; please check data for unshardable values: " + ex.getMessage(), ex );
                    }

                    // Routing by key requires the shard mapping.  If the
                    // server could not supply one, surface it.
                    if ( (routing.routingTable == null) || routing.routingTable.isEmpty() ) {
                        throw new GPUdbException( "No shard mapping is available "
                                + "for table '" + this.tableName + "'; cannot route "
                                + "the lookup to a worker rank." );
                    }

                    url = getLiveWorkerUrl( routing, shardKey.route( routing.routingTable ) );
                }

                GPUdbLogger.debug_with_info( "Retrieving records from <" + url + "/bycolumn> with <" + compositeExpression + ">" );
                response = this.gpudb.submitRequest(GPUdbBase.appendPathToURL(url, "/bycolumn"), request, response, false);
            }

            // Check if shard re-balancing is under way at the server; if so,
            // we need to update the shard mapping
            if ( "true".equals( response.getInfo().get( GPUdbBase.RESPONSE_INFO_DATA_REROUTED ) ) )
                updateWorkerQueues();

            // Set up the decoded response
            decodedResponse.setTableName(response.getTableName());
            decodedResponse.setDataType( Type.fromDynamicSchema( response.getResponseSchemaStr(), response.getBinaryEncodedResponse() ) );
            decodedResponse.setData( DynamicTableRecord.transpose( response.getResponseSchemaStr(), response.getBinaryEncodedResponse(), decodedResponse.getDataType() ) );
            decodedResponse.setTotalNumberOfRecords(response.getTotalNumberOfRecords());
            decodedResponse.setHasMoreRecords(response.getHasMoreRecords());
            decodedResponse.setInfo(response.getInfo());
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
                    throw new GPUdbException( originalCause + "; " + ex2.getMessage(), ex, true );
                }
            } else {
                // For debugging purposes only (can be very useful!)
                GPUdbLogger.debug_with_info( "Caught GPUdbException: " + ex.getMessage() );
            }
            GPUdbLogger.debug_with_info( "Did failover succeed? " + didFailoverSucceed );

            // Update the worker queues since we've failed over to a
            // different cluster
            GPUdbLogger.debug_with_info( "Updating worker queues" );

            // A failure to rebuild must not displace the error we are
            // recovering from; record it and carry on to the retry decision
            // with the queues left unchanged.
            boolean updatedWorkerQueues = false;
            try {
                updatedWorkerQueues = updateWorkerQueues();
            } catch ( Exception rebuildEx ) {
                GPUdbLogger.warn( "Could not update the worker queues while recovering from <"
                                  + ex.getMessage() + ">: " + rebuildEx.getMessage() );
            }
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
                    return this.getColumnsByKey( columns, keyValues, expression );
                } catch (Exception ex2) {
                    // Keep the original failure.  It is the diagnosis; the
                    // retry's own failure is usually a consequence of it, and
                    // replacing it loses the only useful message.
                    throw new GPUdbException( ex.getMessage()
                                              + "; the retry after recovery also failed: "
                                              + ex2.getMessage(), ex );
                }
            }
            throw new GPUdbException( ex.getMessage(), ex );
        } catch (Exception ex) {
            GPUdbLogger.debug_with_info( "Caught java exception: " + ex.getMessage() );
            // Retrieval failed, but maybe due to shard mapping changes (due to
            // cluster reconfiguration)? Check if the mapping needs to be updated
            // or has been updated by another thread already after the
            // insertion was attempted
            // A failure to rebuild must not displace the error we are
            // recovering from; record it and carry on with the queues left
            // unchanged.
            boolean updatedWorkerQueues = false;
            try {
                updatedWorkerQueues = updateWorkerQueues();
            } catch ( Exception rebuildEx ) {
                GPUdbLogger.warn( "Could not update the worker queues while recovering from <"
                                  + ex.getMessage() + ">: " + rebuildEx.getMessage() );
            }

            boolean retry = false;
            synchronized ( this.shardUpdateTime ) {
                retry = ( updatedWorkerQueues
                          || ( retrievalAttemptTimestamp < this.shardUpdateTime.longValue() ) );
            }
            if ( retry ) {
                // We need to try fetching the records again
                try {
                    return this.getColumnsByKey( columns, keyValues, expression );
                } catch (Exception ex2) {
                    // Keep the original failure.  It is the diagnosis; the
                    // retry's own failure is usually a consequence of it, and
                    // replacing it loses the only useful message.
                    throw new GPUdbException( ex.getMessage()
                                              + "; the retry after recovery also failed: "
                                              + ex2.getMessage(), ex );
                }
            }
            throw new GPUdbException( ex.getMessage(), ex );
        }

        return decodedResponse;
    }  // getColumnsByKey()


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
