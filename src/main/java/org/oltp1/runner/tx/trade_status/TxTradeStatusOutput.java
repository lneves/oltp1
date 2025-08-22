package org.oltp1.runner.tx.trade_status;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.oltp1.runner.perf.TxOutput;

public class TxTradeStatusOutput extends TxOutput
{
	public Map<String, Object> trade_name;
	public List<Map<String, Object>> trade_status;
	public int num_found;

	public TxTradeStatusOutput()
	{
		this(0);
	}

	public TxTradeStatusOutput(int status)
	{
		super(status);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("trade_name", trade_name)
				.append("trade_status", trade_status)
				.append("num_found", num_found)
				.append("tx_status", getStatus())
				.append("tx_status_message", getStatusMessage())
				.toString();
	}
}