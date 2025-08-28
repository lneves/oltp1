package org.oltp1.runner.tx.market_watch;

/**
 * Provides the database-agnostic SQL queries for the Market-Watch transaction.
 */
public class DefaultMarketWatchQueries implements MarketWatchQueries
{

	@Override
	public String getPctChangeByAccount()
	{
		return """
				WITH holdings
				AS (
					SELECT hs_s_symb
					FROM holding_summary
					WHERE hs_ca_id = :acct_id
					)
					, prices AS (
					SELECT
						h.hs_s_symb AS symb
						, lt.lt_price AS new_price
						, dm.dm_close AS old_price
						, s.s_num_out
					FROM holdings h
					JOIN last_trade lt ON lt.lt_s_symb = h.hs_s_symb
					JOIN daily_market dm ON dm.dm_s_symb = h.hs_s_symb AND dm.dm_date = :start_date
					JOIN security s ON s.s_symb = h.hs_s_symb
					)
					, market_caps AS (
					SELECT
						(s_num_out * old_price) AS old_mkt_cap
						, (s_num_out * new_price) AS new_mkt_cap
					FROM prices
					)
				SELECT
					100 * COALESCE((SUM(new_mkt_cap) / SUM(old_mkt_cap)) - 1, 0.0) AS pct_change
				FROM
					market_caps
				WHERE old_mkt_cap <> 0;
								""";
	}

	@Override
	public String getPctChangeByCustomer()
	{
		return """
				WITH watch_syms
				AS (
					SELECT wi.wi_s_symb AS symb
					FROM watch_item wi
					JOIN watch_list wl ON wi.wi_wl_id = wl.wl_id
					WHERE wl.wl_c_id = :cust_id
					)
					, prices AS (
					SELECT ws.symb
						, lt.lt_price AS new_price
						, dm.dm_close AS old_price
						, s.s_num_out
					FROM watch_syms ws
					JOIN last_trade lt ON lt.lt_s_symb = ws.symb
					JOIN daily_market dm ON dm.dm_s_symb = ws.symb
						AND dm.dm_date = :start_date
					JOIN security s ON s.s_symb = ws.symb
					)
					, market_caps AS (
					SELECT
						(s_num_out * old_price) AS old_mkt_cap
						, (s_num_out * new_price) AS new_mkt_cap
					FROM prices
					)
				SELECT 100 * COALESCE((SUM(new_mkt_cap) / SUM(old_mkt_cap)) - 1, 0.0) AS pct_change
				FROM market_caps
				WHERE old_mkt_cap <> 0;
								""";
	}

	@Override
	public String getPctChangeByIndustry()
	{
		return """
				WITH industry_syms
				AS (
					SELECT s.s_symb AS symb
					FROM industry i
					JOIN company c ON c.co_in_id = i.in_id
					JOIN security s ON s.s_co_id = c.co_id
					WHERE i.in_name = :industry_name
						AND c.co_id BETWEEN :starting_co_id
							AND :ending_co_id
					)
					, prices AS (
					SELECT
						isy.symb
						, lt.lt_price AS new_price
						, dm.dm_close AS old_price
						, s.s_num_out
					FROM industry_syms isy
					JOIN last_trade lt ON lt.lt_s_symb = isy.symb
					JOIN daily_market dm ON dm.dm_s_symb = isy.symb
						AND dm.dm_date = :start_date
					JOIN security s ON s.s_symb = isy.symb
					)
					, market_caps AS (
					SELECT
						(s_num_out * old_price) AS old_mkt_cap
						, (s_num_out * new_price) AS new_mkt_cap
					FROM prices
					)
				SELECT 100 * COALESCE((SUM(new_mkt_cap) / SUM(old_mkt_cap)) - 1, 0.0) AS pct_change
				FROM market_caps
				WHERE old_mkt_cap <> 0;
								""";
	}
}
