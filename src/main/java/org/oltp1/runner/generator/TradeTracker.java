package org.oltp1.runner.generator;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

public class TradeTracker
{
	public static final long TRADE_SHIFT = 200_000_000_000_000L;

	private final long initialMaxTradeId;
	private final AtomicLong currentMaxTradeId;
	private final LocalDateTime initialEndDts;
	private final long startNanoTime;

	public TradeTracker(long initialMaxTradeId, LocalDateTime initialEndDts)
	{
		this.initialMaxTradeId = (initialMaxTradeId >= TRADE_SHIFT) ? (initialMaxTradeId - TRADE_SHIFT) : initialMaxTradeId;
		this.currentMaxTradeId = new AtomicLong(this.initialMaxTradeId);

		// Start 1 second after the last initial trade
		this.initialEndDts = initialEndDts.plusSeconds(1);
		this.startNanoTime = System.nanoTime();
	}

	public synchronized void recordNewTradeId(long tradeId)
	{
		long unshifted = (tradeId >= TRADE_SHIFT) ? (tradeId - TRADE_SHIFT) : tradeId;
		currentMaxTradeId.accumulateAndGet(unshifted, Math::max);
	}

	public long getMaxActiveTradeId()
	{
		return currentMaxTradeId.get();
	}

	public long getInitialMaxTradeId()
	{
		return initialMaxTradeId;
	}

	/**
	 * Returns the simulated timestamp corresponding to the current point in the
	 * benchmark run. 1 wall-clock second elapsed = 1 simulated second advanced.
	 */
	public LocalDateTime now()
	{
		long elapsedNanos = System.nanoTime() - startNanoTime;
		return initialEndDts.plusNanos(elapsedNanos);
	}

	public LocalDateTime getInitialEndDts()
	{
		return initialEndDts;
	}
}