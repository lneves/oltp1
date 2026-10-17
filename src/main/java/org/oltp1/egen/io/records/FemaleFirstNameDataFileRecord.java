package org.oltp1.egen.io.records;

import java.util.regex.Pattern;

public class FemaleFirstNameDataFileRecord
{
	private static final Pattern splitter = Pattern.compile("\\t");
	public final String name;
	public final int weight;

	private FemaleFirstNameDataFileRecord(String name, int weight)
	{
		this.name = name;
		this.weight = weight;
	}

	public static FemaleFirstNameDataFileRecord parse(String line)
	{
		String[] fields = splitter.split(line);
		if (fields.length != 2)
		{
			throw new IllegalArgumentException("Incorrect field count for FemaleFirstNameDataFileRecord: " + line);
		}

		int weight = Integer.parseInt(fields[0]);
		String name = fields[1];

		return new FemaleFirstNameDataFileRecord(name, weight);
	}
}