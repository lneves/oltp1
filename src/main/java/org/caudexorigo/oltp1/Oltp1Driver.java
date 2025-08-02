package org.caudexorigo.oltp1;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.caudexorigo.db.SqlContext;
import org.caudexorigo.oltp1.generator.TxInputGenerator;
import org.caudexorigo.oltp1.tx.broker_volume.TxBrokerVolume;
import org.caudexorigo.oltp1.tx.customer_position.TxCustomerPosition;
import org.caudexorigo.oltp1.tx.data_maintenance.TxDataMaintenance;
import org.caudexorigo.oltp1.tx.market_watch.TxMarketWatch;
import org.caudexorigo.oltp1.tx.security_detail.TxSecurityDetail;
import org.caudexorigo.oltp1.tx.trade_cleanup.TxTradeCleanup;
import org.caudexorigo.oltp1.tx.trade_lookup.TxTradeLookup;
import org.caudexorigo.oltp1.tx.trade_order.TxTradeOrder;
import org.caudexorigo.oltp1.tx.trade_status.TxTradeStatus;
import org.caudexorigo.oltp1.tx.trade_update.TxTradeUpdate;
import org.caudexorigo.perf.ConsoleReportWriter;
import org.caudexorigo.perf.ErrorAnalyser;
import org.caudexorigo.perf.MixRunner;
import org.caudexorigo.perf.PeriodicTx;
import org.caudexorigo.perf.ThreadPoolBuilder;
import org.caudexorigo.perf.TxBaseLine;
import org.caudexorigo.perf.TxOutput;
import org.caudexorigo.perf.TxRunSummary;
import org.caudexorigo.perf.TxStatsCollector;
import org.caudexorigo.perf.TxVoid;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Query;

import picocli.CommandLine;

public class Oltp1Driver
{
	public static void main(String[] args)
	{
		System.setProperty("chronicle.silent", "true");
		System.setProperty("chronicle.analytics.disable", "true");
		System.setProperty("chronicle.release.notes.suppress", "true");

		org.slf4j.Logger log = LoggerFactory.getLogger(Oltp1Driver.class);

		try
		{

			BenchmarkArgs bargs = new BenchmarkArgs();

			CommandLine cmd = new CommandLine(bargs);
			cmd.registerConverter(DbEngine.class, new CaseInsensitiveEnumConverter());
			cmd.parseArgs(args);

			System.out.println(bargs.toString());

			// String logLevel = false ? "WARN" : "ERROR";
			//
			// // Set log level for the Tx logger
			// Logger txLogger = (Logger)
			// LoggerFactory.getLogger("org.caudexorigo.perf.TxBase");
			// txLogger.setLevel(Level.toLevel(logLevel, Level.ERROR)); // fallback to ERROR

			final int clients = bargs.clients;

			SqlContext sqlCtx = bargs.engine.createSqlContext(bargs, clients);


			final String dbInfo = getDbInfo(sqlCtx);

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

			MixRunner txMixRunner = new MixRunner(dbInfo, bargs);

			final long totalDurationSec = bargs.duration;

			if (bargs.is_baseline)
			{
				txMixRunner.addTx(new TxBaseLine(sqlCtx), 1.0);
				log.info("Running benchmark run");
				txMixRunner.runTxMix(totalDurationSec);
			}
			else
			{
				final TxInputGenerator txInputGen = new TxInputGenerator(sqlCtx);

				int asyncPoolSize = calculatePoolSize(clients);
				final ExecutorService asyncExecutor = ThreadPoolBuilder.newThreadPool(asyncPoolSize, "run-async");

				log.info("Run Trade-Cleanup before test run");

				TxTradeCleanup tradeCleanup = new TxTradeCleanup(txInputGen, sqlCtx);

				TxOutput clnOut = tradeCleanup.execute();

				log.info("Trade-Cleanup finished: {}", clnOut.toString());

				txMixRunner.addPeriodic(new PeriodicTx(new TxDataMaintenance(txInputGen, sqlCtx), 0, 60, TimeUnit.SECONDS));
				txMixRunner.addTx(new TxBrokerVolume(txInputGen, sqlCtx), 0.049);
				txMixRunner.addTx(new TxCustomerPosition(txInputGen, sqlCtx), 0.13);
				txMixRunner.addTx(new TxMarketWatch(txInputGen, sqlCtx), 0.18);
				txMixRunner.addTx(new TxSecurityDetail(txInputGen, sqlCtx), 0.14);
				txMixRunner.addTx(new TxTradeLookup(txInputGen, sqlCtx), 0.08);
				txMixRunner.addTx(new TxTradeStatus(txInputGen, sqlCtx), 0.19);

				TxStatsCollector tradeResultStats = new TxStatsCollector("Trade-Result");
				TxStatsCollector mktFeedStats = new TxStatsCollector("Market-Feed");

				txMixRunner.addTx(new TxTradeOrder(txInputGen, sqlCtx, tradeResultStats, mktFeedStats, asyncExecutor), 0.101);
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

				// Shut down the ExecutorService at the end of the run
				closeAsyncExec(asyncExecutor);
			}

			TxRunSummary runSummary = txMixRunner.buildSummary();

			System.out.println("\n");

			(new ConsoleReportWriter()).accept(runSummary);
			// (new HtmlReportWriter("./target/",
			// sqlCtx.getSqlEngine().getAbrev())).accept(runSummary);

			System.exit(0);
		}
		catch (Throwable t)
		{
			ErrorAnalyser.shutdown(t, log);
		}
	}

	private static long calculateWarmupTime(final long totalDurationSec)
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

	private static int calculatePoolSize(final int clients)
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

	private static void closeAsyncExec(final ExecutorService asyncExecutor)
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

	private static String getDbInfo(SqlContext sqlCtx)
	{
		try (Connection con = sqlCtx.getSql2o().open())
		{
			Query tx = con.createQuery(sqlCtx.getSqlEngine().getInfoQuery());

			String dbInfo0 = tx.executeAndFetchFirst(String.class);

			return dbInfo0;
		}
	}
}
// Transaction Frame Reason for Warning
// Trade-Lookup 2 +621 num_found == 0
// Trade-Lookup 3 +631 num_found == 0
// Trade-Lookup 4 +641 num_trades_found == 0
// Trade-Update 2 +1021 num_updated == 0
// Trade-Update 3 +1031 num_found == 0