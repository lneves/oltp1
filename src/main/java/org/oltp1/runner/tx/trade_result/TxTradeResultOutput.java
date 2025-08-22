package org.oltp1.runner.tx.trade_result;

import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.oltp1.runner.perf.TxOutput;

public class TxTradeResultOutput extends TxOutput
{
	public Map<String, Object> output;

	public TxTradeResultOutput()
	{
		this(0);
	}

	public TxTradeResultOutput(int status)
	{
		super(status);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("output", output)
				.append("tx_status", getStatus())
				.append("tx_status_message", getStatusMessage())
				.toString();
	}
}