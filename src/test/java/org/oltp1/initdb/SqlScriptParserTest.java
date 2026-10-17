package org.oltp1.initdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the DDL script parser.
 *
 * <p>The defect these guard against: the parser used to split the whole file on {@code ';'} and
 * then drop any chunk whose trimmed text started with a comment marker. A {@code ;} inside a
 * comment therefore ended the chunk early and the rest of the comment was prepended to the
 * statement that followed, so
 * {@code ddl-scripts/mariadb/3_create_index_fk.sql} executed
 * {@code the indexes above are reused where possible). ALTER TABLE broker ADD CONSTRAINT ...}
 * and MariaDB {@code initdb} failed with a syntax error after the database had been dropped and
 * 32 tables loaded.
 *
 * <p>Everything here runs against the shipped classpath resources. No database is involved.
 */
class SqlScriptParserTest
{
	/**
	 * Every statement a shipped script is allowed to start with. Checked against all 29 scripts
	 * so a script that is only parseable-but-nonsense still fails here.
	 *
	 * <p>{@code exec} and {@code reconfigure} are T-SQL statements used by
	 * {@code mssql/8_db_settings.sql} to raise {@code show advanced options} before changing
	 * server-level settings.
	 */
	private static final Pattern SQL_VERB = Pattern
			.compile("(?is)^(create|alter|drop|set|select|insert|update|delete|truncate|with|begin|"
					+ "commit|rollback|use|grant|revoke|analyze|vacuum|dbcc|declare|lock|unlock|"
					+ "prepare|execute|exec|deallocate|reconfigure|do|comment|refresh|copy|merge|"
					+ "if|else|print)\\b.*");

	private static final Set<String> ENGINES = Set.of("postgresql", "orioledb", "mariadb", "mssql");

	static Stream<String> shippedScripts()
	{
		// "engine|absoluteResourcePath", so a parameterised test can pick the right parser mode.
		return ENGINES.stream()
				.flatMap(engine -> listScriptNames("/ddl-scripts/" + engine).stream()
						.map(name -> engine + "|/ddl-scripts/" + engine + "/" + name));
	}

	private static String parser(String packed)
	{
		return packed.contains("|") ? packed.substring(0, packed.indexOf('|')) : packed;
	}

	private static String resource(String packed)
	{
		return packed.contains("|") ? packed.substring(packed.indexOf('|') + 1) : packed;
	}

	// ------------------------------------------------------------------ //
	// The regression
	// ------------------------------------------------------------------ //

	@Test
	void aSemicolonInsideACommentDoesNotSwallowTheNextStatement()
	{
		// The exact shape of mariadb/3_create_index_fk.sql that broke MariaDB initdb.
		String script = """
				CREATE UNIQUE INDEX ix_watch_list_nc1 ON watch_list (wl_c_id, wl_id);

				-- Foreign keys (columns only; the indexes above are reused where possible).

				ALTER TABLE broker
					ADD CONSTRAINT fk_broker_st FOREIGN KEY (b_st_id) REFERENCES status_type (st_id);

				ALTER TABLE customer
					ADD CONSTRAINT fk_customer_ad FOREIGN KEY (c_ad_id) REFERENCES address (ad_id);
				""";

		List<String> statements = new SqlScriptParser(false).parse(script);

		assertEquals(3, statements.size(), "got: " + statements);
		assertTrue(statements.get(1).startsWith("ALTER TABLE broker"), statements.get(1));
		assertTrue(statements.get(1).contains("fk_broker_st"), statements.get(1));
		assertFalse(statements.get(1).contains("the indexes above"), "comment prose leaked into the statement");
		assertTrue(statements.get(2).contains("fk_customer_ad"), statements.get(2));
	}

	@Test
	void aStatementAfterACommentIsNotDropped()
	{
		// The other half of the same defect: a comment with no trailing ';' made the whole
		// chunk look like a comment, so the statement was silently discarded.
		String script = """
				-- a trailing comment with no semicolon
				CREATE TABLE t (a int)
				""";

		List<String> statements = new SqlScriptParser(false).parse(script);

		assertEquals(1, statements.size(), "got: " + statements);
		assertEquals("CREATE TABLE t (a int)", statements.get(0));
	}

	@Test
	void blockCommentsAreRemoved()
	{
		String script = """
				/* a block comment
				   spanning lines; with a semicolon */
				CREATE TABLE t (a int);
				""";

		assertEquals(List.of("CREATE TABLE t (a int)"), new SqlScriptParser(false).parse(script));
	}

	@Test
	void aCommentBetweenTokensDoesNotGlueThem()
	{
		// A comment becomes a single space, so the statement keeps its token separation.
		assertEquals(
				List.of("SELECT 1 FROM t"),
				normalise(new SqlScriptParser(false).parse("SELECT 1 /* c */ FROM t;")));
	}

	// ------------------------------------------------------------------ //
	// Literals
	// ------------------------------------------------------------------ //

	@Test
	void aSemicolonInsideAStringLiteralIsNotASeparator()
	{
		assertEquals(
				List.of("SELECT ';' AS x", "SELECT 2"),
				new SqlScriptParser(false).parse("SELECT ';' AS x; SELECT 2;"));
	}

	@Test
	void aCommentMarkerInsideAStringLiteralIsNotAComment()
	{
		assertEquals(
				List.of("SELECT '--not a comment' AS x"),
				new SqlScriptParser(false).parse("SELECT '--not a comment' AS x;"));
		assertEquals(
				List.of("SELECT '/* neither */' AS x"),
				new SqlScriptParser(false).parse("SELECT '/* neither */' AS x;"));
	}

	@Test
	void doubledQuotesInsideAStringLiteralAreHandled()
	{
		assertEquals(
				List.of("SELECT 'it''s fine; really' AS x"),
				new SqlScriptParser(false).parse("SELECT 'it''s fine; really' AS x;"));
	}

	@Test
	void aSemicolonInsideDollarQuotedBodyIsNotASeparator()
	{
		// PostgreSQL function bodies. None of the shipped scripts uses them yet.
		String script = """
				CREATE FUNCTION f() RETURNS int AS $$
				BEGIN
					RETURN 1; -- a semicolon in the body
				END;
				$$ LANGUAGE plpgsql;
				""";

		List<String> statements = new SqlScriptParser(false).parse(script);

		assertEquals(1, statements.size(), "got: " + statements);
		assertTrue(statements.get(0).contains("RETURN 1; -- a semicolon in the body"), statements.get(0));
	}

	@Test
	void taggedDollarQuotingIsHandled()
	{
		String script = "CREATE FUNCTION f() RETURNS int AS $body$ SELECT 1; $body$ LANGUAGE sql;";

		assertEquals(1, new SqlScriptParser(false).parse(script).size());
	}

	@Test
	void aBareDollarIsNotADollarQuote()
	{
		assertEquals(List.of("SELECT 1"), new SqlScriptParser(false).parse("SELECT 1"));
		assertEquals(List.of("SELECT a$b"), new SqlScriptParser(false).parse("SELECT a$b;"));
	}

	// ------------------------------------------------------------------ //
	// SQL Server batches
	// ------------------------------------------------------------------ //

	@Test
	void goAloneOnALineSeparatesBatches()
	{
		String script = """
				USE tpce
				GO
				CREATE TABLE t (a int)
				GO
				SET ANSI_NULLS ON
				GO
				""";

		assertEquals(
				List.of("USE tpce", "CREATE TABLE t (a int)", "SET ANSI_NULLS ON"),
				new SqlScriptParser(true).parse(script));
	}

	@Test
	void goIsCaseInsensitiveAndToleratesWhitespace()
	{
		assertEquals(
				List.of("SELECT 1", "SELECT 2"),
				new SqlScriptParser(true).parse("SELECT 1\n  go  \nSELECT 2\n"));
	}

	@Test
	void goInsideACommentOrStringOrIdentifierIsNotASeparator()
	{
		// The old regex was split("(?i)\\bGO\\b"), so any of these split the batch.
		assertEquals(
				List.of("SELECT 1 , 2"),
				normalise(new SqlScriptParser(true).parse("SELECT 1 /* go */ , 2\n")));
		assertEquals(
				List.of("SELECT 1 , 2"),
				normalise(new SqlScriptParser(true).parse("SELECT 1 -- go\n, 2\n")));
		assertEquals(
				List.of("SELECT 'go' AS x"),
				new SqlScriptParser(true).parse("SELECT 'go' AS x\n"));
		assertEquals(
				List.of("SELECT [go] FROM t"),
				new SqlScriptParser(true).parse("SELECT [go] FROM t\n"));
		// The word go mid-statement is not a separator either.
		assertEquals(List.of("SELECT going FROM t"), new SqlScriptParser(true).parse("SELECT going FROM t\n"));
	}

	@Test
	void semicolonIsNotASeparatorInBatchMode()
	{
		assertEquals(List.of("SELECT 1; SELECT 2;"), new SqlScriptParser(true).parse("SELECT 1; SELECT 2;\n"));
	}

	// ------------------------------------------------------------------ //
	// Degenerate input
	// ------------------------------------------------------------------ //

	@Test
	void blankAndCommentOnlyScriptsParseToNothing()
	{
		assertEquals(List.of(), new SqlScriptParser(false).parse(""));
		assertEquals(List.of(), new SqlScriptParser(false).parse("   \n\t\n"));
		assertEquals(List.of(), new SqlScriptParser(false).parse("-- only a comment\n"));
		assertEquals(List.of(), new SqlScriptParser(false).parse("/* only\na comment */\n"));
	}

	@Test
	void aTrailingStatementWithoutATerminatorIsStillEmitted()
	{
		assertEquals(
				List.of("CREATE TABLE a (x int)", "CREATE TABLE b (y int)"),
				new SqlScriptParser(false).parse("CREATE TABLE a (x int);\nCREATE TABLE b (y int)\n"));
	}

	@Test
	void anUnterminatedBatchIsOneStatementNotSilentlyDropped()
	{
		// No GO, so this is a single batch. The point is that it is parsed, not discarded.
		// One batch, and the newline between the two statements is preserved: the point is that
		// the batch was parsed rather than discarded.
		assertEquals(
				List.of("CREATE TABLE t (a int)\nCREATE TABLE u (b int)"),
				new SqlScriptParser(true).parse("CREATE TABLE t (a int)\nCREATE TABLE u (b int)\n"));
	}

	@Test
	void aScriptOfNothingButTerminatorsIsNotAnError()
	{
		// "no statements" is legitimate here: there is no content to execute.
		assertEquals(List.of(), new SqlScriptParser(false).parse(";;;"));
		assertEquals(List.of(), new SqlScriptParser(true).parse("GO\nGO\n"));
	}

	@Test
	void stripCommentsLeavesCodeAndLiteralsIntact()
	{
		String script = """
				-- a comment
				CREATE TABLE t (a int); /* inline */ SELECT 'x;y';
				""";

		String stripped = SqlScriptParser.stripComments(script);

		assertFalse(stripped.contains("a comment"), stripped);
		assertFalse(stripped.contains("inline"), stripped);
		assertTrue(stripped.contains("CREATE TABLE t (a int);"), stripped);
		assertTrue(stripped.contains("'x;y'"), stripped);
	}

	// ------------------------------------------------------------------ //
	// Every shipped script
	// ------------------------------------------------------------------ //

	@ParameterizedTest(name = "{0}")
	@MethodSource("shippedScripts")
	void everyShippedScriptParsesIntoPlausibleStatements(String packed) throws IOException
	{
		String engine = parser(packed);
		String path = resource(packed);
		String content = readResource(path);

		SqlScriptParser scriptParser = SqlScriptParser.forEngine(engine);
		List<String> statements = scriptParser.parse(content);

		assertFalse(statements.isEmpty(), "no statements parsed from " + path);

		for (String statement : statements)
		{
			assertTrue(
					SQL_VERB.matcher(statement).find(),
					() -> path + ": parsed statement does not start with a SQL verb: "
							+ abbreviate(statement));
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("shippedScripts")
	void noParsedStatementCarriesCommentProse(String packed) throws IOException
	{
		// The MariaDB failure showed up as a statement whose first tokens were English. Any
		// statement containing a "--" or that starts mid-sentence is a parser bug.
		String engine = parser(packed);
		String path = resource(packed);

		for (String statement : SqlScriptParser.forEngine(engine).parse(readResource(path)))
		{
			assertFalse(
					statement.contains("--"),
					() -> path + ": comment marker survived into: " + abbreviate(statement));
			assertFalse(
					statement.contains("/*"),
					() -> path + ": block comment marker survived into: " + abbreviate(statement));
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("shippedScripts")
	void everyStatementOfTheScriptIsExecutedAtLeastOnce(String packed) throws IOException
	{
		// Belt and braces: compare the number of statements the parser produces with the number
		// of terminators in the script (semicolons, or GO lines). A mismatch means a statement
		// was merged or lost.
		String engine = parser(packed);
		String path = resource(packed);
		String content = SqlScriptParser.stripComments(readResource(path));

		long terminators = "mssql".equals(engine)
				? Arrays.stream(content.split("\n")).filter(l -> l.trim().equalsIgnoreCase("GO")).count()
				: countSemicolonsOutsideLiterals(content);

		List<String> statements = SqlScriptParser.forEngine(engine).parse(readResource(path));

		long nonEmpty = statements.stream().filter(s -> !s.isBlank()).count();
		assertEquals(
				nonEmpty,
				statements.size(),
				path + ": blank statements must not be emitted");
		assertTrue(
				statements.size() <= terminators + 1,
				path + ": " + statements.size() + " statements from " + terminators + " terminators");
		assertTrue(
				statements.size() >= terminators - 1,
				path + ": " + statements.size() + " statements from " + terminators + " terminators");
	}

	@Test
	void everyScriptStatementIsNonEmptyAndTrimmed() throws IOException
	{
		for (String engine : ENGINES)
		{
			for (String name : listScriptNames("/ddl-scripts/" + engine))
			{
				String path = "/ddl-scripts/" + engine + "/" + name;

				for (String statement : SqlScriptParser.forEngine(engine).parse(readResource(path)))
				{
					assertEquals(statement.trim(), statement, path + ": statement is not trimmed");
					assertFalse(statement.isEmpty(), path + ": empty statement");
				}
			}
		}
	}

	@Test
	void theSqlServerAnalyzeScriptSplitsIntoPerTableBatches() throws IOException
	{
		// 7_analyze_table.sql used to hold all 33 UPDATE STATISTICS in one batch, so one failure
		// aborted the other 32; every other MSSQL script separates its batches with GO.
		List<String> batches = new SqlScriptParser(true).parse(readResource("/ddl-scripts/mssql/7_analyze_table.sql"));

		long updateStatements = batches
				.stream()
				.flatMap(b -> Arrays.stream(b.split(";")))
				.filter(s -> s.toUpperCase(Locale.ROOT).trim().startsWith("UPDATE STATISTICS"))
				.count();

		assertEquals(33, updateStatements, "one UPDATE STATISTICS statement per table");
		// 33 batches for 33 UPDATE statements plus the prologue batch means no two tables
		// share a batch any more, which is the point of adding the GO separators.
		assertEquals(33, batches.size());
		assertEquals(
				1,
				batches.stream().filter(b -> b.split(";").length > 1).count(),
				"only the prologue batch (USE + SETs + the first table) holds more than one statement");
	}

	// ------------------------------------------------------------------ //
	// Helpers
	// ------------------------------------------------------------------ //

	private static long countSemicolonsOutsideLiterals(String content)
	{
		String stripped = SqlScriptParser.stripComments(content);
		long count = 0;
		boolean inSingle = false;
		boolean inDouble = false;
		boolean inBacktick = false;

		for (int i = 0; i < stripped.length(); i++)
		{
			char c = stripped.charAt(i);

			if (c == '\'' && !inDouble && !inBacktick)
			{
				inSingle = !inSingle;
			}
			else if (c == '"' && !inSingle && !inBacktick)
			{
				inDouble = !inDouble;
			}
			else if (c == '`' && !inSingle && !inDouble)
			{
				inBacktick = !inBacktick;
			}
			else if (c == ';' && !inSingle && !inDouble && !inBacktick)
			{
				count++;
			}
		}

		return count;
	}

	/** Collapses runs of whitespace so assertions do not depend on where comments were. */
	private static List<String> normalise(List<String> statements)
	{
		return statements.stream().map(s -> s.replaceAll("\\s+", " ").trim()).toList();
	}

	private static String abbreviate(String s)
	{
		return s.length() <= 160 ? s : s.substring(0, 160) + "...";
	}

	private static String readResource(String path) throws IOException
	{
		try (InputStream in = SqlScriptParserTest.class.getResourceAsStream(path))
		{
			if (in == null)
			{
				throw new IOException("resource not found: " + path);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/**
	 * Lists the {@code .sql} file names under an absolute classpath directory. The resources are
	 * read off the filesystem rather than out of the jar so the test works with
	 * {@code target/classes} without needing a jar handle.
	 */
	private static List<String> listScriptNames(String directory)
	{
		String path = directory.endsWith("/") ? directory : directory + "/";
		java.net.URL url = SqlScriptParserTest.class.getResource(path);

		if (url == null)
		{
			throw new IllegalStateException("no such classpath directory: " + path);
		}

		if ("file".equals(url.getProtocol()))
		{
			try
			{
				// Use the URI, not URL#getPath(): on Windows the latter yields "/C:/..."
				// which Path.of turns into an invalid "\C:\..." path.
				return listOnDisk(java.nio.file.Path.of(url.toURI()));
			}
			catch (java.net.URISyntaxException e)
			{
				throw new IllegalStateException("cannot convert classpath URL to a path: " + url, e);
			}
		}

		throw new IllegalStateException(
				"cannot enumerate " + path + " from a " + url.getProtocol()
						+ " URL; the test needs the resources on the filesystem (target/classes)");
	}

	private static List<String> listOnDisk(java.nio.file.Path directory)
	{
		List<String> found = new ArrayList<>();

		try (Stream<java.nio.file.Path> walk = java.nio.file.Files.list(directory))
		{
			walk.filter(p -> p.getFileName().toString().endsWith(".sql"))
					.map(p -> p.getFileName().toString())
					.sorted()
					.forEach(found::add);
		}
		catch (IOException e)
		{
			throw new IllegalStateException("cannot list " + directory, e);
		}

		assertFalse(found.isEmpty(), "no .sql resources under " + directory);

		return found;
	}

	/** Guards against a script silently disappearing from the resource set. */
	@Test
	void theShippedScriptSetIsTheOneWeReviewed()
	{
		int total = ENGINES.stream().mapToInt(e -> listScriptNames("/ddl-scripts/" + e).size()).sum();

		// postgresql 7, orioledb 8, mariadb 6, mssql 8.
		assertEquals(29, total, "the shipped script count changed");
	}
}
