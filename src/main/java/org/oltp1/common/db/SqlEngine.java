package org.oltp1.common.db;

import org.apache.commons.lang3.StringUtils;

public enum SqlEngine
{
	MSSQL, POSTGRESQL, MARIADB, ORIOLEDB;

	public String getBaselineQuery()
	{
		return "SELECT 0;";
	}

	public String getDefaultConnectDatabase()
	{
		if (this == MSSQL)
		{
			return "master";
		}
		else if (this == POSTGRESQL || this == ORIOLEDB)
		{
			return "postgres";
		}
		else if (this == MARIADB)
		{
			return "";
		}
		// else if (this == ORACLE)
		// {
		// return "orcl";
		// }
		else
		{
			throw new IllegalArgumentException("Invalid/Unknown database");
		}
	}

	public String getInfoQuery()
	{
		if (this == MSSQL)
		{
			return "SELECT @@version;";
		}
		else if (this == POSTGRESQL || this == ORIOLEDB)
		{
			return "SELECT version();";
		}
		else if (this == MARIADB)
		{
			return "SELECT version();";
		}
		// else if (this == ORACLE)
		// {
		// return "SELECT LISTAGG(banner, ';') WITHIN GROUP (ORDER BY rownum) AS version
		// FROM v$version;";
		// }
		else
		{
			return "SELECT 'Unknown Database engine'";
		}
	}

	public String getAbrev()
	{
		if (this == POSTGRESQL)
		{
			return "pgsql";
		}
		else if (this == ORIOLEDB)
		{
			return "orioledb";
		}
		else
		{
			return toString().toLowerCase();
		}
	}

	/**
	 * Parses a string to SqlEngine, supporting both full names and aliases. This
	 * method replaces valueOf() when you need alias support.
	 * 
	 * Examples: - "POSTGRESQL" or "postgresql" -> POSTGRESQL - "PGSQL" or "pgsql"
	 * -> POSTGRESQL - "MSSQL" or "mssql" -> MSSQL - "MARIADB" or "mariadb" ->
	 * MARIADB - "ORIOLEDB" or "orioledb" -> ORIOLEDB
	 */
	public static SqlEngine fromString(String s)
	{
		if (StringUtils.isBlank(s))
		{
			throw new IllegalArgumentException("Can't create a SQL Engine from a blank String");
		}

		String normalized = s.trim().toUpperCase();

		// Handle aliases
		switch (normalized)
		{
		case "PGSQL":
			return POSTGRESQL;
		case "POSTGRES":
			return POSTGRESQL;
		case "PG":
			return POSTGRESQL;
		case "SQLSERVER":
			return MSSQL;
		case "SQL SERVER":
			return MSSQL;
		case "MYSQL": // MariaDB is MySQL-compatible
			return MARIADB;
		default:
			return SqlEngine.valueOf(normalized);
		}
	}


	public SqlContext createSqlContext(String host, int port, String user, String password, int clients)
	{
		return this.createSqlContext(host, port, getDefaultConnectDatabase(), user, password, clients);
	}

	public SqlContext createSqlContext(String host, int port, String database, String user, String password, int maximumPoolSize)
	{
		switch (this)
		{
		case ORIOLEDB:
			int odbPort = (port == 0) ? 5432 : port;
			String odbUri = String
					.format(
							"jdbc:postgresql://%s:%s/%s",
							host,
							odbPort,
							database);

			return SqlContext
					.buildSqlContext(
							this,
							odbUri,
							"org.postgresql.Driver",
							user,
							password,
							maximumPoolSize);
		case POSTGRESQL:
			int pgPort = (port == 0) ? 5432 : port;
			String pgUri = String
					.format(
							"jdbc:postgresql://%s:%s/%s",
							host,
							pgPort,
							database);

			return SqlContext
					.buildSqlContext(
							this,
							pgUri,
							"org.postgresql.Driver",
							user,
							password,
							maximumPoolSize);

		case MSSQL:
			int msPort = (port == 0) ? 1433 : port;
			String msUri = String
					.format(
							"jdbc:sqlserver://%s:%s;database=%s;encrypt=false;trustServerCertificate=true",
							host,
							msPort,
							database);

			return SqlContext
					.buildSqlContext(
							this,
							msUri,
							"com.microsoft.sqlserver.jdbc.SQLServerDriver",
							user,
							password,
							maximumPoolSize);

		case MARIADB:
			int mariaPort = (port == 0) ? 3306 : port;
			String mariaUri = String
					.format(
							"jdbc:mariadb://%s:%s/%s",
							host,
							mariaPort,
							database);

			return SqlContext
					.buildSqlContext(
							this,
							mariaUri,
							"org.mariadb.jdbc.Driver",
							user,
							password,
							maximumPoolSize);

		default:
			throw new IllegalArgumentException(
					String.format("Database connection creation not supported for engine: %s", this));
		}
	}
}