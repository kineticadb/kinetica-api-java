package com.gpudb.example;

import com.gpudb.GPUdb;
import com.gpudb.GPUdbException;
import com.gpudb.GPUdbLogger;
import com.gpudb.GPUdbSqlIterator;
import com.gpudb.GenericRecord;
import com.gpudb.Record;

/**
 * This class demonstrates the usage of the GPUdbSqlIterator class in iterating
 * through the result set of a SQL statement.
 * 
 * The default SQL statement returns a list of the catalog schemas in the
 * database and the respective count of contained tables & views in each.
 * 
 * Usage:
 *         java -cp "<jar>" -Durl="<url>" -Duser="<username>" -Dpass="<password>" -Dsql="<sql statement>" com.gpudb.example.GPUdbSqlIteratorExample
 *         
 * Options:
 * 
 *         -Durl="<url>"            (Kinetica connection URL, default is"http://127.0.0.1:9191")
 *         -Dsql="<sql statement>"  (SQL statement to run, default is catalog schemas and counts of tables/views within)
 *         -DlogLevel=<level>       (Output logging level, default is "INFO")
 */
public class GPUdbSqlIteratorExample
{
	private final static String DEFAULT_SQL = String.join(" ",
			" SELECT schema_name, COUNT(*) AS total_objects                           ",
			" FROM ki_catalog.ki_objects                                              ",
			" WHERE schema_name IN ('ki_catalog', 'pg_catalog', 'information_schema') ",
			" GROUP BY schema_name                                                    ",
			" ORDER BY schema_name                                                    "
	);

	public static void main(String... args) throws GPUdbException
	{
		String url = System.getProperty("url", "http://127.0.0.1:9191");
		String user = System.getProperty("user", "");
		String pass = System.getProperty("pass", "");
		String sql = System.getProperty("sql", DEFAULT_SQL);
		String logLevel = System.getProperty("logLevel", "INFO");

		GPUdbLogger.setLoggingLevel(logLevel);

		// Establish a connection with a locally running instance of GPUdb
		GPUdb.Options options = new GPUdb.Options();
		options.setUsername(user);
		options.setPassword(pass);
		options.setBypassSslCertCheck(true);
		GPUdb gpudb = new GPUdb(url, options);

		tryWithResourcesUsage(gpudb, sql);
		explicitCloseCallUsage(gpudb, sql);
	}

	private static void tryWithResourcesUsage(GPUdb gpudb, String sql)
	{
		System.out.println("Usage with Try-with-Resources");
		try (GPUdbSqlIterator<Record> iterator = new GPUdbSqlIterator<>(gpudb, sql);)
		{
			for (Record record : iterator)
				System.out.println(record);
		}
		catch (Exception e)
		{
			GPUdbLogger.debug("Error in iteration : " + e.getMessage());
		}
	}

	private static void explicitCloseCallUsage(GPUdb gpudb, String sql)
	{
		System.out.println("\nUsage with Explicit Close Call");
		GPUdbSqlIterator<GenericRecord> iterator = null;

		try
		{
			iterator = new GPUdbSqlIterator<>(gpudb, sql);

			for (Record record : iterator)
				System.out.println(record);

			iterator.close();
		}
		catch (Exception e)
		{
			GPUdbLogger.debug("Error in iteration : " + e.getMessage());
			e.printStackTrace();
		}
	}
}
