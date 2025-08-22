package org.oltp1.egen.model;

import java.util.StringJoiner;

public class NewsXrefRow
{
	public long NX_NI_ID;
	public long NX_CO_ID;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(String.valueOf(NX_NI_ID))
				.add(String.valueOf(NX_CO_ID))
				.toString();
	}
}