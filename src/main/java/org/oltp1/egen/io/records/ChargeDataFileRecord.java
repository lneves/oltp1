package org.oltp1.egen.io.records;

import java.util.regex.Pattern;

public class ChargeDataFileRecord
{
	private static final Pattern splitter = Pattern.compile("\\t");
	public final String ch_tt_id;
	public final int ch_c_tier;
	public final double ch_chrg;

	private ChargeDataFileRecord(String ch_tt_id, int ch_c_tier, double ch_chrg)
	{
		this.ch_tt_id = ch_tt_id;
		this.ch_c_tier = ch_c_tier;
		this.ch_chrg = ch_chrg;
	}

	public static ChargeDataFileRecord parse(String line)
	{
		String[] fields = splitter.split(line);

		// C++ version throws a runtime error if field count is wrong.
		if (fields.length != 3)
		{
			throw new IllegalArgumentException("Incorrect field count for ChargeDataFileRecord: " + line);
		}

		String ch_tt_id = fields[0];
		int ch_c_tier = Integer.parseInt(fields[1]);
		double ch_chrg = Double.parseDouble(fields[2]);

		return new ChargeDataFileRecord(ch_tt_id, ch_c_tier, ch_chrg);
	}
}