package org.caudexorigo.oltp1.tx.data_maintenance;

import org.caudexorigo.db.SqlEngine;

/**
 * Factory for creating the appropriate Data-Maintenance query strategy.
 */
public class DataMaintenanceQueriesFactory
{
	public static DataMaintenanceQueries getQueries(SqlEngine engine)
	{
		switch (engine)
		{
		case POSTGRESQL:
			return new PgSqlDataMaintenanceQueries();
		case MSSQL:
			return new MsSqlDataMaintenanceQueries();
		default:
			throw new IllegalArgumentException(String.format("Unsupported SQL engine for Data-Maintenance: %s", engine));
		}
	}
}
