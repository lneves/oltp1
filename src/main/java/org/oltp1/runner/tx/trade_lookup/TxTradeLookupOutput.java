package org.oltp1.runner.tx.trade_lookup;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.oltp1.runner.runtime.TxOutput;

public class TxTradeLookupOutput extends TxOutput
{
	public List<Map<String, Object>> lst_trades_frm1;
	public List<Map<String, Object>> lst_trades_frm2;
	public List<Map<String, Object>> lst_trades_frm3;
	public List<Map<String, Object>> lst_trades_frm4;
	public List<Map<String, Object>> lst_trades_history;
	public int frame_executed;
	public int num_found; // Number of HOLDING_HISTORY rows returned (may be zero).
	public int num_trades_found;
	public Long trade_id;

	public TxTradeLookupOutput()
	{
		this(0);
	}

	public TxTradeLookupOutput(int status)
	{
		super(status);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("frame_executed", frame_executed)
				.append("lst_trades_frm1", lst_trades_frm1)
				.append("lst_trades_frm2", lst_trades_frm2)
				.append("lst_trades_frm3", lst_trades_frm3)
				.append("lst_trades_frm4", lst_trades_frm4)
				.append("lst_trades_history", lst_trades_history)
				.append("num_found", num_found)
				.append("num_trades_found", num_trades_found)
				.append("trade_id", trade_id)
				.append("tx_status", getStatus())
				.append("tx_status_message", getStatusMessage())
				.toString();
	}
}