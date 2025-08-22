package org.oltp1.runner.perf;

import org.oltp1.common.Assert;
import org.oltp1.common.ErrorCtx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TxBase implements Tx
{
	private static Logger log = LoggerFactory.getLogger(TxBase.class);

	private boolean isLoggingEnabled;

	private final String txName;
	private final TxStatsCollector statCollector;

	public TxBase(TxStatsCollector statCollector)
	{
		this(statCollector, true);
	}

	public TxBase(TxStatsCollector statCollector, boolean isLoggingEnabled)
	{
		super();
		this.statCollector = statCollector;
		this.isLoggingEnabled = isLoggingEnabled;

		Assert.notBlank("txName", statCollector.getTxName());

		this.txName = statCollector.getTxName();
	}

	public final String name()
	{
		return txName;
	}

	@Override
	public final TxOutput execute()
	{
		if (this instanceof TxVoid)
		{
			return run();
		}

		final long start = System.nanoTime();
		statCollector.offerMinTs(start);

		try
		{
			TxOutput txOut = run();

			final long stop = System.nanoTime();
			double txTime = (stop - start) / 1000000.0;
			txOut.setTxTime(txTime);

			statCollector.offerMaxTs(stop);
			statCollector.addValue(txOut.getTxTime());

			if (txOut.getStatus() < 0)
			{
				statCollector.incrementErrors();
				if (isLoggingEnabled)
				{
					log.error("\nTransaction with error status: \n{}", txOut.toString());
				}
			}

			if (txOut.getStatus() > 0)
			{
				statCollector.incrementWarnings();
				if (isLoggingEnabled)
				{
					log.warn("\nTransaction with warning status: \n{}", txOut.toString());
				}
			}

			return txOut;
		}
		catch (Throwable t)
		{
			final long stop = System.nanoTime();
			final double elapsed = stop - start;

			statCollector.incrementErrors();
			statCollector.offerMaxTs(stop);
			statCollector.addValue(elapsed);

			ErrorCtx ectx = new ErrorCtx(t);

			TxError txError = new TxError(ectx.toString());
			txError.setStatus(-1);
			txError.setTxTime(elapsed);

			if (isLoggingEnabled)
			{
				log.error(txError.toString());
			}

			return txError;
		}
	}

	protected abstract TxOutput run();

	public void clearStats()
	{
		statCollector.clearStats();
	}

	public TxSummary getStats()
	{
		return statCollector.getStats();
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
		statCollector.incrementRollBacks();
	}

	public void enableLogging()
	{
		isLoggingEnabled = true;
	}

	public void disableLogging()
	{
		isLoggingEnabled = false;
	}
}