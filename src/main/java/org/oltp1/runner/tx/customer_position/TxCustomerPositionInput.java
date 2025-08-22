package org.oltp1.runner.tx.customer_position;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class TxCustomerPositionInput
{
	public int acct_id_idx;
	public long cust_id;
	public boolean get_history;
	public String tax_id;

	public TxCustomerPositionInput()
	{
		super();
	}

	public TxCustomerPositionInput(int acctIdIdx, long custId, boolean getHistory, String taxId)
	{
		super();
		this.acct_id_idx = acctIdIdx;
		this.cust_id = custId;
		this.get_history = getHistory;
		this.tax_id = taxId;
	}

	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("acct_id_idx", acct_id_idx)
				.append("cust_id", cust_id)
				.append("get_history", get_history)
				.append("tax_id", tax_id)
				.toString();
	}
}