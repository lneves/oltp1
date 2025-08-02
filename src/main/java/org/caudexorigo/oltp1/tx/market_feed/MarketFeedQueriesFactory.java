package org.caudexorigo.oltp1.tx.market_feed;

import org.caudexorigo.db.SqlEngine;

/**
 * Factory for creating the appropriate Market-Feed query strategy based on the target database engine.
 */
public class MarketFeedQueriesFactory
{

	public static MarketFeedQueries getQueries(SqlEngine engine)
	{
		switch (engine)
		{
		case POSTGRESQL:
			return new PgSqlMarketFeedQueries();
		case MSSQL:
			return new MsSqlMarketFeedQueries();
		default:
			throw new IllegalArgumentException(String.format("Unsupported SQL engine for Market-Feed: %s", engine));
		}
	}
}
