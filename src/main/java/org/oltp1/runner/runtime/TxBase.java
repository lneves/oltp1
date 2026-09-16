package org.oltp1.runner.runtime;

import java.util.concurrent.TimeUnit;

import org.oltp1.common.ErrorCtx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TxBase implements Tx
{
	private static Logger log = LoggerFactory.getLogger(TxBase.class);

	private static volatile boolean quiet;

	private final BenchmarkMetrics metrics;
	private final TxStatsCollector statCollector;

	public static void setQuiet(boolean value)
	{
		quiet = value;
	}

	public TxBase(BenchmarkMetrics metrics, TxStatsCollector statCollector)
	{
		super();
		this.metrics = metrics;
		this.statCollector = statCollector;

		if (metrics != null && statCollector != null)
		{
			metrics.registerCollector(statCollector);
		}
	}

	public final String name()
	{
		return statCollector.getTxSpec().getDisplayName();
	}

	@Override
	public final TxOutput execute()
	{
		final long start = System.nanoTime();

		try
		{
			TxOutput txOut = run();

			final long stop = System.nanoTime();
			final long elapsedNanos = stop - start;
			final double txTime = elapsedNanos / 1_000_000.0;

			txOut.setTxTime(txTime);

			if (txOut.getStatus() < 0)
			{
				statCollector.incrementErrors();
				if (!quiet)
				{
					log.error("\nTransaction with error status: \n{}", txOut);
				}
			}
			else
			{
				statCollector.addValue(txTime);
				if (txOut.getStatus() > 0)
				{
					statCollector.incrementWarnings();
				}
			}

			updateMetrics(txOut);
			return txOut;
		}
		catch (Throwable t)
		{
			final long stop = System.nanoTime();
			final double elapsed = TimeUnit.NANOSECONDS.toMillis((stop - start));

			if (t instanceof InterruptedException || Thread.currentThread().isInterrupted())
			{
				Thread.currentThread().interrupt();
			}
			else
			{
				statCollector.incrementErrors();
			}

			ErrorCtx ectx = new ErrorCtx(t);

			TxError txError = new TxError(ectx.toString());
			txError.setStatus(-1);
			txError.setTxTime(elapsed);

			if (!quiet)
			{
				log.error(txError.toString());
			}

			updateMetrics(txError);
			return txError;
		}
	}

	protected void updateMetrics(TxOutput txOut)
	{
		if (metrics != null)
		{
			metrics.incrementTxCount();
			if (txOut.getStatus() < 0)
			{
				metrics.incrementErrorCount();
			}
		}
	}

	protected abstract TxOutput run();

	public TxStatsCollector getStats()
	{
		return statCollector;
	}

	public void incrementErrors()
	{
		statCollector.incrementErrors();
	}

	public void incrementWarnings()
	{
		statCollector.incrementWarnings();
	}

	public void incrementRollBacks()
	{
		statCollector.incrementRollbackCount();
	}
}