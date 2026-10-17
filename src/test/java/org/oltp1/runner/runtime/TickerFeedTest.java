package org.oltp1.runner.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.oltp1.runner.generator.CRandom;
import org.oltp1.runner.model.TickerEntry;
import org.oltp1.runner.model.TradeStatus;
import org.oltp1.runner.tx.market_feed.TxMarketFeedInput;

/**
 * Tests the Market-Feed batching policy against TPC-E v1.14.0.
 * <p>
 * These are the tests that would have caught the original defect: a ticker feed that published a
 * fixed batch every second, which made Market-Feed volume a function of elapsed time instead of
 * of achieved throughput. Nothing here touches a database; the price process and the batching
 * policy are plain in-memory state.
 *
 * <p>Spec references: Clause 6.3.1 (p. 215) "one for every ten Trade-Results generated";
 * Clause 3.3.3 (p. 91) "Each Market-Feed ticker consists of 20 entries"; Clause 3.2.1.2 (p. 77)
 * {@code max_feed_len = 20}.
 */
class TickerFeedTest
{
	private static final int UNIVERSE = 500;

	private static TickerFeed feed(long seed, double[] prices)
	{
		String[] symbols = new String[UNIVERSE];
		for (int i = 0; i < UNIVERSE; i++)
		{
			symbols[i] = "SYM" + i;
		}
		return new TickerFeed(symbols, prices, new CRandom(seed));
	}

	private static double[] prices(double value)
	{
		double[] p = new double[UNIVERSE];
		java.util.Arrays.fill(p, value);
		return p;
	}

	@Test
	void constantsMatchTheSpecification()
	{
		// max_feed_len
		assertEquals(20, TickerFeed.BATCH_SIZE);
		// one real + one artificial entry per completed trade
		assertEquals(2, TickerFeed.ENTRIES_PER_COMPLETED_TRADE);
		// 20 entries / 2 per trade = one Market-Feed per 10 Trade-Results
		assertEquals(10, TickerFeed.TRADES_PER_BATCH);
	}

	@Test
	void oneMarketFeedPerTenTradeResults()
	{
		// The relationship of Clause 6.3.1 must be an identity, not an approximation: N
		// completed trades must produce exactly N / TRADES_PER_BATCH full batches.
		for (int trades = 10; trades <= 10_000; trades += 10)
		{
			TickerFeed feed = feed(1L, prices(25.0));
			List<TickerFeed.CompletedTrade> completed = new ArrayList<>();

			for (int i = 0; i < trades; i++)
			{
				completed.add(new TickerFeed.CompletedTrade("SYM" + (i % UNIVERSE), 100));
			}

			List<TickerFeed.Batch> batches = new ArrayList<>();
			for (int i = 0; i < completed.size(); i += TickerFeed.TRADES_PER_BATCH)
			{
				batches.add(feed.buildBatch(
						completed.subList(i, i + TickerFeed.TRADES_PER_BATCH)));
			}

			assertEquals(trades / TickerFeed.TRADES_PER_BATCH, batches.size(),
					"batches for " + trades + " completed trades");
			assertEquals(trades / TickerFeed.TRADES_PER_BATCH, feed.batchCount());
		}
	}

	@Test
	void everyBatchCarriesMaxFeedLenEntriesAndTheDistinctSymbolCount()
	{
		TickerFeed feed = feed(7L, prices(25.0));

		for (int i = 0; i < 200; i++)
		{
			List<TickerFeed.CompletedTrade> completed = new ArrayList<>();
			for (int j = 0; j < TickerFeed.TRADES_PER_BATCH; j++)
			{
				// Deliberately reuse a few symbols so the batch has duplicates.
				completed.add(new TickerFeed.CompletedTrade("SYM" + (j % 3), 200));
			}

			TickerFeed.Batch batch = feed.buildBatch(completed);

			assertEquals(TickerFeed.BATCH_SIZE, batch.entries().size());

			// TxMarketFeed rolls the batch back unless the number of LAST_TRADE rows updated
			// equals this number, and updateLastTrade aggregates by symbol, so the count must
			// be the number of distinct symbols in the batch.
			long distinct = batch.entries().stream().map(TickerEntry::symbol).distinct().count();
			assertEquals((int) distinct, batch.uniqueSymbols());
		}
	}

	@Test
	void batchedDuplicatesAggregateToTheSameRowCountTheTransactionWillUpdate()
	{
		// Mirrors TxMarketFeed.aggregateBySymbol + the num_updated != unique_symbols check:
		// summing per-symbol quantities must equal the sum over the whole batch, and the
		// number of rows the UPDATE can touch must equal uniqueSymbols.
		TickerFeed feed = feed(11L, prices(25.0));
		List<TickerFeed.CompletedTrade> completed = List.of(
				new TickerFeed.CompletedTrade("AAA", 100),
				new TickerFeed.CompletedTrade("AAA", 100),
				new TickerFeed.CompletedTrade("BBB", 200),
				new TickerFeed.CompletedTrade("BBB", 200),
				new TickerFeed.CompletedTrade("AAA", 300),
				new TickerFeed.CompletedTrade("CCC", 400),
				new TickerFeed.CompletedTrade("AAA", 100),
				new TickerFeed.CompletedTrade("AAA", 100),
				new TickerFeed.CompletedTrade("BBB", 200),
				new TickerFeed.CompletedTrade("AAA", 100));

		TickerFeed.Batch batch = feed.buildBatch(completed);

		int aggregatedRows = 0;
		java.util.Map<String, Integer> qtyBySymbol = new java.util.LinkedHashMap<>();
		for (TickerEntry e : batch.entries())
		{
			qtyBySymbol.merge(e.symbol(), e.quantity(), Integer::sum);
		}
		aggregatedRows = qtyBySymbol.size();

		assertEquals(aggregatedRows, batch.uniqueSymbols(),
				"uniqueSymbols must equal the number of rows the aggregated UPDATE can touch");
		assertEquals(
				batch.entries().stream().mapToInt(TickerEntry::quantity).sum(),
				qtyBySymbol.values().stream().mapToInt(Integer::intValue).sum(),
				"aggregating must not lose quantity");
	}

	@Test
	void priceStaysInsideTheGeneratorBandAndTraversesIt()
	{
		// The band is the same [20.00, 30.00] the data generator populated LAST_TRADE with,
		// so the feed must never publish a price outside it.
		TickerFeed feed = feed(13L, prices(25.0));

		for (int i = 0; i < 5_000; i++)
		{
			TickerFeed.Batch batch = feed.buildIdleBatch();
			for (TickerEntry e : batch.entries())
			{
				assertTrue(
						e.price().doubleValue() >= TickerFeed.MIN_PRICE
								&& e.price().doubleValue() <= TickerFeed.MAX_PRICE,
						"price out of band: " + e.price());
				assertEquals(2, e.price().scale(), "price must carry the DECIMAL(10,2) scale of LT_PRICE");
			}
		}
	}

	@Test
	void priceActuallyMovesSoLimitOrderTriggersCanFire()
	{
		// A price that never moves would leave every pending limit order pending forever, which
		// is what a reflection-free clamp at the band edge would cause.
		TickerFeed feed = feed(17L, prices(25.0));
		java.util.Set<Double> seen = new java.util.HashSet<>();

		for (int i = 0; i < 5_000; i++)
		{
			seen.addAll(feed.buildIdleBatch().entries().stream()
					.map(e -> e.price().doubleValue()).toList());
		}

		// $10 of range at 0.01 granularity: a large number of distinct prices is reachable.
		assertTrue(seen.size() > 500, "distinct prices seen: " + seen.size());
		assertTrue(seen.stream().anyMatch(p -> p <= TickerFeed.MIN_PRICE + 0.01),
				"the price should reach the bottom of the band");
		assertTrue(seen.stream().anyMatch(p -> p >= TickerFeed.MAX_PRICE - 0.01),
				"the price should reach the top of the band");
	}

	@Test
	void realEntryReportsTheTradeQuantityAndArtificialEntryAReferenceQuantity()
	{
		TickerFeed feed = feed(19L, prices(25.0));

		List<TickerFeed.CompletedTrade> completed = new ArrayList<>();
		for (int i = 0; i < TickerFeed.TRADES_PER_BATCH; i++)
		{
			completed.add(new TickerFeed.CompletedTrade("REAL" + i, 100 * (i + 1)));
		}

		TickerFeed.Batch batch = feed.buildBatch(completed);

		// Entries alternate real, artificial.
		List<Integer> realQuantities = new ArrayList<>();
		for (int i = 0; i < batch.entries().size(); i += 2)
		{
			realQuantities.add(batch.entries().get(i).quantity());
			assertTrue(batch.entries().get(i).symbol().startsWith("REAL"),
					"the real entry must carry the trade's own symbol");
		}

		assertEquals(List.of(100, 200, 300, 400, 500, 600, 700, 800, 900, 1000), realQuantities);

		for (int i = 1; i < batch.entries().size(); i += 2)
		{
			int qty = batch.entries().get(i).quantity();
			assertTrue(qty == 325 || qty == 425,
					"artificial entries use the reference quantities, got " + qty);
		}
	}

	@Test
	void everyEntryIsForAKnownSecurity()
	{
		// Market-Feed requires every distinct symbol of the batch to match a LAST_TRADE row,
		// which is why the universe is loaded from LAST_TRADE in the first place.
		TickerFeed feed = feed(23L, prices(25.0));
		java.util.Set<String> universe = new java.util.HashSet<>();
		for (int i = 0; i < UNIVERSE; i++)
		{
			universe.add("SYM" + i);
		}

		for (int i = 0; i < 200; i++)
		{
			for (TickerEntry e : feed.buildIdleBatch().entries())
			{
				assertTrue(universe.contains(e.symbol()), "unknown symbol " + e.symbol());
			}
		}
	}

	@Test
	void marketFeedInputCarriesTheDistinctSymbolCountTheTransactionChecks()
	{
		TickerFeed feed = feed(29L, prices(25.0));
		List<TickerFeed.CompletedTrade> completed = new ArrayList<>();
		for (int i = 0; i < TickerFeed.TRADES_PER_BATCH; i++)
		{
			completed.add(new TickerFeed.CompletedTrade("SYM" + (i % 4), 100));
		}

		TxMarketFeedInput input = TickerFeed.toInput(feed.buildBatch(completed));

		assertNotNull(input);
		assertEquals(TickerFeed.BATCH_SIZE, input.tickers.size());
		assertTrue(input.unique_symbols > 0);
		assertTrue(input.unique_symbols <= input.tickers.size());
		assertEquals(TradeStatus.SUBMITTED.id, input.status_submitted);
	}

	@Test
	void idleBatchIsRecordedAsAStall()
	{
		TickerFeed feed = feed(31L, prices(25.0));

		assertEquals(0, feed.idleBatchCount());
		feed.buildBatch(List.of(new TickerFeed.CompletedTrade("SYM1", 100)));
		assertEquals(0, feed.idleBatchCount(), "a trade-backed batch is not a stall");

		feed.buildIdleBatch();
		feed.buildIdleBatch();
		assertEquals(2, feed.idleBatchCount());
	}

	@Test
	void buildIsDeterministicForAFixedSeed()
	{
		// Reproducibility matters: the same seed must give the same price series, otherwise a
		// run cannot be compared with a previous one.
		TickerFeed a = feed(4242L, prices(25.0));
		TickerFeed b = feed(4242L, prices(25.0));

		for (int i = 0; i < 50; i++)
		{
			List<TickerFeed.CompletedTrade> completed = List.of(
					new TickerFeed.CompletedTrade("SYM" + i, 100),
					new TickerFeed.CompletedTrade("SYM" + (i + 1), 200),
					new TickerFeed.CompletedTrade("SYM" + (i + 2), 400),
					new TickerFeed.CompletedTrade("SYM" + (i + 3), 800),
					new TickerFeed.CompletedTrade("SYM" + (i + 4), 100),
					new TickerFeed.CompletedTrade("SYM" + (i + 5), 200),
					new TickerFeed.CompletedTrade("SYM" + (i + 6), 400),
					new TickerFeed.CompletedTrade("SYM" + (i + 7), 800),
					new TickerFeed.CompletedTrade("SYM" + (i + 8), 100),
					new TickerFeed.CompletedTrade("SYM" + (i + 9), 200));

			assertEquals(a.buildBatch(completed), b.buildBatch(completed));
		}
	}

	@Test
	void rejectsAnEmptyUniverse()
	{
		org.junit.jupiter.api.Assertions.assertThrows(
				IllegalArgumentException.class,
				() -> new TickerFeed(new String[0], new double[0], new CRandom(1L)));
	}
}
