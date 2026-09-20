package com.gpudb;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A list of worker URLs to use for multi-head ingest and retrieval.
 *
 * <p>The addresses belong to the <b>connection</b>, not to this list.  A
 * {@link GPUdb} instance discovers each cluster's rank addresses, applies
 * {@link GPUdbBase.Options#setHostnameRegex the connection's hostname regex} to
 * them, and keeps the result per cluster; this class is a view onto that, held
 * by a {@link BulkInserter} or {@link RecordRetriever}.
 *
 * <p>It does not resolve or filter addresses of its own.  A filter has no
 * coherent meaning here, because it cannot survive a failover: one tuned for a
 * cluster's addressing may match nothing on the next, and multi-head objects
 * fail over with the connection.  Set the regex on the connection instead,
 * where it is applied to every cluster as that cluster is discovered.
 *
 * <p>The one choice that <i>is</i> meaningful per object is declining multi-head
 * altogether -- see {@link #WorkerList()}.  That names an intent rather than an
 * address, and an intent transfers across clusters where a list of addresses
 * does not.
 *
 * <p>Note entry {@code i} of this list is rank {@code i + 1}; the head rank is
 * not a member, and a rank removed from the cluster keeps its slot as
 * {@code null}.  The worker indices in the shard routing table are rank
 * numbers, so compacting the list would silently misroute every rank above the
 * hole.
 */
public class WorkerList extends ArrayList<URL> {

    private static final long serialVersionUID = 1L;

    /**
     * Whether the caller asked for head-node-only operation by handing over an
     * empty list.  Named for the contract rather than the origin: a list built
     * from real addresses is also supplied by a caller, and does <i>not</i> mean
     * this.
     */
    private boolean disableMultiHead = false;


    /**
     * Creates an empty {@link WorkerList}, which declines multi-head operations
     * for the {@link BulkInserter} or {@link RecordRetriever} it is given to.
     *
     * <p>That object will route through the head node for its lifetime.  The
     * choice is authoritative: it is not undone when the worker list is rebuilt
     * after a failover or a shard rebalance.
     *
     * <p>This is the way to decline multi-head for a single object while keeping
     * the connection's fail-over.  Disabling auto-discovery is not equivalent --
     * it also stops the client discovering the other clusters in an HA ring, so
     * it costs fail-over as well.
     *
     * <p>Nothing inside the API may use this constructor: it is a caller's way
     * of expressing an intent, and an internal use would silently pin its own
     * list to head-node-only.
     */
    public WorkerList() {
        this.disableMultiHead = true;
    }


    /**
     * Creates a {@link WorkerList} populated with the given URLs.
     *
     * <p>The list is honored as given, but is <b>not maintained</b>: after the
     * connection fails over or back, the object adopts that cluster's addresses
     * instead, because a list of one cluster's rank addresses says nothing about
     * another's.
     *
     * @param urls  the worker rank URLs, with {@code null} in the slot of any
     *              rank removed from the cluster
     *
     * @deprecated for internal use.  Pass {@code null} to a
     *             {@link BulkInserter} or {@link RecordRetriever} to use the
     *             connection's own addresses, or {@link #WorkerList()} to
     *             decline multi-head.
     */
    @Deprecated
    public WorkerList(List<URL> urls) {
        super(urls);
    }


    /**
     * Creates a {@link WorkerList} from the worker rank addresses the given
     * connection has already discovered for the cluster it is currently on.
     *
     * <p>This reads the connection's addresses; it does not query the server or
     * re-resolve them.  They were filtered through the connection's hostname
     * regex when that cluster was discovered, so they already reflect it.
     *
     * <p>If the connection has no worker addresses for the current cluster --
     * multi-head is off at the server, or the cluster advertises none this
     * client can use -- the list is empty and the object using it operates
     * through the head node.
     *
     * @param gpudb  the {@link GPUdb} instance whose addresses to use
     *
     * @throws GPUdbException  never thrown; retained for source compatibility
     */
    public WorkerList(GPUdb gpudb) throws GPUdbException {
        this( gpudb.getCurrentMultiHeadSnapshot() );
    }


    /**
     * Creates a {@link WorkerList} from a multi-head snapshot the caller is
     * already holding.
     *
     * <p><b>Why this exists alongside {@link #WorkerList(GPUdb)},</b> which looks
     * like it does the same thing.  That one reads the connection <i>now</i>; a
     * caller rebuilding its worker list has already taken a snapshot, and must
     * build from that same one.  Reading again would let another thread's probe
     * land in between, so the object would build its queues from one answer and
     * record having seen another -- and then never rebuild for the difference.
     *
     * <p>A {@code null} snapshot, or one whose cluster cannot give this client
     * multi-head, yields an <b>empty</b> list, which is what makes the object
     * holding it operate through the head node.  Note an empty list is not the
     * same as a declined one: {@link #disablesMultiHead()} stays false here, so
     * a later rebuild can still populate it.  Only {@link #WorkerList()} declines.
     *
     * @param snapshot  the addresses and capability verdict to build from, or
     *                  {@code null} where the connection has no current cluster
     */
    WorkerList( GPUdbBase.MultiHeadSnapshot snapshot ) {
        if ( (snapshot == null) || !snapshot.isMultiHeadAvailable() )
            return;

        List<URL> rankUrls = snapshot.getWorkerRankUrls();
        if ( rankUrls != null )
            addAll( rankUrls );
    }


    /**
     * @param gpudb    the {@link GPUdb} instance whose addresses to use
     * @param ipRegex  ignored
     *
     * @throws GPUdbException  never thrown; retained for source compatibility
     *
     * @deprecated the regex is ignored.  Address filtering belongs on the
     *             connection -- see
     *             {@link GPUdbBase.Options#setHostnameRegex} -- because a filter
     *             at this scope cannot survive a failover: one tuned for a
     *             cluster's addressing may match nothing on the next.  This
     *             constructor behaves as {@link #WorkerList(GPUdb)}.
     */
    @Deprecated
    public WorkerList(GPUdb gpudb, Pattern ipRegex) throws GPUdbException {
        this(gpudb);

        if (ipRegex != null) {
            GPUdbLogger.warn("Ignoring the IP regex given to a WorkerList <" + ipRegex.pattern()
                             + ">; worker addresses are filtered by the connection."
                             + "  Use Options.setHostnameRegex() instead.");
        }
    }


    /**
     * @param gpudb     the {@link GPUdb} instance whose addresses to use
     * @param ipPrefix  ignored
     *
     * @throws GPUdbException  never thrown; retained for source compatibility
     *
     * @deprecated the prefix is ignored; see {@link #WorkerList(GPUdb, Pattern)}
     *             for why, and {@link GPUdbBase.Options#setHostnameRegex} for
     *             the replacement.
     */
    @Deprecated
    public WorkerList(GPUdb gpudb, String ipPrefix) throws GPUdbException {
        this(gpudb);

        if (ipPrefix != null) {
            GPUdbLogger.warn("Ignoring the IP prefix given to a WorkerList <" + ipPrefix
                             + ">; worker addresses are filtered by the connection."
                             + "  Use Options.setHostnameRegex() instead.");
        }
    }


    /**
     * @return  always {@code null}
     *
     * @deprecated a worker list has no regex of its own.  Addresses are filtered
     *             by the connection; see
     *             {@link GPUdbBase.Options#getHostnameRegex}.
     */
    @Deprecated
    public Pattern getIpRegex() {
        return null;
    }


    /**
     * Whether multi-head operations can be used with this list.
     *
     * @return  whether the list names any worker rank
     */
    public boolean isMultiHeadEnabled() {
        return !isEmpty();
    }


    /**
     * Whether this list expresses a request for head-node-only operation.
     *
     * <p>True only for a list built with {@link #WorkerList()}, the empty list a
     * caller hands over to turn multi-head off for one {@code BulkInserter} or
     * {@code RecordRetriever}.  Such a list is authoritative: it is never
     * replaced, including when the worker list is rebuilt after a failover or a
     * shard rebalance.
     *
     * <p><b>Used internally,</b>  this is the routing flag the
     * rebuild paths consult
     *
     * @return  whether multi-head operations were declined for this list
     */
    boolean disablesMultiHead() {
        return this.disableMultiHead;
    }
}
