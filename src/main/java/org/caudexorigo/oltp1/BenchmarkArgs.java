package org.caudexorigo.oltp1;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "OLTP1", mixinStandardHelpOptions = true, description = "Runs OLTP1, a TPC-E inspired, benchmark against a Database server")
public class BenchmarkArgs 
{
	@Option(names = { "-h", "--host" }, description = "Database host", required = true)
	String host;

	@Option(names = { "-p", "--port" }, description = "Database listening port")
	int port;

	@Option(names = { "-U", "--user" }, description = "Database username", required = true)
	String user;

	@Option(names = { "-P", "--password" }, description = "Database user password", required = true)
	String password;

	@Option(names = { "-e", "--engine" }, description = "Database Engine under test, valid values: ${COMPLETION-CANDIDATES}", required = true)
	DbEngine engine;

	@Option(names = { "-d", "--duration" }, description = "Duration of the test run in seconds. [${DEFAULT-VALUE}]", required = true)
	int duration = 360;

	@Option(names = { "-c", "--clients" }, description = "Number of simulated clients/users. [${DEFAULT-VALUE}]", required = true)
	int clients = 10;

	@Option(names = { "-b", "--baseline" }, description = "Only execute a baseline query during the run")
	boolean is_baseline = false;


	@Override
	public String toString()
	{
		return String.format("BenchmarkArgs [host=%s, port=%s, user=%s, engine=%s, duration=%s, clients=%s, is_baseline=%s]", host, port, user, engine, duration, clients, is_baseline);
	}
}
