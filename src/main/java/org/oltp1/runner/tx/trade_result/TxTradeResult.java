package org.oltp1.runner.tx.trade_result;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.oltp1.common.ErrorCtx;
import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.model.TradeStatus;
import org.oltp1.runner.tx.QueryFactory;
import org.oltp1.runner.perf.TxBase;
import org.oltp1.runner.perf.TxOutput;
import org.oltp1.runner.perf.TxStatsCollector;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import org.sql2o.data.Row;

public class TxTradeResult extends TxBase
{
	private final Queue<Map<String, Object>> mq = new ConcurrentLinkedQueue<>();

	private final Sql2o sql2o;
	private final TradeResultQueries sql;

	public TxTradeResult(SqlContext sqlCtx, TxStatsCollector stats)
	{
		super(stats);

		this.sql2o = sqlCtx.getSql2o();
		this.sql = QueryFactory.getQueries(TradeResultQueries.class, sqlCtx.getSqlEngine());
	}

	public void offer(Map<String, Object> meeMsg)
	{
		mq.offer(meeMsg);
		super.execute();
	}

	@Override
	protected final TxOutput run()
	{
		TxTradeResultOutput txOutput = new TxTradeResultOutput();

		Map<String, Object> txInput = mq.poll();

		if (txInput == null)
		{
			txOutput.setStatus(-1);
			txOutput.setStatusMessage("TxTradeResultInput should not be null");
			return txOutput;
		}

		final TradeResultSession session = new TradeResultSession();
		session.putAll(txInput);

		// try (Connection con = sql2o.beginTransaction(sqlCtx.getIsolationLevel()))
		try (Connection con = sql2o.beginTransaction())
		{
			executeFrame1(con, txOutput, session);

			if (txOutput.getStatus() < 0)
			{
				return txOutput;
			}

			executeFrame2(con, txOutput, session);

			int tax_status = session.getAsInt("tax_status");
			double sell_value = session.getAsDouble("sell_value");
			double buy_value = session.getAsDouble("buy_value");

			if ((tax_status == 1 || tax_status == 2) && (sell_value > buy_value))
			{
				executeFrame3(con, txOutput, session);

				if (session.getAsDouble("tax_amount") <= 0.00)
				{
					txOutput.setStatus(-831);
					txOutput.setStatusMessage("tax_amount <= 0.00");
					return txOutput;
				}
			}

			executeFrame4(con, txOutput, session);

			if (session.getAsDouble("comm_rate") <= 0)
			{
				txOutput.setStatus(-841);
				txOutput.setStatusMessage("comm_rate <= 0.00");
				return txOutput;
			}

			executeFrame5(con, txOutput, session);

			executeFrame6(con, txOutput, session);

			Map<String, Object> out = new HashMap<>();
			out.put("acct_id", session.get("acct_id"));
			out.put("acct_bal", session.get("acct_bal"));
			out.put("status", TradeStatus.COMPLETED.id);

			txOutput.output = out;
		}
		catch (Throwable t)
		{
			ErrorCtx ectx = new ErrorCtx(t);
			txOutput.setStatus(-1);
			txOutput.setStatusMessage(ectx.toString());
		}

		return txOutput;
	}

	private void executeFrame1(Connection con, TxOutput txOutput, TradeResultSession session)
	{
		List<Map<String, Object>> tradeInfo = con
				.createQuery(sql.getTradeInfo())
				.addParameter("trade_id", session.get("trade_id"))
				.executeAndFetchTable()
				.asList();

		if (tradeInfo.size() != 1)
		{
			txOutput.setStatus(-811);
		}
		else
		{
			session.put("num_found", tradeInfo.size());
			session.putAll(tradeInfo.get(0));
		}
	}

	private void executeFrame2(Connection con, TxOutput txOutput, TradeResultSession session)
	{
		Map<String, Object> customerAccount = con
				.createQuery(sql.getCustomerAccount())
				.addParameter("acct_id", session.get("acct_id"))
				.executeAndFetchTable()
				.asList()
				.stream()
				.findFirst()
				.orElse(Collections.emptyMap());

		session.putAll(customerAccount);

		if (session.getAsBoolean("type_is_sell"))
		{
			processSellTrade(con, session);
		}
		else
		{
			processBuyTrade(con, session);
		}
	}

	private void executeFrame3(Connection con, TxOutput txOutput, TradeResultSession session)
	{
		double taxRate = con
				.createQuery(sql.getTaxRate())
				.addParameter("cust_id", session.get("cust_id"))
				.executeScalar(Double.class);

		double capitalGain = session.getAsDouble("sell_value") - session.getAsDouble("buy_value");
		double taxAmount = capitalGain > 0 ? capitalGain * taxRate : 0;

		con
				.createQuery(sql.updateTradeTax())
				.addParameter("trade_id", session.get("trade_id"))
				.addParameter("tax_amount", taxAmount)
				.executeUpdate();

		session.put("tax_amount", taxAmount);
	}

	private void executeFrame4(Connection con, TxOutput txOutput, TradeResultSession session)
	{
		Map<String, Object> cmr = con
				.createQuery(sql.getCommissionRate())
				.addParameter("cust_id", session.get("cust_id"))
				.addParameter("symbol", session.get("symbol"))
				.addParameter("trade_qty", session.get("trade_qty"))
				.addParameter("type_id", session.get("type_id"))
				.executeAndFetchTable()
				.asList()
				.stream()
				.findFirst()
				.orElse(Collections.emptyMap());

		session.putAll(cmr);
	}

	private void executeFrame5(Connection con, TxOutput txOutput, TradeResultSession session)
	{
		double commAmount = (session.getAsDouble("comm_rate") / 100) * (session.getAsInt("trade_qty") * session.getAsDouble("requested_price"));

		con
				.createQuery(sql.updateTrade())
				.addParameter("comm_amount", commAmount)
				.addParameter("trade_dts", session.get("trade_dts"))
				.addParameter("st_completed_id", TradeStatus.COMPLETED.id)
				.addParameter("trade_price", session.get("requested_price"))
				.addParameter("trade_id", session.get("trade_id"))
				.executeUpdate();

		con
				.createQuery(sql.insertTradeHistory())
				.addParameter("trade_id", session.get("trade_id"))
				.addParameter("trade_dts", session.get("trade_dts"))
				.addParameter("st_completed_id", TradeStatus.COMPLETED.id)
				.executeUpdate();

		con
				.createQuery(sql.updateBroker())
				.addParameter("comm_amount", commAmount)
				.addParameter("broker_id", session.get("broker_id"))
				.executeUpdate();

		session.put("comm_amount", commAmount);
	}

	private void executeFrame6(Connection con, TxOutput txOutput, TradeResultSession session)
	{
		LocalDateTime dueDate = ((LocalDateTime) session.get("trade_dts")).plus(2, ChronoUnit.DAYS);

		boolean tradeIsCash = session.getAsBoolean("trade_is_cash");
		boolean tradeIsSell = session.getAsBoolean("type_is_sell");
		double tradePrice = session.getAsDouble("requested_price");
		int tradeQty = session.getAsInt("trade_qty");
		double charge = session.getAsDouble("charge");

		double taxAmount = session.get("tax_amount") == null ? 0.0 : session.getAsDouble("tax_amount");
		double commAmount = session.getAsDouble("comm_amount");
		int taxStatus = session.getAsInt("tax_status");

		double seAmount;

		if (tradeIsSell)
		{
			seAmount = (tradeQty * tradePrice) - charge - commAmount;
		}
		else
		{
			seAmount = -((tradeQty * tradePrice) + charge + commAmount);
		}

		if (taxStatus == 1)
		{
			seAmount = seAmount - taxAmount;
		}

		String cashType = tradeIsCash ? "Cash Account" : "Margin";

		con
				.createQuery(sql.insertSettlement())
				.addParameter("trade_id", session.get("trade_id"))
				.addParameter("cash_type", cashType)
				.addParameter("due_date", dueDate)
				.addParameter("se_amount", seAmount)
				.executeUpdate();

		if (tradeIsCash)
		{
			con
					.createQuery(sql.updateCustomerAccount())
					.addParameter("acct_id", session.get("acct_id"))
					.addParameter("se_amount", seAmount)
					.executeUpdate();

			con
					.createQuery(sql.insertCashTransaction())
					.addParameter("trade_id", session.get("trade_id"))
					.addParameter("trade_dts", session.get("trade_dts"))
					.addParameter("se_amount", seAmount)
					.addParameter("ct_name", String.format("%s %d shares of %s", session.getAsString("type_name"), tradeQty, session.getAsString("s_name")))
					.executeUpdate();
		}

		double balance = con
				.createQuery(sql.getAccountBalance())
				.addParameter("acct_id", session.get("acct_id"))
				.executeScalar(BigDecimal.class)
				.doubleValue();

		session.put("acct_bal", balance);
	}

	private void processSellTrade(Connection con, TradeResultSession session)
	{
		double buyValue = 0;
		double sellValue = 0;

		int neededQty = session.getAsInt("trade_qty");
		int hsQty = session.getAsInt("hs_qty");
		double tradePrice = session.getAsDouble("requested_price");
		int tradeQty = session.getAsInt("trade_qty");
		LocalDateTime tradeDts = LocalDateTime.now();

		if (hsQty == 0)
		{
			con
					.createQuery(sql.insertHoldingSummary())
					.addParameter("acct_id", session.get("acct_id"))
					.addParameter("symbol", session.get("symbol"))
					.addParameter("trade_qty", tradeQty)
					.executeUpdate();
		}
		else if (hsQty != session.getAsInt("trade_qty"))
		{
			con
					.createQuery(sql.updateHoldingSummary())
					.addParameter("hs_qty", session.get("hs_qty"))
					.addParameter("acct_id", session.get("acct_id"))
					.addParameter("symbol", session.get("symbol"))
					.addParameter("trade_qty", tradeQty)
					.executeUpdate();
		}

		if (hsQty > 0)
		{
			List<Row> holdingList = populateHoldingList(con, session);
			for (Row holdingItem : holdingList)
			{
				if (neededQty <= 0)
				{
					break;
				}

				long holdId = holdingItem.getLong("hold_id");
				int holdQty = holdingItem.getInteger("hold_qty");
				double holdPrice = holdingItem.getBigDecimal("hold_price").doubleValue();

				if (holdQty > neededQty)
				{
					con
							.createQuery(sql.insertHoldingHistory())
							.addParameter("hold_id", holdId)
							.addParameter("trade_id", session.get("trade_id"))
							.addParameter("hold_qty", holdQty)
							.addParameter("after_qty", holdQty - neededQty)
							.executeUpdate();

					con
							.createQuery(sql.updateHolding())
							.addParameter("qty", holdQty - neededQty)
							.addParameter("hold_id", holdId)
							.executeUpdate();

					buyValue += neededQty * holdPrice;
					sellValue += neededQty * tradePrice;
					neededQty = 0;
				}
				else
				{
					con
							.createQuery(sql.insertHoldingHistory())
							.addParameter("hold_id", holdId)
							.addParameter("trade_id", session.get("trade_id"))
							.addParameter("hold_qty", holdQty)
							.addParameter("after_qty", 0)
							.executeUpdate();

					con
							.createQuery(sql.deleteHolding())
							.addParameter("hold_id", holdId)
							.executeUpdate();

					buyValue += holdQty * holdPrice;
					sellValue += holdQty * tradePrice;
					neededQty = neededQty - holdQty;
				}
			}
		}

		// need to sell more? go short
		if (neededQty > 0)
		{
			con
					.createQuery(sql.insertHoldingHistory())
					.addParameter("hold_id", session.get("trade_id"))
					.addParameter("trade_id", session.get("trade_id"))
					.addParameter("hold_qty", 0)
					.addParameter("after_qty", -neededQty)
					.executeUpdate();

			con
					.createQuery(sql.insertHolding())
					.addParameter("trade_id", session.get("trade_id"))
					.addParameter("acct_id", session.get("acct_id"))
					.addParameter("symbol", session.get("symbol"))
					.addParameter("trade_dts", tradeDts)
					.addParameter("trade_price", tradePrice)
					.addParameter("qty", -neededQty)
					.executeUpdate();

		}
		else if (hsQty == tradeQty)
		{
			con
					.createQuery(sql.deleteHoldingSummary())
					.addParameter("acct_id", session.get("acct_id"))
					.addParameter("symbol", session.get("symbol"))
					.executeUpdate();
		}

		session.put("buy_value", buyValue);
		session.put("sell_value", sellValue);
		session.put("trade_dts", tradeDts);
	}

	private void processBuyTrade(Connection con, TradeResultSession session)
	{
		double buyValue = 0;
		double sellValue = 0;

		int neededQty = session.getAsInt("trade_qty");
		int hsQty = session.getAsInt("hs_qty");
		double tradePrice = session.getAsDouble("requested_price");
		int tradeQty = session.getAsInt("trade_qty");
		LocalDateTime tradeDts = LocalDateTime.now();

		if (hsQty == 0)
		{
			con
					.createQuery(sql.insertHoldingSummary())
					.addParameter("acct_id", session.get("acct_id"))
					.addParameter("symbol", session.get("symbol"))
					.addParameter("trade_qty", tradeQty)
					.executeUpdate();

		}
		else if (-hsQty != tradeQty)
		{
			con
					.createQuery(sql.updateHoldingSummary())
					.addParameter("hs_qty", session.get("hs_qty"))
					.addParameter("acct_id", session.get("acct_id"))
					.addParameter("symbol", session.get("symbol"))
					.addParameter("trade_qty", hsQty + tradeQty)
					.executeUpdate();

		}

		if (hsQty < 0)
		{
			List<Row> holdingList = populateHoldingList(con, session);
			for (Row holdingItem : holdingList)
			{
				if (neededQty <= 0)
				{
					break;
				}

				long holdId = holdingItem.getLong("hold_id");
				int holdQty = holdingItem.getInteger("hold_qty");
				double holdPrice = holdingItem.getBigDecimal("hold_price").doubleValue();

				if (holdQty + neededQty < 0)
				{
					con
							.createQuery(sql.insertHoldingHistory())
							.addParameter("hold_id", holdId)
							.addParameter("trade_id", session.get("trade_id"))
							.addParameter("hold_qty", holdQty)
							.addParameter("after_qty", holdQty + neededQty)
							.executeUpdate();

					con
							.createQuery(sql.updateHolding())
							.addParameter("qty", holdQty + neededQty)
							.addParameter("hold_id", holdId)
							.executeUpdate();

					sellValue += neededQty * holdPrice;
					buyValue += neededQty * tradePrice;
					neededQty = 0;
				}
				else
				{
					con
							.createQuery(sql.insertHoldingHistory())
							.addParameter("hold_id", holdId)
							.addParameter("trade_id", session.get("trade_id"))
							.addParameter("hold_qty", holdQty)
							.addParameter("after_qty", 0)
							.executeUpdate();

					con
							.createQuery(sql.deleteHolding())
							.addParameter("hold_id", holdId)
							.executeUpdate();

					holdQty = -holdQty;
					sellValue += holdQty * holdPrice;
					buyValue += holdQty * tradePrice;
					neededQty = neededQty - holdQty;
				}
			}
			// execute all updates from the above loop

		}

		// all shorts are covered? a new long is created
		if (neededQty > 0)
		{
			con
					.createQuery(sql.insertHoldingHistory())
					.addParameter("hold_id", session.get("trade_id"))
					.addParameter("trade_id", session.get("trade_id"))
					.addParameter("hold_qty", 0)
					.addParameter("after_qty", neededQty)
					.executeUpdate();

			con
					.createQuery(sql.insertHolding())
					.addParameter("trade_id", session.get("trade_id"))
					.addParameter("acct_id", session.get("acct_id"))
					.addParameter("symbol", session.get("symbol"))
					.addParameter("trade_dts", tradeDts)
					.addParameter("trade_price", tradePrice)
					.addParameter("qty", neededQty)
					.executeUpdate();

		}
		else if (-hsQty == tradeQty)
		{
			con
					.createQuery(sql.deleteHoldingSummary())
					.addParameter("acct_id", session.get("acct_id"))
					.addParameter("symbol", session.get("symbol"))
					.executeUpdate();
		}

		session.put("buy_value", buyValue);
		session.put("sell_value", sellValue);
		session.put("trade_dts", tradeDts);
	}

	private List<Row> populateHoldingList(final Connection con, final TradeResultSession session)
	{
		String holdingQStmt;
		if (session.getAsBoolean("is_lifo"))
		{
			// Estimates will be based on closing most recently acquired holdings
			// Could return 0, 1 or many rows
			holdingQStmt = sql.getHoldingDesc();
		}
		else
		{
			// Estimates will be based on closing oldest holdings
			// Could return 0, 1 or many rows
			holdingQStmt = sql.getHoldingAsc();
		}

		return con
				.createQuery(holdingQStmt)
				.addParameter("acct_id", session.get("acct_id"))
				.addParameter("symbol", session.get("symbol"))
				.executeAndFetchTable()
				.rows();

	}

}