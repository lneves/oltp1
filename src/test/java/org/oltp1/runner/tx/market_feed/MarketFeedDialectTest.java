package org.oltp1.runner.tx.market_feed;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.oltp1.runner.db.SqlEngine;
import org.oltp1.runner.tx.QueryFactory;

class MarketFeedDialectTest
{
	@Test
	void resolvesPostgresql()
	{
		assertInstanceOf(
				PgSqlMarketFeedDialect.class,
				QueryFactory.getQueries(MarketFeedDialect.class, SqlEngine.POSTGRESQL));
	}

	@Test
	void resolvesMssql()
	{
		assertInstanceOf(
				MsSqlMarketFeedDialect.class,
				QueryFactory.getQueries(MarketFeedDialect.class, SqlEngine.MSSQL));
	}

	@Test
	void resolvesMariaDb()
	{
		assertInstanceOf(
				MariaDbMarketFeedDialect.class,
				QueryFactory.getQueries(MarketFeedDialect.class, SqlEngine.MARIADB));
	}

	@Test
	void resolvesOrioleDbToPostgresqlDialect()
	{
		assertInstanceOf(
				PgSqlMarketFeedDialect.class,
				QueryFactory.getQueries(MarketFeedDialect.class, SqlEngine.ORIOLEDB));
	}

	@Test
	void rejectsUnregisteredQueryInterface()
	{
		assertThrows(
				IllegalArgumentException.class,
				() -> QueryFactory.getQueries(Runnable.class, SqlEngine.POSTGRESQL));
	}
}
