package org.oltp1.common.db;

import java.util.Arrays;
import java.util.List;

import org.oltp1.common.MissingReferenceDataException;
import org.sql2o.Connection;
import org.sql2o.Query;

/**
 * Scalar and single-row reads that fail with a message naming the query and the key, instead of
 * with a {@link NullPointerException} from unboxing {@code null}.
 *
 * <p>sql2o's {@code Query.executeScalar(Class)} returns {@code null} when the result set is empty,
 * so every one of these call sites used to throw an unbox NPE on an <em>expected</em> data
 * condition: a security with no {@code LAST_TRADE} row, a customer with no
 * {@code CUSTOMER_TAXRATE} row. The transaction was caught and counted as a generic error, and
 * the log line identified neither the query nor the key that was missing.
 */
public final class Scalars
{
	private Scalars()
	{
		// utility class
	}

	/**
	 * Reads a single {@code double}, or fails with a message naming what was missing.
	 *
	 * @param con the connection to read on
	 * @param sql the query
	 * @param reference a human-readable description of the reference, e.g.
	 * {@code "LAST_TRADE row for symbol ACME"}
	 * @param nameValuePairs alternating parameter name and value, for the query's named parameters.
	 * May be empty; a value may be {@code null}.
	 * @return the value, never {@code null}
	 * @throws MissingReferenceDataException if the query returns no row
	 */
	public static double requireDouble(Connection con, String sql, String reference, Object... nameValuePairs)
	{
		Double value = readScalar(con, sql, reference, Double.class, nameValuePairs);

		return requirePresent(value, reference, "Double");
	}

	/**
	 * Reads a single {@code int}, or fails with a message naming what was missing.
	 *
	 * @throws MissingReferenceDataException if the query returns no row
	 * @see #requireDouble(Connection, String, String, Object...)
	 */
	public static int requireInt(Connection con, String sql, String reference, Object... nameValuePairs)
	{
		Integer value = readScalar(con, sql, reference, Integer.class, nameValuePairs);

		return requirePresent(value, reference, "Integer");
	}

	/**
	 * Reads a single {@code long}, or fails with a message naming what was missing.
	 *
	 * @throws MissingReferenceDataException if the query returns no row
	 * @see #requireDouble(Connection, String, String, Object...)
	 */
	public static long requireLong(Connection con, String sql, String reference, Object... nameValuePairs)
	{
		Long value = readScalar(con, sql, reference, Long.class, nameValuePairs);

		return requirePresent(value, reference, "Long");
	}

	/**
	 * Reads a single value, or {@code null} when the query returns no row. Use this when a missing
	 * row should become a domain-level transaction status rather than an exception; the
	 * {@code reference} is then only used to describe the value in the output.
	 */
	public static <T> T find(Connection con, String sql, Class<T> type, Object... nameValuePairs)
	{
		return readScalar(con, sql, null, type, nameValuePairs);
	}

	/**
	 * Reads exactly one row from a result set, or fails with a message naming what was expected.
	 * The alternatives were {@code rows().getFirst()} and {@code rows().get(0)}, which throw
	 * {@code NoSuchElementException} and {@code IndexOutOfBoundsException} and name nothing.
	 *
	 * @throws MissingReferenceDataException if there is no row
	 * @throws IllegalStateException if there is more than one row, which means the query's
	 * cardinality is wrong and is a defect rather than a data condition
	 */
	public static <T> T requireOne(List<T> rows, String reference)
	{
		if (rows == null || rows.isEmpty())
		{
			throw new MissingReferenceDataException(reference, "the query returned no rows");
		}

		if (rows.size() > 1)
		{
			throw new IllegalStateException(reference + " -> expected at most one row but got " + rows.size());
		}

		return rows.get(0);
	}

	private static <T> T readScalar(Connection con, String sql, String reference, Class<T> type, Object... nameValuePairs)
	{
		// Validated before the connection is touched, so a bad call fails immediately and the
		// check does not need a live Connection to be exercised.
		if (nameValuePairs != null && nameValuePairs.length % 2 != 0)
		{
			throw new IllegalArgumentException(
					"nameValuePairs must alternate name and value, got " + Arrays.toString(nameValuePairs));
		}

		Query query = con.createQuery(sql);

		if (nameValuePairs != null)
		{
			for (int i = 0; i < nameValuePairs.length; i += 2)
			{
				query = query.addParameter(String.valueOf(nameValuePairs[i]), nameValuePairs[i + 1]);
			}
		}

		T value = query.executeScalar(type);

		if (value == null && reference != null)
		{
			throw new MissingReferenceDataException(
					reference,
					"no row for " + describe(nameValuePairs) + " [query: " + firstLine(sql) + "]");
		}

		return value;
	}

	/**
	 * The last line of defence against an NPE if {@link #readScalar} ever stops guaranteeing a
	 * non-null result for a non-null reference.
	 */
	private static <T> T requirePresent(T value, String reference, String type)
	{
		if (value == null)
		{
			throw new MissingReferenceDataException(reference, "the query returned no " + type);
		}

		return value;
	}

	private static String describe(Object... nameValuePairs)
	{
		return nameValuePairs == null || nameValuePairs.length == 0
				? "(no parameters)"
				: Arrays.toString(nameValuePairs);
	}

	private static String firstLine(String sql)
	{
		String trimmed = sql.strip();
		int newline = trimmed.indexOf('\n');

		return newline < 0 ? trimmed : trimmed.substring(0, newline) + " ...";
	}
}
