package org.oltp1.runner.runtime;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.generator.TxInputGenerator;
import org.oltp1.runner.tx.market_feed.TxMarketFeed;
import org.oltp1.runner.tx.market_feed.TxMarketFeedInput;
import org.oltp1.runner.tx.market_feed.TxMarketFeedOutput;
import org.oltp1.runner.tx.trade_result.TxTradeResult;
import org.oltp1.runner.tx.trade_result.TxTradeResultInput;
import org.oltp1.runner.tx.trade_result.TxTradeResultOutput;

/**
 * Executes one Market-Feed or Trade-Result on behalf of the MEE. Trade-Results
 * triggered by a Market-Feed are handed back to the MEE for asynchronous
 * execution (reference: {@code SendToMarketFromFrame}), never processed inline
 * here, so a feed cannot occupy its worker for the whole trigger chain.
 */
public class MeeClient
{
	private final Function<TxMarketFeedInput, TxMarketFeedOutput> marketFeedProcessor;
	private final Function<TxTradeResultInput, TxTradeResultOutput> tradeResultProcessor;
	private final Consumer<TxTradeResultInput> triggeredTradeResultSink;

	public MeeClient(
			SqlContext sqlCtx,
			TxInputGenerator txInputGen,
			BenchmarkMetrics metrics,
			Consumer<TxTradeResultInput> triggeredTradeResultSink)
	{
		this(
				new TxMarketFeed(txInputGen, sqlCtx, metrics)::process,
				new TxTradeResult(txInputGen, sqlCtx, metrics)::process,
				triggeredTradeResultSink);
	}

	MeeClient(
			Function<TxMarketFeedInput, TxMarketFeedOutput> marketFeedProcessor,
			Function<TxTradeResultInput, TxTradeResultOutput> tradeResultProcessor,
			Consumer<TxTradeResultInput> triggeredTradeResultSink)
	{
		this.marketFeedProcessor = marketFeedProcessor;
		this.tradeResultProcessor = tradeResultProcessor;
		this.triggeredTradeResultSink = triggeredTradeResultSink;
	}

	public TxTradeResultOutput processTradeResult(TxTradeResultInput input)
	{
		return tradeResultProcessor.apply(input);
	}

	public void processMarketFeed(TxMarketFeedInput txInput)
	{
		TxMarketFeedOutput mktFeedOutput = marketFeedProcessor.apply(txInput);

		List<Map<String, Object>> triggered = mktFeedOutput.trade_requests;

		if (triggered == null)
		{
			return;
		}

		// The ticker entries for these trades are added by the MEE when each
		// Trade-Result completes, exactly like market orders.
		for (Map<String, Object> request : triggered)
		{
			triggeredTradeResultSink.accept(new TxTradeResultInput(
					getAsLong(request, "tr_t_id"),
					getAsDouble(request, "tr_bid_price"),
					getAsString(request, "tr_s_symb"),
					getAsLong(request, "tr_qty")));
		}
	}

	private long getAsLong(Map<String, Object> holder, String prop)
	{
		return Converter.getAsLong(holder.get(prop));
	}

	private double getAsDouble(Map<String, Object> holder, String prop)
	{
		return Converter.getAsDouble(holder.get(prop));
	}

	private String getAsString(Map<String, Object> holder, String prop)
	{
		return Converter.getAsString(holder.get(prop));
	}
}
