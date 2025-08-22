package org.oltp1.runner.sandbox;

import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * Quick & dirty performance comparison between using Array parameters and the
 * function STRING_TO_TABLE() on a comma separated list of values on PGSQL. For
 * the typical use case in OLTP-1 there seems to be no difference.
 */
public class ArrayVsCsv
{
	private static final Random rand = new Random();

	private static final String arrQuery0 = """
			SELECT t_id AS trade_id, t_st_id, tt.tt_name, t_exec_name, t_is_cash
			FROM trade t JOIN trade_type tt ON t.t_tt_id = tt.tt_id
			WHERE t.t_id = ANY(?);
			""";

	private static final String arrQuery1 = """
			SELECT t_id AS trade_id, t_st_id, tt.tt_name, t_exec_name, t_is_cash
			FROM trade t JOIN trade_type tt ON t.t_tt_id = tt.tt_id
			WHERE t.t_id IN (
					SELECT trade_id::bigint FROM unnest(?) AS trade_id
					);
			""";

	private static final String csvQuery = """
			SELECT t_id AS trade_id, t_st_id, tt.tt_name, t_exec_name, t_is_cash
			FROM trade t JOIN trade_type tt ON t.t_tt_id = tt.tt_id
			WHERE t.t_id IN (
					SELECT trade_id::bigint FROM string_to_table(?, ',') AS trade_id
					);
			""";

	private static final List<Long> ids = LongStream
			.rangeClosed(200000000000001L, 200000087279124L)
			.boxed()
			.collect(Collectors.toList());

	private static final int VALUE_LEN = 25;

	private static final int ITERATIONS = 1000;

	public static void main(String[] args) throws Exception
	{
		if (args.length < 1)
		{
			System.err.println("Usage: java ArrayVsCSVBenchmark <jdbc-url>");
			System.exit(1);
		}

		String jdbcUrl = args[0];

		try (Connection conn = DriverManager.getConnection(jdbcUrl))
		{
			System.out.println("Warming up...");

			for (int i = 0; i < ITERATIONS; i++)
			{
				runArrayQuery(conn, arrQuery0);
				runArrayQuery(conn, arrQuery1);
				runCSVQuery(conn);
			}

			// Run each method multiple times
			List<Double> arr0Times = new ArrayList<>();
			List<Double> arr1Times = new ArrayList<>();
			List<Double> csvTimes = new ArrayList<>();

			System.out.println("\nRunning Sql Array tests...");
			for (int i = 0; i < ITERATIONS; i++)
			{
				double time = runArrayQuery(conn, arrQuery0);
				arr0Times.add(time);
				// System.out.printf("Sql Array0 Run %d: %.2f ms%n", i + 1, time);
			}

			for (int i = 0; i < ITERATIONS; i++)
			{
				double time = runArrayQuery(conn, arrQuery1);
				arr1Times.add(time);
				// System.out.printf("Sql Array1 Run %d: %.2f ms%n", i + 1, time);
			}

			System.out.println("\nRunning CSV tests...");
			for (int i = 0; i < ITERATIONS; i++)
			{
				double time = runCSVQuery(conn);
				csvTimes.add(time);
				// System.out.printf("CSV Run %d: %.2f ms%n", i + 1, time);
			}

			System.out.printf("%nAverage Sql Array0 Time: %.2f ms%n", average(arr0Times));
			System.out.printf("Average Sql Array0 Time: %.2f ms%n", average(arr1Times));
			System.out.printf("Average CSV Time: %.2f ms%n", average(csvTimes));
		}
	}

	private static double runArrayQuery(Connection conn, String sql) throws SQLException
	{
		long start = System.nanoTime();

		Long[] arr = new Long[VALUE_LEN];

		for (int i = 0; i < VALUE_LEN; i++)
		{
			int ix = rand.nextInt(ids.size());
			arr[i] = ids.get(ix);
		}

		Array sqlArr = conn
				.createArrayOf("bigint", arr);

		try (PreparedStatement stmt = conn.prepareStatement(sql))
		{
			stmt.setArray(1, sqlArr);
			try (ResultSet rs = stmt.executeQuery())
			{
				consumeResultSet(rs);
			}
		}
		return (System.nanoTime() - start) / 1_000_000.0;
	}

	private static double runCSVQuery(Connection conn) throws SQLException
	{
		long start = System.nanoTime();

		StringBuilder csvp = new StringBuilder();

		for (int i = 0; i < VALUE_LEN - 1; i++)
		{
			csvp.append(ids.get(rand.nextInt(ids.size())));
			csvp.append(",");
		}
		csvp.append(ids.get(rand.nextInt(ids.size())));

		try (PreparedStatement stmt = conn.prepareStatement(csvQuery))
		{
			stmt.setString(1, csvp.toString());
			try (ResultSet rs = stmt.executeQuery())
			{
				consumeResultSet(rs);
			}
		}
		return (System.nanoTime() - start) / 1_000_000.0;
	}

	private static int consumeResultSet(ResultSet rs) throws SQLException
	{
		int count = 0;
		while (rs.next())
		{
			count++;
			// System.out.println(ResultsetHandler.rowToString(rs));
		}
		return count;
	}

	private static double average(List<Double> values)
	{
		return values.stream().mapToDouble(d -> d).average().orElse(0.0);
	}
}
