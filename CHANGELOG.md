# GPUdb Java API Changelog

## Version 7.1

### Version 7.1.8.5 - 2022-11-02

#### Added
-   getWarnings() method on BulkInserter

#### Changed
-   BulkInserter.getErrors() now only returns errors and not warnings -- use getWarnings() for warnings


### Version 7.1.8.4 - 2022-10-26

#### Changed
-   Improved performance of BulkInserter & RecordRetriever key generation


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


### Version 7.0.13.0 - TBD

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
