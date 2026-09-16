package org.oltp1.initdb;

import java.nio.file.Path;

import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.db.SqlEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

public class DbInitializer
{
	private static final Logger log = LoggerFactory.getLogger(DbInitializer.class);

	private DbParameters dbParams;
	private final Path dataDir;
	private final SqlEngine engine;
	private final Sql2o sql2oInit;

	public DbInitializer(SqlContext sqlContextInit, DbParameters dbParams, Path dataDir)
	{
		this.dbParams = dbParams;
		this.dataDir = dataDir;

		this.engine = sqlContextInit.getSqlEngine();
		this.sql2oInit = sqlContextInit.getSql2o();
	}

	public void initialize() throws Exception
	{
		log.info("Starting database initialization for {}", engine);

		if ((engine == SqlEngine.POSTGRESQL) || (engine == SqlEngine.ORIOLEDB))
		{
			initPostgresql();
		}
		else if (engine == SqlEngine.MSSQL)
		{
			initMssql();
		}
		else if (engine == SqlEngine.MARIADB)
		{
			initMariaDb();
		}

		log.info("Database initialization completed successfully");
	}

	private void initMariaDb() throws Exception
	{
		// Clean slate, drop everything first
		executeCommand(sql2oInit, "DROP DATABASE IF EXISTS tpce;");
		executeCommand(sql2oInit, "CREATE DATABASE tpce;");

		// Change SqlContext to point to 'tpce' database
		try (final SqlContext sqlContext = engine
				.createSqlContext(
						dbParams.host,
						dbParams.port,
						"tpce",
						dbParams.user,
						dbParams.password,
						1))
		{
			final SqlScriptExecutor scriptExecutor = new SqlScriptExecutor(sqlContext);

			log.info("Creating tables...");
			scriptExecutor.executeScriptFromResource(getScriptPath("1_create_table.sql"));

			log.info("Loading data...");

			BulkLoader bulkLoader = BulkLoaderFactory.createBulkLoader(sqlContext, dataDir);
			bulkLoader.loadAllTables();

			log.info("Creating indexes and Foreign keys...");
			scriptExecutor.executeScriptFromResource(getScriptPath("3_create_index_fk.sql"));

			log.info("Creating sequences...");
			scriptExecutor.executeScriptFromResource(getScriptPath("4_create_sequence.sql"));

			log.info("Applying database settings...");
			scriptExecutor.executeScriptFromResource(getScriptPath("5_db_settings.sql"));

			log.info("Analyzing tables...");
			scriptExecutor.executeScriptFromResource(getScriptPath("6_analyze_table.sql"));
		}
		catch (Throwable t)
		{
			throw new RuntimeException(t);
		}
	}

	private void initPostgresql() throws Exception
	{
		log.info("Creating database...");

		boolean isDockerEnv = isDockerEnvironment();

		// clean slate, drop everything first
		fetchValue(sql2oInit, Boolean.class, """
				SELECT pg_terminate_backend(pg_stat_activity.pid)
				FROM pg_stat_activity
				WHERE datname = 'tpce' AND pid <> pg_backend_pid();
				""");

		executeCommand(sql2oInit, "DROP DATABASE IF EXISTS tpce;");

		if (isDockerEnv)
		{
			executeCommand(sql2oInit, "DROP TABLESPACE IF EXISTS tblsp_tpce;");
		}

		// Create database
		if (isDockerEnv)
		{
			executeCommand(sql2oInit, "CREATE TABLESPACE tblsp_tpce LOCATION '/mnt/tablespaces/tblsp_tpce';");
			executeCommand(sql2oInit, "CREATE DATABASE tpce WITH TABLESPACE=tblsp_tpce;");
		}
		else
		{
			executeCommand(sql2oInit, "CREATE DATABASE tpce;");
		}

		// Change SqlContext to point to 'tpce' database
		try (final SqlContext sqlContext = engine
				.createSqlContext(
						dbParams.host,
						dbParams.port,
						"tpce",
						dbParams.user,
						dbParams.password,
						1))
		{
			final SqlScriptExecutor scriptExecutor = new SqlScriptExecutor(sqlContext);

			log.info("Creating tables...");
			scriptExecutor.executeScriptFromResource(getScriptPath("1_create_table.sql"));

			log.info("Loading data...");
			BulkLoader bulkLoader = BulkLoaderFactory.createBulkLoader(sqlContext, dataDir);
			bulkLoader.loadAllTables();

			scriptExecutor.executeScriptFromResource(getScriptPath("3_create_keys.sql"));

			log.info("Creating indexes...");
			scriptExecutor.executeScriptFromResource(getScriptPath("4_create_index.sql"));

			log.info("Creating foreign keys...");
			scriptExecutor.executeScriptFromResource(getScriptPath("5_create_fk.sql"));

			log.info("Creating sequences...");
			scriptExecutor.executeScriptFromResource(getScriptPath("6_create_sequence.sql"));

			scriptExecutor.executeScriptFromResource(getScriptPath("7_analyze_table.sql"));

			log.info("Applying database settings...");
			scriptExecutor.executeScriptFromResource(getScriptPath("8_db_settings.sql"));
		}
		catch (Throwable t)
		{
			throw new RuntimeException(t);
		}
	}

	private void initMssql() throws Exception
	{
		// Clean slate, drop everything first
		executeCommand(sql2oInit, "DROP DATABASE IF EXISTS tpce;");
		executeCommand(sql2oInit, "CREATE DATABASE tpce;");

		// Change SqlContext to point to 'tpce' database
		try (final SqlContext sqlContext = engine
				.createSqlContext(
						dbParams.host,
						dbParams.port,
						"tpce",
						dbParams.user,
						dbParams.password,
						1))
		{
			final SqlScriptExecutor scriptExecutor = new SqlScriptExecutor(sqlContext);
			log.info("Creating tables...");
			scriptExecutor.executeScriptFromResource(getScriptPath("1_create_table.sql"));

			log.info("Loading data...");
			BulkLoader bulkLoader = BulkLoaderFactory.createBulkLoader(sqlContext, dataDir);
			bulkLoader.loadAllTables();

			scriptExecutor.executeScriptFromResource(getScriptPath("3_create_pk.sql"));

			log.info("Creating indexes...");
			scriptExecutor.executeScriptFromResource(getScriptPath("4_create_index.sql"));

			log.info("Creating foreign keys...");
			scriptExecutor.executeScriptFromResource(getScriptPath("5_create_fk.sql"));

			log.info("Creating sequences...");
			scriptExecutor.executeScriptFromResource(getScriptPath("6_create_sequence.sql"));

			scriptExecutor.executeScriptFromResource(getScriptPath("7_analyze_table.sql"));

			log.info("Applying database settings...");
			scriptExecutor.executeScriptFromResource(getScriptPath("8_db_settings.sql"));
		}
		catch (Throwable t)
		{
			throw new RuntimeException(t);
		}
	}

	private <T> T fetchValue(Sql2o db, Class<T> clazz, String sql)
	{
		try (Connection con = db.open())
		{
			return con
					.createQuery(sql)
					.executeScalar(clazz);
		}
		catch (Throwable t)
		{
			throw new RuntimeException(t);
		}
	}

	private void executeCommand(Sql2o db, String sql)
	{
		try (Connection con = db.open())
		{
			con
					.createQuery(sql)
					.executeUpdate();
		}
		catch (Throwable t)
		{
			throw new RuntimeException(t);
		}
	}

	private boolean isDockerEnvironment()
	{
		try
		{
			String env = fetchValue(
					sql2oInit,
					String.class,
					"SELECT current_setting('oltp1.environment', true);"); // 'true' prevents missing_ok error
			return "docker".equalsIgnoreCase(env);
		}
		catch (Exception e)
		{
			return false;
		}
	}

	private String getScriptPath(String scriptName)
	{
		return String.format("/ddl-scripts/%s/%s", engine.name().toLowerCase(), scriptName);
	}
}