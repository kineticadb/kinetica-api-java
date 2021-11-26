package com.gpudb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GPUdbLogger {

    // The name of the logger for this API
    protected static final String API_LOGGER_NAME = "com.gpudb";

    // The loggers used for dependent libraries that might have obnoxious
    // default log levels
    protected static final String DEP_LIB_APACHE_CLIENT_LOGGER = "org.apache.http";

    // Actual logger used for the API
    private static final Logger LOG = LoggerFactory.getLogger(API_LOGGER_NAME);

    public static boolean isInfoEnabled() {
        return LOG.isInfoEnabled();
    }

    public static void info(String message) {
        LOG.info(message);
    }

    public static void error(String message) {
        LOG.error(message);
    }

    public static void warn(String message) {
        LOG.warn(message);
    }

    public static void debug(String message) {
        LOG.debug(message);
    }

    public static void trace(String message) {
        LOG.trace(message);
    }

    /**
     * Print extra information with the debug message.
     */
    public static void debug_with_info(String message) {
        if (LOG.isTraceEnabled() || LOG.isDebugEnabled()) {
            // We want the calling method and class name and the line number
            StackTraceElement callingPoint = Thread.currentThread().getStackTrace()[2];
            debug("[" + callingPoint + "] " + message);
        } else {
            debug(message);
        }
    }


    /**
     * Print extra information with the trace message.
     */
    public static void trace_with_info(String message) {
        if (LOG.isTraceEnabled()) {
            // We want the calling method and class name and the line number
            StackTraceElement callingPoint = Thread.currentThread().getStackTrace()[2];
            trace("[" + callingPoint + "] " + message);
        } else {
            trace(message);
        }
    }

}  // end class GPUdbLogger
