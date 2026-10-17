package org.oltp1.runner.runtime;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Terminal progress monitor displaying in-place progress to System.out updated
 * once per second.
 */
public class ProgressMonitor implements AutoCloseable
{
	private static final int BAR_WIDTH = 28;

	private final BenchmarkMetrics metrics;
	private final long durationSeconds;
	private final AtomicBoolean running = new AtomicBoolean(false);
	private final AtomicBoolean draining = new AtomicBoolean(false);
	private Thread monitorThread;

	public ProgressMonitor(BenchmarkMetrics metrics, long durationSeconds)
	{
		this.metrics = metrics;
		this.durationSeconds = durationSeconds;
	}

	public void start()
	{
		running.set(true);
		monitorThread = Thread.ofVirtual().name("progress-monitor").start(this::run);
	}

	public void setDraining(boolean isDraining)
	{
		draining.set(isDraining);
	}

	private void run()
	{
		long lastTime = System.nanoTime();
		long lastSuccess = metrics.getGlobalTxCount();

		while (running.get())
		{
			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e)
			{
				break;
			}

			long now = System.nanoTime();
			long currentSuccess = metrics.getGlobalTxCount();
			double elapsedSec = metrics.getElapsedSeconds();

			double intervalSec = (now - lastTime) / 1_000_000_000.0;
			double currentTps = (intervalSec > 0) ? (currentSuccess - lastSuccess) / intervalSec : 0.0;
			double avgTps = (elapsedSec > 0) ? currentSuccess / elapsedSec : 0.0;
			long errorCount = metrics.getGlobalErrorCount();

			lastTime = now;
			lastSuccess = currentSuccess;

			if (draining.get())
			{
				String line = String
						.format(
								Locale.ROOT,
								"\r[============================] 100%%  [DRAINING MEE: %d pending]  completed %d  avg %6.1f tx/s  errors %d   ",
								metrics.getPendingCount(),
								currentSuccess,
								avgTps,
								errorCount);
				System.err.print(line);
				System.err.flush();
			}
			else
			{
				int remainingSec = (int) Math.max(0, durationSeconds - elapsedSec);
				int pct = Math.min(100, Math.max(0, (int) ((elapsedSec * 100.0) / durationSeconds)));

				int filled = (pct * BAR_WIDTH) / 100;
				int unfilled = Math.max(0, BAR_WIDTH - filled);
				String bar = String
						.format(
								Locale.ROOT,
								"%s%s",
								"=".repeat(filled),
								"-".repeat(unfilled));
				String remainingStr = String.format(Locale.ROOT, "%02d:%02d", remainingSec / 60, remainingSec % 60);

				String line = String
						.format(
								Locale.ROOT,
								"\r[%s] %3d%%  remaining %s  completed %d  current %6.1f tx/s  avg %6.1f tx/s  errors %d",
								bar,
								pct,
								remainingStr,
								currentSuccess,
								currentTps,
								avgTps,
								errorCount);
				System.out.print(line);
				System.out.flush();
			}
		}
	}

	@Override
	public void close()
	{
		running.set(false);
		if (monitorThread != null)
		{
			monitorThread.interrupt();
			try
			{
				monitorThread.join(1000);
			}
			catch (InterruptedException ignored)
			{
			}
		}
		// Print a clean newline on System.err after the progress bar finishes
		System.out.println();
		System.out.flush();
	}
}
