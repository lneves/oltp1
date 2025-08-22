package org.oltp1.egen.model;

import java.util.StringJoiner;

public class StatusTypeRow
{
	public String ST_ID;
	public String ST_NAME;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(ST_ID)
				.add(ST_NAME)
				.toString();
	}
}