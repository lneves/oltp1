package org.oltp1.runner.tx.customer_position;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.oltp1.runner.perf.TxOutput;

public class TxCustomerPositionOutput extends TxOutput
{
	public List<Map<String, Object>> customer_accounts;
	public int acct_len;
	public Map<String, Object> customer;
	public int hist_len;
	public List<Map<String, Object>> trade_history;

	public TxCustomerPositionOutput()
	{
		this(0);
	}

	public TxCustomerPositionOutput(int status)
	{
		super(status);
		customer_accounts = Collections.emptyList();
		trade_history = Collections.emptyList();
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("customer", customer)
				.append("customer_accounts", customer_accounts)
				.append("acct_len", acct_len)
				.append("trade_history", trade_history)
				.append("hist_len", hist_len)
				.append("tx_status", getStatus())
				.append("tx_status_message", getStatusMessage())
				.toString();
	}
}