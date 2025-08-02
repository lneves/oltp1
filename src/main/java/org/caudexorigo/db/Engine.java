package org.caudexorigo.db;

import org.apache.commons.lang3.StringUtils;
import org.caudexorigo.db.Assert;

public class Engine
{
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
		
		return SqlEngine.GENERIC;
	}
}