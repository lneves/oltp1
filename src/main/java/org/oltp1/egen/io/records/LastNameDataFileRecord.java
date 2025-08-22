package org.oltp1.egen.io.records;

import java.util.regex.Pattern;

public class LastNameDataFileRecord
{
	private static final Pattern splitter = Pattern.compile("\\t");
	public final String name;
	public final int weight;

	private LastNameDataFileRecord(String name, int weight)
	{
		this.name = name;
		this.weight = weight;
	}

	public static LastNameDataFileRecord parse(String line)
	{
		String[] fields = splitter.split(line);
		if (fields.length != 2)
		{
			throw new IllegalArgumentException("Incorrect field count for LastNameDataFileRecord: " + line);
		}

		int weight = Integer.parseInt(fields[0]);
		String name = fields[1];

		return new LastNameDataFileRecord(name, weight);
	}
}