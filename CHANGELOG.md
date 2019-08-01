# GPUdb Java API Changelog

## Version 6.2

### Version 6.2.3.0 - 2019-08-01

#### Added
-   Support for overriding the high availability synchronicity mode for
    endpoints; set the mode (enum HASynchronicityMode) with the setter
    method setHASyncMode():
    - DEFAULT
    - SYNCHRONOUS
    - ASYNCRHONOUS
-   Enumeration, Type.Column.ColumnType, to indicate a column's type.
    Use getter Type.Column.getColumnType() to obtain it.  This is more
    efficient than checking for strings in the column's property list.


### Version 6.2.2.0 - 2019-07-20

#### Added
-   A 'putDateTime' method to GenericRecord that parses string values
    with a variety of different date, time, and datetime formats
    and converts them to the appropriate Kinetica format for the column's type.
    Of the accepteble formats, the date component can be any of YMD, MDY, or
    DMY pattern with '-', '.', or '/' as the separator.  And, the time component
    (optional for both date and datetime, but required for time) must have hours
    and minutes, but can optionally have seconds, fraction of a second (up to six
    digits) and some form of a timezone identifier.


### Version 6.2.1.1 - 2019-03-30
-   Added avro shading to the package
-   Added missing types for Type.fromDynamicSchema():
    --  datetime
    --  geometry (mapped to wkt)
-   Added method hasProperty(String) to Type.Column; provides a convenient
    functionality to check if a given column property applies to the given
    column.

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
