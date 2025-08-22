package org.oltp1.egen.model;

import java.util.StringJoiner;

import org.oltp1.egen.util.DateTime;

public class LastTradeRow
{
	public String LT_S_SYMB;
	public DateTime LT_DTS;
	public double LT_PRICE;
	public double LT_OPEN_PRICE;
	public long LT_VOL;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(LT_S_SYMB)
				.add(LT_DTS.toFormattedString(12)) // YYYY-MM-DD HH:mm:ss.SSS
				.add(String.format("%.2f", LT_PRICE))
				.add(String.format("%.2f", LT_OPEN_PRICE))
				.add(String.valueOf(LT_VOL))
				.toString();
	}
}