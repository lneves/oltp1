package org.oltp1;

import org.oltp1.common.CaseInsensitiveEnumConverter;
import org.oltp1.egen.EGenLoader;
import org.oltp1.initdb.DbInitRunner;
import org.oltp1.runner.Oltp1Driver;
import org.oltp1.runner.db.SqlEngine;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "oltp1", description = "TPC-E benchmark tool", mixinStandardHelpOptions = true, // adds -h/--help and -V/--version
		version = "oltp1 1.0", subcommands = {
				EGenLoader.class,
				DbInitRunner.class,
				Oltp1Driver.class
		})
public class Main implements Runnable
{
	public static void main(String[] args)
	{
		//setUpLog();
		System.setProperty("logback.statusListenerClass",
		        "ch.qos.logback.core.status.NopStatusListener");
        
		CommandLine cmd = new CommandLine(new Main());
		cmd.registerConverter(SqlEngine.class, new CaseInsensitiveEnumConverter());
		int exitCode = cmd.execute(args);
		System.exit(exitCode);
	}

	@Override
	public void run()
	{
		new CommandLine(this).usage(System.out);
	}

//	private static void setUpLog()
//	{
//		System.out.println("Main.setUpLog()");
//		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
//		context.reset();
//
//		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
//		encoder.setContext(context);
//		encoder.setPattern("%m%n");
//		encoder.start();
//
//		ConsoleAppender<ILoggingEvent> stdoutAppender = new ConsoleAppender<ILoggingEvent>();
//		stdoutAppender.setContext(context);
//		stdoutAppender.setName("stdout");
//		stdoutAppender.setEncoder(encoder);
//		stdoutAppender.start();
//
//		PatternLayoutEncoder errEncoder = new PatternLayoutEncoder();
//		errEncoder.setContext(context);
//		errEncoder.setPattern("%m%n");
//		errEncoder.start();
//
//		ConsoleAppender<ILoggingEvent> stderrAppender = new ConsoleAppender<ILoggingEvent>();
//		stderrAppender.setContext(context);
//		stderrAppender.setName("stderr");
//		stderrAppender.setTarget("System.err");
//		stderrAppender.setEncoder(errEncoder);
//		stderrAppender.start();
//
//		String[] errorLoggers = { "org.apache", "com.zaxxer.hikari", "org.sql2o", "ch.qos.logback" };
//		for (String name : errorLoggers)
//		{
//			Logger logger = context.getLogger(name);
//			logger.setLevel(Level.ERROR);
//			logger.setAdditive(false);
//			logger.addAppender(stderrAppender);
//		}
//		Logger root = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
//		root.setLevel(Level.INFO);
//		root.addAppender(stdoutAppender);
//	}
}
