package org.oltp1.runner.sandbox;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.microsoft.sqlserver.jdbc.SQLServerDataTable;
import com.microsoft.sqlserver.jdbc.SQLServerPreparedStatement;

/**
 * Quick & dirty performance comparison between using Table-Value parameters and
 * the function STRING_SPLIT() on a comma separated list of values on MSSQL. For
 * the typical use case in OLTP-1 it seems that the STRING_SPLIT() function is
 * slightly faster
 */
public class TvpVsCsv
{
	private static final Random rand = new Random();

	private static final String tvpQuery = """
			    SELECT t_id AS trade_id, t_st_id, tt.tt_name, t_exec_name, t_is_cash
			    FROM trade t
			    JOIN trade_type tt ON t.t_tt_id = tt.tt_id
			    WHERE t.t_id IN (SELECT ivalue FROM ?)
			""";

	private static final String csvQuery = """
			    SELECT t_id AS trade_id, t_st_id, tt.tt_name, t_exec_name, t_is_cash
			    FROM trade t
			    JOIN trade_type tt ON t.t_tt_id = tt.tt_id
			    WHERE t.t_id IN (
			        SELECT CAST(value AS bigint) FROM STRING_SPLIT(?, ',')
			    )
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
			System.err.println("Usage: java TvpVsCsv <jdbc-url>");
			System.exit(1);
		}

		String jdbcUrl = args[0];

		try (Connection conn = DriverManager.getConnection(jdbcUrl))
		{
			System.out.println("Warming up...");

			for (int i = 0; i < ITERATIONS; i++)
			{
				runTVPQuery(conn);
				runCSVQuery(conn);
			}

			// Run each method multiple times
			List<Double> tvpTimes = new ArrayList<>();
			List<Double> csvTimes = new ArrayList<>();

			System.out.println("\nRunning TVP tests...");
			for (int i = 0; i < ITERATIONS; i++)
			{
				double time = runTVPQuery(conn);
				tvpTimes.add(time);
				System.out.printf("TVP Run %d: %.2f ms%n", i + 1, time);
			}

			System.out.println("\nRunning CSV tests...");
			for (int i = 0; i < ITERATIONS; i++)
			{
				double time = runCSVQuery(conn);
				csvTimes.add(time);
				System.out.printf("CSV Run %d: %.2f ms%n", i + 1, time);
			}

			System.out.printf("%nAverage TVP Time: %.2f ms%n", average(tvpTimes));
			System.out.printf("Average CSV Time: %.2f ms%n", average(csvTimes));
		}
	}

	private static double runTVPQuery(Connection conn) throws SQLException
	{
		long start = System.nanoTime();

		SQLServerDataTable tvp = new SQLServerDataTable();
		tvp.addColumnMetadata("ivalue", Types.BIGINT);

		for (int i = 0; i < VALUE_LEN; i++)
		{
			int ix = rand.nextInt(ids.size());
			tvp.addRow(ids.get(ix));
		}

		try (SQLServerPreparedStatement stmt = (SQLServerPreparedStatement) conn.prepareStatement(tvpQuery))
		{
			stmt.setStructured(1, "dbo.bigint_list_type", tvp);
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
