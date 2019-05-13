# GPUdb Java API Changelog

## Version 7.0

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
