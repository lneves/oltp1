package org.oltp1.initdb;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Splits a shipped DDL script into executable statements.
 *
 * <h2>Why this is not {@code content.split(";")}</h2>
 * A naive split on {@code ';'} followed by a "skip chunks that start with a comment" test is
 * wrong in two ways, and both were live bugs:
 * <ul>
 * <li>A {@code ;} <em>inside a comment</em> ends the chunk early, so the remainder of the comment
 * is prepended to the statement that follows and that statement is executed as invalid SQL. In
 * {@code ddl-scripts/mariadb/3_create_index_fk.sql} the comment
 * {@code -- Foreign keys (columns only; the indexes above are reused where possible).} does
 * exactly this to {@code ALTER TABLE broker ADD CONSTRAINT fk_broker_st ...}, so MariaDB
 * {@code initdb} failed with a syntax error <em>after</em> the database had been dropped and 32
 * tables loaded.</li>
 * <li>A statement preceded by a comment whose last line has no {@code ;} is <em>silently
 * dropped</em>, because the whole chunk starts with the comment marker.</li>
 * </ul>
 * A {@code ;} inside a string literal, a PostgreSQL dollar-quoted body, or the word {@code GO}
 * inside a comment or a bracketed identifier have the same failure mode waiting to happen.
 *
 * <h2>What this does instead</h2>
 * One left-to-right scan that understands the lexical structure of the four dialects the project
 * ships scripts for:
 * <ul>
 * <li>{@code --} line comments and {@code /* ... *}{@code /} block comments are consumed and
 * replaced by a single space, so tokens either side of a comment cannot be glued together.</li>
 * <li>{@code '...'} (with {@code ''} doubling), {@code "..."} and {@code `...`} runs are copied
 * verbatim, so a {@code ;} or a {@code --} inside them is not special.</li>
 * <li>PostgreSQL dollar quoting ({@code $$...$$} and {@code $tag$...$tag$}) is copied verbatim.</li>
 * <li>For SQL Server, {@code [bracketed identifiers]} are copied verbatim, and only a {@code GO}
 * alone on its own line (case-insensitive, optional surrounding whitespace) ends a
 * statement.</li>
 * </ul>
 * Comments are dropped rather than executed, and a statement is emitted only if something other
 * than whitespace and comments remains in it, so nothing can be lost silently: every dropped
 * chunk is logged at DEBUG and the parse is logged per statement.
 *
 * <p>Not handled, deliberately: MySQL backslash escapes inside string literals
 * ({@code 'a\'b'}). No shipped script contains one, and treating {@code \} as an escape would
 * mis-read a string that legitimately ends in a backslash, such as
 * {@code LINES TERMINATED BY '\\n'}. The scripts are hand-maintained; a new one that needs it
 * should say so here rather than have this class guess.
 *
 * <p>This class has no dependency on JDBC, so it is directly unit-testable against the shipped
 * classpath resources without a database.
 */
public final class SqlScriptParser
{
	private static final Logger log = LoggerFactory.getLogger(SqlScriptParser.class);

	private final boolean batchSeparators;

	/**
	 * @param batchSeparators {@code true} for SQL Server, where batches are terminated by
	 * {@code GO} on its own line; {@code false} for the dialects that terminate statements with
	 * {@code ;}
	 */
	public SqlScriptParser(boolean batchSeparators)
	{
		this.batchSeparators = batchSeparators;
	}

	/** Convenience factory: SQL Server is the only engine that uses {@code GO} batches. */
	public static SqlScriptParser forEngine(String engineName)
	{
		return new SqlScriptParser("MSSQL".equalsIgnoreCase(engineName));
	}

	/**
	 * Parses a script into the statements to execute, in order.
	 *
	 * @param content the whole script
	 * @return the non-blank statements, with comments removed
	 * @throws IllegalStateException if the script has content but yields no statement, which
	 * means the script was written in a dialect this parser does not understand
	 */
	public List<String> parse(String content)
	{
		Objects.requireNonNull(content, "content");

		StatementCollector sink = new StatementCollector();
		walk(content, batchSeparators, false, sink);

		List<String> statements = sink.statements();

		for (String statement : statements)
		{
			log.debug("Parsed statement: {}", StringUtils.abbreviate(statement.replaceAll("\\R", " "), 200));
		}

		if (statements.isEmpty() && sink.sawContent() > 0)
		{
			throw new IllegalStateException(
					"The script produced no executable statement although it contains "
							+ sink.sawContent() + " non-whitespace, non-terminator character(s). "
							+ "It is probably written for a dialect this parser does not understand "
							+ "(batch separators=" + batchSeparators + "). First 500 characters: "
							+ StringUtils.abbreviate(content, 500));
		}

		return statements;
	}

	/**
	 * The script with every comment removed and no other change, for diagnosing a parse. The
	 * quoted runs and dollar-quoted bodies are preserved exactly.
	 */
	public static String stripComments(String content)
	{
		Objects.requireNonNull(content, "content");

		StringBuilder out = new StringBuilder(content.length());
		walk(content, false, true, out::append);

		return out.toString();
	}

	// ------------------------------------------------------------------ //
	// The scanner
	// ------------------------------------------------------------------ //

	/**
	 * The single left-to-right pass shared by {@link #parse} and {@link #stripComments}.
	 * {@code sink} receives every character that is not part of a comment, and
	 * {@link Sink#endStatement()} marks a statement boundary.
	 *
	 * @param keepTerminators whether a terminator is itself passed on to the sink; only
	 * {@link #stripComments} wants that, because for it a terminator is real script content
	 */
	private static void walk(String content, boolean batchSeparators, boolean keepTerminators, Sink sink)
	{
		final int length = content.length();
		int i = 0;

		while (i < length)
		{
			final char c = content.charAt(i);

			// --- comments: consumed, replaced by a single space so tokens cannot be glued ---

			if (c == '-' && i + 1 < length && content.charAt(i + 1) == '-')
			{
				i = endOfLine(content, i);
				sink.accept(' ');
				continue;
			}

			if (c == '/' && i + 1 < length && content.charAt(i + 1) == '*')
			{
				int end = content.indexOf("*/", i + 2);
				i = (end < 0) ? length : end + 2;
				sink.accept(' ');
				continue;
			}

			// --- statement boundaries, only ever recognised outside a quoted run ---

			if (!batchSeparators && c == ';')
			{
				i++;
				if (keepTerminators)
				{
					sink.accept(';');
				}
				sink.endStatement();
				continue;
			}

			if (batchSeparators)
			{
				int batchEnd = batchSeparatorLength(content, i);

				if (batchEnd > 0)
				{
					if (keepTerminators)
					{
						sink.accept(content, i, i + batchEnd);
					}
					i += batchEnd;
					sink.endStatement();
					continue;
				}

				if (c == '[')
				{
					i = copyBracketedIdentifier(content, i, sink);
					continue;
				}
			}

			// --- quoted runs, copied verbatim ---

			if (c == '\'' || c == '"' || c == '`')
			{
				i = copyQuoted(content, i, c, sink);
				continue;
			}

			if (c == '$' && dollarQuoteTagLength(content, i) > 0)
			{
				i = copyDollarQuoted(content, i, sink);
				continue;
			}

			sink.accept(c);
			i++;
		}

		sink.endStatement();
	}

	/**
	 * Copies a {@code '...'}, {@code "..."} or {@code `...`} run verbatim, handling the doubled
	 * quote escape. Returns the index just past the closing quote.
	 */
	private static int copyQuoted(String content, int start, char quote, Sink sink)
	{
		final int length = content.length();

		sink.accept(quote);
		int i = start + 1;

		while (i < length)
		{
			char c = content.charAt(i);
			sink.accept(c);
			i++;

			if (c != quote)
			{
				continue;
			}

			if (i < length && content.charAt(i) == quote)
			{
				// Doubled quote: an escaped quote, not the end of the run.
				sink.accept(quote);
				i++;
				continue;
			}

			return i;
		}

		// Unterminated run: the script is malformed, but consuming the rest is safer than
		// silently reinterpreting it as code.
		return i;
	}

	/** Copies a SQL Server {@code [...]} identifier verbatim, doubling {@code ]} to escape it. */
	private static int copyBracketedIdentifier(String content, int start, Sink sink)
	{
		final int length = content.length();

		sink.accept('[');
		int i = start + 1;

		while (i < length)
		{
			char c = content.charAt(i);
			sink.accept(c);
			i++;

			if (c != ']')
			{
				continue;
			}

			if (i < length && content.charAt(i) == ']')
			{
				sink.accept(']');
				i++;
				continue;
			}

			return i;
		}

		return i;
	}

	/**
	 * The length of the {@code $tag$} opener at {@code start}, or {@code -1} if there is no
	 * dollar quote there. A bare {@code $} is not a dollar quote, so ordinary SQL containing
	 * {@code $} is unaffected.
	 */
	private static int dollarQuoteTagLength(String content, int start)
	{
		final int length = content.length();
		int i = start + 1;

		while (i < length)
		{
			char c = content.charAt(i);

			if (c == '$')
			{
				return (i == start + 1) ? 2 : (i - start + 1);
			}

			if (!Character.isLetterOrDigit(c) && c != '_')
			{
				return -1;
			}

			i++;
		}

		return -1;
	}

	/** Copies a PostgreSQL dollar-quoted body verbatim. */
	private static int copyDollarQuoted(String content, int start, Sink sink)
	{
		final int tag = dollarQuoteTagLength(content, start);
		final String delimiter = content.substring(start, start + tag);
		final int end = content.indexOf(delimiter, start + tag);

		if (end < 0)
		{
			// Unterminated: copy the remainder rather than reinterpreting the body as code.
			sink.accept(content, start, content.length());
			return content.length();
		}

		sink.accept(content, start, end + tag);
		return end + tag;
	}

	/**
	 * The length of a SQL Server {@code GO} batch separator at {@code start}, or {@code -1} if
	 * the characters there are not a separator. A separator is {@code GO} alone on its line, so
	 * the word {@code go} inside a statement, a comment or a quoted run is not one. Callers only
	 * reach here for unquoted, uncommented text.
	 */
	private static int batchSeparatorLength(String content, int start)
	{
		final int length = content.length();

		if (start + 2 > length)
		{
			return -1;
		}

		char g = content.charAt(start);
		char o = content.charAt(start + 1);

		if ((g != 'G' && g != 'g') || (o != 'O' && o != 'o'))
		{
			return -1;
		}

		int i = start + 2;

		while (i < length && isHorizontalSpace(content.charAt(i)))
		{
			i++;
		}

		if (i < length && !isLineBreak(content.charAt(i)))
		{
			return -1;
		}

		int before = start - 1;

		while (before >= 0 && isHorizontalSpace(content.charAt(before)))
		{
			before--;
		}

		if (before >= 0 && !isLineBreak(content.charAt(before)))
		{
			return -1;
		}

		return i - start;
	}

	/** The index of the line break that ends the line containing {@code start}. */
	private static int endOfLine(String content, int start)
	{
		final int length = content.length();
		int i = start;

		while (i < length && !isLineBreak(content.charAt(i)))
		{
			i++;
		}

		return i;
	}

	private static boolean isLineBreak(char c)
	{
		return c == '\n' || c == '\r';
	}

	private static boolean isHorizontalSpace(char c)
	{
		return c == ' ' || c == '\t';
	}

	// ------------------------------------------------------------------ //
	// Sinks
	// ------------------------------------------------------------------ //

	/** Receives the characters of a script and, optionally, its statement boundaries. */
	private interface Sink
	{
		void accept(char c);

		default void accept(CharSequence text, int start, int end)
		{
			for (int i = start; i < end; i++)
			{
				accept(text.charAt(i));
			}
		}

		default void endStatement()
		{
		}
	}

	/** Accumulates characters into statements, discarding blank ones. */
	private static final class StatementCollector implements Sink
	{
		private final List<String> statements = new ArrayList<>();
		private final StringBuilder current = new StringBuilder();
		private int content;

		@Override
		public void accept(char c)
		{
			current.append(c);

			if (!Character.isWhitespace(c))
			{
				content++;
			}
		}

		@Override
		public void endStatement()
		{
			String statement = current.toString().trim();

			if (!statement.isEmpty())
			{
				statements.add(statement);
			}

			current.setLength(0);
		}

		List<String> statements()
		{
			return List.copyOf(statements);
		}

		/** Non-whitespace characters that were not statement terminators. */
		int sawContent()
		{
			return content;
		}
	}
}
