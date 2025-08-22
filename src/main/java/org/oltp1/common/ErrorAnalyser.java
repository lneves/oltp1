package org.oltp1.common;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;

public class ErrorAnalyser
{
	public static Throwable findRootCause(Throwable ex)
	{
		Throwable error_ex = new Exception(ex);
		while (error_ex.getCause() != null)
		{
			error_ex = error_ex.getCause();
		}
		return error_ex;
	}

	public static void logCause(Throwable ex, Logger log)
	{
		Throwable r = findRootCause(ex);
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		r.printStackTrace(pw);
		String strace = sw.toString();

		log.error(String.format("%s%n%s", r.getMessage(), strace));
	}
}