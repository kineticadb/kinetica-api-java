package com.gpudb;

/**
 * How one request is re-sent to the same server, and what its re-sends
 * left behind.
 * <p>
 * A request is given a budget of re-sends, and is marked repeatable where a
 * second copy is harmless even if the server applied the first.
 * When a repeatable request is re-sent after a failure that leaves the first
 * copy's outcome unknown, the failure is recorded here, so that whoever sent
 * the request can say which of its results may come from that copy.
 * <p>
 * An instance belongs to one request and to the thread sending it.
 */
final class RequestRetries {

    /**
     * A budget of re-sends that is the connection's
     * {@link GPUdbBase#getMaxRetries() maxRetries}, rather than one of the
     * request's own.
     */
    static final int CONNECTION_BUDGET = -1;

    private final int maxRetries;
    private final boolean repeatable;
    private String uncertainFailure;


    /**
     * @param maxRetries  how many times the request may be re-sent to the same
     *        server, or {@link #CONNECTION_BUDGET} for the connection's
     *        {@link GPUdbBase#getMaxRetries() maxRetries}
     * @param repeatable  whether a second copy of the request is harmless
     *        even if the server applied the first
     */
    RequestRetries(int maxRetries, boolean repeatable) {
        this.maxRetries = maxRetries;
        this.repeatable = repeatable;
    }


    /**
     * The connection's budget, for a request that is not repeatable unless it
     * is sent as a read of the client's own.
     *
     * @return  a new instance for one request
     */
    static RequestRetries connectionDefault() {
        return new RequestRetries(CONNECTION_BUDGET, false);
    }


    /**
     * @param connectionMaxRetries  the connection's
     *        {@link GPUdbBase#getMaxRetries() maxRetries}
     * @return  how many times the request may be re-sent to the same server
     */
    int budget(int connectionMaxRetries) {
        return (this.maxRetries == CONNECTION_BUDGET) ? connectionMaxRetries : this.maxRetries;
    }


    /**
     * @return  whether a second copy of the request is harmless even if the
     *          server applied the first
     */
    boolean isRepeatable() {
        return this.repeatable;
    }


    /**
     * Records that the request is being re-sent after a failure that leaves
     * an earlier copy's outcome unknown.
     *
     * @param failure  the failure, as reported
     */
    void noteResentAfterUncertainFailure(String failure) {
        this.uncertainFailure = failure;
    }


    /**
     * @return  the last failure after which the request was re-sent although
     *          an earlier copy may have been applied, or {@code null} where it
     *          never was
     */
    String getUncertainFailure() {
        return this.uncertainFailure;
    }
}
