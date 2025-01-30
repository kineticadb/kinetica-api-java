# GPUdb Java API Changelog

## Version 7.2

### Version 7.2.2.5 - 2025-01-30

#### Added
-   Improved error reporting on connection failures

#### Changed
-   Switched retry handler to retry on `SocketTimeoutException` instead of
    `ConnectTimeoutException`


### Version 7.2.2.4 - 2024-12-17

#### Added
-   Support for key lookups returning a subset of a table's columns with fewer
    lookup restrictions via `RecordRetriever.getColumnsByKey()`
-   Support for key lookups returning records from a specified offset


### Version 7.2.2.3 - 2024-10-29

#### Added
-   Failback to a primary cluster after failing over to a secondary cluster

#### Changed
-   Upgraded Avro library to 1.11.4

#### Fixed
-   Error message for bad URLs with auto-discovery disabled
-   Potential resource leaks upon connection errors


### Version 7.2.2.2 - 2024-10-24

#### Fixed
-   Issue with lookup up server version when auto-discovery is disabled


### Version 7.2.2.1 - 2024-10-22

#### Changed
-   Modified server version extractor to handle all 5 version components
-   Made the full set of system properties for the active cluster available


### Version 7.2.2.0 - 2024-10-15

#### Changed
-   Modified POM for publishing to Maven Central Repository
-   Upgraded Jackson core library to 2.17.1
-   Downgraded Logback library to 1.3.14
-   Upgraded SLF4j library to 2.0.13

#### Fixed
-   Issue with dependent JDBC `fullshaded` driver terminating with no linked
    Snappy library
-   JavaDoc generation warnings

#### Notes
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.


### Version 7.2.1.0 - 2024-09-08

#### Added
-   OAuth2 authentication support

#### Changed
-   Publishing to Maven Central Repository

#### Fixed
-   Snappy error for `fullshaded` JDBC JAR


### Version 7.2.0.5 - 2024-06-07

#### Fixed
-   Out-of-memory error when downloading large files

#### Notes
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.


### Version 7.2.0.4 - 2024-04-04

#### Changed
-   Lowered default server connection timeout to 5 seconds
-   Made server connection timeout (user-specified or default) govern connection
    timeouts in all cases of initially connecting to a server
-   Deprecated `isKineticaRunning()` in favor of `isSystemRunning()`

#### Notes
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.


### Version 7.2.0.3 - 2024-03-20

#### Added
-   Support for unsigned long types and null values in arrays

#### Fixed
-   Concurrency issue with the use of `BulkInserter.insert(List)`

#### Notes
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.


### Version 7.2.0.2 - 2024-03-13

#### Changed
-   Increased connection timeout from ~1 to 20 seconds to account for
    connections over high-traffic and public networks
-   Upgraded Snappy library from 1.1.10.4 to 1.1.10.5

#### Notes
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.


### Version 7.2.0.1 - 2024-02-27

#### Changed
-   Upgraded Apache HTTPClient5 library from 5.3 to 5.3.1


### Version 7.2.0.0 - 2024-02-11

#### Added
-   Support for Array, JSON and Vector data
-   `query()` & `execute()` methods for more easily running SQL statements

#### Notes
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.



## Version 7.1

### Version 7.1.10.4 - 2024-09-08

#### Fixed
-   Snappy error for `fullshaded` JDBC JAR


### Version 7.1.10.3 - 2024-08-05

#### Changed
-   Publishing to Maven Central Repository


### Version 7.1.10.2 - 2024-07-15

#### Changed
-   Several security-related dependency updates


### Version 7.1.10.1 - 2024-06-07

#### Fixed
-   Out-of-memory error when downloading large files


### Version 7.1.10.0 - 2024-05-16

#### Notes
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.


### Version 7.1.9.18 - 2024-04-04

#### Changed
-   Lowered default server connection timeout to 5 seconds
-   Made server connection timeout (user-specified or default) govern connection
    timeouts in all cases of initially connecting to a server
-   Deprecated `isKineticaRunning()` in favor of `isSystemRunning()`

#### Notes
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.


### Version 7.1.9.17 - 2024-03-20

#### Fixed
-   Concurrency issue with the use of `BulkInserter.insert(List)`


### Version 7.1.9.16 - 2024-03-13

#### Changed
-   Increased connection timeout from ~1 to 20 seconds to account for
    connections over high-traffic and public networks
-   Upgraded Snappy library from 1.1.10.4 to 1.1.10.5

#### Notes
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.


### Version 7.1.9.15 - 2024-02-29

#### Changed
-   Upgraded Apache HTTPClient5 library from 5.3 to 5.3.1


### Version 7.1.9.14 - 2023-12-21

#### Changed
-   Aligned read timeouts in cloud & non-cloud environments


### Version 7.1.9.13 - 2023-12-07

#### Changed
-   Upgraded Logback library from 1.2.10 to 1.2.13


### Version 7.1.9.12 - 2023-12-03

#### Added
-   `GPUdbSqlIterator` class for easily looping over the records of a SQL result
    set.

#### Changed
-   Upgraded Avro library from 1.11.1 to 1.11.3
-   Upgraded Snappy library from 1.1.10.1 to 1.1.10.4
-   Upgraded JSON library from 20230227 to 20231013


### Version 7.1.9.11 - 2023-10-24

#### Changed
-   Auto-disabled Snappy compression, if not available on the host system


### Version 7.1.9.10 - 2023-10-17

#### Fixed
-   Deadlock in multi-head ingest


### Version 7.1.9.9 - 2023-10-10

#### Fixed
-   Bug in thread-safety during multi-head ingest HA failover


### Version 7.1.9.8 - 2023-10-05

#### Fixed
-   Bug in multi-head ingest HA failover
-   Bug in head node JSON ingest


### Version 7.1.9.7 - 2023-09-17

#### Fixed
-   Bug in file download API


### Version 7.1.9.6 - 2023-08-11

#### Added
-   `getRecordsJson()` method & overloads for direct egress of data as JSON
    strings
-   `insertRecordsFromJson()` method overloads
-   `BulkInserter` constructor overloads

#### Fixed
-   Error handling for JSON support in `BulkInserter`


### Version 7.1.9.5 - 2023-07-31

#### Fixed
-   Support for large file downloads
-   Upgraded Snappy library from 1.1.8.4 to 1.1.10.1


### Version 7.1.9.4 - 2023-06-08

#### Added
-   Support for ULONG column type
-   Support for UUID column type & primary key


### Version 7.1.9.3 - 2023-04-30

#### Changed
-   Upgraded JSON library from 20220924 to 20230227


### Version 7.1.9.2 - 2023-04-26

#### Fixed
-   Support for routing through the head node all disabled multi-head operations


### Version 7.1.9.1 - 2023-04-23

#### Added
-   Multi-head ingestion support for JSON-formatted data
-   Support for HA failover when user-specified connection URLs don't match the
    server-known URLs; multi-head operations will still be disabled

#### Changed
-   Removed N+1 features & references


### Version 7.1.9.0 - 2023-03-19

#### Added
-   Examples of secure/unsecure connections; improved SSL failure error message

#### Changed
-   Fixed handling of timeout units
-   Fixed application of SSL handshake timeout to read requests
-   Improved error logging format

#### Notes
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.


### Version 7.1.8.10 - 2023-02-16

#### Added
-   JSON support in `BulkInserter`

#### Changed
-   Upgraded Apache HTTP client library from 4.5.13 to 5.2.1
-   Fixed OOM errors during large file uploads
-   Improved handling of non-retryable `BulkInserter` errors
-   Improved usability of file handler example

#### Removed
-   Unneeded class references in logging output


### Version 7.1.8.9 - 2023-01-20

#### Added
-   Retry handler for `org.apache.http.conn.ConnectionTimeoutException` errors

#### Changed
-   Fixed cluster hostname matching check


### Version 7.1.8.8 - 2023-01-08

#### Added
-   Support for boolean type
-   `insertRecordsFromJson()` method for direct ingest of data as JSON strings

#### Changed
-   Improved reporting of SSL certificate errors
-   Updated Jackson databind version to 2.14.1


### Version 7.1.8.7 - 2022-11-30

#### Added
-   Improved reporting of permissions errors during `BulkInserter` insert


### Version 7.1.8.6 - 2022-11-21

#### Added
-   Table permissions check on `BulkInserter` instantiation

#### Changed
-   Updated Jackson databind version to 2.14.0
-   Stopped/suspended clusters are reported as such


### Version 7.1.8.5 - 2022-11-02

#### Added
-   `getWarnings()` method on `BulkInserter`

#### Changed
-   `BulkInserter.getErrors()` now only returns errors and not warnings -- use
    `getWarnings()` for warnings


### Version 7.1.8.4 - 2022-10-26

#### Changed
-   Improved performance of `BulkInserter` & `RecordRetriever` key generation


### Version 7.1.8.3 - 2022-10-17

#### Added
-   Retry handler for all requests
-   Logging during timed flush executions

#### Changed
-   Reduced idle connection check interval to 100ms
-   Improved reporting of `BulkInserter` errors


### Version 7.1.8.2 - 2022-10-03

#### Changed
-   Reduced logging during setting of timed flush


### Version 7.1.8.1 - 2022-10-02

#### Changed
-   Simplified dependency inclusion


### Version 7.1.8.0 - 2022-09-27

#### Added
-   Support for boolean type

#### Changed
-   `BulkInserter` will do a default of 3 local retries for any non-data failure
    before attempting to fail over to another cluster
-   Timed flush mechanism can be set or reset after `BulkInserter` construction
-   Fully relocated dependencies to avoid library conflicts

#### Fixed
-   Java bytecode version mismatch when compiling on Java9+ and running on Java8


### Version 7.1.7.7 - 2022-10-06

#### Changed
-   Added logging during timed flush executions


### Version 7.1.7.6 - 2022-10-03

#### Changed
-   Reduced logging during setting of timed flush


### Version 7.1.7.5 - 2022-09-27

#### Changed
-   BulkInserter will do a default of 3 local reries for any non-data failure
    before attempting to fail over to another cluster
-   Timed flush mechanism can be set or reset after BulkInserter construction
-   No failover will be attempted if only one cluster is found


### Version 7.1.7.4 - 2022-09-19

#### Changed
-   Targeted Java 8 runtime


### Version 7.1.7.3 - 2022-09-08

#### Changed
-   Updated Avro version to 1.11.1


### Version 7.1.7.2 - 2022-08-12

#### Added
-   Method to query a `BulkInserter` multi-head status
 
#### Changed
-   Switched ordering of flush & thread shutdown sequence
-   Disabled client intra-cluster failover if failover is disabled on the server


### Version 7.1.7.1 - 2022-07-27

#### Changed
-   Improved error reporting of environment-related issues during inserts


### Version 7.1.7.0 - 2022-07-18

#### Added
-   Capability to pass in self-signed certificates & passwords as options

#### Changed
-   Updated dependencies to more secure versions
-   Fixed unhandled exception when an Avro encoding error occurs
-   Fixed error where a type's column order would not match a table created from
    it
-   Removed client-side primary key check, to improve performance and make
    returned errors more consistently delivered


### Version 7.1.6.1 - 2022-02-22

#### Breaking Changes
-   Logging changed from using Log4j 1.x to SLF4J with a default Logback stdout
    logger
    - The breaking change was due to security issues with both Log4j 1.x and 2.x
      and SLF4J was chosen since it is the successor to Log4j and more flexible
    - See the README.md for directions on using a SLF4J logger and configuring
      an alternative backend to Logback
    - Note that it is possible to continue to use Log4j 1.x through SLF4J,
      though not recommended
-   Removed `GPUdb.Options.getLoggingLevel()` &
    `GPUdb.Options.setLoggingLevel()`; instead, set the `com.gpudb` log level
    with a user-supplied `logback.xml` resource or with the newly added static
    `GPUdbLogger.setLoggingLevel()`, which can be called at any time


### Version 7.1.6.0 - 2022-01-27

#### Added
-   Option for automatically flushing the BulkInserter at a given delay
-   Added support for retrieving errant records from `BulkInserter` ingest
-   Added support for automatically flushing the `BulkInserter` and cleaning up of
    service objects upon `BulkInserter` shutdown
-   Improved SSL cert bypass


### Version 7.1.5.0 - 2021-10-13

#### Added
-   Minor KiFS API usage improvements

#### Notes
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.


### Version 7.1.4.0 - 2021-07-29

#### Added
-   Introduced a new API for facilitating uploading and downloading of files
    to and from the KIFS. The class encapsulating the API is `GPUdbFileHandler`.
    A complete example has been given in the `gpudb-api-example` project in the
    class `GPUdbFileHandlerExample`.
-   Introduced the capability to upload Parquet files.

#### Notes
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.


### Version 7.1.3.0 - 2021-03-05

#### Added

-   Added option in `GPUdbBase` class to pass in custom
    `SSLConnectionSocketFactory` to facilitate passing in
    a user supplied truststore file along with the password

#### Notes
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.


### Version 7.1.2.3 - 2021-02-03

#### Added
-   Class `GPUdb.GPUdbVersion` that represents the Kinetica server's
    version (the one the API is connected to).
-   Method `GPUdb.getServerVersion()`.


#### Changed
-   Added support for multi-head key lookup for replicated tables.


### Version 7.1.2.2 - 2021-01-29

#### Security
-   Updated the following dependency package versions to eliminate known
    security risks and other issues:
    -  org.apache.avro     1.8.1  -> 1.10.1
    -  commons-codec:      1.10   -> 1.13
    -  httpclient:         4.5.11 -> 4.5.13
    -  maven-shade-plugin: 2.1    -> 3.2.4

#### Notes
-   Due to the dependency updates, applications using this API may
    start getting a warning log from SLF4J saying:
    ```Failed to load class org.slf4j.impl.StaticLoggerBinder```
    This is an innocuous warning.  Please see the README file for
    more details.


### Version 7.1.2.1 - 2021-01-26

#### Fixed
-   An issue with `BulkInserter` flush when retryCount > 0


### Version 7.1.2.0 - 2021-01-25

#### Performance Enhancements
-   Converted the `BulkInserter` flushing mechanism from single-threaded
    to parallel-threaded.

#### Notes
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.


### Version 7.1.1.0 - 2020-10-28

#### Notes
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.


### Version 7.1.0.1 - 2020-09-29

#### Added
-   Another `ping()` method that takes in a timeout as a second parameter.

#### Changed
-   `GPUdb` constructor behavior such that if the server at the user given IP
    address responds with public IPs that the client application environment
    cannot acces, the `GPUdb` object will be created with the user given IP
    addresses, completely disregarding the public addresses given by the
    server.  The side-effect of this that the API's failover mechanism
    must be disabled; this is logged as a warning.


### Version 7.1.0.0 - 2020-08-18

#### Added
-   Support for intra-cluster, also known as N+1, failover.
-   Support for logging.
-   `GPUdb.Options` options (solely handled by getters and setters):
    -   clusterReconnectCount -- The number of times the API tries to
                                 reconnect to the same cluster (when a
                                 failover event has been triggered), before
                                 actually failing over to any available backup
                                 cluster.  Does not apply when only a single
                                 cluster is available.  Default is 1.
    -   disableFailover -- Indicates whether to disable failover upon failures
                           (both high availability--or inter-cluster--failover
                           and N+1--or intra-cluster--failover).  Default false.
    -   disableAutoDiscovery -- Indicates whether to disable automatic discovery
                                of backup clusters or worker rank URLs.  If set
                                to true, then the GPUdb object will not connect
                                to the database at initialization time, and will
                                only work with the URLs given.  Default is false.
    -   haFailoverOrder -- The order of choosing backup clusters in the event of
                           high availability failover.  Default is
                           GPUdb.HAFailoverOrder.RANDOM.
    -   hostnameRegex -- A regular expression to apply to all automatically
                         discovered URLs for the Kinetica servers.  No default.
    -   initialConnectionAttemptTimeout -- The timeout used when trying to
                                           establish a connection to the database
                                           at GPUdb initialization.  The value is
                                           given in milliseconds. The default is
                                           0, which prevents any retry and stores
                                           the user given URLs as is.
    -   intraClusterFailoverRetryCount -- The number of times the API tries to
                                          recover during an intra-cluster (N+1)
                                          failover scenario.  This positive
                                          integer determines how many times all
                                          known ranks will be queried before
                                          giving up (in the first of two stages
                                          of recovery process).  The default is 3.
    -   intraClusterFailoverTimeout -- The amount of time the API tries to
                                       recover during an intra-cluster (N+1)
                                       failover scenario.  Given in milliseconds.
                                       Default is 0 (infinite). This time interval
                                       spans both stages of the N+1 failover
                                       recovery process.
    -   loggingLevel -- The logging level to use for the API.  By default,
                        logging is turned off.  If logging properties are set up
                        by the user (via log4j.properties etc.), then that will
                        be honored only if the default logging level is used.
                        Otherwise, the programmatically set level will be used.
-   Added class `GPUdb.ClusterAddressInfo` which contains information about a
    given Kinetica cluster, including rank URLs and hostnames.
-   `GPUdb` methods:
    -   `getHARingInfo()`
    -   `getHARingSize()`
    -   `getPrimaryHostName()`

#### Changed
-   `BulkInserter` default retry count to 1 (from 0).

#### Deprecated
-   `GPUdb.setHostManagerPort(int)` method.  The user must set the host manager
    at `GPUdb` initialization; changing the host manager port will not be
    permitted post-initialization.  The method is now a no-op (until removed
    in 7.2 or a later version).



## Version 7.0

### Version 7.0.20.10 - 2023-12-06

#### Changed
-   Upgraded Avro library from 1.11.1 to 1.11.3
-   Upgraded Snappy library from 1.1.1.3 to 1.1.10.4
-   Upgraded Logback library from 1.2.10 to 1.2.13


### Version 7.0.20.9 - 2022-11-30

#### Added
-   Improved reporting of permissions errors during `BulkInserter` insert


### Version 7.0.20.8 - 2022-09-08

#### Changed
-   Updated Avro version to 1.11.1


### Version 7.0.20.7 - 2022-07-28

#### Changed
-   Improved error reporting of environment-related issues during inserts

#### Fixed
-   Logger initialization issue


### Version 7.0.20.6 - 2022-07-25

#### Fixed
-   Issue with thread over-accumulation when inserting data


### Version 7.0.20.5 - 2022-06-08

#### Security
-   Updated the following dependency package version to eliminate known
    security risks and other issues:
    -  org.apache.avro     1.10.1  -> 1.11.0


### Version 7.0.20.4 - 2021-02-02

#### Added
-   Class `GPUdb.GPUdbVersion` that represents the Kinetica server's
    version (the one the API is connected to).
-   Method `GPUdb.getServerVersion()`.

#### Changed
-   Added support for multi-head key lookup for replicated tables.


### Version 7.0.20.3 - 2021-01-29

#### Security
-   Updated the following dependency package versions to eliminate known
    security risks and other issues:
    -  org.apache.avro     1.8.1  -> 1.10.1
    -  commons-codec:      1.10   -> 1.13
    -  httpclient:         4.5.11 -> 4.5.13
    -  maven-shade-plugin: 2.1    -> 3.2.4

#### Notes
-   Due to the dependency updates, applications using this API may
    start getting a warning log from SLF4J saying:
    ```Failed to load class org.slf4j.impl.StaticLoggerBinder```
    This is an innocuous warning.  Please see the README file for
    more details.


### Version 7.0.20.2 - 2021-01-26

#### Fixed
-   An issue with `BulkInserter` flush when retryCount > 0


### Version 7.0.20.1 - 2020-12-23

#### Performance Enhancements
-   Converted the `BulkInserter` flushing mechanism from single-threaded
    to parallel-threaded.


### Version 7.0.20.0 - 2020-11-25

#### Notes
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.


### Version 7.0.19.3 - 2021-02-01

#### Added
-   Class `GPUdb.GPUdbVersion` that represents the Kinetica server's
    version (the one the API is connected to).
-   Method `GPUdb.getServerVersion()`.


#### Changed
-   Added support for multi-head key lookup for replicated tables.


### Version 7.0.19.2 - 2021-01-26

#### Fixed
-   An issue with `BulkInserter` flush when retryCount > 0


### Version 7.0.19.1 - 2020-12-23

#### Performance Enhancements
-   Converted the `BulkInserter` flushing mechanism from single-threaded
    to parallel-threaded.


### Version 7.0.19.0 - 2020-08-24

#### Notes
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.


### Version 7.0.18.1 - 2020-07-29

#### Added
-   `GPUdb.Options` member `connectionInactivityValidationTimeout` which controls
    the period of inactivity after which a connection would be checked
    for inactivity or stale-ness before leasing to a client.  The value is given
    in milliseconds.  The default value is 200 ms.  Note that this is for
    fine-tuning the connection manager, and should be used with deep
    understanding of how connections are managed.  The default value would
    likely suffice for most users; we're just letting the user have the control,
    if they want it.

#### Changed
-   The default value of `GPUdb.Options` member `serverConnectionTimeout` to `10000`
    (equivalent to 10 seconds).
-   The default value of `GPUdb.Options` member `maxConnectionsPerHost` to `10`.


### Version 7.0.18.0 - 2020-07-28

#### Note
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.


### Version 7.0.17.1 - 2020-07-14

#### Added
-   `GPUdb.Options` member `serverConnectionTimeout` which controls the
    server connection timeout (not the request timeout).  The
    value is given in milliseconds.  The default value is 3 seconds
    (3000).


### Version 7.0.17.0 - 2020-07-01

#### Note
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.


### Version 7.0.16.0 - 2020-05-28

#### Note
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.


### Version 7.0.15.1 - 2020-05-02

#### Fixed
-   Socket connection timeout--now check IP/hostname availability
    for 1 second (instead of applying the user given timeout--default
    infinite--which resulted in a few minutes of hanging for bad addresses).
-   Set host manager endpoint retry count to 3 (not configurable) so that
    the API does not go into an infinite loop for a bad user given host
    manager port.


### Version 7.0.15.0 - 2020-04-27

#### Note
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.


### Version 7.0.14.0 - 2020-03-25

#### Note
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.


### Version 7.0.13.0 - 2020-03-10

#### Note
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.


### Version 7.0.12.1 - 2020-03-04

#### Added
-   Options for configuring the maximum allowed number of connections:
    -   `GPUdb.Options.maxTotalConnections` (across all hosts; default 40)
    -   `GPUdb.Options.maxConnectionsPerHost` (for any given host; default 40)

#### Fixed
-   Improved connection throughput over SSL.


### Version 7.0.12.0 - 2020-01-17

#### Note
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.


### Version 7.0.9.0 - 2019-10-28

#### Added
-   Support for high-availability failover when the database is in the
    offline mode.

#### Changed
-   `GPUdb` constructor behavior--if a single URL is used and no primary URL
    is specified via the options, the given single URL will be treated as
    the primary URL.


### Version 7.0.8.2 - 2019-10-25

#### Fixed
-   Multi-head insertion high-availability failover issue when retryCount > 0


### Version 7.0.8.1 - 2019-10-21

#### Fixed
-   Multi-head I/O high-availability failover thread-safety issues


### Version 7.0.7.2 - 2019-10-03

#### Fixed
-   Multi-head I/O high-availability failover issue when a worker rank dies.


### Version 7.0.7.1 - 2019-09-11

#### Added
-   An option to `GPUdb.Options` for bypassing SSL certificate verification
    for HTTPS connections.  Obtained by and set by `Options.getBypassSslCertCheck()`
    and `Options.setBypassSslCertCheck(boolean)` methods.


### Version 7.0.7.0 - 2019-08-28

#### Added
-   Support for adding and removing custom headers to the `GPUdb` object.  See
    methods:
    -   `GPUdb.addHttpHeader(String, String)`
    -   `GPUdb.removeHttpHeader(String)`
-   Support for new column property `ulong` to multi-head I/O.  ***Compatible
    with Kinetica Server version 7.0.7.0 and later only.***

#### Fixed
-   A stack overflow bug in an edge case of high availability failover for
    multi-head ingestion.

#### Server Version Compatibilty
-   Kinetica 7.0.7.0 and later


### Version 7.0.6.1 - 2019-08-13

#### Changed
-   Added support for high availability failover when the system is limited
    (in addition to connection problems).  ***Compatible with Kinetica Server
    version 7.0.6.2 and later only.***

#### Server Version Compatibilty
-   Kinetica 7.0.6.2 and later


### Version 7.0.6.0 - 2019-08-05

#### Added
-   Support for passing `/get/records` options to `RecordRetriever`; can be set
    via the constructors and also be set by the setter method.
-   Support for overriding the high availability synchronicity mode for
    endpoints; set the mode (enum `HASynchronicityMode`) with the setter
    method `setHASyncMode()`:
    - `DEFAULT`
    - `SYNCHRONOUS`
    - `ASYNCRHONOUS`
-   Enumerations, `Type.Column.ColumnType` and `Type.Column.ColumnBaseType`,
    to indicate a column's type.  Use getters `Type.Column.getColumnType()`
    and `Type.Column.getColumnBaseType()` to obtain the appropriate enumeration.
    This is more efficient than checking for strings in the column's property
    list or checking for Java class equivalency.

#### Changed
-   Error message format when endpoint submission fails altogether (whether
    no connection can be made or if the database returns some error).


### Version 7.0.5.0 - 2019-07-21

#### Added
-   A `putDateTime` method to `GenericRecord` that parses string values
    with a variety of different date, time, and datetime formats
    and converts them to the appropriate Kinetica format for the column's type.
    Of the accepteble formats, the date component can be any of YMD, MDY, or
    DMY pattern with '-', '.', or '/' as the separator.  And, the time component
    (optional for both date and datetime, but required for time) must have hours
    and minutes, but can optionally have seconds, fraction of a second (up to six
    digits) and some form of a timezone identifier.


### Version 7.0.4.0 - 2019-06-26

#### Added
-   Minor documentation and some options for some endpoints

#### Changed
-   Parameters for `/visualize/isoschrone`


### Version 7.0.3.0 - 2019-05-07

#### Added
-   Support for high availability (HA) to multi-head ingestion
    and retrieval

#### Changed
-   Error messages to include the original error message when Kinetica
    is unavailable and other available HA ring clusters have been tried
    (and failed).


### Version 7.0.2.0 - 2019-04-05
-   Added support for selecting a primary host for the `GPUdb` class


### Version 7.0.1.1 - 2019-04-02
-   Added missing types for `Type.fromDynamicSchema()`:
    --  `datetime`
    --  `geometry` (mapped to `wkt`)
-   Added method `hasProperty(String)` to `Type.Column`; provides a convenient
    functionality to check if a given column property applies to the given
    column.


### Version 7.0.1.0 - 2019-03-11
-   Added support for comma-separated URLs for the `GPUdb` constructor that
    takes a string.


### Version 7.0.0.2 - 2019-02-26
-   Added a new column property: `INIT_WITH_NOW`


### Version 7.0.0.1 - 2019-02-08
-   Added support for high availability (HA) failover logic to the
    `GPUdb` class


### Version 7.0.0.0 - 2019-01-31
-   Added support for cluster reconfiguration to the multi-head I/O operations



## Version 6.2

### Version 6.2.4.0 - 2019-09-05

#### Added
-   An option to `GPUdb.Options` for bypassing SSL certificate verification
    for HTTPS connections.  Obtained by and set by `Options.getBypassSslCertCheck()`
    and `Options.setBypassSslCertCheck(boolean)` methods.


### Version 6.2.3.0 - 2019-08-01

#### Added
-   Support for overriding the high availability synchronicity mode for
    endpoints; set the mode (enum `HASynchronicityMode`) with the setter
    method `setHASyncMode()`:
    - `DEFAULT`
    - `SYNCHRONOUS`
    - `ASYNCRHONOUS`
-   Enumerations, `Type.Column.ColumnType` and `Type.Column.ColumnBaseType`,
    to indicate a column's type.  Use getters `Type.Column.getColumnType()`
    and `Type.Column.getColumnBaseType()` to obtain the appropriate enumeration.
    This is more efficient than checking for strings in the column's property
    list or checking for Java class equivalency.


### Version 6.2.2.0 - 2019-07-20

#### Added
-   A `putDateTime` method to `GenericRecord` that parses string values
    with a variety of different date, time, and datetime formats
    and converts them to the appropriate Kinetica format for the column's type.
    Of the accepteble formats, the date component can be any of YMD, MDY, or
    DMY pattern with '-', '.', or '/' as the separator.  And, the time component
    (optional for both date and datetime, but required for time) must have hours
    and minutes, but can optionally have seconds, fraction of a second (up to six
    digits) and some form of a timezone identifier.


### Version 6.2.1.1 - 2019-03-30
-   Added avro shading to the package
-   Added missing types for `Type.fromDynamicSchema()`:
    --  `datetime`
    --  `geometry` (mapped to `wkt`)
-   Added method `hasProperty(String)` to `Type.Column`; provides a convenient
    functionality to check if a given column property applies to the given
    column.


### Version 6.2.0 - 2018-09-26

-   New `RecordRetriever` class to support multi-head record lookup by
    shard key
-   `BulkInserter.WorkerList` class deprecated in favor of top-level
    `WorkerList` class used by both `BulkInserter` and `RecordRetriever`
-   Added support for host manager endpoints
-   Added member dataType to the response protocol classes that return
    a dynamically generated table.  Currently, that includes:
    -   `AggregateGroupByResponse`
    -   `AggregateUniqueResponse`
    -   `AggregateUnpivotResponse`
    -   `GetRecordsByColumnResponse`



## Version 6.1.0 - 2017-10-05

-   Improved request submission logic to be faster and use less memory



## Version 6.0.0 - 2017-01-24

-   Version release



## Version 5.4.0 - 2016-11-29

-   Version release



## Version 5.2.0 - 2016-10-12

-   Record objects now support complex column names (expressions, multipart
    join names, etc.)
-   Record objects now support access via a Map interface via `getDataMap()`
-   Can now pass arbitrary additional HTTP headers to GPUdb
-   Added nullable column support



## Version 5.1.0 - 2016-05-06

-   Updated documentation generation



## Version 4.2.0 - 2016-04-11

-   Refactor generation of the APIs
-   Added an example package
