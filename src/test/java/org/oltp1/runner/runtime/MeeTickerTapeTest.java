package org.oltp1.runner.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.oltp1.runner.model.Ticker;

class MeeTickerTapeTest
{
	private static final class FakePriceSource implements MeeTickerTape.PriceSource
	{
		private final Map<String, Integer> indexBySymbol = new HashMap<>();
		private final List<String> symbols = new ArrayList<>();
		private final List<Double> prices = new ArrayList<>();

		FakePriceSource(String[] symbols, double[] prices)
		{
			for (int i = 0; i < symbols.length; i++)
			{
				this.symbols.add(symbols[i]);
				this.prices.add(prices[i]);
				this.indexBySymbol.put(symbols[i], i);
			}
		}

		@Override
		public int size()
		{
			return symbols.size();
		}

		@Override
		public String symbol(int i)
		{
			return symbols.get(i);
		}

		@Override
		public double price(int i)
		{
			return prices.get(i);
		}

		@Override
		public void update(String symbol, double price)
		{
			Integer i = indexBySymbol.get(symbol);

			if (i != null)
			{
				prices.set(i, price);
			}
		}
	}

	@Test
	void tenRealTickersEmitOneFullFeed()
	{
		FakePriceSource source = new FakePriceSource(
				new String[] { "AAA", "BBB", "CCC" },
				new double[] { 10.0, 20.0, 30.0 });
		List<List<Ticker>> feeds = new ArrayList<>();
		MeeTickerTape tape = new MeeTickerTape(source, feeds::add, 42L);

		for (int i = 0; i < 9; i++)
		{
			tape.addRealTicker("AAA", 11.0, 100);
		}

		assertTrue(feeds.isEmpty(), "9 real + 9 artificial entries must not fill a batch");

		tape.addRealTicker("AAA", 12.0, 200);

		assertEquals(1, feeds.size());
		assertEquals(MeeTickerTape.MAX_FEED_LEN, feeds.get(0).size());
		assertEquals(1, tape.getEmittedFeedCount());

		Set<String> boardSymbols = new HashSet<>(Set.of("AAA", "BBB", "CCC"));
		int realCount = 0;
		int artificialCount = 0;

		for (Ticker ticker : feeds.get(0))
		{
			assertTrue(boardSymbols.contains(ticker.getSymbol()), ticker.getSymbol());

			if (ticker.getTradeQty() == 100 || ticker.getTradeQty() == 200)
			{
				realCount++;
			}
			else
			{
				assertTrue(
						ticker.getTradeQty() == MeeTickerTape.RANDOM_TRADE_QTY_1
								|| ticker.getTradeQty() == MeeTickerTape.RANDOM_TRADE_QTY_2,
						"unexpected artificial qty " + ticker.getTradeQty());
				artificialCount++;
			}
		}

		assertEquals(10, realCount);
		assertEquals(10, artificialCount);
	}

	@Test
	void partialBatchIsDiscardedNotFlushed()
	{
		FakePriceSource source = new FakePriceSource(new String[] { "AAA" }, new double[] { 10.0 });
		List<List<Ticker>> feeds = new ArrayList<>();
		MeeTickerTape tape = new MeeTickerTape(source, feeds::add, 42L);

		for (int i = 0; i < 5; i++)
		{
			tape.addRealTicker("AAA", 10.0 + i, 100);
		}

		assertTrue(feeds.isEmpty());
		assertEquals(10, tape.discardRemaining());
		assertEquals(0, tape.discardRemaining());
		assertTrue(feeds.isEmpty(), "discarding must not emit a partial feed");
		assertEquals(10, tape.getDiscardedEntryCount());
	}

	@Test
	void artificialEntriesUseBoardPrices()
	{
		FakePriceSource source = new FakePriceSource(new String[] { "AAA", "BBB" }, new double[] { 10.0, 20.0 });
		List<List<Ticker>> feeds = new ArrayList<>();
		MeeTickerTape tape = new MeeTickerTape(source, feeds::add, 7L);

		for (int i = 0; i < 10; i++)
		{
			tape.addRealTicker("AAA", 15.0, 100);
		}

		assertEquals(1, feeds.size());

		for (Ticker ticker : feeds.get(0))
		{
			if ("BBB".equals(ticker.getSymbol()))
			{
				assertEquals(20.0, ticker.getTradePrice(), 0.0);
			}
		}
	}

	@Test
	void realTickerUpdatesBoardPrice()
	{
		FakePriceSource source = new FakePriceSource(new String[] { "AAA" }, new double[] { 10.0 });
		MeeTickerTape tape = new MeeTickerTape(source, batch -> {
		}, 7L);

		tape.addRealTicker("AAA", 42.5, 100);

		assertEquals(42.5, source.price(0), 0.0);
	}

	@Test
	void emissionIsDeterministicForTheSameSeed()
	{
		FakePriceSource first = new FakePriceSource(
				new String[] { "AAA", "BBB", "CCC" },
				new double[] { 1.0, 2.0, 3.0 });
		FakePriceSource second = new FakePriceSource(
				new String[] { "AAA", "BBB", "CCC" },
				new double[] { 1.0, 2.0, 3.0 });
		List<List<Ticker>> firstFeeds = new ArrayList<>();
		List<List<Ticker>> secondFeeds = new ArrayList<>();
		MeeTickerTape firstTape = new MeeTickerTape(first, firstFeeds::add, 123L);
		MeeTickerTape secondTape = new MeeTickerTape(second, secondFeeds::add, 123L);

		for (int i = 0; i < 20; i++)
		{
			firstTape.addRealTicker("AAA", 5.0, 100);
			secondTape.addRealTicker("AAA", 5.0, 100);
		}

		assertEquals(2, firstFeeds.size());
		assertEquals(2, secondFeeds.size());

		for (int batch = 0; batch < 2; batch++)
		{
			for (int i = 0; i < MeeTickerTape.MAX_FEED_LEN; i++)
			{
				assertEquals(firstFeeds.get(batch).get(i).getSymbol(), secondFeeds.get(batch).get(i).getSymbol());
				assertEquals(firstFeeds.get(batch).get(i).getTradePrice(), secondFeeds.get(batch).get(i).getTradePrice(), 0.0);
				assertEquals(firstFeeds.get(batch).get(i).getTradeQty(), secondFeeds.get(batch).get(i).getTradeQty());
			}
		}
	}

	@Test
	void emptyPriceBoardStillEmitsRealEntries()
	{
		FakePriceSource source = new FakePriceSource(new String[] {}, new double[] {});
		List<List<Ticker>> feeds = new ArrayList<>();
		MeeTickerTape tape = new MeeTickerTape(source, feeds::add, 1L);

		for (int i = 0; i < MeeTickerTape.MAX_FEED_LEN; i++)
		{
			tape.addRealTicker("AAA", 5.0, 100);
		}

		assertEquals(1, feeds.size());
		assertEquals(MeeTickerTape.MAX_FEED_LEN, feeds.get(0).size());
	}

	@Test
	void twentyRealTickersEmitTwoFullFeeds()
	{
		FakePriceSource source = new FakePriceSource(new String[] { "AAA" }, new double[] { 10.0 });
		List<List<Ticker>> feeds = new ArrayList<>();
		MeeTickerTape tape = new MeeTickerTape(source, feeds::add, 5L);

		for (int i = 0; i < 20; i++)
		{
			tape.addRealTicker("AAA", 10.0, 100);
		}

		assertEquals(2, feeds.size());
		assertFalse(feeds.stream().anyMatch(batch -> batch.size() != MeeTickerTape.MAX_FEED_LEN));
	}
}
