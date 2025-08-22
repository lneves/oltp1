package org.oltp1.egen;

import java.io.PrintStream;

public final class Spinner implements AutoCloseable
{
	private final char[] frames = { '|', '/', '-', '\\' };
	private final PrintStream out;
	private final String label;

	private int i = 0;
	private boolean active = true;
	private final boolean tty = System.console() != null;
	private long tickInterval;
	private long counter = 0;

	public Spinner(String label, long tickInterval)
	{
		this(label, tickInterval, System.out);
	}

	public Spinner(String label, long tickInterval, PrintStream out)
	{
		this.label = label == null ? "" : label;
		this.tickInterval = tickInterval;
		this.out = out;
		render(); // initial frame
	}

	public void step()
	{
		if (++counter % tickInterval == 0)
			tick();

	}

	private void tick()
	{
		if (!active)
			return;
		i = (i + 1) % frames.length;
		render();
	}

	private void render()
	{
		if (tty)
		{
			out.print("\r" + label + " " + frames[i]);
			out.flush();
		}
		else
		{
			// When output is redirected, avoid carriage returns
			out.print(".");
		}
	}

	/** Print a newline and mark done. */
	public void finish()
	{
		if (!active)
			return;
		active = false;
		if (tty)
			out.println("\r" + label + " done ");
		else
			out.println();
	}

	@Override
	public void close()
	{
		finish();
	}
}
