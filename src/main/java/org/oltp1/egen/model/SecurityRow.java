package org.oltp1.egen.model;

import java.util.StringJoiner;

import org.oltp1.egen.util.DateTime;

public class SecurityRow
{
	public String S_SYMB;
	public String S_ISSUE;
	public String S_ST_ID;
	public String S_NAME;
	public String S_EX_ID;
	public long S_CO_ID;
	public long S_NUM_OUT;
	public DateTime S_START_DATE;
	public DateTime S_EXCH_DATE;
	public double S_PE;
	public double S_52WK_HIGH;
	public DateTime S_52WK_HIGH_DATE;
	public double S_52WK_LOW;
	public DateTime S_52WK_LOW_DATE;
	public double S_DIVIDEND;
	public double S_YIELD;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(S_SYMB)
				.add(S_ISSUE)
				.add(S_ST_ID)
				.add(S_NAME)
				.add(S_EX_ID)
				.add(String.valueOf(S_CO_ID))
				.add(String.valueOf(S_NUM_OUT))
				.add(S_START_DATE.toFormattedString(10))
				.add(S_EXCH_DATE.toFormattedString(10))
				.add(String.format("%.2f", S_PE))
				.add(String.format("%.2f", S_52WK_HIGH))
				.add(S_52WK_HIGH_DATE.toFormattedString(10))
				.add(String.format("%.2f", S_52WK_LOW))
				.add(S_52WK_LOW_DATE.toFormattedString(10))
				.add(String.format("%.2f", S_DIVIDEND))
				.add(String.format("%.2f", S_YIELD))
				.toString();
	}
}