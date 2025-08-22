package org.oltp1.egen.io.records;

import java.util.regex.Pattern;

public class SectorDataFileRecord
{
	private static final Pattern splitter = Pattern.compile("\\t");
	public final String sc_id;
	public final String sc_name;

	private SectorDataFileRecord(String sc_id, String sc_name)
	{
		this.sc_id = sc_id;
		this.sc_name = sc_name;
	}

	public static SectorDataFileRecord parse(String line)
	{
		String[] fields = splitter.split(line);
		if (fields.length != 2)
		{
			throw new IllegalArgumentException("Incorrect field count for SectorDataFileRecord: " + line);
		}

		String sc_id = fields[0];
		String sc_name = fields[1];

		return new SectorDataFileRecord(sc_id, sc_name);
	}
}