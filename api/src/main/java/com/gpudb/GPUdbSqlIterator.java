package com.gpudb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
 * Example usage, passing a database connection and a SQL statement to the
 * {@link GPUdbSqlIterator}:
 * 
 *     try (GPUdbSqlIterator<Record> iterator = new GPUdbSqlIterator<>(gpudb, sql);)
 *     {
 *         for (Record record : iterator)
 *             System.out.println(record);
 *     }
 *     catch (Exception e)
 *     {
 *         System.err.println("Error in iteration: " + e.getMessage());
 *     }
 * 
 * Copyright (c) 2023 Kinetica DB Inc.
 */

public class GPUdbSqlIterator<T extends Record> implements Iterable<T>, AutoCloseable {
    private static final int DEFAULT_BATCH_SIZE = 10000;

    private GPUdb db;
    private String sql;
    private int batchSize;
    private Map<String, String> sqlOptions;

    private int recordPosition = 0;
    private List<T> records = new ArrayList<>();
    private long offset;
    private long totalCount;
    private boolean hasMoreRecords;
    private String pagingTableName;
    private List<String> pagingTableNames = new ArrayList<>();

    /**
     * Constructor for {@link GPUdbSqlIterator}
     * 
     * @param db  - a {@link GPUdb} instance
     * @param sql - the SQL statement to execute
     */
    public GPUdbSqlIterator(GPUdb db, String sql) throws GPUdbException {
        this(db, sql, DEFAULT_BATCH_SIZE, new HashMap<>());
    }

    /**
     * Constructor for {@link GPUdbSqlIterator}
     * 
     * @param db        - a {@link GPUdb} instance
     * @param sql       - the SQL statement to execute
     * @param batchSize - the number of records to fetch
     */
    public GPUdbSqlIterator(GPUdb db, String sql, int batchSize) throws GPUdbException {
        this(db, sql, batchSize, new HashMap<>());
    }

    /**
     * Constructor for {@link GPUdbSqlIterator}
     * 
     * @param db         - a {@link GPUdb} instance
     * @param sql        - the SQL statement to execute
     * @param sqlOptions - the SQL options to be passed in
     * 
     * @see GPUdb#executeSql(String, long, long, String, List, Map)
     */
    public GPUdbSqlIterator(GPUdb db, String sql, Map<String, String> sqlOptions) throws GPUdbException {
        this(db, sql, DEFAULT_BATCH_SIZE, sqlOptions);
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
    public GPUdbSqlIterator(GPUdb db, String sql, int batchSize, Map<String, String> sqlOptions) throws GPUdbException {
        this.db = db;
        this.sql = sql;
        this.batchSize = batchSize;
        this.sqlOptions = sqlOptions;

        this.pagingTableName = UUID.randomUUID().toString().replaceAll("-", "_");
        sqlOptions.put(ExecuteSqlRequest.Options.PAGING_TABLE, this.pagingTableName);
        checkAndFetchRecords();
    }

    public void setSqlOptions(Map<String, String> sqlOptions) {
        this.sqlOptions = sqlOptions;
    }

    public long size()
    {
        return this.totalCount;
    }

    private void checkAndFetchRecords() throws GPUdbException {
        if (this.records.size() > 0 && this.recordPosition < this.records.size()) {
            return;
        }
        this.records.clear();
        this.recordPosition = 0;
        if (this.offset > this.totalCount) {
            return;
        }
        executeSql();
        this.offset += this.batchSize;
    }

    private void executeSql() throws GPUdbException {
        ExecuteSqlResponse response = this.db.executeSql(this.sql, this.offset, this.batchSize, "", null, this.sqlOptions);
        this.records = (List<T>) response.getData();
        this.totalCount = response.getTotalNumberOfRecords();
        this.hasMoreRecords = response.getHasMoreRecords() && this.records.size() > 0;

        if (GPUdbLogger.isTraceEnabled())
        	GPUdbLogger.trace(String.format("Retrieved <%d/%d> records using offset/batch <%d/%d> for query <%s>", this.records.size(), this.totalCount, this.offset, this.batchSize, this.sql));

        if (this.totalCount == 0)
            return;

        this.pagingTableName = response.getPagingTable();
        
        if (this.pagingTableName != null && !this.pagingTableName.isEmpty())
        {
        	this.pagingTableNames.add(this.pagingTableName);
	
	        String resultTableList = response.getInfo().get("result_table_list");

	        if (resultTableList != null && !resultTableList.isEmpty())
	        	this.pagingTableNames.addAll(Arrays.asList(resultTableList.split(",")));
        }
    }

    @Override
    public void close() throws Exception {
        Map<String, String> options = new HashMap<>();
        options.put(ClearTableRequest.Options.NO_ERROR_IF_NOT_EXISTS, ClearTableRequest.Options.TRUE);
        this.pagingTableNames.forEach(tableName -> {
            try {
            	this.db.clearTable(tableName, null, options);
            } catch (GPUdbException e) {
                GPUdbLogger.debug(String.format("Error in deleting paging table <%s>: %s", tableName, e.getMessage()));
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
            return records.size() > 0 && recordPosition < records.size() || hasMoreRecords;
        }

        @Override
        public T next() {
            try {
                checkAndFetchRecords();

                if (recordPosition >= records.size())
                	throw new NoSuchElementException("No more records exist in result set.");

                T record = records.get(recordPosition++);
                return record;
            } catch (GPUdbException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
