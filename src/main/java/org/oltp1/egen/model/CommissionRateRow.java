package org.oltp1.egen.model;

import java.util.StringJoiner;

public class CommissionRateRow
{
	public int CR_C_TIER;
	public String CR_TT_ID;
	public String CR_EX_ID;
	public int CR_FROM_QTY;
	public int CR_TO_QTY;
	public double CR_RATE;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(String.valueOf(CR_C_TIER))
				.add(CR_TT_ID)
				.add(CR_EX_ID)
				.add(String.valueOf(CR_FROM_QTY))
				.add(String.valueOf(CR_TO_QTY))
				.add(String.format("%.2f", CR_RATE))
				.toString();
	}
}