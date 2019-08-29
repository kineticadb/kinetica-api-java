package com.gpudb;

import com.gpudb.protocol.ShowSystemPropertiesRequest;
import com.gpudb.protocol.ShowSystemPropertiesResponse;
import com.gpudb.protocol.ShowTableRequest;
import com.gpudb.protocol.ShowTableResponse;
import com.gpudb.protocol.ShowTypesRequest;
import com.gpudb.protocol.ShowTypesResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.NumberFormatException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.commons.codec.binary.Base64;
import org.xerial.snappy.Snappy;

/**
 * Base class for the GPUdb API that provides general functionality not specific
 * to any particular GPUdb request. This class is never instantiated directly;
 * its functionality is accessed via instances of the {@link GPUdb} class.
 */
public abstract class GPUdbBase {

    /**
     * A set of configurable options for the GPUdb API. May be passed into the
     * {@link GPUdb#GPUdb(String, GPUdbBase.Options) GPUdb constructor} to
     * override the default options.
     */
    public static final class Options {
        private String primaryUrl = "";
        private String username;
        private String password;
        private boolean useSnappy = true;
        private int threadCount = 1;
        private ExecutorService executor;
        private Map<String, String> httpHeaders = new HashMap<>();
        private int hmPort = 9300;  // Default host manager port
        private int timeout;

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
         * Gets the timeout value, in milliseconds, after which a lack of
         * response from the GPUdb server will result in requests being aborted.
         * A timeout of zero is interpreted as an infinite timeout. Note that
         * this applies independently to various stages of communication, so
         * overall a request may run for longer than this without being aborted.
         *
         * @return  the timeout value
         *
         * @see #setTimeout(int)
         */
        public int getTimeout() {
            return timeout;
        }

        /**
         * Sets the URL of the primary cluster to use amongst the HA clusters.
         * This cluster will always be used first.  It can be part of the URLs
         * used to create the GPUdb object, or be a different one.  In either
         * case, this URL will always be chosen first to work against.  Also,
         * the {@link GPUdb} constructors will ensure that no duplicate of this
         * URL exists in the full set of URLs to use.  If this is not set, then
         * all available clusters will be treated with equal probability.
         *
         * Must be given in the form of 'http[s]://X.X.X.X:PORT[/httpd-path]'.
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
     * A special exception indicating the the server is shutting down
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



    /**
     * A enumeration of high-availability synchronicity override modes.
     */
    public enum HASynchronicityMode {
        // No override; defer to the HA process for synchronizing
        // endpoints (which has different logic for different endpoints)
        DEFAULT( "none" ),
        // Synchronize all endpoint calls
        SYNCHRONOUS( "sync" ),
        // Do NOT synchronize any endpoint call
        ASYNCHRONOUS( "async" );

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
    private static final String DB_OFFLINE_ERROR_MESSAGE            = "System is offline";
    private static final String DB_CONNECTION_RESET_ERROR_MESSAGE   = "Connection reset";
    private static final String DB_CONNECTION_REFUSED_ERROR_MESSAGE = "Connection refused";
    private static final String DB_EXITING_ERROR_MESSAGE            = "Kinetica is exiting";
    private static final String DB_SYSTEM_LIMITED_ERROR_MESSAGE     = "system-limited-fatal";

    // Internally used headers (make sure to add them to PROTECTED_HEADERS
    protected static final String HEADER_HA_SYNC_MODE  = "ha_sync_mode";
    protected static final String HEADER_AUTHORIZATION = "Authorization";
    protected static final String HEADER_CONTENT_TYPE  = "Content-type";

    // Headers that are prt
    protected static final String[] PROTECTED_HEADERS = new String[]{ HEADER_HA_SYNC_MODE,
                                                                      HEADER_AUTHORIZATION,
                                                                      HEADER_CONTENT_TYPE
    };
    
    // Fields

    private Options   options;
    private List<URL> urls;
    private List<URL> hmUrls;
    private URL primaryUrl = null;
    private final Object urlLock;
    private List<Integer> haUrlIndices;
    private int currentUrlIndex;
    private int numClusterSwitches;
    private String username;
    private String password;
    private String authorization;
    private boolean useSnappy;
    private int threadCount;
    private ExecutorService executor;
    private Map<String, String> httpHeaders;
    private int timeout;
    private HASynchronicityMode haSyncMode;
    private ConcurrentHashMap<Class<?>, TypeObjectMap<?>> knownTypeObjectMaps;
    private ConcurrentHashMap<String, Object> knownTypes;


    // Constructors
    // ------------

    protected GPUdbBase(String url, Options options) throws GPUdbException {
        urlLock = new Object();

        try {
            // Not using an unmodifiable list because we'll have to update it
            // with the HA ring head node addresses
            this.urls = new ArrayList<URL>();

            // Split the string on commas, if any
            String[] url_strings = url.split(",");
            for (int i = 0; i < url_strings.length; ++i ) {
                this.urls.add( new URL( url_strings[i] ) );
            }
        } catch (MalformedURLException ex) {
            throw new GPUdbException(ex.getMessage(), ex);
        }

        init(options);
    }

    protected GPUdbBase(URL url, Options options) throws GPUdbException {
        urlLock = new Object();

        if ( url == null ) {
            throw new GPUdbException( "Must provide at least one URL; gave none!" );
        }

        // Not using an unmodifiable list because we'll have to update it
        // with the HA ring head node addresses
        this.urls = list( url );
        init(options);
    }

    protected GPUdbBase(List<URL> urls, Options options) throws GPUdbException {
        urlLock = new Object();

        if ( urls.isEmpty() ) {
            throw new GPUdbException( "Must provide at least one URL; gave none!" );
        }
        
        // Not using an unmodifiable list because we'll have to update it
        // with the HA ring head node addresses
        if ( urls.size() > 1 ) {
            this.urls = new ArrayList<>( urls );
        } else {
            this.urls = urls;
        }
        
        init(options);
    }


    /**
     * Construct the GPUdbBase object based on the given URLs and options.
     */
    private void init(Options options) throws GPUdbException {
        // Save the options object
        this.options  = options;

        // Save some parameters passed in via the options object
        this.username = options.getUsername();
        this.password = options.getPassword();

        if ((username != null && !username.isEmpty()) || (password != null && !password.isEmpty())) {
            authorization = ("Basic "
                             + Base64.encodeBase64String( ((username != null ? username : "")
                                                           + ":"
                                                           + (password != null ? password : "")).getBytes() )
                               .replace("\n", "") );
        } else {
            authorization = null;
        }

        this.useSnappy   = options.getUseSnappy();
        this.threadCount = options.getThreadCount();
        this.executor    = options.getExecutor();
        this.timeout     = options.getTimeout();

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
        
        // Create URLs for the host manager
        this.hmUrls = new ArrayList<>();

        // We haven't switched to any cluster yet
        this.numClusterSwitches = 0;
        
        // Handle the primary host URL, if any is given
        handlePrimaryURL();

        // Update the URLs with the available HA ring information
        getHAringHeadNodeAdresses();

        // Create the host manager URLs
        updateHmUrls();

        // Randomize the URL indices (take care of the primary host though)
        randomizeURLs();
    }   // end init



    // Properties
    // ----------

    /**
     * Gets the list of URLs for the GPUdb server. At any given time, one
     * URL will be active and used for all GPUdb calls (call {@link #getURL
     * getURL} to determine which one), but in the event of failure, the
     * other URLs will be tried in order, and if a working one is found
     * it will become the new active URL.
     *
     * @return  the list of URLs
     */
    public List<URL> getURLs() {
        return this.urls;
    }

    /**
     * Gets the active URL of the GPUdb server.
     *
     * @return  the URL
     */
    public URL getURL() {
        if (this.urls.size() == 1) {
            return this.urls.get(0);
        } else {
            synchronized (urlLock) {
                return this.urls.get( this.haUrlIndices.get( this.currentUrlIndex ) );
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
        return this.hmUrls;
    }

    /**
     * Gets the active URL of the GPUdb host manager.
     *
     * @return  the URL
     */
    public URL getHmURL() {
        if (this.hmUrls.size() == 1) {
            return this.hmUrls.get(0);
        } else {
            synchronized (urlLock) {
                return this.hmUrls.get( this.haUrlIndices.get( this.currentUrlIndex ) );
            }
        }
    }

    /**
     * Gets the URL of the primary cluster of the HA environment.
     *
     * @return  the primary URL
     */
    public URL getPrimaryUrl() {
        return this.primaryUrl;
    }


    /**
     * Gets the number of times the client has switched to a different
     * cluster amongst the high availability ring.
     */
    public int getNumClusterSwitches() {
        return this.numClusterSwitches;
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
            throw new GPUdbException( "'null' not allowed for either header or value!" );
        }
        
        // Ensure that the given header is not a protecte header
        for ( int i = 0; i < PROTECTED_HEADERS.length; ++i ) {
            if ( header == PROTECTED_HEADERS[ i ] ) {
                throw new GPUdbException( "Not allowed to change proteced header: "
                                          + header );
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
            throw new GPUdbException( "Need a non-null value for the header; null given!" );
        }
        
        // Ensure that the given header is not a protecte header
        for ( int i = 0; i < PROTECTED_HEADERS.length; ++i ) {
            if ( header == PROTECTED_HEADERS[ i ] ) {
                throw new GPUdbException( "Not allowed to remove proteced header: "
                                          + header );
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
     * Re-sets the host manager port number for the host manager URLs. Some
     * endpoints are supported only at the host manager, rather than the
     * head node of the database.
     *
     * @param value  the host manager port number
     * @return       the current {@link GPUdbBase} instance
     */
    public GPUdbBase setHostManagerPort(int value) throws IllegalArgumentException, GPUdbException {
        if (value < 0) {
            throw new IllegalArgumentException("Host manager port number must be greater than zero; "
                                               + "given " + value );
        }
        if (value > 65536) {
            throw new IllegalArgumentException("Host manager port number must be less than 65536; "
                                               + "given " + value );
        }

        // Reset the port for all the host manager URLs
        for ( int i = 0; i < this.hmUrls.size(); ++i ) {
            try {
                URL oldUrl = this.hmUrls.get( i );
                // Change only the port to be the host manager port
                URL newUrl = new URL( oldUrl.getProtocol(), oldUrl.getHost(),
                                      value,
                                      oldUrl.getFile() );
                this.hmUrls.set( i, newUrl );
            } catch ( MalformedURLException ex ) {
                throw new GPUdbException( ex.getMessage(), ex );
            }
        }
        return this;
    }

    


    // Helper Functions
    // ----------------

    /**
     * Creates/updates the host manager URLs from the regular URLs.
     */
    private void updateHmUrls() throws GPUdbException {
        synchronized (urlLock) {
            // Clear existing host manager URLs
            this.hmUrls.clear();

            // Generate the host manager URLs from the head node URLs
            for ( URL url : this.urls ) {
                try {
                    // Change only the port to be the host manager port
                    URL hmUrl = new URL( url.getProtocol(), url.getHost(),
                                         options.getHostManagerPort(),
                                         url.getFile() );
                    this.hmUrls.add( hmUrl );
                } catch ( MalformedURLException ex ) {
                    throw new GPUdbException( ex.getMessage(), ex );
                }
            }
        }
    }
    

    /**
     * Switches the URL of the HA ring cluster.  Check if we've circled back to
     * the old URL.  If we've circled back to it, then re-shuffle the list of
     * indices so that the next time, we pick up HA clusters in a different random
     * manner and throw an exception.
     */
    private URL switchURL(URL oldURL) throws GPUdbHAUnavailableException {

        synchronized (urlLock) {
            // If there is only one URL, then we can't switch URLs
            if ( this.urls.size() == 1 ) {
                throw new GPUdbHAUnavailableException(" (GPUdb server at "
                                                      + this.urls.get(0).toString()
                                                      + " returned error; no HA clusters "
                                                      + "available to fall back on).");
            }

            // Increment the index by one (mod url list length)
            this.currentUrlIndex = (this.currentUrlIndex + 1) % this.urls.size();

            // Keep a running count of how many times we had to switch clusters
            ++this.numClusterSwitches;

            // We've circled back; shuffle the indices again so that future
            // requests go to a different randomly selected cluster, but also
            // let the caller know that we've circled back
            if (this.urls.get( haUrlIndices.get( currentUrlIndex ) ) == oldURL) {
                // Re-shuffle and set the index counter to zero
                randomizeURLs();

                // Let the user know that we've circled back
                throw new GPUdbHAUnavailableException(" (all GPUdb clusters with "
                                                      + "head nodes [" + this.urls.toString()
                                                      + "] returned error)");
            }

            // Haven't circled back to the old URL; so return the new one
            return this.urls.get( haUrlIndices.get( currentUrlIndex ) );
        }
    }  // end switchURL

    /**
     * Switches the host manager  URL of the HA ring cluster.  Check if we've
     * circled back to the old URL.  If we've circled back to it, then 
     * re-shuffle the list of indices so that the next time, we pick up HA
     * clusters in a different random manner and throw an exception.
     */
    private URL switchHmURL(URL oldURL) throws GPUdbHAUnavailableException {
        synchronized (urlLock) {
            // If there is only one URL, then we can't switch URLs
            if ( this.hmUrls.size() == 1 ) {
                throw new GPUdbHAUnavailableException(" (host manager at "
                                                      + this.hmUrls.get(0).toString()
                                                      + "returned error; no HA clusters "
                                                      + "available to fall back on).");
            }

            // Increment the index by one (mod url list length)
            this.currentUrlIndex = (this.currentUrlIndex + 1) % this.hmUrls.size();

            // Keep a running count of how many times we had to switch clusters
            ++this.numClusterSwitches;

            // We've circled back; shuffle the indices again so that future
            // requests go to a different randomly selected cluster, but also
            // let the caller know that we've circled back
            if (this.hmUrls.get( haUrlIndices.get( currentUrlIndex ) ) == oldURL) {
                // Re-shuffle and set the index counter to zero
                randomizeURLs();

                // Let the user know that we've circled back
                throw new GPUdbHAUnavailableException(" (all host managers at GPUdb clusters with "
                                                      + "head nodes [" + this.hmUrls.toString()
                                                      + "] returned error)");
            }

            // Haven't circled back to the old URL; so return the new one
            return this.hmUrls.get( haUrlIndices.get( currentUrlIndex ) );
        }
    }   // end switchHmUrl


    /**
     * Create and initialize an HTTP connection object with the request headers
     *  (including authorization header), connection type, time out etc.
     *
     * @param url  the URL to which the connection needs to be made
     * @return     the initialized HTTP connection object
     */
    protected HttpURLConnection initializeHttpConnection( URL url ) throws Exception {
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        // Set the timeout
        connection.setConnectTimeout( timeout );
        connection.setReadTimeout( timeout );

        // Set the request type
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        // Set the user defined headers
        for (Map.Entry<String, String> entry : this.httpHeaders.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        // Set the sync mode header
        connection.setRequestProperty( HEADER_HA_SYNC_MODE, this.haSyncMode.getMode() );

        // Set the authorization header
        if (authorization != null) {
            connection.setRequestProperty (HEADER_AUTHORIZATION, authorization);
        }

        return connection;
    }
    

    /**
     * Automatically resets the host manager port number for the host manager
     * URLs by finding out what the host manager port is.
     *
     * @return  boolean indicating if the host manager port was successfully
     *          updated to be a *different* value.
     */
    protected boolean updateHostManagerPort() throws GPUdbException {


        int oldHmPort = this.getHmURL().getPort();
        int hmPort;

        // Find out from the database server what the correct port
        // number is
        ShowSystemPropertiesResponse response = submitRequest("/show/system/properties",
                                                              new ShowSystemPropertiesRequest(),
                                                              new ShowSystemPropertiesResponse(),
                                                              false);
        Map<String, String> systemProperties = response.getPropertyMap();
        String port_str = systemProperties.get( ShowSystemPropertiesResponse.PropertyMap.CONF_HM_HTTP_PORT );

        if (port_str == null) {
            // Instead of throwing an exception, just return the fact that we couldn't
            // update the port
            return false;
        }

        // Parse the integer value from the string
        try {
            hmPort = Integer.parseInt( port_str );
        } catch (NumberFormatException ex) {
            // Instead of throwing an exception, just return the fact that we couldn't
            // update the port
            return false;
        }
        
        // Update the host manager URLs with the correct port number
        try {
            this.setHostManagerPort( hmPort );
        } catch (IllegalArgumentException ex) {
            // Instead of throwing an exception, just return the fact that we couldn't
            // update the port
            return false;
        } catch (GPUdbException ex) {
            // Instead of throwing an exception, just return the fact that we couldn't
            // update the port
            return false;
        }

        // Return whether we actually updated the port or not
        if ( oldHmPort != hmPort ) {
            return true;
        } else {
            return false;
        }
    }   // end updateHostManagerPort


    /**
     * Ensures that any given primary host is included in the list of all HA
     * ring head nodes, and is the very first in line.  No-op if no primary
     * host is given by the user.
     *
     * @param primaryUrlStr  a valid URL string containing the address for the
     *                       HA cluster to be used as the primary host.
     */
    private void handlePrimaryURL() throws GPUdbException {
        String primaryUrlStr = this.options.getPrimaryUrl();
        // No-op if an empty string is given
        if ( primaryUrlStr.isEmpty() ) {
            return;
        }

        // Parse the URL
        try {
            this.primaryUrl = new URL( primaryUrlStr );
        } catch (MalformedURLException ex) {
            throw new GPUdbException( "Error parsing the primary host URL: "
                                      + ex.getMessage(), ex);
        }

        synchronized (urlLock) {
            // Check if the primary host exists in the list of user given hosts
            int primaryIndex = this.urls.indexOf( this.primaryUrl );
            if ( primaryIndex != -1 ) {
                // The primary URL already exists in the list
                if ( primaryIndex > 0 ) {
                    // Note: Do not combine the nested if with the top level if; will change
                    //       logic and may end up getting duplicates of the primary URL

                    // Move the primary URL to the front of the list, if not already
                    java.util.Collections.swap( this.urls, 0, this.urls.indexOf( this.primaryUrl ) );
                }
            } else {
                // The primary URL must be the first element in the list
                this.urls.add( 0, this.primaryUrl );
            }
        }

        // Update the host manager URLs
        updateHmUrls();
    }


    /**
     * Contact the database to find out any high availability (HA) ring head
     * node addresses.  If any found, update the list of hosts.
     */
    private void getHAringHeadNodeAdresses() throws GPUdbException {
        // Get the HA ring head node addresses, if any
        ShowSystemPropertiesResponse response = null;
        try {
            response = submitRequest(appendPathToURL(this.urls.get(0), "/show/system/properties"),
                                     new ShowSystemPropertiesRequest(),
                                     new ShowSystemPropertiesResponse(),
                                     false);
        } catch (MalformedURLException ex) {
            // Note: Not worth dying just because the HA ring node
            // addresses couldn't be found
        } catch (GPUdbException ex) {
            // Note: Not worth dying just because the HA ring node
            // addresses couldn't be found
        }

        // Do the rest only if we were able to get the system properties
        if ( response != null ) {
            Map<String, String> systemProperties = response.getPropertyMap();
            String is_ha_enabled_str = systemProperties.get( ShowSystemPropertiesResponse.PropertyMap.CONF_ENABLE_HA );

            // Only attempt to parse the HA ring node addresses if HA is enabled
            if ( (is_ha_enabled_str != null)
                 && (is_ha_enabled_str.compareToIgnoreCase( "true" ) == 0 ) ) {

                // Parse the HA ringt head node addresses, if any
                String ha_ring_head_nodes_str = systemProperties.get( ShowSystemPropertiesResponse.PropertyMap.CONF_HA_RING_HEAD_NODES );
                if ( (ha_ring_head_nodes_str != null) && !ha_ring_head_nodes_str.isEmpty() ) {
                    List<URL> ha_urls = new ArrayList<URL>();
                    try {
                        String[] ha_ring_head_nodes = ha_ring_head_nodes_str.split(",");
                        for (int i = 0; i < ha_ring_head_nodes.length; ++i ) {
                            ha_urls.add( new URL( ha_ring_head_nodes[i] ) );
                        }
                    } catch (MalformedURLException ex) {
                        throw new GPUdbException( "Error parsing HA ring head "
                                                  + "node address from the "
                                                  + "database configuration "
                                                  + "parameters: "
                                                  + ex.getMessage(), ex);
                    }

                    // Ensure that the first of the given URL is included in the
                    // HA ring head node addresses
                    if ( !ha_urls.contains( this.urls.get( 0 ) ) ) {
                        ha_urls.add( this.urls.get( 0 ) );
                    }

                    // Now save these head node addresses
                    synchronized (urlLock) {
                        this.urls = new ArrayList<>( ha_urls );
                    }
                }

                // Handle the primary host URL, if any is given
                handlePrimaryURL();
                // Update the host manager URLs
                updateHmUrls();
                // Randomize the URLs
                randomizeURLs();
            } // nothing to do if this property isn't returned
        }
    }   // end getHAringHeadNodeAdresses


    /**
     * Randomly shuffles the list of high availability URL indices so that HA
     * failover happens at a random fashion.  One caveat is when a primary host
     * is given by the user; in that case, we need to keep the primary host's
     * index as the first one in the list so that upon failover, when we cricle
     * back, we always pick the first/primary host up again.
     */
    private void randomizeURLs() {
        // Re-create the list of HA URL indices
        this.haUrlIndices.clear();
        for ( int i = 0; i < this.urls.size(); ++i ) {
            this.haUrlIndices.add( i );
        }

        if ( this.primaryUrl == null ) {
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

        // This will keep track of which cluster to pick next (an index of
        // randomly shuffled indices)
        this.currentUrlIndex = 0;
    }
    
    
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
            try {
                return submitRequest(appendPathToURL(url, endpoint), request, response, enableCompression);
            } catch (MalformedURLException ex) {
                // There's an error in creating the URL
                throw new GPUdbRuntimeException(ex.getMessage(), ex);
            } catch (GPUdbExitException ex) {
                // Handle our special exit exception
                try {
                    url = switchURL( originalURL );
                } catch (GPUdbHAUnavailableException ha_ex) {
                    // We've now tried all the HA clusters and circled back
                    // Get the original cause to propagate to the user
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause
                                              + ha_ex.getMessage(), true );
                }
            } catch (SubmitException ex) {
                // Some error occurred during the HTTP request
                try {
                    url = switchURL( originalURL );
                } catch (GPUdbHAUnavailableException ha_ex) {
                    // We've now tried all the HA clusters and circled back
                    // Get the original cause to propagate to the user
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new SubmitException( null, ex.getRequest(), ex.getRequestSize(),
                                               (originalCause
                                                + ha_ex.getMessage()),
                                               ex.getCause(), true );
                }
            } catch (GPUdbException ex) {
                // Any other GPUdbException is a valid failure
                throw ex;
            } catch (Exception ex) {
                // And other random exceptions probably are also connection errors
                try {
                    url = switchURL( originalURL );
                } catch (GPUdbHAUnavailableException ha_ex) {
                    // We've now tried all the HA clusters and circled back
                    // Get the original cause to propagate to the user
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause
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

        while (true) {
            try {
                return submitRequest(appendPathToURL(hmUrl, endpoint), request, response, enableCompression);
            } catch (MalformedURLException ex) {
                // There's an error in creating the URL
                throw new GPUdbRuntimeException(ex.getMessage(), ex);
            } catch (GPUdbExitException ex) {
                // Handle our special exit exception
                try {
                    hmUrl = switchHmURL( originalURL );
                } catch (GPUdbHAUnavailableException ha_ex) {
                    // We've now tried all the HA clusters and circled back
                    // Get the original cause to propagate to the user
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause
                                              + ha_ex.getMessage(), true );
                }
            } catch (SubmitException ex) {
                // Some error occurred during the HTTP request;
                // first check if the host manager port is wrong
                try {
                    if ( this.updateHostManagerPort() ) {
                        // Get the updated URL
                        hmUrl = getHmURL();
                    } else {
                        // Upon failure, try to use other clusters
                        try {
                            hmUrl = switchHmURL( originalURL );
                        } catch (GPUdbHAUnavailableException ha_ex) {
                            // We've now tried all the HA clusters and circled back
                            // Get the original cause to propagate to the user
                            String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                            throw new SubmitException( null, ex.getRequest(), ex.getRequestSize(),
                                                       (originalCause
                                                        + ha_ex.getMessage()),
                                                       ex.getCause(), true );
                        }
                    }
                } catch (Exception ex2) {
                    // Upon any error, try to use other clusters
                    try {
                        hmUrl = switchHmURL( originalURL );
                    } catch (GPUdbHAUnavailableException ha_ex) {
                        // We've now tried all the HA clusters and circled back
                        // Get the original cause to propagate to the user
                        String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                        throw new SubmitException( null, ex.getRequest(), ex.getRequestSize(),
                                                   (originalCause
                                                    + ha_ex.getMessage()),
                                                   ex.getCause(), true );
                    }
                }
            } catch (GPUdbException ex) {
                // the host manager can still be going even if the database is down
                if ( ex.getMessage().contains( DB_OFFLINE_ERROR_MESSAGE ) ) {
                    try {
                        hmUrl = switchHmURL( originalURL );
                    } catch (GPUdbHAUnavailableException ha_ex) {
                        // We've now tried all the HA clusters and circled back
                        // Get the original cause to propagate to the user
                        String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                        throw new GPUdbException( originalCause
                                                  + ha_ex.getMessage(), true );
                    }
                }
                else {
                    // Any other GPUdbException is a valid failure
                    throw ex;
                }
            } catch (Exception ex) {
                // And other random exceptions probably are also connection errors
                try {
                    hmUrl = switchHmURL( originalURL );
                } catch (GPUdbHAUnavailableException ha_ex) {
                    // We've now tried all the HA clusters and circled back
                    // Get the original cause to propagate to the user
                    String originalCause = (ex.getCause() == null) ? ex.toString() : ex.getCause().toString();
                    throw new GPUdbException( originalCause
                                              + ha_ex.getMessage(), true );
                }
            }
        } // end while
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
    public <T extends IndexedRecord> T submitRequest(URL url, IndexedRecord request, T response, boolean enableCompression) throws SubmitException, GPUdbExitException, GPUdbException {

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
    public <T extends IndexedRecord> T submitRequestRaw(URL url, IndexedRecord request, T response, boolean enableCompression) throws SubmitException, GPUdbExitException, GPUdbException {
        int requestSize = -1;
        HttpURLConnection connection = null;

        try {
            connection = initializeHttpConnection( url );

            if (enableCompression && useSnappy) {
                byte[] encodedRequest = Snappy.compress(Avro.encode(request).array());
                requestSize = encodedRequest.length;
                connection.setRequestProperty( HEADER_CONTENT_TYPE, "application/x-snappy" );
                connection.setFixedLengthStreamingMode(requestSize);

                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(encodedRequest);
                }
            } else {
                byte[] encodedRequest = Avro.encode(request).array();
                requestSize = encodedRequest.length;
                connection.setRequestProperty( HEADER_CONTENT_TYPE, "application/octet-stream" );
                connection.setFixedLengthStreamingMode(requestSize);

                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(encodedRequest);
                }

                /*
                connection.setRequestProperty(HEADER_CONTENT_TYPE, "application/octet-stream");
                connection.setChunkedStreamingMode(1024);

                try (OutputStream outputStream = connection.getOutputStream()) {
                    // Manually encode the request directly into the stream to
                    // avoid allocation of intermediate buffers

                    CountingOutputStream countingOutputStream = new CountingOutputStream(outputStream);
                    BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(countingOutputStream, null);
                    new GenericDatumWriter<>(request.getSchema()).write(request, encoder);
                    encoder.flush();
                    requestSize = countingOutputStream.getByteCount();
                }
                */
            }

            try (InputStream inputStream = connection.getResponseCode() < 400 ? connection.getInputStream() : connection.getErrorStream()) {
                if (inputStream == null) {
                    // Some error occurred during the submission process
                    String message = ("Server returned HTTP " + connection.getResponseCode() + " (" + connection.getResponseMessage() + ").");
                    throw new SubmitException(url, request, requestSize, message);
                }

                try {
                    // Manually decode the RawGpudbResponse wrapper directly from
                    // the stream to avoid allocation of intermediate buffers

                    BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(inputStream, null);
                    String status = decoder.readString();
                    String message = decoder.readString();

                    if (status.equals("ERROR")) {
                        // Check if Kinetica is shutting down
                        if ( message.contains( DB_EXITING_ERROR_MESSAGE )
                             || message.contains( DB_CONNECTION_REFUSED_ERROR_MESSAGE )
                             || message.contains( DB_CONNECTION_RESET_ERROR_MESSAGE )
                             || message.contains( DB_SYSTEM_LIMITED_ERROR_MESSAGE ) ) {
                            throw new GPUdbExitException( message );
                        }
                        // A legitimate error
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
            }
        } catch (GPUdbExitException ex) {
            // DB is shutting down
            throw ex;
        } catch (SubmitException ex) {
            // Some sort of submission error
            throw ex;
        } catch (GPUdbException ex) {
            // Some legitimate error
            throw ex;
        } catch (Exception ex) {
            // Some sort of submission error
            throw new SubmitException(url, request, requestSize, ex.getMessage(), ex);
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception ex) {
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
                    url = switchURL( originalURL );
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
    }
}
