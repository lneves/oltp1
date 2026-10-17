package org.oltp1.egen.model;

import java.util.StringJoiner;

public class WatchItemRow
{
	public long WI_WL_ID;
	public String WI_S_SYMB;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(String.valueOf(WI_WL_ID))
				.add(WI_S_SYMB)
				.toString();
	}
}