package org.oltp1.runner.db;

import org.sql2o.Sql2o;
import org.sql2o.quirks.QuirksDetector;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class SqlContext
{
	private static final int MSSQL_TRANSACTION_ISOLATION_SNAPSHOT = 0x1000; // SQLServerConnection.TRANSACTION_SNAPSHOT; = 0x1000;

	public static SqlContext buildSqlContext(String jdbcUrl, String jdbcDriver, int connections)
	{
		SqlEngine engine = SqlEngine.detect(jdbcDriver, jdbcUrl);

		HikariConfig c = new HikariConfig();
		c.setDriverClassName(jdbcDriver);
		c.setMaximumPoolSize(connections);
		c.setJdbcUrl(jdbcUrl);
		c.setMaximumPoolSize(connections);
		c.setConnectionTestQuery(engine.getBaselineQuery());

		HikariDataSource ds = new HikariDataSource(c);

		return new SqlContext(new Sql2o(ds, QuirksDetector.forURL(jdbcUrl)), engine);
	}

	private final Sql2o sql2o;
	private final SqlEngine sqlEngine;
	private final int isolationLevel;

	private SqlContext(Sql2o sql2o, SqlEngine sqlEngine)
	{
		super();
		this.sql2o = sql2o;
		this.sqlEngine = sqlEngine;

		if (sqlEngine == SqlEngine.MSSQL)
		{
			this.isolationLevel = MSSQL_TRANSACTION_ISOLATION_SNAPSHOT;
		}
		else
		{
			this.isolationLevel = java.sql.Connection.TRANSACTION_READ_COMMITTED;
		}
	}

	public Sql2o getSql2o()
	{
		return sql2o;
	}

	public SqlEngine getSqlEngine()
	{
		return sqlEngine;
	}

	public int getIsolationLevel()
	{
		return this.isolationLevel;
	}
}