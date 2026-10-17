package org.oltp1.common;

import java.util.Arrays;
import java.util.Optional;

// Error context
public class ErrorCtx
{
	public String message;
	public String source;

	public ErrorCtx(Throwable t)
	{
		Throwable rootCause = ErrorAnalyser.findRootCause(t);

		// Plenty of exceptions (SQLException in particular) carry a null message, which used
		// to produce "message=null" for every transaction error they caused. Fall back to the
		// type so the log line is still identifiable.
		message = (rootCause.getMessage() != null && !rootCause.getMessage().isBlank())
				? rootCause.getMessage()
				: rootCause.getClass().getName();

		StackTraceElement[] stack = rootCause.getStackTrace();

		Optional<StackTraceElement> se = Arrays
				.asList(stack)
				.stream()
				.filter(s -> s.getClassName().startsWith("org.oltp1"))
				.findFirst();

		if (se.isPresent())
		{
			source = se.get().toString();
		}
		else if (stack.length > 0)
		{
			source = stack[0].toString();
		}
		else
		{
			source = "Unknown (stack trace omitted by JVM)";
		}
	}

	@Override
	public String toString()
	{
		return String.format("[message=%s, source=%s]", message, source);
	}
}
