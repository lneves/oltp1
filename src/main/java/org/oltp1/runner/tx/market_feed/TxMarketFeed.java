package org.oltp1.runner.tx.market_feed;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.oltp1.common.ErrorCtx;
import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.db.SqlEngine;
import org.oltp1.runner.perf.TxBase;
import org.oltp1.runner.perf.TxOutput;
import org.oltp1.runner.perf.TxStatsCollector;
import org.oltp1.runner.tx.QueryFactory;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

public class TxMarketFeed extends TxBase
{
	private final Queue<TxMarketFeedInput> mq = new ConcurrentLinkedQueue<>();
	private final ObjectMapper json = new ObjectMapper();

	private final SqlContext sqlCtx;
	private final Sql2o sql2o;

	private final MarketFeedQueries sql;

	public TxMarketFeed(SqlContext sqlCtx, TxStatsCollector mktFeedStats)
	{
		super(mktFeedStats);
		this.sqlCtx = sqlCtx;

		// json.enable(SerializationFeature.INDENT_OUTPUT);
		json.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

		this.sql2o = sqlCtx.getSql2o();
		this.sql = QueryFactory.getQueries(MarketFeedQueries.class, sqlCtx.getSqlEngine());
	}

	public void offer(TxMarketFeedInput txIn)
	{
		mq.offer(txIn);
		super.execute();
	}

	@Override
	protected final TxOutput run()
	{
		final TxMarketFeedOutput txOutput = new TxMarketFeedOutput();

		final TxMarketFeedInput txInput = mq.poll();

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
			String tickerJson = json.writeValueAsString(txInput.tickers);

			num_updated = con
					.createQuery(sql.updateLastTrade())
					.addParameter("tickers", tickerJson)
					.executeUpdate()
					.getResult();

			List<Map<String, Object>> tradeRequests = con
					.createQuery(sql.getRequestList())
					.addParameter("tickers", tickerJson)
					.addParameter("tt_buy", txInput.type_limit_buy.id)
					.addParameter("tt_sell", txInput.type_limit_sell.id)
					.addParameter("tt_stop", txInput.type_stop_loss.id)
					.executeAndFetchTable()
					.asList();

			// collect the trade_id values from the requested list
			List<String> tradeIds = tradeRequests
					.stream()
					.map(r -> ((Number) r.get("tr_t_id")).toString())
					.distinct()
					.collect(Collectors.toList());

			if (tradeIds.size() > 0)
			{
				String tradeIdsLst;

				if (sqlCtx.getSqlEngine() == SqlEngine.MARIADB)
				{
					tradeIdsLst = json.writeValueAsString(tradeIds);
				}
				else
				{
					tradeIdsLst = StringUtils.join(tradeIds, ",");
				}

				// Update trade status to 'submitted'
				con
						.createQuery(sql.updateTrade())
						.addParameter("status_submitted", txInput.status_submitted)
						.addParameter("trade_lst", tradeIdsLst)
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
						.executeUpdate();
			}

			txOutput.trade_requests = tradeRequests;

			con.commit();
			txOutput.num_trades = txInput.tickers.size();
		}
		catch (Throwable t)
		{
			ErrorCtx ectx = new ErrorCtx(t);
			txOutput.setStatus(-1);
			txOutput.setStatusMessage(ectx.toString());
		}

		if (num_updated < unique_symbols)
		{
			txOutput.setStatus(-311);
			txOutput.setStatusMessage("(num_updated < unique_symbols)");
		}

		return txOutput;
	}
}