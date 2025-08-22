package org.oltp1.runner.tx.market_watch;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.oltp1.runner.perf.TxOutput;

public class TxMarketWatchOutput extends TxOutput
{
	public double pct_change;

	public TxMarketWatchOutput()
	{
		this(0);
	}

	public TxMarketWatchOutput(int status)
	{
		super(status);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("pct_change", pct_change)
				.append("tx_status", getStatus())
				.append("tx_status_message", getStatusMessage())
				.toString();
	}
}