package org.oltp1.runner.runtime;

public interface TxStats
{
	TransactionSpec getTxSpec();

	long getCount();

	long getErrorCount();

	default long getAttemptedCount()
	{
		return getCount() + getErrorCount();
	}

	long getRollbackCount();

	long getWarningCount();

	double getMin();

	double getMax();

	double getMean();

	double getStdDev();

	double getPercentile(double percentile);

}