package org.oltp1.initdb;

import java.nio.file.Path;

import org.oltp1.runner.db.SqlContext;

public class BulkLoaderFactory
{
	public static BulkLoader createBulkLoader(SqlContext sqlContext, Path dataDir)
	{
		switch (sqlContext.getSqlEngine())
		{
		case POSTGRESQL:
		case ORIOLEDB:
			return new PostgreSqlBulkLoader(sqlContext, dataDir);
		case MSSQL:
			return new SqlServerBulkLoader(sqlContext, dataDir);
		case MARIADB:
			return new MariaDbBulkLoader(sqlContext, dataDir);
		default:
			throw new UnsupportedOperationException("Bulk loading not supported for engine: " + sqlContext.getSqlEngine());
		}
	}
}