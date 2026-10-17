package org.oltp1.runner.runtime;

public final class ReportMetrics
{
	private ReportMetrics()
	{
	}

	public static long attemptedCount(TxStats stats)
	{
		return stats.getCount() + stats.getErrorCount();
	}

	public static double tpsE(TxStats tradeResultStats, double elapsedSeconds)
	{
		if (tradeResultStats == null || elapsedSeconds <= 0.0)
		{
			return 0.0;
		}

		return tradeResultStats.getCount() / elapsedSeconds;
	}

	public static double successRate(TxStats stats, double elapsedSeconds)
	{
		if (stats == null || elapsedSeconds <= 0.0)
		{
			return 0.0;
		}

		return stats.getCount() / elapsedSeconds;
	}

	public static double transactionRate(long count, double elapsedSeconds)
	{
		return elapsedSeconds > 0.0 ? count / elapsedSeconds : 0.0;
	}

	public static double actualMixPercent(long attempted, long totalAttempts)
	{
		return totalAttempts > 0 ? 100.0 * attempted / totalAttempts : 0.0;
	}
}
