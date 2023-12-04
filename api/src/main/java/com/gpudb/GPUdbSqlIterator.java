package com.gpudb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.gpudb.protocol.ClearTableRequest;
import com.gpudb.protocol.ExecuteSqlRequest;
import com.gpudb.protocol.ExecuteSqlResponse;

/**
 * Kinetica API class for iterating over records
 * returned by executing an SQL query. This class accepts a
 * {@link GPUdb} instance and an SQL statement to facilitate iteration
 * over the records returned by the query. SQL options can be passed as
 * a {@link Map}. The 'batchSize' for the number of records
 * returned is set to a default of '10000', which can be modified using
 * the parameter 'batchSize' to the constructor.
 * 
 * Copyright (c) 2023 Kinetica DB Inc.
 */

public class GPUdbSqlIterator<T extends Record> implements Iterable<T>, AutoCloseable {

    private GPUdb db;
    private String sql;
    private int batchSize;
    private Map<String, String> sqlOptions;

    private int recordPosition = 0;
    private List<T> records = new ArrayList<>();
    private long offset;
    private long totalCount;
    private String pagingTableName;
    private List<String> pagingTableNames = new ArrayList<>();

    /**
     * Constructor for {@link GPUdbSqlIterator}
     * 
     * @param db  - a {@link GPUdb} instance
     * @param sql - the SQL statement to execute
     */
    public GPUdbSqlIterator(GPUdb db, String sql) {
        this(db, sql, 10000, new HashMap<>());
    }

    /**
     * Constructor for {@link GPUdbSqlIterator}
     * 
     * @param db        - a {@link GPUdb} instance
     * @param sql       - the SQL statement to execute
     * @param batchSize - the number of records to fetch
     */
    public GPUdbSqlIterator(GPUdb db, String sql, int batchSize) {
        this(db, sql, batchSize, new HashMap<>());
    }

    /**
     * Constructor for {@link GPUdbSqlIterator}
     * 
     * @param db         - a {@link GPUdb} instance
     * @param sql        - the SQL statement to execute
     * @param batchSize  - the number of records to fetch
     * @param sqlOptions - the SQL options to be passed in
     * 
     * @see GPUdb#executeSql(String, long, long, String, List, Map)
     */
    public GPUdbSqlIterator(GPUdb db, String sql, int batchSize, Map<String, String> sqlOptions) {
        this.db = db;
        this.sql = sql;
        this.batchSize = batchSize;
        this.sqlOptions = sqlOptions;

        this.pagingTableName = UUID.randomUUID().toString().replaceAll("-", "_");
        sqlOptions.put(ExecuteSqlRequest.Options.PAGING_TABLE, pagingTableName);
        checkAndFetchRecords();
    }

    public void setSqlOptions(Map<String, String> sqlOptions) {
        this.sqlOptions = sqlOptions;
    }

    private void checkAndFetchRecords() {
        if (records.size() > 0 && recordPosition < records.size()) {
            return;
        }
        records.clear();
        recordPosition = 0;
        if (offset > totalCount) {
            return;
        }
        executeSql();
        offset += batchSize;
    }

    private void executeSql() {
        try {
            ExecuteSqlResponse response = db.executeSql(sql, offset, batchSize, "", null, sqlOptions);
            records = (List<T>) response.getData();
            totalCount = response.getTotalNumberOfRecords();

            if (totalCount == 0)
                return;

            pagingTableName = response.getPagingTable();
            pagingTableNames.add(pagingTableName);

            String resultTableList = response.getInfo().get("result_table_list");
            if (resultTableList != null) {
                pagingTableNames.addAll(Arrays.asList(resultTableList.split(",")));
            }

        } catch (GPUdbException e) {
            GPUdbLogger.debug(String.format("Error in executing query : %s", e.getMessage()));
        }
    }

    @Override
    public void close() throws Exception {
        Map<String, String> options = new HashMap<>();
        options.put(ClearTableRequest.Options.NO_ERROR_IF_NOT_EXISTS, ClearTableRequest.Options.TRUE);
        pagingTableNames.forEach(tableName -> {
            try {
                db.clearTable(tableName, null, options);
            } catch (GPUdbException e) {
                GPUdbLogger.debug(String.format("Error in deleting paging tables : %s", e.getMessage()));
            }
        });
    }

    @Override
    public Iterator<T> iterator() {
        return new InnerIterator();
    }

    private class InnerIterator implements Iterator<T> {

        @Override
        public boolean hasNext() {
            return records.size() > 0 && recordPosition < records.size();
        }

        @Override
        public T next() {
            checkAndFetchRecords();
            T record = (T) records.get(recordPosition++);
            return record;
        }

    }

}
