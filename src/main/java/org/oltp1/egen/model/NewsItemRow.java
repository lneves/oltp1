package org.oltp1.egen.model;

import java.util.StringJoiner;

import org.oltp1.egen.util.DateTime;

public class NewsItemRow
{
	public long NI_ID;
	public String NI_HEADLINE;
	public String NI_SUMMARY;
	public String NI_ITEM;
	public DateTime NI_DTS;
	public String NI_SOURCE;
	public String NI_AUTHOR;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(String.valueOf(NI_ID))
				.add(NI_HEADLINE)
				.add(NI_SUMMARY)
				.add(NI_ITEM)
				.add(NI_DTS.toFormattedString(12)) // YYYY-MM-DD HH:mm:ss.SSS
				.add(NI_SOURCE)
				.add(NI_AUTHOR)
				.toString();
	}
}