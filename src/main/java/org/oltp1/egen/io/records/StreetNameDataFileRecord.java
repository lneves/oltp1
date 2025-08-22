package org.oltp1.egen.io.records;

import java.util.regex.Pattern;

public class StreetNameDataFileRecord
{
	private static final Pattern splitter = Pattern.compile("\\t");
	public final String street;
	public final int weight;

	private StreetNameDataFileRecord(String street, int weight)
	{
		this.street = street;
		this.weight = weight;
	}

	public static StreetNameDataFileRecord parse(String line)
	{
		String[] fields = splitter.split(line);
		if (fields.length != 2)
		{
			throw new IllegalArgumentException("Incorrect field count for StreetNameDataFileRecord: " + line);
		}

		int weight = Integer.parseInt(fields[0]);
		String street = fields[1];

		return new StreetNameDataFileRecord(street, weight);
	}
}