package org.oltp1.egen.model;

import java.util.StringJoiner;

/**
 * Represents a single row in the ACCOUNT_PERMISSION table. This class is a data
 * container with public fields, mirroring the C++ ACCOUNT_PERMISSION_ROW
 * struct. It includes a toString() method for flat-file serialization. Based on
 * inc/TableRows.h
 */
public class AccountPermissionRow
{
	public long AP_CA_ID;
	public String AP_ACL;
	public String AP_TAX_ID;
	public String AP_L_NAME;
	public String AP_F_NAME;

	/**
	 * Formats the row for output to a flat file, using "|" as a delimiter. The
	 * format matches the C++ FlatAccountPermissionLoad implementation.
	 * 
	 * @return A delimited string representation of the row.
	 */
	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(String.valueOf(AP_CA_ID))
				.add(AP_ACL != null ? AP_ACL : "")
				.add(AP_TAX_ID != null ? AP_TAX_ID : "")
				.add(AP_L_NAME != null ? AP_L_NAME : "")
				.add(AP_F_NAME != null ? AP_F_NAME : "")
				.toString();
	}
}