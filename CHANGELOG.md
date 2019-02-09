# GPUdb Java API Changelog

## Version 7.0.0

### Version 7.0.0.1 - 2019-02-08
-   Added support for high availability (HA) failover logic to the
    GPUdb class


### Version 7.0.0.0 - 2019-01-31
-   Added support for cluster reconfiguration to the multi-head I/O operations



## Version 6.2.0 - 2018-09-26

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
