package org.oltp1.egen.model;

import java.util.StringJoiner;

public class WatchListRow
{
	public long WL_ID;
	public long WL_C_ID;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(String.valueOf(WL_ID))
				.add(String.valueOf(WL_C_ID))
				.toString();
	}
}