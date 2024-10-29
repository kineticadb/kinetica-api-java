package com.gpudb;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.gpudb.GPUdbBase.FailbackOptions;
import com.gpudb.protocol.ShowSystemPropertiesResponse;

public class FailbackPollerService {

    static final int DEFAULT_START_DELAY = 0, DEFAULT_TERMINATION_TIMEOUT = 5;  // Both values are in secs
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> futureTask;
    private final GPUdbBase gpudb;
    private final URL primaryURL;
    private final long pollingInterval;
    private final TimeUnit timeUnit;
    private volatile boolean isRunning = false;
    private final Object lock = new Object(); // To synchronize start/stop/restart operations

    public FailbackPollerService(GPUdbBase gpUdbBase, FailbackOptions options) {
        this(gpUdbBase, options.getPollingInterval(), TimeUnit.SECONDS);
    }

    public FailbackPollerService(GPUdbBase gpudb, long pollingInterval, TimeUnit timeUnit) {
        this.gpudb = gpudb;
        this.primaryURL = gpudb.getPrimaryUrl();
        this.pollingInterval = pollingInterval;
        this.timeUnit = timeUnit;
    }

    // Start the poller thread
    public void start() {
        synchronized (lock) {
            if (isRunning) {
                GPUdbLogger.warn("Poller is already running.");
                return;
            }

            GPUdbLogger.debug_with_info("Starting the poller...");
            scheduler = Executors.newSingleThreadScheduledExecutor();
            isRunning = true;

            // Schedule the poller task to run at fixed intervals
            futureTask = scheduler.scheduleWithFixedDelay(this::pollingTask, DEFAULT_START_DELAY, pollingInterval, timeUnit);
        }
    }

    // Polling task to be executed at fixed intervals
    private void pollingTask() {
        try {
            GPUdbLogger.debug_with_info("Polling...");
            if (poll()) {
                GPUdbLogger.debug_with_info("Poll successful. Stopping the poller...");
                resetClusterPointers();
                stop(); // Stop if polling operation is successful
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    // Method to do the actual polling
    private boolean poll() {
        boolean kineticaRunning = gpudb.isKineticaRunning(primaryURL); // Tests the availability of the primary URL

        if( kineticaRunning ) {

            GPUdb.Options options = new GPUdb.Options();
            options.setDisableAutoDiscovery(true);
            options.setDisableFailover(true);
            options.setUsername(gpudb.getUsername());
            options.setPassword(gpudb.getPassword());

            GPUdb db;

            try {
                db = new GPUdb(gpudb.getURL(), options);
                GPUdbLogger.debug_with_info(String.format("Failback to primary cluster [%s] succeeded", primaryURL.toString()));
            } catch (GPUdbException e) {
                GPUdbLogger.warn(String.format("Failback to primary cluster at [%s] did not succeed", primaryURL.toString()));
            }

        } else {
            GPUdbLogger.debug_with_info(String.format("Kinetica is not running at : %s", primaryURL.toString()));
        }
        return kineticaRunning;
    }


    private void resetClusterPointers() {
        List<GPUdbBase.ClusterAddressInfo> hostAddresses = gpudb.getHostAddresses();
        // ToDo - Should we check for (&& x.getActiveHeadNodeUrl().toExternalForm().equals(primaryURL.toExternalForm()) as well ?
        int indexOfPrimaryCluster = hostAddresses.stream()
                .filter(x -> x.isPrimaryCluster() )
                .findFirst()
                .map(hostAddresses::indexOf)
                .orElse(-1);

        if( indexOfPrimaryCluster != -1) {
            GPUdbLogger.debug_with_info(String.format("Failing back to cluster at index %d with URL %s", 
            indexOfPrimaryCluster, hostAddresses.get(indexOfPrimaryCluster).getActiveHeadNodeUrl()));
            gpudb.setCurrClusterIndexPointer(indexOfPrimaryCluster);
        } else {
            GPUdbLogger.error(String.format("Primary cluster could not be located for URL : %s", primaryURL));
        }
    }


    // Handle specific exceptions that require warnings
    private void handleException(Exception e) {
        GPUdbLogger.debug_with_info(String.format("Warning: Exception during polling: %s", e.getMessage()));
    }

    // Stop the poller thread
    public void stop() {
        synchronized (lock) {
            if (!isRunning) {
                GPUdbLogger.debug_with_info("Poller is already stopped.");
                return;
            }

            GPUdbLogger.debug_with_info("Stopping the poller...");
            if (futureTask != null) {
                futureTask.cancel(true); // Cancel the scheduled task
            }
            shutdownScheduler();
            isRunning = false;
        }
    }

    public boolean isRunning() {
        boolean running;
        synchronized(lock) {
            running = isRunning;
        }

        return running;
    }

    // Gracefully shut down the internal scheduler
    private void shutdownScheduler() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(DEFAULT_TERMINATION_TIMEOUT, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow(); // Force shutdown if not terminated after waiting
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // Restart the poller thread after it has been stopped
    public void restart() {
        GPUdbLogger.debug_with_info("Restarting the poller...");
        stop(); // Stop the existing poller if it's running
        start(); // Start the poller again
    }

}

