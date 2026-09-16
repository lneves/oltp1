package org.oltp1.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.oltp1.common.CaseInsensitiveEnumConverter;
import org.oltp1.runner.db.SqlEngine;

import picocli.CommandLine;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.PicocliException;

class Oltp1DriverConfigTest
{
	@Test
	void warmupIsFifteenPercentWithinBounds()
	{
		Oltp1Driver driver = new Oltp1Driver();

		assertEquals(30L, driver.calculateWarmupTime(60));
		assertEquals(180L, driver.calculateWarmupTime(360));
		assertEquals(180L, driver.calculateWarmupTime(600));
		assertEquals(180L, driver.calculateWarmupTime(1200));
		assertEquals(600L, driver.calculateWarmupTime(4000));
		assertEquals(600L, driver.calculateWarmupTime(36000));
	}

	@Test
	void rejectsNonPositiveClients()
	{
		Oltp1Driver driver = new Oltp1Driver();
		driver.clients = 0;

		assertThrows(IllegalArgumentException.class, driver::validate);
	}

	@Test
	void rejectsNonPositiveDuration()
	{
		Oltp1Driver driver = new Oltp1Driver();
		driver.duration = 0;

		assertThrows(IllegalArgumentException.class, driver::validate);
	}

	@Test
	void rejectsNonPositiveTpsWhenPacingIsEnabled()
	{
		Oltp1Driver driver = new Oltp1Driver();
		driver.isPacingEnabled = true;
		driver.tps = 0;

		assertThrows(IllegalArgumentException.class, driver::validate);
	}

	@Test
	void acceptsValidConfiguration()
	{
		Oltp1Driver driver = new Oltp1Driver();
		driver.clients = 1;
		driver.duration = 1;
		driver.isPacingEnabled = true;
		driver.tps = 1;

		driver.validate();
	}

	@Test
	void helpIsBoundToLongOptionOnly()
	{
		CommandLine cmd = new CommandLine(new Oltp1Driver());
		cmd.registerConverter(SqlEngine.class, new CaseInsensitiveEnumConverter());

		ParseResult result = cmd
				.parseArgs(
						"--host",
						"localhost",
						"--user",
						"admin",
						"--password",
						"secret",
						"--engine",
						"PGSQL",
						"--help");

		assertTrue(result.isUsageHelpRequested());
	}

	@Test
	void shortDashHRemainsTheHostOption()
	{
		Oltp1Driver driver = new Oltp1Driver();
		CommandLine cmd = new CommandLine(driver);
		cmd.registerConverter(SqlEngine.class, new CaseInsensitiveEnumConverter());

		cmd
				.parseArgs(
						"-h",
						"localhost",
						"-U",
						"admin",
						"-P",
						"secret",
						"-e",
						"PGSQL");

		assertEquals("localhost", driver.host);
	}

	@Test
	void pacingOptionIsRecognized()
	{
		Oltp1Driver driver = new Oltp1Driver();
		CommandLine cmd = new CommandLine(driver);
		cmd.registerConverter(SqlEngine.class, new CaseInsensitiveEnumConverter());

		cmd.parseArgs(
				"-h",
				"localhost",
				"-U",
				"admin",
				"-P",
				"secret",
				"-e",
				"PGSQL",
				"--pacing",
				"--tps",
				"25");

		assertTrue(driver.isPacingEnabled);
		assertEquals(25, driver.tps);
		assertFalse(driver.helpRequested);
	}

	@Test
	void rejectsUnknownEngine()
	{
		CommandLine cmd = new CommandLine(new Oltp1Driver());
		cmd.registerConverter(SqlEngine.class, new CaseInsensitiveEnumConverter());

		assertThrows(
				PicocliException.class,
				() -> cmd
						.parseArgs(
								"-h",
								"localhost",
								"-U",
								"admin",
								"-P",
								"secret",
								"-e",
								"NOT_A_DB"));
	}
}
