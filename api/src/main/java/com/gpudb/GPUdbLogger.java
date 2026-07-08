package com.gpudb;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;


public class GPUdbLogger {

    // The name of the logger for this API
    protected static final String API_LOGGER_NAME = "com.gpudb";

    // The loggers used for dependent libraries that might have verbose
    // default log levels
    protected static final String DEP_LIB_APACHE_CLIENT_LOGGER = "org.apache.http";

    // Actual logger used for the API
    private static Logger LOGGER = LoggerFactory.getLogger(API_LOGGER_NAME);


    /**
     * Initializes the default logger with the value of the
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
        error( null, message);
    }

    public static void error(Throwable exception, String message) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        boolean calledFromErrorMethod = stackTrace[2].getMethodName().equals("error") && stackTrace[2].getClassName().equals("com.gpudb.GPUdbLogger");

        // We want the calling method and class name and the line number
        StackTraceElement callingPoint = calledFromErrorMethod ? stackTrace[ 3 ] : stackTrace[2];

        // Build the message
        String callingPointString = String.format("[%s] %s", callingPoint.toString(), message);

        String errorMessageString;
        if( exception != null ) {
            String rootCauseMessage = String.format(" :: root cause : [ %s ] ", ExceptionUtils.getRootCauseMessage(exception));
            errorMessageString = String.format("%s%s", callingPointString, rootCauseMessage);
        } else {
            errorMessageString = callingPointString;
        }

        LOGGER.error(errorMessageString);
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


    /* Get whether debug is enabled.  Useful for avoiding the overhead of
     * building complex logging lines if debug logging is off.
     */
    public static boolean isDebugEnabled() {
        return LOGGER.isDebugEnabled();
    }

    /* Get whether trace is enabled.  Useful for avoiding the overhead of
     * building complex logging lines if trace logging is off.
     */
    public static boolean isTraceEnabled() {
        return LOGGER.isTraceEnabled();
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
            String messageString = String.format("[%s] %s", callingPoint.toString(), message);

            // Finally, log the debug message
            LOGGER.debug(messageString);
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
            String messageString = String.format("[%s] %s", callingPoint.toString(), message);

            // Finally, log the debug message
            LOGGER.trace(messageString);
        } else {
            // Nothing fancy to calculate if the log level is not debug
            LOGGER.trace( message );
        }
    }

    /**
     * Dynamically set the log level for the 'com.gpudb' logger at runtime.
     *
     * Supports multiple SLF4J backend implementations:
     * <ul>
     *   <li><b>Logback</b> - Native SLF4J implementation (most common)</li>
     *   <li><b>Log4j2</b> - Apache Log4j 2.x via slf4j-log4j2 binding</li>
     *   <li><b>JUL</b> - Java Util Logging via slf4j-jdk14 binding</li>
     * </ul>
     *
     * Note: slf4j-simple does not support runtime log level changes.
     * For slf4j-simple, set the level via system property at startup:
     * {@code -Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG}
     *
     * @param logLevel One of the supported log levels: TRACE, DEBUG, INFO,
     *                 WARN, ERROR, OFF. {@code null} value is treated as 'OFF'.
     * @return {@code true} if the log level was successfully set,
     *         {@code false} if no supported backend was detected or an error occurred.
     */
    public static boolean setLoggingLevel(String logLevel) {
        String logLevelUpper = (logLevel == null) ? "OFF" : logLevel.toUpperCase();

        // Validate the log level
        if (!isValidLogLevel(logLevelUpper)) {
            LOGGER.warn("Invalid log level '{}'. Valid levels are: TRACE, DEBUG, INFO, WARN, ERROR, OFF", logLevel);
            return false;
        }

        // Try backends in order of popularity/likelihood
        if (trySetLogbackLevel(logLevelUpper)) {
            return true;
        }

        if (trySetLog4j2Level(logLevelUpper)) {
            return true;
        }

        if (trySetJulLevel(logLevelUpper)) {
            return true;
        }

        // No supported backend found
        LOGGER.warn("Could not set log level to '{}'. No supported SLF4J backend detected " +
                    "(Logback, Log4j2, or JUL). Configure logging via your backend's " +
                    "configuration file instead.", logLevelUpper);
        return false;
    }

    /**
     * Validates that the given log level is one of the supported values.
     */
    private static boolean isValidLogLevel(String level) {
        return level.equals("TRACE") || level.equals("DEBUG") || level.equals("INFO") ||
               level.equals("WARN") || level.equals("ERROR") || level.equals("OFF");
    }

    /**
     * Attempts to set the log level using Logback.
     * Uses reflection to avoid compile-time dependency on Logback.
     *
     * @param level The log level (TRACE, DEBUG, INFO, WARN, ERROR, OFF)
     * @return true if successful, false otherwise
     */
    private static boolean trySetLogbackLevel(String level) {
        try {
            // Check if Logback is available
            Class<?> logbackLoggerClass = Class.forName("ch.qos.logback.classic.Logger");
            Class<?> logbackLevelClass = Class.forName("ch.qos.logback.classic.Level");

            // Get the Level constant (e.g., Level.DEBUG)
            Field levelField = logbackLevelClass.getField(level);
            Object levelObj = levelField.get(null);

            if (levelObj == null) {
                return false;
            }

            // Get the logger and cast to Logback Logger
            Logger slf4jLogger = LoggerFactory.getLogger(API_LOGGER_NAME);

            // Verify the logger is actually a Logback logger
            if (!logbackLoggerClass.isInstance(slf4jLogger)) {
                return false;
            }

            // Call setLevel on the Logback logger
            Method setLevelMethod = logbackLoggerClass.getMethod("setLevel", logbackLevelClass);
            setLevelMethod.invoke(slf4jLogger, levelObj);

            LOGGER.debug("Log level set to '{}' for logger '{}' using Logback", level, API_LOGGER_NAME);
            return true;

        } catch (ClassNotFoundException e) {
            // Logback is not in the classpath
            return false;
        } catch (NoSuchFieldException e) {
            // Invalid level name for Logback
            LOGGER.debug("Invalid Logback log level: '{}'", level);
            return false;
        } catch (Exception e) {
            // Other reflection errors
            LOGGER.debug("Failed to set Logback log level: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Attempts to set the log level using Log4j2 Configurator.
     * Uses reflection to avoid compile-time dependency on Log4j2.
     *
     * @param level The log level (TRACE, DEBUG, INFO, WARN, ERROR, OFF)
     * @return true if successful, false otherwise
     */
    private static boolean trySetLog4j2Level(String level) {
        try {
            // Check if Log4j2 core is available (needed for Configurator)
            Class<?> configuratorClass = Class.forName("org.apache.logging.log4j.core.config.Configurator");
            Class<?> log4j2LevelClass = Class.forName("org.apache.logging.log4j.Level");

            // Map SLF4J level names to Log4j2 level names (they're mostly the same)
            // Log4j2 uses FATAL instead of OFF for the highest severity, but OFF exists too
            String log4j2LevelName = level;

            // Get the Level using toLevel() method
            Method toLevelMethod = log4j2LevelClass.getMethod("toLevel", String.class);
            Object levelObj = toLevelMethod.invoke(null, log4j2LevelName);

            if (levelObj == null) {
                return false;
            }

            // Call Configurator.setLevel(String loggerName, Level level)
            Method setLevelMethod = configuratorClass.getMethod("setLevel", String.class, log4j2LevelClass);
            setLevelMethod.invoke(null, API_LOGGER_NAME, levelObj);

            LOGGER.debug("Log level set to '{}' for logger '{}' using Log4j2", level, API_LOGGER_NAME);
            return true;

        } catch (ClassNotFoundException e) {
            // Log4j2 core is not in the classpath
            return false;
        } catch (Exception e) {
            // Other reflection errors
            LOGGER.debug("Failed to set Log4j2 log level: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Attempts to set the log level using Java Util Logging (JUL).
     * JUL is always available as it's part of the JDK.
     *
     * @param level The log level (TRACE, DEBUG, INFO, WARN, ERROR, OFF)
     * @return true if successful, false otherwise
     */
    private static boolean trySetJulLevel(String level) {
        try {
            // Check if slf4j-jdk14 binding is being used by checking if the SLF4J logger
            // delegates to JUL. We do this by checking the logger factory.
            String loggerFactoryClass = LoggerFactory.getILoggerFactory().getClass().getName();

            // slf4j-jdk14 uses org.slf4j.impl.JDK14LoggerFactory or similar
            if (!loggerFactoryClass.toLowerCase().contains("jdk14") &&
                !loggerFactoryClass.toLowerCase().contains("jul")) {
                // JUL binding is not in use, don't attempt to configure JUL
                // as it might interfere with the actual backend
                return false;
            }

            // Map SLF4J levels to JUL levels
            java.util.logging.Level julLevel = mapToJulLevel(level);

            if (julLevel == null) {
                return false;
            }

            // Get the JUL logger and set the level
            java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(API_LOGGER_NAME);
            julLogger.setLevel(julLevel);

            // Also need to set the handler levels if they're more restrictive
            // This ensures the level change takes effect
            for (java.util.logging.Handler handler : julLogger.getHandlers()) {
                if (handler.getLevel().intValue() > julLevel.intValue()) {
                    handler.setLevel(julLevel);
                }
            }

            // If no handlers on this logger, check parent (usually root logger)
            if (julLogger.getHandlers().length == 0) {
                java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
                for (java.util.logging.Handler handler : rootLogger.getHandlers()) {
                    if (handler.getLevel().intValue() > julLevel.intValue()) {
                        handler.setLevel(julLevel);
                    }
                }
            }

            LOGGER.debug("Log level set to '{}' for logger '{}' using JUL", level, API_LOGGER_NAME);
            return true;

        } catch (Exception e) {
            LOGGER.debug("Failed to set JUL log level: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Maps SLF4J/Logback level names to Java Util Logging (JUL) levels.
     *
     * Mapping:
     * <ul>
     *   <li>TRACE -> FINEST</li>
     *   <li>DEBUG -> FINE</li>
     *   <li>INFO  -> INFO</li>
     *   <li>WARN  -> WARNING</li>
     *   <li>ERROR -> SEVERE</li>
     *   <li>OFF   -> OFF</li>
     * </ul>
     *
     * @param slf4jLevel The SLF4J level name
     * @return The corresponding JUL Level, or null if not recognized
     */
    private static java.util.logging.Level mapToJulLevel(String slf4jLevel) {
        switch (slf4jLevel) {
            case "TRACE":
                return java.util.logging.Level.FINEST;
            case "DEBUG":
                return java.util.logging.Level.FINE;
            case "INFO":
                return java.util.logging.Level.INFO;
            case "WARN":
                return java.util.logging.Level.WARNING;
            case "ERROR":
                return java.util.logging.Level.SEVERE;
            case "OFF":
                return java.util.logging.Level.OFF;
            default:
                return null;
        }
    }

    /**
     * Sets the log level for a dependent library logger.
     * Useful for silencing verbose third-party libraries.
     *
     * @param loggerName The fully qualified logger name (e.g., "org.apache.http")
     * @param logLevel   The desired log level
     * @return true if successful, false otherwise
     */
    public static boolean setDependencyLogLevel(String loggerName, String logLevel) {
        String logLevelUpper = (logLevel == null) ? "OFF" : logLevel.toUpperCase();

        if (!isValidLogLevel(logLevelUpper)) {
            LOGGER.warn("Invalid log level '{}' for logger '{}'", logLevel, loggerName);
            return false;
        }

        // Try each backend
        if (trySetLogbackLevelForLogger(loggerName, logLevelUpper)) {
            return true;
        }

        if (trySetLog4j2LevelForLogger(loggerName, logLevelUpper)) {
            return true;
        }

        if (trySetJulLevelForLogger(loggerName, logLevelUpper)) {
            return true;
        }

        LOGGER.warn("Could not set log level for '{}'. No supported backend detected.", loggerName);
        return false;
    }

    /**
     * Sets Logback log level for a specific logger.
     */
    private static boolean trySetLogbackLevelForLogger(String loggerName, String level) {
        try {
            Class<?> logbackLoggerClass = Class.forName("ch.qos.logback.classic.Logger");
            Class<?> logbackLevelClass = Class.forName("ch.qos.logback.classic.Level");

            Field levelField = logbackLevelClass.getField(level);
            Object levelObj = levelField.get(null);

            Logger slf4jLogger = LoggerFactory.getLogger(loggerName);

            if (!logbackLoggerClass.isInstance(slf4jLogger)) {
                return false;
            }

            Method setLevelMethod = logbackLoggerClass.getMethod("setLevel", logbackLevelClass);
            setLevelMethod.invoke(slf4jLogger, levelObj);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Sets Log4j2 log level for a specific logger.
     */
    private static boolean trySetLog4j2LevelForLogger(String loggerName, String level) {
        try {
            Class<?> configuratorClass = Class.forName("org.apache.logging.log4j.core.config.Configurator");
            Class<?> log4j2LevelClass = Class.forName("org.apache.logging.log4j.Level");

            Method toLevelMethod = log4j2LevelClass.getMethod("toLevel", String.class);
            Object levelObj = toLevelMethod.invoke(null, level);

            Method setLevelMethod = configuratorClass.getMethod("setLevel", String.class, log4j2LevelClass);
            setLevelMethod.invoke(null, loggerName, levelObj);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Sets JUL log level for a specific logger.
     */
    private static boolean trySetJulLevelForLogger(String loggerName, String level) {
        try {
            String loggerFactoryClass = LoggerFactory.getILoggerFactory().getClass().getName();

            if (!loggerFactoryClass.toLowerCase().contains("jdk14") &&
                !loggerFactoryClass.toLowerCase().contains("jul")) {
                return false;
            }

            java.util.logging.Level julLevel = mapToJulLevel(level);
            if (julLevel == null) {
                return false;
            }

            java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(loggerName);
            julLogger.setLevel(julLevel);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

}  // end class GPUdbLogger
