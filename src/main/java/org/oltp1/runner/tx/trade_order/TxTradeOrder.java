package org.oltp1.runner.tx.trade_order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;
import org.oltp1.common.ErrorCtx;
import org.oltp1.common.db.Scalars;
import org.oltp1.common.db.SqlContext;
import org.oltp1.runner.generator.TxInputGenerator;
import org.oltp1.runner.model.TradeType;
import org.oltp1.runner.runtime.BenchmarkMetrics;
import org.oltp1.runner.runtime.ExchangeEmulator;
import org.oltp1.runner.runtime.TransactionSpec;
import org.oltp1.runner.runtime.TxBase;
import org.oltp1.runner.runtime.TxOutput;
import org.oltp1.runner.runtime.TxStatsCollector;
import org.oltp1.runner.tx.QueryFactory;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import org.sql2o.data.Row;

public class TxTradeOrder extends TxBase
{
	private static final String MEE_ACTION_PROCESS_ORDER = "eMEEProcessOrder";
	private static final String MEE_ACTION_SET_LIMIT_ORDER_TRIGGER = "eMEESetLimitOrderTrigger";

	private final Sql2o sql2o;

	private final TxInputGenerator txInputGen;
	private final TradeOrderDialect sql;

	private final EnumSet<TradeType> isLimit = EnumSet
			.of(
					TradeType.LIMIT_SELL,
					TradeType.LIMIT_BUY,
					TradeType.STOP_LOSS);

	private final ExchangeEmulator mee;

	public TxTradeOrder(TxInputGenerator txInputGen, SqlContext sqlCtx, BenchmarkMetrics metrics, ExchangeEmulator mee)
	{
		this(
				txInputGen,
				sqlCtx.getSql2o(),
				QueryFactory.getQueries(TradeOrderDialect.class, sqlCtx.getSqlEngine()),
				metrics,
				mee);
	}

	/**
	 * Package-private seam so the frame logic can be exercised with a stub dialect and a mocked
	 * {@link Connection}, in the same shape as {@code TxTradeCleanup} and {@code TxDataMaintenance}.
	 * It also makes the SQL a parameter of the class rather than something it looks up itself.
	 */
	TxTradeOrder(TxInputGenerator txInputGen, Sql2o sql2o, TradeOrderDialect sql, BenchmarkMetrics metrics, ExchangeEmulator mee)
	{
		super(metrics, new TxStatsCollector(TransactionSpec.TRADE_ORDER));
		this.txInputGen = txInputGen;
		this.sql = sql;
		this.mee = mee;

		this.sql2o = sql2o;
	}

	@Override
	protected final TxOutput run()
	{
		final TxTradeOrderInput txInput = txInputGen.generateTradeOrderInput();

		boolean valCondition = txInput.acct_id > 0 && txInput.trade_qty > 0 && txInput.requested_price > 0 && txInput.trade_type != null;

		if (!valCondition)
		{
			throw new IllegalArgumentException("Invalid Trade-Order account, quantity, price or type");
		}

		TxTradeOrderOutput txOutput = new TxTradeOrderOutput();
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

			if (txOutput.getStatus() < 0)
			{
				con.rollback();
				return txOutput;
			}

			BigDecimal commRate = BigDecimal.valueOf(session.getAsDouble("comm_rate"));
			BigDecimal tradeQty = BigDecimal.valueOf(txInput.trade_qty);
			BigDecimal reqPrice = BigDecimal.valueOf(txInput.requested_price);

			BigDecimal commAmount = commRate
					.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
					.multiply(tradeQty)
					.multiply(reqPrice)
					.setScale(2, RoundingMode.HALF_UP);

			String execName = StringUtils.trim(txInput.exec_f_name + " " + txInput.exec_l_name);
			boolean isCash = !txInput.type_is_margin;

			session.put("comm_amount", commAmount.doubleValue());
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

				Map<String, Object> meeMsg = new HashMap<>();

				meeMsg.put("requested_price", session.getAsDouble("market_price"));
				meeMsg.put("symbol", session.getAsString("symbol"));
				meeMsg.put("trade_id", session.getAsLong("t_id"));
				meeMsg.put("trade_qty", txInput.trade_qty);
				meeMsg.put("trade_type_id", txInput.trade_type.id);

				boolean isMarket;
				String eAction;

				if (session.getAsBoolean("type_is_market"))
				{
					isMarket = true;
					eAction = MEE_ACTION_PROCESS_ORDER;
				}
				else
				{
					isMarket = false;
					eAction = MEE_ACTION_SET_LIMIT_ORDER_TRIGGER;
				}

				final TxTradeOrderResult tr = new TxTradeOrderResult(
						session.getAsDouble("market_price"),
						session.getAsString("symbol"),
						session.getAsLong("t_id"),
						txInput.trade_qty,
						txInput.trade_type.id,
						eAction,
						isMarket);

				try
				{
					mee.submitTradeOrderToMarket(tr);
				}
				catch (RejectedExecutionException e)
				{
					txOutput.setStatus(1);
					txOutput.setStatusMessage("MEE shut down; trade committed but not sent");
				}
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

		if (securityInfo.isEmpty())
		{
			// A missing reference row is a data condition, not a programming error. The next two
			// reads both key on the security's symbol, so say which key was missing rather than
			// failing on a null unboxing further down.
			String key = StringUtils.isBlank(txInput.symbol)
					? ("company '" + txInput.co_name + "' issue '" + txInput.issue + "'")
					: ("symbol '" + txInput.symbol + "'");

			txOutput.setStatus(-730);
			txOutput.setStatusMessage("no SECURITY row for " + key);

			return;
		}

		session.putAll(securityInfo);

		final String symbol = (String) securityInfo.get("symbol");

		// Get current pricing information for the security. A security with no LAST_TRADE row
		// cannot be priced, so it is a transaction-level error that names the symbol rather than
		// an NPE from unboxing a null Double.
		Double fetchedMarketPrice = Scalars.find(
				con,
				sql.getLastTrade(),
				Double.class,
				"symbol",
				symbol);

		if (fetchedMarketPrice == null)
		{
			txOutput.setStatus(-734);
			txOutput.setStatusMessage("no LAST_TRADE row for symbol '" + symbol + "'");

			return;
		}

		final double marketPrice = fetchedMarketPrice;

		// Set trade characteristics based on the type of trade.
		Row tt = Scalars.requireOne(
				con
						.createQuery(sql.getTradeType())
						.addParameter("trade_type_id", txInput.trade_type.id)
						.executeAndFetchTable()
						.rows(),
				"TRADE_TYPE row for id '" + txInput.trade_type.id + "'");

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
				.addParameter("symbol", symbol)
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
				List<Row> holdingList = getHoldingList(con, txInput, session);

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
				List<Row> holdingList = getHoldingList(con, txInput, session);

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
			long custId = session.getAsLong("cust_id");

			double taxRate = Scalars.requireDouble(
					con,
					sql.getTaxRate(),
					"tax rate for customer " + custId,
					"cust_id",
					custId);

			taxAmount = (sellValue.doubleValue() - buyValue.doubleValue()) * taxRate;
		}

		// Get administrative fees (e.g. trading charge, commission rate)
		Row frow = Scalars.requireOne(
				con
						.createQuery(sql.getFees())
						.addParameter("cust_tier", session.get("cust_tier"))
						.addParameter("trade_type_id", txInput.trade_type.id)
						.addParameter("exch_id", securityInfo.get("exch_id"))
						.addParameter("f_trade_qty", txInput.trade_qty)
						.addParameter("t_trade_qty", txInput.trade_qty)
						.executeAndFetchTable()
						.rows(),
				"fee row for customer tier '" + session.get("cust_tier") + "', trade type '"
						+ txInput.trade_type.id + "', exchange '" + securityInfo.get("exch_id") + "'");

		double commissionRate = frow.getBigDecimal("comm_rate").doubleValue();
		double chargeAmount = frow.getBigDecimal("charge_amount").doubleValue();

		// Compute assets on margin trades
		double accountAssets = 0.0;
		if (txInput.type_is_margin)
		{
			accountAssets = Scalars.requireDouble(
					con,
					sql.getCustomerAssets(),
					"customer account assets for account " + txInput.acct_id,
					"acct_id",
					txInput.acct_id);
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
		double sell_value = sellValue.doubleValue();

		if ((sell_value > buy_value) &&
				((taxStatus == 1) || (taxStatus == 2)) &&
				(taxAmount <= 0.00))
		{
			txOutput.setStatus(-731);
			txOutput.setStatusMessage("((tax_amount <= 0.00))");
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
				.addParameter("trade_dts", txInputGen.now())
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

		session.put("t_id", tid);

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
				.addParameter("trade_dts", txInputGen.now())
				.addParameter("status_id", session.get("status_id"))
				.executeUpdate();
	}

	private void executeFrame5(final Connection con)
	{
		con.rollback();
		incrementRollBacks();
	}

	private void executeFrame6(final Connection con, final TxTradeOrderInput txInput, final TxTradeOrderOutput txOutput, final TradeOrderSession session)
	{
		con.commit();
		txInputGen.recordNewTradeId(session.getAsLong("t_id"));

		Map<String, Object> tradeOrder = new HashMap<>();
		tradeOrder.put("buy_value", session.get("buy_value"));
		tradeOrder.put("sell_value", session.get("sell_value"));
		session.put("status", session.get("status_id"));
		tradeOrder.put("tax_amount", session.get("tax_amount"));
		tradeOrder.put("trade_id", session.get("t_id"));

		txOutput.output = tradeOrder;
	}

	private List<Row> getHoldingList(final Connection con, final TxTradeOrderInput txInput, final TradeOrderSession session)
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
				.addParameter("symbol", StringUtils.firstNonBlank(session.getAsString("symbol"), txInput.symbol))
				.executeAndFetchTable()
				.rows();
	}

	private static boolean notEquals(CharSequence a, CharSequence b)
	{
		return !Strings.CI.equals(a, b);
	}
}