package org.oltp1.egen.model;

import java.io.IOException;
import java.util.StringJoiner;

import org.oltp1.egen.io.AppendableRow;

public class HoldingSummaryRow implements AppendableRow
{
	public long HS_CA_ID;
	public String HS_S_SYMB;
	public int HS_QTY;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(String.valueOf(HS_CA_ID))
				.add(HS_S_SYMB)
				.add(String.valueOf(HS_QTY))
				.toString();
	}

	@Override
	public void writeObject(Appendable out) throws IOException
	{
		write(out, HS_CA_ID);
		write(out, '|');
		write(out, HS_S_SYMB);
		write(out, '|');
		write(out, HS_QTY);

	}
}