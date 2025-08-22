package org.oltp1.runner.tx.trade_cleanup;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class TxTradeCleanupInput
{
	public final String st_canceled_id;
	public final String st_pending_id;
	public final String st_submitted_id;
	public final long start_trade_id;

	public TxTradeCleanupInput(String st_canceled_id, String st_pending_id, String st_submitted_id, long start_trade_id)
	{
		this.st_canceled_id = st_canceled_id;
		this.st_pending_id = st_pending_id;
		this.st_submitted_id = st_submitted_id;
		this.start_trade_id = start_trade_id;
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("start_trade_id", start_trade_id)
				.append("st_canceled_id", st_canceled_id)
				.append("st_pending_id", st_pending_id)
				.append("st_submitted_id", st_submitted_id)
				.toString();
	}
}