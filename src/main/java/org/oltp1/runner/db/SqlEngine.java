package org.oltp1.runner.db;

import org.apache.commons.lang3.StringUtils;
import org.oltp1.common.Assert;

public enum SqlEngine
{
	MSSQL, POSTGRESQL, MARIADB, ORACLE, ORIOLEDB;

	public String getBaselineQuery()
	{
		if (this == ORACLE)
		{
			return "SELECT 0 FROM DUAL;";
		}
		else
		{
			return "SELECT 0;";
		}
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
		else if (this == ORACLE)
		{
			return "orcl";
		}
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
		else if (this == ORACLE)
		{
			return "SELECT LISTAGG(banner, ';') WITHIN GROUP (ORDER BY rownum) AS version FROM v$version;";
		}
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

	/**
	 * @deprecated Use fromString() instead for better alias support
	 */
	@Deprecated
	public static SqlEngine fromAbbrev(String s)
	{
		return fromString(s);
	}

	public static final SqlEngine detect(String jdbcUrl)
	{
		Assert.notBlank("jdbcUrl", jdbcUrl);

		return detect(null, jdbcUrl);
	}

	public static final SqlEngine detect(String className, String jdbcUrl)
	{
		if (StringUtils.isBlank(className) && StringUtils.isBlank(jdbcUrl))
		{
			throw new IllegalArgumentException("'className' and 'jdbcUrl' can not both be blank");
		}

		if ("org.postgresql.Driver".equals(className) || StringUtils.contains(jdbcUrl, "jdbc:postgresql:"))
		{
			return SqlEngine.POSTGRESQL;
		}

		if ("com.impossibl.postgres.jdbc.PGDriver".equals(className) || StringUtils.contains(jdbcUrl, "jdbc:pgsql:"))
		{
			return SqlEngine.POSTGRESQL;
		}

		if (StringUtils.startsWith(className, "com.microsoft.sqlserver") || StringUtils.contains(jdbcUrl, "jdbc:sqlserver:"))
		{
			return SqlEngine.MSSQL;
		}

		if (StringUtils.startsWith(className, "net.sourceforge.jtds.") || StringUtils.contains(jdbcUrl, "jdbc:jtds:"))
		{
			return SqlEngine.MSSQL;
		}

		if (StringUtils.startsWith(className, "org.mariadb.jdbc.") || StringUtils.contains(jdbcUrl, "jdbc:mariadb:"))
		{
			return SqlEngine.MARIADB;
		}

		if (StringUtils.startsWith(className, "oracle.jdbc.") || StringUtils.startsWith(jdbcUrl, "jdbc:oracle:"))
		{
			return SqlEngine.ORACLE;
		}

		throw new IllegalArgumentException("Can't detect the SQL Engine from the JDBC parameters");
	}

	public SqlContext createSqlContext(String host, int port, String user, String password, int clients)
	{
		return this.createSqlContext(host, port, getDefaultConnectDatabase(), user, password, clients);
	}

	public SqlContext createSqlContext(String host, int port, String database, String user, String password, int clients)
	{
		switch (this)
		{
		case ORIOLEDB:
		case POSTGRESQL:
			int pgPort = (port == 0) ? 5432 : port;
			String pgUri = String
					.format(
							"jdbc:postgresql://%s:%s/%s?user=%s&password=%s",
							host,
							pgPort,
							database,
							user,
							password);

			return SqlContext.buildSqlContext(pgUri, "org.postgresql.Driver", clients);

		case MSSQL:
			int msPort = (port == 0) ? 1433 : port;
			String msUri = String
					.format(
							"jdbc:sqlserver://%s:%s;database=%s;user=%s;password=%s;encrypt=false;trustServerCertificate=true",
							host,
							msPort,
							database,
							user,
							password);

			return SqlContext.buildSqlContext(msUri, "com.microsoft.sqlserver.jdbc.SQLServerDriver", clients);

		case MARIADB:
			int mariaPort = (port == 0) ? 3306 : port;
			String mariaUri = String
					.format(
							"jdbc:mariadb://%s:%s/%s?user=%s&password=%s",
							host,
							mariaPort,
							database,
							user,
							password);

			return SqlContext.buildSqlContext(mariaUri, "org.mariadb.jdbc.Driver", clients);

		default:
			throw new IllegalArgumentException(
					String.format("Database connection creation not supported for engine: %s", this));
		}
	}
}