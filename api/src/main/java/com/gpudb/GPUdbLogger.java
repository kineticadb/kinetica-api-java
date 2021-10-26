package com.gpudb;

import org.apache.log4j.*;

import java.util.Enumeration;


public class GPUdbLogger {

    // The default logging level--off
    protected static final Level DEFAULT_LOGGING_LEVEL = Level.OFF;

    // The name of the logger for this API
    protected static final String API_LOGGER_NAME = "Kinetica Java API";

    // The loggers used for dependent libraries that might have obnxious
    // default log levels
    protected static final String DEP_LIB_APACHE_CLIENT_LOGGER = "org.apache.http";


    // Actual logger used for the API
    private static Logger LOG = Logger.getLogger( API_LOGGER_NAME );


    /**
     * Initializes the logger with the given logging level.  It is
     * **crucial** to call this method before using the logger.  This method
     * serves the following purpose:
     * * If no log4j.properties or any other properties file is found
     *   in the class path of the application that uses this API, log4j
     *   emits warnings about not finding any appender.  This method
     *   prevents that from happening.
     * * Older versions of the API did not have logging, so suddenly having
     *   logging might throw off end user applications.  So, by default,
     *   we turn off logging (controlled by the static final member above).
     * * If no logging properties were provided by the end-user, then suppress
     *   the obnoxious debug logging that is turned on by default by the Apache
     *   HTTPClient library.
     * * If the given log level is different from the default, set it explicitly.
     */
    public static void initializeLogger( Level loggingLevel ) {
        Logger rootLogger = Logger.getRootLogger();

        // Check for appenders--if none found, then that means the
        // end-user application has not supplied any logging properties
        // (which indicates that they probably don't expect any logging).
        Enumeration<Appender> appenders = rootLogger.getAllAppenders();
        boolean isLoggerConfiguredByUser = appenders.hasMoreElements();

        if ( !isLoggerConfiguredByUser ) {
            // No appender is found; suppress log4j warnings by explicitly
            // getting the logger for the API
            LOG = Logger.getLogger( API_LOGGER_NAME );

            // Configuring log4j helps towards suppressing the annoying log4j
            // warnings
            PatternLayout layout = new PatternLayout( "%d{yyyy-MM-dd HH:mm:ss} %-5p  %m%n" );
            ConsoleAppender consoleAppender = new ConsoleAppender();
            consoleAppender.setLayout( layout );
            consoleAppender.activateOptions();
            BasicConfigurator.configure( consoleAppender );

            // Set the API's log level
            LOG.setLevel( loggingLevel );

            // Set the Apache HTTPClient log leve as well
            Logger.getLogger( "org.apache.http" ).setLevel( loggingLevel );
        } else {
            // If the log level is different from the default, set it explicitly
            if ( !loggingLevel.equals( DEFAULT_LOGGING_LEVEL ) ) {
                LOG.setLevel( loggingLevel );
                LOG.warn( "Log properties set, but the log level is also "
                          + "programmatically set by the user ('"
                          + loggingLevel.toString()
                          + "'); using that one and ignoring the one in "
                          + "the properties.");
            }

            // Some logging properties found; check if the libraries that this
            // API uses have log levels defined in the properties.  If not, we
            // will turn them off (at least the obnoxious ones).  For that, first
            // look for such loggers in the user given properties.
            boolean isApacheHttpClientLoggerFound = false;
            Enumeration<Logger> loggers = rootLogger.getHierarchy().getCurrentLoggers();
            while ( loggers.hasMoreElements() ) {
                if ( loggers.nextElement()
                     .getName()
                     .equalsIgnoreCase( DEP_LIB_APACHE_CLIENT_LOGGER ) ) {
                    isApacheHttpClientLoggerFound = true;
                }
            }

            // Mute the obnoxious logs if not set by the user
            if ( !isApacheHttpClientLoggerFound ) {
                Logger.getLogger( DEP_LIB_APACHE_CLIENT_LOGGER ).setLevel( Level.OFF );
            }
        }
    }   // end initializeLogger


    public static void info(String message) {
        LOG.info( message );
    }

    public static void error(String message) {
        LOG.error( message );
    }

    public static void warn(String message) {
        LOG.warn( message );
    }

    public static void fatal(String message) {
        LOG.fatal( message );
    }


    public static void debug(String message) {
        LOG.debug( message );
    }


    public static void trace(String message) {
        LOG.trace( message );
    }


    /**
     * Print extra information with the debug message.
     */
    public static void debug_with_info(String message) {
        if ( ( LOG.getEffectiveLevel() == Level.DEBUG )
             || ( LOG.getEffectiveLevel() == Level.TRACE )
             || ( LOG.getEffectiveLevel() == Level.ALL ) ) {
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
            LOG.debug( builder.toString() );
        } else {
            // Nothing fancy to calculate if the log level is not debug
            LOG.debug( message );
        }
    }


    /**
     * Print extra information with the trace message.
     */
    public static void trace_with_info(String message) {
        if ( ( LOG.getEffectiveLevel() == Level.TRACE )
             || ( LOG.getEffectiveLevel() == Level.ALL ) ) {
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
            LOG.trace( builder.toString() );
        } else {
            // Nothing fancy to calculate if the log level is not debug
            LOG.trace( message );
        }
    }

}  // end class GPUdbLogger
