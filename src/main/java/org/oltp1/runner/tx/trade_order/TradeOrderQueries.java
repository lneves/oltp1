package org.oltp1.runner.tx.trade_order;

/**
 * Defines the contract for supplying SQL queries for the Trade-Order
 * transaction.
 */
public interface TradeOrderQueries
{
	String getCustomerInfo();

	String getAccountPermissions();

	String getSecurityInfoByCoName();

	String getSecurityInfoBySymbol();

	String getLastTrade();

	String getTradeType();

	String getHoldingSummary();

	String getHoldingAsc();

	String getHoldingDesc();

	String getTaxRate();

	String getFees();

	String getCustomerAssets();

	String insertTrade();

	String insertTradeRequest();

	String insertTradeHistory();
}
