package org.oltp1.common;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helpers for unwrapping a {@code Throwable} down to the exception that actually failed.
 */
public final class ErrorAnalyser
{
	private static final Logger log = LoggerFactory.getLogger(ErrorAnalyser.class);

	/**
	 * Upper bound on how far {@link #findRootCause(Throwable)} will walk.
	 * <p>
	 * {@code Throwable.initCause} refuses self-causation, but a multi-element cycle
	 * ({@code a.setCause(b); b.setCause(a);}) is legal, so the walk needs a bound rather than just
	 * a {@code getCause() != null} test. Exceeding it is pathological and reported, never silently
	 * truncated.
	 */
	public static final int MAX_CAUSE_DEPTH = 64;

	private ErrorAnalyser()
	{
		// utility class
	}

	/**
	 * Returns the deepest cause of {@code ex}, or {@code ex} itself when it has none.
	 *
	 * <p>Two properties this deliberately guarantees, because callers depend on them:
	 * <ul>
	 * <li><b>No synthetic wrapper.</b> A throwable with no cause is returned as-is, so the reported
	 * type and message are the real ones. Wrapping first would report every cause-less failure
	 * (a JDBC {@code SQLException}, say) as {@code java.lang.Exception}, which is exactly the
	 * information the caller is trying to recover.</li>
	 * <li><b>Termination.</b> The walk is bounded by {@link #MAX_CAUSE_DEPTH} hops and skips
	 * identity repeats, so a cyclic cause chain returns instead of hanging the driver.</li>
	 * </ul>
	 *
	 * <p>When the cause chain is exhausted, the first <em>suppressed</em> exception is considered
	 * next. That is where try-with-resources and multi-failure paths put the interesting error: a
	 * body failure has the close failure suppressed, and a JDBC driver frequently attaches the
	 * real {@code SQLException} as suppressed to a generic wrapper.
	 *
	 * @param ex the throwable to unwrap; must not be {@code null}
	 * @return the deepest non-repeating throwable reachable through {@code getCause()} and then
	 * {@code getSuppressed()}
	 * @throws NullPointerException if {@code ex} is {@code null}
	 */
	public static Throwable findRootCause(Throwable ex)
	{
		Objects.requireNonNull(ex, "ex");

		Throwable current = ex;
		Set<Throwable> seen = null; // allocated lazily: the overwhelmingly common case is depth 0-2

		for (int depth = 0; depth < MAX_CAUSE_DEPTH; depth++)
		{
			Throwable next = nextThrowable(current);

			if (next == null)
			{
				return current;
			}

			if (next == current)
			{
				// Unreachable for a self-cause (initCause forbids it) but cheap to rule out.
				return current;
			}

			if (seen == null)
			{
				seen = newIdentitySet();
				seen.add(current);
			}
			else if (!seen.add(next))
			{
				// Cyclic chain. 'next' is where the walk would go round again, so stopping here
				// reports the last throwable before the cycle closed.
				log.debug("Cause chain of {} contains a cycle; reporting {}", current.getClass().getName(), next);
				return next;
			}

			current = next;
		}

		return current;
	}

	/**
	 * Logs a fatal failure in full: the whole cause chain (so the operation that failed is
	 * visible) with the root cause called out separately (so the actual reason is not buried).
	 *
	 * <p>Prefer this to {@code findRootCause(t).printStackTrace()} at a {@code catch} block. The
	 * root cause's own stack trace is the deepest part of the chain, so printing only that loses
	 * the frames that say <em>which</em> operation was running.
	 */
	public static void logFatal(Throwable t, Logger logger)
	{
		Objects.requireNonNull(t, "t");
		Objects.requireNonNull(logger, "logger");

		Throwable root = findRootCause(t);

		logger.error("Root cause: {}: {}", root.getClass().getName(), root.getMessage());
		logger.error("Full cause chain:", t);

		if (root != t)
		{
			logger.error("Root cause stack trace:", root);
		}
	}

	/** {@link #logFatal(Throwable, Logger)} for the common case of a class-backed logger. */
	public static void logFatal(Throwable t)
	{
		logFatal(t, log);
	}

	/**
	 * Renders a throwable and its whole cause chain to a string, root cause last. Used where a
	 * caller wants the trace on stdout rather than through SLF4J.
	 */
	public static String describe(Throwable t)
	{
		Objects.requireNonNull(t, "t");

		StringWriter sw = new StringWriter();
		Throwable root = findRootCause(t);

		try (PrintWriter pw = new PrintWriter(sw))
		{
			pw.println("Root cause: " + root);
			t.printStackTrace(pw);
		}

		return sw.toString();
	}

	// ------------------------------------------------------------------ //
	// Internals
	// ------------------------------------------------------------------ //

	/**
	 * The next throwable to unwrap: the cause if there is one, otherwise the first non-null
	 * suppressed exception. Returns {@code null} when there is nothing left to unwrap.
	 */
	private static Throwable nextThrowable(Throwable t)
	{
		Throwable cause = t.getCause();

		if (cause != null)
		{
			return cause;
		}

		for (Throwable suppressed : t.getSuppressed())
		{
			if (suppressed != null)
			{
				return suppressed;
			}
		}

		return null;
	}

	private static Set<Throwable> newIdentitySet()
	{
		// A cause chain can legitimately reuse an equal-but-distinct Throwable, and
		// Throwable does not override equals(), so identity is the only correct comparison.
		return java.util.Collections.newSetFromMap(new IdentityHashMap<>());
	}
}
