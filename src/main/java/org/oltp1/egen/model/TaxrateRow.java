package org.oltp1.egen.model;

import java.util.StringJoiner;

public class TaxrateRow
{
	public String TX_ID;
	public String TX_NAME;
	public double TX_RATE;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(TX_ID)
				.add(TX_NAME)
				.add(String.format("%.5f", TX_RATE))
				.toString();
	}
}