package org.oltp1.runner.runtime;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.AggregateSummaryStatistics;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.tdunning.math.stats.TDigest;

public final class TxStatsAggregate implements TxStats
{
	private static final double HISTOGRAM_COMPRESSION = 100;

	private final String txName;
	private final StatisticalSummary stats;
	private final TDigest histogram;

	private final long errorCount;
	private final long rollbackCount;
	private final long warningCount;

	private TxStatsAggregate(
			String txName,
			StatisticalSummary stats,
			TDigest histogram,
			long errorCount,
			long rollbackCount,
			long warningCount)
	{
		this.txName = txName;
		this.stats = stats;
		this.histogram = histogram;
		this.errorCount = errorCount;
		this.rollbackCount = rollbackCount;
		this.warningCount = warningCount;
	}

	public static TxStatsAggregate merge(
			String txName,
			List<TxStatsCollector> collectors)
	{

		if (collectors == null || collectors.isEmpty())
			throw new IllegalArgumentException("collectors must not be empty");

		List<StatisticalSummary> summaries = new ArrayList<>(collectors.size());

		List<TDigest> histograms = new ArrayList<>(collectors.size());

		long errors = 0;
		long rollbacks = 0;
		long warnings = 0;

		for (TxStatsCollector collector : collectors)
		{
			if (collector == null)
			{
				continue;
			}

			if (!txName.equals(collector.getTxSpec().getDisplayName()))
			{
				throw new IllegalArgumentException(String.format("Collector belongs to different transaction: %s", collector.getTxSpec().getDisplayName()));
			}

			if (collector.getCount() > 0)
			{
				summaries.add(collector.getSummaryStatistics());
				histograms.add(collector.getHistogram());
			}

			errors += collector.getErrorCount();
			rollbacks += collector.getRollbackCount();
			warnings += collector.getWarningCount();
		}

		StatisticalSummary mergedStats;
		if (summaries.isEmpty())
		{
			mergedStats = new SummaryStatistics();
		}
		else
		{
			mergedStats = AggregateSummaryStatistics.aggregate(summaries);
		}

		TDigest mergedHistogram = TDigest.createMergingDigest(HISTOGRAM_COMPRESSION);

		if (!histograms.isEmpty())
		{
			mergedHistogram.add(histograms);
			mergedHistogram.compress();
		}

		return new TxStatsAggregate(
				txName,
				mergedStats,
				mergedHistogram,
				errors,
				rollbacks,
				warnings);
	}

	@Override
	public TransactionSpec getTxSpec()
	{
		return TransactionSpec.fromDisplayName(txName);
	}

	@Override
	public long getCount()
	{
		return stats.getN();
	}

	@Override
	public double getMin()
	{
		return stats.getMin();
	}

	@Override
	public double getMax()
	{
		return stats.getMax();
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
		if (percentile < 0.0
				|| percentile > 100.0
				|| Double.isNaN(percentile))
		{
			throw new IllegalArgumentException(
					"Percentile must be between 0 and 100");
		}

		return histogram.quantile(percentile / 100.0);
	}

	public long getErrorCount()
	{
		return errorCount;
	}

	public long getRollbackCount()
	{
		return rollbackCount;
	}

	public long getWarningCount()
	{
		return warningCount;
	}

	@Override
	public String toString()
	{
		return String.format("TxStatsAggregate [txName=%s, stats=%s, histogram=%s, errorCount=%s, rollbackCount=%s, warningCount=%s]", txName, stats, histogram, errorCount, rollbackCount, warningCount);
	}

}