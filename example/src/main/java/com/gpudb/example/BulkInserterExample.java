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
 * 
 * Usage:
 *         java -cp <JAR> -Duser=<username> -Dpass=<password> com.gpudb.example.BulkInserterExample
 *         
 * Other options:
 * 
 *         -Durl=<URL>         (Kinetica connection URL, default is"http://127.0.0.1:9191")
 *         -DlogLevel=<level>  (Output logging level, default is "INFO")
 */
public class BulkInserterExample {

    /**
     * main method
     * @param args - None
     * @throws GPUdbException
     */
    public static void main( String ...args ) throws Exception {

        String url = System.getProperty("url", "http://127.0.0.1:9191");
        String user = System.getProperty("user", "");
        String pass = System.getProperty("pass", "");
        String logLevel = System.getProperty("logLevel", "INFO");

        GPUdb.Options options = new GPUdb.Options();
        options.setUsername(user);
        options.setPassword(pass);
        GPUdbLogger.setLoggingLevel(logLevel);

        // Establish a connection with a locally running instance of GPUdb
        GPUdb gpudb = new GPUdb( url, options );

        bulkInserterWithTryWithResources( gpudb );

        bulkInserterWithExplicitCloseCall( gpudb );

        bulkInserterWithExplicitCloseCallWithTimedFlush(gpudb);

    }

    public static void bulkInserterWithTryWithResources(GPUdb gpudb ) throws Exception {

        Pair<String, Type> tableNameTypePair = setUp(gpudb, false);

        String tableName = tableNameTypePair.getLeft();
        Type tableDefinition = tableNameTypePair.getRight();

        int numRecords = 1000000;

        try(
                BulkInserter<GenericRecord> bi = new BulkInserter<>(
                        gpudb,
                        tableName,
                        tableDefinition,
                        25000,
                        null,
                        new WorkerList(gpudb)
                )
        ) {
            // Try to insert enough records such that at least one batch
            // is pushed automatically, and then we have some leftovers to flush
            for (int x = 0; x < numRecords; ++x) {
                GenericRecord gr = new GenericRecord(tableDefinition);
                gr.put(0, x);
                gr.put(1, x);
                bi.insert(gr);
            }

        } // end of try-with-resources block; pending inserts will be flushed automatically

        Map<String,String> stOptions = GPUdb.options( ShowTableRequest.Options.GET_SIZES, ShowTableRequest.Options.TRUE );
        Long tableRecordCount = gpudb.showTable(tableName, stOptions).getFullSizes().get( 0 );
        if ( tableRecordCount != numRecords ) {
            GPUdbLogger.error(
                    "Failed to insert <" + numRecords + "> records into table <" + tableName + ">; " +
                    "got <" + tableRecordCount + "> instead"
            );
        } else {
            GPUdbLogger.info(
                    "Successfully inserted <" + numRecords + "> records into table <" + tableName + ">"
            );
        }

    }

    public static void bulkInserterWithExplicitCloseCall(GPUdb gpudb ) throws Exception {

        Pair<String, Type> tableNameTypePair = setUp(gpudb, true);

        String tableName = tableNameTypePair.getLeft();
        Type tableDefinition = tableNameTypePair.getRight();

        int numRecords = 10000;

        BulkInserter<GenericRecord> bi = new BulkInserter<>(
                gpudb,
                tableName,
                tableDefinition,
                500,
                null,
                new WorkerList(gpudb)
        );

        // Try to insert enough records such that at least one batch
        // is pushed automatically and then we have some leftovers to flush
        for (int x = 0; x < numRecords; ++x) {
            GenericRecord gr = new GenericRecord(tableDefinition);
            gr.put(0, x);
            gr.put(1, x);
            bi.insert(gr);
        }

        // Call close explicitly; will automatically flush pending updates
        bi.close();


        Map<String,String> stOptions = GPUdb.options( ShowTableRequest.Options.GET_SIZES, ShowTableRequest.Options.TRUE );
        Long tableRecordCount = gpudb.showTable(tableName, stOptions).getFullSizes().get( 0 );
        if ( tableRecordCount != numRecords ) {
            GPUdbLogger.error(
                    "Failed to insert <" + numRecords + "> records into table <" + tableName + ">; " +
                    "got <" + tableRecordCount + "> instead"
            );
        } else {
            GPUdbLogger.info(
                    "Successfully inserted <" + numRecords + "> records into table <" + tableName + ">"
            );
        }

    }

    public static void bulkInserterWithExplicitCloseCallWithTimedFlush(GPUdb gpudb ) throws Exception {

        Pair<String, Type> tableNameTypePair = setUp(gpudb, true);

        String tableName = tableNameTypePair.getLeft();
        Type tableDefinition = tableNameTypePair.getRight();

        int numRecords = 10_000_000;

        BulkInserter.FlushOptions flushOptions = new BulkInserter.FlushOptions(false, 1);

        BulkInserter<GenericRecord> bi = new BulkInserter<>(
            gpudb,
            tableName,
            tableDefinition,
            20000,
            null,
            new WorkerList(gpudb),
            flushOptions
        );

        // Try to insert enough records such that at least one batch
        // is pushed automatically and then we have some leftovers to flush
        for (int x = 0; x < numRecords; ++x) {
            GenericRecord gr = new GenericRecord(tableDefinition);
            gr.put(0, x);
            gr.put(1, x);
            bi.insert(gr);
        }

        // Call close explicitly; will automatically flush pending updates
        bi.close();


        Map<String,String> stOptions = GPUdb.options( ShowTableRequest.Options.GET_SIZES, ShowTableRequest.Options.TRUE );
        Long tableRecordCount = gpudb.showTable(tableName, stOptions).getFullSizes().get( 0 );
        if ( tableRecordCount != numRecords ) {
            GPUdbLogger.error(
                "Failed to insert <" + numRecords + "> records into table <" + tableName + ">; " +
                    "got <" + tableRecordCount + "> instead"
            );
        } else {
            GPUdbLogger.info(
                "Successfully inserted <" + numRecords + "> records into table <" + tableName + ">"
            );
        }

    }

    public static Pair<String, Type> setUp(GPUdb gpudb, boolean useReplicatedTable) throws GPUdbException
    {
        String tableType = useReplicatedTable ? "replicated" : "sharded";
        String tableName = "bi_example_" + tableType;

        Type tableDefinition;
        // Create a table with two simple int columns
        if ( !useReplicatedTable ) {
            // Need sharding for regular tables
            tableDefinition = new Type(
                    new Type.Column( "x", Integer.class, ColumnProperty.SHARD_KEY ),
                    new Type.Column( "int", Integer.class )
            );
        } else {
            // No sharding for replicated tables
            tableDefinition = new Type(
                    new Type.Column( "x", Integer.class ),
                    new Type.Column( "int", Integer.class )
            );
        }

        // Create the table with the given type

        if (gpudb.hasTable(tableName, null).getTableExists()) {
            GPUdbLogger.info("Removing pre-existing table <" + tableName + ">" );
        	gpudb.clearTable(tableName, null, null);
        }

        GPUdbLogger.info("Creating " + tableType + " table <" + tableName + ">" );
        gpudb.createTable( tableName, tableDefinition.create(gpudb), null );

        return Pair.of( tableName, tableDefinition );
    }

    public static void clearTable( GPUdb gpudb, String tableName ) {
        try {
            gpudb.clearTable( tableName, null, null );
        } catch ( Exception ex ) {
            // Not doing anything about it
        }
    }


}
