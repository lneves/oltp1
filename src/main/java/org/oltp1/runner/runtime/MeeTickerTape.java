package org.oltp1.runner.runtime;

import java.util.ArrayList;
import java.util.List;

import org.oltp1.runner.generator.CRandom;
import org.oltp1.runner.model.Ticker;

/**
 * Reference-faithful MEE ticker tape. Every completed Trade-Result adds one real
 * ticker entry; each real entry is padded with artificial entries up to
 * {@link #PADDING_LIMIT}, so a full batch contains roughly 50% real and 50%
 * artificial entries and the feed ratio stays near 1 Market-Feed per 10
 * Trade-Results (reference {@code CMEETickerTape}). Batches are emitted when
 * full only; a partial batch is intentionally discarded at shutdown, never
 * flushed.
 * <p>
 * Deviation from the reference: the local MEE has no in-the-money limit-order
 * queue, so padding always uses random artificial entries (the reference first
 * drains queued limit-order triggers into the padding slots).
 */
final class MeeTickerTape
{
	/** Reference: inc/MEETickerTape.h max_feed_len. */
	static final int MAX_FEED_LEN = 20;
	/** Reference: (max_feed_len / 10) - 1; 10 is the Trade-Result to Market-Feed ratio. */
	static final int PADDING_LIMIT = (MAX_FEED_LEN / 10) - 1;
	/** Reference: CMEETickerTape::RANDOM_TRADE_QTY_1 / RANDOM_TRADE_QTY_2. */
	static final int RANDOM_TRADE_QTY_1 = 325;
	static final int RANDOM_TRADE_QTY_2 = 425;
	/** Reference: RNGSeedBaseMEETickerTape from RNGSeeds.h. */
	static final long TICKER_TAPE_SEED = 42065035L;

	interface PriceSource
	{
		int size();

		String symbol(int index);

		double price(int index);

		void update(String symbol, double price);
	}

	interface MarketFeedSink
	{
		void emit(List<Ticker> batch);
	}

	private final List<Ticker> batch = new ArrayList<>();
	private final PriceSource priceSource;
	private final MarketFeedSink sink;
	private final CRandom random;
	private long emittedFeedCount;
	private long discardedEntryCount;

	MeeTickerTape(PriceSource priceSource, MarketFeedSink sink)
	{
		this(priceSource, sink, TICKER_TAPE_SEED);
	}

	MeeTickerTape(PriceSource priceSource, MarketFeedSink sink, long seed)
	{
		this.priceSource = priceSource;
		this.sink = sink;
		this.random = new CRandom(seed);
	}

	/**
	 * Adds one real ticker entry (a completed Trade-Result) followed by its
	 * artificial padding entries. Emits a complete Market-Feed whenever the batch
	 * reaches {@link #MAX_FEED_LEN} entries.
	 */
	synchronized void addRealTicker(String symbol, double price, long qty)
	{
		priceSource.update(symbol, price);
		addToBatch(new Ticker(symbol, price, qty));

		int paddingAdded = 0;
		while (paddingAdded < PADDING_LIMIT && priceSource.size() > 0)
		{
			addToBatch(createArtificialTicker());
			paddingAdded++;
		}
	}

	private Ticker createArtificialTicker()
	{
		int index = random.rndIntRange(0, priceSource.size() - 1);
		long qty = random.rndPercent(50) ? RANDOM_TRADE_QTY_1 : RANDOM_TRADE_QTY_2;

		return new Ticker(priceSource.symbol(index), priceSource.price(index), qty);
	}

	private void addToBatch(Ticker ticker)
	{
		batch.add(ticker);

		if (batch.size() >= MAX_FEED_LEN)
		{
			List<Ticker> fullBatch = List.copyOf(batch);
			batch.clear();
			emittedFeedCount++;
			sink.emit(fullBatch);
		}
	}

	/**
	 * Drops any partially accumulated batch. The reference never flushes a partial
	 * batch at shutdown, so these entries are discarded on purpose.
	 *
	 * @return the number of entries discarded by this call
	 */
	synchronized long discardRemaining()
	{
		long discarded = batch.size();
		discardedEntryCount += discarded;
		batch.clear();
		return discarded;
	}

	synchronized long getEmittedFeedCount()
	{
		return emittedFeedCount;
	}

	/**
	 * Total number of entries discarded across all {@link #discardRemaining()}
	 * calls.
	 */
	synchronized long getDiscardedEntryCount()
	{
		return discardedEntryCount;
	}
}
