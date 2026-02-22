package com.gpudb.example;

import com.gpudb.*;
import com.gpudb.protocol.ShowTableRequest;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class demonstrates the usage of the BulkInserter class in various ways,
 * including:
 * 
 * <ul>
 *   <li>try-with-resources invocation</li>
 *   <li>try with explicit {@link BulkInserter#close()} call</li>
 *   <li>multi-threaded use of a single {@link BulkInserter}</li>
 *   <li>user-specified timed flush</li>
 *   <li>JSON record handling</li>
 * </ul>
 * 
 * Usage:
 *         java -cp "<jar>" -Durl="<url>" -Duser="<username>" -Dpass="<password>" com.gpudb.example.BulkInserterExample
 *         
 * Options:
 * 
 *         -Durl="<url>"       (Kinetica connection URL, default is"http://127.0.0.1:9191")
 *         -DlogLevel=<level>  (Output logging level, default is "INFO")
 */
public class BulkInserterExample {

    /**
     * Kicks off all bulk ingest examples with the given connection parameters
     * 
     * @param args  None; specified as system properties instead
     * @throws GPUdbException  if something goes awry
     */
    public static void main( String ...args ) throws Exception {

        String url = System.getProperty("url", "http://127.0.0.1:9191");
        String user = System.getProperty("user", "");
        String pass = System.getProperty("pass", "");
        String logLevel = System.getProperty("logLevel", "INFO");

        GPUdb.Options options = new GPUdb.Options();
        options.setUsername(user);
        options.setPassword(pass);
        options.setBypassSslCertCheck(true);
        GPUdbLogger.setLoggingLevel(logLevel);

        // Establish a connection with a locally running instance of GPUdb
        GPUdb gpudb = new GPUdb( url, options );


        bulkInserterWithTryWithResources( gpudb );

        bulkInserterWithExplicitCloseCall( gpudb );

        bulkInserterWithTimedFlush(gpudb);

        bulkInserterMultiThreaded(gpudb);

        bulkInserterJson(gpudb);
    }


    public static void bulkInserterWithTryWithResources(GPUdb gpudb ) throws Exception {

        Pair<String, Type> tableNameTypePair = setUp(gpudb, false);

        String tableName = tableNameTypePair.getLeft();
        Type tableDefinition = tableNameTypePair.getRight();

        // Try to insert enough records such that at least one batch
        // is pushed automatically, and then we have some leftovers to flush
        int numRecords = 1_000_000;

        try(
                BulkInserter<GenericRecord> bi = new BulkInserter<>(
                        gpudb,
                        tableName,
                        tableDefinition,
                        25_000,
                        null,
                        new WorkerList(gpudb)
                )
        ) {
            for (int x = 0; x < numRecords; ++x) {
                GenericRecord gr = new GenericRecord(tableDefinition);
                gr.put(0, x);
                gr.put(1, -x);
                bi.insert(gr);
            }
        } // end of try-with-resources block; pending inserts will be flushed automatically

        Map<String,String> stOptions = GPUdbBase.options( ShowTableRequest.Options.GET_SIZES, ShowTableRequest.Options.TRUE );
        Long tableRecordCount = gpudb.showTable(tableName, stOptions).getFullSizes().get( 0 );
        if ( tableRecordCount != numRecords ) {
            System.err.println(
                    "Failed to insert <" + numRecords + "> records into table <" + tableName + ">; " +
                    "got <" + tableRecordCount + "> instead"
            );
        } else {
            System.out.println(
                    "Successfully inserted <" + numRecords + "> records into table <" + tableName + ">"
            );
        }
    }


    public static void bulkInserterWithExplicitCloseCall(GPUdb gpudb ) throws Exception {

        Pair<String, Type> tableNameTypePair = setUp(gpudb, true);

        String tableName = tableNameTypePair.getLeft();
        Type tableDefinition = tableNameTypePair.getRight();

        // Try to insert enough records such that at least one batch
        // is pushed automatically and then we have some leftovers to flush
        int numRecords = 10_000;

        BulkInserter<GenericRecord> bi = null;

        try {
            bi = new BulkInserter<>(
                    gpudb,
                    tableName,
                    tableDefinition,
                    500,
                    null,
                    new WorkerList(gpudb)
            );

            for (int x = 0; x < numRecords; ++x) {
                GenericRecord gr = new GenericRecord(tableDefinition);
                gr.put(0, x);
                gr.put(1, -x);
                bi.insert(gr);
            }
        } catch (BulkInserter.InsertException ie ) {
            System.err.println("Error in inserting records: " + ie);
        } finally {
            // Call close explicitly; will automatically flush pending updates
            if (bi != null)
                bi.close();
        }


        Map<String,String> stOptions = GPUdbBase.options( ShowTableRequest.Options.GET_SIZES, ShowTableRequest.Options.TRUE );
        Long tableRecordCount = gpudb.showTable(tableName, stOptions).getFullSizes().get( 0 );
        if ( tableRecordCount != numRecords ) {
            System.err.println(
                    "Failed to insert <" + numRecords + "> records into table <" + tableName + ">; " +
                    "got <" + tableRecordCount + "> instead"
            );
        } else {
            System.out.println(
                    "Successfully inserted <" + numRecords + "> records into table <" + tableName + ">"
            );
        }
    }


    /**
     * Method to demonstrate insertion using {@link BulkInserter} from multiple threads
     * This method inserts into the same table but the same pattern can be used for
     * multiple tables as well. The code {@code ExecutorService executors = Executors.newFixedThreadPool(20);}
     * may be dropped and the {@code CompletableFuture.supplyAsync} call could be used without the 'executors'
     * parameter, in that case it will just work off the built-in {@link java.util.concurrent.ForkJoinPool}
     *
     * @param gpudb - a {@link GPUdb} instance
     * @throws Exception
     */
    public static void bulkInserterMultiThreaded(GPUdb gpudb ) throws Exception {

        Pair<String, Type> tableNameTypePair = setUp(gpudb, true);

        String tableName = tableNameTypePair.getLeft();
        Type tableDefinition = tableNameTypePair.getRight();

        int numRecords = 25_000_000;

        // Generate a simple list of record numbers
        List<Integer> integerList = IntStream.range(0, numRecords)
              .boxed()
              .collect(Collectors.toList());

        int listSize = integerList.size();
        int chunkSize = numRecords / 10;

        // Break the list of record numbers in chunks
        List<List<Integer>> listOfIntegerLists = IntStream.range(0, (listSize-1)/chunkSize+1)
             .mapToObj(i -> integerList.subList(i *= chunkSize,
                                       listSize-chunkSize >= i ? i + chunkSize : listSize))
             .collect(Collectors.toList());

        ExecutorService executors = Executors.newFixedThreadPool(20);
        List<Pair<CompletableFuture<String>, Integer>> futuresList = new ArrayList<>();

        listOfIntegerLists.forEach( list -> {
            CompletableFuture<String> errorInInsertingRecords = CompletableFuture.supplyAsync(() -> {
                String result = null;
                System.out.println(String.format("Thread ID = %s", Thread.currentThread().getName()));

                int nRecords = list.size();

                try (BulkInserter<GenericRecord> bi = new BulkInserter<>(
                        gpudb,
                        tableName,
                        tableDefinition,
                        20_000,
                        null,
                        new WorkerList(gpudb)
                )) {
                    for (int x = 0; x < nRecords; ++x) {
                        GenericRecord gr = new GenericRecord(tableDefinition);
                        gr.put(0, x);
                        gr.put(1, -x);
                        bi.insert(gr);
                    }
                } catch (BulkInserter.InsertException ie) {
                    System.err.println("Error in inserting records: " + ie);
                    result = ie.getMessage();
                } catch (GPUdbException e) {
                    System.err.println("Error creating BulkInserter: " + e);
                    result = e.getMessage();
                }
                return result;
            }, executors); //end supplyAsync

            futuresList.add( Pair.of( errorInInsertingRecords, list.size() ) );
        });

        // All threads have started, we wait for the results now
        futuresList.forEach( errorInInsertingRecords -> {
            try {
                String errorInInsertion = errorInInsertingRecords.getLeft().get();
                if( errorInInsertion == null) {
                    System.out.println(String.format("Successfully inserted %d records ", errorInInsertingRecords.getRight()));
                } else {
                    System.err.println("Got some error in current batch execution : " + errorInInsertion);
                }
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Error in getting insertion results: " + e);
            }
        });

        executors.shutdown();


        Map<String,String> stOptions = GPUdbBase.options( ShowTableRequest.Options.GET_SIZES, ShowTableRequest.Options.TRUE );
        Long tableRecordCount = gpudb.showTable(tableName, stOptions).getFullSizes().get( 0 );
        if ( tableRecordCount != numRecords ) {
            System.err.println(
                    "Failed to insert <" + numRecords + "> records into table <" + tableName + ">; " +
                    "got <" + tableRecordCount + "> instead"
            );
        } else {
            System.out.println(
                    "Successfully inserted <" + numRecords + "> records into table <" + tableName + ">"
            );
        }
    }


    public static void bulkInserterWithTimedFlush(GPUdb gpudb ) throws Exception {

        Pair<String, Type> tableNameTypePair = setUp(gpudb, true);

        String tableName = tableNameTypePair.getLeft();
        Type tableDefinition = tableNameTypePair.getRight();

        // Try to insert enough records such that at least one batch
        // is pushed automatically and then we have some leftovers to flush
        int numRecords = 10_000_000;

        BulkInserter.FlushOptions flushOptions = new BulkInserter.FlushOptions(false, 1);

        try (
                BulkInserter<GenericRecord> bi = new BulkInserter<>(
                        gpudb,
                        tableName,
                        tableDefinition,
                        20_000,
                        null,
                        null,
                        flushOptions
                )
        ) {
            for (int x = 0; x < numRecords; ++x) {
                GenericRecord gr = new GenericRecord(tableDefinition);
                gr.put(0, x);
                gr.put(1, -x);
                bi.insert(gr);
            }
        } catch (BulkInserter.InsertException ie) {
        	System.err.println("Error in inserting records: " + ie);
        }


        Map<String,String> stOptions = GPUdbBase.options( ShowTableRequest.Options.GET_SIZES, ShowTableRequest.Options.TRUE );
        Long tableRecordCount = gpudb.showTable(tableName, stOptions).getFullSizes().get( 0 );
        if ( tableRecordCount != numRecords ) {
            System.err.println(
                "Failed to insert <" + numRecords + "> records into table <" + tableName + ">; " +
                    "got <" + tableRecordCount + "> instead"
            );
        } else {
            System.out.println(
                "Successfully inserted <" + numRecords + "> records into table <" + tableName + ">"
            );
        }
    }


    public static void bulkInserterJson(GPUdb gpudb ) throws Exception {

        Pair<String, Type> tableNameTypePair = setUp(gpudb, false);

        String tableName = tableNameTypePair.getLeft();

        // Try to insert enough records such that at least one batch
        // is pushed automatically, and then we have some leftovers to flush
        int numRecords = 1_000_000;

        try (BulkInserter<String> bi = new BulkInserter<>(gpudb, tableName, 25_000)) {
            for (int x = 0; x < numRecords; ++x) {
                JSONObject jsonRecord = new JSONObject();
                jsonRecord.put("x", x);
                jsonRecord.put("y", -x);
                bi.insert(jsonRecord.toString());
            }
        } // end of try-with-resources block; pending inserts will be flushed automatically


        Map<String,String> stOptions = GPUdbBase.options( ShowTableRequest.Options.GET_SIZES, ShowTableRequest.Options.TRUE );
        Long tableRecordCount = gpudb.showTable(tableName, stOptions).getFullSizes().get( 0 );
        if ( tableRecordCount != numRecords ) {
        	System.err.println(
                    "Failed to insert <" + numRecords + "> records into table <" + tableName + ">; " +
                    "got <" + tableRecordCount + "> instead"
            );
        } else {
        	System.out.println(
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
                    new Type.Column( "y", Integer.class )
            );
        } else {
            // No sharding for replicated tables
            tableDefinition = new Type(
                    new Type.Column( "x", Integer.class ),
                    new Type.Column( "y", Integer.class )
            );
        }

        // Create the table with the given type

        gpudb.clearTable(tableName, null, GPUdbBase.options("no_error_if_not_exists", "true"));

        System.out.println("Creating " + tableType + " table <" + tableName + ">" );
        gpudb.createTable( tableName, tableDefinition.create(gpudb), null );

        return Pair.of( tableName, tableDefinition );
    }

    public static void clearTable( GPUdb gpudb, String tableName ) throws GPUdbException {
        gpudb.clearTable( tableName, null, GPUdbBase.options("no_error_if_not_exists", "true") );
    }
}
