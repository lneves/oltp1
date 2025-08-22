package org.oltp1.runner.model;

public class Customer
{
	public long cId;
	public String cTaxId;
	public short tier;

	public Customer()
	{
		super();
	}

	public Customer(long cId, String cTaxId, short tier)
	{
		super();
		this.cId = cId;
		this.cTaxId = cTaxId;
		this.tier = tier;
	}

	@Override
	public String toString()
	{
		return String.format("Customer [cId=%s, cTaxId=%s, tier=%s]", cId, cTaxId, tier);
	}

}