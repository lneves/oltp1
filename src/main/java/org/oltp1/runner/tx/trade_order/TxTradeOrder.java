package org.oltp1.runner.tx.trade_order;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;
import org.oltp1.common.ErrorCtx;
import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.generator.TxInputGenerator;
import org.oltp1.runner.model.Ticker;
import org.oltp1.runner.model.TradeStatus;
import org.oltp1.runner.model.TradeType;
import org.oltp1.runner.tx.QueryFactory;
import org.oltp1.runner.tx.market_feed.TxMarketFeed;
import org.oltp1.runner.tx.market_feed.TxMarketFeedInput;
import org.oltp1.runner.tx.trade_result.TxTradeResult;
import org.oltp1.runner.perf.TxBase;
import org.oltp1.runner.perf.TxOutput;
import org.oltp1.runner.perf.TxStatsCollector;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import org.sql2o.data.Row;

public class TxTradeOrder extends TxBase
{
	private static final String MEE_ACTION_PROCESS_ORDER = "eMEEProcessOrder";
	private static final String MEE_ACTION_SET_LIMIT_ORDER_TRIGGER = "eMEESetLimitOrderTrigger";

	private static final int max_feed_len = 20;

	private final Sql2o sql2o;

	private final TxInputGenerator txInputGen;
	private final TradeOrderQueries sql;

	private final TxTradeResult txTradeResult;
	private final TxMarketFeed txMarketFeed;

	private final Set<Ticker> tickers = new HashSet<Ticker>();

	private final ExecutorService mee;

	private final EnumSet<TradeType> isLimit = EnumSet
			.of(
					TradeType.LIMIT_SELL,
					TradeType.LIMIT_BUY,
					TradeType.STOP_LOSS);

	public TxTradeOrder(TxInputGenerator txInputGen, SqlContext sqlCtx, TxStatsCollector tradeResultStats, TxStatsCollector mktFeedStats, ExecutorService mee)
	{
		super(new TxStatsCollector("Trade-Order"));

		this.txInputGen = txInputGen;

		this.txTradeResult = new TxTradeResult(sqlCtx, tradeResultStats);
		this.txMarketFeed = new TxMarketFeed(sqlCtx, mktFeedStats);
		this.sql = QueryFactory.getQueries(TradeOrderQueries.class, sqlCtx.getSqlEngine());
		this.mee = mee;

		sql2o = sqlCtx.getSql2o();
	}

	@Override
	protected final TxOutput run()
	{
		TxTradeOrderOutput txOutput = new TxTradeOrderOutput();

		final TxTradeOrderInput txInput = txInputGen.generateTradeOrderInput();
		final TradeOrderSession session = new TradeOrderSession();

		session.put("trade_qty", txInput.trade_qty);

		try (Connection con = sql2o.beginTransaction())
		{
			executeFrame1(con, txInput, txOutput, session);

			if (txOutput.getStatus() < 0)
			{
				return txOutput;
			}

			if (notEquals(txInput.exec_l_name, session.getAsString("cust_l_name"))
					|| notEquals(txInput.exec_f_name, session.getAsString("cust_f_name"))
					|| notEquals(txInput.exec_tax_id, session.getAsString("tax_id")))
			{
				executeFrame2(con, txInput, txOutput, session);

				if (txOutput.getStatus() < 0)
				{
					return txOutput;
				}
			}

			executeFrame3(con, txInput, txOutput, session);

			double commRate = session.getAsDouble("comm_rate");
			long tradeQty = txInput.trade_qty;
			double requestedPrice = txInput.requested_price;

			double commAmount = (commRate / 100) * tradeQty * requestedPrice;
			String execName = StringUtils.trim(txInput.exec_f_name + " " + txInput.exec_l_name);
			boolean isCash = !txInput.type_is_margin;

			session.put("comm_amount", commAmount);
			session.put("exec_name", execName);
			session.put("is_cash", isCash);

			executeFrame4(con, txInput, txOutput, session);

			if (txInput.roll_it_back)
			{
				executeFrame5(con);
				txOutput.output = Collections.singletonMap("is_rollback", true);
				txOutput.is_rollback = true;
			}
			else
			{
				executeFrame6(con, txInput, txOutput, session);

				Runnable tradeResultAction = new Runnable()
				{
					public void run()
					{
						Map<String, Object> meeMsg = new HashMap<>();

						meeMsg.put("requested_price", session.get("market_price"));
						meeMsg.put("symbol", session.get("symbol"));
						meeMsg.put("trade_id", session.get("t_id"));
						meeMsg.put("trade_qty", session.get("trade_qty"));
						meeMsg.put("trade_type_id", txInput.trade_type.id);

						if (session.getAsBoolean("type_is_market"))
						{
							meeMsg.put("eAction", MEE_ACTION_PROCESS_ORDER);
						}
						else
						{
							meeMsg.put("eAction", MEE_ACTION_SET_LIMIT_ORDER_TRIGGER);
						}

						txTradeResult.offer(meeMsg);
					}
				};

				mee.execute(tradeResultAction);

				Runnable mktFeedAction = new Runnable()
				{
					public void run()
					{
						String t_symbol = session.getAsString("symbol");
						long t_trade_qty = session.getAsLong("trade_qty");
						double t_trade_price = session.getAsDouble("market_price");

						synchronized (tickers)
						{
							tickers.add(new Ticker(t_symbol, t_trade_price, t_trade_qty));

							if (tickers.size() >= Math.min(10, max_feed_len))
							{
								TxMarketFeedInput txMktFeedIn = new TxMarketFeedInput(
										TradeStatus.SUBMITTED,
										TradeType.LIMIT_BUY,
										TradeType.LIMIT_SELL,
										TradeType.STOP_LOSS,
										tickers,
										tickers.size());
								txMarketFeed.offer(txMktFeedIn);
								tickers.clear();
							}
						}
					}
				};

				mee.execute(mktFeedAction);
			}
		}
		catch (Throwable t)
		{
			ErrorCtx ectx = new ErrorCtx(t);
			txOutput.setStatus(-1);
			txOutput.setStatusMessage(ectx.toString());
		}

		return txOutput;
	}

	private void executeFrame1(final Connection con, final TxTradeOrderInput txInput, final TxTradeOrderOutput txOutput, final TradeOrderSession session)
	{
		Map<String, Object> customerInfo = con
				.createQuery(sql.getCustomerInfo())
				.addParameter("acct_id", txInput.acct_id)
				.executeAndFetchTable()
				.asList()
				.stream()
				.findFirst()
				.orElse(Collections.emptyMap());

		txOutput.num_found = customerInfo.size() > 0 ? 1 : 0;

		if (txOutput.num_found != 1)
		{
			txOutput.setStatus(-711);
			txOutput.setStatusMessage("(num_found != 1)");
		}

		session.putAll(customerInfo);
	}

	private void executeFrame2(final Connection con, final TxTradeOrderInput txInput, final TxTradeOrderOutput txOutput, final TradeOrderSession session)
	{
		String acl = con
				.createQuery(sql.getAccountPermissions())
				.addParameter("acct_id", txInput.acct_id)
				.addParameter("exec_l_name", txInput.exec_l_name)
				.addParameter("exec_f_name", txInput.exec_f_name)
				.addParameter("exec_tax_id", txInput.exec_tax_id)
				.executeScalar(String.class);

		if (StringUtils.isBlank(acl))
		{
			txOutput.setStatus(-721);
			txOutput.setStatusMessage("ap_acl is blank");
		}
		else
		{
			session.put("ap_acl", acl);
		}
	}

	private void executeFrame3(final Connection con, final TxTradeOrderInput txInput, final TxTradeOrderOutput txOutput, final TradeOrderSession session)
	{
		Map<String, Object> securityInfo;

		// Get information on the security
		if (StringUtils.isBlank(txInput.symbol))
		{
			securityInfo = con
					.createQuery(sql.getSecurityInfoByCoName())
					.addParameter("co_name", txInput.co_name)
					.addParameter("issue", txInput.issue)
					.executeAndFetchTable()
					.asList()
					.stream()
					.findFirst()
					.orElse(Collections.emptyMap());
		}
		else
		{
			securityInfo = con
					.createQuery(sql.getSecurityInfoBySymbol())
					.addParameter("symbol", txInput.symbol)
					.executeAndFetchTable()
					.asList()
					.stream()
					.findFirst()
					.orElse(Collections.emptyMap());
		}

		session.putAll(securityInfo);

		// Get current pricing information for the security
		double marketPrice = con
				.createQuery(sql.getLastTrade())
				.addParameter("symbol", securityInfo.get("symbol"))
				.executeScalar(Double.class);

		// Set trade characteristics based on the type of trade.
		Row tt = con
				.createQuery(sql.getTradeType())
				.addParameter("trade_type_id", txInput.trade_type.id)
				.executeAndFetchTable()
				.rows()
				.get(0);

		boolean isMarket = tt.getBoolean("type_is_market");
		boolean isSell = tt.getBoolean("type_is_sell");

		// If this is a limit-order, then the requestedPrice was passed in to the frame,
		// but if this a market-order, then the requestedPrice needs to be set to the
		// current market price.
		if (isMarket)
		{
			txInput.requested_price = marketPrice;
		}

		// Local frame variables used when estimating impact of this trade on
		// any current holdings of the same security.
		final MutableDouble buyValue = new MutableDouble();
		final MutableDouble sellValue = new MutableDouble();
		final MutableDouble requestedPrice = new MutableDouble(txInput.requested_price);
		final MutableInt neededQty = new MutableInt(txInput.trade_qty);

		int hsQty = con
				.createQuery(sql.getHoldingSummary())
				.addParameter("acct_id", txInput.acct_id)
				.addParameter("symbol", securityInfo.get("symbol"))
				.executeAndFetch(Integer.class)
				.stream()
				.findFirst()
				.orElse(0);

		if (isSell)
		{
			// This is a sell transaction, so estimate the impact to any currently held
			// long positions in the security.
			if (hsQty > 0)
			{
				// Estimate, based on the requested price, any profit that may be realized
				// by selling current holdings for this security. The customer may have
				// multiple holdings at different prices for this security (representing
				// multiple purchases different times).
				List<Row> holdingList = getHoldingList(con, txInput);

				holdingList.forEach(r -> {

					if (neededQty.intValue() != 0)
					{
						int holdQty = r.getInteger("h_qty");
						double holdPrice = r.getBigDecimal("h_price").doubleValue();

						if (holdQty > neededQty.intValue())
						{
							// Only a portion of this holding would be sold as a result of the trade
							buyValue.add(neededQty.doubleValue() * holdPrice);
							sellValue.add(neededQty.doubleValue() * requestedPrice.doubleValue());
							neededQty.setValue(0);
						}
						else
						{
							// All of this holding would be sold as a result of this trade.
							buyValue.add(holdQty * holdPrice);
							sellValue.add(holdQty * requestedPrice.doubleValue());
							neededQty.subtract(holdQty);
						}
					}
				});

				// NOTE: If needed_qty is still greater than 0 at this point, then the
				// customer would be liquidating all current holdings for this security, and
				// then creating a new short position for the remaining balance of
				// this transaction.
			}
		}
		else
		{
			// This is a buy transaction, so estimate the impact to any currently held
			// short positions in the security. These are represented as negative H_QTY
			// holdings. Short positions will be covered before opening a long postion in
			// this security.

			if (hsQty < 0) // Existing short position to buy
			{
				// Estimate, based on the requested price, any profit that may be realized
				// by covering short positions currently held for this security. The customer
				// may have multiple holdings at different prices for this security
				// (representing multiple purchases at different times).
				List<Row> holdingList = getHoldingList(con, txInput);

				holdingList.forEach(r -> {

					if (neededQty.intValue() != 0)
					{
						int holdQty = r.getInteger("h_qty");
						double holdPrice = r.getBigDecimal("h_price").doubleValue();

						if (holdQty + neededQty.intValue() < 0)
						{
							// Only a portion of this holding would be covered (bought back) as
							// a result of this trade.
							buyValue.add(neededQty.doubleValue() * requestedPrice.doubleValue());
							sellValue.add(neededQty.doubleValue() * holdPrice);
							neededQty.setValue(0);
						}
						else
						{
							// All of this holding would be covered (bought back) as
							// a result of this trade.
							holdQty = holdQty * -1;
							buyValue.add(holdQty * requestedPrice.doubleValue());
							sellValue.add(holdQty * holdPrice);
							neededQty.subtract(holdQty);
						}
					}
				});

				// NOTE: If needed_qty is still greater than 0 at this point, then the
				// customer would cover all current short positions (if any) for this security,
				// and then open a new long position for the remaining balance
				// of this transaction.
			}
		}

		// Estimate any capital gains tax that would be incurred as a result of this
		// transaction.

		int taxStatus = session.getAsInt("tax_status");

		double taxAmount = 0.0;
		if ((sellValue.doubleValue() > buyValue.doubleValue()) && (taxStatus == 1 || taxStatus == 2))
		{
			double taxRate = con
					.createQuery(sql.getTaxRate())
					.addParameter("cust_id", session.getAsLong("cust_id"))
					.executeScalar(Double.class);

			taxAmount = (sellValue.doubleValue() - buyValue.doubleValue()) * taxRate;
		}

		// Get administrative fees (e.g. trading charge, commision rate)
		Row frow = con
				.createQuery(sql.getFees())
				.addParameter("cust_tier", session.get("cust_tier"))
				.addParameter("trade_type_id", txInput.trade_type.id)
				.addParameter("exch_id", securityInfo.get("exch_id"))
				.addParameter("f_trade_qty", txInput.trade_qty)
				.addParameter("t_trade_qty", txInput.trade_qty)
				.executeAndFetchTable()
				.rows()
				.get(0);

		double commissionRate = frow.getBigDecimal("comm_rate").doubleValue();
		double chargeAmount = frow.getBigDecimal("charge_amount").doubleValue();

		// Compute assets on margin trades
		double accountAssets = 0.0;
		if (txInput.type_is_margin)
		{
			accountAssets = con
					.createQuery(sql.getCustomerAssets())
					.addParameter("acct_id", txInput.acct_id)
					.executeScalar(Double.class);
		}

		String statusId = isMarket ? txInput.st_submitted_id : txInput.st_pending_id;

		session.put("co_name", StringUtils.trimToEmpty(txInput.co_name));
		session.put("requested_price", txInput.requested_price);
		session.put("buy_value", buyValue.doubleValue());
		session.put("charge_amount", chargeAmount);
		session.put("comm_rate", commissionRate);
		session.put("acct_assets", accountAssets);
		session.put("market_price", marketPrice);
		session.put("sell_value", sellValue.doubleValue());
		session.put("status_id", statusId);
		session.put("tax_amount", taxAmount);
		session.put("type_is_market", isMarket);
		session.put("type_is_sell", isSell);

		double buy_value = buyValue.doubleValue();
		double sell_value = buyValue.doubleValue();

		if ((sell_value > buy_value) &&
				((taxStatus == 1) || (taxStatus == 2)) &&
				(taxAmount <= 0.00))
		{
			txOutput.setStatus(-731);
			txOutput.setStatusMessage("(chargeAmount == 0.00)");
		}
		else if (commissionRate <= 0.0000)
		{
			txOutput.setStatus(-732);
			txOutput.setStatusMessage("(commissionRate <= 0.0000)");
		}
		else if (chargeAmount == 0.00)
		{
			txOutput.setStatus(-733);
			txOutput.setStatusMessage("(chargeAmount == 0.00)");
		}

	}

	private void executeFrame4(final Connection con, final TxTradeOrderInput txInput, final TxTradeOrderOutput txOutput, final TradeOrderSession session)
	{
		long tid = ((Number) con
				.createQuery(sql.insertTrade(), true)
				.addParameter("trade_dts", LocalDateTime.now())
				.addParameter("status_id", session.get("status_id"))
				.addParameter("trade_type_id", txInput.trade_type.id)
				.addParameter("is_cash", session.get("is_cash"))
				.addParameter("symbol", session.get("symbol"))
				.addParameter("trade_qty", txInput.trade_qty)
				.addParameter("requested_price", txInput.requested_price)
				.addParameter("acct_id", txInput.acct_id)
				.addParameter("exec_name", session.get("exec_name"))
				.addParameter("charge_amount", session.get("charge_amount"))
				.addParameter("comm_amount", session.get("comm_amount"))
				.addParameter("is_lifo", txInput.is_lifo)
				.executeUpdate()
				.getKey()).longValue();

		// boolean isMarket = session.getAsBoolean("type_is_market");
		// if (!isMarket)
		TradeType tradeType = txInput.trade_type;
		if (isLimit.contains(tradeType))
		{
			con
					.createQuery(sql.insertTradeRequest())
					.addParameter("t_id", tid)
					.addParameter("trade_type_id", txInput.trade_type.id)
					.addParameter("symbol", session.get("symbol"))
					.addParameter("trade_qty", txInput.trade_qty)
					.addParameter("requested_price", txInput.requested_price)
					.addParameter("broker_id", session.get("broker_id"))
					.executeUpdate();
		}

		con
				.createQuery(sql.insertTradeHistory())
				.addParameter("t_id", tid)
				.addParameter("trade_dts", LocalDateTime.now())
				.addParameter("status_id", session.get("status_id"))
				.executeUpdate();

		session.put("t_id", tid);
	}

	private void executeFrame5(final Connection con)
	{
		con.rollback();
		incrementRollBacks();
	}

	private void executeFrame6(final Connection con, final TxTradeOrderInput txInput, final TxTradeOrderOutput txOutput, final TradeOrderSession session)
	{
		con.commit();

		Map<String, Object> tradeOrder = new HashMap<>();
		tradeOrder.put("buy_value", session.get("buy_value"));
		tradeOrder.put("sell_value", session.get("sell_value"));
		session.put("status", session.get("status_id"));
		tradeOrder.put("tax_amount", session.get("tax_amount"));
		tradeOrder.put("trade_id", session.get("t_id"));

		txOutput.output = tradeOrder;
	}

	private List<Row> getHoldingList(final Connection con, final TxTradeOrderInput txInput)
	{
		String holdingQStmt;
		if (txInput.is_lifo)
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
				.addParameter("acct_id", txInput.acct_id)
				.addParameter("symbol", txInput.symbol)
				.executeAndFetchTable()
				.rows();
	}

	private static boolean notEquals(CharSequence a, CharSequence b)
	{
		return !StringUtils.equalsIgnoreCase(a, b);
	}

}