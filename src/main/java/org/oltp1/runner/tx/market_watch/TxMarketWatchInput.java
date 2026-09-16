package org.oltp1.runner.tx.market_watch;

import java.time.LocalDate;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public record TxMarketWatchInput(long acctId, long cId, long startingCoId, long endingCoId, LocalDate startDay, String industryName)
{

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("acct_id", acctId)
				.append("c_id", cId)
				.append("starting_co_id", startingCoId)
				.append("ending_co_id", endingCoId)
				.append("start_day", startDay)
				.append("industry_name", industryName)
				.toString();
	}
}
