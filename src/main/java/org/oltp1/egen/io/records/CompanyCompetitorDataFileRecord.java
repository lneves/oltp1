package org.oltp1.egen.io.records;

import java.util.regex.Pattern;

public class CompanyCompetitorDataFileRecord
{
	private static final Pattern splitter = Pattern.compile("\\t");
	public final long cp_co_id;
	public final long cp_comp_co_id;
	public final String cp_in_id;

	private CompanyCompetitorDataFileRecord(long cp_co_id, long cp_comp_co_id, String cp_in_id)
	{
		this.cp_co_id = cp_co_id;
		this.cp_comp_co_id = cp_comp_co_id;
		this.cp_in_id = cp_in_id;
	}

	public static CompanyCompetitorDataFileRecord parse(String line)
	{
		String[] fields = splitter.split(line);
		if (fields.length != 3)
		{
			throw new IllegalArgumentException("Incorrect field count for CompanyCompetitorDataFileRecord: " + line);
		}

		long cp_co_id = Long.parseLong(fields[0]);
		long cp_comp_co_id = Long.parseLong(fields[1]);
		String cp_in_id = fields[2];

		return new CompanyCompetitorDataFileRecord(cp_co_id, cp_comp_co_id, cp_in_id);
	}
}