package org.oltp1.runner.perf;

import com.tdunning.math.stats.TDigest;

public class TxSummary
{
	private final String txName;
	private double targetMixPct;
	private final long count;
	private final long minTs;
	private final long maxTs;
	private final long warningCount;
	private final long erroCount;
	private final long rollbackCount;
	private final double min;
	private final double max;
	private final double mean;
	private final double sum;
	private final double stdDev;
	private final TDigest histo;
	private double elapsedTime;

	protected TxSummary(String txName, long count, long minTs, long maxTs, long warningCount, long erroCount, long rollbackCount, double min, double max, double mean, double sum, double stdDev, TDigest histo)
	{
		super();
		this.txName = txName;
		this.count = count;
		this.minTs = minTs;
		this.maxTs = maxTs;
		this.warningCount = warningCount;
		this.erroCount = erroCount;
		this.rollbackCount = rollbackCount;
		this.elapsedTime = ((double) (maxTs - minTs) / 1000000.0);
		this.min = min;
		this.max = max;
		this.mean = mean;
		this.sum = sum;
		this.stdDev = stdDev;
		this.histo = histo;
	}

	public String getTxName()
	{
		return txName;
	}

	public double getTargetMixPct()
	{
		return targetMixPct;
	}

	public double getTxElapsedTime()
	{
		return elapsedTime;
	}

	public double getActualMixPct(long totalTx)
	{
		return ((double) getCount()) / ((double) totalTx);
	}

	public long getCount()
	{
		return count;
	}

	public long getMinTs()
	{
		return minTs;
	}

	public long getMaxTs()
	{
		return maxTs;
	}

	public long getWarningCount()
	{
		return warningCount;
	}

	public long getRollbackCount()
	{
		return rollbackCount;
	}

	public long getErroCount()
	{
		return erroCount;
	}

	public double getMin()
	{
		return min;
	}

	public double getMax()
	{
		return max;
	}

	public double getMean()
	{
		return mean;
	}

	public double getSum()
	{
		return sum;
	}

	public double getQuantile(double q)
	{
		return histo.quantile(q);
	}

	public double getStdDev()
	{
		return stdDev;
	}

	public void setTargetMixPct(double d)
	{
		targetMixPct = d;
	}
}
