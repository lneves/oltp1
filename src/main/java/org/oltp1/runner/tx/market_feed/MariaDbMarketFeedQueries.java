package org.oltp1.runner.tx.market_feed;

/**
 * MariaDB specific implementation of the Market-Feed query strategy.
 */
public class MariaDbMarketFeedQueries implements MarketFeedQueries
{

	@Override
	public String updateLastTrade()
	{
		return """
				UPDATE last_trade lt
				INNER JOIN 
				(
					WITH tickers AS (
					    SELECT
					        symbol
					        , trade_qty
					        , trade_price
					    FROM JSON_TABLE(
					        :tickers
					        , '$[*]' COLUMNS (
					            symbol VARCHAR(15) PATH '$.symbol'
					            , trade_price DECIMAL(8,2) PATH '$.trade_price'
					            , trade_qty BIGINT PATH '$.trade_qty'
					        )
					    ) AS x
					)
				  SELECT * FROM tickers
				
				) tickers ON lt.lt_s_symb = tickers.symbol
				SET
				    lt.lt_price = tickers.trade_price,
				    lt.lt_vol = lt.lt_vol + tickers.trade_qty,
				    lt.lt_dts = CURRENT_TIMESTAMP;
												""";
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
				    (tr_tt_id = :tt_stop AND tr_bid_price >= trade_price);
												               """;
	}

	@Override
	public String updateTrade()
	{
		return """
				UPDATE
				    trade
				SET
				    t_dts = CURRENT_TIMESTAMP,
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
				SELECT value AS trade_id, CURRENT_TIMESTAMP, :status_submitted
				FROM JSON_TABLE(:trade_lst, '$[*]' COLUMNS (value bigint PATH '$')) tid
				""";
	}
}
