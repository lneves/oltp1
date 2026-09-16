package org.oltp1.runner.tx.trade_result;

/**
 * Defines the contract for supplying SQL queries for the Trade-Result
 * transaction.
 */
public interface TradeResultDialect
{
	// Frame 1
	String claimTrade();

	String getTradeInfo();

	// Frame 2
	String getCustomerAccount();

	String insertHoldingSummary();

	String updateHoldingSummary();

	String upsertHoldingSummaryBuy();

	String upsertHoldingSummarySell();

	String updateHoldingSummaryBuy();

	String getHoldingSummaryForUpdate();

	String getHoldingAsc();

	String getHoldingDesc();

	String getHoldingAscForUpdate();

	String getHoldingDescForUpdate();

	String insertHoldingHistory();

	String updateHolding();

	String deleteHolding();

	String insertHolding();

	String deleteHoldingSummary();

	// Frame 3
	String getTaxRate();

	String updateTradeTax();

	// Frame 4
	String getCommissionRate();

	// Frame 5
	String updateTrade();

	String insertTradeHistory();

	String updateBroker();

	// Frame 6
	String insertSettlement();

	String updateCustomerAccount();

	String insertCashTransaction();

	String getAccountBalance();
}
