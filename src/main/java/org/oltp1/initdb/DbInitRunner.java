package org.oltp1.initdb;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import org.oltp1.common.CaseInsensitiveEnumConverter;
import org.oltp1.common.ErrorAnalyser;
import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.db.SqlEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "initdb", mixinStandardHelpOptions = true, description = "Initialize database schema and load data")
public class DbInitRunner implements Callable<Integer>
{
	private static final Logger log = LoggerFactory.getLogger(DbInitRunner.class);

	@Option(names = { "-h", "--host" }, description = "Database host", required = true)
	public String host;

	@Option(names = { "-p", "--port" }, description = "Database listening port")
	public int port;

	@Option(names = { "-U", "--user" }, description = "Database username", required = true)
	public String user;

	@Option(names = { "-P", "--password" }, defaultValue = "${env:OLTP1_PASSWORD}", description = "Database user password, You can also set OLTP1_PASSWORD in the environment.", required = true)
	public String password;

	@Option(names = { "-e", "--engine" }, description = "Database Engine, valid values: ${COMPLETION-CANDIDATES}", required = true)
	public SqlEngine engine;

	@Option(names = { "-d", "--data-dir" }, description = "Directory containing flat data files", required = true)
	public Path dataDir;

	@Override
	public Integer call() throws Exception
	{
		try
		{
			if (!dataDir.toFile().exists() || !dataDir.toFile().isDirectory())
			{
				log.error("Data directory does not exist: {}", dataDir);
				return 1;
			}

			log.info("Initializing database with engine: {}", engine);
			log.info("Data directory: {}", dataDir.toAbsolutePath());
			
			DbParameters dbParams = new DbParameters(host, port, host, user, password);

			SqlContext sqlCtxInit = engine.createSqlContext(host, port, user, password, 1);
	
			DbInitializer initializer = new DbInitializer(sqlCtxInit, dbParams, dataDir);

			long startTime = System.currentTimeMillis();
			initializer.initialize();
			long endTime = System.currentTimeMillis();

			log.info("Database initialization completed in {} seconds", (endTime - startTime) / 1000);
			return 0;
		}
		catch (Throwable t)
		{
			log.error("FATAL ERROR: An exception occurred during database initialization");
			ErrorAnalyser.findRootCause(t).printStackTrace();
			return 1;
		}
	}

	public static void main(String[] args)
	{
		CommandLine cmd = new CommandLine(new DbInitRunner());
		cmd.registerConverter(SqlEngine.class, new CaseInsensitiveEnumConverter());
		int exitCode = cmd.execute(args);
		System.exit(exitCode);
	}
}
