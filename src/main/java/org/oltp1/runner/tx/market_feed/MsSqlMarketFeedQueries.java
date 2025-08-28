package org.oltp1.runner.tx.market_feed;

/**
 * Microsoft SQL Server-specific implementation of the Market-Feed query
 * strategy.
 */
public class MsSqlMarketFeedQueries implements MarketFeedQueries
{

	@Override
	public String updateLastTrade()
	{
		return """
				WITH tickers AS (

				SELECT
					symbol,
					trade_price,
					trade_qty
				FROM
					OPENJSON(:tickers)
				WITH (
					symbol NVARCHAR(50),
					trade_price DECIMAL(10,2),
					trade_qty INT
					)
				)
				UPDATE
					last_trade
				SET
					lt_price = tickers.trade_price
					, lt_vol = lt_vol + tickers.trade_qty
					, lt_dts = CURRENT_TIMESTAMP
				FROM
					tickers
				WHERE
					last_trade.lt_s_symb = tickers.symbol;
							   """;
	}

	@Override
	public String getRequestList()
	{
		return """
				WITH tickers AS (

				SELECT
					symbol
					, trade_price
					, trade_qty
				FROM
					OPENJSON(:tickers)
				WITH (
					symbol NVARCHAR(50),
					trade_price DECIMAL(10,2),
					trade_qty INT
					)
				)
				SELECT
					tr_t_id
					, tr_bid_price
					, tr_tt_id
					, tr_qty
					, tr_s_symb
				FROM
					trade_request
					INNER JOIN tickers ON tr_s_symb = symbol
				WHERE
					  (tr_tt_id = :tt_buy AND tr_bid_price >= trade_price) OR
					  (tr_tt_id = :tt_sell AND tr_bid_price <= trade_price) OR
					  (tr_tt_id = :tt_stop AND tr_bid_price >= trade_price)
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
					t_id IN (SELECT CAST(value AS bigint) AS trade_id FROM STRING_SPLIT(:trade_lst,','));
				""";
	}

	@Override
	public String deleteTradeRequest()
	{
		return """
				DELETE FROM trade_request
				WHERE tr_t_id IN (SELECT CAST(value AS bigint) AS trade_id FROM STRING_SPLIT(:trade_lst,','));
				""";
	}

	@Override
	public String insertTradeHistory()
	{
		return """
				INSERT INTO trade_history (th_t_id, th_dts, th_st_id)
				SELECT CAST(value AS bigint), CURRENT_TIMESTAMP, :status_submitted
				FROM STRING_SPLIT(:trade_lst,',');
				""";
	}
}
