package org.oltp1.egen.model;

import java.util.StringJoiner;

public class IndustryRow
{
	public String IN_ID;
	public String IN_NAME;
	public String IN_SC_ID;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(IN_ID)
				.add(IN_NAME)
				.add(IN_SC_ID)
				.toString();
	}
}