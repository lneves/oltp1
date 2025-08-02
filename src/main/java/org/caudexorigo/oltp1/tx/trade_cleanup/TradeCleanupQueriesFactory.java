package org.caudexorigo.oltp1.tx.trade_cleanup;

import org.caudexorigo.db.SqlEngine;

/**
 * Factory for creating the appropriate Trade-Cleanup query strategy.
 */
public class TradeCleanupQueriesFactory
{

	public static TradeCleanupQueries getQueries(SqlEngine engine)
	{
		switch (engine)
		{
		case POSTGRESQL:
			return new PgSqlTradeCleanupQueries();
		case MSSQL:
			return new MsSqlTradeCleanupQueries();
		default:
			throw new IllegalArgumentException(
					String.format("Unsupported SQL engine for Trade-Cleanup: %s", engine));
		}
	}
}
