package org.oltp1.egen.model;

import java.util.StringJoiner;

public class ExchangeRow
{
	public String EX_ID;
	public String EX_NAME;
	public int EX_NUM_SYMB;
	public int EX_OPEN;
	public int EX_CLOSE;
	public String EX_DESC;
	public long EX_AD_ID;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(EX_ID)
				.add(EX_NAME)
				.add(String.valueOf(EX_NUM_SYMB))
				.add(String.valueOf(EX_OPEN))
				.add(String.valueOf(EX_CLOSE))
				.add(EX_DESC)
				.add(String.valueOf(EX_AD_ID))
				.toString();
	}
}