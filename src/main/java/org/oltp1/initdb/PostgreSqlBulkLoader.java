package org.oltp1.initdb;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;

import org.oltp1.common.ErrorAnalyser;
import org.oltp1.runner.db.SqlContext;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

public class PostgreSqlBulkLoader extends BulkLoader
{
	private static final Logger log = LoggerFactory.getLogger(PostgreSqlBulkLoader.class);

	private final SqlContext sqlContext;

	public PostgreSqlBulkLoader(SqlContext sqlContext, Path dataDir)
	{
		super(dataDir);
		this.sqlContext = sqlContext;
	}

	@Override
	public void loadAllTables() throws Exception
	{
		try (Connection conn = sqlContext.getSql2o().open())
		{
			BaseConnection pgConn = conn.getJdbcConnection().unwrap(BaseConnection.class);
			CopyManager copyManager = new CopyManager(pgConn);

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

				String copyCommand = String.format("COPY %s FROM STDIN WITH (FORMAT CSV, DELIMITER '|')", tableName);

				try (FileInputStream fileStream = new FileInputStream(dataFile.toFile()))
				{
					long rowsLoaded = copyManager.copyIn(copyCommand, fileStream);
					log.info("Loaded {} rows into {}", rowsLoaded, tableName);
				}
				catch (SQLException | IOException e)
				{
					Throwable r = ErrorAnalyser.findRootCause(e);
					log.error("Failed to load data into table: {}", tableName, r);
					throw e;
				}
			}
		}
	}
}