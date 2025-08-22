package org.oltp1.egen.io.records;

import java.util.regex.Pattern;

public class NewsDataFileRecord
{
	private static final Pattern splitter = Pattern.compile("\\t");
	public final String word;
	public final int weight;

	private NewsDataFileRecord(String word, int weight)
	{
		this.word = word;
		this.weight = weight;
	}

	public static NewsDataFileRecord parse(String line)
	{
		String[] fields = splitter.split(line);
		if (fields.length != 2)
		{
			throw new IllegalArgumentException("Incorrect field count for NewsDataFileRecord: " + line);
		}
		return new NewsDataFileRecord(fields[1], Integer.parseInt(fields[0]));
	}
}