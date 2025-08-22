package org.oltp1.initdb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.oltp1.runner.db.SqlContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlScriptExecutor
{
	private static final Logger log = LoggerFactory.getLogger(SqlScriptExecutor.class);

	private final SqlContext sqlContext;

	public SqlScriptExecutor(SqlContext sqlContext)
	{
		this.sqlContext = sqlContext;
	}

	public void executeScriptFromResource(String resourcePath) throws Exception
	{
		executeScriptFromResource(resourcePath, null);
	}

	public void executeScriptFromResource(String resourcePath, String dataDir) throws Exception
	{
		InputStream inputStream = getClass().getResourceAsStream(resourcePath);
		if (inputStream == null)
		{
			throw new IllegalArgumentException("Resource not found: " + resourcePath);
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)))
		{
			String content = readFullContent(reader);
			
			// Handle SQL Server variable substitution
			if (dataDir != null && sqlContext.getSqlEngine().name().equals("MSSQL"))
			{
				content = content.replace("$(DATA_DIR)", dataDir);
			}
			
			List<String> statements = parseStatements(content);
			executeStatements(statements);
		}
	}

	private String readFullContent(BufferedReader reader) throws IOException
	{
		StringBuilder content = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null)
		{
			content.append(line).append("\n");
		}
		return content.toString();
	}

	private List<String> parseStatements(String content)
	{
		List<String> statements = new ArrayList<>();
		
		// Handle SQL Server GO statements and PostgreSQL/MySQL semicolon-terminated statements
		if (sqlContext.getSqlEngine().name().equals("MSSQL"))
		{
			// Split on GO statements for SQL Server
			String[] parts = content.split("(?i)\\bGO\\b");
			for (String part : parts)
			{
				String trimmed = part.trim();
				if (!trimmed.isEmpty() && !isComment(trimmed))
				{
					statements.add(trimmed);
				}
			}
		}
		else
		{
			// Split on semicolons for other databases
			String[] parts = content.split(";");
			for (String part : parts)
			{
				String trimmed = part.trim();
				if (!trimmed.isEmpty() && !isComment(trimmed))
				{
					statements.add(trimmed);
				}
			}
		}

		return statements;
	}

	private boolean isComment(String statement)
	{
		String trimmed = statement.trim();
		return trimmed.startsWith("--") || 
		       trimmed.startsWith("/*") || 
		       trimmed.toUpperCase().startsWith("PRINT") ||
		       trimmed.toUpperCase().startsWith("USE ") ||
		       trimmed.isEmpty();
	}

	private void executeStatements(List<String> statements) throws SQLException
	{
		try (Connection conn = sqlContext.getSql2o().open().getJdbcConnection();
		     Statement stmt = conn.createStatement())
		{
			for (String sql : statements)
			{
				try
				{
					log.debug("Executing: {}", StringUtils.abbreviate(sql.replaceAll("\\R", " "), 100));
					stmt.execute(sql);
				}
				catch (SQLException e)
				{
					log.error("Failed to execute statement: {}", StringUtils.abbreviate(sql, 250));
					throw e;
				}
			}
		}
	}
}