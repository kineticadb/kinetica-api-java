# GPUdb Java API Changelog

## Version 7.0

### Version 7.0.13.0 - 2020-03-10

#### Note
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.


### Version 7.0.12.1 - 2020-03-04

#### Added
-   Options for configuring the maximum allowed number of connections:
    -   GPUdb.Options.maxTotalConnections (across all hosts; default 40)
    -   GPUdb.Options.maxConnectionsPerHost (for any given host; default 40)


#### Bugfix
-   Improved connection throughput over SSL.


### Version 7.0.12.0 - 2020-01-17

#### Note
-   Check CHANGELOG-FUNCTIONS.md for endpoint related changes.


### Version 7.0.9.0 - 2019-10-28

#### Added
-   Support for high-availability failover when the database is in the
    offline mode.

#### Changed
-   GPUdb constructor behavior--if a single URL is used and no primary URL
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
-   An option to GPUdb.Options for bypassing SSL certificate verification
    for HTTPS connections.  Obtained by and set by Options.getBypassSslCertCheck()
    and Options.setBypassSslCertCheck(boolean) methods.


### Version 7.0.7.0 - 2019-08-28

#### Added
-   Support for adding and removing custom headers to the GPUdb object.  See
    methods:
    -   GPUdb.addHttpHeader(String, String)
    -   GPUdb.removeHttpHeader(String)
-   Support for new column property 'ulong' to multi-head I/O.  ***Compatible
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
-   Support for passing /get/records options to RecordRetriever; can be set
    via the constructors and also be set by the setter method.
-   Support for overriding the high availability synchronicity mode for
    endpoints; set the mode (enum HASynchronicityMode) with the setter
    method setHASyncMode():
    - DEFAULT
    - SYNCHRONOUS
    - ASYNCRHONOUS
-   Enumerations, Type.Column.ColumnType and Type.Column.ColumnBaseType,
    to indicate a column's type.  Use getters Type.Column.getColumnType()
    and Type.Column.getColumnBaseType() to obtain the appropriate enumeration.
    This is more efficient than checking for strings in the column's property
    list or checking for Java class equivalency.


#### Changed
-   Error message format when endpoint submission fails altogether (whether
    no connection can be made or if the database returns some error).


### Version 7.0.5.0 - 2019-07-21

#### Added
-   A 'putDateTime' method to GenericRecord that parses string values
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
-   Parameters for /visualize/isoschrone


### Version 7.0.3.0 - 2019-05-07

#### Added
-   Support for high availability (HA) to multi-head ingestion
    and retrieval

#### Changed
-   Error messages to include the original error message when Kinetica
    is unavailable and other available HA ring clusters have been tried
    (and failed).


### Version 7.0.2.0 - 2019-04-05
-   Added support for selecting a primary host for the GPUdb class


### Version 7.0.1.1 - 2019-04-02
-   Added missing types for Type.fromDynamicSchema():
    --  datetime
    --  geometry (mapped to wkt)
-   Added method hasProperty(String) to Type.Column; provides a convenient
    functionality to check if a given column property applies to the given
    column.


### Version 7.0.1.0 - 2019-03-11
-   Added support for comma-separated URLs for the GPUdb constructor that
    takes a string.


### Version 7.0.0.2 - 2019-02-26
-   Added a new column property: INIT_WITH_NOW


### Version 7.0.0.1 - 2019-02-08
-   Added support for high availability (HA) failover logic to the
    GPUdb class


### Version 7.0.0.0 - 2019-01-31
-   Added support for cluster reconfiguration to the multi-head I/O operations


## Version 6.2

### Version 6.2.0 - 2018-09-26

-   New RecordRetriever class to support multi-head record lookup by
    shard key
-   BulkInserter.WorkerList class deprecated in favor of top-level
    WorkerList class used by both BulkInserter and RecordRetriever
-   Added support for host manager endpoints
-   Added member dataType to the response protocol classes that return
    a dynamically generated table.  Currently, that includes:
    -   AggregateGroupByResponse
    -   AggregateUniqueResponse
    -   AggregateUnpivotResponse
    -   GetRecordsByColumnResponse


## Version 6.1.0 - 2017-10-05

-   Improved request submission logic to be faster and use less memory


## Version 6.0.0 - 2017-01-24

-   Version release


## Version 5.4.0 - 2016-11-29

-   Version release


## Version 5.2.0 - 2016-10-12

-   Record objects now support complex column names (expressions, multipart
    join names, etc.)
-   Record objects now support access via a Map interface via getDataMap()
-   Can now pass arbitrary additional HTTP headers to GPUdb
-   Added nullable column support


## Version 5.1.0 - 2016-05-06

-   Updated documentation generation


## Version 4.2.0 - 2016-04-11

-   Refactor generation of the APIs
-   Added an example package
