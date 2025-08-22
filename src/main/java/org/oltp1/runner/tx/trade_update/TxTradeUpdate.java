package org.oltp1.runner.tx.trade_update;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.sql2o.Sql2o;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

public class TxTradeUpdate extends TxBase
{
	private static final int max_trades = 20;

	private static final int max_updates = 20;

	private final ObjectMapper json = new ObjectMapper();
	private final Set<String> keysToKeep = Set.of("trade_id", "t_qty", "tt_name", "s_name");

	private final SqlContext sqlCtx;
	private final Sql2o sql2o;

	private final TxInputGenerator txInputGen;
	private final TradeUpdateQueries sql;

	public TxTradeUpdate(TxInputGenerator txInputGen, SqlContext sqlCtx)
	{
		super(new TxStatsCollector("Trade-Update"));

		this.txInputGen = txInputGen;
		this.sqlCtx = sqlCtx;
		this.sql2o = sqlCtx.getSql2o();
		this.sql = QueryFactory.getQueries(TradeUpdateQueries.class, sqlCtx.getSqlEngine());
		this.json.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

	}

	@Override
	protected final TxOutput run()
	{
		TxTradeUpdateOutput txOutput = new TxTradeUpdateOutput();

		final TxTradeUpdateInput txInput = txInputGen.generateTradeUpdateInput();

		try (Connection con = sql2o.beginTransaction())
		{
			if (txInput.frame_to_execute == 1)
			{
				executeFrame1(con, txInput, txOutput);
			}
			else if (txInput.frame_to_execute == 2)
			{
				executeFrame2(con, txInput, txOutput);
			}
			else
			{
				executeFrame3(con, txInput, txOutput);
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

	private void executeFrame1(final Connection con, final TxTradeUpdateInput txInput, final TxTradeUpdateOutput txOutput)
	{
		txOutput.frame_executed = 1;
		int num_found = 0;
		int num_updated = 0;

		List<Long> tradeIds = Arrays
				.stream(txInput.trade_id)
				.boxed()
				.collect(Collectors.toList());

		num_found = tradeIds.size();

		String tradesCsv = getCsv(tradeIds);

		if (sqlCtx.getSqlEngine() == SqlEngine.MARIADB)
		{
			String tradesJsonArr = String.format("[%s]", tradesCsv);

			con
					.createQuery(sql.updateTradesFrame1())
					.addParameter("trade_lst", tradesJsonArr)
					.executeUpdate();
		}
		else
		{
			con
					.createQuery(sql.updateTradesFrame1())
					.addParameter("trade_lst", tradesCsv)
					.executeUpdate();
		}

		num_updated = con.getResult();

		populateFullTradeInfo(con, tradeIds, txOutput);

		if (num_found != max_trades)
		{
			txOutput.setStatus(-1011);
			txOutput.setStatusMessage("(num_found != max_trades)");
		}

		if (num_updated != max_updates)
		{
			txOutput.setStatus(-1012);
			txOutput.setStatusMessage("(num_updated != max_updates)");
		}
	}

	private void executeFrame2(Connection con, TxTradeUpdateInput txInput, TxTradeUpdateOutput txOutput)
	{
		txOutput.frame_executed = 2;
		int num_found = 0;
		int num_updated = 0;

		List<Map<String, Object>> tradeInfo = con
				.createQuery(sql.getTradesFrame2())
				.addParameter("acct_id", txInput.acct_id)
				.addParameter("start_trade_dts", txInput.start_trade_dts)
				.addParameter("end_trade_dts", txInput.end_trade_dts)
				.addParameter("limit", max_trades)
				.executeAndFetchTable()
				.asList();

		num_found = tradeInfo.size();

		txOutput.trade_info = tradeInfo;

		if (tradeInfo.size() > 0)
		{
			List<Long> tradeIds = tradeInfo
					.stream()
					.map(t -> (Long) t.get("trade_id"))
					.collect(Collectors.toList());

			String tradesCsv = getCsv(tradeIds);

			if (sqlCtx.getSqlEngine() == SqlEngine.MARIADB)
			{
				String tradesJsonArr = String.format("[%s]", tradesCsv);

				con
						.createQuery(sql.updateTradesFrame2())
						.addParameter("trade_lst", tradesJsonArr)
						.executeUpdate();
			}
			else
			{
				con
						.createQuery(sql.updateTradesFrame2())
						.addParameter("trade_lst", tradesCsv)
						.executeUpdate();
			}

			num_updated = con.getResult();

			populateFullTradeInfo(con, tradeIds, txOutput);
		}

		if ((num_found < 0) || (num_found > max_trades))
		{
			txOutput.setStatus(-1021);
			txOutput.setStatusMessage("(num_found < 0) || (num_found > max_trades)");
		}

		if (num_updated == 0)
		{
			txOutput.setStatus(1021);
			txOutput.setStatusMessage("(num_updated == 0)");
		}
		else if (num_updated != num_found)
		{
			txOutput.setStatus(-1022);
			txOutput.setStatusMessage("(num_updated != num_found)");
		}
	}

	private void executeFrame3(Connection con, TxTradeUpdateInput txInput, TxTradeUpdateOutput txOutput) throws JsonProcessingException
	{
		txOutput.frame_executed = 3;
		int num_found = 0;
		int num_updated = 0;

		List<Map<String, Object>> tradeInfo = con
				.createQuery(sql.getTradesFrame3())
				.addParameter("symbol", txInput.symbol)
				.addParameter("max_acct_id", txInput.max_acct_id)
				.addParameter("start_trade_dts", txInput.start_trade_dts)
				.addParameter("end_trade_dts", txInput.end_trade_dts)
				.addParameter("limit", max_trades)
				.executeAndFetchTable()
				.asList();

		num_found = tradeInfo.size();

		txOutput.trade_info = tradeInfo;

		List<Map<String, Object>> cashTrades = tradeInfo
				.stream()
				.filter(t -> ((Boolean) t.get("t_is_cash")).booleanValue() == true)
				.map(
						m -> m
								.entrySet()
								.stream()
								.filter(entry -> keysToKeep.contains(entry.getKey()))
								.collect(
										Collectors
												.toMap(
														Map.Entry::getKey,
														Map.Entry::getValue)))

				.collect(Collectors.toList());

		if (cashTrades.size() > 0)
		{

			String cashTradeJson = json.writeValueAsString(cashTrades);

			con
					.createQuery(sql.updateTradesFrame3())
					.addParameter("cash_trades", cashTradeJson)
					.executeUpdate();

			num_updated = con.getResult();
		}

		if (tradeInfo.size() > 0)
		{
			List<Long> tradeIds = tradeInfo
					.stream()
					.map(t -> (Long) t.get("trade_id"))
					.collect(Collectors.toList());

			populateFullTradeInfo(con, tradeIds, txOutput);
		}

		if ((num_found < 0) || (num_found > max_trades))
		{
			txOutput.setStatus(-1032);
			txOutput.setStatusMessage("(num_found < 0) || (num_found > max_trades)");
		}
		if (num_updated == 0)
		{
			txOutput.setStatus(1032);
		}
		else if (num_updated > num_found)
		{
			txOutput.setStatus(-1032);
			txOutput.setStatusMessage("(num_updated > num_found)");
		}
	}

	/**
	 * A helper method to retrieve all related information for a given trade_id.
	 */
	private void populateFullTradeInfo(final Connection con, List<Long> tradeIds, final TxTradeUpdateOutput txOutput)
	{
		String tradeLst;
		String tradesCsv = getCsv(tradeIds);

		if (sqlCtx.getSqlEngine() == SqlEngine.MARIADB)
		{
			tradeLst = String.format("[%s]", tradesCsv);
		}
		else
		{
			tradeLst = tradesCsv;
		}

		if (txOutput.trade_info == null)
		{
			List<Map<String, Object>> tradeInfo = con
					.createQuery(sql.getTradeInfo())
					.addParameter("trade_lst", tradeLst)
					.executeAndFetchTable()
					.asList();
			txOutput.trade_info = tradeInfo;
		}

		List<Map<String, Object>> settlementInfo = con
				.createQuery(sql.getSettlementInfo())
				.addParameter("trade_lst", tradeLst)
				.executeAndFetchTable()
				.asList();

		txOutput.settlement_info = settlementInfo;

		List<Map<String, Object>> history = con
				.createQuery(sql.getTradeHistory())
				.addParameter("trade_lst", tradeLst)
				.executeAndFetchTable()
				.asList();

		txOutput.history = history;

		// collect trade_id values where is_cash is true
		String isCashTradeLst;
		String isCashTradeCsv = txOutput.trade_info
				.stream()
				.filter(t -> ((Boolean) t.get("t_is_cash")).booleanValue() == true)
				.map(t -> ((Long) t.get("trade_id")).toString())
				.collect(Collectors.joining(","));

		if (sqlCtx.getSqlEngine() == SqlEngine.MARIADB)
		{
			isCashTradeLst = String.format("[%s]", isCashTradeCsv);
		}
		else
		{
			isCashTradeLst = isCashTradeCsv;
		}

		if (StringUtils.isNotBlank(isCashTradeCsv))
		{
			List<Map<String, Object>> cashTransactionInfo = con
					.createQuery(sql.getCashTransactionInfo())
					.addParameter("trade_lst", isCashTradeLst)
					.executeAndFetchTable()
					.asList();

			txOutput.cash_transaction_info = cashTransactionInfo;
		}
	}

	private String getCsv(List<Long> tradeIds)
	{
		String tradesCsv = tradeIds
				.stream()
				.map(t -> Long.valueOf(t).toString())
				.collect(Collectors.joining(","));
		return tradesCsv;
	}
}