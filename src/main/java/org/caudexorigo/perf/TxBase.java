package org.caudexorigo.perf;

import java.util.Arrays;
import java.util.Optional;

import org.caudexorigo.db.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TxBase implements Tx
{
	private static Logger log = LoggerFactory.getLogger(TxBase.class);

	private final String txName;
	private final TxStatsCollector statCollector;

	public TxBase(TxStatsCollector statCollector)
	{
		super();
		this.statCollector = statCollector;

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
				log.error("\nTransaction with error status: \n{}", txOut.toString());
			}

			if (txOut.getStatus() > 0)
			{
				statCollector.incrementWarnings();
				log.warn("\nTransaction with warning status: \n{}", txOut.toString());
			}

			if (log.isDebugEnabled())
			{
				log.debug(txOut.toString());
			}

			return txOut;
		}
		catch (Throwable t)
		{
			statCollector.incrementErrors();

			Throwable r = ErrorAnalyser.findRootCause(t);

			writeLog(log, r);

			final long stop = System.nanoTime();
			TxError txError = new TxError(r.getMessage());
			txError.setTxTime(stop - start);

			statCollector.offerMaxTs(stop);
			statCollector.addValue(txError.getTxTime());
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

	protected LogCtx getLogCtx(Throwable r)
	{
		LogCtx logCtx = new LogCtx();

		Optional<StackTraceElement> se = Arrays
				.asList(r.getStackTrace())
				.stream()
				.filter(s -> s.getClassName().startsWith("org.caudexorigo"))
				.findFirst();

		if (se.isPresent())
		{
			logCtx.hasLocalInfo = true;
			logCtx.message = String.format("""
					%n{
						ex_message=%s,
						ex_info=%s
					}
					""", r.getMessage(), se.toString());
		}
		else
		{
			logCtx.hasLocalInfo = false;
			logCtx.message = String.format("""
					%n{
						ex_message=%s
					}
					""", r.getMessage());
		}
		return logCtx;
	}

	protected void writeLog(Logger l, Throwable r)
	{
		LogCtx logCtx = getLogCtx(r);

		if (logCtx.hasLocalInfo)
		{
			l.error(logCtx.message);
		}
		else
		{
			l.error(r.getMessage(), r);
		}
	}
}