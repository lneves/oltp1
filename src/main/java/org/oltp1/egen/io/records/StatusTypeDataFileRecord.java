package org.oltp1.egen.io.records;

import java.util.regex.Pattern;

public class StatusTypeDataFileRecord
{
	private static final Pattern splitter = Pattern.compile("\\t");
	public final String st_id;
	public final String st_name;

	private StatusTypeDataFileRecord(String st_id, String st_name)
	{
		this.st_id = st_id;
		this.st_name = st_name;
	}

	public static StatusTypeDataFileRecord parse(String line)
	{
		String[] fields = splitter.split(line);
		if (fields.length != 2)
		{
			throw new IllegalArgumentException("Incorrect field count for StatusTypeDataFileRecord: " + line);
		}

		String st_id = fields[0];
		String st_name = fields[1];

		return new StatusTypeDataFileRecord(st_id, st_name);
	}
}