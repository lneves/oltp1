package org.oltp1.runner.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.oltp1.runner.tx.market_feed.TxMarketFeedOutput;
import org.oltp1.runner.tx.trade_result.TxTradeResultInput;
import org.oltp1.runner.tx.trade_result.TxTradeResultOutput;

class MeeClientTest
{
	@Test
	void triggeredTradesAreDispatchedInsteadOfProcessedInline()
	{
		List<TxTradeResultInput> triggered = new ArrayList<>();

		MeeClient client = new MeeClient(
				marketFeedInput -> marketFeedWith(
						Map.of("tr_t_id", 11L, "tr_bid_price", 12.5, "tr_s_symb", "AAA", "tr_qty", 100),
						Map.of("tr_t_id", 22L, "tr_bid_price", 20.0, "tr_s_symb", "BBB", "tr_qty", 200)),
				tradeResultInput -> {
					fail("triggered Trade-Results must not be processed inline");
					return null;
				},
				triggered::add);

		client.processMarketFeed(null);

		assertEquals(2, triggered.size());
		assertEquals(11L, triggered.get(0).tradeId());
		assertEquals(12.5, triggered.get(0).tradePrice(), 0.0);
		assertEquals("AAA", triggered.get(0).symbol());
		assertEquals(100L, triggered.get(0).tradeQty());
		assertEquals(22L, triggered.get(1).tradeId());
		assertEquals("BBB", triggered.get(1).symbol());
	}

	@Test
	void marketTradeResultUsesTheInjectedProcessor()
	{
		TxTradeResultOutput expected = new TxTradeResultOutput(0);
		List<TxTradeResultInput> seen = new ArrayList<>();

		MeeClient client = new MeeClient(
				marketFeedInput -> new TxMarketFeedOutput(),
				input -> {
					seen.add(input);
					return expected;
				},
				input -> fail("no triggered trades expected"));

		TxTradeResultInput input = new TxTradeResultInput(5L, 10.0, "SYM", 100);

		assertSame(expected, client.processTradeResult(input));
		assertEquals(1, seen.size());
		assertSame(input, seen.get(0));
	}

	@Test
	void marketFeedWithoutTriggeredTradesDoesNothing()
	{
		MeeClient client = new MeeClient(
				marketFeedInput -> new TxMarketFeedOutput(),
				input -> fail("no Trade-Results expected"),
				input -> fail("no triggered trades expected"));

		client.processMarketFeed(null);
	}

	@SafeVarargs
	private static TxMarketFeedOutput marketFeedWith(Map<String, Object>... requests)
	{
		TxMarketFeedOutput output = new TxMarketFeedOutput();
		output.trade_requests = List.of(requests);
		return output;
	}
}
