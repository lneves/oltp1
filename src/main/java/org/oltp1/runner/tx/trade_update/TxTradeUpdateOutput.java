package org.oltp1.runner.tx.trade_update;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.oltp1.runner.perf.TxOutput;

public class TxTradeUpdateOutput extends TxOutput
{
	public int frame_executed;

	public List<Map<String, Object>> trade_info;
	public List<Map<String, Object>> settlement_info;
	public List<Map<String, Object>> cash_transaction_info;
	public List<Map<String, Object>> history;

	public TxTradeUpdateOutput()
	{
		this(0);
	}

	public TxTradeUpdateOutput(int status)
	{
		super(status);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("frame_executed", frame_executed)
				.append("trade_info", trade_info)
				.append("settlement_info", settlement_info)
				.append("cash_transaction_info", cash_transaction_info)
				.append("history", history)
				.append("tx_status", getStatus())
				.append("tx_status_message", getStatusMessage())
				.toString();
	}
}