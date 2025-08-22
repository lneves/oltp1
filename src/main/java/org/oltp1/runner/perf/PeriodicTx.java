package org.oltp1.runner.perf;

import java.util.concurrent.TimeUnit;

public class PeriodicTx
{
	private final TxBase tx;
	private final long initialDelay;
	private final long delay;
	private final TimeUnit unit;

	public PeriodicTx(TxBase tx, long initialDelay, long delay, TimeUnit unit)
	{
		super();
		this.tx = tx;
		this.initialDelay = initialDelay;
		this.delay = delay;
		this.unit = unit;
	}

	public TxBase getTx()
	{
		return tx;
	}

	public long getInitialDelay()
	{
		return initialDelay;
	}

	public long getDelay()
	{
		return delay;
	}

	public TimeUnit getUnit()
	{
		return unit;
	}

	@Override
	public String toString()
	{
		return String.format("PeriodicTx [tx=%s, initialDelay=%s, delay=%s, unit=%s]", tx.getClass().getName(), initialDelay, delay, unit);
	}

}
