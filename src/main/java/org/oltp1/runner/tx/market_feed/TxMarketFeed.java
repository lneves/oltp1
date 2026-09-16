package org.oltp1.runner.tx.market_feed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.oltp1.common.ErrorCtx;
import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.generator.TxInputGenerator;
import org.oltp1.runner.model.Ticker;
import org.oltp1.runner.runtime.BenchmarkMetrics;
import org.oltp1.runner.runtime.TransactionSpec;
import org.oltp1.runner.runtime.TxBase;
import org.oltp1.runner.runtime.TxOutput;
import org.oltp1.runner.runtime.TxStatsCollector;
import org.oltp1.runner.tx.QueryFactory;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

public class TxMarketFeed extends TxBase
{
	private final Sql2o sql2o;
	private final MarketFeedDialect sql;
	private final TxInputGenerator txInputGen;
	private final Queue<TxMarketFeedInput> mq = new ConcurrentLinkedQueue<>();

	public TxMarketFeed(TxInputGenerator txInputGen, SqlContext sqlCtx, BenchmarkMetrics metrics)
	{
		super(metrics, new TxStatsCollector(TransactionSpec.MARKET_FEED));

		this.txInputGen = txInputGen;
		this.sql2o = sqlCtx.getSql2o();
		this.sql = QueryFactory.getQueries(MarketFeedDialect.class, sqlCtx.getSqlEngine());
	}

	public TxMarketFeedOutput process(TxMarketFeedInput txIn)
	{
		mq.offer(txIn);
		return (TxMarketFeedOutput) super.execute();
	}

	@Override
	protected final TxOutput run()
	{
		final TxMarketFeedOutput txOutput = new TxMarketFeedOutput();

		TxMarketFeedInput txInput = mq.poll();

		if (txInput == null)
		{
			String msg = "TxMarketFeedInput should not be null";
			txOutput.setStatus(-1);
			txOutput.setStatusMessage(msg);
			return txOutput;
		}

		int num_updated = 0;
		int unique_symbols = txInput.unique_symbols;

		try (Connection con = sql2o.beginTransaction())
		{
			final String tickerJson = sql.buildTickerJson(txInput.tickers);
			final List<Ticker> aggregated = aggregateBySymbol(txInput.tickers);
			final String aggTickerJson = sql.buildTickerJson(aggregated);
			final LocalDateTime nowDts = txInputGen.now();

			num_updated = con
					.createQuery(sql.updateLastTrade())
					.addParameter("tickers", aggTickerJson)
					.addParameter("now_dts", nowDts)
					.executeUpdate()
					.getResult();

			if (num_updated != unique_symbols)
			{
				txOutput.setStatus(-311);
				txOutput.setStatusMessage("(num_updated != unique_symbols)");
				con.rollback();
				return txOutput;
			}

			List<Map<String, Object>> tradeRequests = con
					.createQuery(sql.getRequestList())
					.addParameter("tickers", tickerJson)
					.addParameter("tt_buy", txInput.type_limit_buy.id)
					.addParameter("tt_sell", txInput.type_limit_sell.id)
					.addParameter("tt_stop", txInput.type_stop_loss.id)
					.executeAndFetchTable()
					.asList();

			// Duplicate symbols in the ticker batch can return the same
			// trade_request row more than once. Each trade must be submitted to the
			// MEE exactly once, so collapse the list by trade id.
			Map<Long, Map<String, Object>> uniqueTradeRequests = new LinkedHashMap<>();

			for (Map<String, Object> request : tradeRequests)
			{
				long tradeId = ((Number) request.get("tr_t_id")).longValue();
				uniqueTradeRequests.putIfAbsent(tradeId, request);
			}

			tradeRequests = new ArrayList<>(uniqueTradeRequests.values());

			List<Long> tradeIds = new ArrayList<>(uniqueTradeRequests.keySet());

			if (tradeIds.size() > 0)
			{
				String tradeIdsLst = sql.buildTradeIdList(tradeIds);

				// Update trade status to 'submitted'
				con
						.createQuery(sql.updateTrade())
						.addParameter("status_submitted", txInput.status_submitted)
						.addParameter("trade_lst", tradeIdsLst)
						.addParameter("now_dts", nowDts)
						.executeUpdate();

				// Delete the trade_request
				con
						.createQuery(sql.deleteTradeRequest())
						.addParameter("trade_lst", tradeIdsLst)
						.executeUpdate();

				// Insert into trade_history
				con
						.createQuery(sql.insertTradeHistory())
						.addParameter("status_submitted", txInput.status_submitted)
						.addParameter("trade_lst", tradeIdsLst)
						.addParameter("now_dts", nowDts)
						.executeUpdate();
			}

			con.commit();

			txOutput.trade_requests = tradeRequests;
			txOutput.num_trades = txInput.tickers.size();

			// send triggered trades to the Market Exchange Emulator
			// via the SendToMarket interface. This should be done
			// after the related database changes have committed
			// for (j=0; j<rows_sent; j++)
			// {
			// SendToMarketFromFrame(
			// TradeRequestBuffer[j].symbol,
			// TradeRequestBuffer[j].trade_id,
			// TradeRequestBuffer[j].price_quote,
			// TradeRequestBuffer[j].trade_qty,
			// TradeRequestBuffer[j].trade_type
			// );

		}
		catch (Throwable t)
		{
			ErrorCtx ectx = new ErrorCtx(t);
			txOutput.setStatus(-1);
			txOutput.setStatusMessage(ectx.toString());
		}

		return txOutput;
	}

	private static List<Ticker> aggregateBySymbol(Collection<Ticker> tickers)
	{
		Map<String, long[]> qtyBySymbol = new LinkedHashMap<>(); // preserves first-seen order
		Map<String, Double> lastPriceBySymbol = new LinkedHashMap<>();

		for (Ticker t : tickers)
		{
			qtyBySymbol.computeIfAbsent(t.getSymbol(), k -> new long[1])[0] += t.getTradeQty();
			lastPriceBySymbol.put(t.getSymbol(), t.getTradePrice());
		}
		return qtyBySymbol
				.entrySet()
				.stream()
				.map(e -> new Ticker(e.getKey(), lastPriceBySymbol.get(e.getKey()), e.getValue()[0]))
				.toList();
	}
}