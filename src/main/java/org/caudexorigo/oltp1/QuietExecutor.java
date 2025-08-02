package org.caudexorigo.oltp1;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.function.Supplier;

public class QuietExecutor
{
	public static void runQuietly(Runnable block)
	{
		// Save the original stdout
		PrintStream origStdOut = System.out;
		try
		{
			// redirect the std streams to > /dev/null;
			System.setOut(new PrintStream(OutputStream.nullOutputStream()));

			block.run();
		}
		finally
		{
			// restore the stdout
			System.setOut(origStdOut);
		}
	}

	public static <T> T callQuietly(Supplier<T> block)
	{
		// Save the original stdout
		PrintStream origStdOut = System.out;
		try
		{
			// redirect the std streams to > /dev/null;
			System.setOut(new PrintStream(OutputStream.nullOutputStream()));

			return block.get();
		}
		finally
		{
			// restore the stdout
			System.setOut(origStdOut);
		}
	}
}