package org.oltp1.egen.io.records;

public class TaxRateFileRecord
{
	public final String tx_id;
	public final String tx_name;
	public final double tx_rate;

	public TaxRateFileRecord(String tx_id, String tx_name, double tx_rate)
	{
		super();
		this.tx_id = tx_id;
		this.tx_name = tx_name;
		this.tx_rate = tx_rate;
	}
}