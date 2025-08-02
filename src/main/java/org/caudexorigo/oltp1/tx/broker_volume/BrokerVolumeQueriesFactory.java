package org.caudexorigo.oltp1.tx.broker_volume;

import org.caudexorigo.db.SqlEngine;

/**
 * Factory for creating the appropriate Broker-Volume query strategy.
 */
public class BrokerVolumeQueriesFactory
{
	public static BrokerVolumeQueries getQueries(SqlEngine engine)
	{
		switch (engine)
		{
		case POSTGRESQL:
			return new PgSqlBrokerVolumeQueries();
		case MSSQL:
			return new MsSqlBrokerVolumeQueries();
		default:
			throw new IllegalArgumentException(String.format("Unsupported SQL engine for Broker-Volume: %s", engine));
		}
	}
}
