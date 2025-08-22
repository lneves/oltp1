package org.oltp1.egen.model;

import java.io.IOException;
import java.util.StringJoiner;

import org.oltp1.egen.io.AppendableRow;
import org.oltp1.egen.util.DateTime;

public class DailyMarketRow implements AppendableRow
{
	public DateTime DM_DATE;
	public String DM_S_SYMB;
	public double DM_CLOSE;
	public double DM_HIGH;
	public double DM_LOW;
	public long DM_VOL;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(DM_DATE.toFormattedString(10)) // YYYY-MM-DD
				.add(DM_S_SYMB)
				.add(String.format("%.2f", DM_CLOSE))
				.add(String.format("%.2f", DM_HIGH))
				.add(String.format("%.2f", DM_LOW))
				.add(String.valueOf(DM_VOL))
				.toString();
	}

	@Override
	public void writeObject(Appendable out) throws IOException
	{
		writeDate(out, DM_DATE); // YYYY-MM-DD
		write(out, '|');
		write(out, DM_S_SYMB);
		write(out, '|');
		write(out, DM_CLOSE);
		write(out, '|');
		write(out, DM_HIGH);
		write(out, '|');
		write(out, DM_LOW);
		write(out, '|');
		write(out, DM_VOL);
	}
}