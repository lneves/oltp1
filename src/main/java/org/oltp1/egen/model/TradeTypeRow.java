package org.oltp1.egen.model;

import java.util.StringJoiner;

public class TradeTypeRow
{
	public String TT_ID;
	public String TT_NAME;
	public boolean TT_IS_SELL;
	public boolean TT_IS_MRKT;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(TT_ID)
				.add(TT_NAME)
				.add(TT_IS_SELL ? "1" : "0")
				.add(TT_IS_MRKT ? "1" : "0")
				.toString();
	}
}