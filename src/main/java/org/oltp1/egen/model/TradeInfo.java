package org.oltp1.egen.model;

import org.oltp1.egen.util.Money;

public class TradeInfo implements Comparable<TradeInfo>
{
	public long tradeId;
	public TradeType tradeType;
	public StatusType tradeStatus;
	public double pendingTime;
	public double submissionTime;
	public double completionTime;
	public long symbolIndex;
	public int symbolIndexInAccount;
	public int tradeQty;
	public Money bidPrice = new Money();
	public Money tradePrice = new Money();
	public long customer;
	public int customerTier;
	public long customerAccount;
	public boolean isLifo;

	@Override
	public int compareTo(TradeInfo other)
	{
		return Double.compare(this.completionTime, other.completionTime);
		// return Double.compare(this.tradeId, other.tradeId);
	}

	public TradeInfo copy()
	{
		TradeInfo newCopy = new TradeInfo();
		newCopy.tradeId = this.tradeId;
		newCopy.tradeType = this.tradeType;
		newCopy.tradeStatus = this.tradeStatus;
		newCopy.pendingTime = this.pendingTime;
		newCopy.submissionTime = this.submissionTime;
		newCopy.completionTime = this.completionTime;
		newCopy.symbolIndex = this.symbolIndex;
		newCopy.symbolIndexInAccount = this.symbolIndexInAccount;
		newCopy.tradeQty = this.tradeQty;
		newCopy.bidPrice = this.bidPrice;
		newCopy.tradePrice = this.tradePrice;
		newCopy.customer = this.customer;
		newCopy.customerTier = this.customerTier;
		newCopy.customerAccount = this.customerAccount;
		newCopy.isLifo = this.isLifo;
		return newCopy;
	}

	@Override
	public String toString()
	{
		return String.format("tradeId: %s  tradeType: %s  tradeStatus: %s  pendingTime: %s  submissionTime: %s  completionTime: %s  symbolIndex: %s  symbolIndexInAccount: %s  tradeQty: %s  bidPrice: %s  tradePrice: %s  customer: %s  customerTier: %s  customerAccount: %s  isLifo=%s", tradeId, tradeType.ordinal(), tradeStatus.ordinal(), pendingTime, submissionTime, completionTime, symbolIndex, symbolIndexInAccount, tradeQty, bidPrice, tradePrice, customer, customerTier, customerAccount, isLifo);
	}

}