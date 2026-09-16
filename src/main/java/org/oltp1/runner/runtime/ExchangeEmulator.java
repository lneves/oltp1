package org.oltp1.runner.runtime;

import org.oltp1.runner.tx.market_feed.TxMarketFeedInput;
import org.oltp1.runner.tx.trade_order.TxTradeOrderResult;
import org.oltp1.runner.tx.trade_result.TxTradeResultInput;

public interface ExchangeEmulator extends AutoCloseable
{
	void submitTradeOrderToMarket(TxTradeOrderResult orderResult);

	void submitToTradeResult(TxTradeResultInput input);

	void submitToMarketFeed(TxMarketFeedInput txInput);

	void stopAccepting();

	long drain(long timeoutMs);

	long drain();

}