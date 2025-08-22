package org.oltp1.egen.model;

import java.util.StringJoiner;

public class ChargeRow
{
	public String CH_TT_ID;
	public int CH_C_TIER;
	public double CH_CHRG;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(CH_TT_ID)
				.add(String.valueOf(CH_C_TIER))
				.add(String.format("%.2f", CH_CHRG))
				.toString();
	}
}