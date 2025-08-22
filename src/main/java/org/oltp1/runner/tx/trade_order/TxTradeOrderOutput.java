package org.oltp1.runner.tx.trade_order;

import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.oltp1.runner.perf.TxOutput;

public class TxTradeOrderOutput extends TxOutput
{
	public Map<String, Object> output;
	public int num_found;
	public boolean is_rollback;

	public TxTradeOrderOutput()
	{
		this(0);
	}

	public TxTradeOrderOutput(int status)
	{
		super(status);
		is_rollback = false;
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("output", output)
				.append("is_rollback", is_rollback)
				.append("tx_status", getStatus())
				.append("tx_status_message", getStatusMessage())
				.toString();
	}
}