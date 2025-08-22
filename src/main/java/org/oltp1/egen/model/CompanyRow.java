package org.oltp1.egen.model;

import java.util.StringJoiner;

import org.oltp1.egen.util.DateTime;

public class CompanyRow
{
	public long CO_ID;
	public String CO_ST_ID;
	public String CO_NAME;
	public String CO_IN_ID;
	public String CO_SP_RATE;
	public String CO_CEO;
	public long CO_AD_ID;
	public String CO_DESC;
	public DateTime CO_OPEN_DATE;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(String.valueOf(CO_ID))
				.add(CO_ST_ID)
				.add(CO_NAME)
				.add(CO_IN_ID)
				.add(CO_SP_RATE)
				.add(CO_CEO)
				.add(String.valueOf(CO_AD_ID))
				.add(CO_DESC)
				.add(CO_OPEN_DATE.toFormattedString(10)) // YYYY-MM-DD
				.toString();
	}
}