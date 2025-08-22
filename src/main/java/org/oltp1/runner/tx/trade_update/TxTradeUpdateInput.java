package org.oltp1.runner.tx.trade_update;

import java.time.LocalDateTime;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class TxTradeUpdateInput
{
	public int frame_to_execute;
	public long[] trade_id;
	public long acct_id;
	public long max_acct_id;
	public int max_trades;
	public int max_updates;
	public LocalDateTime start_trade_dts;
	public LocalDateTime end_trade_dts;
	public String symbol;

	public TxTradeUpdateInput()
	{
		super();
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("frame_to_execute", frame_to_execute)
				.append("trade_id", trade_id)
				.append("acct_id", acct_id)
				.append("max_acct_id", max_acct_id)
				.append("max_trades", max_trades)
				.append("max_updates", max_updates)
				.append("start_trade_dts", start_trade_dts)
				.append("end_trade_dts", end_trade_dts)
				.append("symbol", symbol)
				.toString();
	}
}