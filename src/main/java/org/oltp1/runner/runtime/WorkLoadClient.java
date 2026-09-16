package org.oltp1.runner.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.oltp1.common.Assert;
import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.generator.TxInputGenerator;
import org.oltp1.runner.tx.broker_volume.TxBrokerVolume;
import org.oltp1.runner.tx.customer_position.TxCustomerPosition;
import org.oltp1.runner.tx.market_watch.TxMarketWatch;
import org.oltp1.runner.tx.security_detail.TxSecurityDetail;
import org.oltp1.runner.tx.trade_lookup.TxTradeLookup;
import org.oltp1.runner.tx.trade_order.TxTradeOrder;
import org.oltp1.runner.tx.trade_status.TxTradeStatus;
import org.oltp1.runner.tx.trade_update.TxTradeUpdate;

public class WorkLoadClient
{
	private final NavigableMap<Double, TxBase> txMixRange = new TreeMap<Double, TxBase>();
	private final List<ImmutablePair<TxBase, Double>> txMix = new ArrayList<>();;
	private final AtomicBoolean isRunning = new AtomicBoolean(true);

	public WorkLoadClient(SqlContext sqlCtx, TxInputGenerator txInputGen, BenchmarkMetrics metrics, ExchangeEmulator mee)
	{
		this(sqlCtx, txInputGen, metrics, mee, false);
	}

	public WorkLoadClient(SqlContext sqlCtx, TxInputGenerator txInputGen, BenchmarkMetrics metrics, ExchangeEmulator mee, boolean isBaselineWorkload)
	{
		super();

		if (isBaselineWorkload)
		{
			addTx(new TxBaseLine(sqlCtx, metrics), 1.0);
		}
		else
		{
			// # Read-Only Transactions
			// Broker Volume Mid-Heavy R/O 4.9%
			// Customer Position Mid-Heavy R/O 13%
			// Market Watch Medium R/O 18%
			// Security Detail Medium R/O 14%
			// Trade-Lookup Medium R/O 8%
			// Trade-Status Light R/O 19%
			//
			// # Read-Write Transactions
			// Trade-Update Medium R/W 2%
			// Trade-Order Heavy R/W 10.1%
			// Trade-Result Heavy R/W 10%
			// Market Feed Medium R/W 1%
			//
			// Data Maintenance Light R/W - 1 per minute -> Not in Tx. Mix
			// Trade-Cleanup Medium R/W - once at start -> Not in Tx. Mix

			// R/O
			addTx(new TxBrokerVolume(txInputGen, sqlCtx, metrics), 0.049);
			addTx(new TxCustomerPosition(txInputGen, sqlCtx, metrics), 0.13);
			addTx(new TxMarketWatch(txInputGen, sqlCtx, metrics), 0.18);
			addTx(new TxSecurityDetail(txInputGen, sqlCtx, metrics), 0.14);
			addTx(new TxTradeLookup(txInputGen, sqlCtx, metrics), 0.08);
			addTx(new TxTradeStatus(txInputGen, sqlCtx, metrics), 0.19);

			// R/W
			addTx(new TxTradeUpdate(txInputGen, sqlCtx, metrics), 0.02);
			addTx(new TxTradeOrder(txInputGen, sqlCtx, metrics, mee), 0.101);
		}

		buildTxMix();
	}

	public void runTxMix(final long runDuration, TimeUnit tu, Pacer pacer)
	{
		final long startTime = System.currentTimeMillis();
		final long totalDurationMs = tu.toMillis(runDuration);

		long remainingMs = totalDurationMs;
		while (isRunning.get() && (remainingMs > 0) && (!Thread.currentThread().isInterrupted()))
		{
			// pick the nearest tx with a key > than a random number
			final double p = ThreadLocalRandom.current().nextDouble();
			Entry<Double, TxBase> entry = txMixRange.ceilingEntry(p);
			final TxBase tx = (entry != null) ? entry.getValue() : txMixRange.lastEntry().getValue();
			pacer.acquire();
			tx.execute();

			remainingMs = totalDurationMs - (System.currentTimeMillis() - startTime);
		}
		stop();
	}

	public boolean isRunning()
	{
		return isRunning.get();
	}

	public void stop()
	{
		isRunning.set(false);
	}

	private void addTx(TxBase tx, double mixPct)
	{
		txMix.add(ImmutablePair.of(tx, mixPct));
	}

	private void buildTxMix()
	{
		// Only the transactions initiated by the workload clients are selectable here.
		// Their weights sum to 0.89 because Trade-Result and Market-Feed are produced
		// asynchronously by the MEE. getPctFactor() renormalizes the cumulative keys to
		// cover [0,1), so each client transaction is selected about 1/0.89 (~12.4%)
		// more often than its TPC-E percentage (Trade-Order ~11.35%, not 10.1%).
		// The report prints the nominal TPC-E weight as Target(%) and the observed
		// share of all attempted transactions (including MEE and Data-Maintenance)
		// as Actual(%), so the two are not expected to match.
		final double factor = getPctFactor();

		// populate a "range" map for the transactions
		txMix.forEach(p -> {
			TxBase tx = p.left;
			double mixPct = p.right * factor;
			Entry<Double, TxBase> lastTx = txMixRange.lastEntry();

			if (lastTx == null)
			{
				txMixRange.put(mixPct, tx);
			}
			else
			{
				double nextKey = lastTx.getKey() + mixPct;
				txMixRange.put(nextKey, tx);
			}
		});
	}

	private double getPctFactor()
	{
		double mixPctSum = txMix
				.stream()
				.mapToDouble(p -> p.right)
				.sum();

		Assert.isInRange("mixPctSum", mixPctSum, 0.0, 1.0);

		final double factor = 1.0 / mixPctSum; // for the cases where all the sum of all tx < 100%
		return factor;
	}
}
