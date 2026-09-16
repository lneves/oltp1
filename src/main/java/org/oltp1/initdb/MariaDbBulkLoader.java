package org.oltp1.initdb;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Statement;

import org.oltp1.common.ErrorAnalyser;
import org.oltp1.runner.db.SqlContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

public class MariaDbBulkLoader extends BulkLoader
{
	private static final Logger log = LoggerFactory.getLogger(MariaDbBulkLoader.class);

	private static final String LOCAL_INFILE_QUERY = "SELECT @@GLOBAL.local_infile";

	private final SqlContext sqlContext;

	public MariaDbBulkLoader(SqlContext sqlContext, Path dataDir)
	{
		super(dataDir);
		this.sqlContext = sqlContext;
	}

	@Override
	public void loadAllTables() throws Exception
	{
		String currTable = "";

		try (Connection conn = sqlContext.getSql2o().open())
		{
			java.sql.Connection jdbcConn = conn.getJdbcConnection();

			try (Statement s = jdbcConn.createStatement())
			{
				requireLocalInfileEnabled(s); // replaces SET GLOBAL local_infile = 1
				configureBulkLoadSession(s); // session-only settings

				try
				{
					for (String fileName : TABLE_LOAD_ORDER)
					{
						Path dataFile = getDataFile(fileName);
						if (!dataFile.toFile().exists())
							throw new FileNotFoundException("Data file not found: " + fileName);

						String tableName = getTableName(fileName);
						currTable = tableName;
						log.info("Loading table: {} from {}", tableName, fileName);

						String loadDataSQL = String
								.format(
										"LOAD DATA LOCAL INFILE 'stdin' INTO TABLE %s "
												+ "FIELDS TERMINATED BY '|' LINES TERMINATED BY '\\n'",
										tableName);

						org.mariadb.jdbc.Statement mstmt = s.unwrap(org.mariadb.jdbc.Statement.class);
						try (FileInputStream fileStream = new FileInputStream(dataFile.toFile()))
						{
							mstmt.setLocalInfileInputStream(fileStream);
							mstmt.execute(loadDataSQL);
							log.info("Successfully loaded data into table: {}", tableName);
						}
					}
				}
				finally
				{
					try
					{
						restoreBulkLoadSession(s);
					}
					catch (SQLException e)
					{
						log.warn("Could not restore MariaDB session settings", e);
					}
				}
			}
		}
		catch (Throwable t)
		{
			Throwable r = ErrorAnalyser.findRootCause(t);
			log.error("MariaDB bulk load failed (current table: '{}'): {}", currTable, r.getMessage(), r);
			throw new RuntimeException(t);
		}
	}

	private static void requireLocalInfileEnabled(Statement s) throws SQLException
	{
		try (java.sql.ResultSet rs = s.executeQuery(LOCAL_INFILE_QUERY))
		{
			if (!(rs.next() && rs.getInt(1) != 0))
				throw new IllegalStateException(
						"The MariaDB server has 'local_infile' disabled; the bulk loader requires "
								+ "LOAD DATA LOCAL INFILE. Enable it persistently with 'local_infile=1' under "
								+ "[mysqld] in my.cnf, or for the current server process run "
								+ "'SET GLOBAL local_infile=1;' as an account with SUPER/SYSTEM_VARIABLES_ADMIN. "
								+ "See README 'Database Setup'.");
		}
	}

	private static void configureBulkLoadSession(Statement s) throws SQLException
	{
		s.execute("SET UNIQUE_CHECKS=0");
		s.execute("SET FOREIGN_KEY_CHECKS=0");
	}

	private static void restoreBulkLoadSession(Statement s) throws SQLException
	{
		s.execute("SET UNIQUE_CHECKS=1");
		s.execute("SET FOREIGN_KEY_CHECKS=1");
	}

}