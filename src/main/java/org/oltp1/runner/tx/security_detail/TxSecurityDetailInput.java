package org.oltp1.runner.tx.security_detail;

import java.time.LocalDate;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public record TxSecurityDetailInput(boolean accessLobFlag, int maxRowsToReturn, LocalDate startDay, String symbol)
{

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("access_lob_flag", accessLobFlag)
				.append("max_rows_to_return", maxRowsToReturn)
				.append("start_day", startDay)
				.append("symbol", symbol)
				.toString();
	}
}