package org.oltp1.initdb;

import java.nio.file.Path;

import org.oltp1.common.ErrorAnalyser;
import org.oltp1.runner.db.SqlContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

import com.microsoft.sqlserver.jdbc.ISQLServerConnection;
import com.microsoft.sqlserver.jdbc.SQLServerBulkCopy;
import com.microsoft.sqlserver.jdbc.SQLServerBulkCopyOptions;

public class SqlServerBulkLoader extends BulkLoader
{
	private static final Logger log = LoggerFactory.getLogger(SqlServerBulkLoader.class);

	private final SqlContext sqlContext;

	public SqlServerBulkLoader(SqlContext sqlContext, Path dataDir)
	{
		super(dataDir);
		this.sqlContext = sqlContext;
	}

	@Override
	public void loadAllTables() throws Exception
	{
		try (Connection conn = sqlContext.getSql2o().open())
		{
			ISQLServerConnection jdbcConn = conn.getJdbcConnection().unwrap(ISQLServerConnection.class);

			for (String fileName : TABLE_LOAD_ORDER)
			{
				Path dataFile = getDataFile(fileName);
				if (!dataFile.toFile().exists())
				{
					log.warn("Data file not found, skipping: {}", fileName);
					continue;
				}

				String tableName = getTableName(fileName);
				log.info("Loading table: {} from {}", tableName, fileName);

				try
				{
					SQLServerBulkCopyOptions options = new SQLServerBulkCopyOptions();
					options.setKeepIdentity(tableName.equals("trade")); // Keep identity for trade table
					options.setTableLock(true);
					options.setBulkCopyTimeout(0); // No timeout

					try (SQLServerBulkCopy bulkCopy = new SQLServerBulkCopy(jdbcConn))
					{
						bulkCopy.setBulkCopyOptions(options);
						bulkCopy.setDestinationTableName(tableName);

						// Create a custom data reader for pipe-delimited files
						PipeDelimitedFileReader dataReader = new PipeDelimitedFileReader(dataFile);
						bulkCopy.writeToServer(dataReader);
						
						log.info("Successfully loaded data into table: {}", tableName);
					}
				}
				catch (Exception e)
				{
					Throwable r = ErrorAnalyser.findRootCause(e);
					log.error("Failed to load data into table: {}", tableName, r);
					throw e;
				}
			}
		}
	}
}