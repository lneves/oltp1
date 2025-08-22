package org.oltp1.egen.io.records;

public class TaxRateDivisionDataFileRecord extends TaxRateFileRecord
{
	private TaxRateDivisionDataFileRecord(String tx_id, String tx_name, double tx_rate)
	{
		super(tx_id, tx_name, tx_rate);
	}

	public static TaxRateDivisionDataFileRecord parse(java.util.Deque<String> fields)
	{
		if (fields.size() != 3)
		{
			throw new IllegalArgumentException("Incorrect field count for TaxRateDivisionDataFileRecord.");
		}
		String tx_id = fields.pop();
		String tx_name = fields.pop();
		double tx_rate = Double.parseDouble(fields.pop());

		return new TaxRateDivisionDataFileRecord(tx_id, tx_name, tx_rate);
	}
}