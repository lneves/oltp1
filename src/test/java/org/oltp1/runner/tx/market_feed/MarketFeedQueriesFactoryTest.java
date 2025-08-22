package org.oltp1.runner.tx.market_feed;

import static org.junit.Assert.assertTrue;

import org.oltp1.runner.db.SqlEngine;
import org.oltp1.runner.tx.QueryFactory;
import org.junit.Test;

public class MarketFeedQueriesFactoryTest
{
	@Test
	public void testGetQueries_PostgreSQL()
	{
		MarketFeedQueries queries = QueryFactory.getQueries(MarketFeedQueries.class, SqlEngine.POSTGRESQL);
		assertTrue(queries instanceof PgSqlMarketFeedQueries);
	}

	@Test
	public void testGetQueries_MSSQL()
	{
		MarketFeedQueries queries = QueryFactory.getQueries(MarketFeedQueries.class, SqlEngine.MSSQL);
		assertTrue(queries instanceof MsSqlMarketFeedQueries);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetQueries_UnsupportedEngine()
	{
		QueryFactory.getQueries(MarketFeedQueries.class, SqlEngine.ORACLE); // Using ORACLE as an example of unsupported
	}
}
