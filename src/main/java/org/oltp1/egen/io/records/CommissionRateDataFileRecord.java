package org.oltp1.egen.io.records;

import java.util.regex.Pattern;

public class CommissionRateDataFileRecord
{
	private static final Pattern splitter = Pattern.compile("\\t");
	public final int cr_c_tier;
	public final String cr_tt_id;
	public final String cr_ex_id;
	public final int cr_from_qty;
	public final int cr_to_qty;
	public final double cr_rate;

	private CommissionRateDataFileRecord(int cr_c_tier, String cr_tt_id, String cr_ex_id, int cr_from_qty, int cr_to_qty, double cr_rate)
	{
		this.cr_c_tier = cr_c_tier;
		this.cr_tt_id = cr_tt_id;
		this.cr_ex_id = cr_ex_id;
		this.cr_from_qty = cr_from_qty;
		this.cr_to_qty = cr_to_qty;
		this.cr_rate = cr_rate;
	}

	public static CommissionRateDataFileRecord parse(String line)
	{
		String[] fields = splitter.split(line);

		// C++ version throws a runtime error if field count is wrong.
		if (fields.length != 6)
		{
			throw new IllegalArgumentException("Incorrect field count for CommissionRateDataFileRecord: " + line);
		}

		int cr_c_tier = Integer.parseInt(fields[0]);
		String cr_tt_id = fields[1];
		String cr_ex_id = fields[2];
		int cr_from_qty = Integer.parseInt(fields[3]);
		int cr_to_qty = Integer.parseInt(fields[4]);
		double cr_rate = Double.parseDouble(fields[5]);

		return new CommissionRateDataFileRecord(cr_c_tier, cr_tt_id, cr_ex_id, cr_from_qty, cr_to_qty, cr_rate);
	}
}