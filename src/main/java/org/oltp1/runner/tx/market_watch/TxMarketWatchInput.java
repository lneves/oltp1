package org.oltp1.runner.tx.market_watch;

import java.time.LocalDate;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class TxMarketWatchInput
{
	public long acct_id;
	public long c_id;
	public long starting_co_id;
	public long ending_co_id;
	public LocalDate start_day;
	public String industry_name;

	public TxMarketWatchInput()
	{
		super();
	}

	public TxMarketWatchInput(long acctId, long cId, long endingCoId, long startingCoId, LocalDate startDay, String industryName)
	{
		super();
		this.acct_id = acctId;
		this.c_id = cId;
		this.starting_co_id = startingCoId;
		this.ending_co_id = endingCoId;
		this.start_day = startDay;
		this.industry_name = industryName;
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("acct_id", acct_id)
				.append("c_id", c_id)
				.append("starting_co_id", starting_co_id)
				.append("ending_co_id", ending_co_id)
				.append("start_day", start_day)
				.append("industry_name", industry_name)
				.toString();
	}
}
