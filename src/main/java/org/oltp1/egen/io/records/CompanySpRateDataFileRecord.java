package org.oltp1.egen.io.records;

import java.util.regex.Pattern;

public class CompanySpRateDataFileRecord
{
	private static final Pattern splitter = Pattern.compile("\\t");
	public final String co_sp_rate;
	public final int weight;

	private CompanySpRateDataFileRecord(String co_sp_rate, int weight)
	{
		this.co_sp_rate = co_sp_rate;
		this.weight = weight;
	}

	public static CompanySpRateDataFileRecord parse(String line)
	{
		String[] fields = splitter.split(line);
		if (fields.length != 2)
		{
			throw new IllegalArgumentException("Incorrect field count for CompanySpRateDataFileRecord: " + line);
		}

		int weight = Integer.parseInt(fields[0]);
		String co_sp_rate = fields[1];

		return new CompanySpRateDataFileRecord(co_sp_rate, weight);
	}
}