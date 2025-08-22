package org.oltp1.runner.tx.trade_update;

/**
 * Defines the contract for supplying SQL queries for the Trade-Update
 * transaction.
 */
public interface TradeUpdateQueries
{
	// Frame 1
	String updateTradesFrame1();

	// Frame 2
	String getTradesFrame2();

	String updateTradesFrame2();

	// Frame 3
	String getTradesFrame3();

	String updateTradesFrame3();

	// Trade Info
	String getTradeInfo();

	String getSettlementInfo();

	String getTradeHistory();

	String getCashTransactionInfo();
}