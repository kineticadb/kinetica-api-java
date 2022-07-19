package com.gpudb.example;

import com.gpudb.*;
import com.gpudb.protocol.ClearTableRequest;
import com.gpudb.protocol.ShowTableRequest;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This class demonstrates the usage of the BulkInserter class in a
 * try-with-resources block and with an explicit call to 'close()' method.
 */
public class BulkInserterExample {

    /**
     * main method
     * @param args - None
     * @throws GPUdbException
     */
    public static void main( String ...args ) throws Exception {

//        String url = System.getProperty("url", "http://127.0.0.1:9191");
        String url = System.getProperty("url", "http://172.17.0.2:9191");

        // Get the log level from the command line, if any
        GPUdb.Options options = new GPUdb.Options();
        String logLevel = System.getProperty("logLevel", "DEBUG");
        if ( !logLevel.isEmpty() ) {
            System.out.println( "Log level given by the user: " + logLevel );
            GPUdbLogger.setLoggingLevel(logLevel);
        } else {
            System.out.println( "No log level given by the user." );
        }

        // Establish a connection with a locally running instance of GPUdb
        GPUdb gpudb = new GPUdb( url, options );

        bulkInserterWithTryWithResources( gpudb );

//        bulkInserterWithExplicitCloseCall( gpudb );

    }

    public static void bulkInserterWithTryWithResources(GPUdb gpudb ) throws Exception {

        Pair<String, Type> tableNameTypePair = setUp(gpudb, true);

        String tableName = tableNameTypePair.getLeft();
        Type type_ = tableNameTypePair.getRight();

        int numRecords = 10000;

        try( BulkInserter<GenericRecord> bi = new BulkInserter<>( gpudb, tableName, type_,
                100, null,
                new WorkerList(gpudb) ); ) {

            // Try to insert enough records such that at least one batch
            // is pushed automatically, and then we have some leftovers to flush
            for (int x = 0; x < numRecords; ++x) {
                GenericRecord gr = new GenericRecord(type_);
                gr.put(0, x);
                gr.put(1, x);
                bi.insert(gr);
            }

        } // end of try-with-resources block; pending inserts will be flushed automatically

        Long table_size = gpudb.showTable( tableName,
                        GPUdb.options( ShowTableRequest.Options.GET_SIZES,
                                ShowTableRequest.Options.TRUE ) )
                .getFullSizes().get( 0 );
        if ( table_size != numRecords ) {
            GPUdbLogger.error( "Table '' does not have the correct number of "
                    + "records; expected " + numRecords + ", got "
                    + table_size );
            throw new Exception( "Table '' does not have the correct number "
                    + "of records; expected " + numRecords
                    + ", got " + table_size );
        } else {
            GPUdbLogger.debug_with_info( "Number of records in table " + table_size );
            clearTable( gpudb, tableName );
        }

    }

    public static void bulkInserterWithExplicitCloseCall(GPUdb gpudb ) throws Exception {

        Pair<String, Type> tableNameTypePair = setUp(gpudb, true);

        String tableName = tableNameTypePair.getLeft();
        Type type_ = tableNameTypePair.getRight();

        int numRecords = 10000;

        BulkInserter<GenericRecord> bi = new BulkInserter<>( gpudb, tableName, type_,
                100, null,
                new WorkerList(gpudb) );

        // Try to insert enough records such that at least one batch
        // is pushed automatically and then we have some leftovers to flush
        for (int x = 0; x < numRecords; ++x) {
            GenericRecord gr = new GenericRecord(type_);
            gr.put(0, x);
            gr.put(1, x);
            bi.insert(gr);
        }

        // Call close explicitly; will automatically flush pending updates
        bi.close();


        Long table_size = gpudb.showTable( tableName,
                        GPUdb.options( ShowTableRequest.Options.GET_SIZES,
                                ShowTableRequest.Options.TRUE ) )
                .getFullSizes().get( 0 );
        if ( table_size != numRecords ) {
            GPUdbLogger.error( "Table '' does not have the correct number of "
                    + "records; expected " + numRecords + ", got "
                    + table_size );
            throw new Exception( "Table '' does not have the correct number "
                    + "of records; expected " + numRecords
                    + ", got " + table_size );
        } else {
            clearTable( gpudb, tableName );
        }

    }

    public static Pair<String, Type> setUp(GPUdb gpudb, boolean useReplicatedTable) throws GPUdbException
    {
        String tableName = "Bi_example_" + UUID.randomUUID();

        Type type_;
        // Create a table with two simple int columns
        if ( !useReplicatedTable ) {
            GPUdbLogger.info("Creating regular table (not replicated)" );
            // Need sharding for regular tables
            type_ = new Type(
                    new Type.Column( "x", Integer.class,
                            ColumnProperty.SHARD_KEY ),
                    new Type.Column( "int", Integer.class )
            );
        } else {
            GPUdbLogger.info("Creating replicated table" );
            // No sharding for replicated tables
            type_ = new Type(
                    new Type.Column( "x", Integer.class ),
                    new Type.Column( "int", Integer.class )
            );
        }

        // Create the table with the necessary options--replicated if the
        // user wants it to be so
        GPUdbLogger.info("Creating table <TABLE NAME: " + tableName + ">" );
        Map<String, String> m_createTableOptions = new HashMap<>();
        gpudb.createTable( tableName, type_.create(gpudb), m_createTableOptions );

        return Pair.of( tableName, type_ );
    }

    public static void clearTable( GPUdb gpudb, String tableName ) {
        try {
            ClearTableRequest request = new ClearTableRequest();
            request.setTableName( tableName );
            Map<String, String> m_clearTableOptions = new HashMap<>();
            request.setOptions( m_clearTableOptions );
            gpudb.clearTable( request );
        } catch ( Exception ex ) {
            // Not doing anything about it
        }
    }


}
