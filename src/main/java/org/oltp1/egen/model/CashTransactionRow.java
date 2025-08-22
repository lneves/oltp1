package org.oltp1.egen.model;

import java.io.IOException;
import java.util.StringJoiner;

import org.oltp1.egen.io.AppendableRow;
import org.oltp1.egen.util.DateTime;

public class CashTransactionRow implements AppendableRow
{
	public long CT_T_ID;
	public DateTime CT_DTS;
	public double CT_AMT;
	public String CT_NAME;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(String.valueOf(CT_T_ID))
				.add(CT_DTS.toFormattedString(12)) // YYYY-MM-DD HH:mm:ss.SSS
				.add(String.format("%.2f", CT_AMT))
				.add(CT_NAME)
				.toString();
	}
	
	@Override
	public void writeObject(Appendable out) throws IOException
	{
		write(out, CT_T_ID);
		write(out, '|');
		writeDateTime(out, CT_DTS);
		write(out, '|');
		write(out, CT_AMT);
		write(out, '|');
		write(out, CT_NAME);
	}
}