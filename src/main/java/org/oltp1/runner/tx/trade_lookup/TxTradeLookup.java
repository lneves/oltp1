package org.oltp1.runner.tx.trade_lookup;

import java.sql.Array;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.oltp1.common.ErrorCtx;
import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.db.SqlEngine;
import org.oltp1.runner.generator.TxInputGenerator;
import org.oltp1.runner.tx.QueryFactory;
import org.oltp1.runner.perf.TxBase;
import org.oltp1.runner.perf.TxOutput;
import org.oltp1.runner.perf.TxStatsCollector;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TxTradeLookup extends TxBase
{
	private final ObjectMapper json = new ObjectMapper();
	private final SqlContext sqlCtx;
	private final Sql2o sql2o;

	private final TxInputGenerator txInputGen;
	private final TradeLookupQueries sql;

	public TxTradeLookup(TxInputGenerator txInputGen, SqlContext sqlCtx)
	{
		super(new TxStatsCollector("Trade-Lookup"));

		this.txInputGen = txInputGen;
		this.sqlCtx = sqlCtx;
		this.sql2o = sqlCtx.getSql2o();
		this.sql = QueryFactory.getQueries(TradeLookupQueries.class, sqlCtx.getSqlEngine());
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

		if (sqlCtx.getSqlEngine() == SqlEngine.POSTGRESQL)
		{
			Array tradeIds = con
					.getJdbcConnection()
					.createArrayOf("bigint", ArrayUtils.toObject(txInput.trade_id));

			frm1Query.addParameter("trade_ids", tradeIds);

		}
		else if (sqlCtx.getSqlEngine() == SqlEngine.MSSQL)
		{
			String tradeIdsCsv = StringUtils.join(txInput.trade_id, ',');
			frm1Query.addParameter("trade_ids", tradeIdsCsv);
		}
		else if (sqlCtx.getSqlEngine() == SqlEngine.MARIADB)
		{
			String tradeIdsJson = json.writeValueAsString(txInput.trade_id);
			frm1Query.addParameter("trade_ids", tradeIdsJson);
		}

		List<Map<String, Object>> lstTrades = frm1Query
				.addParameter("max_trades", txInput.max_trades)
				.executeAndFetchTable()
				.asList();

		txOutput.num_found = lstTrades.size();

		if (txOutput.num_found != txInput.max_trades)
		{
			txOutput.setStatus(-611);
			txOutput.setStatusMessage("num_found != max_trades");
		}

		txOutput.lst_trades_frm1 = lstTrades;

		List<Map<String, Object>> historyTrades = fetchTradeHistory(con, lstTrades);
		txOutput.lst_trades_history = historyTrades;
	}

	private void executeFrame2(TxTradeLookupOutput txOutput, final TxTradeLookupInput txInput, Connection con) throws SQLException
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

		List<Map<String, Object>> historyTrades = fetchTradeHistory(con, lstTrades);
		txOutput.lst_trades_history = historyTrades;
	}

	private void executeFrame3(Connection con, TxTradeLookupInput txInput, TxTradeLookupOutput txOutput) throws SQLException
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

	private void executeFrame4(Connection con, TxTradeLookupInput txInput, TxTradeLookupOutput txOutput)
	{
		List<Map<String, Object>> lstTrades = con
				.createQuery(sql.getFrame4())
				.addParameter("ca_id", txInput.acct_id)
				.addParameter("start_dts", txInput.start_trade_dts)
				.executeAndFetchTable()
				.asList();

		txOutput.num_found = lstTrades.size();

		if ((txOutput.num_found < 1) || (txOutput.num_found > 20))
		{
			txOutput.setStatus(-631);
			txOutput.setStatusMessage("(num_found < 1) || (num_found > 20)");
		}

		txOutput.lst_trades_frm4 = lstTrades;
	}

	private List<Map<String, Object>> fetchTradeHistory(final Connection con, final List<Map<String, Object>> lstTrades) throws SQLException
	{
		Query thQuery = con.createQuery(sql.getTradeHistory());

		if (sqlCtx.getSqlEngine() == SqlEngine.POSTGRESQL)
		{
			Long[] tids = lstTrades
					.stream()
					.map(m -> ((Long) m.get("t_id")).longValue())
					.toArray(Long[]::new);

			Array tradeIds = con.getJdbcConnection().createArrayOf("bigint", tids);
			thQuery.addParameter("trade_ids", tradeIds);
		}
		else if (sqlCtx.getSqlEngine() == SqlEngine.MSSQL)
		{
			String tradeIds = lstTrades
					.stream()
					.map(m -> ((Long) m.get("t_id")).toString())
					.collect(Collectors.joining(","));

			thQuery.addParameter("trade_ids", tradeIds);
		}
		else if (sqlCtx.getSqlEngine() == SqlEngine.MARIADB)
		{
			String tradeIds = lstTrades
					.stream()
					.map(m -> ((Long) m.get("t_id")).toString())
					.collect(Collectors.joining(","));

			String tradeIdsJson = String.format("[%s]", tradeIds);

			thQuery.addParameter("trade_ids", tradeIdsJson);
		}
		else
		{
			throw new UnsupportedOperationException("Unsupported Database for this operation");
		}

		return thQuery.executeAndFetchTable().asList();
	}

}