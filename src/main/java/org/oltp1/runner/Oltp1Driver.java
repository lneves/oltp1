package org.oltp1.runner;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.oltp1.common.CaseInsensitiveEnumConverter;
import org.oltp1.common.ErrorAnalyser;
import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.db.SqlEngine;
import org.oltp1.runner.generator.TxInputGenerator;
import org.oltp1.runner.runtime.BenchmarkMetrics;
import org.oltp1.runner.runtime.ConsoleReportWriter;
import org.oltp1.runner.runtime.ExchangeEmulator;
import org.oltp1.runner.runtime.JsonReportWriter;
import org.oltp1.runner.runtime.MarketExchangeEmulator;
import org.oltp1.runner.runtime.NoOpExchangeEmulator;
import org.oltp1.runner.runtime.Pacer;
import org.oltp1.runner.runtime.ProgressMonitor;
import org.oltp1.runner.runtime.TxBase;
import org.oltp1.runner.runtime.WorkLoadClient;
import org.oltp1.runner.tx.data_maintenance.TxDataMaintenance;
import org.oltp1.runner.tx.trade_cleanup.TxTradeCleanup;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Query;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "driver", mixinStandardHelpOptions = false, description = "Runs OLTP1, a TPC-E inspired, benchmark against a Database server")
public class Oltp1Driver implements Callable<Integer>
{
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(Oltp1Driver.class);

	@Option(names = { "--help" }, usageHelp = true, description = "Show this help message and exit")
	public boolean helpRequested;

	@Option(names = { "-h", "--host" }, description = "Database host", required = true)
	public String host;

	@Option(names = { "-p", "--port" }, description = "Database listening port")
	public int port;

	@Option(names = { "-U", "--user" }, description = "Database username", required = true)
	public String user;

	@Option(names = { "-P", "--password" }, defaultValue = "${env:OLTP1_PASSWORD}", description = "Database user password, You can also set OLTP1_PASSWORD in the environment.", required = true)
	public String password;

	@Option(names = { "-e", "--engine" }, description = "Database Engine under test, valid values: ${COMPLETION-CANDIDATES}", required = true)
	public SqlEngine engine;

	@Option(names = { "-d", "--duration" }, description = "Total run duration in seconds, including warm-up. The first 15% is warm-up (clamped to 180-600s) and is excluded from the measured results. [${DEFAULT-VALUE}]")
	public int duration = 360;

	@Option(names = { "-c", "--clients" }, description = "Number of simulated clients/users. [${DEFAULT-VALUE}]")
	public int clients = 10;

	@Option(names = { "-b", "--baseline" }, description = "Only execute a baseline query during the run")
	public boolean isBaselineRun = false;

	@Option(names = { "-w", "--pacing" }, description = "Enable pacing to control the transaction rate")
	public boolean isPacingEnabled = false;

	@Option(names = { "--tps" }, description = "Target transactions per second for pacing. [${DEFAULT-VALUE}]")
	public int tps = 10;

	@Option(names = { "-j", "--json-output" }, description = "Output benchmark results in JSON format to a timestamped file in the current directory")
	public boolean enableJsonOutput = false;

	@Option(names = { "-q", "--quiet" }, description = "Disable logging of transaction errors and warnings")
	public boolean hideAlerts = false;

	@Option(names = { "--skip-permission-validation" }, description = "Warn instead of aborting at startup when ACCOUNT_PERMISSION owner coverage is inconsistent")
	public boolean skipPermissionValidation = false;

	@Override
	public Integer call() throws Exception
	{
		try
		{
			validate();

			TxBase.setQuiet(hideAlerts);

			int asyncMeePoolSize = calculateMeePoolSize(clients);
			int maximumConnPoolSize = clients + asyncMeePoolSize + 2;

			try (SqlContext sqlCtx = engine.createSqlContext(host, port, "tpce", user, password, maximumConnPoolSize))
			{
				final String dbInfo = getDbInfo(sqlCtx);

				log
						.info(
								"Initialized Db Connection pool for engine {} at {}:{} (maxPoolSize={})",
								dbInfo,
								host,
								port,
								maximumConnPoolSize);

				final Pacer pacer = new Pacer(isPacingEnabled, tps);

				final long totalDurationSec = duration;
				final long warmupDurationSec = calculateWarmupTime(totalDurationSec);
				final long measureDurationSec = Math.max(totalDurationSec - warmupDurationSec, 5);

				log
						.info(
								"Warm-up duration: {} s, measurement duration: {} s",
								warmupDurationSec,
								measureDurationSec);

				final TxInputGenerator txInputGen = new TxInputGenerator(sqlCtx, !skipPermissionValidation);

				if (!isBaselineRun)
				{
					final TxTradeCleanup cu = new TxTradeCleanup(txInputGen, sqlCtx);
					cu.execute();
				}

				log.info("Starting warmup run");
				runTasks(sqlCtx, txInputGen, pacer, warmupDurationSec);

				log.info("Starting measurement run");
				BenchmarkMetrics metrics = runTasks(sqlCtx, txInputGen, pacer, measureDurationSec);

				if (enableJsonOutput)
				{
					JsonReportWriter jsonWriter = new JsonReportWriter(dbInfo);
					jsonWriter.accept(dbInfo, metrics);
					log.info("JSON report written to: {}", jsonWriter.getOutputPath());
				}

				(new ConsoleReportWriter()).accept(dbInfo, metrics);

				return 0;
			}
			catch (Throwable t)
			{
				throw new RuntimeException(t);
			}

		}
		catch (Throwable t)
		{
			log.error("\nFATAL ERROR: An exception occurred during execution.");
			ErrorAnalyser.findRootCause(t).printStackTrace();
			return 1;
		}
	}

	private BenchmarkMetrics runTasks(SqlContext sqlCtx, TxInputGenerator txInputGen, final Pacer pacer, final long durationSec) throws Exception
	{
		BenchmarkMetrics metrics = new BenchmarkMetrics(clients);

		int asyncMeePoolSize = calculateMeePoolSize(clients);

		try (
				final ExchangeEmulator mee = isBaselineRun ? new NoOpExchangeEmulator() : new MarketExchangeEmulator(sqlCtx, txInputGen, asyncMeePoolSize, metrics);
				final ProgressMonitor progressMonitor = new ProgressMonitor(metrics, durationSec);)
		{

			final ExecutorService executor = Executors
					.newFixedThreadPool(
							clients,
							Thread.ofPlatform().name("client-", 0).factory());

			final ScheduledExecutorService dmExec = Executors.newSingleThreadScheduledExecutor();

			metrics.markStart();

			if (!isBaselineRun)
			{
				final TxDataMaintenance dm = new TxDataMaintenance(txInputGen, sqlCtx, metrics);
				dmExec.scheduleAtFixedRate(dm::execute, 0, 60, TimeUnit.SECONDS);
			}

			progressMonitor.start();

			for (int i = 0; i < clients; i++)
			{
				final WorkLoadClient wclient = new WorkLoadClient(sqlCtx, txInputGen, metrics, mee, isBaselineRun);

				executor.submit(() -> wclient.runTxMix(durationSec, TimeUnit.SECONDS, pacer));
			}

			// Wait for all the benchmark tasks to finish
			executor.shutdown();

			if (!executor.awaitTermination(2 * durationSec, TimeUnit.SECONDS))
			{
				executor.shutdownNow();
				log.error("Foreground clients did not terminate");
			}

			dmExec.shutdown();
			if (!dmExec.awaitTermination(2 * durationSec, TimeUnit.SECONDS))
			{
				dmExec.shutdownNow();
				log.error("Asynchronous clients did not terminate");
			}
			
			mee.stopAccepting();
			long remaining = mee.drain();
			metrics.markEnd();
			progressMonitor.setDraining(true); 

			if (remaining > 0)
			{
				log.warn("MEE did not drain: {}", remaining);
			}
		}
		return metrics;
	}

	void validate()
	{
		if (clients < 1)
		{
			throw new IllegalArgumentException("--clients must be >= 1");
		}

		if (duration < 1)
		{
			throw new IllegalArgumentException("--duration must be >= 1");
		}

		if (port < 0 || port > 65535)
		{
			throw new IllegalArgumentException("--port must be between 0 and 65535");
		}

		if (isPacingEnabled && tps < 1)
		{
			throw new IllegalArgumentException("--tps must be >= 1 when --pacing is enabled");
		}
	}

	long calculateWarmupTime(final long totalDurationSec)
	{
		long warmupDurationSec;

		// The ideal heuristic: 15% of total time, capped between 3 minutes (180s) and
		// 10 minutes (600s).
		long idealWarmupSec = (long) (totalDurationSec * 0.15);
		idealWarmupSec = Math.max(180, Math.min(idealWarmupSec, 600));

		// **FIX**: Guard against the total duration being too short for the ideal
		// warmup.
		if (idealWarmupSec >= totalDurationSec)
		{
			// For very short runs where the ideal warmup is too long, fall back to a simple
			// 50/50 split.
			// This ensures the measurement phase always gets time to run.
			warmupDurationSec = totalDurationSec / 2;
		}
		else
		{
			warmupDurationSec = idealWarmupSec;
		}
		return warmupDurationSec;
	}

	private int calculateMeePoolSize(final int clients)
	{
		final double tradeOrderPct = 0.101; // The transaction's mix percentage
		// The calculation numberOfClients * 0.101 gives rough estimate of the
		int asyncPoolSize = (int) Math.ceil(clients * tradeOrderPct);

		asyncPoolSize = Math.max(2, asyncPoolSize); // Ensure a minimum of 2 threads
	
		return 6;
	}

	private String getDbInfo(SqlContext sqlCtx)
	{
		try (Connection con = sqlCtx.getSql2o().open())
		{
			Query tx = con.createQuery(sqlCtx.getSqlEngine().getInfoQuery());

			String dbInfo0 = tx.executeAndFetchFirst(String.class);

			return dbInfo0;
		}
	}

	public static void main(String[] args)
	{
		Oltp1Driver driver = new Oltp1Driver();
		CommandLine cmd = new CommandLine(driver);
		cmd.registerConverter(SqlEngine.class, new CaseInsensitiveEnumConverter());

		int exitCode = cmd.execute(args);

		System.exit(exitCode);
	}
}