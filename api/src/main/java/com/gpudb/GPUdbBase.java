package com.gpudb;

import com.gpudb.protocol.GpudbResponse;
import com.gpudb.protocol.ShowTableRequest;
import com.gpudb.protocol.ShowTableResponse;
import com.gpudb.protocol.ShowTypesRequest;
import com.gpudb.protocol.ShowTypesResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
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
        private String userAuth;
        private boolean useSnappy;
        private int threadCount = 1;
        private ExecutorService executor;
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
         * Gets the user authorization string to be used when making requests to
         * GPUdb.
         *
         * @return  the user authorization string
         *
         * @see #setUserAuth(String)
         */
        public String getUserAuth() {
            return userAuth;
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
         * Sets the user authorization string to be used when making requests to
         * GPUdb. If set to a value other than {@code null} (the default) or an
         * empty string, any GPUdb requests made via the API that support a user
         * authorization string will use this value in lieu of any value
         * provided in the request itself.
         *
         * @param value  the user authorization string
         * @return       the current {@link Options} instance
         *
         * @see #getUserAuth()
         */
        public Options setUserAuth(String value) {
            userAuth = value;
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
            if (timeout < 0) {
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
         * Gets the URL that the failed request was submitted to.
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
         * Gets the size in bytes of the encoded failed request.
         *
         * @return  the size of the encoded request
         */
        public int getRequestSize() {
            return requestSize;
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

    // Fields

    private URL url;
    private String username;
    private String password;
    private String authorization;
    private String userAuth;
    private boolean useSnappy;
    private int threadCount;
    private ExecutorService executor;
    private int timeout;
    private ConcurrentHashMap<Class<?>, TypeObjectMap<?>> knownTypeObjectMaps;
    private ConcurrentHashMap<String, Object> knownTypes;

    // Constructor

    protected GPUdbBase(String url, Options options) throws GPUdbException {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException ex) {
            throw new GPUdbException(ex.getMessage(), ex);
        }

        init(options);
    }

    protected GPUdbBase(URL url, Options options) throws GPUdbException {
        this.url = url;
        init(options);
    }

    private void init(Options options) {
        username = options.getUsername();
        password = options.getPassword();

        if (username != null || password != null) {
            authorization = "Basic " + Base64.encodeBase64String(((username != null ? username : "") + ":" + (password != null ? password : "")).getBytes()).replace("\n", "");
        } else {
            authorization = null;
        }

        userAuth = options.getUserAuth() != null ? options.getUserAuth() : "";
        useSnappy = options.getUseSnappy();
        executor = options.getExecutor();
        threadCount = options.getThreadCount();
        timeout = options.getTimeout();
        knownTypeObjectMaps = new ConcurrentHashMap<>(16, 0.75f, 1);
        knownTypes = new ConcurrentHashMap<>(16, 0.75f, 1);
    }

    // Properties

    /**
    * Gets the URL of the GPUdb server.
    *
    * @return  the URL
    */
    public URL getURL() {
        return url;
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
     * Gets the user authorization string used when making requests to GPUdb.
     * Will be an empty string if none was provided to the {@link
     * GPUdb#GPUdb(String, GPUdbBase.Options)} GPUdb constructor) via {@link
     * Options}.
     *
     * @return  the user authorization string
     *
     * @see Options#setUserAuth(String)
     */
    public String getUserAuth() {
        return userAuth;
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
        if (data.isEmpty()) {
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
        return Avro.encode(typeObjectMap, data, threadCount, executor);
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
     * @return          the response object (same as {@code response} parameter)
     *
     * @throws GPUdbException if an error occurs during the request
     */
    public <T extends IndexedRecord> T submitRequest(String endpoint, IndexedRecord request, T response) throws GPUdbException {
        try {
            return submitRequest(new URL(url, endpoint), request, response, false);
        } catch (MalformedURLException ex) {
            throw new GPUdbException(ex.getMessage(), ex);
        }
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
     * @return                   the response object (same as {@code response}
     *                           parameter)
     *
     * @throws GPUdbException if an error occurs during the request
     */
    public <T extends IndexedRecord> T submitRequest(String endpoint, IndexedRecord request, T response, boolean enableCompression) throws GPUdbException {
        try {
            return submitRequest(new URL(url, endpoint), request, response, enableCompression);
        } catch (MalformedURLException ex) {
            throw new GPUdbException(ex.getMessage(), ex);
        }
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
     * @throws GPUdbException if an error occurs while preparing the request
     *
     * @throws SubmitException if an error occurs while submitting the request
     */
    public <T extends IndexedRecord> T submitRequest(URL url, IndexedRecord request, T response, boolean enableCompression) throws GPUdbException {
        enableCompression = enableCompression && useSnappy;
        byte[] encodedRequest;

        if (enableCompression) {
            try {
                encodedRequest = Snappy.compress(Avro.encode(request).array());
            } catch (IOException ex) {
                throw new GPUdbException(ex.getMessage(), ex);
            }
        } else {
            encodedRequest = Avro.encode(request).array();
        }

        int requestSize = encodedRequest.length;

        try {
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(requestSize);

            if (enableCompression) {
                connection.setRequestProperty("Content-type", "application/x-snappy");
            } else {
                connection.setRequestProperty("Content-type", "application/octet-stream");
            }

            if (!userAuth.isEmpty()) {
                connection.setRequestProperty("GPUdb-User-Auth", userAuth);
            }

            if (authorization != null) {
                connection.setRequestProperty ("Authorization", authorization);
            }

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(encodedRequest);
            }

            byte[] buffer = new byte[1024];

            try (InputStream inputStream = connection.getResponseCode() < 400 ? connection.getInputStream() : connection.getErrorStream()) {
                if (inputStream == null) {
                    throw new SubmitException(url, request, requestSize,
                            "Server returned HTTP " + connection.getResponseCode() + " (" + connection.getResponseMessage() + ").");
                }

                int index = 0;

                while (true) {
                    int count = inputStream.read(buffer, index, buffer.length - index);

                    if (count == -1) {
                        break;
                    }

                    index += count;

                    if (index == buffer.length) {
                        buffer = Arrays.copyOf(buffer, buffer.length * 2);
                    }
                }
            }

            connection.disconnect();
            GpudbResponse gpudbResponse = new GpudbResponse();
            Avro.decode(gpudbResponse, ByteBuffer.wrap(buffer));

            if (gpudbResponse.getStatus().equals("ERROR")) {
                throw new SubmitException(url, request, requestSize, gpudbResponse.getMessage());
            }

            return Avro.decode(response, gpudbResponse.getData());
        } catch (IOException ex) {
            throw new SubmitException(url, request, requestSize, ex.getMessage(), ex);
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
        try {
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestMethod("GET");

            byte[] buffer = new byte[1024];
            int index = 0;

            try (InputStream inputStream = connection.getResponseCode() < 400 ? connection.getInputStream() : connection.getErrorStream()) {
                while (true) {
                    int count = inputStream.read(buffer, index, buffer.length - index);

                    if (count == -1) {
                        break;
                    }

                    index += count;

                    if (index == buffer.length) {
                        buffer = Arrays.copyOf(buffer, buffer.length * 2);
                    }
                }
            }

            String response = new String(Arrays.copyOf(buffer, index));

            if (!response.equals("GPUdb is running!")) {
                throw new GPUdbException(response);
            }
        } catch (IOException ex) {
            throw new GPUdbException(ex.getMessage(), ex);
        }
    }
}