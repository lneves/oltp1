package org.oltp1.runner.tx.market_feed;

/**
 * Strategy interface for providing SQL queries for the Market-Feed transaction.
 */
public interface MarketFeedQueries
{

	/**
	 * SQL to update the last_trade table with new ticker data.
	 */
	String updateLastTrade();

	/**
	 * SQL to retrieve a list of pending trade requests triggered by new market
	 * prices.
	 */
	String getRequestList();

	/**
	 * SQL to update the status of triggered trades.
	 */
	String updateTrade();

	/**
	 * SQL to delete the trade requests that have been triggered.
	 */
	String deleteTradeRequest();

	/**
	 * SQL to insert new records into the trade_history table for the triggered
	 * trades.
	 */
	String insertTradeHistory();
}
