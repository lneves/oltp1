package org.oltp1.egen.io.records;

import java.util.regex.Pattern;

public class TradeTypeDataFileRecord
{
	private static final Pattern splitter = Pattern.compile("\\t");
	public final String tt_id;
	public final String tt_name;
	public final boolean tt_is_sell;
	public final boolean tt_is_mrkt;

	private TradeTypeDataFileRecord(String tt_id, String tt_name, boolean tt_is_sell, boolean tt_is_mrkt)
	{
		this.tt_id = tt_id;
		this.tt_name = tt_name;
		this.tt_is_sell = tt_is_sell;
		this.tt_is_mrkt = tt_is_mrkt;
	}

	public static TradeTypeDataFileRecord parse(String line)
	{
		String[] fields = splitter.split(line);
		if (fields.length != 4)
		{
			throw new IllegalArgumentException("Incorrect field count for TradeTypeDataFileRecord: " + line);
		}

		String tt_id = fields[0];
		String tt_name = fields[1];
		// The file uses "1" for true and "0" for false.
		boolean tt_is_sell = fields[2].equals("1");
		boolean tt_is_mrkt = fields[3].equals("1");

		return new TradeTypeDataFileRecord(tt_id, tt_name, tt_is_sell, tt_is_mrkt);
	}
}