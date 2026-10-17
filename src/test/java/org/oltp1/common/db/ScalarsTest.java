package org.oltp1.common.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.oltp1.common.MissingReferenceDataException;
import org.sql2o.Connection;
import org.sql2o.Query;

/**
 * Tests the null-safe scalar and single-row reads.
 *
 * <p>The defect these guard against: every one of these call sites used to unbox the result of
 * {@code executeScalar(...)} straight into a primitive, so a query that legitimately returned no
 * row &mdash; a security with no {@code LAST_TRADE} row, a customer with no
 * {@code CUSTOMER_TAXRATE} row &mdash; produced a {@link NullPointerException} that named neither
 * the query nor the key. All mocked at the JDBC layer; no database is involved.
 */
class ScalarsTest
{
	private static final String LAST_TRADE_SQL = "SELECT lt_price FROM last_trade WHERE lt_s_symb = :symbol";

	/**
	 * The single {@link Query} a connection hands out for every statement, with the
	 * self-returning {@code addParameter} stub sql2o needs for the
	 * {@code createQuery(..).addParameter(..).executeScalar(..)} chain to survive on a mock.
	 *
	 * <p>One mock per connection, deliberately: a second {@code when(con.createQuery(..))} would
	 * replace the {@code createQuery} stubbing and the earlier Query would become unreachable,
	 * which is how {@code requireIntAndRequireLongBehaveTheSameWay} was returning null.
	 */
	private static Query queryFor(Connection con)
	{
		Query query = mock(Query.class);

		when(query.addParameter(anyString(), ArgumentMatchers.<Object> any())).thenReturn(query);
		when(con.createQuery(anyString())).thenReturn(query);

		return query;
	}

	private static Query returningDouble(Connection con, Double value)
	{
		Query query = queryFor(con);
		when(query.executeScalar(Double.class)).thenReturn(value);
		return query;
	}

	@Test
	void requireDoubleReturnsTheValue()
	{
		Connection con = mock(Connection.class);
		returningDouble(con, 27.5);

		assertEquals(27.5, Scalars.requireDouble(con, LAST_TRADE_SQL, "LAST_TRADE row for ACME", "symbol", "ACME"));
	}

	@Test
	void requireDoubleBindsEveryParameter()
	{
		Connection con = mock(Connection.class);
		Query query = returningDouble(con, 27.5);

		Scalars.requireDouble(con, LAST_TRADE_SQL, "LAST_TRADE row for ACME", "symbol", "ACME");

		// The (Object) cast is load-bearing: the production call site's value is statically
		// Object, so it binds addParameter(String, Object). Without the cast this would verify
		// addParameter(String, String) - a different overload Mockito will never match, and it
		// reports "wanted but not invoked" while listing the very call that did happen.
		verify(query).addParameter("symbol", (Object) "ACME");
		verify(con).createQuery(LAST_TRADE_SQL);
	}

	@Test
	void requireDoubleBindsANullValueRatherThanBlowingUp()
	{
		// A null parameter value is legal SQL; Map.of would have rejected it, which is why the
		// helper takes name/value pairs rather than a Map.
		Connection con = mock(Connection.class);
		Query query = returningDouble(con, 1.0);

		// A bare null is the value. Wrapping it - new Object[] { null } - would make the value an
		// array, and the parameter list three elements long, which the odd-length check rejects.
		Scalars.requireDouble(con, LAST_TRADE_SQL, "LAST_TRADE row for null", "symbol", null);

		verify(query).addParameter("symbol", (Object) null);
	}

	@Test
	void requireDoubleNamesTheQueryAndTheKeyWhenThereIsNoRow()
	{
		Connection con = mock(Connection.class);
		returningDouble(con, null);

		MissingReferenceDataException thrown = assertThrows(
				MissingReferenceDataException.class,
				() -> Scalars.requireDouble(con, LAST_TRADE_SQL, "LAST_TRADE row for symbol 'ZZZZ'", "symbol", "ZZZZ"));

		String message = thrown.getMessage();

		assertTrue(message.contains("LAST_TRADE row for symbol 'ZZZZ'"), message);
		assertTrue(message.contains("ZZZZ"), "the key value must appear: " + message);
		assertTrue(message.contains("lt_price"), "the query must appear: " + message);
		assertEquals("LAST_TRADE row for symbol 'ZZZZ'", thrown.getReference());
	}

	@Test
	void requireDoubleNeverThrowsNullPointerException()
	{
		// The regression in one assertion: this used to be a NullPointerException.
		Connection con = mock(Connection.class);
		returningDouble(con, null);

		assertThrows(MissingReferenceDataException.class,
				() -> Scalars.requireDouble(con, LAST_TRADE_SQL, "r", "symbol", "X"));

		assertThrows(MissingReferenceDataException.class,
				() -> Scalars.requireDouble(con, LAST_TRADE_SQL, "r"));
	}

	@Test
	void requireIntAndRequireLongBehaveTheSameWay()
	{
		Connection none = mock(Connection.class);
		Query empty = queryFor(none);
		when(empty.executeScalar(Integer.class)).thenReturn(null);
		when(empty.executeScalar(Long.class)).thenReturn(null);

		assertThrows(MissingReferenceDataException.class,
				() -> Scalars.requireInt(none, "select 1", "an int", "x", 1));
		assertThrows(MissingReferenceDataException.class,
				() -> Scalars.requireLong(none, "select 1", "a long", "x", 1L));

		Connection ok = mock(Connection.class);
		Query full = queryFor(ok);
		when(full.executeScalar(Integer.class)).thenReturn(7);
		when(full.executeScalar(Long.class)).thenReturn(7L);

		assertEquals(7, Scalars.requireInt(ok, "select 1", "an int"));
		assertEquals(7L, Scalars.requireLong(ok, "select 1", "a long"));
	}

	@Test
	void findReturnsNullInsteadOfThrowing()
	{
		Connection con = mock(Connection.class);
		returningDouble(con, null);

		assertNull(Scalars.find(con, LAST_TRADE_SQL, Double.class, "symbol", "ACME"));
	}

	@Test
	void findBindsParameters()
	{
		Connection con = mock(Connection.class);
		Query query = returningDouble(con, 3.5);

		assertEquals(3.5, Scalars.find(con, LAST_TRADE_SQL, Double.class, "symbol", "ACME"));

		verify(query).addParameter("symbol", (Object) "ACME");
	}

	@Test
	void oddParameterListIsRejected()
	{
		Connection con = mock(Connection.class);
		returningDouble(con, 1.0);

		IllegalArgumentException thrown = assertThrows(
				IllegalArgumentException.class,
				() -> Scalars.requireDouble(con, "select 1", "r", "symbol"));

		assertTrue(thrown.getMessage().contains("alternate name and value"), thrown.getMessage());
	}

	// ------------------------------------------------------------------ //
	// requireOne
	// ------------------------------------------------------------------ //

	@Test
	void requireOneReturnsTheSingleRow()
	{
		assertEquals("row", Scalars.requireOne(List.of("row"), "TRADE_TYPE row for 'TMB'"));
	}

	@Test
	void requireOneNamesWhatWasMissing()
	{
		MissingReferenceDataException thrown = assertThrows(
				MissingReferenceDataException.class,
				() -> Scalars.requireOne(List.of(), "TRADE_TYPE row for 'TMB'"));

		assertTrue(thrown.getMessage().contains("TRADE_TYPE row for 'TMB'"), thrown.getMessage());
		assertTrue(thrown.getMessage().contains("no rows"), thrown.getMessage());
	}

	@Test
	void requireOneHandlesNullAndMoreThanOneRow()
	{
		assertThrows(MissingReferenceDataException.class, () -> Scalars.requireOne(null, "fee row"));

		IllegalStateException tooMany = assertThrows(
				IllegalStateException.class,
				() -> Scalars.requireOne(List.of("a", "b"), "fee row"));

		assertTrue(tooMany.getMessage().contains("at most one row but got 2"), tooMany.getMessage());
	}

	// ------------------------------------------------------------------ //
	// The exception type
	// ------------------------------------------------------------------ //

	@Test
	void missingReferenceDataIsARuntimeExceptionCarryingTheReference()
	{
		MissingReferenceDataException e = new MissingReferenceDataException("LAST_TRADE row for ACME", "no rows");

		assertNotNull(e.getReference());
		assertTrue(e.getMessage().contains("LAST_TRADE row for ACME"), e.getMessage());
		assertTrue(e.getMessage().contains("no rows"), e.getMessage());
		assertTrue(e instanceof RuntimeException);
	}

	@Test
	void missingReferenceDataIsNotADataAccessException()
	{
		// A retry policy keyed on SQLTransientException must not pick this up.
		assertFalse(((Object) new MissingReferenceDataException("r", "d")) instanceof java.sql.SQLException);
	}
}
