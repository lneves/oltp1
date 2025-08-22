package org.oltp1.initdb;

import java.io.FileInputStream;
import java.nio.file.Path;

import org.oltp1.common.ErrorAnalyser;
import org.oltp1.runner.db.SqlContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

public class MariaDbBulkLoader extends BulkLoader
{
	private static final Logger log = LoggerFactory.getLogger(MariaDbBulkLoader.class);

	private final SqlContext sqlContext;

	public MariaDbBulkLoader(SqlContext sqlContext, Path dataDir)
	{
		super(dataDir);
		this.sqlContext = sqlContext;
	}

	@Override
	public void loadAllTables() throws Exception
	{
		try (Connection conn = sqlContext.getSql2o().open())
		{
			java.sql.Connection jdbcConn = conn.getJdbcConnection();
			
			jdbcConn.createStatement().execute("SET GLOBAL local_infile = 1");

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

				// Use LOAD DATA LOCAL INFILE for MariaDB
				String loadDataSQL = String
						.format(
								"LOAD DATA LOCAL INFILE 'stdin' INTO TABLE %s FIELDS TERMINATED BY '|' LINES TERMINATED BY '\\n'",
								tableName);

				try (
						org.mariadb.jdbc.Statement mstmt = jdbcConn.createStatement().unwrap(org.mariadb.jdbc.Statement.class);
						FileInputStream fileStream = new FileInputStream(dataFile.toFile()))
				{
					mstmt.setLocalInfileInputStream(fileStream);
					mstmt.execute(loadDataSQL);
					log.info("Successfully loaded data into table: {}", tableName);
				}
				catch (Throwable t)
				{
					Throwable r = ErrorAnalyser.findRootCause(t);
					log.error("Failed to load data into table: {} - {}", tableName, r);
					throw new RuntimeException(t);
				}
			}
			
			jdbcConn.createStatement().execute("SET GLOBAL local_infile = DEFAULT");
		}
	}
}