# Kinetica Java API


There are two projects in this repository: the Kinetica Java API and an example
project.

## API

In the api directory, run the following command in the api direcotry to create
the API JAR:

```
> mvn clean package
```


In order to use the API JAR for the example, run the following command to
install the jar in the local repository:

```
> mvn install
```


The documentation can be found at http://www.kinetica.com/docs/7.1/index.html.
The Java specific documentation can be found at:

*   http://www.kinetica.com/docs/7.1/tutorials/java_guide.html
*   http://www.kinetica.com/docs/7.1/api/java/index.html


For changes to the client-side API, please see CHANGELOG.md.  For changes
to GPUdb functions, please see CHANGELOG-FUNCTIONS.md.

```
log4j.appender.com.gpudb=INFO, stdout # or whatever appender you wish to use
```

Alternatively, the logging can be enabled and the log level set programmatically
by using the appropriate GPUdb.Options method:

```
    GPUdb.Options options = new GPUdb.Options();
    options.setLoggingLevel( logLevel ); // For example, "info" or "debug"
    GPUdb gpudb = new GPUdb( url, options );
```



### API Logging

To enable logging, either use an appropriate log4j properties file, e.g.
log4j.properties, in the classpath of the application with the following
logger in it:

```
```

### SSL Configuration

When the Kinetica server is configured to use SSL, the root certificate needs
to be installed in the client machine for the API to successfullly make secured
connections with the server.  Please ensure that the root certificate is either
installed in the default java key store (JKS), or supply the separate certificate
JKS and its password to the API via the the following two system environment
variables:

*   javax.net.ssl.trustStore
*   javax.net.ssl.trustStorePassword

This can be done by setting the Java Runtime Environment (JRE) variables at the
command line by using the following arguments:

*   -Djavax.net.ssl.trustStore
*   -Djavax.net.ssl.trustStorePassword

The environment variables can also be programmatically set from applications
that use the Java API:

```
    System.setProperty( "javax.net.ssl.trustStore", trustStorePath );
    System.setProperty( "javax.net.ssl.trustStorePassword", trustStorePassword );
```



## Example

To build the jar, run the following command in the example directory:

> mvn clean package


Then, to run the example, in the target directory, run the following command:

> java -jar gpudb-api-example-1.0-jar-with-dependencies.jar

To provide the URL of the Kinetica server and/or configure the log level,
use the following options:

> java -DlogLevel=debug -Durl=http://172.42.0.80:9191 \
     -jar gpudb-api-example-1.0-jar-with-dependencies.jar

Note that the default URL is "http://localhost:9191" and the default log level
is OFF.  Also note that the two options, if given, must precede the `-jar`
option.

Please make sure that Kinetica is running at the URL and port specified in
line 4 of example/src/main/java/com/gpudb/example/Example.java (the default
is "http://localhost:9191").


## Notes

Since the 7.1.2.2 version of the API, due to the org.apache.avro dependency
having been increased to 1.10.1 for security purposes, applications using this
API may get the following innocuous warning logged:

   ```Failed to load class org.slf4j.impl.StaticLoggerBinder```

This happens due to `http://www.slf4j.org/codes.html#StaticLoggerBinder`, and
according to SLF4J's guidance, this API does not include any SLF4J binding so
that we do not inadvertantly force any specific binding on the client application.
The end-user application is free to choose their own binding; or if no logging is
used, then simply use the no-operation logger implementation by including the
following dependency in the application's POM:

   ```<!-- https://mvnrepository.com/artifact/org.slf4j/slf4j-nop -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-nop</artifactId>
        <version>1.7.30</version>
        <scope>test</scope>
    </dependency>
   ```
