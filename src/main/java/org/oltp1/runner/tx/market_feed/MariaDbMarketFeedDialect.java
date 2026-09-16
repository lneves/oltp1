package org.oltp1.runner.tx.market_feed;

import java.util.Collection;

import org.oltp1.runner.model.Ticker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

/**
 * MariaDB specific implementation of the Market-Feed query strategy.
 */
public class MariaDbMarketFeedDialect implements MarketFeedDialect
{
	private final ObjectMapper json;

	public MariaDbMarketFeedDialect()
	{
		json = new ObjectMapper();
		json.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
	}

	@Override
	public String updateLastTrade()
	{
		return """
				UPDATE last_trade lt
				JOIN JSON_TABLE(:tickers, '$[*]' COLUMNS (
				        symbol      VARCHAR(15) PATH '$.symbol',
				        trade_price DECIMAL(8,2) PATH '$.trade_price',
				        trade_qty   BIGINT      PATH '$.trade_qty')) AS t
				  ON lt.lt_s_symb = t.symbol
				SET lt.lt_price = t.trade_price,
				    lt.lt_vol   = lt.lt_vol + t.trade_qty,
				    lt.lt_dts   = :now_dts;""";
	}

	@Override
	public String getRequestList()
	{
		return """
				WITH tickers AS (
					SELECT
						symbol
						, trade_qty
						, trade_price
					FROM JSON_TABLE(
						:tickers
						, '$[*]' COLUMNS (
							symbol VARCHAR(255) PATH '$.symbol'
							, trade_price DECIMAL(20,6) PATH '$.trade_price'
							, trade_qty INT PATH '$.trade_qty'
						)
					) AS x
				)
				SELECT
					tr_t_id,
					tr_bid_price,
					tr_tt_id,
					tr_qty,
					tr_s_symb
				FROM
					trade_request
					INNER JOIN tickers ON tr_s_symb = symbol
				WHERE
					(tr_tt_id = :tt_buy AND tr_bid_price >= trade_price) OR
					(tr_tt_id = :tt_sell AND tr_bid_price <= trade_price) OR
					(tr_tt_id = :tt_stop AND tr_bid_price >= trade_price)
				ORDER BY tr_t_id FOR UPDATE;
				""";
	}

	@Override
	public String updateTrade()
	{
		return """
				UPDATE
					trade
				SET
					t_dts = :now_dts,
					t_st_id = :status_submitted
				WHERE
					t_id IN (SELECT value FROM JSON_TABLE(:trade_lst, '$[*]' COLUMNS (value bigint PATH '$')) tid);
				""";
	}

	@Override
	public String deleteTradeRequest()
	{
		return """
				DELETE FROM trade_request
				WHERE tr_t_id IN (SELECT value FROM JSON_TABLE(:trade_lst, '$[*]' COLUMNS (value bigint PATH '$')) tid);
				""";
	}

	@Override
	public String insertTradeHistory()
	{
		return """
				INSERT INTO trade_history (th_t_id, th_dts, th_st_id)
				SELECT value AS trade_id, :now_dts, :status_submitted
				FROM JSON_TABLE(:trade_lst, '$[*]' COLUMNS (value bigint PATH '$')) tid
				""";
	}

	@Override
	public String buildTickerJson(Collection<Ticker> tickers)
	{
		try
		{
			return json.writeValueAsString(tickers);
		}
		catch (JsonProcessingException e)
		{
			throw new RuntimeException("Failed to serialize Ticker list to JSON", e);
		}
	}

	@Override
	public String buildTradeIdList(Collection<Long> tradeIdLst)
	{
		try
		{
			return json.writeValueAsString(tradeIdLst);
		}
		catch (JsonProcessingException e)
		{
			throw new RuntimeException("Failed to serialize broker list to JSON", e);
		}
	}
}
