package org.oltp1.runner.runtime;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.oltp1.common.db.JdbcQuery;
import org.oltp1.common.db.SqlContext;
import org.oltp1.runner.generator.ThreadLocalCRandom;
import org.oltp1.runner.generator.TxInputGenerator;
import org.oltp1.runner.model.TradeType;
import org.oltp1.runner.tx.market_feed.TxMarketFeed;
import org.oltp1.runner.tx.market_feed.TxMarketFeedInput;
import org.oltp1.runner.tx.market_feed.TxMarketFeedOutput;
import org.oltp1.runner.tx.trade_order.TxTradeOrder;
import org.oltp1.runner.tx.trade_order.TxTradeOrderResult;
import org.oltp1.runner.tx.trade_result.TxTradeResult;
import org.oltp1.runner.tx.trade_result.TxTradeResultInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-process Market Exchange Emulator (MEE) of the TPC-E specification (section
 * 3.3).
 * <p>
 * The MEE decouples order entry from trade execution. It receives the orders
 * committed by the {@code Trade-Order} transaction, feeds the market with new
 * prices, and dispatches the resulting executions to the {@code Trade-Result}
 * transaction:
 * <ol>
 * <li><b>Process Order</b> - a market order is executed immediately. The order
 * has already been committed in the {@code SUBMITTED} state by
 * {@code Trade-Order}, so {@code Trade-Result} can claim it right away.</li>
 * <li><b>Set Limit Order Trigger</b> - a limit or stop-loss order is not
 * executed here. It stays in {@code TRADE_REQUEST} until a Market-Feed batch
 * moves the security price through its trigger, at which point the
 * {@code Market-Feed} transaction flips it to {@code SUBMITTED} and hands it
 * back to the MEE for execution.</li>
 * <li><b>Ticker feed</b> - every {@link TickerFeed#TRADES_PER_BATCH} completed
 * {@code Trade-Result} transactions a batch of {@link TickerFeed#BATCH_SIZE}
 * ticker entries is generated and applied by one {@code Market-Feed}
 * transaction, and the trades the new prices triggered are dispatched to
 * {@code Trade-Result}.</li>
 * </ol>
 * <p>
 * The feed is event driven rather than periodic, which is what the specification
 * requires: Clause 6.3.1 states that "the number of Market-Feed Transactions is
 * controlled by the CMEE code to be one for every ten Trade-Results generated",
 * and Clause 3.3.3 fixes a ticker at {@code max_feed_len} = 20 entries of which
 * "ten of these entries are a result of trades submitted to the MEE by this
 * brokerage house". Each completed {@code Trade-Result} therefore contributes one
 * real entry plus one artificial entry, and a full batch is exactly ten completed
 * trades. The consequence is that ticker volume &mdash; and therefore the volume
 * of the transactions that drive limit-order triggers &mdash; scales with achieved
 * throughput rather than with elapsed time. See {@link TickerFeed} for the details
 * and for what happens if no {@code Trade-Result} completes.
 * The trigger matching itself is performed by the {@code Market-Feed}
 * transaction, which locks the matching {@code TRADE_REQUEST} rows and returns
 * them. The MEE therefore never matches prices against its own copy of a
 * pending order: doing so would duplicate the SQL logic and would allow the
 * same order to be submitted for execution twice.
 * <h2>Concurrency</h2> A {@link TxTradeResult} hands its input to {@code execute()} through an
 * internal queue that {@code run()} polls back, so a single instance is <b>not</b> safe for
 * concurrent use: one thread could poll up an input another thread had just queued.
 * {@link TxMarketFeed} takes its input as a method argument and holds no mutable state; it is
 * given a per-thread instance only so both MEE transactions are allocated the same way.
 * Every MEE worker thread therefore owns its own pair of transaction instances (see
 * {@link WorkerTx}), and the ticker-feed thread owns the market-feed instance it uses.
 * <h2>Shutdown</h2> {@link #stopAccepting()} first refuses new work and then
 * waits for the ticker feed thread to drain {@link #completedTickerTrades} and
 * publish whatever batches that produces, so no completed {@code Trade-Result}
 * is left without its ticker entry. {@link #drain()} waits for the trade-result
 * queue to empty and {@link #stopAndAwait(long)} terminates the workers. All
 * three are idempotent, and {@link #close()} chains them because the driver
 * closes the MEE from a try-with-resources block.
 */
public class MarketExchangeEmulator implements ExchangeEmulator
{
	private static final Logger log = LoggerFactory.getLogger(MarketExchangeEmulator.class);

	// TPC-E MEE parameters. The ticker feed is event driven, not periodic: a Market-Feed
	// is published for every TickerFeed.TRADES_PER_BATCH completed Trade-Results, which is
	// the "one Market-Feed for every ten Trade-Results" relationship of TPC-E Clause 6.3.1
	// combined with max_feed_len = 20 entries per ticker (Clause 3.2.1.2). The entries and
	// the price process are owned by TickerFeed; see that class for the full rationale.

	/**
	 * How long the ticker-feed thread waits for work in one slice before re-checking the
	 * shutdown flags. Bounds the shutdown latency without busy-waiting.
	 */
	private static final long TICKER_POLL_SLICE_MS = 200L;

	/**
	 * How long the ticker-feed thread is given to notice a stop request and flush whatever
	 * completed Trade-Results are still queued.
	 */
	private static final long TICKER_STOP_TIMEOUT_MS = 5_000L;

	// The reference implementation warns once the pending limit order table grows
	// past this size: it means the ticker feed is not moving prices far enough to
	// trigger the orders the clients keep submitting.
	private static final long PENDING_LIMIT_ORDER_WARN_THRESHOLD = 1_000L;

	// LAST_TRADE rows are streamed in batches while loading the security universe.
	private static final int UNIVERSE_FETCH_SIZE = 1000;

	// The trade-result queue is bounded so that a backlog cannot grow without limit
	// and so that draining at the end of a run is guaranteed to terminate.
	private static final int MIN_TRADE_RESULT_QUEUE_CAPACITY = 1024;
	private static final int TRADE_RESULT_QUEUE_CAPACITY_PER_THREAD = 512;

	// The completed-Trade-Result hand-off to the ticker feed is bounded too. The ticker thread
	// drains it continuously, so overflow means it is genuinely stuck; the offer is then dropped
	// and counted rather than blocking a Trade-Result worker or growing without limit.
	private static final int TICKER_QUEUE_CAPACITY = 4096;

	// How long a worker waits on the queue before re-checking the stop flag.
	private static final long WORKER_POLL_MS = 200L;

	private static final long DEFAULT_DRAIN_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(30);
	private static final long MAX_CLOSE_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5);

	// Columns of the TRADE_REQUEST rows returned by the Market-Feed transaction.
	private static final String COL_TRADE_ID = "tr_t_id";
	private static final String COL_BID_PRICE = "tr_bid_price";
	private static final String COL_TRADE_TYPE_ID = "tr_tt_id";
	private static final String COL_TRADE_QTY = "tr_qty";
	private static final String COL_SYMBOL = "tr_s_symb";

	/**
	 * The transaction instances owned by a single MEE worker thread. Because they
	 * are created from a {@link ThreadLocal}, no two threads can ever reach the
	 * same instance, which is what keeps the internal input queue of
	 * {@link TxMarketFeed} and {@link TxTradeResult} correct.
	 */
	private record WorkerTx(TxMarketFeed marketFeed, TxTradeResult tradeResult)
	{
	}

	private final BenchmarkMetrics metrics;
	private final int meeWorkers;

	/**
	 * The security universe the ticker feed draws from, together with the price this emulator
	 * last published for each. Seeded from {@code LAST_TRADE} and then owned exclusively by the
	 * ticker-feed thread, which is where the {@link TickerFeed} that mutates it is created.
	 */
	private final String[] symbols;
	private final double[] lastPrice;

	/** The market-feed transaction owned by the ticker feed thread. */
	private final TxMarketFeed tickerFeedTx;

	private final ThreadLocal<WorkerTx> workerTx;

	private final BlockingQueue<TxTradeResultInput> tradeResultQueue;

	/**
	 * Completed Trade-Results waiting to become ticker entries. This is the hand-off between the
	 * MEE result workers (producers) and the ticker-feed thread (the single consumer), and it is
	 * what makes Market-Feed volume proportional to achieved throughput: the ticker thread
	 * publishes one Market-Feed for every {@link TickerFeed#TRADES_PER_BATCH} items drained.
	 */
	private final LinkedBlockingQueue<TickerFeed.CompletedTrade> completedTickerTrades = new LinkedBlockingQueue<>(TICKER_QUEUE_CAPACITY);

	private final ExecutorService resultPool;
	private Thread tickerFeedThread;

	private final AtomicBoolean accepting = new AtomicBoolean(true);
	private final AtomicBoolean stopRequested = new AtomicBoolean(false);
	private final AtomicBoolean closed = new AtomicBoolean(false);

	/** Trade results queued or being processed, kept in step with the metrics. */
	private final AtomicInteger outstanding = new AtomicInteger(0);

	/** Trade ids of the limit orders still waiting for a price trigger. */
	private final Set<Long> pendingLimitOrders = ConcurrentHashMap.newKeySet();

	/**
	 * True once the ticker feed has had to fall back to purely artificial batches because no
	 * Trade-Result completed. Surfaced through {@link #getTickerFeedStats()}, because it means
	 * Market-Feed volume was not proportional to throughput for part of the run.
	 */
	private final AtomicBoolean tickerFeedStalled = new AtomicBoolean(false);

	// Ticker-feed counters. The offer side is written by the MEE result workers, the batch side by
	// the ticker-feed thread, so all of them are atomic.
	private final AtomicLong tickerTradesOffered = new AtomicLong(0);
	private final AtomicLong tickerTradesDropped = new AtomicLong(0);
	private final AtomicLong tickerBatchesPublished = new AtomicLong(0);
	private final AtomicLong tickerEntriesPublished = new AtomicLong(0);
	private final AtomicLong tickerIdleBatches = new AtomicLong(0);

	private final AtomicLong triggeredTrades = new AtomicLong(0);
	private final AtomicLong executedTrades = new AtomicLong(0);
	private final AtomicLong invalidOrders = new AtomicLong(0);
	private final AtomicLong rejectedSubmissions = new AtomicLong(0);

	/**
	 * A snapshot of the ticker feed's counters, for the run summary and the progress report.
	 *
	 * @param batches Market-Feed transactions attempted, including any that failed
	 * @param entries total ticker entries in those batches
	 * @param completedTrades Trade-Results that offered a real ticker entry; excludes the tail
	 * trades that ran after the feed was stopped, which never produced one
	 * @param droppedOffers completed Trade-Results the feed could not queue
	 * @param idleBatches batches published with no completed Trade-Result behind them
	 * @param stalled whether {@code idleBatches} fired at all
	 */
	public record TickerFeedStats(
			long batches,
			long entries,
			long completedTrades,
			long droppedOffers,
			long idleBatches,
			boolean stalled)
	{
	}

	/**
	 * How long the ticker feed may go without a completed Trade-Result before it publishes a
	 * purely artificial batch. Kept in the emulator as well because the loop that enforces it
	 * lives here; {@link TickerFeed} owns the batches themselves.
	 */
	private static final long TICKER_IDLE_TIMEOUT_NANOS = TickerFeed.IDLE_TIMEOUT_NANOS;

	public MarketExchangeEmulator(SqlContext sqlCtx, TxInputGenerator txInputGen, int meeWorkers, BenchmarkMetrics metrics)
	{
		super();
		this.metrics = metrics;
		this.meeWorkers = Math.max(1, meeWorkers);

		List<String> loadedSymbols = new ArrayList<>();
		List<Double> loadedPrices = new ArrayList<>();
		loadSecurityUniverse(sqlCtx, loadedSymbols, loadedPrices);

		this.symbols = loadedSymbols.toArray(new String[0]);
		this.lastPrice = new double[loadedPrices.size()];
		for (int i = 0; i < this.lastPrice.length; i++)
		{
			this.lastPrice[i] = loadedPrices.get(i);
		}

		log.info(
				"MEE ticker feed loaded {} securities from LAST_TRADE; a Market-Feed is published "
						+ "for every {} completed Trade-Results ({} entries per ticker)",
				this.symbols.length,
				TickerFeed.TRADES_PER_BATCH,
				TickerFeed.BATCH_SIZE);

		// Owned by the ticker feed thread only.
		this.tickerFeedTx = new TxMarketFeed(txInputGen, sqlCtx, metrics);

		// One transaction pair per worker thread, see the class comment.
		this.workerTx = ThreadLocal
				.withInitial(
						() -> new WorkerTx(
								new TxMarketFeed(txInputGen, sqlCtx, metrics),
								new TxTradeResult(txInputGen, sqlCtx, metrics)));

		this.tradeResultQueue = new ArrayBlockingQueue<>(
				Math.max(MIN_TRADE_RESULT_QUEUE_CAPACITY, this.meeWorkers * TRADE_RESULT_QUEUE_CAPACITY_PER_THREAD));

		this.resultPool = Executors
				.newFixedThreadPool(this.meeWorkers, Thread.ofPlatform().name("mee-result-", 0).factory());

		for (int i = 0; i < this.meeWorkers; i++)
		{
			resultPool.execute(this::runResultWorker);
		}

		this.tickerFeedThread = Thread
				.ofPlatform()
				.name("mee-ticker-feed")
				.start(this::runTickerFeed);
	}

	/**
	 * Loads the security universe the ticker feed draws its quotes from. Only
	 * securities that already have a {@code LAST_TRADE} row are usable, because the
	 * {@code Market-Feed} transaction requires every ticker of a batch to update an
	 * existing row and rejects the batch otherwise.
	 */
	private void loadSecurityUniverse(SqlContext sqlCtx, List<String> outSymbols, List<Double> outPrices)
	{
		final String query = """
				SELECT
					lt_s_symb
					, lt_price
				FROM
					last_trade;
				""";

		new JdbcQuery(sqlCtx).executeQuery(query, UNIVERSE_FETCH_SIZE, rs -> {
			String symbol = rs.getString("lt_s_symb");
			double price = rs.getDouble("lt_price");

			if (symbol != null && !symbol.isBlank() && price > 0.0)
			{
				outSymbols.add(symbol);
				outPrices.add(price);
			}
		});

		if (outSymbols.isEmpty())
		{
			throw new IllegalStateException(
					"The MEE ticker feed found no security with a LAST_TRADE row; LAST_TRADE must be populated before running a benchmark");
		}
	}

	// ------------------------------------------------------------------ //
	// ExchangeEmulator: order intake
	// ------------------------------------------------------------------ //

	@Override
	public void submitTradeOrderToMarket(TxTradeOrderResult orderResult)
	{
		if (orderResult.isMarket())
		{
			// Process Order: a market order executes against the price that
			// Trade-Order already recorded, so Trade-Result can claim it right away.
			enqueueExternalTradeResult(
					toTradeResultInput(
							BigDecimal.valueOf(orderResult.requestedPrice()),
							orderResult.symbol(),
							orderResult.tradeId(),
							orderResult.tradeQty(),
							orderResult.tradeTypeId()),
					orderResult.tradeId());
		}
		else
		{
			// Set Limit Order Trigger: the order stays in TRADE_REQUEST and is
			// executed by whichever Market-Feed batch first moves the security
			// price through the trigger. Only track it here, for reporting.
			requireAccepting("set a limit order trigger");

			pendingLimitOrders.add(orderResult.tradeId());

			int pending = pendingLimitOrders.size();
			if (pending > PENDING_LIMIT_ORDER_WARN_THRESHOLD)
			{
				log
						.warn(
								"MEE has {} limit orders waiting for a price trigger; the ticker feed may not be moving prices far enough",
								pending);
			}
		}
	}

	@Override
	public void submitToTradeResult(TxTradeResultInput input)
	{
		enqueueExternalTradeResult(input, input.tradeId());
	}

	@Override
	public void submitToMarketFeed(TxMarketFeedInput txInput)
	{
		requireAccepting("submit a market feed batch");

		submit(
				() -> runMarketFeed(workerTx.get().marketFeed(), txInput),
				"submit a market feed batch");
	}

	// ------------------------------------------------------------------ //
	// ExchangeEmulator: shutdown
	// ------------------------------------------------------------------ //

	@Override
	public void stopAccepting()
	{
		if (!accepting.compareAndSet(true, false))
		{
			return;
		}

		log.info("MEE stopped accepting new work at {} pending trade results", outstanding.get());

		// The ticker feed thread checks the flag every TICKER_POLL_SLICE_MS and then flushes
		// whatever completed Trade-Results are still queued, so no Market-Feed is lost and no
		// completed trade is left without its ticker entry. It is joined rather than interrupted,
		// to let a batch that is already running commit and dispatch its triggered trades.
		Thread thread = tickerFeedThread;

		if (thread != null)
		{
			try
			{
				thread.join(TICKER_STOP_TIMEOUT_MS);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}

			if (thread.isAlive())
			{
				log.warn("MEE ticker feed thread did not stop within {} ms", TICKER_STOP_TIMEOUT_MS);
			}
		}
	}

	@Override
	public long drain()
	{
		return drain(DEFAULT_DRAIN_TIMEOUT_MS);
	}

	@Override
	public long drain(long timeoutMs)
	{
		// Refusing new work first is what makes the backlog finite.
		stopAccepting();

		final long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(Math.max(timeoutMs, 0L));

		while (outstanding.get() > 0)
		{
			if (System.nanoTime() >= deadline)
			{
				break;
			}

			try
			{
				TimeUnit.MILLISECONDS.sleep(Math.min(WORKER_POLL_MS, 50L));
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				break;
			}
		}

		return outstanding.get();
	}

	@Override
	public void stopAndAwait(long timeoutMs)
	{
		stopAccepting();

		// The workers leave once the queue has run dry, so items queued before the
		// stop request are still executed.
		stopRequested.set(true);
		resultPool.shutdown();

		try
		{
			if (!resultPool.awaitTermination(Math.max(timeoutMs, 0L), TimeUnit.MILLISECONDS))
			{
				resultPool.shutdownNow();

				log.warn("MEE did not stop within {} ms", timeoutMs);
			}
		}
		catch (InterruptedException e)
		{
			resultPool.shutdownNow();
			Thread.currentThread().interrupt();
		}
		finally
		{
			// A batch that was still running when the workers were told to stop can
			// have queued a trade result that no worker is left to pick up.
			discardRemaining();
		}
	}

	@Override
	public void close()
	{
		if (closed.compareAndSet(false, true))
		{
			try
			{
				stopAndAwait(MAX_CLOSE_TIMEOUT_MS);
			}
			finally
			{
				logSummary();
			}
		}
	}

	// ------------------------------------------------------------------ //
	// Ticker feed
	// ------------------------------------------------------------------ //

	/**
	 * The TPC-E ticker feed loop.
	 * <p>
	 * The feed is event driven: a Market-Feed is published for every
	 * {@link TickerFeed#TRADES_PER_BATCH} completed Trade-Results, so the ticker rate is
	 * proportional to achieved throughput rather than to elapsed time (TPC-E Clause 6.3.1 and
	 * Clause 3.3.3). Prices chain off the prices published before, because the {@link TickerFeed}
	 * this thread owns holds the mutable price state, and every quote is for a security that is
	 * known to have a {@code LAST_TRADE} row.
	 * <p>
	 * If no Trade-Result completes for {@link TickerFeed#IDLE_TIMEOUT_MS} the thread publishes a
	 * purely artificial batch so prices keep moving, and records it. That only happens before the
	 * first Trade-Order of a run completes or if the client pool stalls; a healthy run never
	 * takes that path, and {@link #getTickerFeedStats()} reports it.
	 */
	private void runTickerFeed()
	{
		final TickerFeed feed = new TickerFeed(symbols, lastPrice, ThreadLocalCRandom.get());
		final List<TickerFeed.CompletedTrade> batch = new ArrayList<>(TickerFeed.TRADES_PER_BATCH);
		long idleSince = System.nanoTime();
		boolean interrupted = false;

		while (!interrupted)
		{
			try
			{
				// While shutting down, poll without blocking so the queue is drained quickly.
				TickerFeed.CompletedTrade trade = pollCompletedTickerTrade(accepting.get() ? TICKER_POLL_SLICE_MS : 0L);

				if (trade != null)
				{
					batch.add(trade);
					if (batch.size() < TickerFeed.TRADES_PER_BATCH)
					{
						continue;
					}
				}
				else if (batch.isEmpty())
				{
					if (!accepting.get())
					{
						break;
					}

					if (System.nanoTime() - idleSince >= TICKER_IDLE_TIMEOUT_NANOS)
					{
						publishTickerBatch(feed.buildIdleBatch());
						idleSince = System.nanoTime();
					}

					continue;
				}

				publishTickerBatch(feed.buildBatch(batch));
				batch.clear();
				idleSince = System.nanoTime();
			}
			catch (InterruptedException e)
			{
				interrupted = true;
				Thread.currentThread().interrupt();
			}
			catch (Throwable t)
			{
				if (!accepting.get())
				{
					break;
				}

				log.error("MEE ticker feed failed to publish a batch", t);
				batch.clear();
			}
		}

		// Final flush. Clause 3.3.3 permits a Market-Feed to carry fewer than max_feed_len
		// entries, so the completed Trade-Results left over are not thrown away.
		if (!batch.isEmpty() && !interrupted)
		{
			publishTickerBatch(feed.buildBatch(batch));
		}

		tickerIdleBatches.set(feed.idleBatchCount());

		if (tickerFeedStalled.compareAndSet(false, feed.idleBatchCount() > 0))
		{
			log.error(
					"MEE ticker feed had to publish {} artificial batches because no Trade-Result "
							+ "completed; Market-Feed volume was not proportional to throughput for "
							+ "part of this run",
					feed.idleBatchCount());
		}

		log.info(
				"MEE ticker feed thread stopped after {} batches ({} entries, {} completed trades, {} dropped)",
				feed.batchCount(),
				feed.entryCount(),
				tickerTradesOffered.get(),
				tickerTradesDropped.get());
	}

	/**
	 * Records a completed Trade-Result so its ticker entry lands in the next Market-Feed batch.
	 * Called from every MEE result worker after the transaction has run.
	 * <p>
	 * Offered for every processed Trade-Result, matching
	 * {@code CMEETradingFloor::SendTradeResult}, which adds its ticker entry unconditionally
	 * after the transaction runs. That keeps the "one Market-Feed for every ten Trade-Results"
	 * relationship of Clause 6.3.1 exact rather than making it depend on the success rate.
	 * <p>
	 * A no-op once the feed has stopped. {@link #stopAccepting()} joins the ticker-feed thread
	 * <em>before</em> {@link #drain()} lets the tail Trade-Results finish, so the last few trades
	 * of a run deliberately produce no ticker entry: the run is over and the Market-Feed count
	 * should not include batches that could never be published.
	 * <p>
	 * Otherwise it never blocks: if the ticker thread has genuinely stopped draining, the entry is
	 * dropped and counted so a single summary line can report it.
	 */
	private void offerCompletedTickerTrade(String symbol, int quantity)
	{
		if (!accepting.get())
		{
			return;
		}

		tickerTradesOffered.incrementAndGet();

		if (!completedTickerTrades.offer(new TickerFeed.CompletedTrade(symbol, quantity)))
		{
			tickerTradesDropped.incrementAndGet();
		}
	}

	private TickerFeed.CompletedTrade pollCompletedTickerTrade(long timeoutMs) throws InterruptedException
	{
		return completedTickerTrades.poll(timeoutMs, TimeUnit.MILLISECONDS);
	}

	private void publishTickerBatch(TickerFeed.Batch batch)
	{
		tickerBatchesPublished.incrementAndGet();
		tickerEntriesPublished.addAndGet(batch.entries().size());

		try
		{
			runMarketFeed(tickerFeedTx, TickerFeed.toInput(batch));
		}
		catch (Throwable t)
		{
			if (!accepting.get())
			{
				return;
			}

			log.error("MEE ticker feed failed to publish a batch", t);
		}
	}

	/**
	 * Applies a ticker batch and dispatches every trade it triggered. The
	 * transaction commits before the trades are dispatched, so each
	 * {@code Trade-Result} sees its order in the {@code SUBMITTED} state.
	 */
	private void runMarketFeed(TxMarketFeed marketFeed, TxMarketFeedInput txInput)
	{
		TxMarketFeedOutput txOutput = marketFeed.process(txInput);

		if (txOutput.getStatus() < 0 || txOutput.trade_requests == null)
		{
			return;
		}

		for (Map<String, Object> request : txOutput.trade_requests)
		{
			triggeredTrades.incrementAndGet();

			long tradeId = ((Number) request.get(COL_TRADE_ID)).longValue();

			// The order has been triggered, so it is no longer waiting for a price.
			pendingLimitOrders.remove(tradeId);

			try
			{
				enqueueTradeResult(
						toTradeResultInput(
								BigDecimal.valueOf(((Number) request.get(COL_BID_PRICE)).doubleValue()),
								(String) request.get(COL_SYMBOL),
								tradeId,
								((Number) request.get(COL_TRADE_QTY)).longValue(),
								(String) request.get(COL_TRADE_TYPE_ID)),
						tradeId);
			}
			catch (RuntimeException e)
			{
				// The order is already SUBMITTED, so dropping it here would leave it
				// stuck. Count it and let the caller decide how loud to be.
				invalidOrders.incrementAndGet();
				log.error("MEE cannot execute triggered trade {}: {}", tradeId, e.toString());
			}
		}
	}

	// ------------------------------------------------------------------ //
	// Trade-Result workers
	// ------------------------------------------------------------------ //

	private void runResultWorker()
	{
		while (true)
		{
			TxTradeResultInput txInput;

			try
			{
				txInput = tradeResultQueue.poll(WORKER_POLL_MS, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				break;
			}

			if (txInput == null)
			{
				if (stopRequested.get())
				{
					break;
				}

				continue;
			}

			try
			{
				workerTx.get().tradeResult().process(txInput);
				executedTrades.incrementAndGet();

				// One real ticker entry per completed Trade-Result, which is what makes the
				// Market-Feed rate proportional to achieved throughput.
				offerCompletedTickerTrade(txInput.symbol(), txInput.quantity());
			}
			catch (Throwable t)
			{
				log.error("MEE trade-result worker failed on trade {}", txInput.tradeId(), t);
			}
			finally
			{
				releaseOutstanding();
			}
		}
	}

	// ------------------------------------------------------------------ //
	// Helpers
	// ------------------------------------------------------------------ //

	/**
	 * Queues a trade result coming from outside the emulator, refusing it once the
	 * emulator has stopped accepting work. {@link TxTradeOrder} relies on the
	 * rejection to report an order that it has already committed but that will
	 * never be executed.
	 */
	private void enqueueExternalTradeResult(TxTradeResultInput txInput, long tradeId)
	{
		requireAccepting("submit trade " + tradeId + " for execution");

		enqueueTradeResult(txInput, tradeId);
	}

	/**
	 * Queues a trade result. Dispatches from a Market-Feed batch are not subject to
	 * the accepting check: {@link #stopAccepting()} waits for the batch that is
	 * already running, and the orders it triggered are committed in the
	 * {@code SUBMITTED} state, so they have to be executed rather than dropped.
	 */
	private void enqueueTradeResult(TxTradeResultInput txInput, long tradeId)
	{
		// Counted before the hand-off, so that the progress monitor shows an item
		// as pending even while the queue is full and this thread is waiting.
		outstanding.incrementAndGet();

		if (metrics != null)
		{
			metrics.incrementPendingCount();
		}

		try
		{
			tradeResultQueue.put(txInput);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			releaseOutstanding();
			throw new RejectedExecutionException("Interrupted while queueing trade " + tradeId + " for execution", e);
		}
	}

	private void releaseOutstanding()
	{
		if (outstanding.decrementAndGet() < 0)
		{
			throw new IllegalStateException("MEE outstanding trade result count went below zero");
		}

		if (metrics != null)
		{
			metrics.decrementPendingCount();
		}
	}

	/**
	 * Drops whatever is still queued when the workers are forced to stop, so that
	 * the reported pending count matches reality.
	 */
	private void discardRemaining()
	{
		TxTradeResultInput discarded;

		while ((discarded = tradeResultQueue.poll()) != null)
		{
			releaseOutstanding();
			log.warn("MEE discarded unexecuted trade {}", discarded.tradeId());
		}
	}

	private void submit(Runnable task, String description)
	{
		try
		{
			resultPool.execute(task);
		}
		catch (RejectedExecutionException e)
		{
			rejectedSubmissions.incrementAndGet();
			throw new RejectedExecutionException("MEE refused to " + description, e);
		}
	}

	private void requireAccepting(String description)
	{
		if (!accepting.get())
		{
			rejectedSubmissions.incrementAndGet();
			throw new RejectedExecutionException("MEE is shutting down; it can no longer " + description);
		}
	}

	private TxTradeResultInput toTradeResultInput(BigDecimal requestedPrice, String symbol, long tradeId, long quantity, String tradeTypeId)
	{
		TradeType tradeType = TradeType.fromId(tradeTypeId);

		if (tradeType == null)
		{
			throw new IllegalArgumentException("Unknown trade type id '" + tradeTypeId + "' for trade " + tradeId);
		}

		return new TxTradeResultInput(requestedPrice, symbol, tradeId, (int) quantity, tradeType);
	}

	/**
	 * A point-in-time view of the ticker feed. Exposed so the driver can report whether the
	 * Market-Feed stream really did track the Trade-Result stream.
	 */
	public TickerFeedStats getTickerFeedStats()
	{
		return new TickerFeedStats(
				tickerBatchesPublished.get(),
				tickerEntriesPublished.get(),
				tickerTradesOffered.get(),
				tickerTradesDropped.get(),
				tickerIdleBatches.get(),
				tickerFeedStalled.get());
	}

	private void logSummary()
	{
		TickerFeedStats feed = getTickerFeedStats();

		log
				.info(
						"MEE summary: {} ticker batches ({} entries, one per {} completed trades), "
								+ "{} triggered trades, {} trades executed, {} limit orders still pending, "
								+ "{} invalid orders, {} rejected submissions, {} unexecuted at shutdown",
						feed.batches(),
						feed.entries(),
						TickerFeed.TRADES_PER_BATCH,
						triggeredTrades.get(),
						executedTrades.get(),
						pendingLimitOrders.size(),
						invalidOrders.get(),
						rejectedSubmissions.get(),
						outstanding.get());

		if (feed.droppedOffers() > 0)
		{
			log
					.warn(
							"MEE ticker feed dropped {} of {} completed trades because the feed queue was full",
							feed.droppedOffers(),
							feed.completedTrades());
		}

		if (feed.idleBatches() > 0)
		{
			log
					.warn(
							"MEE ticker feed published {} of {} batches with no completed trade behind them; "
									+ "Market-Feed volume did not track Trade-Result throughput for that period",
							feed.idleBatches(),
							feed.batches());
		}
	}
}
