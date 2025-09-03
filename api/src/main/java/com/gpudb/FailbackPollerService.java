package com.gpudb;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.gpudb.GPUdbBase.FailbackOptions;
import com.gpudb.protocol.InsertRecordsRandomRequest;
import com.gpudb.protocol.ShowSystemStatusResponse;

class FailbackPollerService {

    static final int DEFAULT_TERMINATION_TIMEOUT = 5;  // Value in secs
    private final GPUdbBase gpudb;
    private final URL primaryURL;
    private final long pollingIntervalMs;
    private volatile boolean isRunning = false;
    private Thread pollerThread;
    private final Object lock = new Object(); // To synchronize start/stop/restart operations
	private static final InsertRecordsRandomRequest POLL_REQUEST = new InsertRecordsRandomRequest().setTableName("").setCount(1);

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

        if (kineticaRunning) {
            GPUdb.Options options = new GPUdb.Options();
            options.setDisableAutoDiscovery(true);
            options.setDisableFailover(true);
            options.setUsername(this.gpudb.getUsername());
            options.setPassword(this.gpudb.getPassword());
            options.setBypassSslCertCheck(this.gpudb.getBypassSslCertCheck());

            try {
                GPUdb db = new GPUdb(this.primaryURL, options);
                Map<String, String> statusMap = db.showSystemStatus(new HashMap<>()).getStatusMap();
                if (statusMap.containsKey("ha_status")) {
                    String haStatus = statusMap.get("ha_status");
                    if( haStatus.equals("drained")) {
                        GPUdbLogger.info(String.format("Failback to primary cluster at [%s] succeeded", this.primaryURL));
                    } else {
                        GPUdbLogger.debug_with_info(String.format("Kinetica running on primary cluster at [%s], but connection did not succeed", this.primaryURL.toString()));
                        kineticaRunning = false;
                    }
                }
            } catch (GPUdbException e) {
                GPUdbLogger.debug_with_info(String.format("Could not connect to Kinetica %s", e.getMessage()));
                kineticaRunning = false;
            }
        } else {
            GPUdbLogger.debug_with_info(String.format("Kinetica is not running at : %s", this.primaryURL.toString()));
        }
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