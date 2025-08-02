package org.caudexorigo.db;

import org.apache.commons.lang3.StringUtils;

public enum SqlEngine
{
	GENERIC, MSSQL, POSTGRESQL, MARIADB, ORACLE;

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

	public String getInfoQuery()
	{
		if (this == MSSQL)
		{
			return "SELECT @@version;";
		}
		else if (this == POSTGRESQL)
		{
			return "SELECT version();";
		}
		else if (this == MARIADB)
		{
			return """
					SELECT GROUP_CONCAT(variable_value SEPARATOR ', ')
					FROM information_schema.global_variables
					WHERE variable_name IN('version', 'version_compile_machine', 'version_compile_os');
					""";
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
		else
		{
			return toString().toLowerCase();
		}
	}

	public SqlEngine fromAbbrev(String s)
	{
		if ("pgsql".equalsIgnoreCase(s))
		{
			return POSTGRESQL;
		}
		else
		{
			return SqlEngine.valueOf(StringUtils.defaultIfBlank(s, "GENERIC").toUpperCase());
		}
	}
}