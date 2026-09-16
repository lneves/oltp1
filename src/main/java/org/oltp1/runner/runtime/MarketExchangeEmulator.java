package org.oltp1.runner.runtime;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.generator.TxInputGenerator;
import org.oltp1.runner.model.Ticker;
import org.oltp1.runner.model.TradeStatus;
import org.oltp1.runner.model.TradeType;
import org.oltp1.runner.tx.market_feed.TxMarketFeedInput;
import org.oltp1.runner.tx.trade_order.TxTradeOrderResult;
import org.oltp1.runner.tx.trade_result.TxTradeResultInput;
import org.oltp1.runner.tx.trade_result.TxTradeResultOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-process Market Exchange Emulator (MEE) simulation. Executes asynchronous
 * Trade-Result and Market-Feed work on a dedicated ExecutorService.
 */
public class MarketExchangeEmulator implements ExchangeEmulator
{
	private static final int DRAIN_TIMEOUT = 30000;

	private static final Logger log = LoggerFactory.getLogger(MarketExchangeEmulator.class);

	private final ExecutorService executor;

	private final AtomicBoolean accepting = new AtomicBoolean(true);

	private final MeeTickerTape tickerTape;
	private final BenchmarkMetrics metrics;

	private final BlockingQueue<MeeClient> meeClients = new LinkedBlockingQueue<>();

	public MarketExchangeEmulator(SqlContext sqlCtx, TxInputGenerator txInputGen, int numWorkers, BenchmarkMetrics metrics)
	{
		this.metrics = metrics;
		this.executor = Executors
				.newFixedThreadPool(
						numWorkers,
						Thread.ofPlatform().name("mee-worker-", 0).factory());
		this.tickerTape = new MeeTickerTape(new MeePriceBoard(sqlCtx), this::emitMarketFeed);

		for (int i = 0; i < numWorkers; i++)
		{
			final MeeClient meeClient = new MeeClient(sqlCtx, txInputGen, metrics, this::submitTriggeredTradeResult);
			meeClients.offer(meeClient);
		}
	}

	@Override
	public void submitTradeOrderToMarket(TxTradeOrderResult orderResult)
	{
		if (!accepting.get() || orderResult == null)
		{
			return;
		}

		if (orderResult.isMarket())
		{
			// The ticker entry is added when the Trade-Result completes, matching the
			// reference ticker tape (all completed Trade-Results feed the tape).
			submitToTradeResult(
					new TxTradeResultInput(
							orderResult.tradeId(),
							orderResult.requestedPrice(),
							orderResult.symbol(),
							orderResult.tradeQty()));
		}

		// Limit and stop-loss orders are registered in trade_request by Trade-Order
		// and are triggered later by Market-Feed, mirroring the reference MEE.
	}

	/**
	 * Emits one complete ticker batch as a Market-Feed transaction. Used as the
	 * {@link MeeTickerTape} sink; the tape never passes a null or empty batch.
	 */
	private void emitMarketFeed(List<Ticker> batch)
	{
		if ((batch == null) || batch.isEmpty())
		{
			return;
		}

		int uniqueSymbols = (int) batch
				.stream()
				.map(Ticker::getSymbol)
				.distinct()
				.count();

		TxMarketFeedInput txMktFeedIn = new TxMarketFeedInput(
				TradeStatus.SUBMITTED,
				TradeType.LIMIT_BUY,
				TradeType.LIMIT_SELL,
				TradeType.STOP_LOSS,
				batch,
				uniqueSymbols);

		submitToMarketFeed(txMktFeedIn);
	}

	@Override
	public void submitToTradeResult(TxTradeResultInput input)
	{
		if (!accepting.get())
		{
			return;
		}

		enqueueTradeResultTask(input, true);
	}

	/**
	 * Submits a Trade-Result triggered by an already-accepted Market-Feed. Unlike
	 * external submissions these are accepted while draining, because the reference
	 * MEE completes every request derived from an accepted feed.
	 */
	void submitTriggeredTradeResult(TxTradeResultInput input)
	{
		enqueueTradeResultTask(input, false);
	}

	private void enqueueTradeResultTask(TxTradeResultInput input, boolean propagateRejection)
	{
		metrics.incrementPendingCount();

		try
		{
			executor.submit(() -> processTradeResultTask(input));
		}
		catch (RejectedExecutionException e)
		{
			metrics.decrementPendingCount();

			if (propagateRejection)
			{
				throw e;
			}

			log.warn("Dropping triggered Trade-Result for trade {}: MEE executor is shut down", input.tradeId());
		}
	}

	private void processTradeResultTask(TxTradeResultInput input)
	{
		try
		{
			MeeClient c = meeClients.take();
			try
			{
				TxTradeResultOutput result = c.processTradeResult(input);

				if (result.getStatus() >= 0)
				{
					tickerTape.addRealTicker(input.symbol(), input.tradePrice(), input.tradeQty());
				}
			}
			finally
			{
				meeClients.offer(c);
			}
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
		finally
		{
			metrics.decrementPendingCount();
		}
	}

	@Override
	public void submitToMarketFeed(TxMarketFeedInput txInput)
	{
		if (!accepting.get())
		{
			return;
		}

		metrics.incrementPendingCount();

		try
		{
			executor.submit(() -> {
				try
				{
					MeeClient c = meeClients.take();

					try
					{
						c.processMarketFeed(txInput);
					}
					finally
					{
						meeClients.offer(c);
					}
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
				}
				finally
				{
					metrics.decrementPendingCount();
				}
			});

		}
		catch (RejectedExecutionException e)
		{
			metrics.decrementPendingCount();
			throw e;
		}
	}

	@Override
	public void stopAccepting()
	{
		if (!accepting.compareAndSet(true, false))
		{
			return;
		}

		long discarded = tickerTape.discardRemaining();

		log
				.info(
						"\nMEE ticker tape: {} complete Market-Feed batches formed, {} pending entries discarded (partial batches are not flushed, per reference)",
						tickerTape.getEmittedFeedCount(),
						discarded);
	}

	@Override
	public long drain()
	{
		return drain(DRAIN_TIMEOUT);
	}

	@Override
	public long drain(long timeoutMs)
	{
		long deadline = System.currentTimeMillis() + timeoutMs;

		while (metrics.getPendingCount() > 0 && System.currentTimeMillis() < deadline)
		{
			try
			{
				Thread.sleep(50);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				break;
			}
		}
		return metrics.getPendingCount();
	}

	@Override
	public void close()
	{
		stopAccepting();
		drain(DRAIN_TIMEOUT);

		long lateDiscarded = tickerTape.discardRemaining();

		if (lateDiscarded > 0)
		{
			log.info("MEE ticker tape: {} additional entries discarded during shutdown", lateDiscarded);
		}

		executor.shutdown();
		try
		{
			if (!executor.awaitTermination(5, TimeUnit.SECONDS))
			{
				executor.shutdownNow();
			}
		}
		catch (InterruptedException e)
		{
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}
}
