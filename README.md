Kinetica Java API 
=================

There are two projects in this repository: the Kinetica Java API and an example
project.

api
---

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



example
-------

To build the jar, run the following command in the example directory:

> mvn clean package


Then, to run the example, in the target directory, run the following command:

> java -jar gpudb-api-example-1.0-jar-with-dependencies.jar

Please make sure that Kinetica is running at the URL and port specified in
line 4 of example/src/main/java/com/gpudb/example/Example.java (the default
is "http://localhost:9191").
