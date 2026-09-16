package org.oltp1.runner.tx.trade_cleanup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.oltp1.runner.generator.TxInputGenerator;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

class TxTradeCleanupTest
{
	private static final class StubDialect implements TradeCleanupDialect
	{
		@Override
		public String insertTradeHistory1()
		{
			return "insert-history-1";
		}

		@Override
		public String updateTrade1()
		{
			return "update-trade-1";
		}

		@Override
		public String deleteTradeRequest()
		{
			return "delete-trade-request";
		}

		@Override
		public String updateTrade2()
		{
			return "update-trade-2";
		}

		@Override
		public String insertTradeHistory2()
		{
			return "insert-history-2";
		}
	}

	@Test
	void insertsFrame2HistoryBeforeCancelingAndCountsCanceledTrades()
	{
		TxInputGenerator inputGen = mock(TxInputGenerator.class);
		when(inputGen.generateTradeCleanupInput()).thenReturn(
				new TxTradeCleanupInput("CNCL", "PNDG", "SBMT", 42L));

		Sql2o sql2o = mock(Sql2o.class);
		Connection con = mock(Connection.class);
		when(sql2o.beginTransaction()).thenReturn(con);

		Query query = mock(Query.class);
		when(query.addParameter(anyString(), anyString())).thenReturn(query);
		when(query.addParameter(anyString(), anyLong())).thenReturn(query);
		when(query.executeUpdate()).thenReturn(con);
		when(con.createQuery(anyString())).thenReturn(query);
		when(con.getResult()).thenReturn(3, 5);

		TxTradeCleanup tx = new TxTradeCleanup(inputGen, sql2o, new StubDialect());

		TxTradeCleanupOutput output = (TxTradeCleanupOutput) tx.execute();

		assertEquals(0, output.getStatus(), output.getStatusMessage());
		assertEquals(8, output.trades_cleaned_up);

		ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
		verify(con, times(6)).createQuery(sqlCaptor.capture());

		assertEquals(
				List.of(
						"insert-history-1",
						"update-trade-1",
						"insert-history-1",
						"delete-trade-request",
						"insert-history-2",
						"update-trade-2"),
				sqlCaptor.getAllValues());

		verify(con).commit();
	}
}
