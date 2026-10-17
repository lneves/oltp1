package org.oltp1.egen.io.records;

public class NonTaxableAccountNameDataFileRecord
{
	public final String name;

	private NonTaxableAccountNameDataFileRecord(String name)
	{
		this.name = name;
	}

	public static NonTaxableAccountNameDataFileRecord parse(String line)
	{
		// The file format is simple: one field per line. No split needed, but
		// this handles potential whitespace.
		String name = line.trim();
		if (name.isEmpty())
		{
			throw new IllegalArgumentException("Cannot parse an empty line for NonTaxableAccountNameDataFileRecord.");
		}
		return new NonTaxableAccountNameDataFileRecord(name);
	}
}