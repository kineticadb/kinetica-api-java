package com.gpudb.example;

import com.gpudb.BulkInserter;
import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.RecordObject;
import com.gpudb.Type;

import com.gpudb.protocol.AggregateGroupByResponse;
import com.gpudb.protocol.AggregateHistogramResponse;
import com.gpudb.protocol.AggregateStatisticsResponse;
import com.gpudb.protocol.AggregateUniqueResponse;
import com.gpudb.protocol.CreateTableRequest;
import com.gpudb.protocol.FilterResponse;
import com.gpudb.protocol.FilterByListResponse;
import com.gpudb.protocol.FilterByRangeResponse;
import com.gpudb.protocol.GetRecordsRequest;
import com.gpudb.protocol.GetRecordsResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;



public class Example
{
    public static class MyType extends RecordObject
    {
	// Fields and their properties
	@RecordObject.Column(order = 0, properties = { "data" })
	public double col1;

	@RecordObject.Column(order = 1, properties = { "data" })
	public String col2;

	@RecordObject.Column(order = 2, properties = { "data" })
	public String group_id;

	private MyType() {}
    } // end class MyType


    public static void main(String[] args) throws GPUdbException
    {
	// Establish a connection with a locally running instance of GPUdb
        GPUdb gpudb = new GPUdb("http://localhost:9191");

	// Register the desired data type with GPUdb
	Type type = RecordObject.getType( MyType.class );
	// The type ID returned by GPUdb is needed to create a table later
	String type_id = type.create( gpudb );
	System.out.println( "Type id of newly created type: " + type_id + "\n" );

	// Column names (used in queries)
	String col1 = "col1";
	String col2 = "col2";
	String group_id = "group_id";

	// Create a table with 'MyType' data type
	String table_name = "my_table_1";
	Map<String, String> create_table_options = GPUdb.options( CreateTableRequest.Options.NO_ERROR_IF_EXISTS,
								  CreateTableRequest.Options.TRUE );
	gpudb.createTable( table_name, type_id, create_table_options );

	int numRecords = 10;

	// Utilize a convenience functions for inserting records in batches
	BulkInserter<MyType> bulkInserter = new BulkInserter<MyType>( gpudb,
								      table_name,
								      type,
								      numRecords,
								      null );

	// Generate data to be inserted into the table
	for (int i = 0; i < numRecords; i++)
	{
	    MyType record = new MyType();
	    record.put( 0, (i + 0.1) ); // col1
	    record.put( 1, ("string " + String.valueOf( i ) ) ); // col2
	    record.put( 2, "Group 1" );  // group_id
	    bulkInserter.insert( record );
	}  // done generating the objects

	// To actually insert the records, flush the bulk inserter object.
	bulkInserter.flush();

	// Retrieve the inserted records
	Map<String, String> blank_options = new LinkedHashMap<String, String>();
	GetRecordsRequest getRecordsReq = new GetRecordsRequest( table_name,
								 0, numRecords,
								 blank_options );
	GetRecordsResponse getRecordsRsp;
	getRecordsRsp = gpudb.getRecords( getRecordsReq );
	System.out.println( "Returned records: " + getRecordsRsp.getData() + "\n" );

	// Perform a filter calculation on the table
	FilterResponse filterRsp;
	String view_name = "view1";
	String expression = "col1 = 1.1";
	filterRsp = gpudb.filter( table_name, view_name,
				  expression, blank_options );
	System.out.println( "Number of records returned by the filter expression: "
			    + filterRsp.getCount() + "\n" );

	// Retrieve the filtered records (the retrieval method is the same
	// as that from a regular table)
	GetRecordsResponse filteredRecordsRsp;
	filteredRecordsRsp = gpudb.getRecords( view_name,
					       0, 100, blank_options );
	System.out.println( "Filtered records (" + expression + ") :" + filteredRecordsRsp.getData() + "\n" );
	
	// Drop the view
	gpudb.clearTable( view_name, "", blank_options );

	// Perform another filter calculation on the table
	String expression_2 = "(col1 <= 9) and (group_id='Group 1')";
	filterRsp = gpudb.filter( table_name, view_name,
				  expression_2, blank_options );
	System.out.println( "Number of records returned by the second filter expression (" + expression + ") :"
			    + filterRsp.getCount() + "\n" );

	// Retrieve the filtered records (the retrieval method is the same
	// as that from a regular table)
	filteredRecordsRsp = gpudb.getRecords( view_name,
					       0, 100, blank_options );
	System.out.println( "Filtered records: " + filteredRecordsRsp.getData() + "\n" );

	// Perform a filter by list calculation on the table
	FilterByListResponse filterByListRsp;
	String view_name_2 = "view2";
	// Set up the search criteria: for col1, look for values
	// '1.1', '2.1', and '5.1'
	Map<String, List<String>> columnValuesMap = new LinkedHashMap<String, List<String>>();
	List<String> values = new ArrayList<String>();
	values.add( "1.1" );
	values.add( "2.1" );
	values.add( "5.1" );
	columnValuesMap.put( col1, values );
	filterByListRsp = gpudb.filterByList( table_name, view_name_2,
					      columnValuesMap, blank_options );
	System.out.println( "Number of records returned by the filter by list expression: "
			    + filterByListRsp.getCount() + "\n" );

	// Retrieve the filtered (by list) records
	filteredRecordsRsp = gpudb.getRecords( view_name_2,
					       0, 100, blank_options );
	System.out.println( "Filtered (by list) records: "
			    + filteredRecordsRsp.getData() + "\n" );


	// Perform a filter by range calculation on the table
	FilterByRangeResponse filterByRangeRsp;
	String view_name_3 = "view3";
	filterByRangeRsp = gpudb.filterByRange( table_name, view_name_3,
						col1, 1, 5,
						blank_options );
	System.out.println( "Number of records returned by the filter by range expression: "
			    + filterByRangeRsp.getCount() + "\n" );

	// Retrieve the filtered (by list) records
	filteredRecordsRsp = gpudb.getRecords( view_name_3,
					       0, 100, blank_options );
	System.out.println( "Filtered (by range) records: "
			    + filteredRecordsRsp.getData() + "\n" );

	// Perform an aggregate (statistics) operation
	AggregateStatisticsResponse aggStatsRsp;
	aggStatsRsp = gpudb.aggregateStatistics( table_name,
						 col1,
						 "count,sum,mean",
						 blank_options );
	System.out.println( "Statistics of values in 'col1': "
			    + aggStatsRsp.getStats() + "\n" );

	// Generate more data to be inserted into the table
	int numRecords2 = 8;
	for (int i = 1; i < numRecords2; i++)
	{
	    MyType record = new MyType();
	    record.put( 0, (i + 10.1) ); // col1
	    // 'col2' values are NOT unique from the first group of records
	    record.put( 1, ("string " + String.valueOf( i ) ) ); // col2
	    record.put( 2, "Group 2" );  // group_id
	    bulkInserter.insert( record );
	}  // done generating the objects

	// Actually insert the records
	bulkInserter.flush();


	// Find unique values in a column
	AggregateUniqueResponse uniqueRsp;
	uniqueRsp = gpudb.aggregateUnique( table_name,
					   group_id,
					   0, 30,
					   blank_options );
	System.out.println( "Unique values in the '" + group_id + "' column: "
			    + uniqueRsp.getData() + "\n" );


	// Perform a group by aggregation (on column 'col2')
	AggregateGroupByResponse groupByRsp;
	List<String> columns = new ArrayList<String>();
	columns.add( col2 );
	groupByRsp = gpudb.aggregateGroupBy( table_name,
					     columns,
					     0, 1000,
					     blank_options );
	System.out.println( "Group by results for the '" + col2 + "' column: "
			    + groupByRsp.getData() + "\n" );

	// Perform another group by aggregation on:
	//  * column 'group_id'
	//  * count of all
	//  * sum of 'col1'
	columns.clear();
	columns.add( group_id );
	columns.add( "count(*)" );
	columns.add( "sum(" + col1 + ")" );
	groupByRsp = gpudb.aggregateGroupBy( table_name,
					     columns,
					     0, 1000,
					     blank_options );
	System.out.println( "Second group by results: "
			    + groupByRsp.getData() + "\n" );


	// Perform another group by aggregation operation
	columns.clear();
	columns.add( group_id );
	columns.add( "sum(" + col1 + "*10)" );
	groupByRsp = gpudb.aggregateGroupBy( table_name,
					     columns,
					     0, 1000,
					     blank_options );
	System.out.println( "Third group by results: "
			    + groupByRsp.getData() + "\n" );


	// Add more data to the table
	int numRecords3 = 10;
	for (int i = 4; i < numRecords3; i++)
	{
	    MyType record = new MyType();
	    record.put( 0, (i + 0.6) ); // col1
	    // 'col2' values are NOT unique from the first group of records
	    record.put( 1, ("string 2" + String.valueOf( i ) ) ); // col2
	    record.put( 2, "Group 1" );  // group_id
	    bulkInserter.insert( record );
	}  // done generating the objects

	// Actually insert the records
	bulkInserter.flush();


	// Do a histogram on the data
	AggregateHistogramResponse histogramRsp;
	double start = 1.1;
	double end = 2;
	double interval = 1;
	histogramRsp = gpudb.aggregateHistogram( table_name,
						 col1,
						 start, end,
						 interval,
						 blank_options );
	System.out.println( "Histogram counts: " + histogramRsp.getCounts() + "\n" );


	// Drop the table
	gpudb.clearTable( table_name, "", blank_options );

	// Check that dropping a table automatically drops all the
	// dependent views
	try
	{
	    getRecordsRsp = gpudb.getRecords( view_name_3,
					      0, 100, blank_options );
	    System.out.println( "Error: Dropping original table did NOT drop all views!\n" );
	} catch (GPUdbException e)
	{
	    System.out.println( "Dropping original table dropped all views as expected.\n" );
	}

    } // end main




} // end class Example



/*
Output:
=======

Type id of newly created type: 6513146016266117570

Returned records: [{"col1": 0.1, "col2": "string 0", "group_id": "Group 1"}, {"col1": 1.1, "col2": "string 1", "group_id": "Group 1"}, {"col1": 2.1, "col2": "string 2", "group_id": "Group 1"}, {"col1": 3.1, "col2": "string 3", "group_id": "Group 1"}, {"col1": 4.1, "col2": "string 4", "group_id": "Group 1"}, {"col1": 5.1, "col2": "string 5", "group_id": "Group 1"}, {"col1": 6.1, "col2": "string 6", "group_id": "Group 1"}, {"col1": 7.1, "col2": "string 7", "group_id": "Group 1"}, {"col1": 8.1, "col2": "string 8", "group_id": "Group 1"}, {"col1": 9.1, "col2": "string 9", "group_id": "Group 1"}]

Number of records returned by the filter expression: 1

Filtered records (col1 = 1.1) :[{"col1": 1.1, "col2": "string 1", "group_id": "Group 1"}]

Number of records returned by the second filter expression (col1 = 1.1) :9

Filtered records: [{"col1": 0.1, "col2": "string 0", "group_id": "Group 1"}, {"col1": 1.1, "col2": "string 1", "group_id": "Group 1"}, {"col1": 2.1, "col2": "string 2", "group_id": "Group 1"}, {"col1": 3.1, "col2": "string 3", "group_id": "Group 1"}, {"col1": 4.1, "col2": "string 4", "group_id": "Group 1"}, {"col1": 5.1, "col2": "string 5", "group_id": "Group 1"}, {"col1": 6.1, "col2": "string 6", "group_id": "Group 1"}, {"col1": 7.1, "col2": "string 7", "group_id": "Group 1"}, {"col1": 8.1, "col2": "string 8", "group_id": "Group 1"}]

Number of records returned by the filter by list expression: 3

Filtered (by list) records: [{"col1": 1.1, "col2": "string 1", "group_id": "Group 1"}, {"col1": 2.1, "col2": "string 2", "group_id": "Group 1"}, {"col1": 5.1, "col2": "string 5", "group_id": "Group 1"}]

Number of records returned by the filter by range expression: 4

Filtered (by range) records: [{"col1": 1.1, "col2": "string 1", "group_id": "Group 1"}, {"col1": 2.1, "col2": "string 2", "group_id": "Group 1"}, {"col1": 3.1, "col2": "string 3", "group_id": "Group 1"}, {"col1": 4.1, "col2": "string 4", "group_id": "Group 1"}]

Statistics of values in 'col1': {count=10.0, sum=46.0, mean=4.6}

Unique values in the 'group_id' column: [{"group_id": "Group 1"}, {"group_id": "Group 2"}]

Group by results for the 'col2' column: [{"col2": "string 0", "column_1": 1.0}, {"col2": "string 1", "column_1": 2.0}, {"col2": "string 2", "column_1": 2.0}, {"col2": "string 3", "column_1": 2.0}, {"col2": "string 4", "column_1": 2.0}, {"col2": "string 5", "column_1": 2.0}, {"col2": "string 6", "column_1": 2.0}, {"col2": "string 7", "column_1": 2.0}, {"col2": "string 8", "column_1": 1.0}, {"col2": "string 9", "column_1": 1.0}]

Second group by results: [{"group_id": "Group 1", "column_1": 10.0, "column_2": 46.0}, {"group_id": "Group 2", "column_1": 7.0, "column_2": 98.69999999999999}]

Third group by results: [{"group_id": "Group 1", "column_1": 460.0}, {"group_id": "Group 2", "column_1": 987.0}]

Histogram counts: [1.0]

Dropping original table dropped all views as expected.

*/


