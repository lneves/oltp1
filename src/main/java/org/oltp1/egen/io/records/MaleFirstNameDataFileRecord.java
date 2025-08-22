package org.oltp1.egen.io.records;

import java.util.regex.Pattern;

public class MaleFirstNameDataFileRecord
{
	private static final Pattern splitter = Pattern.compile("\\t");
	public final String name;
	public final int weight;

	private MaleFirstNameDataFileRecord(String name, int weight)
	{
		this.name = name;
		this.weight = weight;
	}

	public static MaleFirstNameDataFileRecord parse(String line)
	{
		String[] fields = splitter.split(line);
		if (fields.length != 2)
		{
			throw new IllegalArgumentException("Incorrect field count for MaleFirstNameDataFileRecord: " + line);
		}

		int weight = Integer.parseInt(fields[0]);
		String name = fields[1];

		return new MaleFirstNameDataFileRecord(name, weight);
	}
}