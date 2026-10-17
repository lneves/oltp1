package org.oltp1.egen.io.records;

import java.util.regex.Pattern;

public class AreaCodeDataFileRecord
{
	private static final Pattern splitter = Pattern.compile("\\t");
	public final String areaCode;
	public final int weight;

	private AreaCodeDataFileRecord(String areaCode, int weight)
	{
		this.areaCode = areaCode;
		this.weight = weight;
	}

	public static AreaCodeDataFileRecord parse(String line)
	{
		String[] fields = splitter.split(line);
		if (fields.length != 2)
		{
			throw new IllegalArgumentException("Incorrect field count for AreaCodeDataFileRecord: " + line);
		}

		int weight = Integer.parseInt(fields[0]);
		String areaCode = fields[1];

		return new AreaCodeDataFileRecord(areaCode, weight);
	}
}