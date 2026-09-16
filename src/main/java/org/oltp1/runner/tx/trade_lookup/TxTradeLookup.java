package org.oltp1.runner.tx.trade_lookup;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.oltp1.common.ErrorCtx;
import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.generator.TxInputGenerator;
import org.oltp1.runner.runtime.BenchmarkMetrics;
import org.oltp1.runner.runtime.Converter;
import org.oltp1.runner.runtime.TransactionSpec;
import org.oltp1.runner.runtime.TxBase;
import org.oltp1.runner.runtime.TxOutput;
import org.oltp1.runner.runtime.TxStatsCollector;
import org.oltp1.runner.tx.QueryFactory;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

public class TxTradeLookup extends TxBase
{
	private static final int FRAME4_MAX_ROWS = 20;

	private final TxInputGenerator txInputGen;
	private final Sql2o sql2o;
	private final TradeLookupDialect sql;

	public TxTradeLookup(TxInputGenerator txInputGen, SqlContext sqlCtx, BenchmarkMetrics metrics)
	{
		super(metrics, new TxStatsCollector(TransactionSpec.TRADE_LOOKUP));
		this.txInputGen = txInputGen;
		this.sql2o = sqlCtx.getSql2o();
		this.sql = QueryFactory.getQueries(TradeLookupDialect.class, sqlCtx.getSqlEngine());
	}

	@Override
	protected final TxOutput run()
	{
		final TxTradeLookupInput txInput = txInputGen.generateTradeLookupInput();
		final TxTradeLookupOutput txOutput = new TxTradeLookupOutput();

		txOutput.frame_executed = txInput.frame_to_execute;

		try (Connection con = sql2o.beginTransaction())
		{
			if (txInput.frame_to_execute == 1)
			{
				executeFrame1(con, txInput, txOutput);
			}
			else if (txInput.frame_to_execute == 2)
			{
				executeFrame2(txOutput, txInput, con);
			}
			else if (txInput.frame_to_execute == 3)
			{
				executeFrame3(con, txInput, txOutput);
			}
			else
			{
				executeFrame4(con, txInput, txOutput);
			}

			con.commit();
		}
		catch (Throwable t)
		{
			ErrorCtx ectx = new ErrorCtx(t);
			txOutput.setStatus(-1);
			txOutput.setStatusMessage(ectx.toString());
		}

		return txOutput;
	}

	private void executeFrame1(final Connection con, final TxTradeLookupInput txInput, final TxTradeLookupOutput txOutput) throws Exception
	{
		Query frm1Query = con.createQuery(sql.getTradeInfoFrame1());

		String paramTradeIds = sql.buildTradeIdList(Arrays.stream(txInput.trade_id).boxed().toList());

		List<Map<String, Object>> lstTrades = frm1Query
				.addParameter("trade_ids", paramTradeIds)
				.addParameter("max_trades", txInput.max_trades)
				.executeAndFetchTable()
				.asList();

		txOutput.num_found = lstTrades.size();

		if (txOutput.num_found != txInput.max_trades)
		{
			txOutput.setStatus(-611);
			txOutput.setStatusMessage(String.format("num_found(%d) != max_trades(%d)", txOutput.num_found, txInput.max_trades));
		}

		txOutput.lst_trades_frm1 = lstTrades;

		List<Map<String, Object>> historyTrades = fetchTradeHistory(con, lstTrades);
		txOutput.lst_trades_history = historyTrades;
	}

	private void executeFrame2(final TxTradeLookupOutput txOutput, final TxTradeLookupInput txInput, final Connection con) throws SQLException
	{
		List<Map<String, Object>> lstTrades = con
				.createQuery(sql.getFrame2())
				.addParameter("ca_id", txInput.acct_id)
				.addParameter("start_dts", txInput.start_trade_dts)
				.addParameter("end_dts", txInput.end_trade_dts)
				.addParameter("max_trades", txInput.max_trades)
				.executeAndFetchTable()
				.asList();

		txOutput.num_found = lstTrades.size();

		if (txOutput.num_found > txInput.max_trades)
		{
			txOutput.setStatus(-621);
			txOutput.setStatusMessage("(num_found > max_trades)");
		}
		else if (txOutput.num_found == 0)
		{
			txOutput.setStatus(621);
			txOutput.setStatusMessage("(num_found == 0)");
		}

		txOutput.lst_trades_frm2 = lstTrades;

		if (lstTrades.isEmpty())
		{
			List<Map<String, Object>> historyTrades = Collections.emptyList();
			txOutput.lst_trades_history = historyTrades;
		}
		else
		{
			List<Map<String, Object>> historyTrades = fetchTradeHistory(con, lstTrades);
			txOutput.lst_trades_history = historyTrades;
		}
	}

	private void executeFrame3(final Connection con, final TxTradeLookupInput txInput, final TxTradeLookupOutput txOutput) throws SQLException
	{
		List<Map<String, Object>> lstTrades = con
				.createQuery(sql.getFrame3())
				.addParameter("symbol", txInput.symbol)
				.addParameter("start_dts", txInput.start_trade_dts)
				.addParameter("end_dts", txInput.end_trade_dts)
				.addParameter("max_trades", txInput.max_trades)
				.executeAndFetchTable()
				.asList();

		txOutput.num_found = lstTrades.size();

		if ((txOutput.num_found > txInput.max_trades))
		{
			txOutput.setStatus(-631);
			txOutput.setStatusMessage("(num_found > max_trades)");
		}
		else if (txOutput.num_found == 0)
		{
			txOutput.setStatus(631);
			txOutput.setStatusMessage("(num_found == 0)");
		}

		txOutput.lst_trades_frm3 = lstTrades;

		List<Map<String, Object>> historyTrades = fetchTradeHistory(con, lstTrades);
		txOutput.lst_trades_history = historyTrades;
	}

	private void executeFrame4(final Connection con, final TxTradeLookupInput txInput, final TxTradeLookupOutput txOutput)
	{
		Long tradeId = con
				.createQuery(sql.getFrame4TargetTrade())
				.addParameter("ca_id", txInput.acct_id)
				.addParameter("start_dts", txInput.start_trade_dts)
				.executeScalar(Long.class);

		int numTradesFound = (tradeId == null) ? 0 : 1;

		List<Map<String, Object>> lstTrades = Collections.emptyList();

		if (numTradesFound == 1)
		{
			lstTrades = con
					.createQuery(sql.getFrame4HoldingHistory())
					.addParameter("trade_id", tradeId)
					.addParameter("max_rows", FRAME4_MAX_ROWS)
					.executeAndFetchTable()
					.asList();
		}

		txOutput.trade_id = tradeId;
		txOutput.num_trades_found = numTradesFound;
		txOutput.num_found = lstTrades.size();
		txOutput.lst_trades_frm4 = lstTrades;

		int status = frame4Status(numTradesFound, txOutput.num_found);
		txOutput.setStatus(status);

		if (status != 0)
		{
			txOutput.setStatusMessage(String.format("num_trades_found(%d), num_found(%d)", numTradesFound, txOutput.num_found));
		}
	}

	static int frame4Status(int numTradesFound, int numFound)
	{
		if (numTradesFound == 0)
		{
			return 641;
		}

		if (numTradesFound != 1)
		{
			return -641;
		}

		if (numFound < 1 || numFound > FRAME4_MAX_ROWS)
		{
			return -642;
		}

		return 0;
	}

	private List<Map<String, Object>> fetchTradeHistory(final Connection con, final List<Map<String, Object>> lstTrades) throws SQLException
	{
		List<Long> tradeIds = lstTrades
				.stream()
				.map(r -> Converter.getAsLong(r.get("t_id")))
				.toList();

		String paramTradeIds = sql.buildTradeIdList(tradeIds);

		Query thQuery = con.createQuery(sql.getTradeHistory());
		thQuery.addParameter("trade_ids", paramTradeIds);

		return thQuery.executeAndFetchTable().asList();
	}

}