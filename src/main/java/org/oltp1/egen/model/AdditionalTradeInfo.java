package org.oltp1.egen.model;

import org.oltp1.egen.util.Money;

public class AdditionalTradeInfo
{
	public Money buyValue = new Money();
	public Money sellValue = new Money();
	public long currentBrokerId;
	public TaxStatus accountTaxStatus;
	public Money commission = new Money();
	public Money charge = new Money();
	public Money tax = new Money();
	public Money settlementAmount = new Money();
}