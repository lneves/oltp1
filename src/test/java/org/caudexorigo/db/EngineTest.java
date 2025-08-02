package org.caudexorigo.db;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EngineTest
{

	@Test
	public void testDetect_Postgres()
	{
		assertEquals(SqlEngine.POSTGRESQL, Engine.detect("org.postgresql.Driver", "jdbc:postgresql://host:port/db"));
		assertEquals(SqlEngine.POSTGRESQL, Engine.detect(null, "jdbc:postgresql://host:port/db"));
		assertEquals(SqlEngine.POSTGRESQL, Engine.detect("com.impossibl.postgres.jdbc.PGDriver", "jdbc:pgsql://host:port/db"));
	}

	@Test
	public void testDetect_SqlServer()
	{
		assertEquals(SqlEngine.MSSQL, Engine.detect("com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://host:port;databaseName=db"));
		assertEquals(SqlEngine.MSSQL, Engine.detect(null, "jdbc:sqlserver://host:port"));
		assertEquals(SqlEngine.MSSQL, Engine.detect("net.sourceforge.jtds.jdbc.Driver", "jdbc:jtds:sqlserver://host:port/db"));
	}

	@Test
	public void testDetect_MariaDB()
	{
		assertEquals(SqlEngine.MARIADB, Engine.detect("org.mariadb.jdbc.Driver", "jdbc:mariadb://host:port/db"));
		assertEquals(SqlEngine.MARIADB, Engine.detect(null, "jdbc:mariadb://host:port/db"));
	}

	@Test
	public void testDetect_Oracle()
	{
		assertEquals(SqlEngine.ORACLE, Engine.detect("oracle.jdbc.driver.OracleDriver", "jdbc:oracle:thin:@host:port:sid"));
		assertEquals(SqlEngine.ORACLE, Engine.detect(null, "jdbc:oracle:thin:@host:port:sid"));
	}

	@Test
	public void testDetect_Generic()
	{
		assertEquals(SqlEngine.GENERIC, Engine.detect("com.example.Driver", "jdbc:example://host:port/db"));
		assertEquals(SqlEngine.GENERIC, Engine.detect(null, "jdbc:unknown-db://localhost"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDetect_BothBlank()
	{
		Engine.detect(null, null);
	}
}
