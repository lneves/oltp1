package org.oltp1.egen.model;

import java.io.IOException;
import java.util.StringJoiner;

import org.oltp1.egen.io.AppendableRow;
import org.oltp1.egen.util.DateTime;

public class SettlementRow implements AppendableRow
{
	public long SE_T_ID;
	public String SE_CASH_TYPE;
	public DateTime SE_CASH_DUE_DATE;
	public double SE_AMT;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(String.valueOf(SE_T_ID))
				.add(SE_CASH_TYPE)
				.add(SE_CASH_DUE_DATE.toFormattedString(10)) // YYYY-MM-DD
				.add(String.format("%.2f", SE_AMT))
				.toString();
	}
	
	@Override
	public void writeObject(Appendable out) throws IOException
	{
		write(out, SE_T_ID);
		write(out, '|');
		write(out, SE_CASH_TYPE);
		write(out, '|');
		writeDate(out, SE_CASH_DUE_DATE); // YYYY-MM-DD
		write(out, '|');
		write(out, SE_AMT);
	}
}