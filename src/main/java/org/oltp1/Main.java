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
}
