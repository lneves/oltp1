package org.oltp1.runner.perf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.oltp1.common.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixRunner
{
	private static Logger log = LoggerFactory.getLogger(MixRunner.class);

	private final RandomUtils rnd = RandomUtils.insecure();

	private final List<ImmutablePair<TxBase, Double>> txMix;
	private final List<PeriodicTx> txPeriodic;
	private final TxRunSummary runSummary;
	private final int numClients;
	private final AtomicLong transactionCounter = new AtomicLong(0);

	private final boolean isPacingEnabled;
	private final long interTransactionDelayNanos;

	public MixRunner(MixParameters params)
	{
		super();
		this.txMix = new ArrayList<>();
		this.txPeriodic = new ArrayList<>();
		this.numClients = params.clients;
		this.runSummary = new TxRunSummary(params.sutInfo, numClients);

		this.isPacingEnabled = params.isPacingEnabled;
		if (isPacingEnabled)
		{
			double targetTpsPerClient = (double) params.tps / numClients;
			this.interTransactionDelayNanos = (long) (1_000_000_000.0 / targetTpsPerClient);
		}
		else
		{
			this.interTransactionDelayNanos = 0;
		}
	}

	public void addPeriodic(PeriodicTx tx)
	{
		txPeriodic.add(tx);
	}

	public void addTx(TxBase tx, double mixPct)
	{
		txMix.add(ImmutablePair.of(tx, mixPct));
	}

	public void runTxMix(final long runDuration)
	{
		final double factor = getPctFactor();

		txMix.forEach(pair -> pair.left.clearStats());
		txPeriodic.forEach(t -> t.getTx().clearStats());

		final ConcurrentNavigableMap<Double, TxBase> txMixRange = new ConcurrentSkipListMap<Double, TxBase>();

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

		final Runnable clientTx = () -> {

			long startTime = 0;
			if (isPacingEnabled)
			{
				startTime = System.nanoTime();
			}

			// pick the nearest tx with a key > than a random number
			final double p = rnd.randomDouble(0.0, 1.0);
			final TxBase tx = txMixRange.ceilingEntry(p).getValue();
			tx.execute();
			transactionCounter.incrementAndGet();

			if (isPacingEnabled)
			{
				long endTime = System.nanoTime();
				long transactionDurationNanos = endTime - startTime;
				long waitTimeNanos = interTransactionDelayNanos - transactionDurationNanos;

				if (waitTimeNanos > 0)
				{
					try
					{
						long waitMillis = waitTimeNanos / 1_000_000;
						int waitNanos = (int) (waitTimeNanos % 1_000_000);
						Thread.sleep(waitMillis, waitNanos);
					}
					catch (InterruptedException e)
					{
						Thread.currentThread().interrupt();
					}
				}
			}
		};

		final ScheduledExecutorService schedExec = Executors.newScheduledThreadPool(1);
		final AtomicBoolean isRunning = new AtomicBoolean(true);

		schedExec.schedule(() -> isRunning.set(false), runDuration, TimeUnit.SECONDS);
		transactionCounter.set(0); // Reset counter for the run
		final long startTime = System.currentTimeMillis();
		final long totalDurationMs = runDuration * 1000;

		Runnable progressReporter = () -> {
			long elapsedMs = System.currentTimeMillis() - startTime;
			elapsedMs = Math.min(elapsedMs, totalDurationMs); // Cap at total duration

			double percentComplete = (double) elapsedMs / totalDurationMs;
			double elapsedSeconds = elapsedMs / 1000.0;
			long currentTps = (elapsedSeconds > 0) ? (long) (transactionCounter.get() / elapsedSeconds) : 0;

			int progressChars = (int) (percentComplete * 25); // 25 characters for the bar
			String bar = "=".repeat(progressChars) + ">";

			String output = String
					.format(
							"Progress: [%-26s] %d%% | Elapsed: %ds/%ds | Rate: %,d tx/s",
							bar,
							(int) (percentComplete * 100),
							(int) elapsedSeconds,
							runDuration,
							currentTps);

			System.out.printf("\r%s", output); // Use carriage return to update the line
		};

		schedExec.scheduleWithFixedDelay(progressReporter, 1, 1, TimeUnit.SECONDS);

		txPeriodic.forEach(t -> {

			schedExec.scheduleWithFixedDelay(() -> t.getTx().execute(), t.getInitialDelay(), t.getDelay(), t.getUnit());

		});

		final ExecutorService clients = ThreadPoolBuilder.newThreadPool(numClients, "run-loader");

		if (txMixRange.size() > 0)
		{
			while (isRunning.get())
			{
				clients.execute(clientTx);
			}
			System.out.println("");
		}
		else
		{
			log.warn("No transaction in the mix");
		}

		schedExec.shutdown();
		clients.shutdown();

		try
		{
			clients.awaitTermination(runDuration, TimeUnit.SECONDS);

			// Print a final 100% line to ensure it's complete
			progressReporter.run();
			System.out.println(); // Move to the next line after the progress bar is done
		}
		catch (InterruptedException ie)
		{
			Thread.currentThread().interrupt();
			throw new RuntimeException(ie);
		}
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

	public TxRunSummary buildSummary()
	{
		runSummary.clearSummary();

		final double factor = getPctFactor();

		List<TxSummary> txMixStats = txMix.stream().map(p -> {

			TxSummary s = p.left.getStats();
			s.setTargetMixPct(p.right * factor);
			return s;

		}).collect(Collectors.toList());

		txMixStats.forEach(t -> {
			runSummary.addTxSummary(t);
		});

		List<TxSummary> txPeriodicStats = txPeriodic
				.stream()
				.map(p -> p.getTx().getStats())
				.collect(Collectors.toList());

		txPeriodicStats.forEach(t -> {
			runSummary.addTxSummary(t);
		});

		return runSummary;
	}
}
