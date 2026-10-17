package org.oltp1.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;

/**
 * Tests for the cause-chain unwrapping.
 * <p>
 * The defect these guard against: the previous implementation wrapped the argument in a fresh
 * {@code Exception} before walking, so a throwable with <em>no</em> cause came back as that
 * synthetic wrapper rather than as itself. Every cause-less failure &mdash; a JDBC
 * {@code SQLException}, which is the common case &mdash; was therefore reported as
 * {@code java.lang.Exception} with a spurious {@code Caused by} frame, and the three fatal
 * handlers printed that wrapper's trace instead of the real one. The walk was also unbounded, so a
 * cyclic cause chain hung the driver.
 */
class ErrorAnalyserTest
{
	@Test
	void causeLessThrowableIsReturnedUnchanged()
	{
		// The core regression: no synthetic wrapper.
		SQLException original = new SQLException("FATAL: database \"tpce\" does not exist");

		Throwable root = ErrorAnalyser.findRootCause(original);

		assertSame(original, root, "a cause-less throwable must be returned as-is");
		assertEquals("FATAL: database \"tpce\" does not exist", root.getMessage());
		assertEquals(SQLException.class, root.getClass());
	}

	@Test
	void causeLessThrowableWithNoMessageKeepsItsType()
	{
		SQLException original = new SQLException();

		Throwable root = ErrorAnalyser.findRootCause(original);

		assertSame(original, root);
		assertNull(original.getMessage(), "precondition: JDBC exceptions often carry no message");
		assertEquals(SQLException.class, root.getClass(), "the type must not be lost");
	}

	@Test
	void unwrapsASingleLevel()
	{
		SQLException cause = new SQLException("relation \"trade\" does not exist");

		Throwable root = ErrorAnalyser.findRootCause(new RuntimeException(cause));

		assertSame(cause, root);
	}

	@Test
	void unwrapsADeepChain()
	{
		IllegalStateException deepest = new IllegalStateException("boom");

		Throwable root = ErrorAnalyser.findRootCause(
				new RuntimeException(new IllegalStateException(new IOException(deepest))));

		assertSame(deepest, root);
	}

	@Test
	void survivesTheDoubleWrappingInitdbAndTheDriverDo()
	{
		// DbInitializer / MariaDbBulkLoader wrap in RuntimeException more than once; the root
		// cause must still be the actual failure, not the outermost wrapper.
		SQLException real = new SQLException("syntax error at or near \"the indexes above\"");

		Throwable root = ErrorAnalyser.findRootCause(new RuntimeException(new RuntimeException(real)));

		assertSame(real, root);
	}

	@Test
	void aSelfCausingThrowableTerminates()
	{
		// initCause forbids self-causation, so this needs a subclass, but the walk must not
		// rely on that: MAX_CAUSE_DEPTH has to be enough on its own.
		Throwable selfCausing = new RuntimeException("self")
		{
			@Override
			public synchronized Throwable getCause()
			{
				return this;
			}
		};

		Throwable root = ErrorAnalyser.findRootCause(selfCausing);

		assertSame(selfCausing, root, "must terminate and report something");
	}

	@Test
	void aCyclicCauseChainTerminates()
	{
		// Legal in Java: neither initCause call is a self-causation.
		RuntimeException a = new RuntimeException("a");
		RuntimeException b = new RuntimeException("b");
		a.initCause(b);
		b.initCause(a);

		Throwable root = ErrorAnalyser.findRootCause(a);

		assertNotNull(root);
		assertTrue(root == a || root == b, "must stop inside the cycle, not hang or wander");
	}

	@Test
	void aChainLongerThanTheLimitIsBoundedButStillReported()
	{
		// Longer than MAX_CAUSE_DEPTH links, so the walk must hit its bound. The point is that it
		// stops and reports something rather than hanging the driver; exceeding a 64-deep cause
		// chain is pathological, not something to be quiet about.
		RuntimeException outermost = new RuntimeException("outermost");
		IllegalStateException root = new IllegalStateException("root");
		Throwable cursor = outermost;

		for (int i = 0; i < ErrorAnalyser.MAX_CAUSE_DEPTH; i++)
		{
			RuntimeException next = new RuntimeException("level-" + i);
			cursor.initCause(next);
			cursor = next;
		}
		cursor.initCause(root);

		Throwable reported = ErrorAnalyser.findRootCause(outermost);

		assertNotNull(reported);
		assertNotSame(root, reported, "the bound was reached before the true root");
		assertTrue(reported.getMessage().startsWith("level-"), "expected a mid-chain level, got: " + reported);
	}

	@Test
	void aChainOfExactlyTheLimitIsFullyUnwrapped()
	{
		// MAX_CAUSE_DEPTH links from outermost to root: the walk must reach the real root.
		RuntimeException outermost = new RuntimeException("outermost");
		IllegalStateException root = new IllegalStateException("root");
		Throwable cursor = outermost;

		for (int i = 0; i < ErrorAnalyser.MAX_CAUSE_DEPTH - 1; i++)
		{
			RuntimeException next = new RuntimeException("level-" + i);
			cursor.initCause(next);
			cursor = next;
		}
		cursor.initCause(root);

		assertSame(root, ErrorAnalyser.findRootCause(outermost));
	}

	@Test
	void fallsThroughToSuppressedWhenTheCauseChainEnds()
	{
		// try-with-resources and JDBC drivers put the interesting failure in getSuppressed().
		SQLException suppressed = new SQLException("connection already closed");
		RuntimeException primary = new RuntimeException("could not finish transaction");
		primary.addSuppressed(suppressed);

		assertSame(suppressed, ErrorAnalyser.findRootCause(primary));
	}

	@Test
	void prefersTheCauseChainOverSuppressed()
	{
		// If there is a cause, that is the more specific explanation and must win.
		SQLException suppressed = new SQLException("close failed");
		IOException cause = new IOException("disk went away");
		RuntimeException primary = new RuntimeException("could not finish transaction", cause);
		primary.addSuppressed(suppressed);

		assertSame(cause, ErrorAnalyser.findRootCause(primary));
	}

	@Test
	void unwrapsIntoANestedSuppressedChain()
	{
		IllegalStateException deepest = new IllegalStateException("inner suppressed");
		SQLException middle = new SQLException("outer suppressed");
		middle.addSuppressed(deepest);

		RuntimeException primary = new RuntimeException("primary");
		primary.addSuppressed(middle);

		assertSame(deepest, ErrorAnalyser.findRootCause(primary));
	}

	@Test
	void rejectsNull()
	{
		assertThrows(NullPointerException.class, () -> ErrorAnalyser.findRootCause(null));
	}

	@Test
	void describeNamesTheRootCauseAndPrintsTheWholeChain()
	{
		SQLException cause = new SQLException("permission denied for database tpce");
		RuntimeException wrapper = new RuntimeException("initdb failed", cause);

		String described = ErrorAnalyser.describe(wrapper);

		assertTrue(described.contains("Root cause: " + cause),
				"must name the root cause:\n" + described);
		assertTrue(described.contains("java.sql.SQLException: permission denied for database tpce"),
				"must include the full chain:\n" + described);
		assertTrue(described.contains("Caused by:"), "must show the intermediate wrappers:\n" + described);
	}

	@Test
	void describeHandlesACauseLessThrowable()
	{
		SQLException cause = new SQLException("no such table: customer");

		String described = ErrorAnalyser.describe(cause);

		assertTrue(described.contains("Root cause: " + cause), described);
		assertTrue(!described.contains("Caused by"), "a cause-less throwable has no chain:\n" + described);
	}

	@Test
	void logFatalReportsBothTheChainAndTheRootCause()
	{
		SQLException cause = new SQLException("could not connect to server");
		RuntimeException wrapper = new RuntimeException("driver startup failed", cause);

		try (var logs = captureLogs())
		{
			ErrorAnalyser.logFatal(wrapper);

			String logged = flatten(logs.appender());
			assertTrue(logged.contains("Root cause: java.sql.SQLException"),
					"the root cause type must be called out:\n" + logged);
			assertTrue(logged.contains("could not connect to server"),
					"the root cause message must be called out:\n" + logged);
			assertTrue(logged.contains("driver startup failed"),
					"the intermediate wrapper must still be visible:\n" + logged);
		}
	}

	@Test
	void logFatalHandsTheWholeChainToTheLogFramework()
	{
		// "Caused by:" is rendered by the appender's encoder from the throwable it is given, so
		// what matters here is that logFatal hands over the OUTERMOST throwable (which carries the
		// rest of the chain) rather than the root cause on its own.
		SQLException cause = new SQLException("could not connect to server");
		RuntimeException wrapper = new RuntimeException("driver startup failed", cause);

		try (var logs = captureLogs())
		{
			ErrorAnalyser.logFatal(wrapper);

			boolean fullChainLogged = logs.appender().list.stream()
					.map(ILoggingEvent::getThrowableProxy)
					.filter(java.util.Objects::nonNull)
					.map(IThrowableProxy::getClassName)
					.anyMatch(RuntimeException.class.getName()::equals);

			assertTrue(fullChainLogged,
					"the outermost throwable must be logged so the encoder prints the whole chain");
		}
	}

	@Test
	void logFatalHandlesACauseLessThrowable()
	{
		try (var logs = captureLogs())
		{
			ErrorAnalyser.logFatal(new IllegalArgumentException("bad flag"));

			String logged = flatten(logs.appender());
			assertTrue(logged.contains("Root cause: java.lang.IllegalArgumentException"), logged);
			assertTrue(logged.contains("bad flag"), logged);
		}
	}

	@Test
	void logFatalHandlesACyclicChain()
	{
		RuntimeException a = new RuntimeException("a");
		RuntimeException b = new RuntimeException("b");
		a.initCause(b);
		b.initCause(a);

		try (var logs = captureLogs())
		{
			ErrorAnalyser.logFatal(a);
			assertTrue(flatten(logs.appender()).contains("Root cause:"), "must log rather than hang");
		}
	}

	@Test
	void errorCtxReportsTheRootCauseNotAWrapper()
	{
		// ErrorCtx is the per-transaction error path; before the fix a cause-less failure
		// produced "[message=java.lang.NullPointerException: ..., source=...]".
		IllegalStateException cause = new IllegalStateException("LAST_TRADE row missing");
		RuntimeException wrapper = new RuntimeException(cause);

		assertEquals("LAST_TRADE row missing", new ErrorCtx(wrapper).message);
		assertEquals("LAST_TRADE row missing", new ErrorCtx(cause).message);
	}

	@Test
	void errorCtxFallsBackToTheTypeWhenThereIsNoMessage()
	{
		ErrorCtx ctx = new ErrorCtx(new SQLException());

		assertEquals("java.sql.SQLException", ctx.message, "must not degrade to 'message=null'");
	}

	@Test
	void errorCtxIsSafeOnTheCyclicChain()
	{
		// ErrorCtx is constructed for every failed transaction, so a cyclic chain must not
		// hang the benchmark.
		RuntimeException a = new RuntimeException("a");
		RuntimeException b = new RuntimeException("b");
		a.initCause(b);
		b.initCause(a);

		assertNotNull(new ErrorCtx(a));
	}

	// ------------------------------------------------------------------ //
	// Helpers
	// ------------------------------------------------------------------ //

	/**
	 * Attaches a capturing appender to the root logger for the duration of one test, so the
	 * assertions can read what {@link ErrorAnalyser#logFatal(Throwable)} actually emitted. Uses
	 * the project's own Logback rather than a hand-rolled {@code Logger} implementation, which
	 * would have to track every SLF4J method.
	 */
	/** A capturing appender plus the teardown that keeps it from leaking into other tests. */
	private record CapturedLogs(ListAppender<ILoggingEvent> appender) implements AutoCloseable
	{
		@Override
		public void close()
		{
			ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
					.getLogger(Logger.ROOT_LOGGER_NAME);

			root.detachAppender(appender);
			appender.stop();
		}
	}

	private static CapturedLogs captureLogs()
	{
		ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
				.getLogger(Logger.ROOT_LOGGER_NAME);

		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.setContext(root.getLoggerContext());
		appender.start();
		root.addAppender(appender);

		return new CapturedLogs(appender);
	}

	private static String flatten(ListAppender<ILoggingEvent> events)
	{
		java.io.StringWriter sw = new java.io.StringWriter();
		java.io.PrintWriter pw = new java.io.PrintWriter(sw);

		for (ILoggingEvent event : events.list)
		{
			pw.println(event.getFormattedMessage());

			IThrowableProxy proxy = event.getThrowableProxy();

			if (proxy != null)
			{
				// Render the proxy rather than the throwable: printStackTrace on the original
				// would re-derive the chain, and the point here is to see what was logged.
				pw.println(proxy.getClassName() + (proxy.getMessage() == null ? "" : ": " + proxy.getMessage()));
				pw.println("  " + String.join("\n  ", java.util.Arrays.stream(proxy.getStackTraceElementProxyArray())
						.map(StackTraceElementProxy::getSTEAsString).toList()));
			}
		}

		pw.flush();
		return sw.toString();
	}
}
