package org.oltp1.runner.tx.trade_order;

/**
 * Result data from a successful Trade-Order execution.
 */
public record TxTradeOrderResult(
		double requestedPrice,
		String symbol,
		long tradeId,
		long tradeQty,
		String tradeTypeId,
		String eAction,
		boolean isMarket)
{
}
