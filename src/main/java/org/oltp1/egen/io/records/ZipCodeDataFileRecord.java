package org.oltp1.egen.io.records;

import java.util.regex.Pattern;

public class ZipCodeDataFileRecord
{
	private static final Pattern splitter = Pattern.compile("\\t");
	public final int weight;
	public final int divisionTaxKey;
	public final String zc_code;
	public final String zc_town;
	public final String zc_div;

	private ZipCodeDataFileRecord(int weight, int divisionTaxKey, String zc_code, String zc_town, String zc_div)
	{
		this.weight = weight;
		this.divisionTaxKey = divisionTaxKey;
		this.zc_code = zc_code;
		this.zc_town = zc_town;
		this.zc_div = zc_div;
	}

	public static ZipCodeDataFileRecord parse(String line)
	{
		String[] fields = splitter.split(line);
		if (fields.length != 5)
		{
			throw new IllegalArgumentException("Incorrect field count for ZipCodeDataFileRecord: " + line);
		}

		int weight = Integer.parseInt(fields[0]);
		int divisionTaxKey = Integer.parseInt(fields[1]);
		String zc_code = fields[2];
		String zc_town = fields[3];
		String zc_div = fields[4];

		return new ZipCodeDataFileRecord(weight, divisionTaxKey, zc_code, zc_town, zc_div);
	}
}