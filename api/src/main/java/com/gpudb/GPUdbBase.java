package com.gpudb;

import com.gpudb.protocol.ShowSystemPropertiesRequest;
import com.gpudb.protocol.ShowSystemPropertiesResponse;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
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
        private String username;
        private String password;
        private boolean useSnappy = true;
        private boolean bypassSslCertCheck = false;
        private int threadCount = 1;
        private ExecutorService executor;
        private Map<String, String> httpHeaders = new HashMap<>();
        private int hmPort = 9300;  // Default host manager port
        private int timeout;

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
    }

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
    }

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

    protected static final String HA_SYNC_MODE = "ha_sync_mode";
    
    // Fields

    private List<URL> urls;
    private List<URL> hmUrls;
    private final Object urlLock;
    private int currentURL;
    private int currentHmURL;
    private String username;
    private String password;
    private String authorization;
    private boolean useSnappy;
    private boolean bypassSslCertCheck;
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
            urls = Collections.unmodifiableList(list(new URL(url)));
        } catch (MalformedURLException ex) {
            throw new GPUdbException(ex.getMessage(), ex);
        }

        init(options);
    }

    protected GPUdbBase(URL url, Options options) throws GPUdbException {
        urlLock = new Object();
        urls = Collections.unmodifiableList(list(url));
        init(options);
    }

    protected GPUdbBase(List<URL> urls, Options options) throws GPUdbException {
        urlLock = new Object();
        this.urls = Collections.unmodifiableList(urls);
        currentURL   = (int)(Math.random() * this.urls.size());
        currentHmURL = (int)(Math.random() * this.hmUrls.size());
        init(options);
    }

    private void init(Options options) throws GPUdbException {
        username = options.getUsername();
        password = options.getPassword();

        if ((username != null && !username.isEmpty()) || (password != null && !password.isEmpty())) {
            authorization = "Basic " + Base64.encodeBase64String(((username != null ? username : "") + ":" + (password != null ? password : "")).getBytes()).replace("\n", "");
        } else {
            authorization = null;
        }

        useSnappy = options.getUseSnappy();
        threadCount = options.getThreadCount();
        executor = options.getExecutor();

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
            
        
        // Create URLs for the host manager
        this.hmUrls = new ArrayList();
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

        Map<String, String> tempHttpHeaders = new HashMap<>();

        for (Map.Entry<String, String> entry : options.getHttpHeaders().entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                tempHttpHeaders.put(entry.getKey(), entry.getValue());
            }
        }

        httpHeaders = Collections.unmodifiableMap(tempHttpHeaders);
        timeout = options.getTimeout();
        knownTypeObjectMaps = new ConcurrentHashMap<>(16, 0.75f, 1);
        knownTypes = new ConcurrentHashMap<>(16, 0.75f, 1);

        // Set the default sync mode
        this.haSyncMode = HASynchronicityMode.DEFAULT;
    }

    // Properties

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
        return urls;
    }

    /**
     * Gets the active URL of the GPUdb server.
     *
     * @return  the URL
     */
    public URL getURL() {
        if (urls.size() == 1) {
            return urls.get(0);
        } else {
            synchronized (urlLock) {
                return urls.get(currentURL);
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
                return this.hmUrls.get( currentHmURL );
            }
        }
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

    
    /**
     * Automatically resets the host manager port number for the host manager
     * URLs by finding out what the host manager port is.
     */
    protected void updateHostManagerPort() throws GPUdbException {

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
            throw new GPUdbException("Missing value for '"
                                     + ShowSystemPropertiesResponse.PropertyMap.CONF_HM_HTTP_PORT
                                     + "'" );
        }

        // Parse the integer value from the string
        try {
            hmPort = Integer.parseInt( port_str );
        } catch (NumberFormatException ex) {
            throw new GPUdbException( "No parsable value found for host manager port number "
                                      + "(expected integer, given '" + port_str + "')" );
        }

        // Update the host manager URLs with the correct port number
        this.setHostManagerPort( hmPort );
        return;
    }


    // Type Management

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
        for (Map.Entry<String, String> entry : httpHeaders.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        // Set the sync mode header
        connection.setRequestProperty( HA_SYNC_MODE, this.haSyncMode.getMode() );

        // Set the authorization header
        if (authorization != null) {
            connection.setRequestProperty ("Authorization", authorization);
        }

        return connection;
    }
    

    private URL switchURL(URL oldURL) {
        synchronized (urlLock) {
            if (urls.get(currentURL) == oldURL) {
                ++currentURL;

                if (currentURL >= urls.size()) {
                    currentURL = 0;
                }
            }

            return urls.get(currentURL);
        }
    }

    private URL switchHmURL(URL oldURL) {
        synchronized (urlLock) {
            if (this.hmUrls.get( currentHmURL ) == oldURL) {
                ++currentHmURL;

                if (currentHmURL >= this.hmUrls.size()) {
                    currentHmURL = 0;
                }
            }

            return this.hmUrls.get(currentHmURL);
        }
    }

    
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
                                                      T response ) throws SubmitException {
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
                                                      boolean enableCompression ) throws SubmitException {
        // Send the request to the database server head node
        URL url = getURL();
        URL originalURL = url;

        while (true) {
            try {
                return submitRequest(appendPathToURL(url, endpoint), request, response, enableCompression);
            } catch (MalformedURLException ex) {
                throw new GPUdbRuntimeException(ex.getMessage(), ex);
            } catch (SubmitException ex) {
                if (urls.size() == 1) {
                    throw ex;
                } else if (ex.getCause() != null) {
                    url = switchURL(url);

                    // If we've circled back, then none of the URLs worked
                    if (url == originalURL) {
                        throw new SubmitException(null, ex.getRequest(), ex.getRequestSize(), ex.getMessage(), ex.getCause());
                    }
                } else {
                    throw ex;
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
                                                          T response ) throws SubmitException {
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
                                                          boolean enableCompression ) throws SubmitException {
        // Send the request to the host manager
        URL hmUrl = getHmURL();
        URL originalURL = hmUrl;

        while (true) {
            boolean retry = false;

            try {
                return submitRequest(appendPathToURL(hmUrl, endpoint), request, response, enableCompression);
            } catch (MalformedURLException ex) {
                throw new GPUdbRuntimeException(ex.getMessage(), ex);
            } catch (SubmitException ex) {
                if ( this.hmUrls.size() == 1 ) {
                    retry = true;
                } else if (ex.getCause() != null) {
                    hmUrl = switchHmURL( hmUrl );

                    // If we've circled back, then none of the URLs worked
                    if ( hmUrl == originalURL ) {
                        retry = true;
                    }
                } else {
                    throw ex;
                }

                // Try to automatically update the host manager port
                if ( retry ) {
                    try {
                        updateHostManagerPort();
                        hmUrl = getHmURL();
                    } catch (GPUdbException ex2) {
                        throw new SubmitException(null, ex.getRequest(), ex.getRequestSize(), ex2.getMessage());
                    }

                    // Ping the new URL
                    try {
                        return submitRequest(appendPathToURL(hmUrl, endpoint), request, response, enableCompression);
                    } catch (MalformedURLException ex2) {
                        throw new GPUdbRuntimeException(ex2.getMessage(), ex2);
                    } catch (SubmitException ex2) {
                        throw new SubmitException(null, ex2.getRequest(), ex2.getRequestSize(), ex2.getMessage(), ex2.getCause());
                    }
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
    public <T extends IndexedRecord> T submitRequest(URL url, IndexedRecord request, T response, boolean enableCompression) throws SubmitException {
        int requestSize = -1;
        HttpURLConnection connection = null;

        try {
            connection = initializeHttpConnection( url );

            if (enableCompression && useSnappy) {
                byte[] encodedRequest = Snappy.compress(Avro.encode(request).array());
                requestSize = encodedRequest.length;
                connection.setRequestProperty("Content-type", "application/x-snappy");
                connection.setFixedLengthStreamingMode(requestSize);

                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(encodedRequest);
                }
            } else {
                byte[] encodedRequest = Avro.encode(request).array();
                requestSize = encodedRequest.length;
                connection.setRequestProperty("Content-type", "application/octet-stream");
                connection.setFixedLengthStreamingMode(requestSize);

                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(encodedRequest);
                }

                /*
                connection.setRequestProperty("Content-type", "application/octet-stream");
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

            // try (InputStream inputStream = connection.getResponseCode() < 400 ? connection.getInputStream() : connection.getErrorStream()) {

            int response_code = connection.getResponseCode();

            // Ensure that we're not getting any html snippet (may be
            // returned by the HTTPD server)
            if ( connection.getContentType().startsWith( "text" ) ) {
                String responseMsg = connection.getResponseMessage();
                
                String errorMsg;
                if (response_code == 401) {
                    errorMsg = ("Unauthorized access: '"
                                + responseMsg + "'");
                } else {
                    errorMsg = ("Cannot parse response from server: '"
                                + responseMsg + "'");
                }
                throw new SubmitException( url, request, requestSize, errorMsg );
            }

            // Parse response based on error code
            InputStream inputStream;
            if (response_code == 401) {
                throw new SubmitException( url, request, requestSize,
                                           connection.getResponseMessage());
            }
            else if (response_code < 400) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }

            if (inputStream == null) {
                throw new IOException("Server returned HTTP " + connection.getResponseCode() + " (" + connection.getResponseMessage() + ").");
            }

            try {
                // Manually decode the RawGpudbResponse wrapper directly from
                // the stream to avoid allocation of intermediate buffers

                BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(inputStream, null);
                String status = decoder.readString();
                String message = decoder.readString();

                if (status.equals("ERROR")) {
                    throw new SubmitException(url, request, requestSize, message);
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
        } catch (SubmitException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SubmitException(url, request, requestSize, ex.getMessage(), ex);
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception ex) {
                }
            }
        }
    }

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
                if (urls.size() == 1) {
                    throw new GPUdbException(ex.getMessage(), ex);
                } else {
                    url = switchURL(url);

                    if (url == originalURL) {
                        throw new GPUdbException(ex.getMessage(), ex);
                    }
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
