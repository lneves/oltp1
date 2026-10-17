package org.oltp1.egen.model;

import java.util.StringJoiner;

public class CompanyCompetitorRow
{
	public long CP_CO_ID;
	public long CP_COMP_CO_ID;
	public String CP_IN_ID;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(String.valueOf(CP_CO_ID))
				.add(String.valueOf(CP_COMP_CO_ID))
				.add(CP_IN_ID)
				.toString();
	}
}