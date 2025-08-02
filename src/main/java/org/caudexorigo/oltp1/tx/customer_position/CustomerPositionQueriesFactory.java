package org.caudexorigo.oltp1.tx.customer_position;

import org.caudexorigo.db.SqlEngine;

/**
 * Factory for creating the appropriate Customer-Position query strategy.
 */
public class CustomerPositionQueriesFactory
{
	public static CustomerPositionQueries getQueries(SqlEngine engine)
	{
		switch (engine)
		{
		case POSTGRESQL:
			return new PgSqlCustomerPositionQueries();
		case MSSQL:
			return new MsSqlCustomerPositionQueries();
		default:
			throw new IllegalArgumentException(
					String.format("Unsupported SQL engine for Customer-Position: %s", engine));
		}
	}
}
