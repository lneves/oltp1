package org.caudexorigo.oltp1;

import org.caudexorigo.db.SqlContext;

public enum DbEngine
{
	PGSQL
	{
		@Override
		public SqlContext createSqlContext(BenchmarkArgs args, int clients)
		{
			int port = (args.port == 0) ? 5432 : args.port;
			String uri = String
					.format(
							"jdbc:postgresql://%s:%s/tpce?user=%s&password=%s",
							args.host,
							port,
							args.user,
							args.password);

			return SqlContext.buildSqlContext(uri, "org.postgresql.Driver", clients);
		}
	},
	MSSQL
	{
		@Override
		public SqlContext createSqlContext(BenchmarkArgs args, int clients)
		{
			int port = (args.port == 0) ? 1433 : args.port;
			String uri = String
					.format(
							"jdbc:sqlserver://%s:%s;database=tpce;user=%s;password=%s;encrypt=false;trustServerCertificate=true",
							args.host,
							port,
							args.user,
							args.password);

			return SqlContext.buildSqlContext(uri, "com.microsoft.sqlserver.jdbc.SQLServerDriver", clients);
		}
	},
	ORIOLEDB
	{
		@Override
		public SqlContext createSqlContext(BenchmarkArgs args, int clients)
		{
			// OrioleDB uses the PostgreSQL driver and connection string format
			return PGSQL.createSqlContext(args, clients);
		}
	},
	MARIADB
	{
		@Override
		public SqlContext createSqlContext(BenchmarkArgs args, int clients)
		{
			throw new UnsupportedOperationException("MariaDB connection logic not yet implemented.");
		}
	};

	/**
	 * Creates a SqlContext configured for this specific database engine.
	 *
	 * @param args
	 *            The benchmark arguments containing host, user, etc.
	 * @param clients
	 *            The number of client connections for the pool.
	 * @return A fully configured SqlContext.
	 */
	public abstract SqlContext createSqlContext(BenchmarkArgs args, int clients);
}