package org.oltp1.runner.perf;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.tdunning.math.stats.TDigest;

public class TxStatsCollector
{
	private final AtomicLong errorCounter = new AtomicLong();
	private final AtomicLong warningCounter = new AtomicLong();
	private final AtomicLong rollbackCounter = new AtomicLong();
	private AtomicLong minTs = new AtomicLong(Long.MAX_VALUE);
	private AtomicLong maxTs = new AtomicLong(Long.MIN_VALUE);
	private final Object mutex = new Object();
	private final SummaryStatistics stats = new SummaryStatistics();
	private TDigest histogram;

	private final String txName;

	public TxStatsCollector(String txName)
	{
		super();
		this.txName = txName;
		this.histogram = TDigest.createDigest(100);
	}

	public TxStatsCollector(AtomicLong minTs, AtomicLong maxTs, TDigest histo, String txName, double mixPct)
	{
		super();
		this.minTs = minTs;
		this.maxTs = maxTs;
		this.histogram = histo;
		this.txName = txName;

	}

	public void offerMinTs(long ts)
	{
		minTs.accumulateAndGet(ts, (o, n) -> Math.min(o, n));
	}

	public void offerMaxTs(long ts)
	{
		maxTs.accumulateAndGet(ts, (o, n) -> Math.max(o, n));
	}

	public void incrementErrors()
	{
		errorCounter.incrementAndGet();
	}

	public void incrementWarnings()
	{
		warningCounter.incrementAndGet();
	}

	public void incrementRollBacks()
	{
		rollbackCounter.incrementAndGet();
	}

	public final long getErrorCount()
	{
		return errorCounter.get();
	}

	public final long getWarningCount()
	{
		return warningCounter.get();
	}

	public final long getRollBacksCount()
	{
		return rollbackCounter.get();
	}

	public final void addValue(double v)
	{
		synchronized (mutex)
		{
			stats.addValue(v);
			histogram.add(v);
		}
	}

	public final void clearStats()
	{
		synchronized (mutex)
		{
			minTs.set(Long.MAX_VALUE);
			maxTs.set(Long.MIN_VALUE);
			stats.clear();
			histogram = TDigest.createDigest(100);
			errorCounter.set(0l);
			warningCounter.set(0l);
			rollbackCounter.set(0l);
		}
	}

	public String getTxName()
	{
		return txName;
	}

	public final TxSummary getStats()
	{
		synchronized (mutex)
		{
			TxSummary txSummary = new TxSummary(txName, stats.getN(), minTs.get(), maxTs.get(), getWarningCount(), getErrorCount(), getRollBacksCount(), stats.getMin(), stats.getMax(), stats.getMean(), stats.getSum(), stats.getStandardDeviation(), histogram);
			return txSummary;
		}
	}
}
