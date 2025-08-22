package org.oltp1.egen.model;

import java.io.IOException;
import java.util.StringJoiner;

import org.oltp1.egen.io.AppendableRow;
import org.oltp1.egen.util.DateTime;

public class TradeHistoryRow implements AppendableRow
{
	public long TH_T_ID;
	public DateTime TH_DTS;
	public String TH_ST_ID;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(String.valueOf(TH_T_ID))
				.add(TH_DTS.toFormattedString(12)) // YYYY-MM-DD HH:mm:ss.SSS
				.add(TH_ST_ID)
				.toString();
	}

	@Override
	public void writeObject(Appendable out) throws IOException
	{
		write(out, TH_T_ID);
		write(out, '|');
		writeDateTime(out, TH_DTS); // YYYY-MM-DD HH:mm:ss.SSS
		write(out, '|');
		write(out, TH_ST_ID);
	}
}