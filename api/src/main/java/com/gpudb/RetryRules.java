package com.gpudb;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.core5.http.ConnectionClosedException;
import org.apache.hc.core5.http.NoHttpResponseException;

/**
 * Decides, for a request that failed, whether the client sends it again to the
 * same server, and if not, whether it fails over to another cluster or reports
 * the failure to the caller.
 * <p>
 * Two things decide it.  The first is what the server said, read before its
 * status:  a {@code 503} can report a server that is offline, which a re-send
 * cannot change, or one that is busy, which it may.  The second is whether the
 * server can have acted on the request:  a request that never reached it, or
 * was lost before it was written in full, cannot have been applied, and is
 * re-sent; one lost after that may have been, and is re-sent only if a second
 * copy is harmless -- a bulk-insert batch into a table with a primary key, a
 * read the client sends itself, or a file listing.
 * <p>
 * Re-sends are counted against one budget per request, spent by the
 * connection's request layer; no other layer re-sends to the same server.
 */
final class RetryRules {

    /**
     * Whether a failed attempt may be sent again to the same server.
     */
    enum Resend {
        /**
         * Re-send it, up to the request's budget:  nothing reached the
         * server, the request was lost before it was written in full, or the
         * server or a gateway could not take it at that moment.
         */
        ALWAYS,
        /**
         * Re-send it, up to the request's budget, after a wait that grows with
         * each re-send; see {@link #resendWaitMillis}.  The rank refused the
         * request before handling it because it is rebuilding its HA client
         * after its cluster came back online, which takes seconds:  nothing
         * was stored, and an immediate re-send would spend the budget in
         * milliseconds on a condition it cannot outlast.
         */
        AFTER_WAIT,
        /**
         * Re-send it only if a second copy is harmless:  the server may have
         * applied it, or some of its ranks may have stored their part of it.
         */
        IF_REPEATABLE,
        /** Never re-send it to the same server. */
        NEVER
    }


    /**
     * What the client does with a reply that failed.
     */
    enum Reply {
        /** Re-send it, then fail over. */
        RESEND,
        /**
         * Re-send it after a wait, then fail over:  the rank is rebuilding its
         * HA client, and refused the request before handling it.
         */
        RANK_NOT_READY,
        /** Fail over without re-sending. */
        FAIL_OVER,
        /** Fail over, re-sending only if a second copy is harmless. */
        RANK_FAILURE,
        /** Report it to the caller, without re-sending or failing over. */
        REPORT
    }


    /**
     * What a rank reports, as a per-record error or a warning for the batch,
     * when it skips a record whose primary key matches one already stored.
     */
    static final String PRIMARY_KEY_COLLISION_MESSAGE = "Primary Key already exists";

    /**
     * Messages reporting a server state that a second copy of the request
     * would meet again.
     */
    private static final List<String> SERVER_STATE_MESSAGES = Collections.unmodifiableList(Arrays.asList(
            GPUdbBase.DB_EXITING_ERROR_MESSAGE,
            GPUdbBase.DB_SHUTTING_DOWN_ERROR_MESSAGE,
            GPUdbBase.DB_OFFLINE_ERROR_MESSAGE,
            GPUdbBase.DB_STARTING_UP_ERROR_MESSAGE,
            GPUdbBase.DB_RANK_DOWN_ERROR_MESSAGE,
            GPUdbBase.DB_ADMIN_ONLY_ERROR_MESSAGE,
            GPUdbBase.DB_SYSTEM_LIMITED_ERROR_MESSAGE,
            GPUdbBase.DB_DRAINING_HAQ_ERROR_MESSAGE,
            GPUdbBase.DB_QP_DOWN_ERROR_MESSAGE
    ));

    /**
     * The reason phrases, in lower case, of a {@code 503} reporting a server
     * state.  Two states, migrating and irrecoverable, are shown only
     * here; the others are also in the message, but the reason phrase is the
     * one part of the reply that every encoding shares.
     */
    private static final List<String> SERVER_STATE_REASONS = Collections.unmodifiableList(Arrays.asList(
            "exiting",
            "offline",
            "starting",
            "rank down",
            "admin only",
            "system limited",
            "migrating",
            "irrecoverable"
    ));

    /** The message of a request sent to a host manager other than the head's. */
    private static final String HEAD_HOST_MANAGER_ONLY_MESSAGE = "only accessible from head host manager";

    /** Messages reporting that a rank failed while handling the request. */
    private static final Pattern RANK_FAILURE_MESSAGE = Pattern.compile(
            "Rank \\d+ non-responsive"
            + "|Failed to send insert request to other rank"
            + "|failed to process MH insert redirect");

    /** The reason phrase, in lower case, of a failure sending between ranks. */
    private static final String RANK_FAILURE_REASON = "network error";

    /**
     * Messages reporting that the rank is initializing its HA processors after
     * its cluster came back online.
     */
    private static final Pattern RANK_NOT_READY_MESSAGE = Pattern.compile(
            "Unavailable: (?:Producer not available|Consumer not created)");

    /** The wait before the first re-send of a request a rank was not ready for. */
    static final long FIRST_RESEND_WAIT_MS = 1_000;

    /** The longest wait before a re-send of a request a rank was not ready for. */
    static final long MAX_RESEND_WAIT_MS = 8_000;

    /**
     * Endpoints whose requests are repeatable whoever sends them:
     * listings with no options, which read and change nothing.
     */
    private static final List<String> REPEATABLE_ENDPOINTS = Collections.unmodifiableList(Arrays.asList(
            "/show/files",
            "/show/directories"
    ));


    private RetryRules() {
    }


    /**
     * Whether a failure to send a request, or to receive its response, is one
     * of the connection failures the re-send decision distinguishes:  no
     * response, a timeout, a socket failure, an unresolved host, or a
     * connection closed early.  Any
     * other {@link IOException} is not, and is failed over without a re-send.
     *
     * @param failure  the failure the HTTP client met
     * @return  whether it is a connection failure
     */
    static boolean isConnectionFailure(IOException failure) {
        return
            failure instanceof NoHttpResponseException ||
            failure instanceof SocketTimeoutException  ||
            failure instanceof SocketException         ||
            failure instanceof UnknownHostException    ||
            failure instanceof ConnectionClosedException;
    }


    /**
     * Whether a connection failure happened before the connection was made,
     * so that nothing reached the server:  a refused or unreachable
     * connection, an unresolved host, or a connect timeout.
     *
     * @param failure  the failure the HTTP client met
     * @return  whether nothing can have reached the server
     */
    static boolean isConnectFailure(IOException failure) {
        return
            failure instanceof ConnectException         ||
            failure instanceof ConnectTimeoutException  ||
            failure instanceof NoRouteToHostException   ||
            failure instanceof PortUnreachableException ||
            failure instanceof UnknownHostException;
    }


    /**
     * Whether a failure to send a request, met before any response arrived,
     * may be re-sent to the same server.
     *
     * @param failure  what sending the request failed with
     * @param writtenInFull  whether the request was written in full before
     *        the failure; a request without a body is written in full once
     *        its connection is made
     * @return  {@link Resend#ALWAYS} where nothing can have reached the server
     *          or the request was lost before it was written in full,
     *          {@link Resend#IF_REPEATABLE} where it was lost after, and
     *          {@link Resend#NEVER} for a failure that is not a connection
     *          failure
     */
    static Resend ofSendFailure(Throwable failure, boolean writtenInFull) {
        IOException connectionFailure = connectionFailureIn(failure);
        if (connectionFailure == null)
            return Resend.NEVER;
        if (isConnectFailure(connectionFailure) || !writtenInFull)
            return Resend.ALWAYS;
        return Resend.IF_REPEATABLE;
    }


    /**
     * Whether a failure to read a response's body may be re-sent to the same
     * server.  The server has begun to answer, so it has handled the
     * request.
     *
     * @param failure  what reading the response failed with
     * @return  {@link Resend#IF_REPEATABLE} for a connection lost while the
     *          response was read, or {@code null} for any other failure, such
     *          as a body that cannot be decoded
     */
    static Resend ofReadFailure(Throwable failure) {
        return (connectionFailureIn(failure) == null) ? null : Resend.IF_REPEATABLE;
    }


    /**
     * What the client does with a reply that failed, given what the server
     * said and the reply's status:  its message decides first, then its
     * status.
     *
     * @param httpStatus  the reply's HTTP status code
     * @param reasonPhrase  the reply's reason phrase, or {@code null}
     * @param message  the reply's message, in whichever encoding the reply
     *        used, or {@code null} where it has none
     * @return  what the client does with it
     */
    static Reply ofReply(int httpStatus, String reasonPhrase, String message) {
        if (message != null && RANK_NOT_READY_MESSAGE.matcher(message).find())
            return Reply.RANK_NOT_READY;

        if (isServerState(httpStatus, reasonPhrase, message))
            return Reply.FAIL_OVER;

        if (message != null && message.contains(HEAD_HOST_MANAGER_ONLY_MESSAGE))
            return Reply.REPORT;

        if (isRankFailure(httpStatus, reasonPhrase, message))
            return Reply.RANK_FAILURE;

        switch (httpStatus) {
            case HttpURLConnection.HTTP_BAD_GATEWAY:
            case HttpURLConnection.HTTP_UNAVAILABLE:
            case HttpURLConnection.HTTP_GATEWAY_TIMEOUT:
                return Reply.RESEND;
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                return Reply.FAIL_OVER;
            default:
                return Reply.REPORT;
        }
    }


    /**
     * How long to wait before re-sending a request a rank was not ready for:
     * {@link #FIRST_RESEND_WAIT_MS} before the first re-send, twice as long
     * before each after it, and at most {@link #MAX_RESEND_WAIT_MS}.  A rank
     * takes seconds to rebuild its HA client, so the waits are what let a
     * request's budget of re-sends outlast the rebuild.
     *
     * @param waitsSoFar  how many times this request has already waited
     * @return  the wait, in milliseconds
     */
    static long resendWaitMillis(int waitsSoFar) {
        long wait = FIRST_RESEND_WAIT_MS << Math.min(waitsSoFar, 30);
        return Math.min(wait, MAX_RESEND_WAIT_MS);
    }


    /**
     * Whether a reply reports a server state that a second copy of the
     * request would meet again.
     */
    private static boolean isServerState(int httpStatus, String reasonPhrase, String message) {
        if (message != null)
            for (String stateMessage : SERVER_STATE_MESSAGES)
                if (message.contains(stateMessage))
                    return true;

        return httpStatus == HttpURLConnection.HTTP_UNAVAILABLE
                && reasonPhrase != null
                && SERVER_STATE_REASONS.contains(reasonPhrase.trim().toLowerCase(Locale.ROOT));
    }


    /**
     * Whether a reply reports that a rank failed while handling the request.
     */
    private static boolean isRankFailure(int httpStatus, String reasonPhrase, String message) {
        if (message != null && RANK_FAILURE_MESSAGE.matcher(message).find())
            return true;

        return httpStatus == HttpURLConnection.HTTP_UNAVAILABLE
                && reasonPhrase != null
                && RANK_FAILURE_REASON.equals(reasonPhrase.trim().toLowerCase(Locale.ROOT));
    }


    /**
     * Whether a request to a URL is repeatable whoever sends it, because its
     * endpoint is a file listing, which takes no options and changes
     * nothing, so a caller's copy is as safe to repeat as the client's own.
     *
     * @param url  the URL the request is sent to
     * @return  whether a second copy of the request is harmless
     */
    static boolean isRepeatableEndpoint(URL url) {
        String path = (url == null) ? null : url.getPath();
        if (path == null)
            return false;

        for (String endpoint : REPEATABLE_ENDPOINTS)
            if (path.endsWith(endpoint))
                return true;
        return false;
    }


    /**
     * Whether a message a rank returned for a record, or for a batch, reports
     * a primary key collision:  a record skipped because its primary key
     * matches one already stored.
     *
     * @param message  the per-record error or batch warning
     * @return  whether it reports a primary key collision
     */
    static boolean isPrimaryKeyCollision(String message) {
        return message != null && message.contains(PRIMARY_KEY_COLLISION_MESSAGE);
    }


    /**
     * The first connection failure in a failure's chain of causes, or
     * {@code null} where there is none.
     */
    private static IOException connectionFailureIn(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = (cause.getCause() == cause) ? null : cause.getCause())
            if (cause instanceof IOException && isConnectionFailure((IOException) cause))
                return (IOException) cause;

        return null;
    }
}
