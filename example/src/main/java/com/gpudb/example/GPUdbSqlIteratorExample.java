package com.gpudb.example;

import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.GPUdbLogger;
import com.gpudb.GPUdbSqlIterator;
import com.gpudb.GenericRecord;
import com.gpudb.Record;

public class GPUdbSqlIteratorExample {
    public static void main(String... args) throws GPUdbException {
        if (args.length != 1) {
            System.out.println("SQL statement must be given ...");
            System.exit(-1);
        }

        String sql = args[0];
        String username = null;
        String password = null;
        if (args.length == 2) {
            username = args[1];
        } else if (args.length == 3) {
            username = args[1];
            password = args[2];
        }

        // Establish a connection with a locally running instance of GPUdb
        GPUdb.Options options = new GPUdb.Options();
        options.setUsername(username == null ? "" : username);
        options.setPassword(password == null ? "" : password);
        GPUdb gpudb = new GPUdb("http://127.0.0.1:9191", options);

        tryWithResourcesUsage(gpudb, sql);
        explicitCloseCallUsage(gpudb, sql);
    }

    private static void tryWithResourcesUsage(GPUdb gpudb, String sql) {
        System.out.println("tryWithResourcesUsage ...");
        try (GPUdbSqlIterator<Record> iterator = new GPUdbSqlIterator<>(gpudb, sql);) {
            for (Record record : iterator) {
                System.out.println(record);
            }
        } catch (Exception e) {
            GPUdbLogger.debug("Error in iteration : " + e.getMessage());
        }
    }

    private static void explicitCloseCallUsage(GPUdb gpudb, String sql) throws GPUdbException {
        System.out.println("explicitCloseCallUsage ...");
        GPUdbSqlIterator<GenericRecord> iterator = new GPUdbSqlIterator<>(gpudb, sql);

        for (Record record : iterator) {
            System.out.println(record);
        }

        try {
            iterator.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
