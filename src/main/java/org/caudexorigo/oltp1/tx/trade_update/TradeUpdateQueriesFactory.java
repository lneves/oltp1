package org.caudexorigo.oltp1.tx.trade_update;

import org.caudexorigo.db.SqlEngine;

/**
 * Factory for creating the appropriate Trade-Update query strategy.
 */
public class TradeUpdateQueriesFactory
{

	public static TradeUpdateQueries getQueries(SqlEngine engine)
	{
		switch (engine)
		{
		case POSTGRESQL:
			return new PgSqlTradeUpdateQueries();
		case MSSQL:
			return new MsSqlTradeUpdateQueries();
		default:
			throw new IllegalArgumentException(
					String.format("Unsupported SQL engine for Trade-Update: %s", engine));
		}
	}
}
