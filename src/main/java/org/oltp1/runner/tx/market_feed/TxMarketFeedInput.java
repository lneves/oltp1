package org.oltp1.runner.tx.market_feed;

import java.util.Collection;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.oltp1.runner.model.Ticker;
import org.oltp1.runner.model.TradeStatus;
import org.oltp1.runner.model.TradeType;

public class TxMarketFeedInput
{
	// Status type for submitted trades
	public final String status_submitted;
	// Trade type ID for limit buy orders.
	public final TradeType type_limit_buy;
	// Trade type ID for limit sell orders.
	public final TradeType type_limit_sell;
	// Trade type ID for stop loss orders.
	public final TradeType type_stop_loss;
	// A list of trades that make up the feed for this transaction.
	public final Collection<Ticker> tickers;
	public final int unique_symbols;

	public TxMarketFeedInput(TradeStatus statusSubmitted, TradeType tradeTypeLimitBuy, TradeType tradeTypeLimitSell, TradeType tradeTypeStopLoss, Collection<Ticker> tickers, int uniqueSymbols)
	{
		this.status_submitted = statusSubmitted.id;
		this.type_limit_buy = tradeTypeLimitBuy;
		this.type_limit_sell = tradeTypeLimitSell;
		this.type_stop_loss = tradeTypeStopLoss;
		this.tickers = tickers;
		this.unique_symbols = uniqueSymbols;
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("unique_symbols", unique_symbols)
				.append("status_submitted", status_submitted)
				.append("type_limit_buy", type_limit_buy)
				.append("type_limit_sell", type_limit_sell)
				.append("type_stop_loss", type_stop_loss)
				.toString();
	}
}