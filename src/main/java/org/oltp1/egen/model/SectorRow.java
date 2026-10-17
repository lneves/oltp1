package org.oltp1.egen.model;

import java.util.StringJoiner;

public class SectorRow
{
	public String SC_ID;
	public String SC_NAME;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(SC_ID)
				.add(SC_NAME)
				.toString();
	}
}