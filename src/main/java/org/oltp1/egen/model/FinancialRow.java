package org.oltp1.egen.model;

import java.io.IOException;
import java.util.StringJoiner;

import org.oltp1.egen.io.AppendableRow;
import org.oltp1.egen.util.DateTime;

public class FinancialRow  implements AppendableRow
{
	public long FI_CO_ID;
	public int FI_YEAR;
	public int FI_QTR;
	public DateTime FI_QTR_START_DATE;
	public double FI_REVENUE;
	public double FI_NET_EARN;
	public double FI_BASIC_EPS;
	public double FI_DILUT_EPS;
	public double FI_MARGIN;
	public double FI_INVENTORY;
	public double FI_ASSETS;
	public double FI_LIABILITY;
	public long FI_OUT_BASIC;
	public long FI_OUT_DILUT;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(String.valueOf(FI_CO_ID))
				.add(String.valueOf(FI_YEAR))
				.add(String.valueOf(FI_QTR))
				.add(FI_QTR_START_DATE.toFormattedString(10)) // YYYY-MM-DD
				.add(String.format("%.2f", FI_REVENUE))
				.add(String.format("%.2f", FI_NET_EARN))
				.add(String.format("%.2f", FI_BASIC_EPS))
				.add(String.format("%.2f", FI_DILUT_EPS))
				.add(String.format("%.2f", FI_MARGIN))
				.add(String.format("%.2f", FI_INVENTORY))
				.add(String.format("%.2f", FI_ASSETS))
				.add(String.format("%.2f", FI_LIABILITY))
				.add(String.valueOf(FI_OUT_BASIC))
				.add(String.valueOf(FI_OUT_DILUT))
				.toString();
	}
	
	@Override
	public void writeObject(Appendable out) throws IOException
	{
		write(out, FI_CO_ID);
		write(out, '|');
		write(out, FI_YEAR);
		write(out, '|');
		write(out, FI_QTR);
		write(out, '|');
		writeDate(out, FI_QTR_START_DATE); // YYYY-MM-DD
		write(out, '|');
		write(out, FI_REVENUE);
		write(out, '|');
		write(out, FI_NET_EARN);
		write(out, '|');
		write(out, FI_BASIC_EPS);
		write(out, '|');
		write(out, FI_DILUT_EPS);
		write(out, '|');
		write(out, FI_MARGIN);
		write(out, '|');
		write(out, FI_INVENTORY);
		write(out, '|');
		write(out, FI_ASSETS);
		write(out, '|');
		write(out, FI_LIABILITY);
		write(out, '|');
		write(out, FI_OUT_BASIC);
		write(out, '|');
		write(out, FI_OUT_DILUT);
	}
}