package org.oltp1.runner.runtime;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * Pacer to control aggregate foreground transaction rate across all clients.
 * Uses standard library primitives without drift or external rate-limiter
 * dependencies.
 */
public class Pacer
{
	private final boolean enabled;
	private final long intervalNanos;
	private final AtomicLong nextStartNano;

	public Pacer(boolean enabled, double targetTps)
	{
		this.enabled = enabled;
		if (enabled && targetTps > 0)
		{
			this.intervalNanos = (long) (1_000_000_000.0 / targetTps);
			this.nextStartNano = new AtomicLong(System.nanoTime());
		}
		else
		{
			this.intervalNanos = 0;
			this.nextStartNano = null;
		}
	}

	/**
	 * Blocks until the caller thread is permitted to execute the next foreground
	 * transaction. Pacing wait time is spent here and is NOT included in
	 * transaction latency measurements.
	 */
	public void acquire()
	{
		if (!enabled || intervalNanos <= 0)
		{
			return;
		}

		long scheduled = nextStartNano.getAndAdd(intervalNanos);
		long now = System.nanoTime();

		if (scheduled > now)
		{
			LockSupport.parkNanos(scheduled - now);
		}
		else if (now - scheduled > 1_000_000_000L)
		{
			// Reset schedule if the system has fallen behind by more than 1 second to avoid
			// massive bursts
			nextStartNano.updateAndGet(curr -> Math.max(curr, System.nanoTime() - intervalNanos));
		}
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public double getTargetTps()
	{
		return (intervalNanos > 0) ? (1_000_000_000.0 / intervalNanos) : 0.0;
	}
}
