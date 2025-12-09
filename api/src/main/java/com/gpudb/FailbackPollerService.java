package com.gpudb;

import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.gpudb.GPUdbBase.FailbackOptions;

class FailbackPollerService {

    private static final String HA_STATUS_KEY = "ha_status";
    private static final String HA_STATUS_DRAINED_KEY = "drained";
    private static final String HA_STATUS_DRAINED_VALUE_DRAINING = "draining";

    static final int DEFAULT_TERMINATION_TIMEOUT = 5;  // Value in secs
    private final GPUdbBase gpudb;
    private final URL primaryURL;
    private final long pollingIntervalMs;
    private volatile boolean isRunning = false;
    private Thread pollerThread;
    private final Object lock = new Object(); // To synchronize start/stop/restart operations

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    public FailbackPollerService(GPUdbBase gpUdbBase, FailbackOptions options) {
        this(gpUdbBase, options.getPollingInterval(), TimeUnit.SECONDS);
    }

    public FailbackPollerService(GPUdbBase gpudb, long pollingInterval, TimeUnit timeUnit) {
        this.gpudb = gpudb;
        this.primaryURL = gpudb.getPrimaryUrl();
        this.pollingIntervalMs = timeUnit.toMillis(pollingInterval);
    }

    // Start the poller thread
    public void start() {
        synchronized (this.lock) {
            if (this.isRunning) {
                GPUdbLogger.warn("Poller is already running.");
                return;
            }

            GPUdbLogger.debug_with_info("Starting the poller...");
            this.isRunning = true;
            
            // Create a daemon thread for polling
            this.pollerThread = new Thread(() -> {
                try {
                    while (this.isRunning) {
                        try {
                            GPUdbLogger.debug_with_info("Polling...");
                            if (poll()) {
                                GPUdbLogger.debug_with_info("Poll successful. Stopping the poller...");
                                resetClusterPointers();
                                stop(); // Stop if polling operation is successful
                                break;
                            }
                            // Sleep for the polling interval
                            Thread.sleep(this.pollingIntervalMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (Exception e) {
                            handleException(e);
                        }
                    }
                } catch (Exception e) {
                    GPUdbLogger.error("Error in poller thread: " + e.getMessage());
                }
            });
            
            // Set as daemon thread so it doesn't prevent JVM shutdown
            this.pollerThread.setDaemon(true);
            this.pollerThread.setName("FailbackPoller-Thread");
            this.pollerThread.start();
        }
    }

    // Method to do the actual polling
    private boolean poll() {
        boolean kineticaRunning = this.gpudb.isKineticaRunning(this.primaryURL); // Tests the availability of the primary URL
        GPUdb db = null;

        // If preliminary running test checks out, acquire a database connection
        if (kineticaRunning) {
            GPUdb.Options options = new GPUdb.Options();
            options.setDisableAutoDiscovery(true);
            options.setDisableFailover(true);
            options.setUsername(this.gpudb.getUsername());
            options.setPassword(this.gpudb.getPassword());
            options.setBypassSslCertCheck(this.gpudb.getBypassSslCertCheck());

            try {
                db = new GPUdb(this.primaryURL, options);
            } catch (Exception e) {
                GPUdbLogger.debug_with_info(String.format("Could not connect to Kinetica: %s", e.getMessage()));
                kineticaRunning = false;
            }
        } else {
            GPUdbLogger.debug_with_info(String.format("Kinetica is not running at URL: [%s]", this.primaryURL.toString()));
        }

        // If database connection is successful, check draining status
        if (kineticaRunning && db != null) {
            String statusJsonString = null;
            try {
                statusJsonString = db.showSystemStatus(new HashMap<>()).getStatusMap().get(HA_STATUS_KEY);
    
                if (HA_STATUS_DRAINED_VALUE_DRAINING.equals(JSON_MAPPER.readTree( statusJsonString ).get(HA_STATUS_DRAINED_KEY).textValue())) {
                    GPUdbLogger.debug_with_info(String.format("Kinetica running on primary cluster at [%s], but HA queues are still draining", this.primaryURL.toString()));
                    kineticaRunning = false;
                }
            } catch ( Exception e ) {
                GPUdbLogger.warn(String.format("Could not parse system status block [%s] for URL: [%s]", statusJsonString, this.primaryURL.toString()));
                kineticaRunning = false;
            }
        }

        // If draining status check is successful, issue query
        if (kineticaRunning && db != null) {
            long timeCheck = System.currentTimeMillis();
            try (GPUdbSqlIterator<Record> si = db.query("SELECT " + timeCheck)) {
                if (si.size() != 1) {
                    GPUdbLogger.debug_with_info(String.format("Kinetica running on primary cluster at [%s], but query check failed to return expected result count--instead [%d]", this.primaryURL.toString(), si.size()));
                    kineticaRunning = false;
                } else if (si.iterator().next().getLong(0) != timeCheck) {
                    GPUdbLogger.debug_with_info(String.format("Kinetica running on primary cluster at [%s], but query check failed to return expected result--instead [%d] != [%d]", this.primaryURL.toString(), si.iterator().next().getLong(0), timeCheck));
                    kineticaRunning = false;
                }
            } catch ( Exception e ) {
                GPUdbLogger.warn(String.format("Kinetica running on primary cluster at [%s], but query check failed: [%s]", this.primaryURL.toString(), e.getMessage()));
                kineticaRunning = false;
            }
        }

        if (kineticaRunning)
            GPUdbLogger.info(String.format("Failback to primary cluster at [%s] succeeded", this.primaryURL));

        return kineticaRunning;
    }

    private void resetClusterPointers() {
        this.gpudb.setCurrClusterIndexPointer(0);
    }

    // Handle specific exceptions that require warnings
    private static void handleException(Exception e) {
        GPUdbLogger.debug_with_info(String.format("Warning: Exception during polling: %s", e.getMessage()));
    }

    // Stop the poller thread
    public void stop() {
        synchronized (this.lock) {
            if (!this.isRunning) {
                GPUdbLogger.debug_with_info("Poller is already stopped.");
                return;
            }

            GPUdbLogger.debug_with_info("Stopping the poller...");
            this.isRunning = false;
            
            // Interrupt the thread to wake it up if it's sleeping
            if (this.pollerThread != null) {
                this.pollerThread.interrupt();
                
                // Give the thread some time to terminate naturally
                try {
                    this.pollerThread.join(TimeUnit.SECONDS.toMillis(DEFAULT_TERMINATION_TIMEOUT));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                this.pollerThread = null;
            }
        }
    }

    public boolean isRunning() {
        boolean running;
        synchronized (this.lock) {
            running = this.isRunning;
        }
        return running;
    }

    // Restart the poller thread after it has been stopped
    public void restart() {
        GPUdbLogger.debug_with_info("Restarting the poller...");
        stop(); // Stop the existing poller if it's running
        start(); // Start the poller again
    }
}