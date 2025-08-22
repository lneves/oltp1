package org.oltp1.runner;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.oltp1.common.CaseInsensitiveEnumConverter;
import org.oltp1.common.ErrorAnalyser;
import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.db.SqlEngine;
import org.oltp1.runner.generator.TxInputGenerator;
import org.oltp1.runner.perf.ConsoleReportWriter;
import org.oltp1.runner.perf.JsonReportWriter;
import org.oltp1.runner.perf.MixParameters;
import org.oltp1.runner.perf.MixRunner;
import org.oltp1.runner.perf.PeriodicTx;
import org.oltp1.runner.perf.ThreadPoolBuilder;
import org.oltp1.runner.perf.TxBaseLine;
import org.oltp1.runner.perf.TxOutput;
import org.oltp1.runner.perf.TxRunSummary;
import org.oltp1.runner.perf.TxStatsCollector;
import org.oltp1.runner.perf.TxVoid;
import org.oltp1.runner.tx.broker_volume.TxBrokerVolume;
import org.oltp1.runner.tx.customer_position.TxCustomerPosition;
import org.oltp1.runner.tx.data_maintenance.TxDataMaintenance;
import org.oltp1.runner.tx.market_watch.TxMarketWatch;
import org.oltp1.runner.tx.security_detail.TxSecurityDetail;
import org.oltp1.runner.tx.trade_cleanup.TxTradeCleanup;
import org.oltp1.runner.tx.trade_lookup.TxTradeLookup;
import org.oltp1.runner.tx.trade_order.TxTradeOrder;
import org.oltp1.runner.tx.trade_status.TxTradeStatus;
import org.oltp1.runner.tx.trade_update.TxTradeUpdate;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Query;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "driver", mixinStandardHelpOptions = true, description = "Runs OLTP1, a TPC-E inspired, benchmark against a Database server")
public class Oltp1Driver implements Callable<Integer>
{
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(Oltp1Driver.class);

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

	@Option(names = { "-d", "--duration" }, description = "Duration of the test run in seconds. [${DEFAULT-VALUE}]")
	public int duration = 360;

	@Option(names = { "-c", "--clients" }, description = "Number of simulated clients/users. [${DEFAULT-VALUE}]")
	public int clients = 10;

	@Option(names = { "-b", "--baseline" }, description = "Only execute a baseline query during the run")
	public boolean isBaselineRun = false;

	@Option(names = { "-w", "--wait-time" }, description = "Enable pacing to control the transaction rate")
	public boolean isPacingEnabled = false;

	@Option(names = { "--tps" }, description = "Target transactions per second for pacing. [${DEFAULT-VALUE}]")
	public int tps = 10;

	@Option(names = { "-j", "--json-output" }, description = "Output benchmark results in JSON format to a timestamped file in the current directory")
	public boolean enableJsonOutput = false;

	@Option(names = { "-q", "--quiet" }, description = "Disable logging of transaction errors and warnings")
	public boolean hideAlerts = false;

	@Override
	public Integer call() throws Exception
	{
		try
		{
			SqlContext sqlCtx = engine.createSqlContext(host, port, "tpce", user, password, clients);

			final String dbInfo = getDbInfo(sqlCtx);

			MixParameters mparams = new MixParameters(dbInfo, clients, isPacingEnabled, tps);
			MixRunner txMixRunner = new MixRunner(mparams);

			final long totalDurationSec = duration;

			if (isBaselineRun)
			{
				txMixRunner.addTx(new TxBaseLine(sqlCtx), 1.0);
				log.info("Running 'baseline' benchmark");
				txMixRunner.runTxMix(totalDurationSec);
			}
			else
			{
				final TxInputGenerator txInputGen = new TxInputGenerator(sqlCtx);

				int asyncPoolSize = calculatePoolSize(clients);
				final ExecutorService mee = ThreadPoolBuilder.newThreadPool(asyncPoolSize, "run-async");

				// # Read-Only Transactions
				// Broker Volume Mid-Heavy R/O 4.9%
				// Customer Position Mid-Heavy R/O 13%
				// Market Watch Medium R/O 18%
				// Security Detail Medium R/O 14%
				// Trade-Lookup Medium R/O 8%
				// Trade-Status Light R/O 19%
				//
				// # Read-Write Transactions
				// Data Maintenance Light R/W - 1 per minute
				// Trade-Cleanup Medium R/W - once at start
				//
				// Market Feed Medium R/W 1%
				// Trade-Order Heavy R/W 10.1%
				// Trade-Result Heavy R/W 10%
				// Trade-Update Medium R/W 2%

				log.info("Execute 'Trade-Cleanup' before test run");

				TxTradeCleanup tradeCleanup = new TxTradeCleanup(txInputGen, sqlCtx);

				TxOutput clnOut = tradeCleanup.execute();

				log.info("'Trade-Cleanup' finished: {}", clnOut.toString());

				txMixRunner.addPeriodic(new PeriodicTx(new TxDataMaintenance(txInputGen, sqlCtx), 0, 60, TimeUnit.SECONDS));
				txMixRunner.addTx(new TxBrokerVolume(txInputGen, sqlCtx), 0.049);
				txMixRunner.addTx(new TxCustomerPosition(txInputGen, sqlCtx), 0.13);
				txMixRunner.addTx(new TxMarketWatch(txInputGen, sqlCtx), 0.18);
				txMixRunner.addTx(new TxSecurityDetail(txInputGen, sqlCtx), 0.14);
				txMixRunner.addTx(new TxTradeLookup(txInputGen, sqlCtx), 0.08);
				txMixRunner.addTx(new TxTradeStatus(txInputGen, sqlCtx), 0.19);

				TxStatsCollector tradeResultStats = new TxStatsCollector("Trade-Result");
				TxStatsCollector mktFeedStats = new TxStatsCollector("Market-Feed");

				txMixRunner.addTx(new TxTradeOrder(txInputGen, sqlCtx, tradeResultStats, mktFeedStats, mee), 0.101);
				txMixRunner.addTx(new TxVoid(tradeResultStats), 0.1); // placeholder for TradeResult
				txMixRunner.addTx(new TxVoid(mktFeedStats), 0.01); // placeholder for MarketFeed

				txMixRunner.addTx(new TxTradeUpdate(txInputGen, sqlCtx), 0.02);

				long warmupDurationSec = calculateWarmupTime(totalDurationSec);

				// Ensure the measurement phase is at least 5 second to avoid errors.
				long measureDurationSec = Math.max(totalDurationSec - warmupDurationSec, 5);

				log.info("Total Run: {}s (Warmup: {}s, Measure: {}s)", totalDurationSec, warmupDurationSec, measureDurationSec);

				log.info("Starting warmup run");
				txMixRunner.runTxMix(measureDurationSec);

				log.info("Starting measurement run");
				txMixRunner.runTxMix(measureDurationSec);

				closeAsyncExec(mee);
			}

			TxRunSummary runSummary = txMixRunner.buildSummary();

			if (enableJsonOutput)
			{
				JsonReportWriter jsonWriter = new JsonReportWriter(sqlCtx.getSqlEngine());
				jsonWriter.accept(runSummary);
				log.info("JSON report written to: {}", jsonWriter.getOutputPath());
			}

			(new ConsoleReportWriter()).accept(runSummary);

			return 0;
		}
		catch (Throwable t)
		{
			log.error("\nFATAL ERROR: An exception occurred during execution.");
			ErrorAnalyser.findRootCause(t).printStackTrace();
			return 1;
		}
	}

	private long calculateWarmupTime(final long totalDurationSec)
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

	private int calculatePoolSize(final int clients)
	{
		final double tradeOrderPct = 0.101; // The transaction's mix percentage
		final double scalingFactor = 0.5; // A factor for short-lived tasks

		// The calculation numberOfClients * 0.101 gives rough estimate of the
		// peak number of Trade-Order transactions that might be completing
		// concurrently.
		// The scalingFactor = 0.5 reduces this number. It's a heuristic that
		// essentially says, "I only need a pool of threads half the size of the
		// theoretical peak because the tasks are so fast that a backlog is unlikely to
		// build up."
		// Calculate the pool size using the heuristic
		int asyncPoolSize = (int) Math.ceil(clients * tradeOrderPct * scalingFactor);

		asyncPoolSize = Math.max(2, asyncPoolSize); // Ensure a minimum of 2 threads
		return asyncPoolSize;
	}

	private void closeAsyncExec(final ExecutorService asyncExecutor)
	{
		asyncExecutor.shutdown();
		try
		{
			if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS))
			{
				asyncExecutor.shutdownNow();
			}
		}
		catch (InterruptedException e)
		{
			asyncExecutor.shutdownNow();
			Thread.currentThread().interrupt();
		}
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
// Transaction Frame Reason for Warning
// Trade-Lookup 2 +621 num_found == 0
// Trade-Lookup 3 +631 num_found == 0
// Trade-Lookup 4 +641 num_trades_found == 0
// Trade-Update 2 +1021 num_updated == 0
// Trade-Update 3 +1031 num_found == 0