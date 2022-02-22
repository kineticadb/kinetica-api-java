package com.gpudb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.MessageFormat;


public class GPUdbLogger {

    // The name of the logger for this API
    protected static final String API_LOGGER_NAME = "com.gpudb";

    // The loggers used for dependent libraries that might have verbose
    // default log levels
    protected static final String DEP_LIB_APACHE_CLIENT_LOGGER = "org.apache.http";

    // Actual logger used for the API
    private static Logger LOGGER = LoggerFactory.getLogger(API_LOGGER_NAME);


    /**
     * Initializes the default logback logger with the value of the
     * 'logging.level.com.gpudb' system property.
     * Log level can be set when executing a jar with a '-Dlogging.level.com.gpudb=DEBUG' arg.
     */
    public static void initializeLogger() {

        String logLevel = System.getProperty("logging.level.com.gpudb");

        if ((logLevel != null) && !logLevel.isEmpty()) {
            setLoggingLevel(logLevel);
        }

    }   // end initializeLogger


    public static void info(String message) {
        LOGGER.info( message );
    }

    public static void error(String message) {
        LOGGER.error( message );
    }

    public static void warn(String message) {
        LOGGER.warn( message );
    }

    public static void debug(String message) {
        LOGGER.debug( message );
    }


    public static void trace(String message) {
        LOGGER.trace( message );
    }


    /**
     * Print extra information with the debug message.
     */
    public static void debug_with_info(String message) {
        if ( LOGGER.isDebugEnabled() || LOGGER.isTraceEnabled() ) {
            // Getting the line number is expensive, so only do this
            // if the appropriate log level is chosen
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

            // We want the calling method and class name and the line number
            StackTraceElement callingPoint = stackTrace[ 2 ];

            // Build the message
            StringBuilder builder = new StringBuilder();
            builder.append( "[" );
            builder.append( callingPoint.toString() );
            builder.append( "] " );
            builder.append( message );

            // Finally, log the debug message
            LOGGER.debug( builder.toString() );
        } else {
            // Nothing fancy to calculate if the log level is not debug
            LOGGER.debug( message );
        }
    }


    /**
     * Print extra information with the trace message.
     */
    public static void trace_with_info(String message) {
        if ( LOGGER.isTraceEnabled() ) {
            // Getting the line number is expensive, so only do this
            // if the appropriate log level is chosen
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

            // We want the calling method and class name and the line number
            StackTraceElement callingPoint = stackTrace[ 2 ];

            // Build the message
            StringBuilder builder = new StringBuilder();
            builder.append( "[" );
            builder.append( callingPoint.toString() );
            builder.append( "] " );
            builder.append( message );

            // Finally, log the debug message
            LOGGER.trace( builder.toString() );
        } else {
            // Nothing fancy to calculate if the log level is not debug
            LOGGER.trace( message );
        }
    }

    /**
     * Dynamically set the default 'logback' logger 'com.gpudb' log level.
     * Does nothing and logs warning if 'logback' is not the slf4f logger implementation, eg. log4j.
     *
     * @param logLevel   One of the supported log levels: TRACE, DEBUG, INFO,
     *                   WARN, ERROR, FATAL, OFF. {@code null} value is considered as 'OFF'.
     */
    public static boolean setLoggingLevel(String logLevel)
    {
        // Dynamically call this function so we don't require any logback imports.
        // ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("com.gpudb");
        // logger.setLevel(level);

        String logLevelUpper = (logLevel == null) ? "OFF" : logLevel.toUpperCase();

        try
        {
            Package logbackPackage = Package.getPackage("ch.qos.logback.classic");
            if (logbackPackage == null)
            {
                LOGGER.warn("logback is not in the classpath, ignoring GPUdbLogger::setLoggingLevel("+logLevel+"). " +
                            "The 'com.gpudb' log level must be set with the current slf4j logger implementation.");
                return false;
            }

            Class<?> logLevelClass = Class.forName("ch.qos.logback.classic.Level");
            Field    logLevelField = logLevelClass.getField(logLevelUpper);
            Object   logLevelObj = logLevelField.get(null);

            if (logLevelObj == null)
            {
                LOGGER.error("No such ch.qos.logback.classic log level: '{}', ignoring", logLevelUpper);
                return false;
            }

            Class<?>[] paramTypes = { logLevelObj.getClass() };
            Object[]   params = { logLevelObj };

            Class<?> loggerClass = Class.forName("ch.qos.logback.classic.Logger");
            Method   setLevelmethod = loggerClass.getMethod("setLevel", paramTypes);
            Logger   logger = LoggerFactory.getLogger(API_LOGGER_NAME);
            setLevelmethod.invoke(logger, params);

            LOGGER.debug("Log level set to '{}' for the logger '{}'", logLevelUpper, API_LOGGER_NAME);
            return true;
        }
        catch (Exception e)
        {
            LOGGER.warn("Couldn't set log level to '{}' for the logger '{}'", logLevelUpper, API_LOGGER_NAME, e);
            return false;
        }
    }

}  // end class GPUdbLogger
