package org.caudexorigo.oltp1.tx.customer_position;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.caudexorigo.db.SqlContext;
import org.caudexorigo.oltp1.generator.TxInputGenerator;
import org.caudexorigo.perf.ErrorAnalyser;
import org.caudexorigo.perf.TxBase;
import org.caudexorigo.perf.TxOutput;
import org.caudexorigo.perf.TxStatsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

public class TxCustomerPosition extends TxBase
{
	private static final Logger log = LoggerFactory.getLogger(TxCustomerPosition.class);

	private static final int max_acct_len = 10;
	private static final int max_hist_len = 30;

	private final Sql2o sql2o;
	private final TxInputGenerator txInputGen;
	private final CustomerPositionQueries sql;

	public TxCustomerPosition(TxInputGenerator txInputGen, SqlContext sqlCtx)
	{
		super(new TxStatsCollector("Customer-Position"));
		this.txInputGen = txInputGen;
		this.sql2o = sqlCtx.getSql2o();
		this.sql = CustomerPositionQueriesFactory.getQueries(sqlCtx.getSqlEngine());
	}

	@Override
	protected final TxOutput run()
	{
		final TxCustomerPositionInput txInput = txInputGen.generateCustomerPositionInput();

		final TxCustomerPositionOutput txOutput = new TxCustomerPositionOutput();

		try (Connection con = sql2o.beginTransaction())
		{
			executeFrame1(con, txInput, txOutput);

			if (txOutput.getStatus() > -1)
			{
				if (txInput.get_history)
				{
					executeFrame2(con, txInput, txOutput);
				}
				else
				{
					executeFrame3(con);
				}
			}
			else
			{
				con.commit();
			}
		}
		catch (Throwable t)
		{
			Throwable r = ErrorAnalyser.findRootCause(t);
			log.error(r.getMessage(), r);
			txOutput.setStatus(-1);
			txOutput.setStatusMessage(r.getMessage());
		}

		return txOutput;
	}

	private void executeFrame1(final Connection con, final TxCustomerPositionInput frm1Input, final TxCustomerPositionOutput txOutput)
	{
		long customerId = getCustomerId(con, frm1Input);

		Map<String, Object> customer = con
				.createQuery(sql.getCustomerByCid())
				.addParameter("cust_id", customerId)
				.executeAndFetchTable()
				.asList()
				.stream()
				.findFirst()
				.orElse(null);

		List<Map<String, Object>> customerAccounts;

		if (customer != null)
		{
			txOutput.customer = customer;

			customerAccounts = con
					.createQuery(sql.getCustomerAccounts())
					.addParameter("cust_id", customer.get("cust_id"))
					.executeAndFetchTable()
					.asList();
		}
		else
		{
			customerAccounts = Collections.emptyList();
		}

		txOutput.customer_accounts = customerAccounts;
		txOutput.acct_len = customerAccounts.size();

		if ((txOutput.acct_len < 1) || (txOutput.acct_len > max_acct_len))
		{

			log.error("Frame1 input: \n{}", frm1Input.toString());
			txOutput.setStatus(-221);
			txOutput.setStatusMessage("(acct_len  < 1) || (acct_len  > max_acct_len)");
		}
	}

	private void executeFrame2(final Connection con, final TxCustomerPositionInput txInput, final TxCustomerPositionOutput txOutput)
	{
		Map<String, Object> customerAsset = txOutput.customer_accounts.get(txInput.acct_id_idx);

		List<Map<String, Object>> tradeHistory = con
				.createQuery(sql.getTradeHistory())
				.addParameter("acct_id", (Long) customerAsset.get("acct_id"))
				.executeAndFetchTable()
				.asList();

		txOutput.trade_history = tradeHistory;
		txOutput.hist_len = tradeHistory.size();

		if ((txOutput.hist_len < 10) || (txOutput.hist_len > max_hist_len))
		{
			txOutput.setStatus(-221);
			txOutput.setStatusMessage("(hist_len  < 10) || (hist_len  > max_hist_len)");
		}

		con.commit();
	}

	private void executeFrame3(final Connection con)
	{
		con.commit();
	}

	private long getCustomerId(final Connection con, final TxCustomerPositionInput frm1Input)
	{
		if (frm1Input.cust_id > 0)
		{
			return frm1Input.cust_id;
		}
		else if (StringUtils.isNotBlank(frm1Input.tax_id))
		{
			Long oCid = con
					.createQuery(sql.getCustomerByTaxid())
					.addParameter("tax_id", frm1Input.tax_id)
					.executeScalar(Long.class);

			return oCid != null ? oCid.longValue() : -1;
		}
		else
		{
			throw new IllegalStateException("An invalid TxCustomerPositionInput argument was generated");
		}
	}
}