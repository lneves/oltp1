package org.oltp1.egen.model;

import java.util.StringJoiner;

public class CustomerTaxRateRow
{
	public long CX_C_ID;
	public String CX_TX_ID;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(CX_TX_ID)
				.add(String.valueOf(CX_C_ID))
				.toString();
	}
}