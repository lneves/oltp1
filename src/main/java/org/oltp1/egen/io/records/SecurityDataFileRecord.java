package org.oltp1.egen.io.records;

import java.util.regex.Pattern;

public class SecurityDataFileRecord
{
	private static final Pattern splitter = Pattern.compile("\\t");
	public final long s_id;
	public final String s_st_id;
	public final String s_symb;
	public final String s_issue;
	public final String s_ex_id;
	public final long s_co_id;

	private SecurityDataFileRecord(long s_id, String s_st_id, String s_symb, String s_issue, String s_ex_id, long s_co_id)
	{
		this.s_id = s_id;
		this.s_st_id = s_st_id;
		this.s_symb = s_symb;
		this.s_issue = s_issue;
		this.s_ex_id = s_ex_id;
		this.s_co_id = s_co_id;
	}

	public static SecurityDataFileRecord parse(String line)
	{
		String[] fields = splitter.split(line);
		if (fields.length != 6)
		{
			throw new IllegalArgumentException("Incorrect field count for SecurityDataFileRecord: " + line);
		}

		long s_id = Long.parseLong(fields[0]);
		String s_st_id = fields[1];
		String s_symb = fields[2];
		String s_issue = fields[3];
		String s_ex_id = fields[4];
		long s_co_id = Long.parseLong(fields[5]);

		return new SecurityDataFileRecord(s_id, s_st_id, s_symb, s_issue, s_ex_id, s_co_id);
	}
}