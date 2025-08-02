package org.caudexorigo.oltp1.tx.market_feed;

import static org.junit.Assert.assertTrue;

import org.caudexorigo.db.SqlEngine;
import org.junit.Test;

public class MarketFeedQueriesFactoryTest
{
	@Test
	public void testGetQueries_PostgreSQL()
	{
		MarketFeedQueries queries = MarketFeedQueriesFactory.getQueries(SqlEngine.POSTGRESQL);
		assertTrue(queries instanceof PgSqlMarketFeedQueries);
	}

	@Test
	public void testGetQueries_MSSQL()
	{
		MarketFeedQueries queries = MarketFeedQueriesFactory.getQueries(SqlEngine.MSSQL);
		assertTrue(queries instanceof MsSqlMarketFeedQueries);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetQueries_UnsupportedEngine()
	{
		MarketFeedQueriesFactory.getQueries(SqlEngine.MARIADB); // Using MARIADB as an example of unsupported
	}
}
