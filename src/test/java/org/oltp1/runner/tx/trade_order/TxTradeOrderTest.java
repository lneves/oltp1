package org.oltp1.runner.tx.trade_order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.oltp1.runner.generator.TxInputGenerator;
import org.oltp1.runner.model.TradeType;
import org.oltp1.runner.model.TransactionStatus;
import org.oltp1.runner.runtime.ExchangeEmulator;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;
import org.sql2o.data.Table;

/**
 * Tests how Trade-Order handles a reference row the database does not have.
 *
 * <p>These are reachable data conditions, not programming errors. {@code CompanySelector} builds
 * the symbol universe from {@code SECURITY INNER JOIN company} without requiring a
 * {@code LAST_TRADE} row, and the tax rate is only read for a customer with a capital gain, so a
 * customer with no {@code CUSTOMER_TAXRATE} row only fails on some transactions.
 *
 * <p>Before the fix each of those reads unboxed {@code executeScalar(...)} straight into a
 * {@code double}, so the failure surfaced as a {@link NullPointerException} naming neither the
 * query nor the symbol, and the transaction was counted as a generic error.
 *
 * <p>All JDBC is mocked; no database is involved.
 */
class TxTradeOrderTest
{
	private static final String GET_CUSTOMER_INFO = "get-customer-info";
	private static final String GET_SECURITY_BY_SYMBOL = "get-security-by-symbol";
	private static final String GET_LAST_TRADE = "get-last-trade";
	private static final String GET_ACCOUNT_PERMISSIONS = "get-account-permissions";
	private static final String GET_TRADE_TYPE = "get-trade-type";
	private static final String GET_HOLDING_SUMMARY = "get-holding-summary";
	private static final String GET_TAX_RATE = "get-tax-rate";
	private static final String GET_FEES = "get-fees";
	private static final String GET_CUSTOMER_ASSETS = "get-customer-assets";

	/** A dialect whose every method returns a distinct, identifiable marker. */
	private static final class StubDialect implements TradeOrderDialect
	{
		@Override
		public String getCustomerInfo()
		{
			return GET_CUSTOMER_INFO;
		}

		@Override
		public String getAccountPermissions()
		{
			return GET_ACCOUNT_PERMISSIONS;
		}

		@Override
		public String getSecurityInfoByCoName()
		{
			return "get-security-by-co-name";
		}

		@Override
		public String getSecurityInfoBySymbol()
		{
			return GET_SECURITY_BY_SYMBOL;
		}

		@Override
		public String getLastTrade()
		{
			return GET_LAST_TRADE;
		}

		@Override
		public String getTradeType()
		{
			return GET_TRADE_TYPE;
		}

		@Override
		public String getHoldingSummary()
		{
			return GET_HOLDING_SUMMARY;
		}

		@Override
		public String getHoldingAsc()
		{
			return "get-holding-asc";
		}

		@Override
		public String getHoldingDesc()
		{
			return "get-holding-desc";
		}

		@Override
		public String getTaxRate()
		{
			return GET_TAX_RATE;
		}

		@Override
		public String getFees()
		{
			return GET_FEES;
		}

		@Override
		public String getCustomerAssets()
		{
			return GET_CUSTOMER_ASSETS;
		}

		@Override
		public String insertTrade()
		{
			return "insert-trade";
		}

		@Override
		public String insertTradeRequest()
		{
			return "insert-trade-request";
		}

		@Override
		public String insertTradeHistory()
		{
			return "insert-trade-history";
		}
	}

	private static TxTradeOrderInput marketBuy(String symbol)
	{
		TxTradeOrderInput input = new TxTradeOrderInput();
		input.acct_id = 4242L;
		input.symbol = symbol;
		input.trade_qty = 100;
		input.requested_price = 25.0;
		input.trade_type = TradeType.MARKET_BUY;
		input.type_is_margin = false;
		input.st_pending_id = "PNDG";
		input.st_submitted_id = "SBMT";
		return input;
	}

	private static Table tableOf(List<Map<String, Object>> rows)
	{
		Table table = mock(Table.class);
		when(table.asList()).thenReturn(rows);
		return table;
	}

	/**
	 * A Query that binds and returns the given rows.
	 *
	 * <p>The Table and the Query are both fully built <em>before</em> they are attached with
	 * {@code when(con.createQuery(..))}. Evaluating a stubbing call inside the argument of an
	 * unfinished {@code when(..)} is what produces Mockito's
	 * {@code UnfinishedStubbingException}: {@code when()} is a stateful operation and cannot be
	 * nested.
	 */
	private static Query queryReturningTable(List<Map<String, Object>> rows)
	{
		Table table = tableOf(rows);

		Query query = newQuery();
		when(query.executeAndFetchTable()).thenReturn(table);

		return query;
	}

	private static Query queryReturningScalar(Double scalar)
	{
		Query query = newQuery();
		when(query.executeScalar(Double.class)).thenReturn(scalar);
		return query;
	}

	/**
	 * A Query mock that survives sql2o's fluent-builder chain.
	 *
	 * <p>{@code RETURNS_SELF}, not a hand-written {@code addParameter} stub. sql2o overloads
	 * {@code addParameter} once per value type, and which overload the compiler picks at a call
	 * site depends on the <em>static</em> type of the value: {@code addParameter("acct_id", someLong)}
	 * binds the {@code (String, long)} overload, {@code addParameter("symbol", someString)} the
	 * {@code (String, String)} one, and {@code addParameter("cust_tier", map.get(..))} the
	 * {@code (String, Object)} one. Stubbing a single overload therefore leaves the others
	 * returning null, and the very next link in the chain throws
	 * {@code NullPointerException: ...addParameter(...) is null}.
	 *
	 * <p>Only the terminal methods are stubbed explicitly; an explicit stub always takes
	 * precedence over the default answer.
	 */
	private static Query newQuery()
	{
		return mock(Query.class, Answers.RETURNS_SELF);
	}

	private static void returns(Connection con, String sql, Query query)
	{
		when(con.createQuery(sql)).thenReturn(query);
	}

	/** Frame 1 succeeds and the executor's own name and tax id match, so frame 2 is skipped. */
	private static Connection connectionWithHappyFrames1And2(Connection con, Map<String, Object> customer)
	{
		returns(con, GET_CUSTOMER_INFO, queryReturningTable(List.of(customer)));

		Query acl = newQuery();
		when(acl.executeScalar(String.class)).thenReturn("1111");
		returns(con, GET_ACCOUNT_PERMISSIONS, acl);

		return con;
	}

	@Test
	void aSecurityWithNoLastTradeRowIsNamedInTheErrorNotAnNpe()
	{
		// The regression. LAST_TRADE has no row, so executeScalar returns null.
		TxInputGenerator inputGen = mock(TxInputGenerator.class);

		Map<String, Object> customer = new LinkedHashMap<>();
		customer.put("cust_id", 1L);
		customer.put("cust_l_name", "SAME");
		customer.put("cust_f_name", "SAME");
		customer.put("tax_id", "TAX");

		TxTradeOrderInput input = marketBuy("ZZZZ");
		input.exec_l_name = "SAME";
		input.exec_f_name = "SAME";
		input.exec_tax_id = "TAX";
		when(inputGen.generateTradeOrderInput()).thenReturn(input);

		Sql2o sql2o = mock(Sql2o.class);
		Connection con = mock(Connection.class);
		when(sql2o.beginTransaction()).thenReturn(con);
		connectionWithHappyFrames1And2(con, customer);

		Map<String, Object> security = new LinkedHashMap<>();
		security.put("symbol", "ZZZZ");
		security.put("exch_id", "NYSE");
		returns(con, GET_SECURITY_BY_SYMBOL, queryReturningTable(List.of(security)));

		// The bug: no LAST_TRADE row.
		returns(con, GET_LAST_TRADE, queryReturningScalar(null));

		TxTradeOrder tx = new TxTradeOrder(inputGen, sql2o, new StubDialect(), null, mock(ExchangeEmulator.class));

		TxTradeOrderOutput output = (TxTradeOrderOutput) tx.execute();

		assertEquals(-734, output.getStatus(), "expected the no-LAST_TRADE status, got: " + output.getStatusMessage());
		assertTrue(output.getStatusMessage().contains("LAST_TRADE"), output.getStatusMessage());
		assertTrue(output.getStatusMessage().contains("ZZZZ"), "the symbol must be named: " + output.getStatusMessage());
		assertFalse(
				output.getStatusMessage().contains("NullPointerException"),
				"must not surface as an NPE: " + output.getStatusMessage());

		// A transaction that fails must roll back and must not commit.
		verify(con).rollback();
		verify(con, never()).commit();
	}

	@Test
	void aSecurityThatDoesNotExistIsNamedBeforeAnyOtherRead()
	{
		TxInputGenerator inputGen = mock(TxInputGenerator.class);

		TxTradeOrderInput input = marketBuy("NOPE");
		input.exec_l_name = "SAME";
		input.exec_f_name = "SAME";
		input.exec_tax_id = "TAX";
		when(inputGen.generateTradeOrderInput()).thenReturn(input);

		Map<String, Object> customer = new LinkedHashMap<>();
		customer.put("cust_id", 1L);
		customer.put("cust_l_name", "SAME");
		customer.put("cust_f_name", "SAME");
		customer.put("tax_id", "TAX");

		Sql2o sql2o = mock(Sql2o.class);
		Connection con = mock(Connection.class);
		when(sql2o.beginTransaction()).thenReturn(con);
		connectionWithHappyFrames1And2(con, customer);

		// No SECURITY row for the symbol.
		returns(con, GET_SECURITY_BY_SYMBOL, queryReturningTable(List.of()));

		TxTradeOrder tx = new TxTradeOrder(inputGen, sql2o, new StubDialect(), null, mock(ExchangeEmulator.class));

		TxTradeOrderOutput output = (TxTradeOrderOutput) tx.execute();

		assertEquals(-730, output.getStatus(), output.getStatusMessage());
		assertTrue(output.getStatusMessage().contains("SECURITY"), output.getStatusMessage());
		assertTrue(output.getStatusMessage().contains("NOPE"), output.getStatusMessage());

		// It must bail out before reading a price for a security it does not have.
		verify(con, never()).createQuery(GET_LAST_TRADE);

		verify(con).rollback();
		verify(con, never()).commit();
	}

	@Test
	void theNewStatusCodesAreRegistered()
	{
		// The codes the frame sets must be documented, so an operator reading a failure can look
		// it up, and so a typo cannot slip in silently.
		assertEquals(-730, TransactionStatus.TRADE_ORDER_F3_NO_SECURITY_FOUND.getCode());
		assertTrue(TransactionStatus.TRADE_ORDER_F3_NO_SECURITY_FOUND.isError());
		assertEquals(-734, TransactionStatus.TRADE_ORDER_F3_NO_LAST_TRADE_FOUND.getCode());
		assertTrue(TransactionStatus.TRADE_ORDER_F3_NO_LAST_TRADE_FOUND.isError());
	}

	@Test
	void aMissingSecurityLookedUpByCompanyNamesTheCompanyAndIssue()
	{
		TxInputGenerator inputGen = mock(TxInputGenerator.class);

		TxTradeOrderInput input = marketBuy(null);
		input.co_name = "ACME WIDGETS";
		input.issue = "AA";
		input.exec_l_name = "SAME";
		input.exec_f_name = "SAME";
		input.exec_tax_id = "TAX";
		when(inputGen.generateTradeOrderInput()).thenReturn(input);

		Map<String, Object> customer = new LinkedHashMap<>();
		customer.put("cust_id", 1L);
		customer.put("cust_l_name", "SAME");
		customer.put("cust_f_name", "SAME");
		customer.put("tax_id", "TAX");

		Sql2o sql2o = mock(Sql2o.class);
		Connection con = mock(Connection.class);
		when(sql2o.beginTransaction()).thenReturn(con);
		connectionWithHappyFrames1And2(con, customer);

		returns(con, "get-security-by-co-name", queryReturningTable(List.of()));

		TxTradeOrder tx = new TxTradeOrder(inputGen, sql2o, new StubDialect(), null, mock(ExchangeEmulator.class));

		TxTradeOrderOutput output = (TxTradeOrderOutput) tx.execute();

		assertEquals(-730, output.getStatus(), output.getStatusMessage());
		assertTrue(output.getStatusMessage().contains("ACME WIDGETS"), output.getStatusMessage());
		assertTrue(output.getStatusMessage().contains("AA"), output.getStatusMessage());
	}
}
