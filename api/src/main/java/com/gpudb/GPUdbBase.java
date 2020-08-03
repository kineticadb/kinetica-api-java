package com.gpudb;

import com.gpudb.protocol.ShowSystemPropertiesRequest;
import com.gpudb.protocol.ShowSystemPropertiesResponse;
import com.gpudb.protocol.ShowSystemStatusRequest;
import com.gpudb.protocol.ShowSystemStatusResponse;
import com.gpudb.protocol.ShowTableRequest;
import com.gpudb.protocol.ShowTableResponse;
import com.gpudb.protocol.ShowTypesRequest;
import com.gpudb.protocol.ShowTypesResponse;
import com.gpudb.util.ssl.X509TrustManagerBypass;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.NumberFormatException;
import java.net.HttpURLConnection;
import javax.net.ssl.HostnameVerifier;
import java.net.MalformedURLException;
import java.net.URL;
import javax.net.ssl.SSLContext;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.xerial.snappy.Snappy;



/**
 * Base class for the GPUdb API that provides general functionality not specific
 * to any particular GPUdb request. This class is never instantiated directly;
 * its functionality is accessed via instances of the {@link GPUdb} class.
 */
public abstract class GPUdbBase {

    // The amount of time for checking if a given IP/hostname is good (10 seconds)
    private static final int DEFAULT_SERVER_CONNECTION_TIMEOUT = 10000;

    // The amount of time of inactivity after which the connection would be
    // validated: 200ms
    private static final int DEFAULT_CONNECTION_INACTIVITY_VALIDATION_TIMEOUT = 200;

    // The default port for host manager URLs
    private static final int DEFAULT_HOST_MANAGER_PORT = 9300;

    // The default port for host manager URLs when using HTTPD
    private static final int DEFAULT_HTTPD_HOST_MANAGER_PORT = 8082;

    // The number of times that the API will attempt to submit a host
    // manager endpoint request.  We need this in case the user chose
    // a bad host manager port.  We don't want to go into an infinite
    // loop
    private static final int HOST_MANAGER_SUBMIT_REQUEST_RETRY_COUNT = 3;

    // The default number of times that the API will attempt to reconnect
    // to a cluster (when possibly a failover event has occured) before
    // switching to other clusters
    private static final int DEFAULT_CLUSTER_CONNECTION_RETRY_COUNT = 1;

    // The minimum value allowed for the cluster connection retry count.
    // It is set to 1 so that backup cluster failure can trigger an
    // update of the addresses (in case an N+1 failover event happened
    // in the past at that backup cluster that the API is unaware of)
    private static final int MINIMUM_CLUSTER_CONNECTION_RETRY_COUNT = 1;

    // The timeout (in milliseconds) used for endpoint calls used during N+1
    // failover recovery; we use a small timeout so that it does not take
    // a long time to figure out that a rank is down.  Using 1.5 seconds.
    private static final int DEFAULT_INTERNAL_ENDPOINT_CALL_TIMEOUT = 1500;

    // The timeout interval (in milliseconds) used when trying to establish a
    // connection to the database at GPUdb initialization time.  The default
    // is 0 (no retry).
    private static final long DEFAULT_INITIAL_CONNECTION_ATTEMPT_TIMEOUT_MS = 0;

    // The timeout interval (in milliseconds) used when trying to recover from
    // an intra-cluster failover event.  The default is 0, equivalent to infinite.
    private static final long DEFAULT_INTRA_CLUSTER_FAILOVER_TIMEOUT_MS = 0;

    // The maxium number of connections across all or an individual host
    private static final int DEFAULT_MAX_TOTAL_CONNECTIONS    = 40;
    private static final int DEFAULT_MAX_CONNECTIONS_PER_HOST = 10;


    // JSON parser
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();


    /**
     * A set of configurable options for the GPUdb API. May be passed into the
     * {@link GPUdb#GPUdb(String, GPUdbBase.Options) GPUdb constructor} to
     * override the default options.
     */
    public static final class Options {
        private String  primaryUrl = "";
        private String  username;
        private String  password;
        private Pattern hostnameRegex        = null;
        private boolean useSnappy            = true;
        private boolean bypassSslCertCheck   = false;
        private boolean disableFailover      = false;
        private boolean disableAutoDiscovery = false;
        private HAFailoverOrder haFailoverOrder = HAFailoverOrder.RANDOM;
        private ExecutorService executor;
        private Map<String, String> httpHeaders = new HashMap<>();
        private int   timeout;
        private int   connectionInactivityValidationTimeout = DEFAULT_CONNECTION_INACTIVITY_VALIDATION_TIMEOUT;
        private int   serverConnectionTimeout = DEFAULT_SERVER_CONNECTION_TIMEOUT;
        private int   threadCount = 1;
        private int   hmPort      = DEFAULT_HOST_MANAGER_PORT;
        private int   clusterReconnectCount = DEFAULT_CLUSTER_CONNECTION_RETRY_COUNT;
        private long  initialConnectionAttemptTimeout = DEFAULT_INITIAL_CONNECTION_ATTEMPT_TIMEOUT_MS;
        private long  intraClusterFailoverTimeout     = DEFAULT_INTRA_CLUSTER_FAILOVER_TIMEOUT_MS;
        private int   maxTotalConnections   = DEFAULT_MAX_TOTAL_CONNECTIONS;
        private int   maxConnectionsPerHost = DEFAULT_MAX_CONNECTIONS_PER_HOST;
        private Level loggingLevel = GPUdbLogger.DEFAULT_LOGGING_LEVEL;

        /**
         * Gets the URL of the primary cluster of the HA environment.
         *
         * @return  the primary URL
         *
         * @see #setPrimaryUrl(String)
         */
        public String getPrimaryUrl() {
            return primaryUrl;
        }

        /**
         * Gets the username to be used for authentication to GPUdb.
         *
         * @return  the username
         *
         * @see #setUsername(String)
         */
        public String getUsername() {
            return username;
        }

        /**
         * Gets the password to be used for authentication to GPUdb.
         *
         * @return  the password
         *
         * @see #setPassword(String)
         */
        public String getPassword() {
            return password;
        }

        /**
         * Gets the regex pattern to be used to filter URLs of the servers.  If
         * null, then the first URL encountered per rank will be used.
         *
         * @return  the IP or hostname regex to match URLs against
         *
         * @see #setHostnameRegex(Pattern)
         */
        public Pattern getHostnameRegex() {
            return this.hostnameRegex;
        }

        /**
         * Gets the value of the flag indicating whether to use Snappy
         * compression for certain GPUdb requests that potentially submit large
         * amounts of data.
         *
         * @return  the value of the Snappy compression flag
         *
         * @see #setUseSnappy(boolean)
         */
        public boolean getUseSnappy() {
            return useSnappy;
        }

        /**
         * Gets the value of the flag indicating whether to verify the SSL
         * certificate for HTTPS connections.
         *
         * @return  the value of the SSL certificate verification bypass flag
         *
         * @see #setBypassSslCertCheck(boolean)
         */
        public boolean getBypassSslCertCheck() {
            return this.bypassSslCertCheck;
        }

        /**
         * Gets the value of the flag indicating whether to disable failover
         * upon failures (both high availability--or inter-cluster--failover
         * and N+1--or intra-cluster--failover).
         *
         * @return  the value of the failover disabling flag
         *
         * @see #setDisableFailover(boolean)
         */
        public boolean getDisableFailover() {
            return this.disableFailover;
        }

        /**
         * Gets the value of the flag indicating whether to disable automatic
         * discovery of backup clusters or worker rank URLs.  If set to true,
         * then the GPUdb object will not connect to the database at initialization
         * time, and will only work with the URLs given.  Default is false.
         *
         * @return  the value of the automatic disovery disabling flag
         *
         * @see #setDisableAutoDiscovery(boolean)
         */
        public boolean getDisableAutoDiscovery() {
            return this.disableAutoDiscovery;
        }

        /**
         * Gets the current high availability failover order.  Default
         * is RANDOM.
         *
         * @return the inter-cluster failover order
         *
         * @see #setHAFailoverOrder(HAFailoverOrder)
         * @see GPUdbBase#HAFailoverOrder
         */
        public HAFailoverOrder getHAFailoverOrder() {
            return this.haFailoverOrder;
        }

        /**
         * Gets the number of threads that will be used during data encoding and
         * decoding operations.
         *
         * @return  the number of threads
         *
         * @see #setThreadCount(int)
         * @see #setExecutor(ExecutorService)
         */
        public int getThreadCount() {
            return threadCount;
        }

        /**
         * Gets the {@link ExecutorService executor service} used for managing
         * threads during data encoding and decoding operations. If
         * {@code null}, threads will be created on demand; not used if
         * {@link #setThreadCount(int) thread count} equals 1.
         *
         * @return  the executor service
         *
         * @see #setExecutor(ExecutorService)
         * @see #setThreadCount(int)
         */
        public ExecutorService getExecutor() {
            return executor;
        }

        /**
         * Gets the map of additional HTTP headers to send to GPUdb with each
         * request. If empty, no additional headers will be sent.
         *
         * @return  the map
         *
         * @see #addHttpHeader(String, String)
         * @see #setHttpHeaders(Map)
         */
        public Map<String, String> getHttpHeaders() {
            return httpHeaders;
        }

        /**
         * Gets the host manager port number. Some endpoints are supported only
         * at the host manager, rather than the head node of the database.
         *
         * @return  the host manager port
         *
         * @see #setHostManagerPort(int)
         */
        public int getHostManagerPort() {
            return hmPort;
        }

        /**
         * Gets the period of inactivity (in milliseconds) after
         * which connection validity would be checked before reusing it.
         * This is for fine-tuning server connection parameters.  Using the
         * default of 200ms should suffice for most users.
         *
         * @return  the connection inactivity period (in ms)
         *
         * @see #setConnectionInactivityValidationTimeout(int)
         */
        public int getConnectionInactivityValidationTimeout() {
            return connectionInactivityValidationTimeout;
        }

        /**
         * Gets the timeout value, in milliseconds, after which a lack of
         * response from the GPUdb server will result in requests being aborted.
         * A timeout of zero is interpreted as an infinite timeout. Note that
         * this applies independently to various stages of communication, so
         * overall a request may run for longer than this without being aborted.
         *
         * @return  the timeout value
         *
         * @see #setTimeout(int)
         * @see #getServerConnectionTimeout()
         */
        public int getTimeout() {
            return timeout;
        }

        /**
         * Gets the server connection timeout value, in milliseconds, after
         * which an inability to establish a connection with the GPUdb server
         * will result in requests being aborted.  A timeout of zero is interpreted
         * as an infinite timeout. Note that this is different from the request
         * timeout.
         *
         * @return  the connection timeout value
         *
         * @see #getTimeout()
         * @see #setServerConnectionTimeout(int)
         */
        public int getServerConnectionTimeout() {
            return serverConnectionTimeout;
        }

        /**
         * Gets the maximum number of connections, across all hosts, allowed at
         * any given time.
         *
         * @return  the maxTotalConnections value
         *
         * @see #setMaxTotalConnections(int)
         */
        public int getMaxTotalConnections() {
            return maxTotalConnections;
        }

        /**
         * Gets the maximum number of connections, per host, allowed at
         * any given time.
         *
         * @return  the maxConnectionsPerHost value
         *
         * @see #setMaxConnectionsPerHost(int)
         */
        public int getMaxConnectionsPerHost() {
            return maxConnectionsPerHost;
        }

        /**
         * Gets the number of times the API tries to reconnect to the
         * same cluster (when a failover event has been triggered),
         * before actually failing over to any available backup
         * cluster.
         *
         * @return  the clusterReconnectCount value
         *
         * @see #setClusterReconnectCount(int)
         */
        public int getClusterReconnectCount() {
            return this.clusterReconnectCount;
        }



        /**
         * Gets the timeout used when trying to establish a connection to the
         * database at GPUdb initialization.  The value is given in milliseconds
         * and the default is 0.  0 indicates no retry will be done; instead,
         * the user given URLs will be stored without farther discovery.
         *
         * If multiple URLs are given by the user, then API will try all of them
         * once before retrying or giving up.  When this timeout is set
         * to a non-zero value, and the first attempt failed, then
         * the API will wait (sleep) for a certain amount of time and
         * try again.  Upon consecutive failures, the sleep amount
         * will be doubled.  So, before the first retry (i.e. the second
         * attempt), the API will sleep for one minute.  Before the second
         * retry, the API will sleep for two minutes, the next sleep interval
         * would be four minutes, and onward.
         *
         * @return  the initialConnectionAttemptTimeout value
         *
         * @see #setInitialConnectionAttemptTimeout(long)
         */
        public long getInitialConnectionAttemptTimeout() {
            return this.initialConnectionAttemptTimeout;
        }


        /**
         * Gets the timeout used when trying to recover from an intra-cluster
         * failover event.  The value is given in milliseconds.  The default is
         * equivalent to 5 minutes.
         *
         * @return  the intraClusterFailoverTimeout value
         *
         * @see #setIntraClusterFailoverTimeout( long )
         */
        public long getIntraClusterFailoverTimeout() {
            return this.intraClusterFailoverTimeout;
        }


        /**
         * Gets the logging level that will be used by the API.  By default,
         * logging is turned off; but if logging properties are provided
         * by the user, those properties will be respected.  If the user sets
         * the logging level explicitly (and it is not the default log level),
         * then the programmatically set level will be used instead of the
         * one set in the properties file.
         *
         * @return  the logging level
         *
         * @see #setLoggingLevel(String)
         * @see #setLoggingLevel(Level)
         */
        public Level getLoggingLevel() {
            return this.loggingLevel;
        }



        /**
         * Sets the URL of the primary cluster to use amongst the HA clusters.
         * This cluster will always be used first.  It can be part of the URLs
         * used to create the GPUdb object, or be a different one.  In either
         * case, this URL will always be chosen first to work against.  Also,
         * the {@link GPUdb} constructors will ensure that no duplicate of this
         * URL exists in the full set of URLs to use.  If this is not set, then
         * all available clusters will be treated with equal probability (unless
         * only a single URL is given for the Kinetica server, in which case it
         * will also be treated as the primary cluster).
         *
         * Can be given in the form of 'http[s]://X.X.X.X:PORT[/httpd-path]' or
         * just the IP address or the hostname.
         *
         * @param value  the URL of the primary cluster
         * @return       the current {@link Options} instance
         *
         * @see #getPrimaryUrl()
         */
        public Options setPrimaryUrl(String value) {
            primaryUrl = value;
            return this;
        }

        /**
         * Sets the username to be used for authentication to GPUdb. This
         * username will be sent with every GPUdb request made via the API along
         * with the {@link #setPassword(String) password} and may be used for
         * authorization decisions by the server if it is so configured. If
         * both the username and password are {@code null} (the default) or
         * empty strings, no authentication will be performed.
         *
         * @param value  the username
         * @return       the current {@link Options} instance
         *
         * @see #getUsername()
         * @see #setPassword(String)
         */
        public Options setUsername(String value) {
            username = value;
            return this;
        }

        /**
         * Sets the password to be used for authentication to GPUdb. This
         * password will be sent with every GPUdb request made via the API along
         * with the {@link #setUsername(String) username} and may be used for
         * authorization decisions by the server if it is so configured. If
         * both the username and password are {@code null} (the default) or
         * empty strings, no authentication will be performed.
         *
         * @param value  the password
         * @return       the current {@link Options} instance
         *
         * @see #getPassword()
         * @see #setUsername(String)
         */
        public Options setPassword(String value) {
            password = value;
            return this;
        }


        /**
         * Sets the IP address or hostname regex against which the server's
         * rank URLs would be matched when obtaining them.  If null, then
         * the first URL for any given rank would be used (if multiple are
         * available in the system properties).
         *
         * Note that the regex MUST NOT have the protocol or the port; it
         * should be a regular expression ONLY for the hostname/IP address.
         *
         * @param value  the IP or hostname regex to match URLs against
         * @return       the current {@link Options} instance
         *
         * @see #getHostnameRegex()
         */
        public Options setHostnameRegex(String value) throws GPUdbException {
            try {
                this.hostnameRegex = Pattern.compile( value );
                return this;
            } catch (PatternSyntaxException ex ) {
                String errorMsg = ( "Error in parsing the pattern for "
                                    + "hostname regex; given: '"
                                    + value + "'; got error: "
                                    + ex.getMessage() );
                throw new GPUdbException( errorMsg );
            }
        }


        /**
         * Sets the IP address or hostname regex against which the server's
         * rank URLs would be matched when obtaining them.  If null, then
         * the first URL for any given rank would be used (if multiple are
         * available in the system properties).
         *
         * Note that the regex MUST have an **optional** [http://] or
         * [https://] part to it since the regex would be applied in
         * various scenarios (where the protocol is sometimes present
         * and sometimes not).
         *
         * @param value  the IP or hostname regex to match URLs against
         * @return       the current {@link Options} instance
         *
         * @see #getHostnameRegex()
         */
        public Options setHostnameRegex(Pattern value) {
            this.hostnameRegex = value;
            return this;
        }

        /**
         * Sets the flag indicating whether to use Snappy compression for
         * certain GPUdb requests that potentially submit large amounts of data.
         * If {@code true}, such requests will be automatically compressed
         * before being sent to the server; the default is {@code false}.
         *
         * @param value  the new value for the Snappy compression flag
         * @return       the current {@link Options} instance
         *
         * @see #getUseSnappy()
         */
        public Options setUseSnappy(boolean value) {
            useSnappy = value;
            return this;
        }

        /**
         * Sets the flag indicating whether to verify the SSL certificate for
         * HTTPS connections.  If {@code true}, then the SSL certificate sent
         * by the server during HTTPS connection handshake will not be verified;
         * the public key sent by the server will be blindly trusted and used
         * to encrypt the packets.  The default is {@code false}.
         *
         * @param value  the value of the SSL certificate verification bypass
         *               flag
         * @return       the current {@link Options} instance
         *
         * @see #getBypassSslCertCheck()
         */
        public Options setBypassSslCertCheck(boolean value) {
            this.bypassSslCertCheck = value;
            return this;
        }

        /**
         * Sets the value of the flag indicating whether to disable failover
         * upon failures (both high availability--or inter-cluster--failover
         * and N+1--or intra-cluster--failover.  If {@code true}, then no
         * failover would be attempted upon triggering events regardless of
         * the availabilty of a high availability cluster or N+1 failover.
         * The default is {@code false}.
         *
         * @param value  the value of the failover disabling flag
         * @return       the current {@link Options} instance
         *
         * @see #getDisableFailover()
         */
        public Options setDisableFailover(boolean value) {
            this.disableFailover = value;
            return this;
        }

        /**
         * Sets the value of the flag indicating whether to disable automatic
         * discovery of backup clusters or worker rank URLs.  If set to true,
         * then the GPUdb object will not connect to the database at initialization
         * time, and will only work with the URLs given.
         *
         * @param value  the value of the automatic disovery disabling flag
         * @return       the current {@link Options} instance
         *
         * @see #getDisableAutoDiscovery()
         */
        public Options setDisableAutoDiscovery(boolean value) {
            this.disableAutoDiscovery = value;
            return this;
        }


        /**
         * Sets the value of the enum controlling the inter-cluster (high
         * availability) failover priority.
         *
         * @param value  the value of the high availability failover priority
         * @return       the current {@link Options} instance
         *
         * @see #getDisableAutoDiscovery()
         * @see GPUdbBase#HAFailoverOrder
         */
        public Options setHAFailoverOrder(HAFailoverOrder value) {
            this.haFailoverOrder = value;
            return this;
        }

        /**
         * Sets the number of threads that will be used during data encoding and
         * decoding operations. If set to one (the default), all encoding and
         * decoding will be performed in the calling thread.
         *
         * If an {@link #setExecutor(ExecutorService) executor service} is
         * provided, it will be used for thread management, and up to this many
         * operations will be submitted to it at a time for execution. (Note
         * that if the provided executor service supports fewer than this many
         * threads, fewer operations will actually be executed simultaneously.)
         * If no executor service is provided, any required threads will be
         * created on demand.
         *
         * @param value  the number of threads
         * @return       the current {@link Options} instance
         *
         * @throws IllegalArgumentException if {@code value} is less than one
         *
         * @see #getThreadCount()
         * @see #setExecutor(ExecutorService)
         */
        public Options setThreadCount(int value) {
            if (value <= 0) {
                throw new IllegalArgumentException("Thread count must be greater than zero.");
            }

            threadCount = value;
            return this;
        }

        /**
         * Sets the {@link ExecutorService executor service} used for managing
         * threads during data encoding and decoding operations. If
         * {@code null}, threads will be created on demand; not used if
         * {@link #setThreadCount(int) thread count} equals 1.
         *
         * If the provided executor service supports fewer threads than the
         * {@link #setThreadCount(int) thread count}, fewer operations will
         * actually be executed simultaneously.
         *
         * @param value  the executor service
         * @return       the current {@link Options} instance
         *
         * @see #getExecutor()
         * @see #setThreadCount(int)
         */
        public Options setExecutor(ExecutorService value) {
            executor = value;
            return this;
        }

        /**
         * Replaces the contents of the map of additional HTTP headers to send
         * to GPUdb with each request with the contents of the specified map.
         * If empty, no additional headers will be sent.
         *
         * @param value  the map
         * @return       the current {@link Options} instance
         *
         * @see #addHttpHeader(String, String)
         * @see #getHttpHeaders()
         */
        public Options setHttpHeaders(Map<String, String> value) {
            httpHeaders.clear();
            httpHeaders.putAll(value);
            return this;
        }

        /**
         * Adds an HTTP header to the map of additional HTTP headers to send to
         * GPUdb with each request. If the header is already in the map, its
         * value is replaced with the specified value.
         *
         * @param header  the HTTP header
         * @param value   the value of the HTTP header
         * @return        the current {@link Options} instance
         *
         * @see #getHttpHeaders()
         * @see #setHttpHeaders(Map)
         */
        public Options addHttpHeader(String header, String value) {
            httpHeaders.put(header, value);
            return this;
        }

        /**
         * Sets the host manager port number. Some endpoints are supported only
         * at the host manager, rather than the head node of the database.
         *
         * @param value  the host manager port number
         * @return       the current {@link Options} instance
         *
         * @see #getHostManagerPort()
         */
        public Options setHostManagerPort(int value) {
            if (value < 0) {
                throw new IllegalArgumentException("Host manager port number must be greater than zero; "
                                                   + "given " + value);
            }
            if (value > 65536) {
                throw new IllegalArgumentException("Host manager port number must be less than 65536; "
                                                   + "given " + value);
            }

            hmPort = value;
            return this;
        }

        /**
         * Sets the timeout value, in milliseconds, after which a lack of
         * response from the GPUdb server will result in requests being aborted.
         * A timeout of zero is interpreted as an infinite timeout. Note that
         * this applies independently to various stages of communication, so
         * overall a request may run for longer than this without being aborted.
         *
         * @param value  the timeout value
         * @return       the current {@link Options} instance
         *
         * @see #getTimeout()
         */
        public Options setTimeout(int value) {
            if (value < 0) {
                throw new IllegalArgumentException("Timeout must be greater than or equal to zero.");
            }

            timeout = value;
            return this;
        }

        /**
         * Sets the server connection timeout value, in milliseconds, after
         * which an inability to establish a connection with the GPUdb server
         * will result in requests being aborted.  A timeout of zero is interpreted
         * as an infinite timeout. Note that this is different from the request
         *
         * The default is 10000, which is equivalent to 10 seconds.
         *
         * @param value  the server connection timeout value
         * @return       the current {@link Options} instance
         *
         * @see #getServerConnectionTimeout()
         */
        public Options setServerConnectionTimeout(int value) {
            if (value < 0) {
                throw new IllegalArgumentException("Server connection timeout "
                                                   + "must be greater than or equal to zero.");
            }

            serverConnectionTimeout = value;
            return this;
        }

        /**
         * Sets the period of inactivity (in milliseconds) after
         * which connection validity would be checked before reusing it.
         * This is for fine-tuning server connection parameters.  Using the
         * default of 200ms should suffice for most users.
         *
         * The default is 200 (in milliseconds).
         *
         * *Note*: Non-positive value passed to this method disables connection
         *         validation.  So, use with great caution!
         *
         * @param value  the connection inactivity timeout (in ms)
         * @return       the current {@link Options} instance
         *
         * @see #getServerConnectionTimeout()
         */
        public Options setConnectionInactivityValidationTimeout(int value) {
            connectionInactivityValidationTimeout = value;
            return this;
        }

        /**
         * Sets the maximum number of connections, across all hosts, allowed at
         * any given time.  Must be 1 at a minimum.  The default value is 40.
         *
         * @param value  the maxTotalConnections value
         * @return       the current {@link Options} instance
         *
         * @see #getMaxTotalConnections()
         */
        public Options setMaxTotalConnections(int value) {
            if (value < 1) {
                throw new IllegalArgumentException("maxTotalConnections must be greater than zero.");
            }

            maxTotalConnections = value;
            return this;
        }


        /**
         * Sets the maximum number of connections, per host, allowed at
         * any given time.  Must be 1 at a minimum.  The default value is 10.
         *
         * @param value  the maxConnectionsPerHost value
         * @return       the current {@link Options} instance
         *
         * @see #getMaxConnectionsPerHost()
         */
        public Options setMaxConnectionsPerHost(int value) {
            if (value < 1) {
                throw new IllegalArgumentException("maxConnectionsPerHost must be greater than zero.");
            }

            maxConnectionsPerHost = value;
            return this;
        }

        /**
         * Sets the number of times the API tries to reconnect to the
         * same cluster (when a failover event has been triggered),
         * before actually failing over to any available backup
         * cluster.  The default value is 1.  The minimum is 1.
         *
         * @param value  the clusterReconnectCount value
         * @return       the current {@link Options} instance
         */
        public Options setClusterReconnectCount(int value) {
            if (value < MINIMUM_CLUSTER_CONNECTION_RETRY_COUNT) {
                throw new IllegalArgumentException("clusterReconnectCount must be greater than or equal to zero.");
            }

            this.clusterReconnectCount = value;
            return this;
        }


        /**
         * Sets the timeout used when trying to establish a connection to the
         * database at GPUdb initialization.  The value is given in
         * milliseconds.  0 indicates no retry will be done; instead, the user
         * given URLs will be stored without farther discovery.
         *
         * If multiple URLs are given by the user, then the API will try all of
         * them once before retrying or giving up.  When this timeout is set
         * to a non-zero value, and the first attempt failed, then
         * the API will wait (sleep) for a certain amount of time and
         * try again.  Upon consecutive failures, the sleep amount
         * will be doubled.  So, before the first retry (i.e. the second
         * attempt), the API will sleep for one minute.  Before the second
         * retry, the API will sleep for two minutes, the next sleep interval
         * would be four minutes, and onward.
         *
         * The default is 0, meaning the connection will NOT be re-attempted
         * upon failure.
         *
         * @param value  the initialConnectionAttemptTimeout value
         * @return       the current {@link Options} instance
         *
         * @see #getInitialConnectionAttemptTimeout()
         */
        public Options setInitialConnectionAttemptTimeout(long value) {
            if (value < 0) {
                throw new IllegalArgumentException( "initialConnectionAttemptTimeout "
                                                    + "must be greater than or equal "
                                                    + "to zero; given " + value);
            } else if ( value > 9223372036854L ) {
                throw new IllegalArgumentException("initialConnectionAttemptTimeout "
                                                   +"must be less than 9223372036854L;"
                                                   + " given " + value);
            }

            this.initialConnectionAttemptTimeout = value;
            return this;
        }



        /**
         * Sets the timeout used when trying to recover from an intra-cluster
         * failover event.  The value is given in milliseconds.
         *
         * @param value  the intraClusterFailoverTimeout value
         * @return       the current {@link Options} instance
         *
         * @see #getIntraClusterFailoverTimeout()
         */
        public Options setIntraClusterFailoverTimeout(long value) {
            if (value < 0) {
                throw new IllegalArgumentException( "intraClusterFailoverTimeout "
                                                    + "must be greater than or equal "
                                                    + "to zero; given " + value);
            } else if ( value > 9223372036854L ) {
                throw new IllegalArgumentException("intraClusterFailoverTimeout "
                                                   +"must be less than 9223372036854L;"
                                                   + " given " + value);
            }

            this.intraClusterFailoverTimeout = value;
            return this;
        }


        /**
         * Sets the logging level that will be used by the API.
         * Supported values:
         * <ul>
         *     <li> ALL
         *     <li> DEBUG
         *     <li> ERROR
         *     <li> FATAL
         *     <li> INFO
         *     <li> OFF (the default)
         *     <li> TRACE
         *     <li> TRACE_INT
         *     <li> WARN
         * </ul>
         *
         * If `OFF` is given, and if logging properties are provided by the user
         * (via log4j.properties or other files), those properties will be
         * respected.  If the user set logging level is not the default log
         * level, i.e. `OFF`), then the programmatically set level will be used
         * instead of the one set in the properties file.
         *
         * @return  the current {@link Options} instance
         *
         * @see #getLoggingLevel()
         */
        public Options setLoggingLevel(String value) throws GPUdbException {

            // Parse the level
            Level level = Level.toLevel( value );

            // Ensure a valid level was given
            if ( (level == Level.DEBUG)
                 && !value.equalsIgnoreCase( "DEBUG" ) ) {
                // The user didn't give debug, but Level returned it
                // (which it does when it can't parse the given value)
                String errorMsg = ( "Must provide a valid logging level "
                                    + "(please see documentation); given: '"
                                    + value + "'" );
                throw new GPUdbException( errorMsg );
            }

            this.loggingLevel = level;
            return this;
        }

        /**
         * Sets the logging level that will be used by the API.
         * Supported values:
         * <ul>
         *     <li> Level.ALL
         *     <li> Level.DEBUG
         *     <li> Level.ERROR
         *     <li> Level.FATAL
         *     <li> Level.INFO
         *     <li> Level.OFF (the default)
         *     <li> Level.TRACE
         *     <li> Level.TRACE_INT
         *     <li> Level.WARN
         * </ul>
         *
         * If `OFF` is given, and if logging properties are provided by the user
         * (via log4j.properties or other files), those properties will be
         * respected.  If the user set logging level is not the default log
         * level, i.e. `OFF`), then the programmatically set level will be used
         * instead of the one set in the properties file.
         *
         * @return  the current {@link Options} instance
         *
         * @see #getLoggingLevel()
         */
        public Options setLoggingLevel(Level value) {
            this.loggingLevel = value;
            return this;
        }

    }  // end class Options

    /**
     * An exception that occurred during the submission of a request to GPUdb.
     */
    public static final class SubmitException extends GPUdbException {
        private static final long serialVersionUID = 1L;

        private final URL url;
        private final transient IndexedRecord request;
        private final int requestSize;

        private SubmitException(URL url, IndexedRecord request, int requestSize, String message) {
            super(message);
            this.url = url;
            this.request = request;
            this.requestSize = requestSize;
        }

        private SubmitException(URL url, IndexedRecord request, int requestSize, String message, Throwable cause) {
            super(message, cause);
            this.url = url;
            this.request = request;
            this.requestSize = requestSize;
        }

        private SubmitException(URL url, IndexedRecord request, int requestSize, String message, boolean connectionFailure) {
            super(message, connectionFailure);
            this.url = url;
            this.request = request;
            this.requestSize = requestSize;
        }

        private SubmitException(URL url, IndexedRecord request, int requestSize, String message, Throwable cause, boolean connectionFailure) {
            super(message, cause, connectionFailure);
            this.url = url;
            this.request = request;
            this.requestSize = requestSize;
        }

        /**
         * Gets the URL that the failed request was submitted to, or
         * {@code null} if multiple failover URLs all failed.
         *
         * @return  the URL
         */
        public URL getURL() {
            return url;
        }

        /**
         * Gets the failed request.
         *
         * @return  the request
         */
        public IndexedRecord getRequest() {
            return request;
        }

        /**
         * Gets the size in bytes of the encoded failed request, or -1 if the
         * request was not yet encoded at the time of failure.
         *
         * @return  the size of the encoded request
         */
        public int getRequestSize() {
            return requestSize;
        }
    }   // end class SubmitException


    /**
     * A special exception indicating the server is shutting down
     */
    public class GPUdbExitException extends GPUdbException {
        /**
         * Creates a new {@link GPUdbExitException} with the specified message.
         *
         * @param message  the message
         */
        public GPUdbExitException(String message) {
            super(message);
        }

        /**
         * Creates a new {@link GPUdbExitException} with the specified message and
         * cause.
         *
         * @param message  the message
         * @param cause    the cause
         */
        public GPUdbExitException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static final class GPUdbHAUnavailableException extends GPUdbException {
        /**
         * Creates a new {@link GPUdbHAUnavailableException} with the specified message.
         *
         * @param message  the message
         */
        public GPUdbHAUnavailableException(String message) {
            super(message);
        }

        /**
         * Creates a new {@link GPUdbHAUnavailableException} with the specified message and
         * cause.
         *
         * @param message  the message
         * @param cause    the cause
         */
        public GPUdbHAUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }


    public static final class GPUdbFailoverDisabledException extends GPUdbException {
        /**
         * Creates a new {@link GPUdbFailoverDisabledException} with the specified message.
         *
         * @param message  the message
         */
        public GPUdbFailoverDisabledException(String message) {
            super(message);
        }

        /**
         * Creates a new {@link GPUdbFailoverDisabledException} with the specified message and
         * cause.
         *
         * @param message  the message
         * @param cause    the cause
         */
        public GPUdbFailoverDisabledException(String message, Throwable cause) {
            super(message, cause);
        }
    }


    public static final class GPUdbHostnameRegexFailureException extends GPUdbException {
        /**
         * Creates a new {@link GPUdbHostnameRegexFailureException} with the specified message.
         *
         * @param message  the message
         */
        public GPUdbHostnameRegexFailureException(String message) {
            super(message);
        }

        /**
         * Creates a new {@link GPUdbHostnameRegexFailureException} with the specified message and
         * cause.
         *
         * @param message  the message
         * @param cause    the cause
         */
        public GPUdbHostnameRegexFailureException(String message, Throwable cause) {
            super(message, cause);
        }
    }



    /**
     * Indicates that there is an authorization-related problem occurred.
     */
    public class GPUdbUnauthorizedAccessException extends GPUdbException {
        /**
         * Creates a new {@link GPUdbUnauthorizedAccessException} with the
         * specified message.
         *
         * @param message  the message
         */
        public GPUdbUnauthorizedAccessException(String message) {
            super(message);
        }

        /**
         * Creates a new {@link GPUdbUnauthorizedAccessException} with the
         * specified message and cause.
         *
         * @param message  the message
         * @param cause    the cause
         */
        public GPUdbUnauthorizedAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }



    /**
     * A enumeration of high-availability (inter-cluster) failover order.
     */
    public enum HAFailoverOrder {
        // Randomly choose the next cluster to failover to.  This is the
        // default behavior
        RANDOM,
        // Select the clusters in the order listed by the user (or returned
        // by the server when performing auto-discovery)
        SEQUENTIAL;
    }


    /**
     * A enumeration of high-availability synchronicity override modes.
     */
    public enum HASynchronicityMode {
        // No override; defer to the HA process for synchronizing
        // endpoints (which has different logic for different endpoints)
        DEFAULT( "default" ),
        // Do not replicate the endpoint calls
        NONE( "REPL_NONE" ),
        // Synchronize all endpoint calls
        SYNCHRONOUS( "REPL_SYNC" ),
        // Do NOT synchronize any endpoint call
        ASYNCHRONOUS( "REPL_ASYNC" );

        private String syncMode;

        HASynchronicityMode( String syncMode ) {
            this.syncMode = syncMode;
        }

        public String getMode() {
            return this.syncMode;
        }
    }


    /**
     * Pass-through OutputStream that counts the number of bytes written to it
     * (for diagnostic purposes).
     */
    private static final class CountingOutputStream extends OutputStream {
        private final OutputStream outputStream;
        private int byteCount = 0;

        public CountingOutputStream(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        public int getByteCount() {
            return byteCount;
        }

        @Override
        public void close() throws IOException {
            outputStream.close();
        }

        @Override
        public void flush() throws IOException {
            outputStream.flush();
        }

        @Override
        public void write(byte[] b) throws IOException {
            outputStream.write(b);
            byteCount += b.length;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            outputStream.write(b, off, len);
            byteCount += len;
        }

        @Override
        public void write(int b) throws IOException {
            outputStream.write(b);
            byteCount++;
        }
    } // end class CountingOutputStream

    /**
     * Gets the version number of the GPUdb Java API.
     *
     * @return  the version number
     */
    public static String getApiVersion() {
        try {
            InputStream stream = GPUdbBase.class.getResourceAsStream("/gpudb-api-build.properties");

            if (stream != null) {
                Properties properties = new Properties();
                properties.load(stream);
                return properties.getProperty("version") + "-" + properties.getProperty("buildNumber");
            }
        } catch (IOException ex) {
        }

        return "unknown";
    }

    // Internal Helper

    static URL appendPathToURL(URL url, String path) throws MalformedURLException {
        String newPath = url.getPath();

        if (newPath.endsWith("/")) {
            newPath += path.substring(1);
        } else {
            newPath += path;
        }

        return new URL(url.getProtocol(), url.getHost(), url.getPort(), newPath);
    }

    // Parameter Helpers

    /**
     * A utility method for creating a list of objects inline.
     * Can be used with certain GPUdb requests for specifying lists of strings
     * or other objects.
     *
     * @param <T>     the type of objects in the list
     * @param values  the objects to be added to the list
     * @return        the list
     */
    @SafeVarargs
    public static <T> List<T> list(T... values) {
        return Arrays.asList(values);
    }

    /**
     * A utility method for creating a map of
     * strings to strings} inline. Can be used with certain GPUdb requests for
     * specifying additional named parameters. An even number of string values
     * must be specified; the first in each pair is the key for a map entry,
     * and the second is the value.
     *
     * @param values  an even number of string values
     * @return        the map
     *
     * @throws IllegalArgumentException if an odd number of values is specified
     */
    public static Map<String, String> options(String... values) {
        if (values.length % 2 != 0) {
            throw new IllegalArgumentException("Missing value for last key.");
        }

        LinkedHashMap<String, String> result = new LinkedHashMap<>();

        for (int i = 0; i < values.length; i += 2) {
            result.put(values[i], values[i + 1]);
        }

        return result;
    }

    // Constants

    /**
     * Constant used with certain requests to indicate that the maximum allowed
     * number of results should be returned.
     */
    public static final long END_OF_SET = -9999;
    private static final String DB_HM_OFFLINE_ERROR_MESSAGE         = "System is offline";
    private static final String DB_OFFLINE_ERROR_MESSAGE            = "Kinetica is offline";
    private static final String DB_CONNECTION_RESET_ERROR_MESSAGE   = "Connection reset";
    private static final String DB_CONNECTION_REFUSED_ERROR_MESSAGE = "Connection refused";
    private static final String DB_EXITING_ERROR_MESSAGE            = "Kinetica is exiting";
    private static final String DB_SYSTEM_LIMITED_ERROR_MESSAGE     = "system-limited-fatal";
    private static final String DB_EOF_FROM_SERVER_ERROR_MESSAGE    = "Unexpected end of file from server";

    // Endpoints
    private static final String ENDPOINT_SHOW_SYSTEM_STATUS     = "/show/system/status";
    private static final String ENDPOINT_SHOW_SYSTEM_PROPERTIES = "/show/system/properties";

    // Constants used in endpoint responses
    private static final String SHOW_SYSTEM_STATUS_RESPONSE_SYSTEM  = "system";
    private static final String SHOW_SYSTEM_STATUS_RESPONSE_STATUS  = "status";
    private static final String SHOW_SYSTEM_STATUS_RESPONSE_RUNNING = "running";
    private static final String SHOW_SYSTEM_STATUS_RESPONSE_LEADERLESS = "leaderless";
    private static final String SHOW_SYSTEM_STATUS_RESPONSE_TRUE       = "true";
    private static final String SHOW_SYSTEM_STATUS_RESPONSE_CLUSTER_OPERATION_RUNNING = "cluster_operation_running";
    private static final String SHOW_SYSTEM_STATUS_RESPONSE_CLUSTER_OPERATION_STATUS  = "cluster_operation_status";
    private static final String SHOW_SYSTEM_STATUS_RESPONSE_CLUSTER_IRRECOVERABLE     = "irrecoverable";

    private static final String SYSTEM_PROPERTIES_RESPONSE_HM_PORT         = "conf.hm_http_port";
    private static final String SYSTEM_PROPERTIES_RESPONSE_HEAD_FAILOVER   = "conf.np1.enable_head_failover";
    private static final String SYSTEM_PROPERTIES_RESPONSE_WORKER_FAILOVER = "conf.np1.enable_worker_failover";
    private static final String SYSTEM_PROPERTIES_RESPONSE_NUM_HOSTS       = "conf.number_of_hosts";
    private static final String SYSTEM_PROPERTIES_RESPONSE_USE_HTTPS       = "conf.use_https";
    private static final String SYSTEM_PROPERTIES_RESPONSE_HEAD_NODE_URLS  = "conf.ha_ring_head_nodes_full";
    private static final String SYSTEM_PROPERTIES_RESPONSE_SERVER_URLS     = "conf.worker_http_server_urls";
    private static final String SYSTEM_PROPERTIES_RESPONSE_TRUE            = "TRUE";

    // Internally used headers (make sure to add them to PROTECTED_HEADERS
    protected static final String HEADER_HA_SYNC_MODE  = "X-Kinetica-Group";
    protected static final String HEADER_AUTHORIZATION = "Authorization";
    protected static final String HEADER_CONTENT_TYPE  = "Content-type";

    // Headers that are prt
    protected static final String[] PROTECTED_HEADERS = new String[]{ HEADER_HA_SYNC_MODE,
                                                                      HEADER_AUTHORIZATION,
                                                                      HEADER_CONTENT_TYPE
    };



    // Internal classes
    // ----------------

    /**
     * Helper class which contains all possible address related information
     * for a given Kinetica cluster.  Used to keep track of the multiple
     * Kinetica clusters' addresses.
     */
    private class ClusterAddressInfo {
        // Members
        private URL           activeHeadNodeUrl;
        private List<URL>     workerRankUrls;
        private Set<String>   hostNames;  // could have IPs, too
        private URL           hostManagerUrl;
        private boolean       isPrimaryCluster;
        private boolean       isIntraClusterFailoverEnabled;

        // Constructors
        // ------------
        protected ClusterAddressInfo( URL activeHeadNodeUrl,
                                      List<URL> workerRankUrls,
                                      Set<String> hostNames,
                                      URL hostManagerUrl,
                                      boolean isPrimaryCluster,
                                      boolean isIntraClusterFailoverEnabled ) throws GPUdbException {
            this.activeHeadNodeUrl = activeHeadNodeUrl;
            this.workerRankUrls    = workerRankUrls;
            this.hostNames         = hostNames;
            this.hostManagerUrl    = hostManagerUrl;
            this.isPrimaryCluster  = isPrimaryCluster;
            this.isIntraClusterFailoverEnabled = isIntraClusterFailoverEnabled;

            // Ensure that all the known ranks' hostnames are also accounted for
            updateHostnamesBasedOnRankUrls();
        }

        /// As close to a default constructor as we can get...
        protected ClusterAddressInfo( URL activeHeadNodeUrl ) throws GPUdbException {
            this.activeHeadNodeUrl    = activeHeadNodeUrl;

            // Set default values for the rest of the members
            this.workerRankUrls   = new ArrayList<URL>();
            this.hostNames        = new HashSet<String>();
            this.isPrimaryCluster = false;
            this.isIntraClusterFailoverEnabled = false;

            // Create a host manager URL with the Kinetica default port for
            // host managers
            try {
                if ( !activeHeadNodeUrl.getPath().isEmpty() ) {
                    // If we're using HTTPD, then use the appropriate URL
                    // (likely, http[s]://hostname_or_IP:port/gpudb-host-manager)
                    // Also, use the default httpd port (8082, usually)
                    this.hostManagerUrl = new URL( activeHeadNodeUrl.getProtocol(),
                                                   activeHeadNodeUrl.getHost(),
                                                   DEFAULT_HTTPD_HOST_MANAGER_PORT,
                                                   "/gpudb-host-manager" );
                } else {
                    // The host manager URL shouldn't use any path and
                    // use the host manager port
                    this.hostManagerUrl = new URL( activeHeadNodeUrl.getProtocol(),
                                                   activeHeadNodeUrl.getHost(),
                                                   DEFAULT_HOST_MANAGER_PORT,
                                                   "" );
                }
            } catch ( MalformedURLException ex ) {
                throw new GPUdbException( "Error in creating the host manager URL: "
                                          + ex.getMessage(),
                                          ex );
            }

            // Ensure that all the known ranks' hostnames are also accounted for
            updateHostnamesBasedOnRankUrls();
        }

        // Allow a host manager port along with the active URL
        protected ClusterAddressInfo( URL activeHeadNodeUrl,
                                      int hostManagerPort ) throws GPUdbException {
            this.activeHeadNodeUrl = activeHeadNodeUrl;

            // Set default values for the rest of the members
            this.workerRankUrls   = new ArrayList<URL>();
            this.hostNames        = new HashSet<String>();
            this.isPrimaryCluster = false;
            this.isIntraClusterFailoverEnabled = false;

            // Create a host manager URL with the given port for host managers
            try {
                if ( !activeHeadNodeUrl.getPath().isEmpty() ) {
                    // If we're using HTTPD, then use the appropriate URL
                    // (likely, http[s]://hostname_or_IP:port/gpudb-host-manager)
                    // Also, use the default httpd port (8082, usually)
                    this.hostManagerUrl = new URL( activeHeadNodeUrl.getProtocol(),
                                                   activeHeadNodeUrl.getHost(),
                                                   hostManagerPort,
                                                   "/gpudb-host-manager" );
                } else {
                    // The host manager URL shouldn't use any path and
                    // use the host manager port
                    this.hostManagerUrl = new URL( activeHeadNodeUrl.getProtocol(),
                                                   activeHeadNodeUrl.getHost(),
                                                   hostManagerPort,
                                                   "" );
                }
            } catch ( MalformedURLException ex ) {
                throw new GPUdbException( "Error in creating the host manager URL: "
                                          + ex.getMessage(),
                                          ex );
            }

            // Ensure that all the known ranks' hostnames are also accounted for
            updateHostnamesBasedOnRankUrls();
        }


        // Getters
        // -------
        public URL getActiveHeadNodeUrl() {
            return this.activeHeadNodeUrl;
        }

        public List<URL> getWorkerRankUrls() {
            return this.workerRankUrls;
        }

        public Set<String> getHostNames() {
            return this.hostNames;
        }

        public URL getHostManagerUrl() {
            return this.hostManagerUrl;
        }

        public boolean getIsPrimaryCluster() {
            return this.isPrimaryCluster;
        }

        public boolean getIsIntraClusterFailoverEnabled() {
            return this.isIntraClusterFailoverEnabled;
        }

        /**
         * Another getter for the primary cluster boolean flag for convenience.
         */
        public boolean isPrimaryCluster() {
            return this.isPrimaryCluster;
        }

        /**
         * Another getter for the N+1 failover boolean flag for convenience.
         */
        public boolean isIntraClusterFailoverEnabled() {
            return this.isIntraClusterFailoverEnabled;
        }

        // Setters
        // -------

        /**
         * Set the active head node URL.  Return this object to be able to
         * chain operations.
         */
        public ClusterAddressInfo setActiveHeadNodeUrl( URL value ) {
            this.activeHeadNodeUrl = value;
            return this;
        }

        /**
         * Set the worker rank URLs.  Return this object to be able to
         * chain operations.
         */
        public ClusterAddressInfo setWorkerRankUrls( List<URL> value ) {
            this.workerRankUrls = value;
            return this;
        }

        /**
         * Set the list of host names for all available machines (whether active
         * or passive).  Return this object to be able to chain operations.
         */
        public ClusterAddressInfo setHostNames( Set<String> value ) {
            this.hostNames = value;
            return this;
        }

        /**
         * Set the host manager URL.  Return this object to be able to
         * chain operations.
         */
        public ClusterAddressInfo setHostManagerUrl( URL value ) {
            this.hostManagerUrl = value;
            return this;
        }

        /**
         * Set whether this cluster is the primary one.  Return this object to
         * be able to chain operations.
         */
        public ClusterAddressInfo setIsPrimaryCluster( boolean value ) {
            this.isPrimaryCluster = value;
            return this;
        }

        /**
         * Set whether this cluster has N+1 failover enabled.  Return this object to
         * be able to chain operations.
         */
        public ClusterAddressInfo setIsIntraClusterFailoverEnabled( boolean value ) {
            this.isIntraClusterFailoverEnabled = value;
            return this;
        }


        // Private Helper Methods
        // ----------------------
        /**
         * Add the hostnames of the head and worker ranks URLs to the
         * list of hostnames if they are not already part of it.
         */
        private void updateHostnamesBasedOnRankUrls() {
            // Put the head rank's hostname in the saved hostnames (only if
            // it doesn't exist there already)
            if ( !doesClusterContainNode( this.activeHeadNodeUrl.getHost() ) ) {
                String headRankHostname = (this.activeHeadNodeUrl.getProtocol()
                                           + "://"
                                           + this.activeHeadNodeUrl.getHost() );
                GPUdbLogger.debug_with_info( "Adding head rank's hostname to "
                                             + "hostname list: "
                                             + headRankHostname );
                this.hostNames.add( headRankHostname );
            }

            // Put each worker rank's hostname in the saved hostnames (only if
            // it doesn't exist there already)
            Iterator<URL> iter = this.workerRankUrls.iterator();
            while ( iter.hasNext() ) {
                URL workerRank = iter.next();
                // Check if this worker rank's host is already accounted for
                if( !doesClusterContainNode( workerRank.getHost() ) ) {
                    String workerRankHostname = ( workerRank.getProtocol()
                                                 + "://"
                                                 + workerRank.getHost() );
                    GPUdbLogger.debug_with_info( "Adding worker rank's hostname to "
                                                 + "hostname list: "
                                                 + workerRankHostname );
                    // Add the worker rank's hostname to the list
                    this.hostNames.add( workerRankHostname );
                }
            }
        }


        // Convenience Methods
        // --------------------
        /**
         * Checks if the given hostname (or IP address) is part of this
         * cluster.
         *
         * @return true if this cluster contains a machine with the given
         *          hostname or IP address, false otherwise.
         */
        public boolean doesClusterContainNode( String hostName ) {
            GPUdbLogger.debug_with_info( "Begin checking for hostname: " + hostName);
            Iterator<String> iter = this.hostNames.iterator();
            while ( iter.hasNext() ) {
                // We need to check for a string subset match since the
                // hostnames contain the protocol as well as the actual hostname
                // or IP address
                String hostname_ = iter.next();
                GPUdbLogger.debug_with_info( "Checking for match with '"
                                             + hostname_ + "'" );
                if( hostname_.contains( hostName ) ) {
                    GPUdbLogger.debug_with_info( "Found matching hostname");
                    return true;
                }
                else    GPUdbLogger.debug_with_info( "Did NOT find matching hostname");
            }
            GPUdbLogger.debug_with_info( "No match; returning false");
            return false; // found no match
        }

        // Overridden methods
        // ------------------
        @Override
        public boolean equals(Object obj) {
            if( obj == this ) {
                return true;
            }

            if( (obj == null) || (obj.getClass() != this.getClass()) ) {
                return false;
            }

            ClusterAddressInfo that = (ClusterAddressInfo)obj;

            return ( this.activeHeadNodeUrl.equals( that.activeHeadNodeUrl )
                     // The order of the worker ranks matter
                     && this.workerRankUrls.equals( that.workerRankUrls )
                     // The order of the hostnames do NOT matter
                     && this.hostNames.equals( that.hostNames )
                     && (this.isPrimaryCluster == that.isPrimaryCluster)
                     && (this.isIntraClusterFailoverEnabled
                         == that.isIntraClusterFailoverEnabled) );
        }


        @Override
        public int hashCode() {
            int hashCode = 1;
            hashCode = (31 * hashCode) + this.activeHeadNodeUrl.hashCode();
            hashCode = (31 * hashCode) + this.workerRankUrls.hashCode();
            hashCode = (31 * hashCode) + this.hostNames.hashCode();
            hashCode = (31 * hashCode) + this.hostManagerUrl.hashCode();
            // Java uses 1231 for true and 1237 for false for boolean hashcodes
            // https://docs.oracle.com/javase/7/docs/api/java/lang/Boolean.html#hashCode()
            hashCode = (31 * hashCode) + ( this.isPrimaryCluster ? 1231 : 1237 );
            hashCode = (31 * hashCode) + ( this.isIntraClusterFailoverEnabled
                                           ? 1231 : 1237 );
            return hashCode;
        }


        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append( "{ activeHeadNodeUrl: " );
            builder.append( this.activeHeadNodeUrl.toString() );
            builder.append( ", workerRankUrls: " );
            builder.append( java.util.Arrays.toString( this.workerRankUrls.toArray() ) );
            builder.append( ", hostNames: " );
            builder.append( java.util.Arrays.toString( this.hostNames.toArray() ) );
            builder.append( ", hostManagerUrl: " );
            builder.append( this.hostManagerUrl );
            builder.append( ", isPrimaryCluster: " + this.isPrimaryCluster );
            builder.append( ", isIntraClusterFailoverEnabled: " );
            builder.append( this.isIntraClusterFailoverEnabled );
            builder.append( " }" );

            return builder.toString();
        }
    }   // end class ClusterAddressInfo


    // Fields

    private List<ClusterAddressInfo> hostAddresses;
    private Options       options;
    private String        primaryUrlHostname = "";
    private final Object  urlLock;
    private List<Integer> haUrlIndices;
    private int           currentClusterIndexPointer;
    private int           numClusterSwitches;
    private String        username;
    private String        password;
    private String        authorization;
    private Pattern       hostnameRegex;
    private boolean       useHttpd = false;
    private boolean       useSnappy;
    private boolean       bypassSslCertCheck;
    private boolean       disableFailover;
    private boolean       disableAutoDiscovery;
    private int           threadCount;
    private int           timeout;
    private int           hostManagerPort;
    private int           clusterReconnectCount;
    private long          intraClusterFailoverTimeoutNS;
    private long          initialConnectionAttemptTimeoutNS;
    private ExecutorService     executor;
    private Map<String, String> httpHeaders;
    private HASynchronicityMode haSyncMode;
    private HAFailoverOrder     haFailoverOrder;
    private CloseableHttpClient httpClient;
    private ConcurrentHashMap<Class<?>, TypeObjectMap<?>> knownTypeObjectMaps;
    private ConcurrentHashMap<String, Object> knownTypes;


    // Constructors
    // ------------

    protected GPUdbBase(String url, Options options) throws GPUdbException {
        urlLock = new Object();

        // Initialize the logger before anything else.  This MUST be donce
        // before any logging happens!
        GPUdbLogger.initializeLogger( options.getLoggingLevel() );

        if ( url == null ) {
            String errorMsg = ( "Must provide a non-null and non-empty "
                                + "string for the URL; given null" );
            GPUdbLogger.error( errorMsg );
            throw new GPUdbException( errorMsg );
        }

        List<URL> urls;
        try {
            // Not using an unmodifiable list because we'll have to update it
            // with the HA ring head node addresses
            urls = new ArrayList<URL>();

            // Split the string on commas, if any
            String[] url_strings = url.split(",");
            for (int i = 0; i < url_strings.length; ++i ) {
                urls.add( new URL( url_strings[i] ) );
            }
        } catch (MalformedURLException ex) {
            GPUdbLogger.error( ex.getMessage() );
            throw new GPUdbException(ex.getMessage(), ex);
        }

        init( urls, options );
    }

    protected GPUdbBase(URL url, Options options) throws GPUdbException {
        urlLock = new Object();

        // Initialize the logger before anything else.  This MUST be donce
        // before any logging happens!
        GPUdbLogger.initializeLogger( options.getLoggingLevel() );

        if ( url == null ) {
            String errorMsg = "Must provide at least one URL; gave none!";
            GPUdbLogger.error( errorMsg );
            throw new GPUdbException( errorMsg );
        }

        // Not using an unmodifiable list because we'll have to update it
        // with the HA ring head node addresses
        init( list( url ), options );
    }

    protected GPUdbBase(List<URL> urls, Options options) throws GPUdbException {
        urlLock = new Object();

        // Initialize the logger before anything else.  This MUST be donce
        // before any logging happens!
        GPUdbLogger.initializeLogger( options.getLoggingLevel() );

        if ( urls.isEmpty() ) {
            String errorMsg = "Must provide at least one URL; gave none!";
            GPUdbLogger.error( errorMsg );
            throw new GPUdbException( errorMsg );
        }

        init( urls, options );
    }


    /**
     * Construct the GPUdbBase object based on the given URLs and options.
     */
    private void init(List<URL> urls, Options options) throws GPUdbException {
        // Save the options object
        this.options  = options;

        // Save some parameters passed in via the options object
        this.username      = options.getUsername();
        this.password      = options.getPassword();
        this.hostnameRegex = options.getHostnameRegex();

        if ((username != null && !username.isEmpty()) || (password != null && !password.isEmpty())) {
            authorization = ("Basic "
                             + Base64.encodeBase64String( ((username != null ? username : "")
                                                           + ":"
                                                           + (password != null ? password : "")).getBytes() )
                               .replace("\n", "") );
        } else {
            authorization = null;
        }

        // Save various options
        this.useSnappy            = options.getUseSnappy();
        this.disableFailover      = options.getDisableFailover();
        this.disableAutoDiscovery = options.getDisableAutoDiscovery();
        this.threadCount          = options.getThreadCount();
        this.executor             = options.getExecutor();
        this.timeout              = options.getTimeout();
        this.hostManagerPort      = options.getHostManagerPort();
        this.haFailoverOrder   = options.getHAFailoverOrder();
        this.clusterReconnectCount             = options.getClusterReconnectCount();
        this.initialConnectionAttemptTimeoutNS = options.getInitialConnectionAttemptTimeout();
        this.intraClusterFailoverTimeoutNS     = options.getIntraClusterFailoverTimeout();

        // Convert the initial connection attempt timeout from milliseconds
        // to nano seconds
        GPUdbLogger.debug_with_info( "Initial connection attempt timeout in ms: "
                                     + this.initialConnectionAttemptTimeoutNS);
        this.initialConnectionAttemptTimeoutNS = this.initialConnectionAttemptTimeoutNS * 1000000L;
        GPUdbLogger.debug_with_info( "Initial connection attempt timeout in ns: "
                                     + this.initialConnectionAttemptTimeoutNS);

        // Convert the intra-cluster failover recovery timeout from milliseconds
        // to nano seconds
        GPUdbLogger.debug_with_info( "Intra-cluster failover timeout in ms: "
                                     + this.intraClusterFailoverTimeoutNS);
        this.intraClusterFailoverTimeoutNS = this.intraClusterFailoverTimeoutNS * 1000000L;
        GPUdbLogger.debug_with_info( "Intra-cluster failover timeout in ns:"
                                     + this.intraClusterFailoverTimeoutNS
                                     + " (0 means infinite waiting)");

        // Handle SSL certificate verification bypass for HTTPS connections
        this.bypassSslCertCheck = options.getBypassSslCertCheck();
        if ( this.bypassSslCertCheck ) {
            // This bypass works only for HTTPS connections
            try {
                X509TrustManagerBypass.install();
            } catch (GeneralSecurityException ex) {
                // Not doing anything about it since we're trying to bypass
                // to reduce distractions anyway
            }
        }

        // The headers must be set before any call can be made
        httpHeaders = new HashMap<>();

        for (Map.Entry<String, String> entry : options.getHttpHeaders().entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                httpHeaders.put(entry.getKey(), entry.getValue());
            }
        }

        // Initialize the caches for table types
        knownTypeObjectMaps = new ConcurrentHashMap<>(16, 0.75f, 1);
        knownTypes = new ConcurrentHashMap<>(16, 0.75f, 1);

        // Set the default sync mode
        this.haSyncMode = HASynchronicityMode.DEFAULT;

        // Instantiate the list of HA URL indices around for easy URL picking
        this.haUrlIndices = new ArrayList<>();

        // We haven't switched to any cluster yet
        this.numClusterSwitches = 0;


        // Initiate the HttpClient object
        // ------------------------------
        // Create a socket factory in order to use an http connection manager
        SSLConnectionSocketFactory secureSocketFactory = SSLConnectionSocketFactory.getSocketFactory();
        if ( this.bypassSslCertCheck ) {
            // Allow self-signed certs
            SSLContext sslContext = null;
            try {
                sslContext = SSLContextBuilder
                    .create()
                    .loadTrustMaterial( new TrustSelfSignedStrategy() )
                    .build();
            } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException ex) {
            }

            // Disable hostname verification
            HostnameVerifier allowAllHosts = new NoopHostnameVerifier();

            // Create the appropriate SSL socket factory
            if ( sslContext != null ) {
                secureSocketFactory = new SSLConnectionSocketFactory( sslContext, allowAllHosts );
            }
        }

        // And a plain http socket factory
        PlainConnectionSocketFactory plainSocketFactory = PlainConnectionSocketFactory.getSocketFactory();
        Registry<ConnectionSocketFactory> connSocketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
            .register("https", secureSocketFactory)
            .register("http", plainSocketFactory)
            .build();

        // Create a connection pool manager
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager( connSocketFactoryRegistry );
        connectionManager.setMaxTotal( options.getMaxTotalConnections() );
        connectionManager.setDefaultMaxPerRoute( options.getMaxConnectionsPerHost() );
        connectionManager.setValidateAfterInactivity( options.getConnectionInactivityValidationTimeout() );

        // Set the timeout defaults
        RequestConfig requestConfig = RequestConfig.custom()
            .setSocketTimeout( this.timeout )
            .setConnectTimeout( options.getServerConnectionTimeout() )
            .setConnectionRequestTimeout( this.timeout )
            .build();

        // Build the http client.
        this.httpClient = HttpClients.custom()
            .setConnectionManager( connectionManager )
            .setDefaultRequestConfig( requestConfig )
            .build();

        // Parse the user given URL(s) and store information on all clusters
        // running Kinetica--automatically discovers the HA ring head nodes,
        // takes care of the primary URL, and randomizes the backup clusters
        GPUdbLogger.debug_with_info( "Before parsing URLs" );
        parseUrls( urls );
        GPUdbLogger.debug_with_info( "After parsing URLs; ring size " + getHARingSize() + " this.haUrlIndices size " + this.haUrlIndices.size() );
    }   // end init


    /**
     *  Clean up resources--namely, the HTTPClient object(s).
     */
    protected void finalize() throws Throwable {
        // Release the resources-- the HTTP client and connection manager
        this.httpClient.getConnectionManager().shutdown();
        httpClient.close();
    }


    // Properties
    // ----------

    /**
     * Gets the list of URLs of the active head ranks of all the clusters for
     * the GPUdb server. At any given time, one URL will be active and used for
     * all GPUdb calls (call {@link #getURL getURL} to determine which one), but
     * in the event of failure, the other URLs will be tried in order, and if a
     * working one is found it will become the new active URL.
     *
     * @return  the list of URLs
     */
    public List<URL> getURLs() {
        List<URL> activeHeadNodeURLs = new ArrayList<URL>();
        for (int i = 0; i < this.hostAddresses.size(); ++i) {
            activeHeadNodeURLs.add( this.hostAddresses.get( i )
                                    .getActiveHeadNodeUrl() );
        }
        return activeHeadNodeURLs;
    }

    /**
     * Gets the active URL of the GPUdb server.
     *
     * @return  the URL
     */
    public URL getURL() {
        if ( getHARingSize() == 1 ) {
            return this.hostAddresses.get( 0 ).getActiveHeadNodeUrl();
        } else {
            synchronized (urlLock) {
                int currClusterIndex = getCurrentClusterIndex();
                return this.hostAddresses.get( currClusterIndex ).getActiveHeadNodeUrl();
            }
        }
    }

    /**
     * Gets the list of URLs for the GPUdb host manager. At any given time, one
     * URL will be active and used for all GPUdb calls (call {@link #getHmURL
     * getHmURL} to determine which one), but in the event of failure, the
     * other URLs will be tried in order, and if a working one is found
     * it will become the new active URL.
     *
     * @return  the list of URLs
     */
    public List<URL> getHmURLs() {
        List<URL> hmURLs = new ArrayList<URL>();
        for (int i = 0; i < this.hostAddresses.size(); ++i) {
            hmURLs.add( this.hostAddresses.get( i )
                        .getHostManagerUrl() );
        }
        return hmURLs;
    }

    /**
     * Gets the active URL of the GPUdb host manager.
     *
     * @return  the URL
     */
    public URL getHmURL() {
        if ( getHARingSize() == 1 ) {
            return this.hostAddresses.get( 0 ).getHostManagerUrl();
        } else {
            synchronized (urlLock) {
                int currClusterIndex = getCurrentClusterIndex();
                return this.hostAddresses.get( currClusterIndex ).getHostManagerUrl();
            }
        }
    }

    /**
     * Gets the URL of the head node of the primary cluster of the HA
     * environment, if any cluster is identified as the primary cluster.
     *
     * @return  the primary URL hostname or IP address if there is
     *          a primary cluster, null otherwise.
     */
    public URL getPrimaryUrl() {
        if ( !this.primaryUrlHostname.isEmpty() ) {
            return this.hostAddresses.get( 0 ).getActiveHeadNodeUrl();
        }
        return null;
    }


    /**
     * Gets the current high availability synchronicity override mode.
     *
     * @return the ha synchronicity override mode
     *
     * @see GPUdbBase#setHASyncMode(HASynchronicityMode)
     */
    public HASynchronicityMode getHASyncMode() {
        return this.haSyncMode;
    }

    /**
     * Gets the username used for authentication to GPUdb. Will be an empty
     * string if none was provided to the {@link GPUdb#GPUdb(String,
     * GPUdbBase.Options) GPUdb constructor} via {@link Options}.
     *
     * @return  the username
     *
     * @see Options#setUsername(String)
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the password used for authentication to GPUdb. Will be an empty
     * string if none was provided to the {@link GPUdb#GPUdb(String,
     * GPUdbBase.Options) GPUdb constructor} via {@link Options}.
     *
     * @return  the password
     *
     * @see Options#setPassword(String)
     */
    public String getPassword() {
        return password;
    }

    /**
     * Gets the value of the flag indicating whether Snappy compression will be
     * used for certain GPUdb requests that potentially submit large amounts of
     * data. Will be {@code false} if not overridden using the {@link
     * GPUdb#GPUdb(String, GPUdbBase.Options) GPUdb constructor} via {@link
     * Options}.
     *
     * @return  the value of the Snappy compression flag
     *
     * @see Options#setUseSnappy(boolean)
     */
    public boolean getUseSnappy() {
        return useSnappy;
    }

    /**
     * Gets the number of threads used during data encoding and decoding
     * operations. Will be one if not overridden using the {@link
     * GPUdb#GPUdb(String, GPUdbBase.Options) GPUdb constructor} via {@link
     * Options}.
     *
     * @return  the number of threads
     *
     * @see Options#setThreadCount(int)
     */
    public int getThreadCount() {
        return threadCount;
    }

    /**
     * Gets the {@link ExecutorService executor service} used for managing
     * threads during data encoding and decoding operations. Will be
     * {@code null} if none was provided to the
     * {@link GPUdb#GPUdb(String, GPUdbBase.Options) GPUdb constructor} via
     * {@link Options}.
     *
     * @return  the executor service
     *
     * @see Options#setExecutor(ExecutorService)
     */
    public ExecutorService getExecutor() {
        return executor;
    }

    /**
     * Gets the map of additional HTTP headers that will be sent to GPUdb with
     * each request. Will be empty if none were provided to the {@link
     * GPUdb#GPUdb(String, GPUdbBase.Options) GPUdb constructor} via {@link
     * Options}.
     *
     * @return  the map
     *
     * @see Options#addHttpHeader(String, String)
     * @see Options#setHttpHeaders(Map)
     */
    public Map<String, String> getHttpHeaders() {
        return httpHeaders;
    }

    /**
     * Gets the timeout value, in milliseconds, after which a lack of
     * response from the GPUdb server will result in requests being aborted.
     * A timeout of zero is interpreted as an infinite timeout. Note that
     * this applies independently to various stages of communication, so
     * overall a request may run for longer than this without being aborted.
     *
     * @return  the timeout value
     *
     * @see Options#setTimeout(int)
     */
    public int getTimeout() {
        return timeout;
    }



    /**
     * Adds an HTTP header to the map of additional HTTP headers to send to
     * GPUdb with each request. If the header is already in the map, its
     * value is replaced with the specified value.  The user is not allowed
     * to modify the following headers:
     * <ul>
     *    <li> Authorization
     *    <li> ha_sync_mode
     *    <li> Content-type
     * </ul>
     *
     * @param header  the HTTP header (cannot be null)
     * @param value   the value of the HTTP header (cannot be null)
     *
     * @see #getHttpHeaders()
     * @see #removeHttpHeader(String)
     */
    public void addHttpHeader(String header, String value) throws GPUdbException {
        // Check for nulls (not allowed)
        if ( (header == null) || (value == null) ) {
            String errorMsg = "'null' not allowed for either header or value!";
            GPUdbLogger.error( errorMsg );
            throw new GPUdbException( errorMsg );
        }

        // Ensure that the given header is not a protecte header
        for ( int i = 0; i < PROTECTED_HEADERS.length; ++i ) {
            if ( header == PROTECTED_HEADERS[ i ] ) {
                String errorMsg = ( "Not allowed to change proteced header: "
                                    + header );
                GPUdbLogger.error( errorMsg );
                throw new GPUdbException( errorMsg );
            }
        }

        this.httpHeaders.put(header, value);
        return;
    }


    /**
     * Removes the given HTTP header from the map of additional HTTP headers to
     * send to GPUdb with each request. The user is not allowed
     * to remove the following headers:
     * <ul>
     *    <li> Authorization
     *    <li> ha_sync_mode
     *    <li> Content-type
     * </ul>
     *
     * @param header  the HTTP header (cannot be null)
     *
     * @see #getHttpHeaders()
     * @see #addHttpHeader(String, String)
     */
    public void removeHttpHeader(String header) throws GPUdbException {
        // Check for nulls (not allowed)
        if ( header == null) {
            String errorMsg = "Need a non-null value for the header; null given!";
            GPUdbLogger.error( errorMsg );
            throw new GPUdbException( errorMsg );
        }

        // Ensure that the given header is not a protecte header
        for ( int i = 0; i < PROTECTED_HEADERS.length; ++i ) {
            if ( header == PROTECTED_HEADERS[ i ] ) {
                String errorMsg = ( "Not allowed to remove proteced header: "
                                    + header );
                GPUdbLogger.error( errorMsg );
                throw new GPUdbException( errorMsg );
            }
        }

        this.httpHeaders.remove( header );
        return;
    }



    /**
     * Sets the current high availability synchronicity override mode.
     * Until it is changed, all subsequent endpoint calls made to the
     * server will maintain the given synchronicity across all clusters
     * in the high availability ring.  When set to {@link
     * HASynchronicityMode#DEFAULT}, normal operation will resume (where
     * the high availability process determines which endpoints will be
     * synchronous and which ones will be asynchronous).
     *
     * @param syncMode  the ha synchronicity override mode
     */
    public void setHASyncMode( HASynchronicityMode syncMode) {
        this.haSyncMode = syncMode;
    }


    /**
     * @deprecated  As of version 7.1.0.0, this method will no longer be
     * functional.  This method will be a no-op, not changing host manager
     * port.  The method will be removed in version 7.2.0.0.
     *
     * @param value  the host manager port number
     * @return       the current {@link GPUdbBase} instance
     */
    @Deprecated public GPUdbBase setHostManagerPort(int value) throws IllegalArgumentException, GPUdbException {
        return this;
    }




    // Helper Functions
    // ----------------

    /**
     * Gets the size of the high availability ring (i.e. how many clusters
     * are in it).
     *
     * @return  the size of the HA cluster
     */
    protected int getHARingSize() {
        return this.hostAddresses.size();
    }

    /**
     * Gets the number of times the client has switched to a different
     * cluster amongst the high availability ring.
     */
    protected int getNumClusterSwitches() {
        return this.numClusterSwitches;
    }

    /**
     * Gets the number of times the client has switched to a different
     * cluster amongst the high availability ring.
     */
    protected void incrementNumClusterSwitches() {
        synchronized ( urlLock ) {
            ++this.numClusterSwitches;
        }
    }


    /**
     * Return the pointer to the current URL index in a thread-safe manner.
     */
    private int getCurrClusterIndexPointer() {
        synchronized ( urlLock ) {
            return this.currentClusterIndexPointer;
        }
    }

    /**
     * Set the pointer to the current URL index in a thread-safe manner.
     */
    private void setCurrClusterIndexPointer(int newIndex) {
        synchronized ( urlLock ) {
            this.currentClusterIndexPointer = newIndex;
        }
    }


    /**
     * Return the index of the current cluster in use in a thread-safe manner.
     */
    private int getCurrentClusterIndex() {
        return this.haUrlIndices.get( getCurrClusterIndexPointer() );
    }


    /**
     * For the given cluster in the HA ring, see if an N+1 event happened in the
     * past and try to recover the current list or URLs super quickly.  If N+1
     * is ongoing, simply return rather than spinning and waiting.
     *
     * @param clusterIndex  The index of the cluster to use
     */
    private synchronized boolean updateClusterAddresses( int clusterIndex ) {
        GPUdbLogger.debug_with_info( "Begin clusterIndex " + clusterIndex);
        // Retrieve info for the given cluster
        ClusterAddressInfo currClusterInfo = this.hostAddresses.get( clusterIndex );

        URL currHeadRankUrl = currClusterInfo.getActiveHeadNodeUrl();
        GPUdbLogger.debug_with_info( "Given cluster's head rank is at " + currHeadRankUrl.toString());

        // Generate the list of the given cluster's rank-0 URL and worker
        // rank URLs
        List<URL> rankUrls = new ArrayList<URL>();
        rankUrls.add( currHeadRankUrl );
        rankUrls.addAll( currClusterInfo.getWorkerRankUrls() );
        GPUdbLogger.debug_with_info( "Cluster info: " + currClusterInfo.toString() );

        // Try to get the new addresses for shuffled ranks from the
        // currently known ranks (whichever ones are still in place)
        for ( int i = 0; i < rankUrls.size(); ++i ) {
            URL url = rankUrls.get( i );
            GPUdbLogger.debug_with_info( "Loop iteration #" + i + "; trying url " + url.toString());

            // Check the system status (if this rank is responding to requests,
            // keep pinging until status is back up to running)
            try {
                JsonNode systemStatusInfo = getSystemStatusInformation( url );

                // Check if this rank is in an irrecoverable state
                if ( isClusterOperationStatusIrrecoverable( systemStatusInfo ) ) {
                    // The system is hosed; there is no hope of recovery!
                    GPUdbLogger.debug_with_info( "Cluster is irrecoverable; returning false ");
                    return false;
                }

                // Check if this rank has become leaderless (i.e. no
                // head host manager can be elected)
                if ( isRankLeaderless( systemStatusInfo ) ) {
                    GPUdbLogger.debug_with_info( "Rank is leaderless; skipping to the next rank ");
                    continue;  // with the next rank
                }

                // Check if the system is back up and running
                if ( isSystemRunning( systemStatusInfo ) ) {
                    GPUdbLogger.debug_with_info( "Cluster is running; getting /show/sys/props ");
                    // System is back up; re-parse the URLs for this cluster

                    // Get the latest system properties of the cluster, if
                    // can't get it, skip to the next one
                    Map<String, String> systemProperties = getSystemProperties( url );
                    ClusterAddressInfo clusterInfoRefreshed = createClusterAddressInfo( url,
                                                                                        systemProperties );

                    GPUdbLogger.debug_with_info( "Current cluster info:   " + currClusterInfo.toString() );
                    GPUdbLogger.debug_with_info( "Refreshed cluster info: " + clusterInfoRefreshed.toString() );

                    // Check if the newly gotten addresses are the same as the old
                    // ones
                    if ( clusterInfoRefreshed.equals( currClusterInfo ) ) {
                        // The addresses have remained the same; so we didn't
                        // make any effective change
                        GPUdbLogger.debug_with_info( "Returning false; obtained addresses are the same as the existing one ");
                        return false;
                    } else {
                        // Replace the stale cluster info with the refreshed one
                        this.hostAddresses.set( clusterIndex, clusterInfoRefreshed );

                        // We actually changed the addresses for this cluster;
                        // the caller should know this
                        GPUdbLogger.debug_with_info( "Returning true; actually changed addresses ");
                        return true;
                    }
                }
            } catch ( GPUdbException ex ) {
                GPUdbLogger.debug_with_info( "Caught GPUdb exception (not doing anything about it): " + ex.getMessage());
                // Simply try the next rank
            }  // end try
        }   // end for

        // We couldn't reset the cluster's addresses
        GPUdbLogger.debug_with_info( "Returning false (could/did not reset the addresses)");
        return false;
    }   // updateClusterAddresses



    /**
     * Given a ClusterAddressInfo object, check that all the worker ranks are up
     * by pinging them individually.  Do this in an infinite loop.
     *
     * **Caution**: Since this method runs in an infinite loop, be very careful
     *              of how to use it.  Ought to only be called from
     *              doIntraClusterFailover().
     */
    private boolean areAllRanksReady( ClusterAddressInfo clusterAddresses ) throws GPUdbException {
        GPUdbLogger.debug_with_info( "Start checking all rank http servers' statuses..." );
        // Generate the list of the given cluster's rank-0 URL and worker
        // rank URLs
        List<URL> rankUrls = new ArrayList<URL>();
        rankUrls.add( clusterAddresses.getActiveHeadNodeUrl() );
        rankUrls.addAll( clusterAddresses.getWorkerRankUrls() );

        // Sleep for a short amount of time (three seconds)
        int sleepInterval = 3000;

        boolean wasSomeRankUnresponsive;
        int numRankCheckAttempt = 0;

        // Keep pinging all ranks until ALL have their http servers up
        while ( true ) {
            // Keep track of if any rank does no respond
            wasSomeRankUnresponsive = false;
            GPUdbLogger.debug_with_info( "Iteration #" + numRankCheckAttempt );

            // Check if all the ranks are up and listening
            for ( int i = 0; i < rankUrls.size(); ++i ) {
                URL url = rankUrls.get( i );
                // Keep pinging this rank until it is up
                boolean keepPingingThisRank = true;
                while ( keepPingingThisRank ) {
                    if ( isKineticaRunning( url ) ) {
                        // We'll move on to the next rank
                        keepPingingThisRank = false;
                        GPUdbLogger.debug_with_info( "Rank http server @ "
                                                     + url.toString()
                                                     + " did respond" );
                    } else {
                        GPUdbLogger.debug_with_info( "Rank http server @ "
                                                     + url.toString()
                                                     + " did NOT respond" );
                        // Keep track of the fact that this rank's http server
                        // did NOT respond
                        wasSomeRankUnresponsive = true;

                        // Sleep a few seconds before retrying; we will keep
                        // pinging this rank until its http server comes up
                        try {
                            GPUdbLogger.debug_with_info( "Sleeping for "
                                                         + (sleepInterval / 1000)
                                                         + " seconds..." );
                            Thread.sleep( sleepInterval );
                        } catch ( InterruptedException ex ) {
                            GPUdbLogger.debug_with_info( "Sleep interrupted; throwing exception: "
                                                         + ex.getMessage() );
                            throw new GPUdbException( "Intra-cluster failover interrupted: "
                                                      + ex.getMessage(), ex );
                        }
                    }   // end if
                }   // end inner while
            } // end for loop

            // Success if all ranks responded
            // Note: If during this iteration even one rank was unresponsive,
            //       we will ping all the ranks once more to ensure everybody
            //       is still up
            if ( !wasSomeRankUnresponsive ) {
                // Every single rank responded; the cluster is ready for business!
                return true;
            }

            // Set values for the next iteration
            numRankCheckAttempt++;

            // Put a blank line for ease of reading the log
            GPUdbLogger.debug_with_info( "" );
        }  // end while
    }   // end areAllRanksReady


    /**
     * Given a ClusterAddressInfo object, check that all the worker ranks are up
     * by pinging them individually.  Do this one time per rank.
     */
    private boolean areAllRanksReadyCheckOnce( ClusterAddressInfo clusterAddresses ) throws GPUdbException {
        GPUdbLogger.debug_with_info( "Start checking all rank http servers' statuses..." );
        // Generate the list of the given cluster's rank-0 URL and worker
        // rank URLs
        List<URL> rankUrls = new ArrayList<URL>();
        rankUrls.add( clusterAddresses.getActiveHeadNodeUrl() );
        rankUrls.addAll( clusterAddresses.getWorkerRankUrls() );

        // Sleep for a short amount of time (three seconds)
        int sleepInterval = 3000;

        boolean wasSomeRankUnresponsive = false;

        // Check if all the ranks are up and listening
        for ( int i = 0; i < rankUrls.size(); ++i ) {
            URL url = rankUrls.get( i );
            if ( isKineticaRunning( url ) ) {
                GPUdbLogger.debug_with_info( "Rank http server @ " + url.toString()
                                             + " did respond" );
            } else {
                GPUdbLogger.debug_with_info( "Rank http server @ " + url.toString()
                                             + " did NOT respond" );
                // Keep track of the fact that this rank's http server
                // did NOT respond
                wasSomeRankUnresponsive = true;
            }   // end if
        } // end for loop

        // Success if all ranks responded
        if ( !wasSomeRankUnresponsive ) {
            // Every single rank responded; the cluster is ready for business!
            return true;
        } else {
            return false;
        }
    }   // end areAllRanksReadyCheckOnce


    /**
     * For the given cluster in the HA ring, try to recover the new set of
     * addresses for all the ranks etc.  If N+1 failover is in progress, spin
     * and wait until it is in a good state and return the result.
     */
    private synchronized boolean doIntraClusterFailover( int clusterIndex ) throws GPUdbException {
        // We need to keep an eye on the clock (do NOT use
        // System.currentTimeMillis() as that often gets adjusted by the
        // operating system)
        long startTime = System.nanoTime();

        // Retrieve info for the given cluster
        GPUdbLogger.debug_with_info( "BEGIN clusterIndex: " + clusterIndex );
        ClusterAddressInfo currClusterInfo = this.hostAddresses.get( clusterIndex );
        GPUdbLogger.debug_with_info( "Got cluster info: " + currClusterInfo.toString() );

        URL currHeadRankUrl = currClusterInfo.getActiveHeadNodeUrl();
        GPUdbLogger.info( "Starting N+1 failover recovery for cluster "
                          + "with head rank " + currHeadRankUrl.toString()
                          + "; timeout is: "
                          + (this.intraClusterFailoverTimeoutNS / 1000000000L)
                          + " seconds (0 means infinite waiting)" );

        // Generate the list of the given cluster's rank-0 URL and worker
        // rank URLs
        List<URL> rankUrls = new ArrayList<URL>();
        rankUrls.add( currHeadRankUrl );
        rankUrls.addAll( currClusterInfo.getWorkerRankUrls() );

        // We will sleep for 10 seconds when we need to wait and re-ping a cluster
        int nPlusOneFailoverSleepIntervalLong = 10000;
        // Sleep for a shorter amount of time in certain cases (three seconds)
        int nPlusOneFailoverSleepIntervalShort = 3000;

        // Keep track of how many ranks are leaderless as we get system
        // status from them
        int numLeaderlessRanks = 0;

        // Keep track of how many ranks do not respond
        int numRanksNoResponse = 0;

        // Keep track of how many ranks returned the current addresses; this
        // will help us decide if it was merely a network glitch without any
        // N+1 event happening.  In such a case, we would return the current
        // addresses.
        int numRanksGaveSameAddressAsCurrent = 0;

        // The intra-cluster failover is done in two stages.
        // Stage 1:
        //
        // The purpose of this stage is to find out from the known ranks
        // what the state of the cluster is, and update the addresses if
        // possible.
        //
        // * Query all the known ranks for the cluster status and the rank
        //   addresses.
        // * If any rank indicates that the cluster is in an irrecoverable
        //   state, we return false to indicate failure.
        // * If any rank is in a leaderless state, we skip that rank and go
        //   to the next one.
        // * With a good (running) status, if any rank gives a new (different)
        //   set of rank addresses, we set those addresses and are done (return
        //   true to indicate success).
        // * With a good (running) status, if any rank gives addresses that are
        //   the same as the current ones, we keep track of that and move on
        //   to the next rank.
        // * If a cluster operation is running, or the status is not "running",
        //   we sleep and then query the same rank again.
        // * We keep querying all the ranks in a loop until one of the following
        //   conditions are met:
        //   * Any rank is in the 'running' state and gives us a fresh set of
        //     of addresses, we return success.
        //   * All ranks are in the 'running' status and return the same
        //     addresses as the current ones, we assume all is well and return
        //     success.
        //   * If all but one rank are leaderless, we assume we are in a bad
        //     state and return failure.
        //   * If we hit the timeout, we return failure.
        //   * If all the ranks were unresponsive, we break out of stage 1
        //     and proceed to stage 2.
        //
        // Stage 2:
        //
        // We are in this stage only if stage 1 failed in a very particular
        // way: all ranks were unresponsive.  The reasons for which this can
        // happen are as follows:
        // 1) The cluster is fully down; we need to wait for the administrator
        //    to turn it back on.
        // 2) All the ranks were moved to other hosts.
        // 3) There is a serious network issue, which also needs to be solved
        //    by the administrator.
        //
        // In either case, we will ping all hosts to find out if rank-0 is
        // running there.  This will happen infinitely, unless the user has
        // set a timeout.  We will stop when we hit the timeout.  The logic
        // for this stage is rather similar to stage 1 with some minor changes.
        //
        // The aim of this stage is to keep the client application going until
        // either the cluster is fixed, or there is human intervention.  It is
        // not good for client applications to stop working, specially if there
        // are many long running applications.  We need to be resilient and keep
        // on working, unless the client tells us to stop at a certain time via
        // the intraClusterFailoverTimeout parameter.


        GPUdbLogger.debug_with_info( "N+1 failover recovery stage 1" );
        GPUdbLogger.debug_with_info( "-----------------------------" );

        // Try to get the new addresses for shuffled ranks from the
        // currently known ranks (whichever ones are still in place)
        GPUdbLogger.debug_with_info( "Before for loop; will attempt until hitting"
                                     + " the timeout or all ranks are unresponsive" );
        int i = 0;
        while ( true ) {
            // Keep track of the iteration only for debug logging purpose
            ++i;
            GPUdbLogger.debug_with_info( "N+1 failover; stage 1 attempt #" + i );

            // Keep track of how many ranks say they are leaderless
            numLeaderlessRanks = 0;
            // Keep track of how many ranks did not respond
            numRanksNoResponse = 0;
            // Keep track of how many ranks gave the same addresses as the
            // current ones
            numRanksGaveSameAddressAsCurrent = 0;

            // Get information from the ranks (and try all of them if
            // one/some don't have useful information)
            for ( int j = 0; j < rankUrls.size(); ++j ) {
                URL url = rankUrls.get( j );
                GPUdbLogger.debug_with_info( "Attempt #" + i + " rank-" + j + "; URL: " + url.toString() );

                boolean keepUsingThisRank = true;
                while ( keepUsingThisRank ) {

                    // If we've reached the timeout, just return
                    long currTime = System.nanoTime();
                    long elapsedTime = (currTime - startTime);
                    GPUdbLogger.info( "N+1 failover recovery elapsed time so far: "
                                      + (elapsedTime / 1000000000L)
                                      + " seconds" );
                    if ( (this.intraClusterFailoverTimeoutNS != 0)
                         && ( elapsedTime >= this.intraClusterFailoverTimeoutNS ) ) {
                        GPUdbLogger.debug_with_info( "Hit N+1 failover recovery timeout;"
                                                     + " returning false" );
                        return false;
                    }

                    // Check the system status (if this rank is responding to requests,
                    // keep pinging until status is back up to running)
                    try {
                        JsonNode systemStatusInfo = getSystemStatusInformation( url );

                        // Check if this rank is in an irrecoverable state
                        if ( isClusterOperationStatusIrrecoverable( systemStatusInfo ) ) {
                            // The system is hosed; there is no hope of recovery!
                            GPUdbLogger.debug_with_info( "System is irrecoverable; returning false" );
                            return false;
                        }

                        // Check if this rank has become leaderless (i.e. no
                        // head host manager can be elected)
                        if ( isRankLeaderless( systemStatusInfo ) ) {
                            GPUdbLogger.debug_with_info( "Rank is leaderless; skipping to next rank" );
                            keepUsingThisRank = false;
                            ++numLeaderlessRanks;
                            continue;
                        }

                        // Check if the system is back up and running
                        if ( isClusterOperationRunning( systemStatusInfo ) ) {
                            GPUdbLogger.debug_with_info( "Cluster operation running; will sleep" );
                            // Some sort of cluster operation is ongoing; wait for
                            // a certain amount of time before retrying status check
                            try {
                                GPUdbLogger.debug_with_info( "Sleeping for "
                                                             + (nPlusOneFailoverSleepIntervalLong / 1000)
                                                             + " seconds..." );
                                Thread.sleep( nPlusOneFailoverSleepIntervalLong );
                            } catch ( InterruptedException ex ) {
                                GPUdbLogger.debug_with_info( "Sleep interrupted; throwing exception: "
                                                             + ex.getMessage() );
                                throw new GPUdbException( "Intra-cluster failover interrupted: "
                                                          + ex.getMessage(), ex );
                            }
                        } else if ( isSystemRunning( systemStatusInfo ) ) {
                            GPUdbLogger.debug_with_info( "System is running; getting sys props" );
                            // System is back up; re-parse the URLs for this cluster

                            // Get the latest system properties of the cluster, if
                            // can't get it, skip to the next one
                            Map<String, String> systemProperties = getSystemProperties( url );
                            ClusterAddressInfo clusterInfoRefreshed = createClusterAddressInfo( url,
                                                                                                systemProperties );

                            GPUdbLogger.debug_with_info( "Refreshed addresses: " + clusterInfoRefreshed.toString() );
                            GPUdbLogger.debug_with_info( "Current addresses:   " + currClusterInfo.toString() );
                            // Check if the newly gotten addresses are the same as the old
                            // ones
                            if ( clusterInfoRefreshed.equals( currClusterInfo ) ) {
                                // The addresses have remained the same; so we didn't
                                // make any effective change
                                GPUdbLogger.debug_with_info( "Refrehsed addresses the same as the old one at rank "
                                                             + url.toString() + "; either no N+1 failover "
                                                             + "happening or this rank "
                                                             + "has stale information; moving to the next rank, if any.");
                                keepUsingThisRank = false;

                                // Keep track of the fact that this rank gave
                                // the same address as the current ones
                                ++numRanksGaveSameAddressAsCurrent;
                            } else {
                                // Replace the stale cluster info with the refreshed one
                                this.hostAddresses.set( clusterIndex, clusterInfoRefreshed );

                                // We actually changed the addresses for this cluster;
                                GPUdbLogger.debug_with_info( "Actually changed addresses; check for rank readiness...");
                                areAllRanksReady( clusterInfoRefreshed );

                                GPUdbLogger.debug_with_info( "Returning true; all ranks up and ready");
                                return true;
                            }
                        } else {
                            // For all other system statuses, we will retry (but
                            // we'll wait a certain amount of time before that)
                            try {
                                GPUdbLogger.debug_with_info( "Sleeping for "
                                                             + (nPlusOneFailoverSleepIntervalLong / 1000)
                                                             + " seconds..." );
                                Thread.sleep( nPlusOneFailoverSleepIntervalLong );
                            } catch ( InterruptedException ex ) {
                                GPUdbLogger.debug_with_info( "Sleep interrupted; throwing exception" );
                                throw new GPUdbException( "Intra-cluster failover interrupted: "
                                                          + ex.getMessage(), ex );
                            }
                        }
                    } catch ( GPUdbUnauthorizedAccessException ex ) {
                        // Any permission related problem should get propagated
                        throw ex;
                    } catch ( GPUdbExitException ex ) {
                        GPUdbLogger.debug_with_info( "Caught GPUdb EXIT "
                                                     + "exception; skipping "
                                                     + "to next rank: "
                                                     + ex.getMessage() );
                        // Try the next URL, but keep track of the fact that this
                        // could not be connected to
                        keepUsingThisRank = false;
                        ++numRanksNoResponse;
                    } catch ( GPUdbException ex ) {
                        GPUdbLogger.debug_with_info( "Caught GPUdb exception; skipping to next rank: " + ex.getMessage() );
                        // If error says system limited fatal then throw the exception;
                        // in all other cases, try the next URL
                        keepUsingThisRank = false;
                    }  // end try
                }   // end while (using a single rank)
            }   // end inner for (over all ranks)

            // Check if ALL ranks are claiming that the addresses have not
            // changed.  If so, then maybe we just had a network glitch or
            // some other issue, and no real N+1 event is happening.  In
            // that case, we will just return the current addresses as is.
            if ( numRanksGaveSameAddressAsCurrent == rankUrls.size() ) {
                // There is no need to change any addresses, nor is there any
                // need to ping all the ranks
                GPUdbLogger.debug_with_info( "All ranks claim the addresses are "
                                             +" the same; assuming no N+1 event "
                                             + "happening; returning true");
                return true;
            }

            // If we get to this spot, we've tried all known ranks and it looks
            // like the cluster has failed to elect a leader; so, we quit trying.
            // If all but one rank has said they're leaderless, it's good
            // enough for us to give up.
            if ( numLeaderlessRanks >= (rankUrls.size() - 1) ) {
                GPUdbLogger.debug_with_info( "All but one rank are leaderless; "
                                             + "returning false" );
                return false; // we're giving up
            }

            // Check if ALL ranks were UNresponsive.  If so, then break out
            // of stage 1 and proceed to stage 2.
            if ( numRanksNoResponse == rankUrls.size() ) {
                // There is no need to change any addresses, nor is there any
                // need to ping all the ranks
                GPUdbLogger.debug_with_info( "All ranks were UNresponsive; "
                                             +" ending stage 1 of N+1 failover "
                                             + "recovery");
                break; // out of the while loop
            }

            // Sleep a little before trying all the ranks again
            try {
                GPUdbLogger.debug_with_info( "Sleeping for "
                                             + (nPlusOneFailoverSleepIntervalShort / 1000)
                                             + " seconds before trying all the ranks again" );
                Thread.sleep( nPlusOneFailoverSleepIntervalShort );
            } catch ( InterruptedException ex ) {
                GPUdbLogger.debug_with_info( "Sleep interrupted; throwing exception" );
                throw new GPUdbException( "Intra-cluster failover interrupted: "
                                          + ex.getMessage(), ex );
            }
        }   // end while (stage 1)

        GPUdbLogger.debug_with_info( "N+1 failover recovery stage 1 done; "
                                     + "at last attempt, # rank with no response: "
                                     + numRanksNoResponse
                                     + "; # leaderless ranks: "
                                     + numLeaderlessRanks );

        // If we get to this spot, then all the known ranks have failed to give
        // us the current state of the cluster.  Now, we must try to find the new
        // head rank, hoping it's up.  We will keep searching for it the user
        // given timeout period.

        GPUdbLogger.debug_with_info( "N+1 failover recovery stage 2" );
        GPUdbLogger.debug_with_info( "-----------------------------" );

        // Generate a list of possible head rank URLs (we know all the hosts
        // on this machine
        GPUdbLogger.debug_with_info( "Generating head rank urls for all hosts" );
        List<URL> headRankUrls = new ArrayList<URL>();
        Iterator<String> iter = currClusterInfo.getHostNames().iterator();
        while ( iter.hasNext() ) {
            // Get the hostname (which *may* have the protocol attached)
            String hostnameWithProtocol = iter.next();
            GPUdbLogger.debug_with_info( "Got hostname: " + hostnameWithProtocol );
            // Split the hostname to extract just the host part
            String[] splitHostname = hostnameWithProtocol.split( "://" );
            String host;
            if ( splitHostname.length > 1 ) {
                host = splitHostname[ 1 ];
            } else {
                host = splitHostname[ 0 ];
            }
            GPUdbLogger.debug_with_info( "Got host: " + host );

            // Create a URL with the same protocol, port, and file as the
            // current head rank URL, but use the other hostname/IP address
            try {
                URL url = new URL( currHeadRankUrl.getProtocol(),
                                   host,
                                   currHeadRankUrl.getPort(),
                                   currHeadRankUrl.getFile() );
                GPUdbLogger.debug_with_info( "Created potential head rank url: "
                                             + url.toString() );
                // Won't be of any use if we don't add it to the list! :-)
                headRankUrls.add( url );
            } catch ( MalformedURLException ex ) {
                throw new GPUdbException( "Could not form a valid URL for "
                                          + "possible head rank at host '"
                                          + host + "': " + ex.getMessage(), ex );
            }
        }
        GPUdbLogger.debug_with_info( "Head rank urls for all hosts: " + java.util.Arrays.toString( headRankUrls.toArray() ) );


        // Iterate over the hosts to see where the new head rank has
        // ended up.  Do this for the given timeout period
        // Note: This for loop's body is very similar to the previous for loop's
        //       body; but we need to keep them separate since they're not
        //       exactly the same.
        GPUdbLogger.debug_with_info( "Before starting checking for moved head rank" );
        i = 0;
        int stage2IterCount = 0;
        while ( true ) {
            // Keep track of the iteration only for debug logging purpose
            GPUdbLogger.debug_with_info( "N+1 failover; stage 2 attempt #" + stage2IterCount );
            ++stage2IterCount;

            // Get the URL for the head rank if it were to end up at the
            // current host
            URL potentialHeadRankUrl = headRankUrls.get( i );
            GPUdbLogger.debug_with_info( "URL: " + potentialHeadRankUrl.toString() );

            boolean keepUsingThisHost = true;
            while ( keepUsingThisHost ) {
                // If we've reached the timeout, just return
                long currTime = System.nanoTime();
                long elapsedTime = (currTime - startTime);
                GPUdbLogger.info( "N+1 failover recovery elapsed time so far: "
                                  + (elapsedTime / 1000000000L)
                                  + " seconds (and trying...)" );
                if ( (this.intraClusterFailoverTimeoutNS != 0)
                     && ( elapsedTime >= this.intraClusterFailoverTimeoutNS ) ) {
                    GPUdbLogger.debug_with_info( "Hit N+1 failover recovery timeout;"
                                                 + " returning false" );
                    return false;
                }

                // Check the system status (if this rank is responding to requests,
                // keep pinging until status is back up to running)
                try {
                    JsonNode systemStatusInfo = getSystemStatusInformation( potentialHeadRankUrl );

                    // Check if this rank is in an irrecoverable state
                    if ( isClusterOperationStatusIrrecoverable( systemStatusInfo ) ) {
                        // The system is hosed; there is no hope of recovery!
                        GPUdbLogger.debug_with_info( "Cluster is irrecoverable; returning false" );
                        return false;
                    }

                    // Note: There is no leaderless check here because it doesn't
                    //       have any bearing on what we would do.

                    // Check if the system is running
                    if ( isClusterOperationRunning( systemStatusInfo ) ) {
                        GPUdbLogger.debug_with_info( "Cluster operation running; will sleep" );
                        // Some sort of cluster operation is ongoing; wait for
                        // a certain amount of time before retrying status check
                        try {
                            GPUdbLogger.debug_with_info( "Sleeping for "
                                                         + (nPlusOneFailoverSleepIntervalLong / 1000)
                                                         + " seconds..." );
                            Thread.sleep( nPlusOneFailoverSleepIntervalLong );
                        } catch ( InterruptedException ex ) {
                            GPUdbLogger.debug_with_info( "Sleep interrupted; throwing exception" );
                            throw new GPUdbException( "Intra-cluster failover interrupted: "
                                                      + ex.getMessage(), ex );
                        }
                    } else if ( isSystemRunning( systemStatusInfo ) ) {
                        // System is back up; re-parse the URLs for this cluster
                        GPUdbLogger.debug_with_info( "System is running; getting sys props" );

                        // Get the latest system properties of the cluster, if
                        // can't get it, skip to the next one
                        Map<String, String> systemProperties = getSystemProperties( potentialHeadRankUrl );
                        ClusterAddressInfo clusterInfoRefreshed = createClusterAddressInfo( potentialHeadRankUrl,
                                                                                            systemProperties );

                        GPUdbLogger.debug_with_info( "Refreshed addresses: " + clusterInfoRefreshed.toString() );
                        GPUdbLogger.debug_with_info( "Current addresses:   " + currClusterInfo.toString() );
                        // Check if the newly gotten addresses are the same as the old
                        // ones
                        if ( clusterInfoRefreshed.equals( currClusterInfo ) ) {
                            // The addresses have remained the same; so we didn't
                            // make any effective change
                            GPUdbLogger.debug_with_info( "Refrehsed addresses are the same as the old ones at rank-0 "
                                                         + potentialHeadRankUrl.toString() + "; possibly no N+1 failover "
                                                         + "actually happened (maybe a network glitch?)");

                            // Verify that the ranks are actually up and ready
                            GPUdbLogger.debug_with_info( "Verifying that the ranks are ready...");
                            boolean isClusterReady = areAllRanksReadyCheckOnce( clusterInfoRefreshed );

                            if ( isClusterReady ) {
                                GPUdbLogger.debug_with_info( "Returning true; all ranks up and ready");
                                return true;
                            } else {
                                GPUdbLogger.debug_with_info( "Not all ranks are ready; will retry");
                            }
                        } else {
                            // Replace the stale cluster info with the refreshed one
                            GPUdbLogger.debug_with_info( "Set the different addresses");
                            this.hostAddresses.set( clusterIndex, clusterInfoRefreshed );

                            // We actually changed the addresses for this cluster
                            GPUdbLogger.debug_with_info( "Actually changed addresses; check for rank readiness...");
                            areAllRanksReady( clusterInfoRefreshed );

                            GPUdbLogger.debug_with_info( "Returning true; all ranks up and ready");
                            return true;
                        }
                    } else {
                        GPUdbLogger.debug_with_info( "System is NOT running; skip to the next host" );
                        // The system is neither up nor is it running a cluster
                        // operation; continue with the next known rank to see
                        // what is going on.
                        keepUsingThisHost = false;
                    }
                } catch ( GPUdbUnauthorizedAccessException ex ) {
                    // Any permission related problem should get propagated
                    throw ex;
                } catch ( GPUdbException ex ) {
                    GPUdbLogger.debug_with_info( "Caught GPUdb exception; skipping to next host: " + ex.getMessage() );
                    // If error says system limited fatal then throw the
                    // exception; in all other cases, try the next URL
                    keepUsingThisHost = false;
                }  // end try

                // Sleep a second before retrying
                try {
                    GPUdbLogger.debug_with_info( "Sleeping for "
                                                 + (nPlusOneFailoverSleepIntervalShort / 1000)
                                                 + " seconds..." );
                    Thread.sleep( nPlusOneFailoverSleepIntervalShort );
                } catch ( InterruptedException ex ) {
                    GPUdbLogger.debug_with_info( "Sleep interrupted; throwing exception: "
                                                 + ex.getMessage() );
                    throw new GPUdbException( "Intra-cluster failover interrupted: "
                                              + ex.getMessage(), ex );
                }
            }   // end inner while

            // Increment the counter to get the next rank-0 URL (need to loop
            // over all the URLs)
            i = ((i + 1) % headRankUrls.size());
        }   // end outer while
    }   // end doIntraClusterFailover



    /**
     * Select the next cluster based on the HA failover priority set by the user.
     */
    protected synchronized void selectNextCluster() {
        // Increment the index by one (mod url list length)
        GPUdbLogger.debug_with_info( "Before incrementing 'currl url index': " +  getCurrClusterIndexPointer() );
        this.setCurrClusterIndexPointer( (getCurrClusterIndexPointer() + 1) % getHARingSize() );
        GPUdbLogger.debug_with_info( "After incrementing 'currl url index': " +  getCurrClusterIndexPointer() );

        // Keep a running count of how many times we had to switch clusters
        GPUdbLogger.debug_with_info( "Before incrementing # cluster switches: " +  getNumClusterSwitches() );
        this.incrementNumClusterSwitches();
        GPUdbLogger.debug_with_info( "After incrementing # cluster switches: " +  getNumClusterSwitches() );
    }


    /**
     * Switches the URL of the HA ring cluster.  Check if we've circled back to
     * the old URL.  If we've circled back to it, then re-shuffle the list of
     * indices so that the next time, we pick up HA clusters in a different random
     * manner and throw an exception.
     */
    protected URL switchURL(URL oldURL, int numClusterSwitches)
        throws GPUdbFailoverDisabledException,
               GPUdbHAUnavailableException,
               GPUdbUnauthorizedAccessException {
        if ( this.disableFailover ) {
            GPUdbLogger.debug_with_info( "Failover is disabled; throwing exception" );
            throw new GPUdbFailoverDisabledException( "Failover is disabled!" );
        }

        GPUdbLogger.debug_with_info( "Switching from URL: " + getURL().toString()
                                     + "; old url: " + oldURL.toString() );

        synchronized (urlLock) {
            // If there is only one URL, then we can't switch URLs
            if ( getHARingSize() == 1 ) {
                GPUdbLogger.debug_with_info( "Ring size is 1");
                try {
                    // Try to find out the new cluster configuration and all
                    // the relevant URLs; may take a long while
                    int currClusterIndex = getCurrentClusterIndex();
                    GPUdbLogger.debug_with_info( "currClusterIndex " + currClusterIndex + "; attempting intra cluster failover...");
                    if ( doIntraClusterFailover( currClusterIndex ) ) {
                        GPUdbLogger.debug_with_info( "Intra cluster failover succeeded; switched to url: " + getURL().toString());
                        // We have updated all the addresses; return the
                        // current/new head rank URL
                        return getURL();
                    } else {
                        GPUdbLogger.debug_with_info( "N+1 failover recovery failed; throwing error" );
                        throw new GPUdbHAUnavailableException( "N+1 failover at cluster with (possibly stale) "
                                                               + "rank-0 URL "
                                                               + getURL().toString()
                                                               + " did not complete successfully "
                                                               + "( no backup clusters available to fall back on)" );
                    }
                } catch ( GPUdbHAUnavailableException ex ) {
                    throw ex;
                } catch ( GPUdbUnauthorizedAccessException ex ) {
                    // Any permission related problem should get propagated
                    throw ex;
                } catch ( GPUdbException ex ) {
                    GPUdbLogger.debug_with_info( "N+1 failover recovery had exception: "
                                                 + ex.getMessage() );
                    throw new GPUdbHAUnavailableException( "N+1 failover at cluster with (possibly stale) "
                                                           + "rank-0 URL "
                                                           + getURL().toString()
                                                           + " did not complete successfully ("
                                                           + "no backup clusters available to fall back on);"
                                                           + " error: "
                                                           + ex.getMessage(),
                                                           ex );
                }
            }

            // Get how many times we've switched clusters since the caller called
            // this function
            int countClusterSwitchesSinceInvocation = (getNumClusterSwitches() - numClusterSwitches);
            // Check if the client has switched clusters more than the number
            // of clusters available in the HA ring
            boolean haveSwitchedClustersAcrossTheRing = ( countClusterSwitchesSinceInvocation
                                                          >= getHARingSize() );
            GPUdbLogger.debug_with_info( "Ring size is bigger than 1; " + getHARingSize()
                                         + " countClusterSwitchesSinceInvocation: "
                                         + countClusterSwitchesSinceInvocation
                                         + " haveSwitchedClustersAcrossTheRing "
                                         + haveSwitchedClustersAcrossTheRing);
            if ( haveSwitchedClustersAcrossTheRing ) {
                throw new GPUdbHAUnavailableException(" (all GPUdb clusters with "
                                                      + "head nodes [" + getURLs().toString()
                                                      + "] returned error)");
            }

            // Check if another thread beat us to switching the URL
            GPUdbLogger.debug_with_info( "Current url: " + getURL().toString() + " oldurl " + oldURL.toString() );
            if ( !getURL().equals( oldURL )
                 && (countClusterSwitchesSinceInvocation > 0) ) {
                GPUdbLogger.debug_with_info( "Switched to url: " + getURL().toString() );
                // Another thread must have already switched the URL; nothing
                // to do
                return getURL();
            }

            // Re-check the health of this cluster and see if maybe an N+1
            // event happened in the past (and therefore we have stale addresses).
            // In such a case, update the addresses; do this as many times as
            // as the client wants us to
            for (int i = 0; i < this.clusterReconnectCount; ++i) {
                if ( updateClusterAddresses( getCurrentClusterIndex() ) ) {
                    GPUdbLogger.debug_with_info( "Updated cluster address; switched to url: " +  getURL().toString() );
                    // We actually did update/change the cluster addresses, so just
                    // return the fresh head-rank URL so that we can re-try
                    // endpoint submission
                    return getURL();
                }
            }

            // Select the next cluster to use during this HA failover
            this.selectNextCluster();

            // We've circled back; shuffle the indices again so that future
            // requests go to a different randomly selected cluster, but also
            // let the caller know that we've circled back
            if ( getURL().equals( oldURL ) ) {
                GPUdbLogger.debug_with_info( "Curr url: " +  getURL()
                                             + " is the same as the 'old url':"
                                             + oldURL.toString()
                                             + "; randomizing URLs and throwing exception" );
                // Re-shuffle and set the index counter to zero
                randomizeURLs();

                // Let the user know that we've circled back
                throw new GPUdbHAUnavailableException(" (all GPUdb clusters with "
                                                      + "head nodes [" + getURLs().toString()
                                                      + "] returned error)");
            }

            // Haven't circled back to the old URL; so return the new one
            GPUdbLogger.debug_with_info( "Switched to url: " +  getURL().toString()
                                         + " (NOT the same as the 'old url':"
                                         + oldURL.toString()
                                         + ")" );
            return getURL();
        }
    }  // end switchURL

    /**
     * Switches the host manager  URL of the HA ring cluster.  Check if we've
     * circled back to the old URL.  If we've circled back to it, then
     * re-shuffle the list of indices so that the next time, we pick up HA
     * clusters in a different random manner and throw an exception.
     */
    private URL switchHmURL(URL oldURL, int numClusterSwitches)
        throws GPUdbFailoverDisabledException,
               GPUdbHAUnavailableException,
               GPUdbUnauthorizedAccessException {
        if ( this.disableFailover ) {
            GPUdbLogger.debug_with_info( "Failover is disabled; throwing exception" );
            throw new GPUdbFailoverDisabledException( "Failover is disabled!" );
        }

        GPUdbLogger.debug_with_info( "Switching from HM URL: " + oldURL.toString() );

        synchronized (urlLock) {
            // If there is only one URL, then we can't switch URLs
            if ( getHARingSize() == 1 ) {
                GPUdbLogger.debug_with_info( "Ring size is 1");
                try {
                    // N+1 failover recovery: try to find out the new cluster
                    // configuration and all the relevant URLs; may take a long while
                    int currClusterIndex = getCurrentClusterIndex();
                    GPUdbLogger.debug_with_info( "currClusterIndex " + currClusterIndex + "; attempting intra cluster failover...");
                    if ( doIntraClusterFailover( currClusterIndex ) ) {
                        GPUdbLogger.debug_with_info( "Intra cluster failover succeeded; "
                                                     + "switched to hm url: "
                                                     + getHmURL().toString());
                        // We have updated all the addresses; return the
                        // current/new head rank URL
                        return getHmURL();
                    } else {
                        throw new GPUdbHAUnavailableException( "N+1 failover at cluster with (possibly stale) "
                                                               + "host manager URL "
                                                               + getHmURL().toString()
                                                               + " did not complete successfully "
                                                               + "( no backup clusters available "
                                                               + "to fall back on)" );
                    }
                } catch ( GPUdbHAUnavailableException ex ) {
                    throw ex;
                } catch ( GPUdbUnauthorizedAccessException ex ) {
                    // Any permission related problem should get propagated
                    throw ex;
                } catch ( GPUdbException ex ) {
                    throw new GPUdbHAUnavailableException( "N+1 failover at cluster with (possibly stale) "
                                                           + "host manager URL " + getHmURL().toString()
                                                           + " did not complete successfully ("
                                                           + "no backup clusters available to fall back on);"
                                                           + " error: " + ex.getMessage(), ex );
                }
            }

            // Get how many times we've switched clusters since the caller called
            // this function
            int countClusterSwitchesSinceInvocation = (getNumClusterSwitches() - numClusterSwitches);
            // Check if the client has switched clusters more than the number
            // of clusters available in the HA ring
            boolean haveSwitchedClustersAcrossTheRing = ( countClusterSwitchesSinceInvocation
                                                          >= getHARingSize() );
            GPUdbLogger.debug_with_info( "Ring size is bigger than 1; " + getHARingSize()
                                         + " countClusterSwitchesSinceInvocation: "
                                         + countClusterSwitchesSinceInvocation
                                         + " haveSwitchedClustersAcrossTheRing "
                                         + haveSwitchedClustersAcrossTheRing );
            if ( haveSwitchedClustersAcrossTheRing ) {
                throw new GPUdbHAUnavailableException(" (all host managers at GPUdb clusters "
                                                      + "at [" + getHmURLs().toString()
                                                      + "] returned error)");
            }

            // Check if another thread beat us to switching the URL
            GPUdbLogger.debug_with_info( "Current hm url: " + getHmURL().toString()
                                         + " oldurl " + oldURL.toString() );
            if ( !getHmURL().equals( oldURL )
                 && (countClusterSwitchesSinceInvocation > 0) ) {
                GPUdbLogger.debug_with_info( "Switched to hm url: "
                                             + getHmURL().toString() );
                // Another thread must have already switched the URL; nothing
                // to do
                return getHmURL();
            }

            // Re-check the health of this cluster and see if maybe an N+1
            // event happened in the past (and therefore we have stale addresses).
            // In such a case, update the addresses.
            if ( updateClusterAddresses( getCurrentClusterIndex() ) ) {
                GPUdbLogger.debug_with_info( "Updated cluster address; switched to hm url: "
                                             +  getHmURL().toString() );
                // We actually did update/change the cluster addresses, so just
                // return the fresh head-rank URL so that we can re-try
                // endpoint submission
                return getHmURL();
            }

            // Select the next cluster to use during this HA failover
            this.selectNextCluster();

            // We've circled back; shuffle the indices again so that future
            // requests go to a different randomly selected cluster, but also
            // let the caller know that we've circled back
            if ( getHmURL().equals( oldURL ) ) {
                GPUdbLogger.debug_with_info( "Curr hm url: " +  getHmURL()
                                             + " is the same as the 'old url':"
                                             + oldURL.toString()
                                             + "; randomizing URLs and throwing exception" );
                // Re-shuffle and set the index counter to zero
                randomizeURLs();

                // Let the user know that we've circled back
                throw new GPUdbHAUnavailableException(" (all host managers at GPUdb clusters "
                                                      + "at [" + getHmURLs().toString()
                                                      + "] returned error)");
            }

            // Haven't circled back to the old URL; so return the new one
            GPUdbLogger.debug_with_info( "Switched to hm url: " +  getHmURL().toString()
                                         + " (NOT the same as the 'old url':"
                                         + oldURL.toString()
                                         + ")" );
            return getHmURL();
        }
    }   // end switchHmUrl


    /**
     * Create and initialize an HTTP connection object with the request headers
     *  (including authorization header), connection type, time out etc.
     *
     * @param url  the URL to which the connection needs to be made
     * @return     the initialized HttpPost connection object
     */
    protected HttpPost initializeHttpPostRequest( URL url ) throws Exception {
        return initializeHttpPostRequest( url, this.timeout );
    }


    /**
     * Create and initialize an HTTP connection object with the request headers
     *  (including authorization header), connection type, time out etc.
     *
     * @param url  the URL to which the connection needs to be made
     * @param timeout a positive integer representing the number of
     *                milliseconds to use for connection timeout
     * @return     the initialized HttpPost connection object
     */
    protected HttpPost initializeHttpPostRequest( URL url, int timeout ) throws Exception {
        // Verify that a sensible timeout is given
        if ( timeout < 0 ) {
            throw new GPUdbException( "Positive timeout value required, given "
                                      + timeout );
        }

        HttpPost connection = new HttpPost( url.toURI() );

        // Set the timeout explicitly if it is different from the default value
        if ( timeout != this.timeout ) {
            RequestConfig requestConfigWithCustomTimeout = RequestConfig.custom()
                .setSocketTimeout( timeout )
                .setConnectTimeout( timeout )
                .setConnectionRequestTimeout( timeout )
                .build();
            connection.setConfig( requestConfigWithCustomTimeout );
        }

        // Set the user defined headers
        for (Map.Entry<String, String> entry : this.httpHeaders.entrySet()) {
            connection.addHeader( entry.getKey(), entry.getValue() );
        }

        // Set the sync mode header (only if not using the default)
        if ( this.haSyncMode != HASynchronicityMode.DEFAULT ) {
            connection.addHeader( HEADER_HA_SYNC_MODE, this.haSyncMode.getMode() );
        }

        // Set the authorization header
        if (authorization != null) {
            connection.addHeader( HEADER_AUTHORIZATION, authorization );
        }

        return connection;
    }   // end initializeHttpPostRequest


    /**
     * Create and initialize an HTTP connection object with the request headers
     *  (including authorization header), connection type, time out etc.
     *
     * @param url  the URL to which the connection needs to be made
     * @return     the initialized HTTP connection object
     */
    protected HttpURLConnection initializeHttpConnection( URL url ) throws Exception {
        return initializeHttpConnection( url, this.timeout );
    }

    /**
     * Create and initialize an HTTP connection object with the request headers
     *  (including authorization header), connection type, time out etc.
     *
     * @param url  the URL to which the connection needs to be made
     * @param timeout a positive integer representing the number of
     *                milliseconds to use for connection timeout
     * @return     the initialized HTTP connection object
     */
    protected HttpURLConnection initializeHttpConnection( URL url, int timeout )
        throws Exception {

        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        // Set the timeout
        if ( timeout < 0 ) {
            throw new GPUdbException( "Positive timeout value required, given "
                                      + timeout );
        }
        connection.setConnectTimeout( timeout );
        connection.setReadTimeout( timeout );

        // Set the request type
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        // Set the user defined headers
        for (Map.Entry<String, String> entry : this.httpHeaders.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        // Set the sync mode header (only if not using the default)
        if ( this.haSyncMode != HASynchronicityMode.DEFAULT ) {
            connection.setRequestProperty( HEADER_HA_SYNC_MODE, this.haSyncMode.getMode() );
        }

        // Set the authorization header
        if (authorization != null) {
            connection.setRequestProperty (HEADER_AUTHORIZATION, authorization);
        }

        return connection;
    }



    /**
     * Given a hostname or IP address, check if the known clusters
     * have/use/contain it.
     *
     * @return the index of the cluster that contains this node; -1
     * if not found in the system.
     */
    private int getIndexOfClusterContainingNode( String hostName ) {
        if ( this.hostAddresses.isEmpty() ) {
            GPUdbLogger.debug_with_info( "hostAddressses is empty; returning -1");
            return -1;
        }

        // Check each cluster for the hostname/IP
        for (int i = 0; i < this.hostAddresses.size(); ++i) {
            if ( this.hostAddresses.get( i ).doesClusterContainNode( hostName ) ) {
                GPUdbLogger.debug_with_info( "Found match at " + i + "th iteration");
                return i;
            }
            GPUdbLogger.debug_with_info( "Did not find match at " + i + "th iteration");
        }

        // Did not find any cluster that uses/has the given hostname/IP address
        GPUdbLogger.debug_with_info( "Did not find any cluster with hostname "
                                     + hostName + "; returning -1");
        return -1;
    }

    /**
     * Given a URL, return the system status information.
     */
    private JsonNode getSystemStatusInformation( URL url )
        throws GPUdbException, GPUdbExitException {

        // Call /show/system/status at the given URL
        ShowSystemStatusResponse statusResponse = null;
        try {
            GPUdbLogger.debug_with_info( "Getting system status from: "
                                         + url.toString());
            statusResponse = submitRequest( appendPathToURL( url,
                                                             ENDPOINT_SHOW_SYSTEM_STATUS ),
                                            new ShowSystemStatusRequest(),
                                            new ShowSystemStatusResponse(),
                                            false,
                                            DEFAULT_INTERNAL_ENDPOINT_CALL_TIMEOUT );
        } catch (MalformedURLException ex) {
            throw new GPUdbException( "Error forming URL: " + ex.getMessage(),
                                      ex );
        }

        // Get the 'system' entry in the status response and parse it
        String systemStatusStr = statusResponse.getStatusMap().get( SHOW_SYSTEM_STATUS_RESPONSE_SYSTEM );
        GPUdbLogger.debug_with_info( "Got system status: " + systemStatusStr);
        JsonNode systemStatus;
        if ( systemStatusStr == null ) {
            throw new GPUdbException( "No entry for 'system' in /show/system/status!" );
        } else {
            try {
                systemStatus = this.JSON_MAPPER.readTree( systemStatusStr );
            } catch ( IOException ex ) {
                throw new GPUdbException( "Could not parse /show/system/status "
                                          + "entry for 'system': "
                                          + ex.getMessage(),
                                          ex );
            }
        }

        return systemStatus;
    }   // end getSystemStatusInformation


    /**
     * Given a URL, return the system properties information
     */
    private Map<String, String> getSystemProperties( URL url ) throws GPUdbException {
        // Call /show/system/properties at the given URL
        ShowSystemPropertiesResponse response = null;
        try {
            response = submitRequest( appendPathToURL( url,
                                                       ENDPOINT_SHOW_SYSTEM_PROPERTIES ),
                                      new ShowSystemPropertiesRequest(),
                                      new ShowSystemPropertiesResponse(),
                                      false );
        } catch (MalformedURLException ex) {
            throw new GPUdbException( "Error forming URL: " + ex.getMessage(),
                                      ex );
        }

        // Get the property map from the response and return it
        if ( response != null ) {
            GPUdbLogger.debug_with_info( "Got system properties from: "
                                         + url.toString() );
            Map<String, String> systemProperties = response.getPropertyMap();

            // Is HTTPD being used (helps in figuring out the host manager URL
            String is_httpd_enabled_str = systemProperties.get( "conf.enable_httpd_proxy" );

            // Figure out if we're using HTTPD
            if ( (is_httpd_enabled_str != null)
                 && (is_httpd_enabled_str.compareToIgnoreCase( "true" ) == 0 ) ) {
                this.useHttpd = true;
            }

            // Return the property map
            return systemProperties;
        } else {
            throw new GPUdbException( "Could not obtain system properties; got a null response!" );
        }
    }   // end getSystemProperties


    /**
     * Given the response to a /show/system/status query, figure out whether
     * the server is performing a cluster operation.
     */
    private boolean isClusterOperationRunning( JsonNode systemStatusInfo ) {
        try {
            // Then look for 'status' and see if it is 'running'
            JsonNode clusterOpRunningVal = systemStatusInfo.get( SHOW_SYSTEM_STATUS_RESPONSE_CLUSTER_OPERATION_RUNNING );
            GPUdbLogger.debug_with_info( "Got status: " + clusterOpRunningVal.toString() );

            if ( ( clusterOpRunningVal != null)
                 && SHOW_SYSTEM_STATUS_RESPONSE_TRUE.equals( clusterOpRunningVal.getTextValue() ) ) {
                GPUdbLogger.debug_with_info( "Returning true");
                return true;
            }
        } catch ( Exception ex ) {
            // Any error means we don't know whether the system is running
            GPUdbLogger.debug_with_info( "Caught exception " + ex.toString());
        }

        GPUdbLogger.debug_with_info( "Returning false");
        return false;
    }


    /**
     * Given the response to a /show/system/status query, figure out whether
     * the server is in an irrecoverable situation.
     */
    private boolean isClusterOperationStatusIrrecoverable( JsonNode systemStatusInfo ) {
        try {
            // Then look for 'cluster_operation_status' and see if it is 'irrecoverable'
            JsonNode clusterOpStatusVal = systemStatusInfo.get( SHOW_SYSTEM_STATUS_RESPONSE_CLUSTER_OPERATION_STATUS );
            GPUdbLogger.debug_with_info( "Got status: " + clusterOpStatusVal.toString() );

            if ( ( clusterOpStatusVal != null)
                 && SHOW_SYSTEM_STATUS_RESPONSE_CLUSTER_IRRECOVERABLE.equals( clusterOpStatusVal.getTextValue() ) ) {
                GPUdbLogger.debug_with_info( "Returning true");
                return true;
            }
        } catch ( Exception ex ) {
            // Any error means we don't know whether the system is running
            GPUdbLogger.debug_with_info( "Caught exception " + ex.toString());
        }

        GPUdbLogger.debug_with_info( "Returning false");
        return false;
    }


    /**
     * Given the response to a /show/system/status query, figure out whether
     * the rank that responded is leaderless.
     */
    private boolean isRankLeaderless( JsonNode systemStatusInfo ) {
        try {
            // Then look for 'status' and see if it is 'running'
            JsonNode systemStatus = systemStatusInfo.get( SHOW_SYSTEM_STATUS_RESPONSE_STATUS );
            GPUdbLogger.debug_with_info( "Got status: " + systemStatus.toString() );

            if ( ( systemStatus != null)
                 && SHOW_SYSTEM_STATUS_RESPONSE_LEADERLESS.equals( systemStatus.getTextValue() ) ) {
                GPUdbLogger.debug_with_info( "Returning true");
                return true;
            }
        } catch ( Exception ex ) {
            // Any error means we don't know whether the system is running
            GPUdbLogger.debug_with_info( "Caught exception " + ex.toString());
        }

        GPUdbLogger.debug_with_info( "Returning false");
        return false;
    }


    /**
     * Given the response to a /show/system/status query, figure out whether
     * the server is running or not.
     */
    private boolean isSystemRunning( JsonNode systemStatusInfo ) {
        try {
            // Then look for 'status' and see if it is 'running'
            JsonNode systemStatus = systemStatusInfo.get( SHOW_SYSTEM_STATUS_RESPONSE_STATUS );
            GPUdbLogger.debug_with_info( "Got status: " + systemStatus.toString() );

            if ( ( systemStatus != null)
                 && SHOW_SYSTEM_STATUS_RESPONSE_RUNNING.equals( systemStatus.getTextValue() ) ) {
                GPUdbLogger.debug_with_info( "Returning true");
                return true;
            }
        } catch ( Exception ex ) {
            // Any error means we don't know whether the system is running
            GPUdbLogger.debug_with_info( "Caught exception " + ex.toString());
        }

        GPUdbLogger.debug_with_info( "Returning false");
        return false;
    }


    /**
     * Given a URL, return whether the server is running at that address.
     */
    private boolean isSystemRunning( URL url ) {
        try {
            JsonNode systemStatusInfo = getSystemStatusInformation( url );

            // Then look for 'status' and see if it is 'running'
            JsonNode systemStatus = systemStatusInfo.get( SHOW_SYSTEM_STATUS_RESPONSE_STATUS );
            GPUdbLogger.debug_with_info( url.toString()  + " got status: " + systemStatus.toString() );

            if ( ( systemStatus != null)
                 && SHOW_SYSTEM_STATUS_RESPONSE_RUNNING.equals( systemStatus.getTextValue() ) ) {
                GPUdbLogger.debug_with_info( url.toString()  + " returning true");
                return true;
            }
        } catch ( Exception ex ) {
            // Any error means we don't know whether the system is running
            GPUdbLogger.debug_with_info( url.toString()
                                         + " caught exception " + ex.toString());
        }

        GPUdbLogger.debug_with_info( url.toString()  + " returning false");
        return false;
    }



    /**
     * Given system properties, deduce if the given cluster has N+1 failover
     * enabled or not.  For it to be enabled, either head failover or worker
     * failover need to be turned on.
     *
     * @param systemProperties  A map containing all relevant system properties.
     *
     * @return boolean value indicating if N+1 failover is enabled
     */
    private boolean isIntraClusterFailoverEnabled( Map<String, String> systemProperties )
        throws GPUdbException {

        boolean isIntraClusterFailoverEnabled = false;

        // Get the conf param for N+1 failover for the head rank
        String headFailover = systemProperties.get( SYSTEM_PROPERTIES_RESPONSE_HEAD_FAILOVER );
        if (headFailover == null) {
            throw new GPUdbException( "Missing value for "
                                      + SYSTEM_PROPERTIES_RESPONSE_HEAD_FAILOVER );
        }

        // Check if head failover is turned on
        if ( headFailover.equals( SYSTEM_PROPERTIES_RESPONSE_TRUE ) ) {
            isIntraClusterFailoverEnabled = true;
        }

        // Get the conf param for N+1 failover for worker ranks
        String workerFailover = systemProperties.get( SYSTEM_PROPERTIES_RESPONSE_WORKER_FAILOVER );
        if (workerFailover == null) {
            throw new GPUdbException( "Missing value for "
                                      + SYSTEM_PROPERTIES_RESPONSE_WORKER_FAILOVER );
        }

        // Check if worker failover is turned on
        if ( workerFailover.equals( SYSTEM_PROPERTIES_RESPONSE_TRUE ) ) {
            isIntraClusterFailoverEnabled = true;
        }

        GPUdbLogger.debug_with_info( "isIntraClusterFailoverEnabled: "
                                     + isIntraClusterFailoverEnabled);
        return isIntraClusterFailoverEnabled;
    }  // isIntraClusterFailoverEnabled


    /**
     * Given system properties, extrace the head- and worker rank URLs.
     *
     * @param systemProperties  A map containing all relevant system properties.
     * @param hostnameRegex     The regex to match the URLs against; if null,
     *                          then use the first element of the list, if the
     *                          system properties has multiple URLs for a given
     *                          rank.
     *
     * @return a list of URLs, where the first entry is the rank-0 URL.
     */
    private List<URL> getRankURLs( Map<String, String> systemProperties,
                                   Pattern hostnameRegex )
        throws GPUdbHostnameRegexFailureException,
               GPUdbException {

        // Get the protocol being used
        String protocol = "http";
        String propertyVal = systemProperties.get( SYSTEM_PROPERTIES_RESPONSE_USE_HTTPS );

        // Check if we're to use HTTPS
        if ( (propertyVal != null)
             && propertyVal.equals( SYSTEM_PROPERTIES_RESPONSE_TRUE ) ) {
            protocol = "https";
        }

        List<URL> rankURLs = new ArrayList<URL>();

        propertyVal = systemProperties.get( SYSTEM_PROPERTIES_RESPONSE_SERVER_URLS );
        if ( (propertyVal != null)
             && !propertyVal.isEmpty() ){
            GPUdbLogger.debug_with_info( "Got property '"
                                         + SYSTEM_PROPERTIES_RESPONSE_SERVER_URLS
                                         + "' value: " + propertyVal );

            // Get the URL for each of the ranks
            // ---------------------------------
            String[] urlLists = propertyVal.split(";");

            // Parse each entry (corresponds to a rank, could be an
            // empty slot for a removed rank)
            for (int i = 0; i < urlLists.length; ++i) {

                // Handle removed ranks
                if ( urlLists[i].isEmpty() ) {
                    continue;
                }

                // Each rank can have multiple URLs associated with it
                String[] urls = urlLists[i].split(",");
                boolean found = false;

                for (int j = 0; j < urls.length; ++j) {
                    String urlString = urls[j];
                    // If a regex is given, get a matching URL--if there isn't
                    // a match, throw an error.  If no regex is given, take
                    // the first URL.
                    URL url;
                    boolean doAdd = false;

                    // Ensure it's a valid URL
                    try {
                        url = new URL( urlString );
                    } catch (MalformedURLException ex) {
                        throw new GPUdbException(ex.getMessage(), ex);
                    }

                    if (hostnameRegex != null) {
                        // Check if this URL matches the given regex
                        doAdd = hostnameRegex.matcher( url.getHost() ).matches();
                        GPUdbLogger.debug_with_info( "Does rank URL " + url.toString()
                                                     + " match hostname regex '"
                                                     + hostnameRegex.toString()
                                                     + "' with host '"
                                                     + url.getHost() + "'?: "
                                                     + doAdd );
                    } else {
                        // No regex is given, so we'll take the first one
                        GPUdbLogger.debug_with_info( "No hostname regex given; "
                                                     + "adding rank url: "
                                                     + url.toString() );
                        doAdd = true;
                    }

                    if ( doAdd ) {
                        // Found a match (whether a regex is given or not)
                        rankURLs.add( url );
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    // It's a problem to not have any URL for this rank!
                    if (hostnameRegex != null) {
                        // The reason we don't have a URL is because it didn't
                        // match the given rege
                        throw new GPUdbHostnameRegexFailureException( "No matching "
                                                                      + "IP/hostname found for worker "
                                                                      + i + " (given hostname regex "
                                                                      + hostnameRegex.toString()
                                                                      + ")");
                    }
                    // We couldn't find it for some other reason
                    throw new GPUdbException("No matching IP/hostname found for worker " + i + ".");
                }
            }
        } else {
            GPUdbLogger.debug_with_info( "No entry for '"
                                         + SYSTEM_PROPERTIES_RESPONSE_SERVER_URLS
                                         + "' in " + ENDPOINT_SHOW_SYSTEM_PROPERTIES
                                         + " response; returning empty list") ;
        }

        return rankURLs;
    }  // end getRankURLs


    /**
     * Given system properties, find the host manager port number.
     *
     * @param systemProperties  A map containing all relevant system properties.
     *
     * @return the host manager port
     */
    private int getHostManagerPortFromProperties( Map<String, String> systemProperties )
        throws GPUdbException {


        // Get the conf param for the host manager port
        String hmPortStr = systemProperties.get( SYSTEM_PROPERTIES_RESPONSE_HM_PORT );
        if (hmPortStr == null) {
            throw new GPUdbException( "Missing value for "
                                      + SYSTEM_PROPERTIES_RESPONSE_HM_PORT );
        }

        int hmPort;
        try {
            hmPort = Integer.parseInt( hmPortStr, 10 );
        } catch ( NumberFormatException ex ) {
            throw new GPUdbException( "Unparsable entry for '"
                                      + SYSTEM_PROPERTIES_RESPONSE_HM_PORT
                                      + "' (" + hmPortStr + "); need an integer" );
        }
        return hmPort;
    }  // getHostManagerPortFromProperties



    /**
     * Given system properties, extract the hostnames or IP addresses of all the
     * physical nodes (machines) used in the cluster, whether or not there are
     * active ranks running on any of them.  Each string will contain the protocol
     * and the hostname or the IP address, e.g. "http://abcd.com",
     * "https://123.4.5.6".
     *
     * @param systemProperties  A map containing all relevant system properties.
     * @param hostnameRegex     The regex to match the URLs again; if null, then
     *                          use the first element of the list, if the system
     *                          properties has multiple URLs for a given rank.
     *
     * @return a list of hostnames or IP addresses along with the protocol.
     *         These are not full URLs.
     */
    private Set<String> getHostNamesFromSystemProperties( Map<String, String> systemProperties,
                                                          Pattern hostnameRegex )
        throws GPUdbHostnameRegexFailureException,
               GPUdbException {

        // Get the total number of hosts/machines in the cluster
        String numHostsStr = systemProperties.get( SYSTEM_PROPERTIES_RESPONSE_NUM_HOSTS );
        if (numHostsStr == null) {
            throw new GPUdbException( "Missing value for "
                                      + SYSTEM_PROPERTIES_RESPONSE_NUM_HOSTS );
        }
        int numHosts;
        try {
            numHosts = Integer.parseInt( numHostsStr, 10 );
        } catch ( NumberFormatException ex ) {
            throw new GPUdbException( "Unparsable entry for '"
                                      + SYSTEM_PROPERTIES_RESPONSE_NUM_HOSTS
                                      + "' (" + numHostsStr + "); need an integer" );
        }


        // Extract the hostnames from the system properties
        Set<String> clusterHostnames = new HashSet<String>();
        for (int i = 0; i < numHosts; ++i) {
            // Each hostname is listed individually in the system properties
            // as 'conf.host<i>_public_urls'
            String hostnameKey = "conf.host" + String.valueOf( i )
                + "_public_urls";

            String hostnameStr = systemProperties.get( hostnameKey );
            if (hostnameStr == null) {
                throw new GPUdbException( "Missing value for "
                                          + i + "th hostname '"
                                          + hostnameKey + "'" );
            }

            // Each host can have multiple hostnames associated with it
            String[] hostnames = hostnameStr.split(",");
            boolean found = false;

            // Try to find a usable hostname for this host
            for (int j = 0; j < hostnames.length; ++j) {
                String hostname = hostnames[ j ];

                // If a regex is given, get a matching hostname--if there isn't
                // a match, throw an error.  If no regex is given, take
                // the first hostname.
                boolean doAdd = false;
                if (hostnameRegex != null) {
                    // The hostname might have the protocol; strip that out
                    String[] splitHostname = hostname.split( "://" );
                    String host;
                    if ( splitHostname.length > 1 ) {
                        host = splitHostname[ 1 ];
                    } else {
                        host = splitHostname[ 0 ];
                    }

                    // Check if this hostname matches the regex
                    doAdd = hostnameRegex.matcher( host ).matches();
                    GPUdbLogger.debug_with_info( "Does hostname " + host
                                                 + " match hostname regex '"
                                                 + hostnameRegex.toString()
                                                 + "'?: " + doAdd );
                } else {
                    // No regex given, so take the first one
                    doAdd = true;
                }

                if ( doAdd ) {
                    // We've decided to use this hostname
                    clusterHostnames.add( hostname );
                    found = true;
                    break;
                }
            }

            if (!found) {
                // No eligible hostname found!
                if (hostnameRegex != null) {
                    // The reason we don't have a URL is because it didn't
                    // match the given reges
                    throw new GPUdbHostnameRegexFailureException( "No matching hostname found for host #"
                                                                  + i + " (given hostname regex "
                                                                  + hostnameRegex.toString()
                                                                  + ")" );
                }
                throw new GPUdbException("No matching hostname found for host #" + i + ".");
            }
        }

        return clusterHostnames;
    }  // getHostNamesFromSystemProperties


    /**
     * Given system properties, extract all the relevant address information
     * about the cluster and create an object containing the following:
     * -- active head rank URL
     * -- all worker rank URLs
     * -- host manager URL
     * -- hostnames for all the nodes in the cluster
     *
     * @return a ClusterAddressInfo object
     *
     */
    private ClusterAddressInfo createClusterAddressInfo( URL url,
                                                         Map<String, String> systemProperties )
        throws GPUdbHostnameRegexFailureException,
               GPUdbException {

        // Figure out if this cluster has N+1 failover enabled
        boolean isIntraClusterFailoverEnabled = isIntraClusterFailoverEnabled( systemProperties );
        GPUdbLogger.debug_with_info( "Is intra-cluster failover enabled?: " + isIntraClusterFailoverEnabled );

        // Get the rank URLs (head and worker ones)
        URL activeHeadNodeUrl;
        List<URL> rankURLs = getRankURLs( systemProperties, this.hostnameRegex );

        // Get the head node URL and keep it separately
        if ( !rankURLs.isEmpty() ) {
            GPUdbLogger.debug_with_info( "Got rank urls (including rank-0): "
                                         + java.util.Arrays.toString( rankURLs.toArray() ) );
            activeHeadNodeUrl = rankURLs.remove( 0 );
        } else {
            activeHeadNodeUrl = url;
            GPUdbLogger.debug_with_info( "No worker rank urls; using rank-0: "
                                         + url.toString() );
        }

        // Get hostnames for all the nodes/machines in the cluster
        Set<String> clusterHostnames = getHostNamesFromSystemProperties( systemProperties,
                                                                         hostnameRegex );

        // Create the host manager URL
        URL hostManagerUrl;
        try {
            // Create the host manager URL using the user given (or default) port
            if ( ( this.useHttpd == true )
                 && !activeHeadNodeUrl.getPath().isEmpty() ) {
                // We're using HTTPD, so use the appropriate URL
                // (likely, http[s]://hostname_or_IP:port/gpudb-host-manager)
                // Also, use the default httpd port (8082, usually)
                hostManagerUrl = new URL( activeHeadNodeUrl.getProtocol(),
                                          activeHeadNodeUrl.getHost(),
                                          // the port will be the same as the
                                          // head rank's; we'll just use a
                                          // different path
                                          activeHeadNodeUrl.getPort(),
                                          "/gpudb-host-manager" );
            } else {
                // The host manager URL shouldn't use any path and
                // use the host manager port
                hostManagerUrl = new URL( activeHeadNodeUrl.getProtocol(),
                                          activeHeadNodeUrl.getHost(),
                                          this.hostManagerPort,
                                          "" );
            }
        } catch ( MalformedURLException ex ) {
            throw new GPUdbException( ex.getMessage(), ex );
        }

        // Create an object to store all the information about this cluster
        ClusterAddressInfo clusterInfo = new ClusterAddressInfo( activeHeadNodeUrl,
                                                                 rankURLs,
                                                                 clusterHostnames,
                                                                 hostManagerUrl,
                                                                 false,
                                                                 isIntraClusterFailoverEnabled );

        // Check if this cluster is the primary cluster
        GPUdbLogger.debug_with_info( "Checking if this is the primary cluster; "
                                     + "this.primaryUrlHostname: "
                                     + this.primaryUrlHostname );
        if ( !this.primaryUrlHostname.isEmpty()
             && clusterInfo.doesClusterContainNode( this.primaryUrlHostname ) ) {
            // Yes, it is; mark this cluster as the primary cluster
            clusterInfo.setIsPrimaryCluster( true );
        }
        GPUdbLogger.debug_with_info( "Is primary cluster?: "
                                     + clusterInfo.getIsPrimaryCluster() );

        return clusterInfo;
    }  // end createClusterAddressInfo



    /**
     * Given system properties, extract the head node URLs for the
     * high-availability cluster.
     *
     * @return a list of full URLs for each of the head node in the
     * high availability cluster, if any is set up.
     */
    private List<URL> getHARingHeadNodeURLs( Map<String, String> systemProperties )
        throws GPUdbHostnameRegexFailureException,
               GPUdbException {

        List<URL> haRingHeadNodeURLs = new ArrayList<URL>();

        // First, find out if the database has a high-availability ring set up
        String is_ha_enabled_str = systemProperties.get( ShowSystemPropertiesResponse.PropertyMap.CONF_ENABLE_HA );

        // Only attempt to parse the HA ring node addresses if HA is enabled
        if ( (is_ha_enabled_str != null)
             && (is_ha_enabled_str.compareToIgnoreCase( "true" ) == 0 ) ) {

            // Parse the HA ring head node addresses, if any
            String ha_ring_head_nodes_str = systemProperties.get( SYSTEM_PROPERTIES_RESPONSE_HEAD_NODE_URLS );
            if ( (ha_ring_head_nodes_str != null) && !ha_ring_head_nodes_str.isEmpty() ) {

                String[] haRingHeadNodeUrlLists = ha_ring_head_nodes_str.split(";");

                // Parse each entry (corresponds to a cluster)
                for (int i = 0; i < haRingHeadNodeUrlLists.length; ++i) {

                    // Each cluster's head node can have multiple URLs
                    // associated with it
                    String[] urls = haRingHeadNodeUrlLists[i].split(",");
                    boolean found = false;

                    for (int j = 0; j < urls.length; ++j) {
                        String urlString = urls[j];
                        // If a regex is given, get a matching URL--if there isn't
                        // a match, throw an error.  If no regex is given, take
                        // the first URL.
                        URL url;
                        boolean doAdd = false;

                        // Ensure it's a valid URL
                        try {
                            url = new URL( urlString );
                        } catch (MalformedURLException ex) {
                            throw new GPUdbException(ex.getMessage(), ex);
                        }

                        if (this.hostnameRegex != null) {
                            // Check if this URL matches the given regex
                            doAdd = this.hostnameRegex.matcher( url.getHost() ).matches();
                            GPUdbLogger.debug_with_info( "Does cluster " + i
                                                         + " head node URL "
                                                         + url.toString()
                                                         + " match hostname regex '"
                                                         + hostnameRegex.toString()
                                                         + "' with host '"
                                                         + url.getHost() + "'?: "
                                                         + doAdd );
                        } else {
                            // No regex is given, so we'll take the first one
                            doAdd = true;
                        }

                        if ( doAdd ) {
                            // Found a match (whether a regex is given or not)
                            haRingHeadNodeURLs.add( url );
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        // No eligible hostname found!
                        if (this.hostnameRegex != null) {
                            // The reason we don't have a URL is because it didn't
                            // match the given reges
                            throw new GPUdbHostnameRegexFailureException( "No matching IP/hostname found "
                                                                          + "for cluster with head node URLs "
                                                                          + haRingHeadNodeUrlLists[i]
                                                                          + " (given hostname regex "
                                                                          + this.hostnameRegex.toString()
                                                                          + ")" );
                        }
                        throw new GPUdbException("No matching IP/hostname found "
                                                 + "for cluster with head node URLs"
                                                 + haRingHeadNodeUrlLists[i] );
                    }
                }   // end for
            }   // end if
        }   // nothing to do if this property isn't returned or is empty

        return haRingHeadNodeURLs;
    }  // getHARingHeadNodeURLs



    /**
     * Parse the given list of URLs which may have, in any order, URLs
     * for head node (rank-0) or worker ranks of any number of clusters.
     * Sort it all out and save information in a list of objects each
     * of which stores all pertinent information on a given cluster.
     *
     * If the first attempt fails, do this for a set period of initial
     * connecttion attempt timeout, as many times as is possible.
     *
     * ***This method should be called from the constructor initialization
     *    method only!***
     */
    private synchronized void parseUrls( List<URL> urls ) throws GPUdbException {

        // The very first time we sleep, if at all, will be for one minute
        int parseUrlsReattemptWaitInterval = 60000;

        // We need to keep an eye on the clock (do NOT use
        // System.currentTimeMillis() as that often gets adjusted by the
        // operating system)
        long startTime = System.nanoTime();

        // Try to parse the URLs for a preset amount of time (in case the
        // first attempt fails)
        boolean keepTrying = true;
        while ( keepTrying ) {
            try {
                // Parse the URLs (a single attempt)
                GPUdbLogger.debug_with_info( "Attempting to parse the user given URLs" );
                this.parseUrlsOnce( urls );
                return; // one successful attempt is all we need
            } catch (GPUdbHostnameRegexFailureException ex) {
                // There's no point in keep trying since the URLs aren't
                // going to magically change
                throw new GPUdbException( "Could not connect to any working Kinetica server due "
                                          + "to hostname regex mismatch (given "
                                          + " URLs: " + urls.toString()
                                          + "); " + ex.getMessage() );
            } catch (GPUdbException ex ) {
                GPUdbLogger.debug_with_info( "Attempt at parsing URLs failed: " + ex.getMessage() );

                // If the user does not want us to retry, parse the URLs as is
                if ( this.initialConnectionAttemptTimeoutNS == 0 ) {
                    GPUdbLogger.debug_with_info( "Initial connection attempt timeout set to 0; "
                                                 + "parse the given URLs without auto discovery." );
                    this.disableAutoDiscovery = true;
                } else {
                    // Do we keep trying another time?  Has enough time passed?
                    keepTrying = ( (System.nanoTime() - startTime)
                                   <= this.initialConnectionAttemptTimeoutNS );
                    GPUdbLogger.debug_with_info( "Keep trying to parse URLs?: " + keepTrying );
                    if ( keepTrying ) {
                        GPUdbLogger.warn( "Attempt at parsing user given URLs "
                                          + java.util.Arrays.toString( urls.toArray() )
                                          + " failed; waiting for "
                                          + (parseUrlsReattemptWaitInterval / 60000)
                                          + " mintue(s) before retrying");
                        try {
                            // We will sleep before trying again
                            GPUdbLogger.debug_with_info( "Sleeping for "
                                                         + (parseUrlsReattemptWaitInterval / 60000)
                                                         + " minutes before trying again" );
                            Thread.sleep( parseUrlsReattemptWaitInterval );

                            // The next time, we will sleep for twice as long
                            parseUrlsReattemptWaitInterval = (2 * parseUrlsReattemptWaitInterval);
                        } catch ( InterruptedException ex2 ) {
                            GPUdbLogger.debug_with_info( "Sleep interrupted ("
                                                         + ex2.getMessage()
                                                         + "); throwing exception" );
                            throw new GPUdbException( "Initial parsing of user given URLs interrupted: "
                                                      + ex2.getMessage(), ex2 );
                        }
                    }
                }
            }
        }

        // We should never get here, but just in case, check that we have got
        // at least one working URL
        if ( getHARingSize() == 0 ) {
            GPUdbLogger.debug_with_info( "No cluster found!" );
            throw new GPUdbException( "Could not connect to any working Kinetica server! "
                                      + "Given URLs: " + urls.toString() );
        }
    }   // end parseUrls



    /**
     * Parse the given list of URLs which may have, in any order, URLs
     * for head node (rank-0) or worker ranks of any number of clusters.
     * Sort it all out and save information in a list of objects each
     * of which stores all pertinent information on a given cluster.
     *
     * ***This method should be called from the constructor initialization
     *    method only (via parseURls, not directly)!***
     */
    private synchronized void parseUrlsOnce( List<URL> urls )
        throws GPUdbHostnameRegexFailureException,
               GPUdbException {

        this.hostAddresses = new ArrayList<ClusterAddressInfo>();

        // Convert the list of URLs to a set (to remove duplicates) and then
        // into a queue (so that we can add HA ring addresses as we get them
        // from servers and add them to the end of the queue while iterating
        // over it--other forms of collections don't allow for it)
        ArrayDeque<URL> urlQueue = new ArrayDeque( new HashSet( urls ) );

        // If a fully qualified URL is given for the primary URL, process
        // that, too
        String primaryUrlStr = this.options.getPrimaryUrl();
        GPUdbLogger.debug_with_info( "Primary url str: " + primaryUrlStr);
        // Save the hostname of the primary URL (which could be an empty string)
        this.primaryUrlHostname = primaryUrlStr;
        if ( !primaryUrlStr.isEmpty() ) {
            GPUdbLogger.debug_with_info( "Primary url string given: "
                                         + primaryUrlStr);
            try {
                // If it's a full URL, add it to the queue for processing
                URL primaryUrl = new URL( primaryUrlStr );

                // Add this URL to the list of URLs to process if it's not
                // already in it
                if ( !urlQueue.contains( primaryUrl ) ) {
                    GPUdbLogger.debug_with_info( "Primary url not in user given URLs; adding it");
                    urlQueue.add( primaryUrl );
                }

                // Save just the hostname of the primary clsuter's URL for
                // future use
                this.primaryUrlHostname = primaryUrl.getHost();
            } catch (MalformedURLException ex) {
                // No-op if it's not a fully qualified URL (e.g. the user
                // may have only given a hostname)
            }
        } else {
            GPUdbLogger.debug_with_info( "No primary url given");
            // The user didn't give any primary URL, but let's see if they gave
            // only one server URL (in which case, we'll treat that as the
            // primary cluster
            if ( urlQueue.size() == 1 ) {
                // Save just the hostname of the ONLY, hence the primary,
                // cluster's URL for future use
                GPUdbLogger.debug_with_info( "Only one url given by the user; setting that as the primary");
                this.primaryUrlHostname = urlQueue.getFirst().getHost();
            }
        }
        GPUdbLogger.debug_with_info( "Primary hostname: " + this.primaryUrlHostname);

        GPUdbLogger.debug_with_info( "User given URLs (size "
                                     + urlQueue.size() + "): "
                                     + java.util.Arrays.toString(urlQueue.toArray()) );

        // Parse each user given URL (until the queue is empty)
        while ( !urlQueue.isEmpty() ) {
            URL url = urlQueue.remove();
            GPUdbLogger.debug_with_info( "Processing url: " + url.toString());
            GPUdbLogger.debug_with_info( "URLs queue after removing this url (size "
                                         + urlQueue.size() + "): "
                                         + java.util.Arrays.toString(urlQueue.toArray()) );

            // Skip processing this URL if the hostname/IP address is used in
            // any of the known (already registered_ clusters
            if ( getIndexOfClusterContainingNode( url.getHost() ) != -1 ) {
                GPUdbLogger.debug_with_info( "Already contains hostname "
                                             + url.toString()  + " skipping");
                continue;
            }

            // Skip auto-discovery of cluster information if the user says so
            GPUdbLogger.debug_with_info( "Auto discovery disabling flag value: "
                                         + this.disableAutoDiscovery );
            if ( this.disableAutoDiscovery ) {
                GPUdbLogger.debug_with_info( "Auto discovery disabled; not connecting"
                                             + " to server at " + url.toString()
                                             + " at initialization for verification" );
                // Create a cluster info object with just the given URL and the
                // host manager port in the option
                ClusterAddressInfo clusterInfo = new ClusterAddressInfo( url,
                                                                         this.hostManagerPort );
                this.hostAddresses.add( clusterInfo );
                GPUdbLogger.debug_with_info( "Added cluster: " + clusterInfo.toString() );
                continue; // skip to the next URL
            }

            // Skip processing this URL if Kinetica is not running at this address
            if ( !isSystemRunning( url ) ) {
                GPUdbLogger.debug_with_info( "System is not running at  "
                                             + url.toString()  + " skipping" );
                continue;
            }

            // Get system properties of the cluster, if can't get it, skip
            // to the next one
            Map<String, String> systemProperties;
            try {
                systemProperties = getSystemProperties( url );
            } catch ( GPUdbException ex ) {
                // Couldn't get the properties, so can't process this URL
                GPUdbLogger.debug_with_info( "Could not get properties from "
                                             + url.toString()  + " skipping");
                continue;
            }

            // Create an object to store all the information about this cluster
            // (this could fail due to a host name regex mismatch)
            ClusterAddressInfo clusterInfo = createClusterAddressInfo( url, systemProperties );
            this.hostAddresses.add( clusterInfo );

            GPUdbLogger.debug_with_info( "Added cluster for url: " + url.toString() );
            GPUdbLogger.debug_with_info( "Added cluster: " + clusterInfo.toString() );
            GPUdbLogger.debug_with_info( "URLs queue after processing this url (size "
                                         + urlQueue.size() + "): "
                                         + java.util.Arrays.toString(urlQueue.toArray()) );

            // Parse the HA ring head nodes in the properties and add them
            // to this queue (only if we haven't processed them already).
            // This could fail due to a hostname regex mismatch.
            List<URL> haRingHeadNodeURLs = getHARingHeadNodeURLs( systemProperties );
            GPUdbLogger.debug_with_info( "Got ha ring head urls: "
                                         + java.util.Arrays.toString(haRingHeadNodeURLs.toArray()) );
            for ( int i = 0; i < haRingHeadNodeURLs.size(); ++i ) {
                URL haUrl = haRingHeadNodeURLs.get( i );
                GPUdbLogger.debug_with_info( "Processing ha ring head urls: "
                                             + haUrl.toString() + " host is "
                                             + haUrl.getHost() );
                if ( getIndexOfClusterContainingNode( haUrl.getHost() ) == -1 ) {
                    // We have not encountered this cluster yet; add it to the
                    // list of URLs to process
                    GPUdbLogger.debug_with_info( "Currently known clusters don't have this node; adding the url for processing" );
                    urlQueue.add( haUrl );
                } else {
                    GPUdbLogger.debug_with_info( "Currently known clusters DO have this node; NOT adding the url for processing" );
                }
            }
            GPUdbLogger.debug_with_info( "URLs queue after processing this ha head "
                                         + "nodes (size " + urlQueue.size() + "): "
                                         + java.util.Arrays.toString(urlQueue.toArray()) );
        }   // end while

        // Check that we have got at least one working URL
        if ( getHARingSize() == 0 ) {
            GPUdbLogger.error( "No clusters found at user given URLs "
                               + java.util.Arrays.toString( urls.toArray() )
                               + "!");
            throw new GPUdbException( "Could not connect to any working Kinetica server! "
                                      + "Given URLs: " + urls.toString() );
        }

        GPUdbLogger.debug_with_info( "Before re-setting primary for a single "
                                     + "cluster '" + primaryUrlStr + "'" );
        // If we end up with a single cluster, ensure that its head rank URL is
        // set as the primary (we need this check in case only a single URL was
        // given to the constructor BUT it was a worker URL)
        if ( getHARingSize() == 1 ) {
            GPUdbLogger.debug_with_info( "Only one cluster in the ring; "
                                         + "(re-)setting this one as the primary" );
            // Mark the single cluster as the primary cluster
            this.hostAddresses.get( 0 ).setIsPrimaryCluster( true );

            // Save the hostname of the single/primary cluster for
            // future use
            this.primaryUrlHostname = this.hostAddresses
                .get( 0 )
                .getActiveHeadNodeUrl()
                .getHost();

            // Also save it in the options for the future
            this.options.setPrimaryUrl( primaryUrlHostname );
            GPUdbLogger.debug_with_info( "New primary host name " + primaryUrlHostname );
        } else { GPUdbLogger.debug_with_info( "More than one cluster in the "
                                              + "ring; nothing to do re: primary "  );
        }

        // Flag the primary cluster as such and ensure it's the first element in
        // this.hostAddresses
        // ----------------------------------------------------------------------
        // Check if the primary host exists in the list of user given hosts
        int primaryIndex = getIndexOfClusterContainingNode( this.primaryUrlHostname );
        GPUdbLogger.debug_with_info( "Checking if the primary cluster is in the"
                                     + " ring; index: " + primaryIndex );
        if ( primaryIndex != -1 ) {
            GPUdbLogger.debug_with_info( "Found match!  setting the cluster "
                                         + "as primary: " + primaryIndex );
            // There is a match; mark the respective cluster as the primary cluster
            this.hostAddresses.get( primaryIndex ).setIsPrimaryCluster( true );

            if ( primaryIndex > 0 ) {
                GPUdbLogger.debug_with_info( "Primary cluster not at the "
                                             + "forefront; put it there" );
                // Note: Do not combine the nested if with the top level if; will change
                //       logic and may end up getting duplicates of the primary URL

                // Move the primary URL to the front of the list
                java.util.Collections.swap( this.hostAddresses, 0, primaryIndex );
            }
        }
        // Note that if no primary URL is specified by the user, then primaryIndex
        // above would be -1; but we need not handle that case since it would be
        // a no-op

        // Randomize the URL indices taking care that the primary cluster is
        // always at the front
        randomizeURLs();
    }   // end parseUrlsOnce


    /**
     * Randomly shuffles the list of high availability URL indices so that HA
     * failover happens at a random fashion.  One caveat is when a primary host
     * is given by the user; in that case, we need to keep the primary host's
     * index as the first one in the list so that upon failover, when we cricle
     * back, we always pick the first/primary host up again.
     */
    private void randomizeURLs() {
        GPUdbLogger.debug_with_info( "Ring size " + getHARingSize() );
        synchronized (this.haUrlIndices) {
            // Re-create the list of HA URL indices (automatically in an
            // monotonically increasing order)
            this.haUrlIndices.clear();
            for ( int i = 0; i < getHARingSize(); ++i ) {
                this.haUrlIndices.add( i );
            }

            // If the user chose to failover in a random fashion, we need to
            // shuffle the list (while ensure the primary always gets chosen
            // first)
            if ( this.haFailoverOrder == HAFailoverOrder.RANDOM ) {
                if ( this.primaryUrlHostname.isEmpty() ) {
                    // We don't have any primary URL; so treat all URLs similarly
                    // Randomly order the HA clusters and pick one to start working with
                    Collections.shuffle( this.haUrlIndices );
                } else {
                    // Shuffle from the 2nd element onward, only if there are more than
                    // two elements, of course
                    if ( this.haUrlIndices.size() > 2 ) {
                        Collections.shuffle( this.haUrlIndices.subList( 1, this.haUrlIndices.size() ) );
                    }
                }
            }
        }

        // This will keep track of which cluster to pick next (an index of
        // randomly shuffled indices)
        setCurrClusterIndexPointer( 0 );
    }   // end randomizeURLs


    // Type Management
    // ---------------

    /**
     * Adds a type descriptor for a GPUdb type (excluding types of join tables),
     * identified by a type ID, to the known type list. If that type is
     * subsequently encountered in results from a GPUdb request for which an
     * explicit type descriptor is not provided, the specified type descriptor
     * will be used for decoding.
     *
     * @param typeId          the type ID of the type in GPUdb (must not be the
     *                        type of a join table)
     * @param typeDescriptor  the type descriptor to be used for decoding the
     *                        type
     *
     * @throws IllegalArgumentException if {@code typeDescriptor} is not a
     * {@link Schema}, {@link Type}, {@link TypeObjectMap}, or {@link Class}
     * that implements {@link IndexedRecord}
     */
    public void addKnownType(String typeId, Object typeDescriptor) {
        if (typeDescriptor == null) {
            knownTypes.remove(typeId);
        } else if (typeDescriptor instanceof Schema
                || typeDescriptor instanceof Type
                || typeDescriptor instanceof TypeObjectMap
                || typeDescriptor instanceof Class && IndexedRecord.class.isAssignableFrom((Class)typeDescriptor)) {
            knownTypes.put(typeId, typeDescriptor);
        } else {
            throw new IllegalArgumentException("Type descriptor must be a Schema, Type, TypeObjectMap, or Class implementing IndexedRecord.");
        }
    }

    /**
     * Adds a type object map for the specified class as a type descriptor for
     * a GPUdb type, identified by a type ID, to the known type list, and also
     * adds the type object map to the known type object map list. If either the
     * type or the specified class is subsequently encountered in a GPUdb
     * request for which an explicit type object map is not provided, or in its
     * results, the specified type object map will be used for decoding and
     * encoding.
     *
     * @param <T>            the class
     * @param typeId         the type ID of the type in GPUdb
     * @param objectClass    the class
     * @param typeObjectMap  the type object map to be used for encoding and
     *                       decoding
     */
    public <T> void addKnownType(String typeId, Class<T> objectClass, TypeObjectMap<T> typeObjectMap) {
        addKnownType(typeId, typeObjectMap);
        addKnownTypeObjectMap(objectClass, typeObjectMap);
    }

    /**
     * Adds a type descriptor for the GPUdb type stored in the specified table
     * to the known type list. If that type is subsequently encountered in
     * results from a GPUdb request for which an explicit type descriptor is not
     * provided, the specified type descriptor will be used for decoding. Note
     * that this method makes a request to GPUdb to obtain table information.
     *
     * @param tableName       the name of the table in GPUdb
     * @param typeDescriptor  the type descriptor to be used for decoding the
     *                        type
     *
     * @throws IllegalArgumentException if {@code typeDescriptor} is not a
     * {@link Schema}, {@link Type}, {@link TypeObjectMap}, or {@link Class}
     * that implements {@link IndexedRecord}
     *
     * @throws GPUdbException if the table does not exist or is not homogeneous,
     * or if an error occurs during the request for table information
     */
    public void addKnownTypeFromTable(String tableName, Object typeDescriptor) throws GPUdbException {
        if (typeDescriptor == null
                || typeDescriptor instanceof Schema
                || typeDescriptor instanceof Type
                || typeDescriptor instanceof TypeObjectMap
                || typeDescriptor instanceof Class && IndexedRecord.class.isAssignableFrom((Class)typeDescriptor)) {
            ShowTableResponse response = submitRequest("/show/table", new ShowTableRequest(tableName, null), new ShowTableResponse(), false);

            if (response.getTypeIds().isEmpty()) {
                throw new GPUdbException("Table " + tableName + " does not exist.");
            }

            List<String> typeIds = response.getTypeIds();
            String typeId = typeIds.get(0);

            for (int i = 1; i < typeIds.size(); i++) {
                if (!typeIds.get(i).equals(typeId)) {
                    throw new GPUdbException("Table " + tableName + " is not homogeneous.");
                }
            }

            if (typeDescriptor == null) {
                knownTypes.remove(typeId);
            } else {
                knownTypes.put(typeId, typeDescriptor);
            }
        } else {
            throw new IllegalArgumentException("Type descriptor must be a Schema, Type, TypeObjectMap, or Class implementing IndexedRecord.");
        }
    }

    /**
     * Adds a type object map for the specified class as a type descriptor for
     * the GPUdb type stored in the specified table to the known type list, and
     * also adds the type object map to the known type object map list. If
     * either the type or the specified class is subsequently encountered in
     * a GPUdb request for which an explicit type object map is not provided, or
     * in its results, the specified type object map will be used for decoding
     * and encoding. Note that this method makes a request to GPUdb to obtain
     * table information.
     *
     * @param <T>            the class
     * @param tableName      the name of the table in GPUdb
     * @param objectClass    the class
     * @param typeObjectMap  the type object map to be used for encoding and
     *                       decoding
     *
     * @throws GPUdbException if the table does not exist or is not homogeneous,
     * or if an error occurs during the request for information
     */
    public <T> void addKnownTypeFromTable(String tableName, Class<T> objectClass, TypeObjectMap<T> typeObjectMap) throws GPUdbException {
        addKnownTypeFromTable(tableName, typeObjectMap);
        addKnownTypeObjectMap(objectClass, typeObjectMap);
    }

    /**
     * Adds a type object map for the specified class to the known type object
     * map list. If that class is subsequently encountered in a GPUdb request
     * for which an explicit type object map is not provided, the specified type
     * object map will be used for encoding.
     *
     * @param <T>            the class
     * @param objectClass    the class
     * @param typeObjectMap  the type object map to be used for encoding
     */
    public <T> void addKnownTypeObjectMap(Class<T> objectClass, TypeObjectMap<T> typeObjectMap) {
        if (typeObjectMap == null) {
            knownTypeObjectMaps.remove(objectClass);
        } else {
            knownTypeObjectMaps.put(objectClass, typeObjectMap);
        }
    }


    // Helper functions
    // ----------------

    protected <T> List<T> decode(Object typeDescriptor, List<ByteBuffer> data) throws GPUdbException {
        return Avro.decode(typeDescriptor, data, threadCount, executor);
    }

    protected <T> List<T> decode(String typeId, List<ByteBuffer> data) throws GPUdbException {
        return Avro.decode(getTypeDescriptor(typeId), data, threadCount, executor);
    }

    protected <T> List<T> decode(List<String> typeIds, List<ByteBuffer> data) throws GPUdbException {
        List<T> result = new ArrayList<>(data.size());

        if (!data.isEmpty()) {
            String lastTypeId = typeIds.get(0);
            int start = 0;

            for (int i = 1; i < data.size(); i++) {
                String typeId = typeIds.get(i);

                if (!typeId.equals(lastTypeId)) {
                    result.addAll(Avro.<T>decode(getTypeDescriptor(lastTypeId), data, start, i - start, threadCount, executor));
                    lastTypeId = typeId;
                    start = i;
                }
            }

            result.addAll(Avro.<T>decode(getTypeDescriptor(lastTypeId), data, start, data.size() - start, threadCount, executor));
        }

        return result;
    }

    protected <T> List<List<T>> decodeMultiple(Object typeDescriptor, List<List<ByteBuffer>> data) throws GPUdbException {
        List<List<T>> result = new ArrayList<>(data.size());

        for (int i = 0; i < data.size(); i++) {
            result.add(Avro.<T>decode(typeDescriptor, data.get(i), threadCount, executor));
        }

        return result;
    }

    protected <T> List<List<T>> decodeMultiple(List<String> typeIds, List<List<ByteBuffer>> data) throws GPUdbException {
        List<List<T>> result = new ArrayList<>(data.size());

        for (int i = 0; i < data.size(); i++) {
            result.add(Avro.<T>decode(getTypeDescriptor(typeIds.get(i)), data.get(i), threadCount, executor));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    protected <T> List<ByteBuffer> encode(List<T> data) throws GPUdbException {
        if (data == null || data.isEmpty()) {
            return new ArrayList<>();
        }

        T object = data.get(0);

        if (object instanceof IndexedRecord) {
            return Avro.encode((List<? extends IndexedRecord>)data, threadCount, executor);
        } else {
            return Avro.encode((TypeObjectMap<T>)getTypeObjectMap(object.getClass()), data, threadCount, executor);
        }
    }

    protected <T> List<ByteBuffer> encode(TypeObjectMap<T> typeObjectMap, List<T> data) throws GPUdbException {
        return Avro.encode(typeObjectMap, data == null ? new ArrayList<T>() : data, threadCount, executor);
    }

    protected Object getTypeDescriptor(String typeId) throws GPUdbException {
        Object typeDescriptor = knownTypes.get(typeId);

        if (typeDescriptor != null) {
            return typeDescriptor;
        }

        ShowTypesResponse response = submitRequest("/show/types", new ShowTypesRequest(typeId, null, null), new ShowTypesResponse(), false);

        if (response.getTypeSchemas().isEmpty()) {
            throw new GPUdbException("Unable to obtain type information for type " + typeId + ".");
        }

        typeDescriptor = new Type(response.getLabels().get(0), response.getTypeSchemas().get(0), response.getProperties().get(0));
        knownTypes.putIfAbsent(typeId, typeDescriptor);
        return typeDescriptor;
    }

    @SuppressWarnings("unchecked")
    protected <T> TypeObjectMap<T> getTypeObjectMap(Class<T> objectClass) throws GPUdbException {
        TypeObjectMap<T> typeObjectMap = (TypeObjectMap<T>)knownTypeObjectMaps.get(objectClass);

        if (typeObjectMap == null) {
            throw new GPUdbException("No known type object map for class " + objectClass.getName() + ".");
        }

        return typeObjectMap;
    }

    protected void setTypeDescriptorIfMissing(String typeId, String label, String typeSchema, Map<String, List<String>> properties) throws GPUdbException {
        // If the table is a collection, it does not have a proper type so
        // ignore it

        if (typeId.equals("<collection>")) {
            return;
        }

        // Check first to avoid creating a Type object unnecessarily

        if (!knownTypes.containsKey(typeId)) {
            knownTypes.putIfAbsent(typeId, new Type(label, typeSchema, properties));
        }
    }



    // Requests


    /**
     * Submits an arbitrary request to GPUdb and saves the response into a
     * pre-created response object. The request and response objects must
     * implement the Avro {@link IndexedRecord} interface.
     *
     * @param <T>       the type of the response object
     * @param endpoint  the GPUdb endpoint to send the request to
     * @param request   the request object
     * @param response  the response object
     *
     * @return          the response object (same as {@code response} parameter)
     *
     * @throws SubmitException if an error occurs while submitting the request
     */
    public <T extends IndexedRecord> T submitRequest( String endpoint,
                                                      IndexedRecord request,
                                                      T response ) throws SubmitException, GPUdbException {
        return submitRequest(endpoint, request, response, false);
    }

    /**
     * Submits an arbitrary request to GPUdb and saves the response into a
     * pre-created response object, optionally compressing the request before
     * sending. The request and response objects must implement the Avro
     * {@link IndexedRecord} interface. The request will only be compressed if
     * {@code enableCompression} is {@code true} and the
     * {@link GPUdb#GPUdb(String, GPUdbBase.Options) GPUdb constructor} was
     * called with the {@link Options#setUseSnappy(boolean) Snappy compression
     * flag} set to {@code true}.
     *
     * @param <T>                the type of the response object
     * @param endpoint           the GPUdb endpoint to send the request to
     * @param request            the request object
     * @param response           the response object
     * @param enableCompression  whether to compress the request
     *
     * @return                   the response object (same as {@code response}
     *                           parameter)
     *
     * @throws SubmitException if an error occurs while submitting the request
     */
    public <T extends IndexedRecord> T submitRequest( String endpoint,
                                                      IndexedRecord request,
                                                      T response,
                                                      boolean enableCompression ) throws SubmitException, GPUdbException {
        // Send the request to the database server head node
        URL url = getURL();
        URL originalURL = url;

        while (true) {
            // We need a snapshot of the current state re: HA failover.  When
            // multiple threads work on this object, we'll need to know how
            // many times we've switched clusters *before* attempting another
            // request submission.
            int currentClusterSwitchCount = getNumClusterSwitches();

            try {
                return submitRequest(appendPathToURL(url, endpoint), request, response, enableCompression);
            } catch (MalformedURLException ex) {
                // There's an error in creating the URL
                throw new GPUdbRuntimeException(ex.getMessage(), ex);
            } catch (GPUdbExitException ex) {
                GPUdbLogger.debug_with_info( "Got EXIT exception when trying endpoint "
                                             + endpoint + " at " + url.toString()
                                             + ": " + ex.getMessage() + "; switch URL..." );
                // Handle our special exit exception
                try {
                    url = switchURL( originalURL, currentClusterSwitchCount );
                    GPUdbLogger.debug_with_info( "Switched to " + url.toString() );
                } catch (GPUdbHAUnavailableException ha_ex) {
                    // We've now tried all the HA clusters and circled back
                    // Get the original cause to propagate to the user
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause + "; "
                                              + ha_ex.getMessage(), true );
                } catch (GPUdbFailoverDisabledException ha_ex) {
                    // Failover is disabled; return the original cause
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause + "; "
                                              + ha_ex.getMessage(), true );
                }
            } catch (SubmitException ex) {
                GPUdbLogger.debug_with_info( "Got submit exception when trying endpoint "
                                             + endpoint + " at " + url.toString()
                                             + ": " + ex.getMessage() + "; switch URL..." );
                // Some error occurred during the HTTP request
                try {
                    url = switchURL( originalURL, currentClusterSwitchCount );
                    GPUdbLogger.debug_with_info( "Switched to " + url.toString() );
                } catch (GPUdbHAUnavailableException ha_ex) {
                    // We've now tried all the HA clusters and circled back
                    // Get the original cause to propagate to the user
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new SubmitException( null, ex.getRequest(), ex.getRequestSize(),
                                               (originalCause + "; "
                                                + ha_ex.getMessage()),
                                               ex.getCause(), true );
                } catch (GPUdbFailoverDisabledException ha_ex) {
                    // Failover is disabled; return the original cause
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new SubmitException( null, ex.getRequest(), ex.getRequestSize(),
                                               (originalCause + "; "
                                                + ha_ex.getMessage()),
                                               ex.getCause(), true );
                }
            } catch (GPUdbException ex) {
                // Any other GPUdbException is a valid failure
                GPUdbLogger.debug_with_info( "Got GPUdbException, so propagating: "
                                             + ex.getMessage() );
                throw ex;
            } catch (Exception ex) {
                GPUdbLogger.debug_with_info( "Got java exception when trying endpoint "
                                             + endpoint + " at " + url.toString()
                                             + ": " + ex.getMessage() + "; switch URL..." );
                // And other random exceptions probably are also connection errors
                try {
                    url = switchURL( originalURL, currentClusterSwitchCount );
                    GPUdbLogger.debug_with_info( "Switched to " + url.toString() );
                } catch (GPUdbHAUnavailableException ha_ex) {
                    // We've now tried all the HA clusters and circled back
                    // Get the original cause to propagate to the user
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause + "; "
                                              + ha_ex.getMessage(), true );
                } catch (GPUdbFailoverDisabledException ha_ex) {
                    // Failover is disabled; return the original cause
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause + "; "
                                              + ha_ex.getMessage(), true );
                }
            }
        } // end while
    } // end submitRequest


    /**
     * Submits an arbitrary request to the GPUdb host manager and saves the
     * response into a pre-created response object. The request and response
     * objects must implement the Avro {@link IndexedRecord} interface.
     *
     * @param <T>       the type of the response object
     * @param endpoint  the GPUdb endpoint to send the request to
     * @param request   the request object
     * @param response  the response object
     *
     * @return          the response object (same as {@code response} parameter)
     *
     * @throws SubmitException if an error occurs while submitting the request
     */
    public <T extends IndexedRecord> T submitRequestToHM( String endpoint,
                                                          IndexedRecord request,
                                                          T response ) throws SubmitException, GPUdbException {
        return submitRequestToHM(endpoint, request, response, false );
    }


    /**
     * Submits an arbitrary request to the GPUdb host manager and saves the
     * response into a pre-created response object, optionally compressing
     * the request before sending. The request and response objects must
     * implement the Avro {@link IndexedRecord} interface. The request
     * will only be compressed if {@code enableCompression} is {@code true} and
     * the {@link GPUdb#GPUdb(String, GPUdbBase.Options) GPUdb constructor} was
     * called with the {@link Options#setUseSnappy(boolean) Snappy compression
     * flag} set to {@code true}.
     *
     * @param <T>                the type of the response object
     * @param endpoint           the GPUdb endpoint to send the request to
     * @param request            the request object
     * @param response           the response object
     * @param enableCompression  whether to compress the request
     *
     * @return                   the response object (same as {@code response}
     *                           parameter)
     *
     * @throws SubmitException if an error occurs while submitting the request
     */
    public <T extends IndexedRecord> T submitRequestToHM( String endpoint,
                                                          IndexedRecord request,
                                                          T response,
                                                          boolean enableCompression ) throws SubmitException, GPUdbException {
        // Send the request to the host manager
        URL hmUrl = getHmURL();
        URL originalURL = hmUrl;

        GPUdbException original_exception = null;

        for (int i = 0; i < HOST_MANAGER_SUBMIT_REQUEST_RETRY_COUNT; ++i) {
            // We need a snapshot of the current state re: HA failover.  When
            // multiple threads work on this object, we'll need to know how
            // many times we've switched clusters *before* attempting another
            // request submission.
            int currentClusterSwitchCount = getNumClusterSwitches();

            try {
                return submitRequest(appendPathToURL(hmUrl, endpoint), request, response, enableCompression);
            } catch (MalformedURLException ex) {
                // Save the original exception for later use
                if (original_exception == null) {
                    original_exception = new GPUdbException( ex.getMessage() );
                }

                // There's an error in creating the URL
                throw new GPUdbRuntimeException(ex.getMessage(), ex);
            } catch (GPUdbExitException ex) {
                // Save the original exception for later use
                if (original_exception == null) {
                    original_exception = ex;
                }

                // Upon failure, try to use other clusters
                try {
                    hmUrl = switchHmURL( originalURL, currentClusterSwitchCount );
                    GPUdbLogger.debug_with_info( "Switched to " + hmUrl.toString() );
                } catch (GPUdbHAUnavailableException ha_ex) {
                    // We've now tried all the HA clusters and circled back
                    // Get the original cause to propagate to the user
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause + "; "
                                              + ha_ex.getMessage(), true );
                } catch (GPUdbFailoverDisabledException ha_ex) {
                    // Failover is disabled; return the original cause
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause + "; "
                                              + ha_ex.getMessage(), true );
                }
            } catch (SubmitException ex) {
                // Save the original exception for later use
                if (original_exception == null) {
                    original_exception = ex;
                }

                // Upon failure, try to use other clusters
                try {
                    hmUrl = switchHmURL( originalURL, currentClusterSwitchCount );
                    GPUdbLogger.debug_with_info( "Switched to " + hmUrl.toString() );
                } catch (GPUdbHAUnavailableException ha_ex) {
                    // We've now tried all the HA clusters and circled back
                    // Get the original cause to propagate to the user
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new SubmitException( null, ex.getRequest(), ex.getRequestSize(),
                                               (originalCause + "; "
                                                + ha_ex.getMessage()),
                                               ex.getCause(), true );
                } catch (GPUdbFailoverDisabledException ha_ex) {
                    // Failover is disabled; return the original cause
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new SubmitException( null, ex.getRequest(), ex.getRequestSize(),
                                               (originalCause + "; "
                                                + ha_ex.getMessage()),
                                               ex.getCause(), true );
                }
            } catch (GPUdbException ex) {
                // Save the original exception for later use
                if (original_exception == null) {
                    original_exception = ex;
                }

                // the host manager can still be going even if the database is down
                if ( ex.getMessage().contains( DB_HM_OFFLINE_ERROR_MESSAGE ) ) {
                    try {
                        hmUrl = switchHmURL( originalURL, currentClusterSwitchCount );
                        GPUdbLogger.debug_with_info( "Switched to " + hmUrl.toString() );
                    } catch (GPUdbHAUnavailableException ha_ex) {
                        // We've now tried all the HA clusters and circled back
                        // Get the original cause to propagate to the user
                        String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                        throw new GPUdbException( originalCause + "; "
                                                  + ha_ex.getMessage(), true );
                    } catch (GPUdbFailoverDisabledException ha_ex) {
                        // Failover is disabled; return the original cause
                        String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                        throw new GPUdbException( originalCause + "; "
                                                  + ha_ex.getMessage(), true );
                    }
                }
                else {
                    // Any other GPUdbException is a valid failure
                    throw ex;
                }
            } catch (Exception ex) {
                // Save the original exception for later use
                if (original_exception == null) {
                    original_exception = new GPUdbException( ex.getMessage() );
                }

                // And other random exceptions probably are also connection errors
                try {
                    hmUrl = switchHmURL( originalURL, currentClusterSwitchCount );
                    GPUdbLogger.debug_with_info( "Switched to " + hmUrl.toString() );
                } catch (GPUdbHAUnavailableException ha_ex) {
                    // We've now tried all the HA clusters and circled back
                    // Get the original cause to propagate to the user
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause + "; "
                                              + ha_ex.getMessage(), true );
                } catch (GPUdbFailoverDisabledException ha_ex) {
                    // Failover is disabled; return the original cause
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause + "; "
                                              + ha_ex.getMessage(), true );
                }
            }
        } // end for

        // If we reach here, then something went wrong
        GPUdbLogger.debug_with_info( "Failed to submit host manager endpoint; "
                                     + "exceeded retry count "
                                     + HOST_MANAGER_SUBMIT_REQUEST_RETRY_COUNT
                                     + "; original exception: '"
                                     + original_exception.getMessage()
                                     + "'; please check if the host manager port"
                                     + " is wrong: " + hmUrl.toString() );
        throw original_exception;
    } // end submitRequestToHM


    /**
     * Submits an arbitrary request to GPUdb via the specified URL and saves the
     * response into a pre-created response object, optionally compressing the
     * request before sending. The request and response objects must implement
     * the Avro {@link IndexedRecord} interface. The request will only be
     * compressed if {@code enableCompression} is {@code true} and the
     * {@link GPUdb#GPUdb(String, GPUdbBase.Options) GPUdb constructor} was
     * called with the {@link Options#setUseSnappy(boolean) Snappy compression
     * flag} set to {@code true}.
     *
     * @param <T>                the type of the response object
     * @param url                the URL to send the request to
     * @param request            the request object
     * @param response           the response object
     * @param enableCompression  whether to compress the request
     * @return                   the response object (same as {@code response}
     *                           parameter)
     *
     * @throws SubmitException if an error occurs while submitting the request
     */
    public <T extends IndexedRecord> T submitRequest( URL url,
                                                      IndexedRecord request,
                                                      T response,
                                                      boolean enableCompression)
        throws SubmitException, GPUdbExitException, GPUdbException {

        return submitRequestRaw( url, request, response, enableCompression );
    }

    /**
     * Submits an arbitrary request to GPUdb via the specified URL and saves the
     * response into a pre-created response object, optionally compressing the
     * request before sending. The request and response objects must implement
     * the Avro {@link IndexedRecord} interface. The request will only be
     * compressed if {@code enableCompression} is {@code true} and the
     * {@link GPUdb#GPUdb(String, GPUdbBase.Options) GPUdb constructor} was
     * called with the {@link Options#setUseSnappy(boolean) Snappy compression
     * flag} set to {@code true}.  Optionally, the timeout period can be set
     * in milliseconds.
     *
     * @param <T>                the type of the response object
     * @param url                the URL to send the request to
     * @param request            the request object
     * @param response           the response object
     * @param enableCompression  whether to compress the request
     * @param timeout            a positive integer representing the number of
     *                           milliseconds to use for connection timeout
     * @return                   the response object (same as {@code response}
     *                           parameter)
     *
     * @throws SubmitException if an error occurs while submitting the request
     */
    public <T extends IndexedRecord> T submitRequest( URL url,
                                                      IndexedRecord request,
                                                      T response,
                                                      boolean enableCompression,
                                                      int timeout)
        throws SubmitException, GPUdbExitException, GPUdbException {

        return submitRequestRaw( url, request, response,
                                 enableCompression, timeout );
    }

    /**
     * Submits an arbitrary request to GPUdb via the specified URL and saves the
     * response into a pre-created response object, optionally compressing the
     * request before sending. The request and response objects must implement
     * the Avro {@link IndexedRecord} interface. The request will only be
     * compressed if {@code enableCompression} is {@code true} and the
     * {@link GPUdb#GPUdb(String, GPUdbBase.Options) GPUdb constructor} was
     * called with the {@link Options#setUseSnappy(boolean) Snappy compression
     * flag} set to {@code true}.
     *
     * @param <T>                the type of the response object
     * @param url                the URL to send the request to
     * @param request            the request object
     * @param response           the response object
     * @param enableCompression  whether to compress the request
     * @return                   the response object (same as {@code response}
     *                           parameter)
     *
     * @throws SubmitException if an error occurs while submitting the request
     */
    public <T extends IndexedRecord> T submitRequestRaw( URL url,
                                                         IndexedRecord request,
                                                         T response,
                                                         boolean enableCompression )
        throws SubmitException,
               GPUdbExitException,
               GPUdbException {
        // Use the set timeout for the GPUdb object for the http connection
        return submitRequestRaw( url, request, response,
                                 enableCompression,
                                 this.timeout );
    }


    /**
     * Submits an arbitrary request to GPUdb via the specified URL and saves the
     * response into a pre-created response object, optionally compressing the
     * request before sending. The request and response objects must implement
     * the Avro {@link IndexedRecord} interface. The request will only be
     * compressed if {@code enableCompression} is {@code true} and the
     * {@link GPUdb#GPUdb(String, GPUdbBase.Options) GPUdb constructor} was
     * called with the {@link Options#setUseSnappy(boolean) Snappy compression
     * flag} set to {@code true}.
     *
     * @param <T>                the type of the response object
     * @param url                the URL to send the request to
     * @param request            the request object
     * @param response           the response object
     * @param enableCompression  whether to compress the request
     * @param timeout            a positive integer representing the number of
     *                           milliseconds to use for connection timeout
     * @return                   the response object (same as {@code response}
     *                           parameter)
     *
     * @throws SubmitException if an error occurs while submitting the request
     */
    public <T extends IndexedRecord> T submitRequestRaw( URL url,
                                                         IndexedRecord request,
                                                         T response,
                                                         boolean enableCompression,
                                                         int timeout)
        throws SubmitException,
               GPUdbExitException,
               GPUdbException {

        int requestSize = -1;
        HttpPost              postRequest    = null;
        HttpEntity            responseEntity = null;
        CloseableHttpResponse postResponse   = null;

        try {
            // Log at the trace level
            GPUdbLogger.trace_with_info( "Sending request to " + url.toString() );

            // Use the given timeout, instead of the member variable one
            postRequest = initializeHttpPostRequest( url, timeout );

            HttpEntity requestPacket;
            if (enableCompression && useSnappy) {
                // Use snappy to compress the original request body
                byte[] encodedRequest = Snappy.compress(Avro.encode(request).array());
                requestSize = encodedRequest.length;
                postRequest.addHeader( HEADER_CONTENT_TYPE, "application/x-snappy" );

                // Create the entity for the compressed request
                requestPacket = new ByteArrayEntity( encodedRequest );
            } else {
                byte[] encodedRequest = Avro.encode(request).array();
                requestSize = encodedRequest.length;
                postRequest.addHeader( HEADER_CONTENT_TYPE, "application/octet-stream" );

                // Create the entity for the request
                requestPacket = new ByteArrayEntity( encodedRequest );
            }

            // Save the request into the http post object as a payload
            postRequest.setEntity( requestPacket );

            // Execute the request
            try {
                postResponse = this.httpClient.execute( postRequest );
            } catch ( javax.net.ssl.SSLException ex) {
                String errorMsg = ("Encountered SSL exception when trying to connect to "
                                   + url.toString() + "; error: "
                                   + ex.getMessage() );
                GPUdbLogger.debug_with_info( "Throwing unauthroized exception due to: "
                                             + ex.getMessage() );
                throw new GPUdbUnauthorizedAccessException( errorMsg );
            } catch (Exception ex) {
                // Trigger an HA failover at the caller level
                GPUdbLogger.debug_with_info( "Throwing exit exception due to: "
                                             + ex.getMessage() );
                throw new GPUdbExitException( "Error submitting endpoint request: "
                                              + ex.getMessage() );
            }

            // Get the status code and the messages of the response
            int statusCode = postResponse.getStatusLine().getStatusCode();
            String responseMessage = postResponse.getStatusLine().getReasonPhrase();

            // Get the entity and the content of the response
            responseEntity = postResponse.getEntity();

            // Ensure that we're not getting any html snippet (may be
            // returned by the HTTPD server)
            if ( (responseEntity.getContentType() != null)
                 && (responseEntity.getContentType().getElements().length > 0)
                 && (responseEntity.getContentType().getElements()[0].getName()
                     .startsWith( "text" )) ) {
                String errorMsg;
                // Handle unauthorized connection specially--better error messaging
                if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    GPUdbLogger.debug_with_info( "Got status code: " + statusCode );
                    errorMsg = ("Unauthorized access: " + responseMessage );
                    throw new GPUdbUnauthorizedAccessException( errorMsg );
                } else if ( (statusCode == HttpURLConnection.HTTP_UNAVAILABLE)
                            || (statusCode == HttpURLConnection.HTTP_INTERNAL_ERROR)
                            || (statusCode == HttpURLConnection.HTTP_GATEWAY_TIMEOUT)
                            ) {
                    // Some status codes should trigger an exit exception which will
                    // in turn trigger a failover on the client side
                    GPUdbLogger.debug_with_info( "Throwing EXIT exception from "
                                                 + url.toString() + "; "
                                                 + "response_code: " + statusCode
                                                 + "; content type "
                                                 + responseEntity.getContentType()
                                                       .getElements()[0].getName()
                                                 + "; response message: "
                                                 + responseMessage);
                    throw new GPUdbExitException( responseMessage );
                } else {
                    // All other issues are simply propagated to the user
                    errorMsg = ("Cannot parse response from server: '"
                                + responseMessage + "'; status code: "
                                + statusCode );
                }
                throw new SubmitException( url, request, requestSize, errorMsg );
            }

            // Parse response based on error code
            if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                // Got sttaus 401 -- unauthorized
                GPUdbLogger.debug_with_info( "Got status code: " + statusCode );
                String errorMsg = ("Unauthorized access: " + responseMessage );
                throw new GPUdbUnauthorizedAccessException( errorMsg );
                // Note: Keeping the original code here in case some unforeseen
                // problem arises that we haven't thought of yet by changing
                // which exception is thrown.
                // throw new SubmitException( url, request, requestSize,
                //                            responseMessage );
            }

            InputStream inputStream = responseEntity.getContent();
            if (inputStream == null) {
                // Trigger an HA failover at the caller level
                throw new GPUdbExitException( "Server returned HTTP "
                                              + statusCode + " ("
                                              + responseMessage + ")."
                                              + " returning EXIT exception");
            }

            try {
                // Manually decode the RawGpudbResponse wrapper directly from
                // the stream to avoid allocation of intermediate buffers

                BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(inputStream, null);
                String status = decoder.readString();
                String message = decoder.readString();

                if (status.equals("ERROR")) {
                    // Check if Kinetica is shutting down
                    if ( (statusCode == HttpURLConnection.HTTP_UNAVAILABLE)
                         || (statusCode == HttpURLConnection.HTTP_INTERNAL_ERROR)
                         || (statusCode == HttpURLConnection.HTTP_GATEWAY_TIMEOUT)
                         || message.contains( DB_EXITING_ERROR_MESSAGE )
                         || message.contains( DB_CONNECTION_REFUSED_ERROR_MESSAGE )
                         || message.contains( DB_CONNECTION_RESET_ERROR_MESSAGE )
                         || message.contains( DB_SYSTEM_LIMITED_ERROR_MESSAGE )
                         || message.contains( DB_OFFLINE_ERROR_MESSAGE ) ) {
                        GPUdbLogger.debug_with_info( "Throwing EXIT exception from "
                                                     + url.toString() + "; "
                                                     + "response_code: " + statusCode
                                                     + "; message: " + message);
                        throw new GPUdbExitException( message );
                    }
                    // A legitimate error
                    GPUdbLogger.debug_with_info( "Throwing GPUdb exception from "
                                                 + url.toString() + "; "
                                                 + "response_code: " + statusCode
                                                 + "; message: " + message);
                    throw new GPUdbException( message );
                }

                // Skip over data_type field
                decoder.skipString();

                // Decode data field
                decoder.readInt();
                return new Avro.DatumReader<T>(response.getSchema()).read(response, decoder);
            } finally {
                // Attempt to read any remaining data in the stream

                try {
                    inputStream.skip(Long.MAX_VALUE);
                } catch (Exception ex) {
                }
            }
        } catch (GPUdbExitException ex) {
            // An HA failover should be triggered
            throw ex;
        } catch (SubmitException ex) {
            GPUdbLogger.debug_with_info( "Got SubmitException: " + ex.getMessage() );
            // Some sort of submission error
            throw ex;
        } catch (GPUdbException ex) {
            if ( ex.getMessage().contains( DB_EOF_FROM_SERVER_ERROR_MESSAGE ) ) {
                // The server did not send a response; we need to trigger an HA
                // failover scenario
                throw new GPUdbExitException( ex.getMessage() );
            }
            // Some legitimate error
            throw ex;
        } catch (java.net.SocketException ex) {
            // Any network issue should trigger an HA failover
            throw new GPUdbExitException( ex.getMessage() );
        } catch (Exception ex) {
            GPUdbLogger.debug_with_info( "Caught Exception: " + ex.getMessage() );
            // Some sort of submission error
            throw new SubmitException(url, request, requestSize, ex.getMessage(), ex);
        } finally {
            // Release all resources held by the responseEntity
            if ( responseEntity != null ) {
                EntityUtils.consumeQuietly( responseEntity );
            }

            // Close the stream
            if ( postResponse != null ) {
                try {
                    postResponse.close();
                } catch (IOException ex) {
                }
            }
        }
    }   // end submitRequestRaw

    // Utilities

    /**
     * Verifies that GPUdb is running on the server.
     *
     * @throws GPUdbException if an error occurs and/or GPUdb is not running on
     * the server
     */
    public void ping() throws GPUdbException {
        URL url = getURL();
        URL originalURL = url;

        while (true) {
            HttpURLConnection connection = null;

            try {
                connection = initializeHttpConnection( getURL() );

                // Ping is a get, unlike all endpoints which are post
                connection.setRequestMethod("GET");

                byte[] buffer = new byte[1024];
                int index = 0;

                try (InputStream inputStream = connection.getResponseCode() < 400 ? connection.getInputStream() : connection.getErrorStream()) {
                    if (inputStream == null) {
                        throw new IOException("Server returned HTTP " + connection.getResponseCode() + " (" + connection.getResponseMessage() + ").");
                    }

                    int count;

                    while ((count = inputStream.read(buffer, index, buffer.length - index)) > -1) {
                        index += count;

                        if (index == buffer.length) {
                            buffer = Arrays.copyOf(buffer, buffer.length * 2);
                        }
                    }
                }

                String response = new String(Arrays.copyOf(buffer, index));

                if (!response.equals("Kinetica is running!")) {
                    throw new GPUdbException("Server returned invalid response: " + response);
                }

                return;
            } catch (GPUdbException ex) {
                throw ex;
            } catch (Exception ex) {
                try {
                    url = switchURL( originalURL, getNumClusterSwitches() );
                } catch (GPUdbHAUnavailableException ha_ex) {
                    // We've now tried all the HA clusters and circled back
                    throw new GPUdbException(ex.getMessage(), ex, true);
                }
            } finally {
                if (connection != null) {
                    try {
                        connection.disconnect();
                    } catch (Exception ex) {
                    }
                }
            }
        }
    }   // end ping at the current url


    /**
     * Pings the given URL and returns the response.  If no response,
     * returns an empty string.
     *
     * @return the ping response, or an empty string if it fails.
     */
    public String ping(URL url) {

        HttpURLConnection connection = null;

        try {
            connection = initializeHttpConnection( url );

            // Ping is a get, unlike all endpoints which are post
            connection.setRequestMethod("GET");

            byte[] buffer = new byte[1024];
            int index = 0;

            try (InputStream inputStream = connection.getResponseCode() < 400 ? connection.getInputStream() : connection.getErrorStream()) {
                if (inputStream == null) {
                    throw new IOException("Server returned HTTP " + connection.getResponseCode() + " (" + connection.getResponseMessage() + ").");
                }

                int count;

                while ((count = inputStream.read(buffer, index, buffer.length - index)) > -1) {
                    index += count;

                    if (index == buffer.length) {
                        buffer = Arrays.copyOf(buffer, buffer.length * 2);
                    }
                }
            }

            String response = new String(Arrays.copyOf(buffer, index));

            return response;
        } catch (Exception ex) {
            return "";
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception ex) {
                }
            }
        }
    }   // ping URL



    /**
     * Verifies that GPUdb is running at the given URL (does not do any HA failover).
     *
     * @return true if Kinetica is running, false otherwise.
     */
    public boolean isKineticaRunning(URL url) {

        String pingResponse = ping( url );
        GPUdbLogger.debug_with_info( "HTTP server @ " + url.toString()
                                     + " responded with: '"
                                     + pingResponse + "'" );
        if ( pingResponse.equals("Kinetica is running!") ) {
            // Kinetica IS running!
            return true;
        }

        // Did not get the expected response
        return false;
    }   // end isKineticaRunning

}
