package org.oltp1.runner.tx.trade_lookup;

/**
 * Defines the contract for supplying SQL queries for the Trade-Lookup
 * transaction.
 */
public interface TradeLookupQueries
{
	String getTradeInfoFrame1();

	String getTradeHistory();

	String getFrame2();

	String getFrame3();

	String getFrame4();
}
