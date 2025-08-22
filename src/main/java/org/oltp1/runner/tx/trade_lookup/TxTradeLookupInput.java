package org.oltp1.runner.tx.trade_lookup;

import java.time.LocalDateTime;
import java.util.Arrays;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class TxTradeLookupInput
{
	public int frame_to_execute;
	public long[] trade_id;
	public long acct_id;
	public long max_acct_id;
	public int max_trades;
	public LocalDateTime start_trade_dts;
	public LocalDateTime end_trade_dts;
	public String symbol;

	public TxTradeLookupInput()
	{
		super();
	}

	public TxTradeLookupInput(int frameToexecute, long[] tradeId, long acctId, long maxAcctId, int maxTrades, LocalDateTime startTradeDts, LocalDateTime endTradeDts, String symbol)
	{
		super();
		this.frame_to_execute = frameToexecute;
		this.trade_id = tradeId;
		this.acct_id = acctId;
		this.max_acct_id = maxAcctId;
		this.max_trades = maxTrades;
		this.start_trade_dts = startTradeDts;
		this.end_trade_dts = endTradeDts;
		this.symbol = symbol;
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("frame_to_execute", frame_to_execute)
				.append("trade_id", Arrays.toString(trade_id))
				.append("acct_id", acct_id)
				.append("max_acct_id", max_acct_id)
				.append("max_trades", max_trades)
				.append("start_trade_dts", start_trade_dts)
				.append("end_trade_dts", end_trade_dts)
				.append("symbol", symbol)
				.toString();
	}
}