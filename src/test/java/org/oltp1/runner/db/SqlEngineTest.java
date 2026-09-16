package org.oltp1.runner.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class SqlEngineTest
{
	@Test
	public void testDetect_Postgres()
	{
		assertEquals(SqlEngine.POSTGRESQL, SqlEngine.detect("org.postgresql.Driver", "jdbc:postgresql://host:port/db"));
		assertEquals(SqlEngine.POSTGRESQL, SqlEngine.detect(null, "jdbc:postgresql://host:port/db"));
		assertEquals(SqlEngine.POSTGRESQL, SqlEngine.detect("com.impossibl.postgres.jdbc.PGDriver", "jdbc:pgsql://host:port/db"));
	}

	@Test
	public void testDetect_SqlServer()
	{
		assertEquals(SqlEngine.MSSQL, SqlEngine.detect("com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://host:port;databaseName=db"));
		assertEquals(SqlEngine.MSSQL, SqlEngine.detect(null, "jdbc:sqlserver://host:port"));
		assertEquals(SqlEngine.MSSQL, SqlEngine.detect("net.sourceforge.jtds.jdbc.Driver", "jdbc:jtds:sqlserver://host:port/db"));
	}

	@Test
	public void testDetect_MariaDB()
	{
		assertEquals(SqlEngine.MARIADB, SqlEngine.detect("org.mariadb.jdbc.Driver", "jdbc:mariadb://host:port/db"));
		assertEquals(SqlEngine.MARIADB, SqlEngine.detect(null, "jdbc:mariadb://host:port/db"));
	}

	// @Test
	// public void testDetect_Oracle()
	// {
	// assertEquals(SqlEngine.ORACLE,
	// SqlEngine.detect("oracle.jdbc.driver.OracleDriver",
	// "jdbc:oracle:thin:@host:port:sid"));
	// assertEquals(SqlEngine.ORACLE, SqlEngine.detect(null,
	// "jdbc:oracle:thin:@host:port:sid"));
	// }

	@Test
	void testDetect_Generic() {
	    assertThrows(
	        IllegalArgumentException.class,
	        () -> SqlEngine.detect(
	            "com.example.Driver",
	            "jdbc:example://host:port/db"
	        )
	    );
	}

	@Test
    void testDetect_BothBlank() {
        assertThrows(
            IllegalArgumentException.class,
            () -> SqlEngine.detect(null, null)
        );
    }
}
