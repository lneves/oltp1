package org.oltp1.runner.model;

import java.util.Objects;

/**
 * Represents a single trade update within the Market-Feed transaction.
 */
public class Ticker
{
	private final String symbol;
	private final double tradePrice;
	private final long tradeQty;

	public Ticker(String symbol, double tradePrice, long tradeQty)
	{
		this.symbol = symbol;
		this.tradePrice = tradePrice;
		this.tradeQty = tradeQty;
	}

	public String getSymbol()
	{
		return symbol;
	}

	public double getTradePrice()
	{
		return tradePrice;
	}

	public long getTradeQty()
	{
		return tradeQty;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(symbol);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Ticker other = (Ticker) obj;
		return Objects.equals(symbol, other.symbol);
	}

	@Override
	public String toString()
	{
		return String.format("Symbol: %-5s, Price: %8.2f, Quantity: %d", symbol, tradePrice, tradeQty);
	}
}