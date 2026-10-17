package org.oltp1.egen.io.records;

import java.util.regex.Pattern;

public class CompanyDataFileRecord
{
	private static final Pattern splitter = Pattern.compile("\\t");
	public final long co_id;
	public final String co_st_id;
	public final String co_name;
	public final String co_in_id;
	public final String co_desc;

	private CompanyDataFileRecord(long co_id, String co_st_id, String co_name, String co_in_id, String co_desc)
	{
		this.co_id = co_id;
		this.co_st_id = co_st_id;
		this.co_name = co_name;
		this.co_in_id = co_in_id;
		this.co_desc = co_desc;
	}

	public static CompanyDataFileRecord parse(String line)
	{
		String[] fields = splitter.split(line);
		if (fields.length != 5)
		{
			throw new IllegalArgumentException("Incorrect field count for CompanyDataFileRecord: " + line);
		}

		long co_id = Long.parseLong(fields[0]);
		String co_st_id = fields[1];
		String co_name = fields[2];
		String co_in_id = fields[3];
		String co_desc = fields[4];

		return new CompanyDataFileRecord(co_id, co_st_id, co_name, co_in_id, co_desc);
	}
}