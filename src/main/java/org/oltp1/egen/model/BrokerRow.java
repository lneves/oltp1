package org.oltp1.egen.model;

import java.io.IOException;
import java.util.StringJoiner;

import org.oltp1.egen.io.AppendableRow;

public class BrokerRow implements AppendableRow
{
	public long B_ID;
	public String B_ST_ID;
	public String B_NAME;
	public int B_NUM_TRADES;
	public double B_COMM_TOTAL;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(String.valueOf(B_ID))
				.add(B_ST_ID)
				.add(B_NAME)
				.add(String.valueOf(B_NUM_TRADES))
				.add(String.format("%.2f", B_COMM_TOTAL))
				.toString();
	}

	@Override
	public void writeObject(Appendable out) throws IOException
	{
		write(out, B_ID);
		write(out, '|');
		write(out, B_ST_ID);
		write(out, '|');
		write(out, B_NAME);
		write(out, '|');
		write(out, B_NUM_TRADES);
		write(out, '|');
		write(out, B_COMM_TOTAL);
	}
}