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
		message = rootCause.getMessage();

		StackTraceElement[] stack = rootCause.getStackTrace();

		Optional<StackTraceElement> se = Arrays
				.asList(stack)
				.stream()
				.filter(s -> s.getClassName().startsWith("org.oltp1"))
				.findFirst();

		if (se.isPresent())
		{
			source = se.toString();
		}
		else
		{
			source = stack[0].toString();
		}
	}

	@Override
	public String toString()
	{
		return String.format("[message=%s, source=%s]", message, source);
	}
}
