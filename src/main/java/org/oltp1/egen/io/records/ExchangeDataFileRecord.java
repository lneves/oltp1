package org.oltp1.egen.io.records;

import java.util.regex.Pattern;

public class ExchangeDataFileRecord
{
	private static final Pattern splitter = Pattern.compile("\\t");
	public final String ex_id;
	public final String ex_name;
	public final int ex_open;
	public final int ex_close;
	public final String ex_desc;
	public final long ex_ad_id;

	private ExchangeDataFileRecord(String ex_id, String ex_name, int ex_open, int ex_close, String ex_desc, long ex_ad_id)
	{
		this.ex_id = ex_id;
		this.ex_name = ex_name;
		this.ex_open = ex_open;
		this.ex_close = ex_close;
		this.ex_desc = ex_desc;
		this.ex_ad_id = ex_ad_id;
	}

	public static ExchangeDataFileRecord parse(String line)
	{
		String[] fields = splitter.split(line);
		if (fields.length != 6)
		{
			throw new IllegalArgumentException("Incorrect field count for ExchangeDataFileRecord: " + line);
		}

		String ex_id = fields[0];
		String ex_name = fields[1];
		int ex_open = Integer.parseInt(fields[2]);
		int ex_close = Integer.parseInt(fields[3]);
		String ex_desc = fields[4];
		long ex_ad_id = Long.parseLong(fields[5]);

		return new ExchangeDataFileRecord(ex_id, ex_name, ex_open, ex_close, ex_desc, ex_ad_id);
	}
}