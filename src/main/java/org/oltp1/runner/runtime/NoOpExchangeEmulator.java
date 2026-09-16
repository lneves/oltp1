package org.oltp1.runner.runtime;

import org.oltp1.runner.tx.market_feed.TxMarketFeedInput;
import org.oltp1.runner.tx.trade_order.TxTradeOrderResult;
import org.oltp1.runner.tx.trade_result.TxTradeResultInput;

public class NoOpExchangeEmulator implements ExchangeEmulator
{
	@Override
	public void submitTradeOrderToMarket(TxTradeOrderResult orderResult)
	{
	}

	@Override
	public void submitToTradeResult(TxTradeResultInput input)
	{
	}

	@Override
	public void submitToMarketFeed(TxMarketFeedInput txInput)
	{
	}

	@Override
	public void stopAccepting()
	{
	}

	@Override
	public long drain()
	{
		return 0;
	}

	@Override
	public long drain(long timeoutMs)
	{
		return 0;
	}

	@Override
	public void close()
	{
	}
}
