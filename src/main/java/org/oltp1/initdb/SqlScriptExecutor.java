package org.oltp1.initdb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.oltp1.common.db.SqlContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlScriptExecutor
{
	private static final Logger log = LoggerFactory.getLogger(SqlScriptExecutor.class);

	private final SqlContext sqlContext;
	private final SqlScriptParser parser;

	public SqlScriptExecutor(SqlContext sqlContext)
	{
		this.sqlContext = sqlContext;
		this.parser = SqlScriptParser.forEngine(sqlContext.getSqlEngine().name());
	}

	/**
	 * Executes a DDL script from the classpath.
	 *
	 * @param resourcePath an absolute classpath resource, e.g. {@code /ddl-scripts/postgresql/1_create_table.sql}
	 */
	public void executeScriptFromResource(String resourcePath) throws Exception
	{
		String content = readResource(resourcePath);

		List<String> statements = parser.parse(content);

		log.debug("{}: {} statement(s)", resourcePath, statements.size());

		executeStatements(statements);
	}

	/**
	 * Executes the statements on one connection.
	 * <p>
	 * Fail-fast: the first failure is logged with the offending statement and rethrown, so a
	 * half-applied script is obvious rather than silent. Note that each statement is its own
	 * transaction, which is why a failure part-way leaves an incomplete schema; see
	 * {@code DbInitializer} for the consequences.
	 */
	private void executeStatements(List<String> statements) throws SQLException
	{
		// Both wrappers are closed: the sql2o Connection owns the JDBC handle and returning it
		// matters, and closing the sql2o Connection is what actually returns the physical
		// connection to the pool.
		try (org.sql2o.Connection sql2oConn = sqlContext.getSql2o().open();
				Connection jdbcConn = sql2oConn.getJdbcConnection();
				Statement stmt = jdbcConn.createStatement())
		{
			for (String sql : statements)
			{
				log.debug("Executing: {}", StringUtils.abbreviate(sql.replaceAll("\\R", " "), 200));

				try
				{
					stmt.execute(sql);
				}
				catch (SQLException e)
				{
					log.error("Failed to execute statement: {}", StringUtils.abbreviate(sql, 500), e);
					throw e;
				}
			}
		}
	}

	private String readResource(String resourcePath) throws IOException
	{
		InputStream inputStream = getClass().getResourceAsStream(resourcePath);

		if (inputStream == null)
		{
			throw new IllegalArgumentException("Resource not found: " + resourcePath);
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)))
		{
			StringBuilder content = new StringBuilder();
			String line;

			while ((line = reader.readLine()) != null)
			{
				content.append(line).append('\n');
			}

			return content.toString();
		}
	}
}
