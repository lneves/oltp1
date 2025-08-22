package org.oltp1.egen.model;

import java.util.StringJoiner;

/**
 * Represents a single row in the ADDRESS table. This class is a data container
 * with public fields, mirroring the C++ ADDRESS_ROW struct. It includes a
 * toString() method for flat-file serialization. Based on inc/TableRows.h
 */
public class AddressRow
{
	public long AD_ID;
	public String AD_LINE1;
	public String AD_LINE2;
	public String AD_ZC_CODE;
	public String AD_CTRY;

	/**
	 * Formats the row for output to a flat file, using "|" as a delimiter. The
	 * format matches the C++ FlatAddressLoad implementation.
	 * 
	 * @return A delimited string representation of the row.
	 */
	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(String.valueOf(AD_ID))
				.add(AD_LINE1 != null ? AD_LINE1 : "")
				.add(AD_LINE2 != null ? AD_LINE2 : "")
				.add(AD_ZC_CODE != null ? AD_ZC_CODE : "")
				.add(AD_CTRY != null ? AD_CTRY : "")
				.toString();
	}
}