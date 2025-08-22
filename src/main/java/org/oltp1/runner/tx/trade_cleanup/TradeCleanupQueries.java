package org.oltp1.runner.tx.trade_cleanup;

/**
 * Defines the contract for supplying SQL queries for the Trade-Cleanup
 * transaction.
 */
public interface TradeCleanupQueries
{
	String insertTradeHistory1();

	String updateTrade1();

	String deleteTradeRequest();

	String updateTrade2();

	String insertTradeHistory2();
}
