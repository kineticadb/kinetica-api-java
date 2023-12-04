package com.gpudb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpudb.protocol.*;
import com.gpudb.util.ssl.X509TrustManagerBypass;
import com.gpudb.util.url.UrlUtils;
import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.xerial.snappy.Snappy;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;



/**
 * Base class for the GPUdb API that provides general functionality not specific
 * to any particular GPUdb request. This class is never instantiated directly;
 * its functionality is accessed via instances of the {@link GPUdb} class.
  *
 * Usage patterns:
 * 1. Unsecured setup
 * <pre>
 *         String url = "http://your_server_ip_or_FQDN:9191";
 *
 *         GPUdb.Options options = new GPUdb.Options();
 *         options.setUsername("user");
 *         options.setPassword("password");
 *
 *         GPUdb gpudb = new GPUdb( url, options );
 *
 * </pre>
 *
 * 2. Secured setup
 *
 * @apiNote - If {@code options.setBypassSslCertCheck( true )} is set, then all forms
 * of certificate checking whether self-signed or one issued by a known CA will be
 * bypassed and an unsecured connection will be set up.
 *
 * A) This setup will enforce checking the truststore and the
 *    connection will fail if a mismatch is found
 * <pre>
 *         String url = "https://your_server_ip_or_FQDN:8082/gpudb-0";
 *
 *         GPUdb.Options options = new GPUdb.Options();
 *         options.setUsername("user");
 *         options.setPassword("password");
 *         options.setTrustStoreFilePath("/path/to/truststore.jks");
 *         options.setTrustStorePassword("password_for_truststore");
 *
 *         GPUdb gpudb = new GPUdb( url, options );
 * </pre>
 *
 * B) This setup will not enforce checking the truststore and the
 *    connection will succeed since the truststore will not be verified.
 *
 *    if {@code options.setBypassSslCertCheck(true)} is there, then all other
 *    checks will be left out and the connection will succeed unless
 *    there are other errors like wrong credentials, non-responsive
 *    server etc.
 *
 *    If a truststore (only for self-signed certificates) has to be used,
 *    one can set {@code options.setTrustStoreFilePath("/path/to/truststore.jks")}
 *    and {@code options.setTrustStorePassword("password_for_truststore")}, but in
 *    this case {@code options.setBypassSslCertCheck( true )} cannot be used.
 * <pre>
 *         String url = "https://your_server_ip_or_FQDN:8082/gpudb-0";
 *
 *         GPUdb.Options options = new GPUdb.Options();
 *         options.setUsername("user");
 *         options.setPassword("password");
 *         options.setBypassSslCertCheck( true );
 *
 *         GPUdb gpudb = new GPUdb( url, options );
 * </pre>
 *
 * C) This setup will enforce checking any security related configurations
 * and will proceed to set up a secured connection only if the server has SSL set
 * up and a certificate from a well-known CA is available. In this case, the Java
 * runtime makes sure that the server certificate is validated without the user
 * having to specify anything.
 *
 * <pre>
 *         String url = "https://your_server_ip_or_FQDN:8082/gpudb-0";
 *
 *         GPUdb.Options options = new GPUdb.Options();
 *         options.setUsername("user");
 *         options.setPassword("password");
 *
 *         GPUdb gpudb = new GPUdb( url, options );
 * </pre>
 *
 * @see GPUdbBase.Options
 * @see GPUdbBase.Options#setBypassSslCertCheck(boolean)
*/

public abstract class GPUdbBase {

    // The amount of time for checking if a given IP/hostname is good (10 seconds)
    private static final int DEFAULT_SERVER_CONNECTION_TIMEOUT = 10000;

    // The amount of time of inactivity after which the connection would be
    // validated: 100ms
    private static final int DEFAULT_CONNECTION_INACTIVITY_VALIDATION_TIMEOUT = 100;

    // The default port for host manager URLs
    private static final int DEFAULT_HOST_MANAGER_PORT = 9300;

    // The default port for host manager URLs when using HTTPD
    private static final int DEFAULT_HTTPD_HOST_MANAGER_PORT = 8082;

    // The number of times that the API will attempt to submit a host
    // manager endpoint request.  We need this in case the user chose
    // a bad host manager port.  We don't want to go into an infinite
    // loop
    private static final int HOST_MANAGER_SUBMIT_REQUEST_RETRY_COUNT = 3;

    // The timeout (in milliseconds) used for checking the status of a node; we
    // use a small timeout so that it does not take a long time to figure out
    // that a rank is down.  Using 1.5 seconds.
    private static final int DEFAULT_INTERNAL_ENDPOINT_CALL_TIMEOUT = 1500;

    // The timeout interval (in milliseconds) used when trying to establish a
    // connection to the database at GPUdb initialization time.  The default
    // is 0 (no retry).
    private static final long DEFAULT_INITIAL_CONNECTION_ATTEMPT_TIMEOUT_MS = 0;

    // The maximum number of connections across all or an individual host
    // used in setting up the {@link PoolingHttpClientConnectionManager} instance
    // in the {@link #init} method.
    private static final int DEFAULT_MAX_TOTAL_CONNECTIONS    = 40;
    private static final int DEFAULT_MAX_CONNECTIONS_PER_HOST = 10;

    // Used to set the number of retries for HTTP requests.
    // KECO-2150 - Handle 'Failed to respond' errors resulting from
    // NoHttpResponseException due to socket HALF_OPEN states.
    private static final int HTTP_REQUEST_MAX_RETRY_ATTEMPTS = 5;

    // This is used to control the timeout value for obtaining
    // a connection from the HTTP connection pool. Right now it is
    // set to 3 minutes which is the default as per the Apache
    // HttpClient RequestConfig.Builder javadocs.
    private static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = 3; // Minutes

    // JSON parser
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    // Some string constants to be used internally
    private static final String DATABASE_SERVER_VERSION_KEY = "version.gpudb_core_version";

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

        private String trustStoreFilePath;

        private String trustStorePassword;

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
        private long  initialConnectionAttemptTimeout = DEFAULT_INITIAL_CONNECTION_ATTEMPT_TIMEOUT_MS;
        private int   maxTotalConnections   = DEFAULT_MAX_TOTAL_CONNECTIONS;
        private int   maxConnectionsPerHost = DEFAULT_MAX_CONNECTIONS_PER_HOST;

        /**
         * No-argument constructor needed for the copy constructor.
         */
        public Options() {}

        /**
         * Copy constructor.
         * Note: Member executor will get a shallow copy.
         */
        public Options( Options other ) {
            super();

            this.primaryUrl           = other.primaryUrl;
            this.username             = other.username;
            this.password             = other.password;
            this.hostnameRegex        = other.hostnameRegex;
            this.useSnappy            = other.useSnappy;
            this.bypassSslCertCheck   = other.bypassSslCertCheck;
            this.disableFailover      = other.disableFailover;
            this.disableAutoDiscovery = other.disableAutoDiscovery;
            this.haFailoverOrder      = other.haFailoverOrder;
            this.httpHeaders          = new HashMap<>( other.httpHeaders );
            this.timeout              = other.timeout;
            this.serverConnectionTimeout     = other.serverConnectionTimeout;
            this.threadCount                 = other.threadCount;
            this.hmPort                      = other.hmPort;
            this.maxTotalConnections         = other.maxTotalConnections;
            this.maxConnectionsPerHost       = other.maxConnectionsPerHost;
            this.initialConnectionAttemptTimeout = other.initialConnectionAttemptTimeout;
            this.connectionInactivityValidationTimeout = other.connectionInactivityValidationTimeout;
            this.trustStoreFilePath = other.trustStoreFilePath;
            this.trustStorePassword = other.trustStorePassword;

            // Shallow copy; not cloneable and no copy constructor available
            this.executor             = other.executor;
        }

        /**
         * Returns the set of options as a JSON-style string
         * 
         * @return options as a String
         */
        public String toString() {
            final StringBuilder s = new StringBuilder("{");

            s.append("username: ").append(this.username).append(", ");
            s.append("password: ").append(this.password == null ? "null" : "********").append(", ");
            s.append("trustStoreFilePath: ").append(this.trustStoreFilePath).append(", ");
            s.append("trustStorePassword: ").append(this.trustStorePassword == null ? "null" : "********").append(", ");
            s.append("bypassSslCertCheck: ").append(this.bypassSslCertCheck).append(", ");
            s.append("primaryUrl: ").append(this.primaryUrl).append(", ");
            s.append("hostnameRegex: ").append(this.hostnameRegex).append(", ");
            s.append("disableFailover: ").append(this.disableFailover).append(", ");
            s.append("disableAutoDiscovery: ").append(this.disableAutoDiscovery).append(", ");
            s.append("haFailoverOrder: ").append(this.haFailoverOrder).append(", ");
            s.append("httpHeaders: ").append(this.httpHeaders).append(", ");
            s.append("hmPort: ").append(this.hmPort).append(", ");
            s.append("timeout: ").append(this.timeout).append(", ");
            s.append("serverConnectionTimeout: ").append(this.serverConnectionTimeout).append(", ");
            s.append("initialConnectionAttemptTimeout: ").append(this.initialConnectionAttemptTimeout).append(", ");
            s.append("connectionInactivityValidationTimeout: ").append(this.connectionInactivityValidationTimeout).append(", ");
            s.append("threadCount: ").append(this.threadCount).append(", ");
            s.append("maxTotalConnections: ").append(this.maxTotalConnections).append(", ");
            s.append("maxConnectionsPerHost: ").append(this.maxConnectionsPerHost).append(", ");
            s.append("useSnappy: ").append(this.useSnappy);

            s.append("}");

            return s.toString();
        }

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
         * Gets the value of the SSL trustStore file path
         * @return - a String - SSL trustStore file path
         */
        public String getTrustStoreFilePath() {
            return trustStoreFilePath;
        }

        /**
         * Gets the SSL trustStore file password
         * @return - a String - trustStore file password
         */
        public String getTrustStorePassword() {
            return trustStorePassword;
        }

        /**
         * Gets the value of the flag indicating whether to disable failover
         * upon failures.
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
         * @return  the value of the automatic discovery disabling flag
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
         * @see #setHAFailoverOrder(GPUdbBase.HAFailoverOrder)
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
         * @return  the connection inactivity period (in milliseconds)
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
         * @deprecated
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
            return 0;
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
         * @deprecated
         * Gets the timeout used when trying to recover from an intra-cluster
         * failover event.  The value is given in milliseconds.  Returns 0.
         *
         * @return  the intraClusterFailoverTimeout value
         *
         * @see #setIntraClusterFailoverTimeout( long )
         */
        public long getIntraClusterFailoverTimeout() {
            return 0;
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
         * Note also that the regex must match all servers in all clusters of
         * the ring, as there is only one in use per connection.
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
         * Note also that the regex must match all servers in all clusters of
         * the ring, as there is only one in use per connection.
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
         * Sets the SSL trustStore file path. This is useful if the user wants
         * to pass in a self-signed certificate. If this is passed in then
         * the corresponding password for the file must also be set.
         *
         * <pre>
         *   GPUdbBase.Options m_gpudbOptions = new GPUdbBase.Options();
         *   m_gpudbOptions = m_gpudbOptions.setTrustStoreFilePath(m_trustStoreFileName);
         *   m_gpudbOptions = m_gpudbOptions.setTrustStorePassword(m_trustStorePassword);
         * </pre>
         *
         * @param trustStoreFilePath - String indicating the full file path
         * @return - the current {@link Options} instance
         *
         * @see #setTrustStorePassword(String)
         */
        public Options setTrustStoreFilePath(String trustStoreFilePath) {
            this.trustStoreFilePath = trustStoreFilePath;
            return this;
        }

        /**
         * Sets the SSL trustStore file password. This is useful if the user wants
         * to pass in a self-signed certificate.
         *
         * <pre>
         *   GPUdbBase.Options m_gpudbOptions = new GPUdbBase.Options();
         *   m_gpudbOptions = m_gpudbOptions.setTrustStoreFilePath(m_trustStoreFileName);
         *   m_gpudbOptions = m_gpudbOptions.setTrustStorePassword(m_trustStorePassword);
         * </pre>
         *
         * @param trustStorePassword - String indicating the full file path
         * @return - the current {@link Options} instance
         *
         * @see #setTrustStoreFilePath(String) (String)
         */
        public Options setTrustStorePassword(String trustStorePassword) {
            this.trustStorePassword = trustStorePassword;
            return this;
        }

        /**
         * Sets the value of the flag indicating whether to disable failover
         * upon failures.
         * If {@code true}, then no failover would be attempted upon triggering
         * events regardless of the availability of a high availability cluster.
         *
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
         * then the GPUdb object will not connect to the database at
         * initialization time, and will only work with the URLs given.
         *
         * @param value  the value of the automatic discovery disabling flag
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
         * @see #getHAFailoverOrder()
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
         * @param value  the connection inactivity timeout (in milliseconds)
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
         * @deprecated
         * Sets the number of times the API tries to reconnect to the
         * same cluster (when a failover event has been triggered),
         * before actually failing over to any available backup
         * cluster.
         *
         * @param value  the clusterReconnectCount value
         * @return       the current {@link Options} instance
         */
        public Options setClusterReconnectCount(int value) {
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
         * @deprecated
         * Sets the timeout used when trying to recover from an intra-cluster
         * failover event.  The value is given in milliseconds.
         *
         * @param value  the intraClusterFailoverTimeout value
         * @return       the current {@link Options} instance
         *
         * @see #getIntraClusterFailoverTimeout()
         */
        public Options setIntraClusterFailoverTimeout(long value) {
            return this;
        }

    }  // end class Options

    /*
        One of the major shortcomings of the classic blocking I/O model is that the network socket can react to I/O events
        only when blocked in an I/O operation. When a connection is released back to the manager, it can be kept alive however
        it is unable to monitor the status of the socket and react to any I/O events. If the connection gets closed on the
        server side, the client side connection is unable to detect the change in the connection state (and react
        appropriately by closing the socket on its end).

        HttpClient tries to mitigate the problem by testing whether the connection is 'stale', that is no longer valid
        because it was closed on the server side, prior to using the connection for executing an HTTP request. The stale
        connection check is not 100% reliable. The only feasible solution that does not involve a one thread per socket
        model for idle connections is a dedicated monitor thread used to evict connections that are considered expired due
        to a long period of inactivity. The monitor thread can periodically call ClientConnectionManager#closeExpiredConnections()
        method to close all expired connections and evict closed connections from the pool. It can also optionally call
        ClientConnectionManager#closeIdleConnections() method to close all connections that have been idle over a given period of time.
     */


    /**
     * Contains the version of the client API or the GPUdb server.
     * Has helper methods to compare with other versions.
     */
    public static final class GPUdbVersion {
        private final int major;
        private final int minor;
        private final int revision;
        private final int abiVersion;

        public GPUdbVersion( int major, int minor,
                             int revision, int abiVersion ) {
            this.major = major;
            this.minor = minor;
            this.revision = revision;
            this.abiVersion = abiVersion;
        }

        /**
         * Gets the major (first) component of the version.
         *
         * @return  the 'major' component value
         */
        public int getMajor() {
            return this.major;
        }

        /**
         * Gets the minor (second) component of the version.
         *
         * @return  the 'minor' component value
         */
        public int getMinor() {
            return this.minor;
        }

        /**
         * Gets the revision (third) component of the version.
         *
         * @return  the 'revision' component value
         */
        public int getRevision() {
            return this.revision;
        }

        /**
         * Gets the ABI version (fourth) component of the version.
         *
         * @return  the 'ABI version' component value
         */
        public int getAbiVersion() {
            return this.abiVersion;
        }


        /**
         * Given all four components of a version, check if this version
         * object is newer than the given one (not equal to).
         *
         * @return true if this version is newer than the given version.
         */
        public boolean isNewerThan( int major, int minor,
                                    int revision, int abiVersion ) {
            if ( this.major > major ) {
                // Example: _7_.2.1.0 is newer than _5_.0.0.0
                return true;
            }
            if ( (this.major == major) && (this.minor > minor) ) {
                // Example: 6._2_.0.0 is newer than 6._1_.1.0, but
                // 7._2_.0.0 is NOT newer than 8._1_.0.0
                return true;
            }
            if ( (this.major == major) && (this.minor == minor)
                && (this.revision > revision) ) {
                // Example: 6.2._2_.0 is newer than 6.2._1_.0, but
                // 7.7._3_.0 is NOT newer than 9.9._0_.0
                return true;
            }
            if ( (this.major == major) && (this.minor == minor)
                && (this.revision == revision)
                && (this.abiVersion > abiVersion) ) {
                // Example: 6.2.1._3_ is newer than 6.2.1._1_, but
                // 7.7.3._1_ is NOT newer than 9.9.0._0_
                return true;
            }

            // All others must be a newer version than this
            return false;
        }  // isNewerrThan

        /**
         * Given all four components of a version, check if this version
         * object is older than the given one (not equal to).
         *
         * @return true if this version is older than the given version.
         */
        public boolean isOlderThan( int major, int minor,
                                    int revision, int abiVersion ) {
            if ( this.major < major ) {
                // Example: _6_.2.1.0 is older than _7_.0.0.0
                return true;
            }
            if ( (this.major == major) && (this.minor < minor) ) {
                // Example: 6._1_.1.0 is older than 6._2_.0.0, but
                // 8._1_.0.0 is NOT older than 7._2_.0.0
                return true;
            }
            if ( (this.major == major) && (this.minor == minor)
                && (this.revision < revision) ) {
                // Example: 6.2._1_.0 is older than 6.2._2_.0, but
                // 9.9._0_.0 is NOT older than 7.7._3_.0
                return true;
            }
            if ( (this.major == major) && (this.minor == minor)
                && (this.revision == revision)
                && (this.abiVersion < abiVersion) ) {
                // Example: 6.2.1._1_ is older than 6.2.1._3_, but
                // 9.9.0._0_ is NOT older than 7.7.3._1_
                return true;
            }
            // All others must be an older version than this
            return false;
        }  // end isOlderThan


        /**
         * Given all four components of a version, check if this version
         * object is to the given one.
         *
         * @return true if this version is the same as the given version.
         */
        public boolean isEqualTo( int major, int minor,
                                  int revision, int abiVersion ) {
            return ( (this.major == major)
                && (this.minor == minor)
                && (this.revision == revision)
                && (this.abiVersion == abiVersion) );
        }

        @Override
        public boolean equals(Object obj) {
            if( obj == this ) {
                return true;
            }

            if( (obj == null) || (obj.getClass() != this.getClass()) ) {
                return false;
            }

            GPUdbVersion that = (GPUdbVersion)obj;

            return ( (this.major == that.major)
                && (this.minor == that.minor)
                && (this.revision == that.revision)
                && (this.abiVersion == that.abiVersion) );
        }

        @Override
        public String toString() {

            return this.major +
                    "." +
                    this.minor +
                    "." +
                    this.revision +
                    "." +
                    this.abiVersion;
        }

    }  // end class GPUdbVersion


    public static final class InsertRecordsJsonRequest extends InsertRecordsFromPayloadRequest {

        @Override
        public String getTableName() {
            return super.getTableName();
        }

        @Override
        public InsertRecordsFromPayloadRequest setTableName(String tableName) {
            return super.setTableName(tableName);
        }

        @Override
        public String getDataText() {
            return super.getDataText();
        }

        @Override
        public InsertRecordsFromPayloadRequest setDataText(String dataText) {
            return super.setDataText(dataText);
        }

        @Override
        public ByteBuffer getDataBytes() {
            throw new NotImplementedException("Method not available");
        }

        @Override
        public InsertRecordsFromPayloadRequest setDataBytes(ByteBuffer dataBytes) {
            throw new NotImplementedException("Method not available");
        }

        @Override
        public Map<String, Map<String, String>> getModifyColumns() {
            throw new NotImplementedException("Method not available");
        }

        @Override
        public InsertRecordsFromPayloadRequest setModifyColumns(Map<String, Map<String, String>> modifyColumns) {
            throw new NotImplementedException("Method not available");
        }

        @Override
        public Map<String, String> getCreateTableOptions() {
            return super.getCreateTableOptions();
        }

        @Override
        public InsertRecordsFromPayloadRequest setCreateTableOptions(Map<String, String> createTableOptions) {
            return super.setCreateTableOptions(createTableOptions);
        }

        @Override
        public Map<String, String> getOptions() {
            return super.getOptions();
        }

        @Override
        public InsertRecordsFromPayloadRequest setOptions(Map<String, String> options) {
            return super.setOptions(options);
        }

        @Override
        public Schema getSchema() {
            throw new NotImplementedException("Method not available");
        }

        public InsertRecordsJsonRequest() {
        }
    }

    /**
     * An exception that occurred during the submission of a request to GPUdb.
     */
    public static final class SubmitException extends GPUdbException {
        private static final long serialVersionUID = 1L;

        private final URL url;
        private transient IndexedRecord request;

        private transient String payload;
        private int requestSize;

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

        private SubmitException(URL url, String payload, String message, Throwable cause) {
            super(message, cause);
            this.url = url;
            this.payload = payload;
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

        private SubmitException(URL url, String payload, String errorMsg) {
            super( errorMsg );
            this.url = url;
            this.payload = payload;
        }

        private SubmitException(URL url, String errorMsg) {
            super( errorMsg );
            this.url = url;
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
     * This class provides options to control JSON ingest using {@link BulkInserter}.
     * By default, for performance reasons {@link JsonOptions#VALIDATE_JSON} is set to
     * false and {@link JsonOptions#COMPRESSION_ON} set to true
     */
    public static final class JsonOptions {

        public static final boolean VALIDATE_JSON = false;
        public static final boolean COMPRESSION_ON = true;

        private final boolean validateJson;
        private final boolean compressionOn;

        /**
         * This method returns an instance of {@link JsonOptions} with default values.
         * @return - a new instance of {@link JsonOptions} class
         */
        public static JsonOptions defaultOptions() {
            return new JsonOptions();
        }

        /**
         * Default constructor
         */
        public JsonOptions() {
            this.validateJson = VALIDATE_JSON;
            this.compressionOn = COMPRESSION_ON;
        }

        public JsonOptions(boolean validateJson, boolean compressionOn) {
            this.validateJson = validateJson;
            this.compressionOn = compressionOn;
        }

        public boolean isValidateJson() {
            return validateJson;
        }

        public boolean isCompressionOn() {
            return compressionOn;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            JsonOptions that = (JsonOptions) o;
            return isValidateJson() == that.isValidateJson() && isCompressionOn() == that.isCompressionOn();
        }

        @Override
        public int hashCode() {
            return Objects.hash(isValidateJson(), isCompressionOn());
        }

        @Override
        public String toString() {
            return "JsonOptions{" +
                    "validateJson=" + validateJson +
                    ", compressionOn=" + compressionOn +
                    '}';
        }
    }


    /**
     * A special exception indicating the server is shutting down
     */
    public static class GPUdbExitException extends GPUdbException {
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
    public static class GPUdbUnauthorizedAccessException extends GPUdbException {
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
    @SuppressWarnings("unused")
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
    private static final String DB_SHUTTING_DOWN_ERROR_MESSAGE      = "System shutting down";
    private static final String DB_SYSTEM_LIMITED_ERROR_MESSAGE     = "system-limited-fatal";
    private static final String DB_EOF_FROM_SERVER_ERROR_MESSAGE    = "Unexpected end of file from server";

    // Endpoints
    private static final String ENDPOINT_SHOW_SYSTEM_STATUS     = "/show/system/status";
    private static final String ENDPOINT_SHOW_SYSTEM_PROPERTIES = "/show/system/properties";

    // Constants used in endpoint responses
    private static final String SHOW_SYSTEM_STATUS_RESPONSE_SYSTEM  = "system";
    private static final String SHOW_SYSTEM_STATUS_RESPONSE_STATUS  = "status";
    private static final String SHOW_SYSTEM_STATUS_RESPONSE_RUNNING = "running";

    private static final String SYSTEM_PROPERTIES_RESPONSE_ENABLE_HTTPD    = "conf.enable_httpd_proxy";
    private static final String SYSTEM_PROPERTIES_RESPONSE_NUM_HOSTS       = "conf.number_of_hosts";
    private static final String SYSTEM_PROPERTIES_RESPONSE_HEAD_NODE_URLS  = "conf.ha_ring_head_nodes_full";
    private static final String SYSTEM_PROPERTIES_RESPONSE_SERVER_URLS     = "conf.worker_http_server_urls";
    private static final String SYSTEM_PROPERTIES_RESPONSE_TRUE            = "TRUE";

    // Internally used headers (make sure to add them to PROTECTED_HEADERS)
    protected static final String HEADER_HA_SYNC_MODE  = "X-Kinetica-Group";
    protected static final String HEADER_AUTHORIZATION = "Authorization";
    protected static final String HEADER_CONTENT_TYPE  = "Content-type";

    // Headers that are read-only once the connection has been created
    protected static final String[] PROTECTED_HEADERS = new String[]{
        HEADER_HA_SYNC_MODE,
        HEADER_AUTHORIZATION,
        HEADER_CONTENT_TYPE
    };

    protected static final String SslErrorMessageFormat = "<%s>.  To fix, either:\n" +
            "* Specify a trust store containing the server's certificate or a CA cert in the server's certificate path\n" +
            "* Skip the certificate check using the bypassSslCertCheck option\n" +
            "  Examples:  https://docs.kinetica.com/7.1/api/concepts/#https-without-certificate-validation";

    // Internal classes
    // ----------------

    /**
     * Helper class which contains all possible address related information
     * for a given Kinetica cluster.  Used to keep track of the multiple
     * Kinetica clusters' addresses.
     */
    public static class ClusterAddressInfo {
        // Members
        private URL           activeHeadNodeUrl;
        private List<URL>     workerRankUrls;
        private Set<String>   hostNames;  // could have IPs, too
        private URL           hostManagerUrl;
        private boolean       isPrimaryCluster;

        // Constructors
        // ------------
        protected ClusterAddressInfo( URL activeHeadNodeUrl,
                                      List<URL> workerRankUrls,
                                      Set<String> hostNames,
                                      URL hostManagerUrl,
                                      boolean isPrimaryCluster) throws GPUdbException {
            this.activeHeadNodeUrl = activeHeadNodeUrl;
            this.workerRankUrls    = workerRankUrls;
            this.hostNames         = hostNames;
            this.hostManagerUrl    = hostManagerUrl;
            this.isPrimaryCluster  = isPrimaryCluster;

            // Ensure that all the known ranks' hostnames are also accounted for
            updateHostnamesBasedOnRankUrls();
        }

        /**
         * @deprecated
         * Constructor for intra-cluster failover-enabled systems
         */
        protected ClusterAddressInfo( URL activeHeadNodeUrl,
                        List<URL> workerRankUrls,
                        Set<String> hostNames,
                        URL hostManagerUrl,
                        boolean isPrimaryCluster,
                        boolean isIntraClusterFailoverEnabled ) throws GPUdbException {
            this(activeHeadNodeUrl, workerRankUrls, hostNames, hostManagerUrl, isPrimaryCluster);
        }

        /// As close to a default constructor as we can get...
        protected ClusterAddressInfo( URL activeHeadNodeUrl ) throws GPUdbException {
            this.activeHeadNodeUrl    = activeHeadNodeUrl;

            // Set default values for the rest of the members
            this.workerRankUrls   = new ArrayList<>();
            this.hostNames        = new HashSet<>();
            this.isPrimaryCluster = false;

            // Create a host manager URL with the Kinetica default port for
            // host managers
            try {
                if ( !activeHeadNodeUrl.getPath().isEmpty() ) {
                    // If we're using HTTPD, then use the appropriate URL
                    // (likely, http[s]://hostname_or_IP:port/gpudb-host-manager)
                    // Also, use the default httpd port (8082, usually)
                    this.hostManagerUrl = new URL(
                            activeHeadNodeUrl.getProtocol(),
                            activeHeadNodeUrl.getHost(),
                            DEFAULT_HTTPD_HOST_MANAGER_PORT,
                            "/gpudb-host-manager"
                    );
                } else {
                    // The host manager URL shouldn't use any path and
                    // use the host manager port
                    this.hostManagerUrl = new URL(
                            activeHeadNodeUrl.getProtocol(),
                            activeHeadNodeUrl.getHost(),
                            DEFAULT_HOST_MANAGER_PORT,
                            ""
                    );
                }
            } catch ( MalformedURLException ex ) {
                throw new GPUdbException( "Error in creating the host manager URL: " + ex.getMessage(), ex );
            }

            GPUdbLogger.debug_with_info( "Created host manager URL: " + this.hostManagerUrl );

            // Ensure that all the known ranks' hostnames are also accounted for
            updateHostnamesBasedOnRankUrls();
        }

        // Allow a host manager port along with the active URL
        protected ClusterAddressInfo( URL activeHeadNodeUrl,
                                      int hostManagerPort ) throws GPUdbException {
            this.activeHeadNodeUrl = activeHeadNodeUrl;

            // Set default values for the rest of the members
            this.workerRankUrls   = new ArrayList<>();
            this.hostNames        = new HashSet<>();
            this.isPrimaryCluster = false;

            // Create a host manager URL with the given port for host managers
            try {
                if ( !activeHeadNodeUrl.getPath().isEmpty() ) {
                    // If we're using HTTPD, then use the appropriate URL
                    // (likely, http[s]://hostname_or_IP:port/gpudb-host-manager)
                    // Also, use the default httpd port (8082, usually)
                    this.hostManagerUrl = new URL(
                            activeHeadNodeUrl.getProtocol(),
                            activeHeadNodeUrl.getHost(),
                            hostManagerPort,
                            "/gpudb-host-manager"
                    );
                } else {
                    // The host manager URL shouldn't use any path and
                    // use the host manager port
                    this.hostManagerUrl = new URL(
                            activeHeadNodeUrl.getProtocol(),
                            activeHeadNodeUrl.getHost(),
                            hostManagerPort,
                            ""
                    );
                }
            } catch ( MalformedURLException ex ) {
                throw new GPUdbException( "Error creating the host manager URL: " + ex.getMessage(), ex );
            }

            GPUdbLogger.debug_with_info( "Created host manager URL: " + this.hostManagerUrl );

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

        /**
         * @deprecated
         * Get whether intra-cluster failover is enabled
         */
        public boolean getIsIntraClusterFailoverEnabled() {
            return false;
        }

        /**
         * Another getter for the primary cluster boolean flag for convenience.
         */
        public boolean isPrimaryCluster() {
            return this.isPrimaryCluster;
        }

        /**
         * @deprecated
         * Get whether intra-cluster failover is enabled
         */
        public boolean isIntraClusterFailoverEnabled() {
            return false;
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
         * @deprecated
         * Set whether this cluster has intra-cluster failover enabled.
         * Return this object to be able to chain operations.
         */
        public ClusterAddressInfo setIsIntraClusterFailoverEnabled( boolean value ) {
            return this;
        }


        // Private Helper Methods
        // ----------------------
        /**
         * Add the hostnames of the head and worker ranks URLs to the
         * list of hostnames if they are not already part of it.
         */
        private void updateHostnamesBasedOnRankUrls() {
            GPUdbLogger.debug_with_info( "Updating hostname list: " + this.hostNames );

            // Put the head rank's hostname in the saved hostnames (only if
            // it doesn't exist there already)
            if ( !doesClusterContainNode( this.activeHeadNodeUrl.getHost() ) ) {
                String headRankHostname =
                        this.activeHeadNodeUrl.getProtocol() + "://" + this.activeHeadNodeUrl.getHost();
                GPUdbLogger.debug_with_info( "Adding head rank's hostname to hostname list: " + headRankHostname );
                this.hostNames.add( headRankHostname );
            }

            // Put each worker rank's hostname in the saved hostnames (only if
            // it doesn't exist there already)
            for (URL workerRank : this.workerRankUrls) {
                // Check if this worker rank's host is already accounted for
                if (!doesClusterContainNode(workerRank.getHost())) {
                    String workerRankHostname =
                            workerRank.getProtocol() + "://" + workerRank.getHost();
                    GPUdbLogger.debug_with_info("Adding worker rank's hostname to hostname list: " + workerRankHostname);
                    // Add the worker rank's hostname to the list
                    this.hostNames.add(workerRankHostname);
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
            GPUdbLogger.debug_with_info(String.format("Check for hostname %s in hostname list", hostName));
            for (String hostname_ : this.hostNames) {
                // We need to check for a string subset match since the
                // hostnames contain the protocol as well as the actual hostname
                // or IP address
                try {
                    URL tempUrl = new URL(hostname_);
                    if (tempUrl.getHost().equals(hostName)) {
                        GPUdbLogger.debug_with_info("Found matching hostname in hostname list");
                        return true;
                    }
                } catch (MalformedURLException e) {
                    GPUdbLogger.warn(String.format("Hostname %s is not a well formed URL", hostname_));
                }
            }
            GPUdbLogger.debug_with_info( "Hostname not found in hostname list");
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

            return (
                    this.activeHeadNodeUrl.equals( that.activeHeadNodeUrl )
                    // The order of the worker ranks matter
                    && this.workerRankUrls.equals( that.workerRankUrls )
                    // The order of the hostnames do NOT matter
                    && this.hostNames.equals( that.hostNames )
                    && (this.isPrimaryCluster == that.isPrimaryCluster)
            );
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
            return hashCode;
        }


        @Override
        public String toString() {

            return "{ activeHeadNodeUrl: " +
                    this.activeHeadNodeUrl.toString() +
                    ", workerRankUrls: " +
                    Arrays.toString(this.workerRankUrls.toArray()) +
                    ", hostNames: " +
                    Arrays.toString(this.hostNames.toArray()) +
                    ", hostManagerUrl: " +
                    this.hostManagerUrl +
                    ", isPrimaryCluster: " + this.isPrimaryCluster +
                    " }";
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

    private String trustStoreFilePath;

    private String trustStorePassword;

    private boolean       disableFailover;
    private boolean       disableAutoDiscovery;
    private int           threadCount;
    private int           timeout;
    private int           hostManagerPort;
    private long          initialConnectionAttemptTimeoutNS;
    private GPUdbVersion  serverVersion;
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

        // Initialize the logger before anything else.  This MUST be done
        // before any logging happens!
        GPUdbLogger.initializeLogger();

        if ( url == null ) {
            String errorMsg = ( "Must provide a non-null and non-empty string for the URL; given null" );
            GPUdbLogger.error( errorMsg );
            throw new GPUdbException( errorMsg );
        }

        List<URL> urls;
        try {
            // Not using an unmodifiable list because we'll have to update it
            // with the HA ring head node addresses
            urls = new ArrayList<>();

            // Split the string on commas, if any
            String[] url_strings = url.split(",");
            for (String url_string : url_strings) {
                urls.add(new URL(url_string));
            }
        } catch (MalformedURLException ex) {
            GPUdbLogger.error( ex.getMessage() );
            throw new GPUdbException(ex.getMessage(), ex);
        }

        init( urls, options );
    }

    protected GPUdbBase(URL url, Options options) throws GPUdbException {
        urlLock = new Object();

        // Initialize the logger before anything else.  This MUST be done
        // before any logging happens!
        GPUdbLogger.initializeLogger();

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

        // Initialize the logger before anything else.  This MUST be done
        // before any logging happens!
        GPUdbLogger.initializeLogger();

        if ( urls.isEmpty() ) {
            String errorMsg = "Must provide at least one URL; gave none!";
            GPUdbLogger.error( errorMsg );
            throw new GPUdbException( errorMsg );
        }

        init( urls, options );
    }


    /**
     * Construct the GPUdbBase object based on the given URLs and options. This is a helper method called from the
     * constructor of the {@link GPUdbBase} class.
     *
     * @param urls - List of URLs passes by the user.
     * @param options - An instance of the {@link GPUdbBase.Options} class.
     * @throws GPUdbException - thrown in a number of cases like:
     *                          1. Error raised by the method in processing the URLs
     *                          2. Error raised in case of any SSL related handling like mismatched certificate etc.
     */
    private void init(List<URL> urls, Options options) throws GPUdbException {
        // Save the options object
        this.options  = options;

        // Save some parameters passed in via the options object
        this.username      = options.getUsername();
        this.password      = options.getPassword();
        this.hostnameRegex = options.getHostnameRegex();

        if ((username != null && !username.isEmpty()) || (password != null && !password.isEmpty())) {
            authorization =
                    "Basic "
                    + Base64.encodeBase64String( ((username != null ? username : "")
                        + ":"
                        + (password != null ? password : "")).getBytes() )
                    .replace("\n", "");
        } else {
            authorization = null;
        }


        // Save various options
        this.useSnappy            = checkSnappy(options.getUseSnappy());
        this.disableFailover      = options.getDisableFailover();
        this.disableAutoDiscovery = options.getDisableAutoDiscovery();
        this.threadCount          = options.getThreadCount();
        this.executor             = options.getExecutor();
        this.timeout              = options.getTimeout();
        this.hostManagerPort      = options.getHostManagerPort();
        this.haFailoverOrder      = options.getHAFailoverOrder();
        this.initialConnectionAttemptTimeoutNS = options.getInitialConnectionAttemptTimeout();

        GPUdbLogger.debug_with_info( "URLs: " + urls.toString());
        GPUdbLogger.debug_with_info( "Options: " + options.toString());

        // Convert the initial connection attempt timeout from milliseconds
        // to nanoseconds
        this.initialConnectionAttemptTimeoutNS = this.initialConnectionAttemptTimeoutNS * 1000000L;

        this.trustStoreFilePath = options.getTrustStoreFilePath() == null ? System.getProperty("javax.net.ssl.trustStore") : options.getTrustStoreFilePath();
        this.trustStorePassword = options.getTrustStorePassword() == null ? System.getProperty("javax.net.ssl.trustStorePassword") : options.getTrustStorePassword();

        // Handle SSL certificate verification bypass for HTTPS connections
        this.bypassSslCertCheck = options.getBypassSslCertCheck();

        if ( this.bypassSslCertCheck ) {
            GPUdbLogger.debug_with_info("Bypassing SSL certificate check for HTTPS connections");

            // This bypass works only for HTTPS connections
            try {
                X509TrustManagerBypass.install();
            } catch (GeneralSecurityException ex) {
                // Not doing anything about it since we're trying to bypass
                // to reduce distractions anyway
            }
        } else {
            GPUdbLogger.debug_with_info(String.format("Using %s trust store for HTTPS connections: ", trustStoreFilePath));
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
            // Allow self-signed certificates
            SSLContext sslContext = null;
            try {
                sslContext = SSLContextBuilder
                    .create()
                    .loadTrustMaterial( new TrustAllStrategy() )
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

        // If 'trustStoreFilePath' and 'trustStorePassword' are both set, use
        // that to create the 'secureSocketFactory' variable unless 'bypassSslCertCheck` is
        // set to 'true'.
        if( (trustStoreFilePath != null && !trustStoreFilePath.isEmpty())
            && (trustStorePassword != null && !trustStorePassword.isEmpty()) && !options.getBypassSslCertCheck()) {
            SSLConnectionSocketFactory connectionSocketFactory;

            TrustStrategy acceptingTrustStrategy = (chain, authType) -> {
                String truststoreFile = trustStoreFilePath;
                String truststorePassword = trustStorePassword;
                KeyStore truststore;
                try {
                    truststore = KeyStore.getInstance(KeyStore.getDefaultType());
                } catch (KeyStoreException e) {
                    String errorMessage = String.format(SslErrorMessageFormat, e.getMessage());
                    GPUdbLogger.error( errorMessage );
                    return false;
                }
                try (FileInputStream truststoreInputStream = new FileInputStream(truststoreFile)) {
                    try {
                        truststore.load(truststoreInputStream, truststorePassword.toCharArray());
                    } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
                        String errorMessage = String.format(SslErrorMessageFormat, e.getMessage());
                        GPUdbLogger.error( errorMessage );
                        return false;
                    }
                } catch (IOException e) {
                    GPUdbLogger.error( e.getMessage() );
                    return false;
                }

                Certificate certificate;
                // Enumerate the certificates in the truststore
                try {
                    ArrayList<String> trustStoreAliasList = Collections.list(truststore.aliases());
                    int trustStoreAliasListSize = trustStoreAliasList.size();
                    if( trustStoreAliasList.size() <= 0 ) {
                        GPUdbLogger.error(String.format("User supplied truststore %s contains no certificates", truststoreFile));
                        return false;
                    }
                    for (int i = 0; i < trustStoreAliasListSize; i++) {
                        String alias = trustStoreAliasList.get(i);
                        try {
                            certificate = truststore.getCertificate(alias);
                            if (chain[0].equals(certificate))
                                return true;
                        } catch (KeyStoreException e) {
                            GPUdbLogger.error(e.getMessage());
                            if( i == trustStoreAliasListSize - 1)
                                return false;
                        }
                    }
                } catch (KeyStoreException e) {
                    String errorMessage = String.format(SslErrorMessageFormat, e.getMessage());
                    GPUdbLogger.error( errorMessage );
                    return false;
                }

                return false;
            };

            try {
                SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(
                        new File(trustStoreFilePath),
                        trustStorePassword.toCharArray(),
                        acceptingTrustStrategy
                    ).build();

                connectionSocketFactory = new SSLConnectionSocketFactory(sslContext, new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostName, SSLSession session) {
                        return true;
                    }
                });
                secureSocketFactory = connectionSocketFactory;
            } catch (Exception e) {
                GPUdbLogger.error(String.format("Exception : %s", e.getMessage()));
                throw new GPUdbException(e.getMessage());
            }

        }

        // And a plain http socket factory
        PlainConnectionSocketFactory plainSocketFactory = PlainConnectionSocketFactory.getSocketFactory();
        Registry<ConnectionSocketFactory> connSocketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
            .register("https", secureSocketFactory)
            .register("http", plainSocketFactory)
            .build();

        // Create a connection pool manager
        final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(
                connSocketFactoryRegistry,
                PoolConcurrencyPolicy.LAX,
                PoolReusePolicy.LIFO,
                TimeValue.ofMinutes(1),
                null,
                null,
                null);

        connectionManager.setMaxTotal( options.getMaxTotalConnections() );
        connectionManager.setDefaultMaxPerRoute( options.getMaxConnectionsPerHost() );

        connectionManager.setDefaultConnectionConfig(ConnectionConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(options.getServerConnectionTimeout()))
            .setSocketTimeout(Timeout.ofMilliseconds(options.getTimeout()))
            .setValidateAfterInactivity( TimeValue.ofMilliseconds( options.getConnectionInactivityValidationTimeout() ))
            .setTimeToLive(TimeValue.ofMinutes(1))
            .build());

        // ToDo - check the config n the next line - commented for now
//        connectionManager.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(Timeout.ofMilliseconds(options.getServerConnectionTimeout())).build());

        // Set the timeout defaults
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout( Timeout.ofMinutes( DEFAULT_CONNECTION_REQUEST_TIMEOUT ) )
                .build();

        // Build the http client.
        this.httpClient = HttpClients.custom()
            .setConnectionManager( connectionManager )
            .setDefaultRequestConfig( requestConfig )
            .evictExpiredConnections()
            .evictIdleConnections(TimeValue.ofSeconds(DEFAULT_CONNECTION_INACTIVITY_VALIDATION_TIMEOUT))
            .setRetryStrategy(new HttpRequestRetryStrategy() {
                @Override
                public boolean retryRequest(HttpRequest httpRequest, IOException e, int executionCount, HttpContext httpContext) {
                    if (executionCount > HTTP_REQUEST_MAX_RETRY_ATTEMPTS) {
                        return false;
                    }

                    // Retry if the exception is one of the ones below
                    return
                        e instanceof NoHttpResponseException ||
                        e instanceof ConnectTimeoutException;
                }

                @Override
                public boolean retryRequest(HttpResponse httpResponse, int i, HttpContext httpContext) {
                    return false;
                }

                @Override
                public TimeValue getRetryInterval(HttpResponse httpResponse, int i, HttpContext httpContext) {
                    return null;
                }
            })
            .build();

        // Parse the user given URL(s) and store information on all clusters
        // running Kinetica--automatically discovers the HA ring head nodes,
        // takes care of the primary URL, and randomizes the backup clusters
        processUrls( urls );

        // Get the Kinetica server's version and store it for future use
        try {
            updateServerVersion();
        } catch (GPUdbException ex) {
            GPUdbLogger.debug_with_info( "Could not update the server version: " + ex.getMessage() );
        }
    }   // end init


    /**
     *  Clean up resources--namely, the HTTPClient object(s).
     */
    protected void finalize() throws Throwable {
        // Release the resources-- the HTTP client

        httpClient.close(CloseMode.GRACEFUL);
    }


    // Properties
    // ----------

    /**
     * Gets the list of URLs of the active head ranks of all the clusters for
     * the GPUdb server. At any given time, one URL will be active and used for
     * all GPUdb calls (call {@link #getURL getURL} to determine which one), but
     * in the event of failure, the other URLs will be tried in a randomized
     * order, and if a working one is found it will become the new active URL.
     *
     * @return  the list of URLs
     */
    public List<URL> getURLs() {
        List<URL> activeHeadNodeURLs = new ArrayList<>();
        for (ClusterAddressInfo hostAddress : this.hostAddresses) {
            activeHeadNodeURLs.add(hostAddress.getActiveHeadNodeUrl());
        }
        return activeHeadNodeURLs;
    }

    /**
     * Gets the list of URLs of the active head ranks of all the clusters for
     * the GPUdb server. At any given time, one URL will be active and used for
     * all GPUdb calls (call {@link #getURL getURL} to determine which one), but
     * in the event of failure, the other URLs will be tried in order, and if a
     * working one is found it will become the new active URL.
     *
     * @return  the list of URLs
     */
    public List<URL> getFailoverURLs() {
        List<URL> failoverUrls = new ArrayList<>();
        for (int urlIndex : this.haUrlIndices)
            failoverUrls.add(this.hostAddresses.get(urlIndex).getActiveHeadNodeUrl());
        return failoverUrls;
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
        List<URL> hmURLs = new ArrayList<>();
        for (ClusterAddressInfo hostAddress : this.hostAddresses) {
            hmURLs.add(hostAddress.getHostManagerUrl());
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
     * Gets the hostname of the primary cluster of the HA environment, if any
     * cluster is identified as the primary cluster.
     *
     * @return  the primary URL hostname or IP address if there is
     *          a primary cluster, empty string otherwise.
     */
    public String getPrimaryHostname() {
        return this.primaryUrlHostname;
    }


    /**
     * Gets the version of the database server that this client is connected
     * to.  If the server version was not successfully saved, then returns null.
     *
     * @return  the server version {@link GPUdbBase.GPUdbVersion}, or null.
     */
    public GPUdbVersion getServerVersion() {
        return this.serverVersion;
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
     * Checks whether Snappy is available on the host system, if requested.
     * 
     * @param requestUseSnappy  whether the user (or default config) has
     *            requested Snappy compression be enabled
     * @return    whether Snappy compression should be enabled, based on
     *            request and system availability
     */
    private boolean checkSnappy(boolean requestUseSnappy) {
        if (requestUseSnappy) {
            try {
                Snappy.getNativeLibraryVersion();
                return true;
            }
            catch (UnsatisfiedLinkError e) {
                GPUdbLogger.warn(
                        "Disabling Snappy compression, as it is unavailable on this system, " +
                        "potentially due to lack of write permission on the system temp directory.  " +
                        "To allow Snappy to use the current system temp directory, allow write permissions to it.  " +
                        "To use a different directory, use the JVM option: -Dorg.xerial.snappy.tempdir=<other/temp/path>  " +
                        "To disable Snappy in Java and remove this warning message, use options.setUseSnappy(false) in the GPUdb constructor.  " +
                        "To disable Snappy in JDBC and remove this warning message, use DisableSnappy=1 in the connection string.  " +
                        "To disable Snappy in KiSQL and remove this warning message, use \"--disableSnappy true\" on the command line.  "
                );
            }
        }

        return false;
    }

    /**
     * Gets whether auto-discovery is enabled or not on the current connection.
     *
     * @return  true if auto-discovery is enabled; false otherwise
     */
    public boolean isAutoDiscoveryEnabled() {
        return !disableAutoDiscovery;
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
     * Gets a copy of the list of ClusterAddressInfo objects that contain
     * information about the Kinetica ring of clusters that this GPUdb
     * object is connected to.
     *
     * @return  the size of the HA cluster
     */
    public List<ClusterAddressInfo> getHARingInfo() {
        return new ArrayList<>(this.hostAddresses);
    }

    /**
     * Gets the size of the high availability ring (i.e. how many clusters
     * are in it).
     *
     * @return  the size of the HA cluster
     */
    public int getHARingSize() {
        return this.hostAddresses.size();
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

        // Ensure that the given header is not a protected header
        for (String protectedHeader : PROTECTED_HEADERS) {
            if (header.equals(protectedHeader)) {
                String errorMsg = ("Not allowed to change protected header: " + header);
                GPUdbLogger.error(errorMsg);
                throw new GPUdbException(errorMsg);
            }
        }

        this.httpHeaders.put(header, value);
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

        // Ensure that the given header is not a protected header
        for (String protectedHeader : PROTECTED_HEADERS) {
            if (header.equals(protectedHeader)) {
                String errorMsg = ("Not allowed to remove protected header: " + header);
                GPUdbLogger.error(errorMsg);
                throw new GPUdbException(errorMsg);
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
     * Select the next cluster based on the HA failover priority set by the user.
     */
    protected synchronized void selectNextCluster() {

        int currClusterIndexPointer = getCurrClusterIndexPointer();

        GPUdbLogger.debug_with_info(String.format(
                "Cluster switch #%s from cluster #%s (%s) to the next one in %s",
                getNumClusterSwitches() + 1, currClusterIndexPointer + 1, getURL(), getFailoverURLs()
        ));

        // Increment the index by one (wrapping around at the list end)
        this.setCurrClusterIndexPointer( (currClusterIndexPointer + 1) % getHARingSize() );

        // Keep a running count of how many times we had to switch clusters
        this.incrementNumClusterSwitches();

        GPUdbLogger.debug_with_info(String.format(
                "Cluster switch #%s to cluster #%s (%s)",
                getNumClusterSwitches(), getCurrClusterIndexPointer() + 1, getURL()
        ));
    }


    /**
     * Switches the URL of the HA ring cluster.  Check if we've circled back to
     * the old URL.  If we've circled back to it, then re-shuffle the list of
     * indices so that the next time, we pick up HA clusters in a different random
     * manner and throw an exception.
     *
     * @param oldURL
     *            the head rank URL in use at the time of the failover that
     *            initiated this switch
     * @param oldNumClusterSwitches
     *            the total number of cluster switches that have occurred
     *            up to the moment before this thread's switch was initiated;
     *            this will be used to determine whether another thread is
     *            already trying to fail over to the next cluster and that
     *            this thread should stand down
     * 
     * @return    the next cluster head rank {@link URL} to try
     */
    protected URL switchURL(URL oldURL, int oldNumClusterSwitches)
        throws
                GPUdbFailoverDisabledException,
                GPUdbHAUnavailableException,
                GPUdbUnauthorizedAccessException
    {
        GPUdbLogger.debug_with_info(String.format(
                "Attempting to switch URLs, from: %s; originally failing URL: %s",
                getURL().toString(), oldURL.toString()));

        if ( this.disableFailover ) {
            GPUdbLogger.debug_with_info( "Failover is disabled; throwing exception" );
            throw new GPUdbFailoverDisabledException( "Failover is disabled!" );
        }

        synchronized (urlLock) {
            // If there is only one URL, then we can't switch URLs
            if ( getHARingSize() == 1 ) {
                GPUdbLogger.debug_with_info( "Only one cluster in ring--no fail-over cluster available");
                throw new GPUdbHAUnavailableException("Only one cluster in ring; HA failover unavailable");
            }

            // Get how many more times other threads have switched clusters
            // since the caller called this function.  If the situation is:
            //
            // count = 0             -> the calling thread is the first to get
            //                          here; switch to the next cluster
            // 0 < count < ring size -> another thread is either in the process
            //                          of switching clusters or has switched to
            //                          a working one; use the new current one
            // count >= ring size    -> another thread has already tried all
            //                          failover clusters; throw exception
            int countClusterSwitchesSinceInvocation = (getNumClusterSwitches() - oldNumClusterSwitches);

            // Check if another thread has tried all the clusters in the HA ring
            boolean haveSwitchedClustersAcrossTheRing = countClusterSwitchesSinceInvocation >= getHARingSize();
            GPUdbLogger.debug_with_info(String.format(
                    "Cluster fail-over attempts across all threads vs. total clusters in ring:  %s vs. %s",
                    countClusterSwitchesSinceInvocation,
                    getHARingSize()
            ));
            if ( haveSwitchedClustersAcrossTheRing ) {
                throw new GPUdbHAUnavailableException("Fail-over attempted as many times as clusters in the ring; URLs attempted: " + getURLs().toString());
            }

            // Check if another thread beat us to switching the URL
            if ( !getURL().equals( oldURL ) && (countClusterSwitchesSinceInvocation > 0) ) {
                GPUdbLogger.debug_with_info( "Already failed over to URL: " + getURL().toString() );
                // Another thread must have already switched the URL; use the
                // new current URL
                return getURL();
            }

            // This thread is the first one here--select the next cluster to use
            // during this HA failover
            this.selectNextCluster();

            // If we've circled back, shuffle the indices again so that future
            // requests go to a different randomly selected cluster, but also
            // let the caller know that we've circled back
            if ( getURL().equals( oldURL ) ) {
                GPUdbLogger.debug_with_info(String.format(
                        "Current URL is the same as the original URL: %s; randomizing URLs and throwing exception",
                        oldURL
                ));
                // Re-shuffle and set the index counter to zero
                randomizeURLs();

                // Let the user know that we've circled back
                throw new GPUdbHAUnavailableException("Circled back to original URL; no clusters available for fail-over among these: " + getURLs().toString());
            }

            // Haven't circled back to the old URL; so return the new one
            GPUdbLogger.warn("Switched to fail-over URL: " +  getURL().toString());
            return getURL();
        }
    }  // end switchURL

    /**
     * Switches the host manager  URL of the HA ring cluster.  Check if we've
     * circled back to the old URL.  If we've circled back to it, then
     * re-shuffle the list of indices so that the next time, we pick up HA
     * clusters in a different random manner and throw an exception.
     *
     * @param oldURL
     *            the host manager URL in use at the time of the failover that
     *            initiated this switch
     * @param oldNumClusterSwitches
     *            the total number of cluster switches that have occurred
     *            up to the moment before this thread's switch was initiated;
     *            this will be used to determine whether another thread is
     *            already trying to fail over to the next cluster and that
     *            this thread should stand down
     * 
     * @return    the next host manager {@link URL} to try
     */
    private URL switchHmURL(URL oldURL, int oldNumClusterSwitches)
        throws
                GPUdbFailoverDisabledException,
                GPUdbHAUnavailableException,
                GPUdbUnauthorizedAccessException
    {
        GPUdbLogger.debug_with_info(String.format(
                "Attempting to switch Host Manager URLs, from: %s; originally failing URL: %s",
                getHmURL().toString(), oldURL.toString()));

        if ( this.disableFailover ) {
            GPUdbLogger.debug_with_info( "Failover is disabled; throwing exception" );
            throw new GPUdbFailoverDisabledException( "Failover is disabled!" );
        }

        synchronized (urlLock) {
            // If there is only one URL, then we can't switch URLs
            if ( getHARingSize() == 1 ) {
                GPUdbLogger.debug_with_info( "Only one cluster in ring--no fail-over cluster available");
                throw new GPUdbHAUnavailableException("Only one cluster in ring; HA failover unavailable");
            }

            // Get how many more times other threads have switched clusters
            // since the caller called this function.  If the situation is:
            //
            // count = 0             -> the calling thread is the first to get
            //                          here; switch to the next cluster
            // 0 < count < ring size -> another thread is either in the process
            //                          of switching clusters or has switched to
            //                          a working one; use the new current one
            // count >= ring size    -> another thread has already tried all
            //                          failover clusters; throw exception
            int countClusterSwitchesSinceInvocation = (getNumClusterSwitches() - oldNumClusterSwitches);

            // Check if another thread has tried all the clusters in the HA ring
            boolean haveSwitchedClustersAcrossTheRing = countClusterSwitchesSinceInvocation >= getHARingSize();
            GPUdbLogger.debug_with_info(String.format(
                    "Host Manager cluster fail-over attempts across all threads vs. total clusters in ring:  %s vs. %s",
                    countClusterSwitchesSinceInvocation,
                    getHARingSize()
            ));
            if ( haveSwitchedClustersAcrossTheRing ) {
                throw new GPUdbHAUnavailableException("Host Manager fail-over attempted as many times as clusters in the ring; URLs attempted: " + getURLs().toString());
            }

            // Check if another thread beat us to switching the URL
            if ( !getHmURL().equals( oldURL )
                && (countClusterSwitchesSinceInvocation > 0) ) {
                GPUdbLogger.debug_with_info( "Already failed over to Host Manager URL: " + getHmURL().toString() );
                // Another thread must have already switched the URL; use the
                // new current URL
                return getHmURL();
            }

            // This thread is the first one here--select the next cluster to use
            // during this HA failover
            this.selectNextCluster();

            // If we've circled back, shuffle the indices again so that future
            // requests go to a different randomly selected cluster, but also
            // let the caller know that we've circled back
            if ( getHmURL().equals( oldURL ) ) {
                GPUdbLogger.debug_with_info(String.format(
                        "Current Host Manager URL is the same as the original URL: %s; randomizing URLs and throwing exception",
                        oldURL
                ));
                // Re-shuffle and set the index counter to zero
                randomizeURLs();

                // Let the user know that we've circled back
                throw new GPUdbHAUnavailableException("Circled back to original URL; no clusters available for Host Manager fail-over among these: " + getHmURLs().toString());
            }

            // Haven't circled back to the old URL; so return the new one
            GPUdbLogger.warn("Switched to Host Manager fail-over URL: " +  getHmURL().toString());
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
            throw new GPUdbException( "Positive timeout value required, given " + timeout );
        }

        HttpPost connection = new HttpPost( url.toURI() );

        // Set the timeout explicitly if it is different from the default value
        if ( timeout != this.timeout ) {
            RequestConfig requestConfigWithCustomTimeout = RequestConfig.custom()
                .setConnectTimeout( Timeout.ofMilliseconds(timeout ))
                .setConnectionRequestTimeout( Timeout.ofMilliseconds(timeout ))
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
            throw new GPUdbException( "Positive timeout value required, given " + timeout );
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
            return -1;
        }

        // Check each cluster for the hostname/IP
        for (int i = 0; i < this.hostAddresses.size(); ++i) {
            if ( this.hostAddresses.get( i ).doesClusterContainNode( hostName ) ) {
                GPUdbLogger.debug_with_info( "Host match found in cluster #" + i);
                return i;
            }
        }

        // Did not find any cluster that uses/has the given hostname/IP address
        GPUdbLogger.debug_with_info( "Did not find any cluster with hostname <" + hostName + ">");
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
            GPUdbLogger.debug_with_info( "Getting system status for URL: " + url);
            statusResponse = submitRequest(
                    appendPathToURL( url, ENDPOINT_SHOW_SYSTEM_STATUS ),
                    new ShowSystemStatusRequest(),
                    new ShowSystemStatusResponse(),
                    false,
                    DEFAULT_INTERNAL_ENDPOINT_CALL_TIMEOUT
            );
        } catch (MalformedURLException ex) {
            throw new GPUdbException( "Error forming URL: " + url + " -- " + ex.getMessage(), ex );
        }

        // Get the 'system' entry in the status response and parse it
        String systemStatusStr = statusResponse.getStatusMap().get( SHOW_SYSTEM_STATUS_RESPONSE_SYSTEM );
        GPUdbLogger.debug_with_info( "Got system status " + systemStatusStr + " for URL: " + url);
        JsonNode systemStatus;
        if ( systemStatusStr == null ) {
            throw new GPUdbException(String.format("No entry for <%s> in %s for URL: %s",
                    SHOW_SYSTEM_STATUS_RESPONSE_SYSTEM, ENDPOINT_SHOW_SYSTEM_STATUS, url));
        } else {
            try {
                systemStatus = GPUdbBase.JSON_MAPPER.readTree( systemStatusStr );
            } catch ( IOException ex ) {
                throw new GPUdbException( "Could not parse system status " + systemStatusStr + " for URL: " + url, ex );
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
            GPUdbLogger.debug_with_info( "Getting system properties for URL: " + url);
            response = submitRequest(
                    appendPathToURL( url, ENDPOINT_SHOW_SYSTEM_PROPERTIES ),
                    new ShowSystemPropertiesRequest(),
                    new ShowSystemPropertiesResponse(),
                    false
            );
        } catch (MalformedURLException ex) {
            throw new GPUdbException( "Error forming URL: " + url + " -- " + ex.getMessage(), ex );
        }

        // Get the property map from the response and return it
        if ( response != null ) {
            GPUdbLogger.debug_with_info( "Got system properties for URL: " + url);
            Map<String, String> systemProperties = response.getPropertyMap();

            // Is HTTPD being used (helps in figuring out the host manager URL
            String is_httpd_enabled_str = systemProperties.get( SYSTEM_PROPERTIES_RESPONSE_ENABLE_HTTPD );

            // Figure out if we're using HTTPD
            if ( (is_httpd_enabled_str != null)
                && (is_httpd_enabled_str.compareToIgnoreCase( SYSTEM_PROPERTIES_RESPONSE_TRUE ) == 0 ) ) {
                GPUdbLogger.debug_with_info( "Setting use httpd to true for URL: " + url);
                this.useHttpd = true;
            }

            // Return the property map
            return systemProperties;
        } else {
            throw new GPUdbException( "Could not get system properties for URL: " + url);
        }
    }   // end getSystemProperties


    /**
     * Given a URL, return whether the server is running at that address.
     *
     * @param url - The URL to check for
     *
     * @throws GPUdbException - Could be {@link GPUdbException}, {@link GPUdbExitException} or {@link GPUdbUnauthorizedAccessException}
     */
    private boolean isSystemRunning( URL url ) throws GPUdbException {
        boolean isSystemRunning = false;
        try {
            JsonNode systemStatusInfo = getSystemStatusInformation( url );

            // Then look for 'status' and see if it is 'running'
            JsonNode systemStatus = systemStatusInfo.get( SHOW_SYSTEM_STATUS_RESPONSE_STATUS );

            if ( ( systemStatus != null)
                && SHOW_SYSTEM_STATUS_RESPONSE_RUNNING.equals( systemStatus.textValue() ) ) {
                isSystemRunning = true;
                GPUdbLogger.debug_with_info(String.format("System running at URL %s", url));
            } else {
                GPUdbLogger.warn(String.format("System not confirmed running at URL %s", url));
            }
        } catch ( Exception ex ) {
            if( ex instanceof GPUdbUnauthorizedAccessException ) {
                throw ex;
            }

            // Any error means we don't know whether the system is running
            GPUdbLogger.warn(String.format(
                    "Exception checking running status of URL %s -- %s",
                    url.toString(), ex
            ));
        }
        return isSystemRunning;
    }

    /**
     * This method inserts a JSON payload (either a single JSON record or an array) into a Kinetica table
     * @param insertRecordsJsonRequest - an instance of {@link InsertRecordsJsonRequest} class
     * @return - the JSON response as a Map of String to Object.
     * @throws GPUdbException - in case of an error raised by the underlying endpoint
     */
    public Map<String, Object> insertRecordsFromJson(InsertRecordsJsonRequest insertRecordsJsonRequest, JsonOptions jsonOptions) throws GPUdbException {
        if( insertRecordsJsonRequest == null ) {
            throw new GPUdbException("Request object is null");
        }
        return insertRecordsFromJson( insertRecordsJsonRequest.getTableName(),
                insertRecordsJsonRequest.getDataText(),
                jsonOptions,
                insertRecordsJsonRequest.getCreateTableOptions(),
                insertRecordsJsonRequest.getOptions());
    }

    /**
     * This method inserts a JSON payload (either a single JSON record or an array) into a Kinetica table
     * with all default options
     * @param jsonRecords - A single JSON record or an array of records as JSON
     * @param tableName - the table to insert the records into
     *
     * @return - the JSON response as a Map of String to Object.
     * @throws GPUdbException - in case of an error raised by the underlying endpoint
     */
    public Map<String, Object> insertRecordsFromJson(String jsonRecords,
                                                     String tableName) throws GPUdbException {
        return insertRecordsFromJson(jsonRecords, tableName, null, null, null);
    }

    /**
     * This method inserts a JSON payload (either a single JSON record or an array) into a Kinetica table
     * @param jsonRecords - A single JSON record or an array of records as JSON
     * @param tableName - the table to insert the records into
     * @param jsonOptions - Indicates whether Snappy compression is to be set on or not
     * @param createTableOptions - an instance of the class
     *        {@link com.gpudb.protocol.InsertRecordsFromPayloadRequest.CreateTableOptions InsertRecordsJsonRequest.CreateTableOptions}
     * @param options - an instance of the class
     *        {@link com.gpudb.protocol.InsertRecordsFromPayloadRequest.Options InsertRecordsJsonRequest.Options}
     *
     * @return - the JSON response as a Map of String to Object.
     * @throws GPUdbException - in case of an error raised by the underlying endpoint
     */
    public Map<String, Object> insertRecordsFromJson(String jsonRecords,
                                                     String tableName,
                                                     JsonOptions jsonOptions,
                                                     Map<String, String> createTableOptions,
                                                     Map<String, String> options) throws GPUdbException {
        if( jsonRecords == null || jsonRecords.isEmpty() ) {
            throw new GPUdbException( "No records to insert" );
        }

        if( tableName == null || tableName.isEmpty() ) {
            throw new GPUdbException( "Table name not given, cannot insert" );
        }

        if( jsonOptions == null ) {
            jsonOptions = JsonOptions.defaultOptions();
        }

        if( createTableOptions == null ) {
            createTableOptions = new LinkedHashMap<>();
        }

        if( options == null ) {
            options = new LinkedHashMap<>();
        }

        //Overwrite the value in the map with the `tableName` parameter value
        options.put( "table_name", tableName );

        Map<String, String> combinedOptions = Stream.of(createTableOptions, options)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (value1, value2) -> value1));

        String endpoint = UrlUtils.constructEndpointUrl( "/insert/records/json", combinedOptions );

        return Collections.unmodifiableMap(submitRequest(endpoint, jsonRecords, jsonOptions.isCompressionOn()));
    }

    /**
     * This class models the response returned by the method {@link #getRecordsJson}
     * The default constructor is used to create instance whenever there is an error
     * encountered by the method {@link #getRecordsJson}.
     * The all argument constructor is used to return the values in case of a successful
     * execution of the method {@link #getRecordsJson}.
     */
    public static final class GetRecordsJsonResponse {
        private int totalRecords;
        private boolean hasMoreRecords;
        private String jsonRecords;

        public GetRecordsJsonResponse() {
        }

        /**
         * All argument constructor
         * 
         * @param totalRecords - Total number of records returned
         * @param hasMoreRecords - Whether there are more records available or not
         * @param jsonRecords - Records as a JSON array string
         */
        public GetRecordsJsonResponse(int totalRecords, boolean hasMoreRecords, String jsonRecords) {
            this.totalRecords = totalRecords;
            this.hasMoreRecords = hasMoreRecords;
            this.jsonRecords = jsonRecords;
        }

        public int getTotalRecords() {
            return totalRecords;
        }

        public boolean hasMoreRecords() {
            return hasMoreRecords;
        }

        public String getJsonRecords() {
            return jsonRecords;
        }

        @Override
        public String toString() {
            return "GetRecordsJsonResponse [totalRecords=" + totalRecords + ", hasMoreRecords=" + hasMoreRecords
                    + ", jsonRecords=" + jsonRecords + "]";
        }
    }


    /**
     * This method is used to retrieve records from a Kinetica table in the form of
     * a JSON array (stringified). The only mandatory parameter is the 'tableName'.
     * The rest are all optional with suitable defaults wherever applicable.
     * 
     * @param tableName - Name of the table
     * @param columnNames - the columns names to retrieve; a value of null will
     *        return all columns
     * @param offset - the offset to start from
     * @param limit - the maximum number of records
     * 
     * @return - an instance of {@link GetRecordsJsonResponse} class
     * 
     * @throws GPUdbException
     */
    public GetRecordsJsonResponse getRecordsJson(
            final String tableName,
            final List<String> columnNames,
            final long offset,
            final long limit
    ) throws GPUdbException {
        return getRecordsJson(tableName, columnNames, offset, limit, null, null, null);
    }


    /**
     * This method is used to retrieve records from a Kinetica table in the form of
     * a JSON array (stringified). The only mandatory parameter is the 'tableName'.
     * The rest are all optional with suitable defaults wherever applicable.
     * 
     * @param tableName - Name of the table
     * @param columnNames - the columns names to retrieve; a value of null will
     *        return all columns
     * @param offset - the offset to start from
     * @param limit - the maximum number of records
     * @param orderByColumns - the list of columns to order by
     * 
     * @return - an instance of {@link GetRecordsJsonResponse} class
     * 
     * @throws GPUdbException
     */
    public GetRecordsJsonResponse getRecordsJson(
            final String tableName,
            final List<String> columnNames,
            final long offset,
            final long limit,
            final List<String> orderByColumns
    ) throws GPUdbException {
        return getRecordsJson(tableName, columnNames, offset, limit, null, orderByColumns, null);
    }


    /**
     * This method is used to retrieve records from a Kinetica table in the form of
     * a JSON array (stringified). The only mandatory parameter is the 'tableName'.
     * The rest are all optional with suitable defaults wherever applicable.
     * 
     * @param tableName - Name of the table
     * @param columnNames - the columns names to retrieve; a value of null will
     *        return all columns
     * @param offset - the offset to start from
     * @param limit - the maximum number of records
     * @param expression - the filter expression
     * 
     * @return - an instance of {@link GetRecordsJsonResponse} class
     * 
     * @throws GPUdbException
     */
    public GetRecordsJsonResponse getRecordsJson(
            final String tableName,
            final List<String> columnNames,
            final long offset,
            final long limit,
            final String expression
    ) throws GPUdbException {
        return getRecordsJson(tableName, columnNames, offset, limit, expression, null, null);
    }


    /**
     * This method is used to retrieve records from a Kinetica table in the form of
     * a JSON array (stringified). The only mandatory parameter is the 'tableName'.
     * The rest are all optional with suitable defaults wherever applicable.
     * 
     * @param tableName - Name of the table
     * @param columnNames - the columns names to retrieve; a value of null will
     *        return all columns
     * @param offset - the offset to start from
     * @param limit - the maximum number of records
     * @param expression - the filter expression
     * @param orderByColumns - the list of columns to order by
     * 
     * @return - an instance of {@link GetRecordsJsonResponse} class
     * 
     * @throws GPUdbException
     */
    public GetRecordsJsonResponse getRecordsJson(
            final String tableName,
            final List<String> columnNames,
            final long offset,
            final long limit,
            final String expression,
            final List<String> orderByColumns
    ) throws GPUdbException {
        return getRecordsJson(tableName, columnNames, offset, limit, expression, orderByColumns, null);
    }


    /**
     * This method is used to retrieve records from a Kinetica table in the form of
     * a JSON array (stringified). The only mandatory parameter is the 'tableName'.
     * The rest are all optional with suitable defaults wherever applicable.
     * 
     * @param tableName - Name of the table
     * @param columnNames - the columns names to retrieve; a value of null will
     *        return all columns
     * @param offset - the offset to start from
     * @param limit - the maximum number of records
     * @param expression - the filter expression
     * @param havingClause - the having clause
     * 
     * @return - an instance of {@link GetRecordsJsonResponse} class
     * 
     * @throws GPUdbException
     */
    public GetRecordsJsonResponse getRecordsJson(
            final String tableName,
            final List<String> columnNames,
            final long offset,
            final long limit,
            final String expression,
            final String havingClause
    ) throws GPUdbException {
        return getRecordsJson(tableName, columnNames, offset, limit, expression, null, havingClause);
    }


    /**
     * This method is used to retrieve records from a Kinetica table in the form of
     * a JSON array (stringified). The only mandatory parameter is the 'tableName'.
     * The rest are all optional with suitable defaults wherever applicable.
     * 
     * @param tableName - Name of the table
     * @param columnNames - the columns names to retrieve
     * @param offset - the offset to start from
     * @param limit - the maximum number of records
     * @param expression - the filter expression
     * @param orderByColumns - the list of columns to order by
     * @param havingClause - the having clause
     * 
     * @return - an instance of {@link GetRecordsJsonResponse} class
     * 
     * @throws GPUdbException
     */
    @SuppressWarnings("unchecked")
    public GetRecordsJsonResponse getRecordsJson(
            final String tableName,
            final List<String> columnNames,
            final long offset,
            final long limit,
            final String expression,
            final List<String> orderByColumns,
            final String havingClause
    ) throws GPUdbException {

        if ( tableName == null || tableName.length() == 0) {
            throw new GPUdbException( "Table name not given, cannot retrieve records" );
        }

        Map<String, String> options = new LinkedHashMap<>();

        options.put( "table_name", tableName );

        if( columnNames != null && columnNames.size() > 0) {
            options.put("column_names", columnNames.stream().collect(Collectors.joining(",")));
        }

        options.put("offset", String.valueOf(offset < 0 ? 0 : offset));
        options.put("limit", String.valueOf(limit < 0 ? GPUdb.END_OF_SET : limit));

        if (expression != null && expression.length() > 0 ) {
            options.put("expression", expression);
        }

        if (orderByColumns != null && orderByColumns.size() > 0 ) {
            options.put("order_by", orderByColumns.stream().collect(Collectors.joining(",")));
        }

        if ( havingClause != null && havingClause.length() > 0 ) {
            options.put("having", havingClause);
        }

        String endpoint = UrlUtils.constructEndpointUrl( "/get/records/json", options );

        String json = submitRequest(endpoint, false);
        TypeReference<HashMap<String,Object>> typeRef
                = new TypeReference<HashMap<String,Object>>() {};
        Map<String, Object> response = new HashMap<>();
        try {
            response = JSON_MAPPER.readValue(json.getBytes(StandardCharsets.UTF_8), typeRef);
        } catch (IOException e) {
            throw new GPUdbException("Error reading JSON response", e);
        }

        if (response != null && response.size() > 0 ){
            String status = (String) response.get("status");
            if( status.equals("OK")) {
                int totalRecords = (Integer) ((Map<String,Object>)response.get("data")).get("total_number_of_records");
                boolean hasMoreRecords = (Boolean) ((Map<String,Object>)response.get("data")).get("has_more_records");

                List<Object> recordList = (List<Object>) ((LinkedHashMap<String,Object>)response.get("data")).get("records");
                String jsonRecords = null;
                try {
                    jsonRecords = JSON_MAPPER.writeValueAsString(recordList);
                } catch (JsonProcessingException e) {
                    throw new GPUdbException("Could not convert records to JSON", e);
                }
                return new GetRecordsJsonResponse(totalRecords, hasMoreRecords, jsonRecords);
            } else {
                throw new GPUdbException("Error getting JSON records: " + response.get("message"));
            }
        } else {
            throw new GPUdbException("Empty response from server gettting JSON records");
        }
    }

    /**
     * Given system properties, extract the head and worker rank URLs.
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
        throws GPUdbHostnameRegexFailureException, GPUdbException {

        List<URL> rankURLs = new ArrayList<>();

        String propertyVal = systemProperties.get( SYSTEM_PROPERTIES_RESPONSE_SERVER_URLS );
        if ( (propertyVal != null) && !propertyVal.isEmpty() ){
            GPUdbLogger.debug_with_info(
                    String.format(
                            "Known rank URLs <%s> from server: %s%s",
                            SYSTEM_PROPERTIES_RESPONSE_SERVER_URLS,
                            propertyVal,
                            hostnameRegex == null || hostnameRegex.pattern().isEmpty() ? "" : " vs. user-given regex: " + hostnameRegex.pattern()
                    )
            );

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

                // Each rank can have multiple URLs (public/private) associated with it
                String[] urls = urlLists[i].split(",");
                boolean found = false;

                // Look through the URLs associated with this rank for a valid one that matches the regex,
                //   or just use the first valid one in the list if there's no regex
                for (String urlString : urls) {
                    URL url;
                    boolean doAdd = false;

                    // Ensure it's a valid URL
                    try {
                        url = new URL(urlString);
                    } catch (MalformedURLException ex) {
                        throw new GPUdbException(ex.getMessage(), ex);
                    }

                    if (hostnameRegex == null) {
                        // No regex is given, so we'll take the first one
                        GPUdbLogger.debug_with_info("Keeping rank URL: " + url);
                        doAdd = true;
                    } else {
                        // Check if this URL matches the given regex
                        doAdd = hostnameRegex.matcher(url.getHost()).matches();
                        if (doAdd)
                            GPUdbLogger.debug_with_info("Keeping matching rank URL: " + url);
                        else
                            GPUdbLogger.debug_with_info("Skipping non-matching rank URL: " + url);
                    }

                    if (doAdd) {
                        // Found a match (whether a regex is given or not)
                        rankURLs.add(url);
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    // If there's no valid URL matching the regex throw a match error
                    if (hostnameRegex != null) {
                        throw new GPUdbHostnameRegexFailureException(
                                "No valid matching IP/hostname found for worker: " + i
                        );
                    }
                    // If there's no valid URL throw an error
                    throw new GPUdbException("No valid IP/hostname found for worker: " + i);
                }
            }
        } else {
            GPUdbLogger.debug_with_info(String.format("No entry for <%s> in %s response",
                    SYSTEM_PROPERTIES_RESPONSE_SERVER_URLS, ENDPOINT_SHOW_SYSTEM_PROPERTIES));
        }

        return rankURLs;
    }  // end getRankURLs



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
        throws GPUdbHostnameRegexFailureException, GPUdbException {

        GPUdbLogger.debug_with_info(String.format(
                "Extracting server-known host names from system properties%s",
                hostnameRegex == null ? "" : " using user-given regex: " + hostnameRegex));
        
        // Get the total number of hosts/machines in the cluster
        String numHostsStr = systemProperties.get( SYSTEM_PROPERTIES_RESPONSE_NUM_HOSTS );
        if (numHostsStr == null) {
            throw new GPUdbException( "Missing value for " + SYSTEM_PROPERTIES_RESPONSE_NUM_HOSTS );
        }
        int numHosts;
        try {
            numHosts = Integer.parseInt( numHostsStr, 10 );
        } catch ( NumberFormatException ex ) {
            throw new GPUdbException( String.format(
                    "Unparsable entry for '%s' (%s); need an integer",
                    SYSTEM_PROPERTIES_RESPONSE_NUM_HOSTS, numHostsStr));
        }


        // Extract the hostnames from the system properties
        Set<String> clusterHostnames = new HashSet<>();
        for (int i = 0; i < numHosts; ++i) {
            // Each hostname is listed individually in the system properties
            // as 'conf.host<i>_public_urls'
            String hostnameKey = String.format("conf.host%s_public_urls", i);

            String hostnameStr = systemProperties.get( hostnameKey );
            if (hostnameStr == null) {
                throw new GPUdbException( String.format("Missing value for %sth hostname '%s'",
                        i, hostnameKey));
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
                if (hostnameRegex == null) {
                    // No regex given, so take the first one
                    GPUdbLogger.debug_with_info("Keeping hostname: " + hostname);
                    doAdd = true;
                } else {
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
                    if (doAdd)
                        GPUdbLogger.debug_with_info("Keeping matching hostname: " + host);
                    else
                        GPUdbLogger.debug_with_info("Skipping non-matching hostname: " + host);
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
                    // match the given regex
                    throw new GPUdbHostnameRegexFailureException(String.format(
                            "No matching hostname found for host #%s (given hostname regex %s)",
                            i, hostnameRegex));
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
        throws GPUdbHostnameRegexFailureException, GPUdbException {

        GPUdbLogger.debug_with_info( "Establishing a cluster record associated with URL: " + url );

        // Get the rank URLs (head and worker ones)
        URL activeHeadNodeUrl;
        List<URL> rankURLs = getRankURLs( systemProperties, this.hostnameRegex );

        // Get the head node URL and keep it separately
        if ( !rankURLs.isEmpty() ) {
            GPUdbLogger.debug_with_info( String.format(
                    "Assigning head rank URL %s from server-known rank URLs: %s",
                    rankURLs.get(0),
                    Arrays.toString( rankURLs.toArray() ) ));
            activeHeadNodeUrl = rankURLs.remove( 0 );
        } else {
            GPUdbLogger.debug_with_info( String.format(
                    "Assigning head rank URL to the user-given one %s, as no server-known worker rank URLs found",
                    url.toString() ));
            activeHeadNodeUrl = url;
        }

        // Get hostnames for all the nodes/machines in the cluster
        Set<String> clusterHostnames = getHostNamesFromSystemProperties( systemProperties, hostnameRegex );

        // Create the host manager URL
        URL hostManagerUrl;
        try {
            // Create the host manager URL using the user given (or default) port
            if ( (this.useHttpd)
                && !activeHeadNodeUrl.getPath().isEmpty() ) {
                // We're using HTTPD, so use the appropriate URL
                // (likely, http[s]://hostname_or_IP:port/gpudb-host-manager)
                // Also, use the default httpd port (8082, usually)
                hostManagerUrl = new URL(
                        activeHeadNodeUrl.getProtocol(),
                        activeHeadNodeUrl.getHost(),
                        // the port will be the same as the
                        // head rank's; we'll just use a
                        // different path
                        activeHeadNodeUrl.getPort(),
                        "/gpudb-host-manager"
                );
            } else {
                // The host manager URL shouldn't use any path and
                // use the host manager port
                hostManagerUrl = new URL(
                        activeHeadNodeUrl.getProtocol(),
                        activeHeadNodeUrl.getHost(),
                        this.hostManagerPort,
                        ""
                );
            }
            GPUdbLogger.debug_with_info( "Created host manager URL: " + hostManagerUrl );
        } catch ( MalformedURLException ex ) {
            throw new GPUdbException( "Error creating the host manager URL: " + ex.getMessage(), ex );
        }

        // Create an object to store all the information about this cluster
        ClusterAddressInfo clusterInfo = new ClusterAddressInfo(
                activeHeadNodeUrl,
                rankURLs,
                clusterHostnames,
                hostManagerUrl,
                false
        );

        // Check if this cluster is the primary cluster
        if ( !this.primaryUrlHostname.isEmpty()
            && clusterInfo.doesClusterContainNode( this.primaryUrlHostname ) ) {
            // Yes, it is; mark this cluster as the primary cluster
            clusterInfo.setIsPrimaryCluster( true );
            GPUdbLogger.debug_with_info( "Marked this cluster as primary" );
        }

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
        throws GPUdbHostnameRegexFailureException, GPUdbException {

        List<URL> haRingHeadNodeURLs = new ArrayList<>();

        // First, find out if the database has a high-availability ring set up
        String is_ha_enabled_str = systemProperties.get( ShowSystemPropertiesResponse.PropertyMap.CONF_ENABLE_HA );

        // Only attempt to parse the HA ring node addresses if HA is enabled
        if ( (is_ha_enabled_str != null)
            && (is_ha_enabled_str.compareToIgnoreCase( ShowSystemPropertiesResponse.PropertyMap.TRUE ) == 0 ) ) {

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

                    for (String urlString : urls) {
                        // If a regex is given, get a matching URL--if there isn't
                        // a match, throw an error.  If no regex is given, take
                        // the first URL.
                        URL url;
                        boolean doAdd = false;

                        // Ensure it's a valid URL
                        try {
                            url = new URL(urlString);
                        } catch (MalformedURLException ex) {
                            throw new GPUdbException(ex.getMessage(), ex);
                        }

                        if (this.hostnameRegex == null) {
                            // No regex is given, so we'll take the first one
                            GPUdbLogger.debug_with_info("Keeping head node URL: " + url);
                            doAdd = true;
                        } else {
                            // Check if this URL matches the given regex
                            doAdd = this.hostnameRegex.matcher(url.getHost()).matches();
                            if (doAdd)
                                GPUdbLogger.debug_with_info("Keeping matching head node URL: " + url);
                            else
                                GPUdbLogger.debug_with_info("Skipping non-matching head node URL: " + url);
                        }

                        if (doAdd) {
                            // Found a match (whether a regex is given or not)
                            haRingHeadNodeURLs.add(url);
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        // No eligible hostname found!
                        if (this.hostnameRegex != null) {
                            // The reason we don't have a URL is because it didn't
                            // match the given regex
                            throw new GPUdbHostnameRegexFailureException(String.format(
                                    "No matching IP/hostname found for cluster with head node URLs %s (given hostname regex %s)",
                                    haRingHeadNodeUrlLists[i], this.hostnameRegex));
                        }
                        throw new GPUdbException("No matching IP/hostname found for cluster with head node URLs " + haRingHeadNodeUrlLists[i] );
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
     * connection attempt timeout, as many times as is possible.
     *
     * ***This method should be called from the constructor initialization
     *    method only!***
     */
    private synchronized void processUrls( List<URL> urls ) throws GPUdbException {

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
                GPUdbLogger.debug_with_info( "Attempting to parse the user-given URLs: " + urls.toString() );
                this.processClusterInformationForAllUrls( urls );
                GPUdbLogger.debug_with_info( "Parsed the user-given URLs successfully: " + this.hostAddresses.toString() );
                return; // one successful attempt is all we need
            } catch (GPUdbHostnameRegexFailureException ex) {
                // There's no point in keep trying since the URLs aren't
                // going to magically change
                throw new GPUdbException(
                        "Could not connect to any working Kinetica server due to hostname regex mismatch (given URLs: " + urls.toString() + "); " + ex.getMessage()
                );
            } catch (GPUdbException ex ) {
                if( ex instanceof GPUdbUnauthorizedAccessException ) {
                    GPUdbLogger.error("Got Unauthorized while communicating to server, cannot proceed ..");
                    throw ex;
                }

                GPUdbLogger.debug_with_info( "Attempt at parsing URLs failed: " + ex.getMessage() );

                // If the user does not want us to retry, parse the URLs as is
                if ( this.initialConnectionAttemptTimeoutNS == 0 ) {
                    GPUdbLogger.debug_with_info( "Initial connection attempt timeout set to 0; parse the given URLs without auto discovery." );
                    this.disableAutoDiscovery = true;
                } else {
                    // Do we keep trying another time?  Has enough time passed?
                    keepTrying = (System.nanoTime() - startTime) <= this.initialConnectionAttemptTimeoutNS;
                    GPUdbLogger.debug_with_info( "Keep trying to parse URLs?: " + keepTrying );
                    if ( keepTrying ) {
                        GPUdbLogger.warn(String.format(
                                "Attempt at parsing user given URLs %s failed; waiting for %s minute(s) before retrying",
                                Arrays.toString( urls.toArray() ), (parseUrlsReattemptWaitInterval / 60000)
                        ));
                        try {
                            // We will sleep before trying again
                            GPUdbLogger.debug_with_info( "Sleeping for " + (parseUrlsReattemptWaitInterval / 60000) + " minutes before trying again" );
                            Thread.sleep( parseUrlsReattemptWaitInterval );

                            // The next time, we will sleep for twice as long
                            parseUrlsReattemptWaitInterval = (2 * parseUrlsReattemptWaitInterval);
                        } catch ( InterruptedException ex2 ) {
                            GPUdbLogger.debug_with_info( "Sleep interrupted (" + ex2.getMessage() + "); throwing exception" );
                            throw new GPUdbException( "Initial parsing of user given URLs interrupted: " + ex2.getMessage(), ex2 );
                        }
                    }
                }
            }
        }

        // We should never get here, but just in case, check that we have got
        // at least one working URL
        if ( getHARingSize() == 0 ) {
            GPUdbLogger.debug_with_info( "No cluster found!" );
            throw new GPUdbException( "Could not connect to any working Kinetica server! " + "Given URLs: " + urls.toString() );
        }
    }   // end parseUrls




    /**
     * Parse the given list of URLs which may have, in any order, URLs
     * for head node (rank-0) or worker ranks of any number of clusters.
     * Sort it all out and save information in a list of objects each
     * of which stores all pertinent information on a given cluster.
     *
     * ***This method should be called from the constructor initialization
     *    method only (via processUrls, not directly)!***
     */
    private synchronized void processClusterInformationForAllUrls(List<URL> urls )
        throws GPUdbHostnameRegexFailureException, GPUdbException {

        this.hostAddresses = new ArrayList<>();

        // Convert the list of URLs to a set (to remove duplicates) and then
        // into a queue (so that we can add HA ring addresses as we get them
        // from servers and add them to the end of the queue while iterating
        // over it--other forms of collections don't allow for it)
        ArrayDeque<URL> urlQueue = new ArrayDeque<>( new HashSet<>( urls ) );

        // If a fully qualified URL is given for the primary URL, process
        // that, too
        String primaryUrlStr = this.options.getPrimaryUrl();

        // Save the hostname of the primary URL (which could be an empty string)
        this.primaryUrlHostname = primaryUrlStr;
        if ( !primaryUrlStr.isEmpty() ) {

            try {
                // If it's a full URL, add it to the queue for processing
                URL primaryUrl = new URL( primaryUrlStr );

                // Add this URL to the list of URLs to process if it's not
                // already in it
                if ( !urlQueue.contains( primaryUrl ) ) {
                    GPUdbLogger.debug_with_info( "Primary URL not in user-given URLs; adding it");
                    urlQueue.add( primaryUrl );
                }

                // Save just the hostname of the primary cluster's URL for
                // future use
                this.primaryUrlHostname = primaryUrl.getHost();
            } catch (MalformedURLException ex) {
                // No-op if it's not a fully qualified URL (e.g. the user
                // may have only given a hostname)
            }
        }

        String userGivenUrlsStr = Arrays.toString( urlQueue.toArray() );
        GPUdbLogger.debug_with_info( "Consolidated list of " + urlQueue.size() + " URLs to process: " + userGivenUrlsStr );

        // We will store API-discovered URLs even if we cannot communicate with
        // any server at that address (it might be temporarily down)
        int numUserGivenURLs    = urlQueue.size();
        int numProcessedURLs    = 0;
        boolean isDiscoveredURL = false;

        // We need to keep track of whether all the user given URLs all belong
        // to the same cluster (for the purpose of primary choosing)
        List<Integer> clusterIndicesOfUserGivenURLs = new ArrayList<>();

        // Parse each user given URL (until the queue is empty)
        while ( !urlQueue.isEmpty() ) {
            URL url = urlQueue.remove();
            String urlStr = url.toString();
            GPUdbLogger.debug_with_info( "Processing URL: " + urlStr);
            GPUdbLogger.debug_with_info( "Remaining " + urlQueue.size() + " URL(s): " + Arrays.toString(urlQueue.toArray()) );

            // We need to know down the road if this URL was discovered by the
            // API or was given by the user.
            if (numProcessedURLs >= numUserGivenURLs) {
                GPUdbLogger.debug_with_info( "This URL is API-discovered");
                isDiscoveredURL = true;
            }
            ++numProcessedURLs;

            // Skip processing this URL if the hostname/IP address is used in
            // any of the known (already registered) clusters
            int indexOfHostnameInRing = getIndexOfClusterContainingNode( url.getHost() );
            if ( indexOfHostnameInRing != -1 ) {

                // Save the fact that this user given URL belongs to an existing
                // cluster
                if ( !isDiscoveredURL ) {
                    GPUdbLogger.debug_with_info(
                            "Skipping user-given URL " + urlStr + " (already found); adding index "
                            + indexOfHostnameInRing + " to user-given processed cluster list"
                    );
                    clusterIndicesOfUserGivenURLs.add( indexOfHostnameInRing );
                } else {
                    GPUdbLogger.debug_with_info("Skipping discovered URL " + urlStr + " (already found)" );
                } // end if

                continue;
            }

            // Skip auto-discovery of cluster information if the user says so
            if ( this.disableAutoDiscovery ) {

                if ( !isDiscoveredURL ) {
                    GPUdbLogger.debug_with_info(
                            "Skipping connect verification of user-given URL " + urlStr + " (auto-discovery disabled); adding index "
                            + this.hostAddresses.size() + " to user-given processed cluster list"
                    );

                    // Mark this user-given URL as a valid cluster
                    clusterIndicesOfUserGivenURLs.add( this.hostAddresses.size() );
                } else {
                    GPUdbLogger.debug_with_info("Skipping connect verification of API-discovered URL " + urlStr + " (auto-discovery disabled)" );
                } // end if

                // Create a cluster info object with just the given URL and the
                // host manager port in the option
                ClusterAddressInfo clusterInfo = new ClusterAddressInfo(url, this.hostManagerPort);
                this.hostAddresses.add( clusterInfo );
                GPUdbLogger.debug_with_info( "Added cluster: " + clusterInfo.toString() );
                continue; // skip to the next URL
            }

            // Skip processing this URL if Kinetica is not running at this address
            if ( !isSystemRunning( url ) ) {

                // If this URL has been discovered by the API, then add it to
                // the cluster list anyway
                if ( isDiscoveredURL ) {
                    // Create a cluster info object with just the given URL and the
                    // host manager port in the option
                    ClusterAddressInfo clusterInfo = new ClusterAddressInfo(url, this.hostManagerPort);
                    this.hostAddresses.add( clusterInfo );
                    GPUdbLogger.debug_with_info( "Added non-running cluster with API-discovered URL: " + clusterInfo.toString() );
                } else {
                    GPUdbLogger.debug_with_info( "Skipping non-running user-given URL: " + urlStr );
                }
                continue;
            }

            // Get system properties of the cluster, if can't get it, skip
            // to the next one
            Map<String, String> systemProperties;
            try {
                systemProperties = getSystemProperties( url );
            } catch ( GPUdbException ex ) {

                // If this URL has been discovered by the API, then add it to
                // the cluster list anyway
                if ( isDiscoveredURL ) {
                    // Create a cluster info object with just the given URL and the
                    // host manager port in the option
                    ClusterAddressInfo clusterInfo = new ClusterAddressInfo(url, this.hostManagerPort);
                    this.hostAddresses.add( clusterInfo );
                    GPUdbLogger.debug_with_info( "Added failed system properties lookup cluster with API-discovered URL: " + clusterInfo.toString() );
                } else {
                    GPUdbLogger.debug_with_info( "Skipping failed system properties lookup user-given URL: " + urlStr );
                }
                continue;
            }

            // Create an object to store all the information about this cluster
            // (this could fail due to a host name regex mismatch)
            ClusterAddressInfo clusterInfo = createClusterAddressInfo( url, systemProperties );

            // If this is a user-given URL, verify connectivity to the cluster
            // it connects to using that cluster's known rank URLs
            if ( !isDiscoveredURL ) {

                // Check if the user-given URL is in the server's list of rank URLs;
                // if not, the connection may need to be handled differently
                if ( !clusterInfo.doesClusterContainNode( url.getHost() ) ) {
                    GPUdbLogger.debug_with_info( "Obtained cluster addresses do not contain user given URL: " + urlStr );

                    // Check if the server given head node address is reachable.
                    // If so, use that URL instead of the user-given one.
                    // If not, the user will not be able to use the server-known
                    // address for connecting normally.  The API will need to
                    // reprocess the user-given URLs with auto-discovery
                    // disabled, so that the user can issue database commands,
                    // but where multi-head operations will not be available.
                    if ( !isKineticaRunning( clusterInfo.getActiveHeadNodeUrl() ) ) {

                        GPUdbLogger.warn(String.format(
                                "Disabling auto-discovery & multi-head operations--cluster reachable with user-given URL <%s> but not with server-known URL <%s>",
                                urlStr, clusterInfo.getActiveHeadNodeUrl()));

                        // Disable auto-discovery and throw exception to reprocess user-given URLs
                        this.disableAutoDiscovery = true;

                        throw new GPUdbException(String.format(
                                "Could not connect to server-known head node address: %s (user given URL: %s)",
                                clusterInfo.toString(), urlStr));
                    }
                }   // end if

                GPUdbLogger.debug_with_info(String.format(
                        "Verified connectivity with user-given URL %s; adding index %s to user-given processed cluster list",
                        urlStr, this.hostAddresses.size()));
                clusterIndicesOfUserGivenURLs.add( this.hostAddresses.size() );
            } // end if


            this.hostAddresses.add( clusterInfo );

            GPUdbLogger.debug_with_info(String.format("Added URL %s -> cluster %s", urlStr, clusterInfo.toString()));
            GPUdbLogger.debug_with_info(String.format("URLs queue after processing this URL (size %s): %s", urlQueue.size(), Arrays.toString(urlQueue.toArray())));

            // Parse the HA ring head nodes in the properties and add them
            // to this queue (only if we haven't processed them already).
            // This could fail due to a hostname regex mismatch.
            List<URL> haRingHeadNodeURLs = getHARingHeadNodeURLs( systemProperties );
            GPUdbLogger.debug_with_info( "Got HA ring head node URLs: " + Arrays.toString(haRingHeadNodeURLs.toArray()) );
            for (URL haUrl : haRingHeadNodeURLs) {
                if (getIndexOfClusterContainingNode(haUrl.getHost()) == -1) {
                    // We have not encountered this cluster yet; add it to the
                    // list of URLs to process
                    GPUdbLogger.debug_with_info( "HA ring head node URL " + haUrl.toString() + " not found in known clusters; adding to queue to process" );
                    urlQueue.add( haUrl );
                } else {
                    GPUdbLogger.debug_with_info( "HA ring head node URL " + haUrl.toString() + " found in known clusters; skipping" );
                }
            }
            GPUdbLogger.debug_with_info(
                    "URLs queue after processing this HA ring's head node URLs (size " + urlQueue.size() + "): "
                    + Arrays.toString(urlQueue.toArray())
            );
        }   // end while

        // Check that we have got at least one working URL
        if ( getHARingSize() == 0 ) {
            GPUdbLogger.error( "No clusters found at user given URLs " + urls.toString() + "!");
            throw new GPUdbException( "Could not connect to any working Kinetica server, given URLs: " + urls.toString() );
        }

        // Set the primary cluster & head node
        if ( getHARingSize() == 1 ) {

            // Mark the single cluster as the primary cluster
            this.hostAddresses.get( 0 ).setIsPrimaryCluster( true );

            // Update the primary cluster head node hostname, as the original
            // one may have been a worker node
            final String originalPrimaryUrlHostname = this.primaryUrlHostname;
            this.primaryUrlHostname = this.hostAddresses
                .get( 0 )
                .getActiveHeadNodeUrl()
                .getHost();

            // Also save it in the options for the future
            this.options.setPrimaryUrl( this.primaryUrlHostname );
            GPUdbLogger.debug_with_info(String.format(
                    "Updated primary host name %s -> %s for single-cluster connection",
                    originalPrimaryUrlHostname, this.primaryUrlHostname
            ));
        } else {
            // If the user has not given any primary host AND all the user
            // given URLs belong to a single cluster, set that as the primary
            if ( this.primaryUrlHostname.isEmpty() ) {

                boolean allUrlsInSameCluster = (new HashSet<>(clusterIndicesOfUserGivenURLs).size() == 1 );

                if ( allUrlsInSameCluster ) {
                    int primaryIndex = clusterIndicesOfUserGivenURLs.get( 0 );

                    // Save the hostname of the newly identified primary cluster
                    final String originalPrimaryUrlHostname = this.primaryUrlHostname;
                    this.primaryUrlHostname = this.hostAddresses
                        .get( primaryIndex )
                        .getActiveHeadNodeUrl()
                        .getHost();

                    // Also save it in the options
                    this.options.setPrimaryUrl( primaryUrlHostname );
                    GPUdbLogger.debug_with_info(String.format(
                            "Updated primary host name %s -> %s for multi-cluster connection",
                            originalPrimaryUrlHostname, this.primaryUrlHostname
                    ));
                } else {
                    GPUdbLogger.debug_with_info( "Could not update primary host name for multi-cluster connection, as user-given URLs belong to different clusters" );
                } // end innermost if
            }   // end if
        }   // end if

        // Flag the primary cluster as such and ensure it's the first element in
        // this.hostAddresses
        // ----------------------------------------------------------------------
        if (this.primaryUrlHostname != null && !this.primaryUrlHostname.isEmpty()) {

            // Check if the primary host exists in the list of user given hosts
            int primaryIndex = getIndexOfClusterContainingNode( this.primaryUrlHostname );
    
            GPUdbLogger.debug_with_info( "Checking if the primary cluster is in the ring; index: " + primaryIndex );
            if ( primaryIndex != -1 ) {
                GPUdbLogger.debug_with_info( "Setting that cluster as primary");
                // There is a match; mark the respective cluster as the primary cluster
                this.hostAddresses.get( primaryIndex ).setIsPrimaryCluster( true );
    
                if ( primaryIndex > 0 ) {
                    GPUdbLogger.debug_with_info( "Moving primary cluster to the front of the list" );
                    // Note: Do not combine the nested if with the top level if; will change
                    //       logic and may end up getting duplicates of the primary URL
    
                    // Move the primary URL to the front of the list
                    Collections.swap( this.hostAddresses, 0, primaryIndex );
                }
            }
            else {
                // Note that if no primary URL is specified by the user, then primaryIndex
                // above would be -1; but we need not handle that case since it would be
                // a no-op
                GPUdbLogger.debug_with_info(String.format(
                        "Designated primary cluster with host %s not found in cluster list.",
                        this.primaryUrlHostname
                ));
            }
        }

        // Randomize the URL indices taking care that the primary cluster is
        // always at the front
        randomizeURLs();
    }   // end parseUrlsOnce


    /**
     * Randomly shuffles the list of high availability URL indices so that HA
     * failover happens at a random fashion.  One caveat is when a primary host
     * is given by the user; in that case, we need to keep the primary host's
     * index as the first one in the list so that upon failover, when we circle
     * back, we always pick the first/primary host up again.
     */
    private void randomizeURLs() {
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
                    GPUdbLogger.debug_with_info( "Randomizing all clusters for HA failover--no primary host given");
                    // We don't have any primary URL; so treat all URLs similarly
                    // Randomly order the HA clusters and pick one to start working with
                    Collections.shuffle( this.haUrlIndices );
                } else {
                    GPUdbLogger.debug_with_info( "Randomizing all cluster for HA failover except for primary host " + this.primaryUrlHostname );
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


    /**
     * Given the system properties, extract and return the server version.  If
     * no server version was not able to parsed, throw an exception.
     *
     * @return {@link GPUdbBase.GPUdbVersion} object or null.
     */
    private GPUdbVersion parseServerVersion( Map<String, String> systemProperties )
        throws GPUdbException {
        // Get the server version in a string format
        String serverVersionStr = systemProperties.get( DATABASE_SERVER_VERSION_KEY );

        if ( serverVersionStr == null ) {
            throw new GPUdbException( "System properties does not have any entry for the '" + DATABASE_SERVER_VERSION_KEY + "' key" );
        }

        // Now parse the version string
        try {
            // Split on period (.); note that the expected format is
            // A.B.C.D.datewithtimestamp.  We will only extract A, B, C, and D.
            int[] components = new int[4];
            // Need to escape the period since we're passing a regex
            String[] componentStrings = serverVersionStr.split( "\\.", 5 );
            // Check that we get at least four components
            if ( componentStrings.length < 4 ) {
                throw new GPUdbException( "Server version string in /show/system/properties response malformed (expected at least four components): " + serverVersionStr );
            }
            // Parse each component
            for ( int i = 0; i < 4; ++i  ) {
                components[ i ] = Integer.parseInt( componentStrings[ i ] );
            }

            return new GPUdbVersion( components[0], components[1], components[2], components[3] );
        } catch (Exception ex) {
            throw new GPUdbException( "Could not parse server version; error: " + ex.getMessage(), ex );
        }
    }  // end parseServerVersion





    /**
     * If the database version hasn't been set already, update it by querying
     * the server about its system properties.  If any issue arises, throw
     * an exception.  Save the server version internally.
     */
    protected void updateServerVersion() throws GPUdbException {
        if ( this.serverVersion != null ) {
            // We've already gotten the server version; so nothing to do!
            return;
        }

        // Get the database version from the server
        try {
            ShowSystemPropertiesResponse response;
            response = submitRequest(
                    appendPathToURL( this.getURL(), "/show/system/properties" ),
                    new ShowSystemPropertiesRequest(),
                    new ShowSystemPropertiesResponse(),
                    false
            );
            this.serverVersion = parseServerVersion( response.getPropertyMap() );
        } catch ( MalformedURLException | GPUdbException ex ) {
            String msg = ( "Failed to get database version from the server; " + ex.getMessage() );
            throw new GPUdbException( msg, ex );
        }
    }  // end updateServerVersion



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
    @SuppressWarnings("rawtypes")
    public void addKnownType(String typeId, Object typeDescriptor) {
        if (typeDescriptor == null)
            knownTypes.remove(typeId);
        else
            if (
                typeDescriptor instanceof Schema ||
                typeDescriptor instanceof Type ||
                typeDescriptor instanceof TypeObjectMap ||
                typeDescriptor instanceof Class && IndexedRecord.class.isAssignableFrom((Class)typeDescriptor)
            )
                knownTypes.put(typeId, typeDescriptor);
            else
                throw new IllegalArgumentException("Type descriptor must be a Schema, Type, TypeObjectMap, or Class implementing IndexedRecord.");
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
    @SuppressWarnings("rawtypes")
    public void addKnownTypeFromTable(String tableName, Object typeDescriptor) throws GPUdbException {
        if (
                typeDescriptor == null ||
                typeDescriptor instanceof Schema ||
                typeDescriptor instanceof Type ||
                typeDescriptor instanceof TypeObjectMap ||
                typeDescriptor instanceof Class && IndexedRecord.class.isAssignableFrom((Class)typeDescriptor)
        ) {
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

        for (List<ByteBuffer> datum : data) {
            result.add(Avro.<T>decode(typeDescriptor, datum, threadCount, executor));
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
            return Avro.encode((List<? extends IndexedRecord>)data, threadCount, executor).getLeft();
        } else {
            return Avro.encode((TypeObjectMap<T>)getTypeObjectMap(object.getClass()), data, threadCount, executor).getLeft();
        }
    }

    protected <T> List<ByteBuffer> encode(TypeObjectMap<T> typeObjectMap, List<T> data) throws GPUdbException {
        return Avro.encode(typeObjectMap, data == null ? new ArrayList<>() : data, threadCount, executor).getLeft();
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
     * NOTE:  This method's primary use is in support of requests other than
     * {@link #getRecordsJson} and its other variants and
     * {@link #insertRecordsFromJson} and its other variants.
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
     * NOTE:  This method's primary use is in support of requests other than
     * {@link #getRecordsJson} and its other variants and
     * {@link #insertRecordsFromJson} and its other variants.
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
                GPUdbLogger.warn(
                        "Got EXIT exception when trying endpoint " + endpoint
                        + " at " + url.toString()
                        + ": " + ex.getMessage() + "; switch URL..."
                );
                // Handle our special exit exception
                try {
                    url = switchURL( originalURL, currentClusterSwitchCount );
                    GPUdbLogger.debug_with_info( "Switched to " + url.toString() );
                } catch (GPUdbHAUnavailableException ha_ex) {
                    // We've now tried all the HA clusters and circled back
                    // Get the original cause to propagate to the user
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause + "; " + ha_ex.getMessage(), true );
                } catch (GPUdbFailoverDisabledException ha_ex) {
                    // Failover is disabled; return the original cause
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause + "; " + ha_ex.getMessage(), true );
                }
            } catch (SubmitException ex) {
                GPUdbLogger.warn(
                        "Got submit exception when trying endpoint " + endpoint
                        + " at " + url.toString()
                        + ": " + ex.getMessage() + "; switch URL..."
                );
                // Some error occurred during the HTTP request
                try {
                    url = switchURL( originalURL, currentClusterSwitchCount );
                    GPUdbLogger.debug_with_info( "Switched to " + url.toString() );
                } catch (GPUdbHAUnavailableException ha_ex) {
                    // We've now tried all the HA clusters and circled back
                    // Get the original cause to propagate to the user
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new SubmitException(
                            null,
                            ex.getRequest(),
                            ex.getRequestSize(),
                            originalCause + "; " + ha_ex.getMessage(),
                            ex.getCause(),
                            true
                    );
                } catch (GPUdbFailoverDisabledException ha_ex) {
                    // Failover is disabled; return the original cause
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new SubmitException(
                            null,
                            ex.getRequest(),
                            ex.getRequestSize(),
                            originalCause + "; " + ha_ex.getMessage(),
                            ex.getCause(),
                            true
                    );
                }
            } catch (GPUdbException ex) {
                // Any other GPUdbException is a valid failure
                GPUdbLogger.debug_with_info( "Got GPUdbException, so propagating: " + ex.getMessage() );
                throw ex;
            } catch (Exception ex) {
                GPUdbLogger.warn(
                        "Got Java exception when trying endpoint " + endpoint
                        + " at " + url.toString()
                        + ": " + ex.getMessage() + "; switch URL..."
                );
                // And other random exceptions probably are also connection errors
                try {
                    url = switchURL( originalURL, currentClusterSwitchCount );
                    GPUdbLogger.debug_with_info( "Switched to " + url.toString() );
                } catch (GPUdbHAUnavailableException ha_ex) {
                    // We've now tried all the HA clusters and circled back
                    // Get the original cause to propagate to the user
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause + "; " + ha_ex.getMessage(), true );
                } catch (GPUdbFailoverDisabledException ha_ex) {
                    // Failover is disabled; return the original cause
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause + "; " + ha_ex.getMessage(), true );
                }
            }
        } // end while
    } // end submitRequest

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
     * NOTE:  This method's primary use is in support of
     * {@link #insertRecordsFromJson} and its other variants.
     *
     * @param endpoint           the GPUdb endpoint to send the request to
     * @param payload            the payload as a JSON String
     * @param enableCompression  whether to compress the request
     *
     * @return                   the response object (same as {@code response}
     *                           parameter)
     *
     * @throws SubmitException if an error occurs while submitting the request
     */
    public Map<String, Object> submitRequest( String endpoint,
                                                      String payload,
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
                return submitRequest(appendPathToURL(url, endpoint), payload, enableCompression);
            } catch (MalformedURLException ex) {
                // There's an error in creating the URL
                throw new GPUdbRuntimeException(ex.getMessage(), ex);
            } catch (GPUdbExitException ex) {
                GPUdbLogger.warn(
                        "Got EXIT exception when trying endpoint " + endpoint
                                + " at " + url.toString()
                                + ": " + ex.getMessage() + "; switch URL..."
                );
                // Handle our special exit exception
                try {
                    url = switchURL( originalURL, currentClusterSwitchCount );
                    GPUdbLogger.debug_with_info( "Switched to " + url.toString() );
                } catch (GPUdbHAUnavailableException ha_ex) {
                    // We've now tried all the HA clusters and circled back
                    // Get the original cause to propagate to the user
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause + "; " + ha_ex.getMessage(), true );
                } catch (GPUdbFailoverDisabledException ha_ex) {
                    // Failover is disabled; return the original cause
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause + "; " + ha_ex.getMessage(), true );
                }
            } catch (SubmitException ex) {
                GPUdbLogger.warn(
                        "Got submit exception when trying endpoint " + endpoint
                                + " at " + url.toString()
                                + ": " + ex.getMessage() + "; switch URL..."
                );
                // Some error occurred during the HTTP request
                try {
                    url = switchURL( originalURL, currentClusterSwitchCount );
                    GPUdbLogger.debug_with_info( "Switched to " + url.toString() );
                } catch (GPUdbHAUnavailableException ha_ex) {
                    // We've now tried all the HA clusters and circled back
                    // Get the original cause to propagate to the user
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new SubmitException(
                            null,
                            ex.getRequest(),
                            ex.getRequestSize(),
                            originalCause + "; " + ha_ex.getMessage(),
                            ex.getCause(),
                            true
                    );
                } catch (GPUdbFailoverDisabledException ha_ex) {
                    // Failover is disabled; return the original cause
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new SubmitException(
                            null,
                            ex.getRequest(),
                            ex.getRequestSize(),
                            originalCause + "; " + ha_ex.getMessage(),
                            ex.getCause(),
                            true
                    );
                }
            } catch (GPUdbException ex) {
                // Any other GPUdbException is a valid failure
                GPUdbLogger.debug_with_info( "Got GPUdbException, so propagating: " + ex.getMessage() );
                throw ex;
            } catch (Exception ex) {
                GPUdbLogger.warn(
                        "Got Java exception when trying endpoint " + endpoint
                                + " at " + url.toString()
                                + ": " + ex.getMessage() + "; switch URL..."
                );
                // And other random exceptions probably are also connection errors
                try {
                    url = switchURL( originalURL, currentClusterSwitchCount );
                    GPUdbLogger.debug_with_info( "Switched to " + url.toString() );
                } catch (GPUdbHAUnavailableException ha_ex) {
                    // We've now tried all the HA clusters and circled back
                    // Get the original cause to propagate to the user
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause + "; " + ha_ex.getMessage(), true );
                } catch (GPUdbFailoverDisabledException ha_ex) {
                    // Failover is disabled; return the original cause
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause + "; " + ha_ex.getMessage(), true );
                }
            }
        } // end while
    } // end submitRequest


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
     * NOTE:  This method's primary use is in support of
     * {@link #getRecordsJson} and its other variants.
     *
     * @param endpoint           the GPUdb endpoint to send the request to
     * @param enableCompression  whether to compress the request
     *
     * @return                   the response object (same as {@code response}
     *                           parameter)
     *
     * @throws SubmitException if an error occurs while submitting the request
     */
    public String submitRequest( String endpoint,
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
                return submitRequest(appendPathToURL(url, endpoint), enableCompression);
            } catch (MalformedURLException ex) {
                // There's an error in creating the URL
                throw new GPUdbRuntimeException(ex.getMessage(), ex);
            } catch (GPUdbExitException ex) {
                GPUdbLogger.warn(
                        "Got EXIT exception when trying endpoint " + endpoint
                                + " at " + url.toString()
                                + ": " + ex.getMessage() + "; switch URL..."
                );
                // Handle our special exit exception
                try {
                    url = switchURL( originalURL, currentClusterSwitchCount );
                    GPUdbLogger.debug_with_info( "Switched to " + url.toString() );
                } catch (GPUdbHAUnavailableException ha_ex) {
                    // We've now tried all the HA clusters and circled back
                    // Get the original cause to propagate to the user
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause + "; " + ha_ex.getMessage(), true );
                } catch (GPUdbFailoverDisabledException ha_ex) {
                    // Failover is disabled; return the original cause
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause + "; " + ha_ex.getMessage(), true );
                }
            } catch (SubmitException ex) {
                GPUdbLogger.warn(
                        "Got submit exception when trying endpoint " + endpoint
                                + " at " + url.toString()
                                + ": " + ex.getMessage() + "; switch URL..."
                );
                // Some error occurred during the HTTP request
                try {
                    url = switchURL( originalURL, currentClusterSwitchCount );
                    GPUdbLogger.debug_with_info( "Switched to " + url.toString() );
                } catch (GPUdbHAUnavailableException ha_ex) {
                    // We've now tried all the HA clusters and circled back
                    // Get the original cause to propagate to the user
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new SubmitException(
                            null,
                            ex.getRequest(),
                            ex.getRequestSize(),
                            originalCause + "; " + ha_ex.getMessage(),
                            ex.getCause(),
                            true
                    );
                } catch (GPUdbFailoverDisabledException ha_ex) {
                    // Failover is disabled; return the original cause
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new SubmitException(
                            null,
                            ex.getRequest(),
                            ex.getRequestSize(),
                            originalCause + "; " + ha_ex.getMessage(),
                            ex.getCause(),
                            true
                    );
                }
            } catch (GPUdbException ex) {
                // Any other GPUdbException is a valid failure
                GPUdbLogger.debug_with_info( "Got GPUdbException, so propagating: " + ex.getMessage() );
                throw ex;
            } catch (Exception ex) {
                GPUdbLogger.warn(
                        "Got Java exception when trying endpoint " + endpoint
                                + " at " + url.toString()
                                + ": " + ex.getMessage() + "; switch URL..."
                );
                // And other random exceptions probably are also connection errors
                try {
                    url = switchURL( originalURL, currentClusterSwitchCount );
                    GPUdbLogger.debug_with_info( "Switched to " + url.toString() );
                } catch (GPUdbHAUnavailableException ha_ex) {
                    // We've now tried all the HA clusters and circled back
                    // Get the original cause to propagate to the user
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause + "; " + ha_ex.getMessage(), true );
                } catch (GPUdbFailoverDisabledException ha_ex) {
                    // Failover is disabled; return the original cause
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause + "; " + ha_ex.getMessage(), true );
                }
            }
        } // end while
    } // end submitRequest

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
     * NOTE:  This method's primary use is in support of
     * {@link #getRecordsJson} and its other variants.
     *
     * @param url                the URL to send the request to
     * @param enableCompression  whether to compress the request
     * @return                   the response object (same as {@code response}
     *                           parameter)
     *
     * @throws SubmitException if an error occurs while submitting the request
     */
    public String submitRequest( URL url,
                                                      boolean enableCompression)
            throws SubmitException, GPUdbExitException, GPUdbException {

        return submitRequestRaw( url, enableCompression );
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
     * NOTE:  This method's primary use is in support of
     * {@link #insertRecordsFromJson} and its other variants.
     *
     * @param url                the URL to send the request to
     * @param payload            the payload as a JSON String
     * @param enableCompression  whether to compress the request
     * @return                   the response object (same as {@code response}
     *                           parameter)
     *
     * @throws SubmitException if an error occurs while submitting the request
     */
    public Map<String, Object> submitRequest( URL url,
                                                      String payload,
                                                      boolean enableCompression)
            throws SubmitException, GPUdbExitException, GPUdbException {

        return submitRequestRaw( url, payload, enableCompression );
    }
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
                    throw new GPUdbException( originalCause + "; " + ha_ex.getMessage(), true );
                } catch (GPUdbFailoverDisabledException ha_ex) {
                    // Failover is disabled; return the original cause
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause + "; " + ha_ex.getMessage(), true );
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
                    throw new SubmitException(
                            null,
                            ex.getRequest(),
                            ex.getRequestSize(),
                            originalCause + "; " + ha_ex.getMessage(),
                            ex.getCause(),
                            true
                    );
                } catch (GPUdbFailoverDisabledException ha_ex) {
                    // Failover is disabled; return the original cause
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new SubmitException(
                            null,
                            ex.getRequest(),
                            ex.getRequestSize(),
                            originalCause + "; " + ha_ex.getMessage(),
                            ex.getCause(),
                            true
                    );
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
                        throw new GPUdbException( originalCause + "; " + ha_ex.getMessage(), true );
                    } catch (GPUdbFailoverDisabledException ha_ex) {
                        // Failover is disabled; return the original cause
                        String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                        throw new GPUdbException( originalCause + "; " + ha_ex.getMessage(), true );
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
                    throw new GPUdbException( originalCause + "; " + ha_ex.getMessage(), true );
                } catch (GPUdbFailoverDisabledException ha_ex) {
                    // Failover is disabled; return the original cause
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause + "; " + ha_ex.getMessage(), true );
                }
            }
        } // end for

        // If we reach here, then something went wrong
        GPUdbLogger.debug_with_info(
                "Failed to submit host manager endpoint; "
                + "exceeded retry count " + HOST_MANAGER_SUBMIT_REQUEST_RETRY_COUNT
                + "; original exception: '" + original_exception.getMessage()
                + "'; please check if the host manager port is wrong: " + hmUrl.toString()
        );
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
     * NOTE:  This method's primary use is in support of requests other than
     * {@link #getRecordsJson} and its other variants and
     * {@link #insertRecordsFromJson} and its other variants.
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
     * NOTE:  This method's primary use is in support of requests other than
     * {@link #getRecordsJson} and its other variants and
     * {@link #insertRecordsFromJson} and its other variants.
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

        return submitRequestRaw( url, request, response, enableCompression, timeout );
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
     * NOTE:  This method's primary use is in support of requests other than
     * {@link #getRecordsJson} and its other variants and
     * {@link #insertRecordsFromJson} and its other variants.
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
        throws SubmitException, GPUdbExitException, GPUdbException {
        // Use the set timeout for the GPUdb object for the http connection
        return submitRequestRaw( url, request, response, enableCompression, this.timeout );
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
     * NOTE:  This method's primary use is in support of
     * {@link #insertRecordsFromJson} and its other variants.
     *
     * @param url                the URL to send the request to
     * @param payload            the payload as a JSON String
     * @param enableCompression  whether to compress the request
     * @return                   the response object (same as {@code response}
     *                           parameter)
     *
     * @throws SubmitException if an error occurs while submitting the request
     */
    public Map<String, Object> submitRequestRaw( URL url,
                                                         String payload,
                                                         boolean enableCompression )
            throws SubmitException, GPUdbExitException, GPUdbException {
        // Use the set timeout for the GPUdb object for the http connection
        return submitRequestRaw( url, payload, enableCompression, this.timeout );
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
     * NOTE:  This method's primary use is in support of
     * {@link #getRecordsJson} and its other variants.
     *
     * @param url                the URL to send the request to
     * @param enableCompression  whether to compress the request
     * @return                   the response object (same as {@code response}
     *                           parameter)
     *
     * @throws SubmitException if an error occurs while submitting the request
     */
    public String submitRequestRaw( URL url,
                                                         boolean enableCompression )
            throws SubmitException, GPUdbExitException, GPUdbException {
        // Use the set timeout for the GPUdb object for the http connection
        return submitRequestRaw( url, enableCompression, this.timeout );
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
     * NOTE:  This method's primary use is in support of requests other than
     * {@link #getRecordsJson} and its other variants and
     * {@link #insertRecordsFromJson} and its other variants.
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
        throws SubmitException, GPUdbExitException, GPUdbException {

        int requestSize = -1;
        HttpPost              postRequest    = null;
        HttpEntity            responseEntity = null;
        HttpHost              host = null;
        ClassicHttpResponse postResponse   = null;

        try {
            // Log at the trace level
            GPUdbLogger.trace_with_info( "Sending request to " + url.toString() );

            // Use the given timeout, instead of the member variable one
            postRequest = initializeHttpPostRequest( url, timeout );
            host = new HttpHost( url.getProtocol(), url.getHost(), url.getPort() );

            HttpEntity requestPacket;
            if (enableCompression && useSnappy) {
                // Use snappy to compress the original request body
                byte[] encodedRequest = Snappy.compress(Avro.encode(request).array());
                requestSize = encodedRequest.length;
                postRequest.addHeader( HEADER_CONTENT_TYPE, "application/x-snappy" );

                // Create the entity for the compressed request
                requestPacket = new ByteArrayEntity( encodedRequest, ContentType.create("application/x-snappy")  );
            } else {
                byte[] encodedRequest = Avro.encode(request).array();
                requestSize = encodedRequest.length;
                postRequest.addHeader( HEADER_CONTENT_TYPE, "application/octet-stream" );

                // Create the entity for the request
                requestPacket = new ByteArrayEntity( encodedRequest, ContentType.APPLICATION_OCTET_STREAM  );
            }

            // Save the request into the http post object as a payload
            postRequest.setEntity( requestPacket );

            // Execute the request
            try {
                postResponse = this.httpClient.executeOpen( host, postRequest, null );
            } catch ( javax.net.ssl.SSLException ex) {
                String errorMsg = null;
                if( ex.getMessage().toLowerCase().contains("subject alternative names")) {
                    errorMsg = String.format(SslErrorMessageFormat, "Server SSL certificate not found in specified trust store.");
                } else if( ex.getMessage().toLowerCase().contains("unable to find valid certification path")) {
                    errorMsg = String.format(SslErrorMessageFormat, "No trust store was specified, but server requires SSL certificate validation.");
                } else {
                    errorMsg = String.format(SslErrorMessageFormat, "Error: " + ex.getMessage());
                }
                GPUdbLogger.error( errorMsg );
                throw new GPUdbException( errorMsg );
            } catch (Exception ex) {
                // Trigger an HA failover at the caller level
                GPUdbLogger.error( ex, "Throwing exit exception due to ");
                throw new GPUdbExitException("Error submitting endpoint request: " + ExceptionUtils.getRootCauseMessage(ex));
            }

            // Get the status code and the messages of the response
            StatusLine statusLine = new StatusLine(postResponse);
            int statusCode = statusLine.getStatusCode();
            String responseMessage = statusLine.getReasonPhrase();

            // Get the entity and the content of the response
            responseEntity = postResponse.getEntity();

            if (GPUdbLogger.isTraceEnabled())
                GPUdbLogger.trace_with_info(String.format(
                        "<Status|Message|Type>: <%d|%s|%s>",
                        statusCode, responseMessage, responseEntity.getContentType()
                ));

            // Ensure that we're not getting any HTML snippet (may be
            // returned by the HTTPD server)
            if (
                    (responseEntity.getContentType() != null) &&
                    (responseEntity.getContentType().length() > 0) &&
                    responseEntity.getContentType().startsWith( "text" )
            ) {
                String errorMsg;
                // Handle unauthorized connection specially--better error messaging
                if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    GPUdbLogger.debug_with_info( "Got status code: " + statusCode );
                    errorMsg = ("Unauthorized access: " + responseMessage );
                    throw new GPUdbUnauthorizedAccessException(errorMsg);
                } else if ( (statusCode == HttpURLConnection.HTTP_UNAVAILABLE)
                    || (statusCode == HttpURLConnection.HTTP_INTERNAL_ERROR)
                    || (statusCode == HttpURLConnection.HTTP_GATEWAY_TIMEOUT)
                ) {
                    // Some status codes should trigger an exit exception which will
                    // in turn trigger a failover on the client side
                    GPUdbLogger.debug_with_info(
                            "Throwing EXIT exception from " + url.toString()
                            + "; response_code: " + statusCode
                            + "; content type " + responseEntity.getContentType()
                            + "; response message: " + responseMessage
                    );
                    throw new GPUdbExitException(responseMessage);
                } else {
                    // All other issues are simply propagated to the user
                    errorMsg = ("Cannot parse response from server: '" + responseMessage + "'; status code: " + statusCode );
                }
                throw new SubmitException( url, request, requestSize, errorMsg );
            }

            // Parse response based on error code
            if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                // Got status 401 -- unauthorized
                GPUdbLogger.debug_with_info( "Got status code: " + statusCode );
                String errorMsg = (String.format("Server response status: %d : %s", statusCode, responseMessage));
                throw new GPUdbUnauthorizedAccessException(errorMsg);
                // Note: Keeping the original code here in case some unforeseen
                // problem arises that we haven't thought of yet by changing
                // which exception is thrown.
                // throw new SubmitException( url, request, requestSize,
                //                            responseMessage );
            }

            InputStream inputStream = responseEntity.getContent();
            if (inputStream == null) {
                // Trigger an HA failover at the caller level
                throw new GPUdbExitException("Server returned HTTP " + statusCode + " (" + responseMessage + "); returning EXIT exception");
            }

            try {
                // Manually decode the RawGpudbResponse wrapper directly from
                // the stream to avoid allocation of intermediate buffers

                BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(inputStream, null);
                String status = decoder.readString();
                String message = decoder.readString();

                if (status.equals("ERROR")) {
                    // Check if Kinetica is shutting down
                    if (
                            statusCode == HttpURLConnection.HTTP_INTERNAL_ERROR ||
                            statusCode == HttpURLConnection.HTTP_GATEWAY_TIMEOUT ||
                            message.contains( DB_EXITING_ERROR_MESSAGE ) ||
                            message.contains( DB_CONNECTION_REFUSED_ERROR_MESSAGE ) ||
                            message.contains( DB_CONNECTION_RESET_ERROR_MESSAGE ) ||
                            message.contains( DB_SYSTEM_LIMITED_ERROR_MESSAGE ) ||
                            message.contains( DB_OFFLINE_ERROR_MESSAGE )
                    ) {
                        GPUdbLogger.debug_with_info(
                                "Throwing EXIT exception from " + url.toString() + "; response_code: " + statusCode + "; message: " + message
                        );
                        throw new GPUdbExitException(message);
                    } else if( statusCode == HttpURLConnection.HTTP_UNAVAILABLE ) {
                        GPUdbLogger.debug_with_info(
                            "Throwing EXIT exception from " + url.toString() + "; response_code: " + statusCode + "; message: " + message
                        );
                        throw new GPUdbExitException("cluster may be stopped or suspended");
                    }
                    // A legitimate error
                    GPUdbLogger.debug_with_info(
                            "Throwing GPUdb exception from " + url.toString() + "; response_code: " + statusCode + "; message: " + message
                    );
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
                throw new GPUdbExitException(ex.getMessage());
            }
            // Some legitimate error
            throw ex;
        } catch (java.net.SocketException ex) {
            // Any network issue should trigger an HA failover
            throw new GPUdbExitException(ex.getMessage());
        } catch (Exception ex) {
            GPUdbLogger.debug_with_info( "Caught Exception: " + ex.getMessage() );
            // Some sort of submission error
            throw new SubmitException(url, request, requestSize, ex.getMessage(), ex);
        } catch (UnsatisfiedLinkError ex) {
            GPUdbLogger.debug_with_info( "Caught Exception: " + ex.getMessage() );
            // Snappy /tmp directory issue
            throw new SubmitException(url, request, requestSize, "Snappy unable to write to directory; possible permissions issue on /tmp", ex);
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
                    GPUdbLogger.error( ex.getMessage() );
                }
            }
        }
    }   // end submitRequestRaw

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
     * NOTE:  This method's primary use is in support of
     * {@link #insertRecordsFromJson} and its other variants.
     *
     * @param url                the URL to send the request to
     * @param payload            the payload as a JSON String
     * @param enableCompression  whether to compress the request
     * @param timeout            a positive integer representing the number of
     *                           milliseconds to use for connection timeout
     * @return                   the response object (same as {@code response}
     *                           parameter)
     *
     * @throws SubmitException if an error occurs while submitting the request
     */
    public Map<String, Object> submitRequestRaw( URL url,
                                                String payload,
                                                boolean enableCompression,
                                                int timeout)
            throws SubmitException, GPUdbExitException, GPUdbException {

        HttpPost              postRequest    = null;
        HttpEntity            responseEntity = null;
        HttpHost              host = null;
        ClassicHttpResponse postResponse   = null;

        try {
            // Log at the trace level
            GPUdbLogger.trace_with_info( "Sending request to " + url.toString() );

            // Use the given timeout, instead of the member variable one
            postRequest = initializeHttpPostRequest( url, timeout );
            host = new HttpHost( url.getProtocol(), url.getHost(), url.getPort() );

            // Save the payload into the http post object as a JSON payload
            postRequest.setEntity( new StringEntity( payload, ContentType.APPLICATION_JSON ));

            // Execute the request
            try {
                postResponse = this.httpClient.executeOpen( host, postRequest, null );
            } catch ( javax.net.ssl.SSLException ex) {
                String errorMsg;
                if( ex.getMessage().toLowerCase().contains("subject alternative names")) {
                    errorMsg = "Server SSL certificate not found in specified trust store.  Please use the options to " +
                            "bypass the certificate check or add the server's certificate or CA cert in the server's " +
                            "certificate path to the trust store and try again.";
                } else if( ex.getMessage().toLowerCase().contains("unable to find valid certification path")) {
                    errorMsg = "No trust store was specified, but server requires SSL certificate validation.  " +
                            "Please use the options to bypass the certificate check or specify a trust store containing " +
                            "the server's certificate or CA cert in the server's certificate path and try again.";
                } else {
                    errorMsg = "Error: " + ex.getMessage();
                }
                errorMsg = String.format("Encountered SSL error when trying to connect to server at %s.  %s", url.toString(), errorMsg);
                GPUdbLogger.error( errorMsg );
                throw new GPUdbException( errorMsg );
            } catch (Exception ex) {
                // Trigger an HA failover at the caller level
                GPUdbLogger.error( ex, "Throwing exit exception due to ");
                throw new GPUdbExitException("Error submitting endpoint request: " + ExceptionUtils.getRootCauseMessage(ex));
            }

            // Get the status code and the messages of the response
            StatusLine statusLine = new StatusLine(postResponse);
            int statusCode = statusLine.getStatusCode();
            String responseMessage = statusLine.getReasonPhrase();

            // Get the entity and the content of the response
            responseEntity = postResponse.getEntity();

            // use org.apache.http.util.EntityUtils to read JSON as string
            String json = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);

            TypeReference<HashMap<String,Object>> typeRef
                    = new TypeReference<HashMap<String,Object>>() {};
            Map<String, Object> response = JSON_MAPPER.readValue(json.getBytes(StandardCharsets.UTF_8), typeRef);

            if (GPUdbLogger.isTraceEnabled())
                GPUdbLogger.trace_with_info(String.format(
                        "<Status|Message|Type|Response>: <%d|%s|%s|%s>",
                        statusCode, responseMessage, responseEntity.getContentType(), response
                ));

            // Ensure that we're not getting any HTML snippet (may be
            // returned by the HTTPD server)
            if (
                    responseEntity.getContentType() != null &&
                    responseEntity.getContentType().length() > 0 &&
                    responseEntity.getContentType().startsWith( "text" )
            ) {
                String errorMsg = ("Cannot parse response from server: '" + responseMessage + "'; status code: " + statusCode );
                throw new SubmitException( url, payload, errorMsg );
            }

            // Parse response based on error code
            if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                GPUdbLogger.debug_with_info( "Got status code: " + statusCode );
                String errorMsg = (String.format("Server response status: %d : %s", statusCode, responseMessage));
                throw new GPUdbUnauthorizedAccessException(errorMsg);
            }
            
            if (
                    statusCode == HttpURLConnection.HTTP_UNAVAILABLE ||
                    statusCode == HttpURLConnection.HTTP_INTERNAL_ERROR ||
                    statusCode == HttpURLConnection.HTTP_GATEWAY_TIMEOUT ||
                    responseMessage.contains( DB_EXITING_ERROR_MESSAGE ) ||
                    responseMessage.contains( DB_CONNECTION_REFUSED_ERROR_MESSAGE ) ||
                    responseMessage.contains( DB_CONNECTION_RESET_ERROR_MESSAGE ) ||
                    responseMessage.contains( DB_SYSTEM_LIMITED_ERROR_MESSAGE ) ||
                    responseMessage.contains( DB_OFFLINE_ERROR_MESSAGE )
            ) {
                // Some status codes should trigger an exit exception which will
                // in turn trigger a failover on the client side
                GPUdbLogger.debug_with_info(
                        "Throwing EXIT exception from " + url.toString()
                                + "; response_code: " + statusCode
                                + "; content type " + responseEntity.getContentType()
                                + "; response message: " + responseMessage
                );
                throw new GPUdbExitException(responseMessage);
            }
            
            if (statusCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                String message = response.get("message").toString();
                if (message != null && message.contains( DB_SHUTTING_DOWN_ERROR_MESSAGE ))
                    throw new GPUdbExitException("Kinetica shutting down");
            }

            return response;

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
                throw new GPUdbExitException(ex.getMessage());
            }
            // Some legitimate error
            throw ex;
        } catch (java.net.SocketException ex) {
            // Any network issue should trigger an HA failover
            throw new GPUdbExitException(ex.getMessage());
        } catch (Exception ex) {
            GPUdbLogger.debug_with_info( "Caught Exception: " + ex.getMessage() );
            // Some sort of submission error
            throw new SubmitException(url, payload, ex.getMessage(), ex);
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
                    GPUdbLogger.error( ex.getMessage() );
                }
            }
        }
    }   // end submitRequestRaw


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
     * NOTE:  This method's primary use is in support of
     * {@link #getRecordsJson} and its other variants.
     *
     * @param url                the URL to send the request to
     * @param enableCompression  whether to compress the request
     * @param timeout            a positive integer representing the number of
     *                           milliseconds to use for connection timeout
     * @return                   the response object (same as {@code response}
     *                           parameter)
     *
     * @throws SubmitException if an error occurs while submitting the request
     */
    public String submitRequestRaw( URL url,
                                    boolean enableCompression,
                                    int timeout)
            throws SubmitException, GPUdbExitException, GPUdbException {

        HttpPost              postRequest    = null;
        HttpEntity            responseEntity = null;
        HttpHost              host = null;
        ClassicHttpResponse postResponse   = null;

        try {
            GPUdbLogger.trace_with_info( "Sending request to " + url.toString() );

            // Use the given timeout, instead of the member variable one
            postRequest = initializeHttpPostRequest( url, timeout );
            host = new HttpHost( url.getProtocol(), url.getHost(), url.getPort() );

            // Execute the request
            try {
                postResponse = this.httpClient.executeOpen( host, postRequest, null );
            } catch ( javax.net.ssl.SSLException ex) {
                String errorMsg;
                if( ex.getMessage().toLowerCase().contains("subject alternative names")) {
                    errorMsg = "Server SSL certificate not found in specified trust store.  Please use the options to " +
                            "bypass the certificate check or add the server's certificate or CA cert in the server's " +
                            "certificate path to the trust store and try again.";
                } else if( ex.getMessage().toLowerCase().contains("unable to find valid certification path")) {
                    errorMsg = "No trust store was specified, but server requires SSL certificate validation.  " +
                            "Please use the options to bypass the certificate check or specify a trust store containing " +
                            "the server's certificate or CA cert in the server's certificate path and try again.";
                } else {
                    errorMsg = "Error: " + ex.getMessage();
                }
                errorMsg = String.format("Encountered SSL error when trying to connect to server at %s.  %s", url.toString(), errorMsg);
                GPUdbLogger.error( errorMsg );
                throw new GPUdbException( errorMsg );
            } catch (Exception ex) {
                // Trigger an HA failover at the caller level
                GPUdbLogger.error( ex, "Throwing exit exception due to ");
                throw new GPUdbExitException("Error submitting endpoint request: " + ExceptionUtils.getRootCauseMessage(ex));
            }

            // Get the status code and the messages of the response
            StatusLine statusLine = new StatusLine(postResponse);
            int statusCode = statusLine.getStatusCode();
            String responseMessage = statusLine.getReasonPhrase();

            // Get the entity and the content of the response
            responseEntity = postResponse.getEntity();

            // use org.apache.http.util.EntityUtils to read JSON as string
            String json = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);

            if (GPUdbLogger.isTraceEnabled())
                GPUdbLogger.trace_with_info(String.format(
                        "<Status|Message|Entity>: <%d|%s|%s>",
                        statusCode, responseMessage, responseEntity
                ));

            // Ensure that we're not getting any HTML snippet (may be
            // returned by the HTTPD server)
            if (
                    responseEntity.getContentType() != null &&
                    responseEntity.getContentType().length() > 0 &&
                    responseEntity.getContentType().startsWith( "text" )
            ) {
                String errorMsg = ("Cannot parse response from server: '" + responseMessage + "'; status code: " + statusCode );
                throw new SubmitException( url, errorMsg );
            }

            // Parse response based on error code
            if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                GPUdbLogger.debug_with_info( "Got status code: " + statusCode );
                String errorMsg = (String.format("Server response status: %d : %s", statusCode, responseMessage));
                throw new GPUdbUnauthorizedAccessException(errorMsg);
            }
            
            if (
                    statusCode == HttpURLConnection.HTTP_UNAVAILABLE ||
                    statusCode == HttpURLConnection.HTTP_INTERNAL_ERROR ||
                    statusCode == HttpURLConnection.HTTP_GATEWAY_TIMEOUT ||
                    responseMessage.contains( DB_EXITING_ERROR_MESSAGE ) ||
                    responseMessage.contains( DB_CONNECTION_REFUSED_ERROR_MESSAGE ) ||
                    responseMessage.contains( DB_CONNECTION_RESET_ERROR_MESSAGE ) ||
                    responseMessage.contains( DB_SYSTEM_LIMITED_ERROR_MESSAGE ) ||
                    responseMessage.contains( DB_OFFLINE_ERROR_MESSAGE )
            ) {
                // Some status codes should trigger an exit exception which will
                // in turn trigger a failover on the client side
                GPUdbLogger.debug_with_info(
                        "Throwing EXIT exception from " + url.toString()
                                + "; response_code: " + statusCode
                                + "; content type " + responseEntity.getContentType()
                                + "; response message: " + responseMessage
                );
                throw new GPUdbExitException(responseMessage);
            }

            return json;
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
                throw new GPUdbExitException(ex.getMessage());
            }
            // Some legitimate error
            throw ex;
        } catch (java.net.SocketException ex) {
            // Any network issue should trigger an HA failover
            throw new GPUdbExitException(ex.getMessage());
        } catch (Exception ex) {
            GPUdbLogger.debug_with_info( "Caught Exception: " + ex.getMessage() );
            // Some sort of submission error
            throw new SubmitException(url, null, ex.getMessage(), ex);
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
                    GPUdbLogger.error( ex.getMessage() );
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
                GPUdbLogger.debug_with_info( "Pinging URL: " + url.toString() );
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
     * returns an empty string.  Use the timeout of this GPUdb class.
     *
     * @return the ping response, or an empty string if it fails.
     */
    public String ping(URL url) {
        return ping( url, this.timeout );
    }


    /**
     * Pings the given URL and returns the response.  If no response,
     * returns an empty string.
     *
     * @param url  the URL to which the connection needs to be made
     * @param timeout a positive integer representing the number of
     *                milliseconds to use for connection timeout
     *
     * @return the ping response, or an empty string if it fails.
     */
    public String ping(URL url, int timeout) {
        GPUdbLogger.debug_with_info( "Pinging URL: " + url.toString() );

        HttpURLConnection connection = null;

        try {
            connection = initializeHttpConnection( url, timeout );

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
     * Use a very short timeout so that we don't wait for a long time if the server
     * at the given URL is not accessible.
     *
     * @return true if Kinetica is running, false otherwise.
     */
    public boolean isKineticaRunning(URL url) {

        // Use a super short timeout (0.5 second)
        String pingResponse = ping( url, 500 );
        GPUdbLogger.debug_with_info( "HTTP server @ " + url.toString() + " responded with: '" + pingResponse + "'" );
        if ( pingResponse.equals("Kinetica is running!") ) {
            // Kinetica IS running!
            return true;
        }

        // Did not get the expected response
        return false;
    }   // end isKineticaRunning

}
