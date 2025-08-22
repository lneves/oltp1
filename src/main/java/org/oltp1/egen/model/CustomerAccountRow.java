package org.oltp1.egen.model;

import java.util.StringJoiner;

public class CustomerAccountRow
{
	public long CA_ID;
	public long CA_B_ID;
	public long CA_C_ID;
	public String CA_NAME;
	public char CA_TAX_ST;
	public double CA_BAL;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(String.valueOf(CA_ID))
				.add(String.valueOf(CA_B_ID))
				.add(String.valueOf(CA_C_ID))
				.add(CA_NAME)
				.add(String.valueOf(CA_TAX_ST))
				.add(String.format("%.2f", CA_BAL))
				.toString();
	}
}