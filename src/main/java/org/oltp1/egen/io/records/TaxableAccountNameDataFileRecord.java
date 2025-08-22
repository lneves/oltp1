package org.oltp1.egen.io.records;

public class TaxableAccountNameDataFileRecord
{
	public final String name;

	private TaxableAccountNameDataFileRecord(String name)
	{
		this.name = name;
	}

	public static TaxableAccountNameDataFileRecord parse(String line)
	{
		// The file format is simple: one field per line. No split needed, but
		// this handles potential whitespace.
		String name = line.trim();
		if (name.isEmpty())
		{
			throw new IllegalArgumentException("Cannot parse an empty line for TaxableAccountNameDataFileRecord.");
		}
		return new TaxableAccountNameDataFileRecord(name);
	}
}