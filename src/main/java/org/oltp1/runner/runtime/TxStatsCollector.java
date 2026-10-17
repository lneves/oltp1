package org.oltp1.runner.runtime;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.tdunning.math.stats.TDigest;

// This class is not to be shared among Threads.
// There will be an instance for every Transaction type for each Client/Thread.
public class TxStatsCollector implements TxStats
{
	private static TDigest createHistogram()
	{
		return TDigest.createMergingDigest(100);
	}

	private long errorCounter;
	private TDigest histogram;
	private long rollbackCounter;
	private long warningCounter;
	private final SummaryStatistics stats = new SummaryStatistics();
	private final TransactionSpec txSpec;

	public TxStatsCollector(TransactionSpec txSpec)
	{
		this.txSpec = txSpec;
		this.histogram = createHistogram();
	}

	public final void addValue(double v)
	{
		if (Double.isFinite(v) && (v >= 0))
		{
			histogram.add(v);
			stats.addValue(v);
		}
	}

	@Override
	public long getCount()
	{
		return stats.getN();
	}

	@Override
	public final long getErrorCount()
	{
		return errorCounter;
	}

	@Override
	public double getMax()
	{
		return stats.getMax();
	}

	@Override
	public double getMin()
	{
		return stats.getMin();
	}

	@Override
	public double getMean()
	{
		return stats.getMean();
	}

	@Override
	public double getStdDev()
	{
		return stats.getStandardDeviation();
	}

	@Override
	public double getPercentile(double percentile)
	{
		if (percentile < 0.0 || percentile > 100.0 || Double.isNaN(percentile))
			throw new IllegalArgumentException(
					"Percentile must be between 0 and 100");

		return histogram.quantile(percentile / 100.0);
	}

	@Override
	public final long getRollbackCount()
	{
		return rollbackCounter;
	}

	@Override
	public final long getWarningCount()
	{
		return warningCounter;
	}

	@Override
	public TransactionSpec getTxSpec()
	{
		return txSpec;
	}

	public void incrementErrors()
	{
		errorCounter++;
	}

	public void incrementRollbackCount()
	{
		rollbackCounter++;
	}

	public void incrementWarnings()
	{
		warningCounter++;
	}

	public StatisticalSummary getSummaryStatistics()
	{
		return stats;
	}

	public TDigest getHistogram()
	{
		return histogram;
	}
}
