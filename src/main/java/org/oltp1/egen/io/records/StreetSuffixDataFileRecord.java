package org.oltp1.egen.io.records;

import java.util.regex.Pattern;

public class StreetSuffixDataFileRecord
{
	private static final Pattern splitter = Pattern.compile("\\t");
	public final String suffix;
	public final int weight;

	private StreetSuffixDataFileRecord(String suffix, int weight)
	{
		this.suffix = suffix;
		this.weight = weight;
	}

	public static StreetSuffixDataFileRecord parse(String line)
	{
		String[] fields = splitter.split(line);
		if (fields.length != 2)
		{
			throw new IllegalArgumentException("Incorrect field count for StreetSuffixDataFileRecord: " + line);
		}

		int weight = Integer.parseInt(fields[0]);
		String suffix = fields[1];

		return new StreetSuffixDataFileRecord(suffix, weight);
	}
}