package org.oltp1.egen.model;

import java.io.IOException;
import java.util.StringJoiner;

import org.oltp1.egen.io.AppendableRow;
import org.oltp1.egen.util.DateTime;

public class HoldingRow implements AppendableRow
{
	public long H_T_ID;
	public long H_CA_ID;
	public String H_S_SYMB;
	public DateTime H_DTS;
	public double H_PRICE;
	public int H_QTY;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(String.valueOf(H_T_ID))
				.add(String.valueOf(H_CA_ID))
				.add(H_S_SYMB)
				.add(H_DTS.toFormattedString(12)) // YYYY-MM-DD HH:mm:ss.SSS
				.add(String.format("%.2f", H_PRICE))
				.add(String.valueOf(H_QTY))
				.toString();
	}
	
	@Override
	public void writeObject(Appendable out) throws IOException
	{
		write(out, H_T_ID);
		write(out, '|');
		write(out, H_CA_ID);
		write(out, '|');
		write(out, H_S_SYMB);
		write(out, '|');
		writeDateTime(out, H_DTS);
		write(out, '|');
		write(out, H_PRICE);
		write(out, '|');
		write(out, H_QTY);
	}
}