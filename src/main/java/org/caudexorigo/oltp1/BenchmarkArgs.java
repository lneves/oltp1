package org.caudexorigo.oltp1;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "OLTP1", mixinStandardHelpOptions = true, description = "Runs OLTP1, a TPC-E inspired, benchmark against a Database server")
public class BenchmarkArgs 
{
	@Option(names = { "-h", "--host" }, description = "Database host", required = true)
	public String host;

	@Option(names = { "-p", "--port" }, description = "Database listening port")
	public int port;

	@Option(names = { "-U", "--user" }, description = "Database username", required = true)
	public String user;

	@Option(names = { "-P", "--password" }, description = "Database user password", required = true)
	public String password;

	@Option(names = { "-e", "--engine" }, description = "Database Engine under test, valid values: ${COMPLETION-CANDIDATES}", required = true)
	public DbEngine engine;

	@Option(names = { "-d", "--duration" }, description = "Duration of the test run in seconds. [${DEFAULT-VALUE}]", required = true)
	public int duration = 360;

	@Option(names = { "-c", "--clients" }, description = "Number of simulated clients/users. [${DEFAULT-VALUE}]", required = true)
	public int clients = 10;

	@Option(names = { "-b", "--baseline" }, description = "Only execute a baseline query during the run")
	public boolean is_baseline = false;

	@Option(names = { "-w", "--wait-time" }, description = "Enable pacing to control the transaction rate")
	public boolean is_pacing = false;

	@Option(names = { "--tps" }, description = "Target transactions per second for pacing. [${DEFAULT-VALUE}]")
	public int tps = 10;

	@Override
	public String toString()
	{
		return String.format("BenchmarkArgs [host=%s, port=%s, user=%s, engine=%s, duration=%s, clients=%s, is_baseline=%s]", host, port, user, engine, duration, clients, is_baseline);
	}
}
