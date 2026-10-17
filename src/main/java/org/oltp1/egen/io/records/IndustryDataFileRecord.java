package org.oltp1.egen.io.records;

import java.util.regex.Pattern;

public class IndustryDataFileRecord
{
	private static final Pattern splitter = Pattern.compile("\\t");
	public final String in_id;
	public final String in_name;
	public final String in_sc_id;

	private IndustryDataFileRecord(String in_id, String in_name, String in_sc_id)
	{
		this.in_id = in_id;
		this.in_name = in_name;
		this.in_sc_id = in_sc_id;
	}

	public static IndustryDataFileRecord parse(String line)
	{
		String[] fields = splitter.split(line);
		if (fields.length != 3)
		{
			throw new IllegalArgumentException("Incorrect field count for IndustryDataFileRecord: " + line);
		}

		String in_id = fields[0];
		String in_name = fields[1];
		String in_sc_id = fields[2];

		return new IndustryDataFileRecord(in_id, in_name, in_sc_id);
	}
}