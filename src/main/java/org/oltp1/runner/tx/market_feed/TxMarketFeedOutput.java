package org.oltp1.runner.tx.market_feed;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.oltp1.runner.perf.TxOutput;

public class TxMarketFeedOutput extends TxOutput
{
	public int num_trades;
	public List<Map<String, Object>> trade_requests;

	public TxMarketFeedOutput()
	{
		super(0);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("num_trades", num_trades)
				.append("trade_requests", trade_requests)
				.append("tx_status", getStatus())
				.append("tx_status_message", getStatusMessage())
				.toString();
	}
}