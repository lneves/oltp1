package org.oltp1.egen.model;

import java.util.StringJoiner;

public class ZipCodeRow
{
	public String ZC_CODE;
	public String ZC_TOWN;
	public String ZC_DIV;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(ZC_CODE)
				.add(ZC_TOWN)
				.add(ZC_DIV)
				.toString();
	}
}