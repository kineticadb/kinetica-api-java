# Kinetica Java API


There are two projects in this repository: the Kinetica Java API and an example
project.

## API


In the api directory, run the following command in the api direcotry to create
the API JAR:

> mvn clean package


In order to use the API JAR for the example, run the following command to
install the jar in the local repository:

> mvn install


The documentation can be found at http://www.kinetica.com/docs/7.0/index.html.
The Java specific documentation can be found at:

*   http://www.kinetica.com/docs/7.0/tutorials/java_guide.html
*   http://www.kinetica.com/docs/7.0/api/java/index.html


For changes to the client-side API, please see CHANGELOG.md.  For changes
to GPUdb functions, please see CHANGELOG-FUNCTIONS.md.


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

Please make sure that Kinetica is running at the URL and port specified in
line 4 of example/src/main/java/com/gpudb/example/Example.java (the default
is "http://localhost:9191").
