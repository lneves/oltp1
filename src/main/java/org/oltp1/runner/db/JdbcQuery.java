package org.oltp1.runner.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.sql2o.Connection;

public class JdbcQuery
{
	private final SqlContext sqlCtx;

	public JdbcQuery(SqlContext sqlCtx)
	{
		this.sqlCtx = sqlCtx;
	}

	public void executeQuery(String query, int fetchSize, ResultSetRowHandler rowHandler)
	{
		java.sql.Connection jdbcConn = null;

		try (Connection sql2oConn = sqlCtx.getSql2o().open())
		{
			jdbcConn = sql2oConn.getJdbcConnection();
			boolean initialAutocommit = true;

			try (java.sql.Statement stmt = jdbcConn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY))
			{
				initialAutocommit = jdbcConn.getAutoCommit();
				jdbcConn.setAutoCommit(false);
				stmt.setFetchSize(fetchSize);

				try (java.sql.ResultSet r = stmt.executeQuery(query))
				{
					while (r.next())
						rowHandler.handle(r);
				}
			}

			jdbcConn.setAutoCommit(initialAutocommit);
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
	}
}