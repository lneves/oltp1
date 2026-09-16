package org.oltp1.runner.tx.trade_lookup;

import java.util.Collection;

/**
 * Defines the contract for supplying SQL queries for the Trade-Lookup
 * transaction.
 */
public interface TradeLookupDialect
{
	String getFrame2();

	String getFrame3();

	String getFrame4HoldingHistory();

	String getFrame4TargetTrade();

	String getTradeHistory();

	String getTradeInfoFrame1();

	String buildTradeIdList(Collection<Long> tradeIds);
}
