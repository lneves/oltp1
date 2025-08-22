package org.oltp1.runner.tx.trade_status;

/**
 * Defines the contract for supplying SQL queries for the Trade-Status
 * transaction.
 */
public interface TradeStatusQueries
{
	String getTradeName();

	String getTradeStatus();
}
