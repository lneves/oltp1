package org.oltp1.runner.tx.customer_position;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.oltp1.common.ErrorCtx;
import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.generator.TxInputGenerator;
import org.oltp1.runner.runtime.BenchmarkMetrics;
import org.oltp1.runner.runtime.TransactionSpec;
import org.oltp1.runner.runtime.TxBase;
import org.oltp1.runner.runtime.TxOutput;
import org.oltp1.runner.runtime.TxStatsCollector;
import org.oltp1.runner.tx.QueryFactory;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

public class TxCustomerPosition extends TxBase
{
	private static final int max_acct_len = 10;
	private static final int max_hist_len = 30;

	private final Sql2o sql2o;
	private final TxInputGenerator txInputGen;
	private final CustomerPositionDialect sql;

	public TxCustomerPosition(TxInputGenerator txInputGen, SqlContext sqlCtx, BenchmarkMetrics metrics)
	{
		super(metrics, new TxStatsCollector(TransactionSpec.CUSTOMER_POSITION));
		this.txInputGen = txInputGen;
		this.sql2o = sqlCtx.getSql2o();
		this.sql = QueryFactory.getQueries(CustomerPositionDialect.class, sqlCtx.getSqlEngine());
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
				if (txInput.getHistory())
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
			ErrorCtx lctx = new ErrorCtx(t);
			txOutput.setStatus(-1);
			txOutput.setStatusMessage(lctx.toString());
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

		if (customer != null)
		{
			txOutput.customer = customer;

			txOutput.customer_accounts = con
					.createQuery(sql.getCustomerAccounts())
					.addParameter("cust_id", customer.get("cust_id"))
					.executeAndFetchTable()
					.asList();
		}
		else
		{
			txOutput.customer_accounts = Collections.emptyList();
		}

		txOutput.acct_len = txOutput.customer_accounts.size();

		if ((txOutput.acct_len < 1) || (txOutput.acct_len > max_acct_len))
		{
			txOutput.setStatus(-211);
			txOutput.setStatusMessage("(acct_len  < 1) || (acct_len  > max_acct_len)");
		}
	}

	private void executeFrame2(final Connection con, final TxCustomerPositionInput txInput, final TxCustomerPositionOutput txOutput)
	{
		Map<String, Object> customerAsset = txOutput.customer_accounts.get(txInput.acctIdIdx());

		txOutput.trade_history = con
				.createQuery(sql.getTradeHistory())
				.addParameter("acct_id", (Long) customerAsset.get("acct_id"))
				.executeAndFetchTable()
				.asList();

		txOutput.hist_len = txOutput.trade_history.size();

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
		if (frm1Input.custId() > 0)
		{
			return frm1Input.custId();
		}
		else if (StringUtils.isNotBlank(frm1Input.taxId()))
		{
			Long oCid = con
					.createQuery(sql.getCustomerByTaxid())
					.addParameter("tax_id", frm1Input.taxId())
					.executeScalar(Long.class);

			return oCid != null ? oCid.longValue() : -1;
		}
		else
		{
			throw new IllegalStateException("An invalid TxCustomerPositionInput argument was generated");
		}
	}
}