package org.oltp1.runner.db;

import org.sql2o.Sql2o;
import org.sql2o.quirks.QuirksDetector;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class SqlContext implements AutoCloseable
{
	public static SqlContext buildSqlContext(String jdbcUrl, String jdbcDriver, String user, String passwd, int maximumPoolSize)
	{
		SqlEngine engine = SqlEngine.detect(jdbcDriver, jdbcUrl);

		HikariConfig c = new HikariConfig();
		c.setDriverClassName(jdbcDriver);
		c.setMaximumPoolSize(maximumPoolSize);
		c.setUsername(user);
		c.setPassword(passwd);
		c.setJdbcUrl(jdbcUrl);
		c.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
		c.setPoolName("OLTP1-HikariPool");

		// PostgreSQL (and OrioleDB) do prepared statement caching by default
		switch (engine)
		{
		case MSSQL:
			c.addDataSourceProperty("disableStatementPooling", "false");
			c.addDataSourceProperty("statementPoolingCacheSize", "250");
			break;
		case MARIADB:
			c.addDataSourceProperty("useServerPrepStmts", true);
			c.addDataSourceProperty("cachePrepStmts", true);
			c.addDataSourceProperty("prepStmtCacheSize", 250);
		default:
			break;
		}

		HikariDataSource ds = new HikariDataSource(c);

		return new SqlContext(new Sql2o(ds, QuirksDetector.forURL(jdbcUrl)), engine, ds);
	}

	private final Sql2o sql2o;
	private final SqlEngine sqlEngine;
	private final HikariDataSource dataSource;

	private SqlContext(Sql2o sql2o, SqlEngine sqlEngine, HikariDataSource dataSource)
	{
		super();
		this.sql2o = sql2o;
		this.sqlEngine = sqlEngine;
		this.dataSource = dataSource;
	}

	public Sql2o getSql2o()
	{
		return sql2o;
	}

	public SqlEngine getSqlEngine()
	{
		return sqlEngine;
	}

	@Override
	public void close() throws Exception
	{
		dataSource.close();
	}
}