package org.oltp1.egen.model;

import org.oltp1.egen.util.DateTime;
import org.oltp1.egen.util.Money;

public class HoldingInfo
{
	public final long tradeId;
	public int tradeQty; // This is mutable as it's updated by buy/sell trades
	public Money tradePrice = new Money();
	public final DateTime buyDts;
	public final long symbolIndex;

	public HoldingInfo(long tradeId, int tradeQty, Money tradePrice,
			DateTime buyDts, long symbolIndex)
	{
		this.tradeId = tradeId;
		this.tradeQty = tradeQty;
		this.tradePrice = tradePrice;
		this.buyDts = buyDts;
		this.symbolIndex = symbolIndex;
	}
}