package com.gpudb;

import com.gpudb.protocol.AdminShowShardsRequest;
import com.gpudb.protocol.GetRecordsRequest;
import com.gpudb.protocol.GetRecordsResponse;
import com.gpudb.protocol.RawGetRecordsResponse;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final List<Integer> routingTable;
    private final List<URL> workerUrls;

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

        if (workers != null && !workers.isEmpty()) {
            try {
                for (URL url : workers) {
                    this.workerUrls.add(GPUdbBase.appendPathToURL(url, "/get/records"));
                }
            } catch (MalformedURLException ex) {
                throw new GPUdbException(ex.getMessage(), ex);
            }

            routingTable = gpudb.adminShowShards(new AdminShowShardsRequest()).getRank();

            for (int i = 0; i < routingTable.size(); i++) {
                if (routingTable.get(i) > workers.size()) {
                    throw new IllegalArgumentException("Too few worker URLs specified.");
                }
            }
        } else {
            routingTable = null;
        }
    }

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

        if (routingTable != null) {
            RecordKey shardKey = shardKeyBuilder.build(keyValues);
            URL url = workerUrls.get(shardKey.route(routingTable));
            response = gpudb.submitRequest(url, request, response, false);
        } else {
            response = gpudb.submitRequest("/get/records", request, response, false);
        }

        GetRecordsResponse<T> decodedResponse = new GetRecordsResponse<>();
        decodedResponse.setTableName(response.getTableName());
        decodedResponse.setTypeName(response.getTypeName());
        decodedResponse.setTypeSchema(response.getTypeSchema());

        if (typeObjectMap == null) {
            decodedResponse.setData(gpudb.<T>decode(type, response.getRecordsBinary()));
        } else {
            decodedResponse.setData(gpudb.<T>decode(typeObjectMap, response.getRecordsBinary()));
        }

        decodedResponse.setTotalNumberOfRecords(response.getTotalNumberOfRecords());
        decodedResponse.setHasMoreRecords(response.getHasMoreRecords());
        return decodedResponse;
    }
}