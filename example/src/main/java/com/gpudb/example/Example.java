package com.gpudb.example;

import com.gpudb.BulkInserter;
import com.gpudb.GPUdb;
import com.gpudb.GPUdbBase;
import com.gpudb.GPUdbException;
import com.gpudb.GPUdbLogger;
import com.gpudb.GenericRecord;
import com.gpudb.Record;
import com.gpudb.RecordObject;
import com.gpudb.Type;
import com.gpudb.protocol.AggregateGroupByRequest;
import com.gpudb.protocol.AggregateGroupByResponse;
import com.gpudb.protocol.AggregateHistogramResponse;
import com.gpudb.protocol.AggregateStatisticsResponse;
import com.gpudb.protocol.AggregateUniqueResponse;
import com.gpudb.protocol.CreateTableRequest;
import com.gpudb.protocol.FilterResponse;
import com.gpudb.protocol.GetRecordsRequest;
import com.gpudb.protocol.FilterByListResponse;
import com.gpudb.protocol.FilterByRangeResponse;
import com.gpudb.protocol.GetRecordsResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;


public class Example
{
	public static class MyType extends RecordObject
	{
		// Fields and their properties
		@RecordObject.Column(order = 0)
		public double col1;

		@RecordObject.Column(order = 1, properties = { "char16" })
		public String col2;

		@RecordObject.Column(order = 2, properties = { "char8" })
		public String group_id;

		private MyType() {}
	} // end class MyType


	public static void main(String[] args) throws GPUdbException
	{
		// Get the URL to use from the command line, or use the default
		String url = System.getProperty("url", "http://127.0.0.1:9191");
		String user = System.getProperty("user", "");
		String pass = System.getProperty("pass", "");
		String logLevel = System.getProperty("logLevel", "INFO");

		GPUdbLogger.setLoggingLevel(logLevel);

		// Establish a connection with a locally running instance of GPUdb
		GPUdb.Options options = new GPUdb.Options();
		if (!"".equals(user))
			options.setUsername(user);
		if (!"".equals(pass))
			options.setPassword(pass);
		options.setBypassSslCertCheck(true);
		GPUdb gpudb = new GPUdb( url, options );

		// Register the desired data type with GPUdb
		Type type = RecordObject.getType( MyType.class );
		// The type ID returned by GPUdb is needed to create a table later
		String typeId = type.create( gpudb );

		// Create a table with 'MyType' data type
		String tableName = "my_table";
		Map<String, String> ctOpts = GPUdbBase.options(
				CreateTableRequest.Options.NO_ERROR_IF_EXISTS,
				CreateTableRequest.Options.TRUE
		);
		gpudb.createTable( tableName, typeId, ctOpts );

		int numRecords = 10;

		try (BulkInserter<MyType> bulkInserter = new BulkInserter<>(gpudb, tableName, type, numRecords, null))
		{
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
			Map<String,String> grOpts = GPUdbBase.options(GetRecordsRequest.Options.SORT_BY, "col1");
			GetRecordsResponse<GenericRecord> grResp = gpudb.getRecords(tableName, 0, numRecords, grOpts);
			System.out.println( "Returned records: " );
			for (GenericRecord record : grResp.getData())
				System.out.println("* " + record);
			System.out.println();

			// Perform a filter calculation on the table
			String viewName = "view1";
			String expression = "col1 = 1.1";
			FilterResponse fResp = gpudb.filter( tableName, viewName, expression, null );
			System.out.println( "Number of records returned for filter expression <" + expression + ">: " + fResp.getCount() + "\n" );

			// Retrieve the filtered records (the retrieval method is the same
			// as that from a regular table)
			grResp = gpudb.getRecords( viewName, 0, 100, grOpts );
			System.out.println( "Filtered (by expression) record(s):" );
			for (GenericRecord record : grResp.getData())
				System.out.println("* " + record);
			System.out.println();

			// Drop the view
			gpudb.clearTable( viewName, "", null );

			// Perform another filter calculation on the table
			expression = "(col1 <= 9) and (group_id = 'Group 1')";
			fResp = gpudb.filter( tableName, viewName, expression, null );
			System.out.println( "Number of records returned for second filter expression <" + expression + ">: " + fResp.getCount() + "\n" );

			// Retrieve the filtered records (the retrieval method is the same
			// as that from a regular table)
			grResp = gpudb.getRecords( viewName, 0, 100, grOpts );
			System.out.println( "Filtered (by expression) record(s):" );
			for (GenericRecord record : grResp.getData())
				System.out.println("* " + record);
			System.out.println();

			// Perform a filter by list calculation on the table
			viewName = "view2";
			// Set up the search criteria: for col1, look for values
			// '1.1', '2.1', and '5.1'
			Map<String, List<String>> columnValuesMap = new LinkedHashMap<>();
			List<String> values = new ArrayList<>();
			values.add( "1.1" );
			values.add( "2.1" );
			values.add( "5.1" );
			columnValuesMap.put( "col1", values );
			FilterByListResponse fblResp = gpudb.filterByList( tableName, viewName, columnValuesMap, null );
			System.out.println( "Number of records returned for filter by list expression <" + values + ">: " + fblResp.getCount() + "\n" );

			// Retrieve the filtered (by list) records
			grResp = gpudb.getRecords( viewName, 0, 100, grOpts );
			System.out.println( "Filtered (by list) record(s):" );
			for (GenericRecord record : grResp.getData())
				System.out.println("* " + record);
			System.out.println();

			// Perform a filter by range calculation on the table
			viewName = "view3";
			FilterByRangeResponse fbrResp = gpudb.filterByRange( tableName, viewName, "col1", 1, 5, null );
			System.out.println( "Number of records returned for filter by range expression <col1 1-5>: " + fbrResp.getCount() + "\n" );

			// Retrieve the filtered (by list) records
			grResp = gpudb.getRecords( viewName, 0, 100, grOpts );
			System.out.println( "Filtered (by range) record(s):" );
			for (GenericRecord record : grResp.getData())
				System.out.println("* " + record);
			System.out.println();

			// Perform an aggregate (statistics) operation
			AggregateStatisticsResponse asResp = gpudb.aggregateStatistics( tableName, "col1", "count,sum,mean", null );
			System.out.println( "Statistics of values in <col1>:");
			for (Entry<String, Double> record : asResp.getStats().entrySet())
				System.out.println("* " + record.getKey() + " = " + record.getValue());
			System.out.println();


			// Generate more data to be inserted into the table
			for (int i = 1; i < 8; i++)
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
			AggregateUniqueResponse auResp = gpudb.aggregateUnique( tableName, "group_id", 0, 30, null );
			System.out.println( "Unique values in the <group_id> column: " + auResp.getData().size());
			for (Record record : auResp.getData())
				System.out.println("* " + record.getString("group_id"));
			System.out.println();

			// Perform a group by aggregation over the whole table
			List<String> columns = GPUdbBase.list("COUNT(*)", "SUM(col1)");
			AggregateGroupByResponse agbResp = gpudb.aggregateGroupBy( tableName, columns, 0, 1000, null );
			System.out.println( "Group by results for the <col2> column:" + agbResp.getData());
			for (Record record : agbResp.getData())
				for (int colNum = 0; colNum < columns.size(); colNum++)
					System.out.println("* " + columns.get(colNum) + " = " + record.get(colNum));
			System.out.println();

			// Perform another group by aggregation on the group_id column
			columns = GPUdbBase.list("group_id", "COUNT(*)", "SUM(col1)");
			Map<String, String> agbOpts = GPUdbBase.options(AggregateGroupByRequest.Options.ORDER_BY, "group_id");
			agbResp = gpudb.aggregateGroupBy( tableName, columns, 0, 1000, agbOpts );
			System.out.println( "Group by results for the <group_id> column:");
			for (Record record : agbResp.getData())
			{
				System.out.println("* " + record.getString(0));
				for (int colNum = 1; colNum < columns.size(); colNum++)
					System.out.println("  * " + columns.get(colNum) + " = " + record.get(colNum));
			}
			System.out.println();


			// Add more data to the table
			for (int i = 4; i < 10; i++)
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
			double start = 1.1;
			double end = 2;
			double interval = 1;
			AggregateHistogramResponse ahResp = gpudb.aggregateHistogram( tableName, "col1", start, end, interval, null );
			System.out.println( "Histogram counts: " + ahResp.getCounts() + "\n" );


			// Drop the table
			gpudb.clearTable( tableName, "", null );


			// Check that dropping a table automatically drops all the
			// dependent views
			if (gpudb.hasTable(viewName, null).getTableExists())
				System.out.println( "Error: Dropping original table did NOT drop all views!\n" );
			else
				System.out.println( "Dropping original table dropped all views as expected.\n" );
		}
	} // end main
} // end class Example



/**

Output:
=======

Returned records: 
* {"col1":0.1,"col2":"string 0","group_id":"Group 1"}
* {"col1":1.1,"col2":"string 1","group_id":"Group 1"}
* {"col1":2.1,"col2":"string 2","group_id":"Group 1"}
* {"col1":3.1,"col2":"string 3","group_id":"Group 1"}
* {"col1":4.1,"col2":"string 4","group_id":"Group 1"}
* {"col1":5.1,"col2":"string 5","group_id":"Group 1"}
* {"col1":6.1,"col2":"string 6","group_id":"Group 1"}
* {"col1":7.1,"col2":"string 7","group_id":"Group 1"}
* {"col1":8.1,"col2":"string 8","group_id":"Group 1"}
* {"col1":9.1,"col2":"string 9","group_id":"Group 1"}

Number of records returned for filter expression <col1 = 1.1>: 1

Filtered (by expression) record(s):
* {"col1":1.1,"col2":"string 1","group_id":"Group 1"}

Number of records returned for second filter expression <(col1 <= 9) and (group_id = 'Group 1')>: 9

Filtered (by expression) record(s):
* {"col1":0.1,"col2":"string 0","group_id":"Group 1"}
* {"col1":1.1,"col2":"string 1","group_id":"Group 1"}
* {"col1":2.1,"col2":"string 2","group_id":"Group 1"}
* {"col1":3.1,"col2":"string 3","group_id":"Group 1"}
* {"col1":4.1,"col2":"string 4","group_id":"Group 1"}
* {"col1":5.1,"col2":"string 5","group_id":"Group 1"}
* {"col1":6.1,"col2":"string 6","group_id":"Group 1"}
* {"col1":7.1,"col2":"string 7","group_id":"Group 1"}
* {"col1":8.1,"col2":"string 8","group_id":"Group 1"}

Number of records returned for filter by list expression <[1.1, 2.1, 5.1]>: 3

Filtered (by list) record(s):
* {"col1":1.1,"col2":"string 1","group_id":"Group 1"}
* {"col1":2.1,"col2":"string 2","group_id":"Group 1"}
* {"col1":5.1,"col2":"string 5","group_id":"Group 1"}

Number of records returned for filter by range expression <col1 1-5>: 4

Filtered (by range) record(s):
* {"col1":1.1,"col2":"string 1","group_id":"Group 1"}
* {"col1":2.1,"col2":"string 2","group_id":"Group 1"}
* {"col1":3.1,"col2":"string 3","group_id":"Group 1"}
* {"col1":4.1,"col2":"string 4","group_id":"Group 1"}

Statistics of values in <col1>:
* count = 10.0
* mean = 4.6
* sum = 46.0

Unique values in the <group_id> column: 2
* Group 1
* Group 2

Group by results for the <col2> column:[{"COUNT(*)":17,"SUM(col1)":144.7}]
* COUNT(*) = 17
* SUM(col1) = 144.7

Group by results for the <group_id> column:
* Group 1
  * COUNT(*) = 10
  * SUM(col1) = 46.0
* Group 2
  * COUNT(*) = 7
  * SUM(col1) = 98.7

Histogram counts: [1.0]

Dropping original table dropped all views as expected.

*/


