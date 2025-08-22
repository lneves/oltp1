package org.oltp1.runner.tx.trade_status;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class TxTradeStatusInput
{
	public long acct_id;

	public TxTradeStatusInput()
	{
		super();
	}

	public TxTradeStatusInput(long acctId)
	{
		super();
		this.acct_id = acctId;
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("acct_id", acct_id)
				.toString();
	}
}