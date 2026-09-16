package org.oltp1.runner.tx.customer_position;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public record TxCustomerPositionInput(int acctIdIdx, long custId, boolean getHistory, String taxId)
{

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("acct_id_idx", acctIdIdx)
				.append("cust_id", custId)
				.append("get_history", getHistory)
				.append("tax_id", taxId)
				.toString();
	}
}