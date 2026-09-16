package org.oltp1.runner.tx.trade_result;

public record TxTradeResultInput(long tradeId, double tradePrice, String symbol, long tradeQty)
{

}
