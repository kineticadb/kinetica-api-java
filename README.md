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


The documentation can be found at https://docs.kinetica.com/7.1/.
The Java specific documentation can be found at:

* https://docs.kinetica.com/7.1/guides/java_guide/
* https://docs.kinetica.com/7.1/api/java/


For changes to the client-side API, please see CHANGELOG.md.  For changes
to GPUdb functions, please see CHANGELOG-FUNCTIONS.md.


### KIFS File upload and download facility

The purpose of this API is to facilitate uploading of files into KIFS from a
local directory and downloading from the KIFS into a local directory. There
are multiple methods available for uploading and downloading files. This new
facility is available for Kinetica version `7.1.4.0` onwards.

#### GPUdbFileHandler API

The `upload` and the `download` methods of the class `GPUdbFileHandler` are
overloaded to accommodate either a single file name, or a list of file names to
upload to or download from KIFS.

The following methods are available -
-    `upload` - Has multiple overloads for uploading single/multiple files.
-    `download` - Has similar overloads as uploads for downloading files.



### API Logging

The Java API uses the Simple Logging Facade for Java (SLF4J) with Logback as
the backend for flexibility and convenience. Users of the API are encouraged to
use the slf4j logging interface as well.

```
{
    // Example of creating and using a SLF4J logger a java class file.
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    ...
    private static Logger LOGGER = LoggerFactory.getLogger(Example.class);
    ...
    LOGGER.warn("..."); // trace(), debug(), info(), warn(), error()
}
```

The default logging level is INFO and output will be sent to a STDOUT appender.
The 'com.gpudb' logging level can be set by a system property when running a jar
with the arg '-Dlogging.level.com.gpudb=DEBUG' and a convenience function has
been added to change the level from Java code.

```
com.gpudb.GPUdbLogger.setLoggingLevel("DEBUG");
```

A complete logback logging configuration can be specified with a
logback.xml resource file in the application pom or on the
command-line when running the jar with a
'-Dlogback.configurationFile=/path/to/logback.xml' arg.
See the GPUdb API's logback.xml file for an example of a STDOUT logger.
The full documentation, including examples, of a logback configuration
file can be found here: https://logback.qos.ch/manual/configuration.html

```
    <!-- Example resource for logback configuration file in an application pom.xml file. -->
    <resource>
        <directory>src/main/resources</directory>
        <includes>
            <include>logback.xml</include>
        </includes>
    </resource>
```

The SLF4J logger backend can be changed to any of the supported backends,
including log4j, by requiring the appropriate wrapper package and excluding the
'logback-classic' backend. See https://www.slf4j.org/legacy.html for
more information. Note that if the SLF4J backend is changed from 'logback' the
convenience function GPUdbLogger.setLoggingLevel() and the system parameter
'logging.level.com.gpudb' will not work to change the log level. The level
will have to be set using the new logger implementation.

```
    <!-- Example of log4j2 as the slf4j backend for the application and GPUdb API in a pom.xml file. -->
    <dependency>
        <groupId>org.apache.logging.log4j</groupId>
        <artifactId>log4j-api</artifactId>
        <version>2.7</version>
    </dependency>
    <dependency>
        <groupId>org.apache.logging.log4j</groupId>
        <artifactId>log4j-core</artifactId>
        <version>2.7</version>
    </dependency>
    <dependency>
        <groupId>org.apache.logging.log4j</groupId>
        <artifactId>log4j-slf4j-impl</artifactId>
        <version>2.7</version>
    </dependency>
    <dependency>
        <groupId>com.gpudb</groupId>
        <artifactId>gpudb-api</artifactId>
        <version>${gpudb-api.version}</version>
        <exclusions>
            <exclusion>
                <groupId>ch.qos.logback</groupId>
                <artifactId>logback-classic</artifactId>
            </exclusion>
        </exclusions>
    </dependency>

    ...

    <resource>
        <directory>src/main/resources</directory>
        <includes>
            <include>log4j2.xml</include>
        </includes>
    </resource>
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

Note that the default URL is "http://localhost:9191".
Also note that the two options, if given, must precede the `-jar` option.

Please make sure that Kinetica is running at the URL and port specified in
line 4 of example/src/main/java/com/gpudb/example/Example.java (the default
is "http://localhost:9191").

### GPUdbFileHandler Example
An example usage of the API can be found in the `gpudb-api-example` project
in the file `GPUdbFileHandlerExample.java`. The example can be run using the
command -

> java -Durl="http://10.0.0.10:9191" -DlogLevel="INFO" \
-cp ./target/gpudb-api-example-1.0-jar-with-dependencies.jar \
com.gpudb.example.GPUdbFileHandlerExample

The URL VM argument needs to be changed suitably to match the exact environment.
