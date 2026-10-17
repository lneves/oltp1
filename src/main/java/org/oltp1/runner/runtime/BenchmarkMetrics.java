package org.oltp1.runner.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

// BenchmarkMetrics is shared between worker threads.
// Each TxStatsCollector is owned by exactly one worker thread and must not be accessed concurrently.
public class BenchmarkMetrics
{
	private enum State
	{
		NEW, RUNNING, ENDED
	}

	private final int numClients;
	private final List<TxStatsCollector> txStats;
	private long startNanoTime;
	private long endNanoTime;

	// These counters are for live progress reporting only.
	// Final benchmark statistics are derived from the per-thread collectors.
	private final LongAdder txCount = new LongAdder();
	private final LongAdder errorCount = new LongAdder();
	private final AtomicInteger pendingCount = new AtomicInteger(0);

	private State state = State.NEW;

	public BenchmarkMetrics(int numClients)
	{
		this.numClients = numClients;
		this.txStats = new ArrayList<>();
	}

	public synchronized void markStart()
	{
		if (state != State.NEW)
		{
			throw new IllegalStateException(String.format("BenchmarkMetrics can only be started once. Current state: %s", state));
		}

		startNanoTime = System.nanoTime();
		state = State.RUNNING;
	}

	public synchronized void markEnd()
	{
		if (state != State.RUNNING)
		{
			throw new IllegalStateException(String.format("BenchmarkMetrics can only be ended after it has started. Current state: %s", state));
		}

		endNanoTime = System.nanoTime();
		state = State.ENDED;
	}

	public synchronized double getElapsedSeconds()
	{
		if (state == State.NEW)
		{
			throw new IllegalStateException("Benchmark has not been started");
		}

		long end = (state == State.ENDED)
				? endNanoTime
				: System.nanoTime();

		return (end - startNanoTime) / 1_000_000_000.0;
	}

	public int getNumClients()
	{
		return numClients;
	}

	public void registerCollector(TxStatsCollector tStats)
	{
		if (tStats != null)
		{
			synchronized (txStats)
			{
				txStats.add(tStats);
			}
		}
	}

	public long getGlobalTxCount()
	{
		return txCount.longValue();
	}

	public long getGlobalErrorCount()
	{
		return errorCount.longValue();
	}

	public void incrementTxCount()
	{
		txCount.increment();
	}

	public void incrementErrorCount()
	{
		errorCount.increment();
	}

	public void incrementPendingCount()
	{
		pendingCount.incrementAndGet();
	}

	public void decrementPendingCount()
	{
		pendingCount.updateAndGet(current -> {
			if (current == 0)
				throw new IllegalStateException(
						"Cannot decrement pending transaction count below zero");

			return current - 1;
		});
	}

	public int getPendingCount()
	{
		return pendingCount.get();
	}

	public Map<String, TxStatsAggregate> aggregateTxStats()
	{
		List<TxStatsCollector> snapshot;

		synchronized (this)
		{
			if (state != State.ENDED)
			{
				throw new IllegalStateException("Benchmark must be ended before aggregating statistics");
			}
		}

		synchronized (txStats)
		{
			snapshot = new ArrayList<>(txStats);
		}

		Map<String, List<TxStatsCollector>> grouped = new LinkedHashMap<>();

		for (TxStatsCollector collector : snapshot)
		{
			grouped
					.computeIfAbsent(
							collector.getTxSpec().getDisplayName(),
							ignored -> new ArrayList<>())
					.add(collector);
		}

		Map<String, TxStatsAggregate> result = new LinkedHashMap<>();

		for (Map.Entry<String, List<TxStatsCollector>> entry : grouped.entrySet())
		{
			result
					.put(
							entry.getKey(),
							TxStatsAggregate
									.merge(
											entry.getKey(),
											entry.getValue()));
		}

		return Collections.unmodifiableMap(result);
	}

}