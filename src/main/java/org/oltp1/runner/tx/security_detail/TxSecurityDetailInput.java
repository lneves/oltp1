package org.oltp1.runner.tx.security_detail;

import java.time.LocalDate;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class TxSecurityDetailInput
{
	public boolean access_lob_flag;
	public int max_rows_to_return;
	public LocalDate start_day;
	public String symbol;

	public TxSecurityDetailInput()
	{
		super();
	}

	public TxSecurityDetailInput(boolean accessLobFlag, int maxRowsToReturn, LocalDate startDay, String symbol)
	{
		super();
		this.access_lob_flag = accessLobFlag;
		this.max_rows_to_return = maxRowsToReturn;
		this.start_day = startDay;
		this.symbol = symbol;
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("access_lob_flag", access_lob_flag)
				.append("max_rows_to_return", max_rows_to_return)
				.append("start_day", start_day)
				.append("symbol", symbol)
				.toString();
	}
}