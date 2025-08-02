package org.caudexorigo.oltp1.tx.market_watch;

/**
 * Provides the database-agnostic SQL queries for the Market-Watch transaction.
 */
public class DefaultMarketWatchQueries implements MarketWatchQueries
{

	@Override
	public String getPctChangeByAccount()
	{
		return """
				SELECT
					100 * COALESCE(((SUM(new_mkt_cap) / SUM(old_mkt_cap)) -1), 0.0) AS pct_change
				FROM
				(
				SELECT
					(s_num_out * old_price) AS old_mkt_cap,
					(s_num_out * new_price) AS new_mkt_cap
				FROM
					(
					SELECT
						lt_price AS new_price
						, dm_close AS old_price
						, s_num_out
					FROM
						last_trade
						, daily_market
						, security
						, (
							SELECT
								hs_s_symb
							FROM
								holding_summary
							WHERE
								hs_ca_id = :acct_id
							) AS x
					WHERE
						lt_s_symb = x.hs_s_symb
						AND dm_s_symb = x.hs_s_symb
						AND s_symb = x.hs_s_symb
						AND dm_date = :start_date
						AND dm_s_symb = lt_s_symb
						AND dm_s_symb = s_symb
					) AS s
				)
				AS market_watch_fr1
				WHERE old_mkt_cap <> 0;
				               """;
	}

	@Override
	public String getPctChangeByCustomer()
	{
		return """
				SELECT
					100 * COALESCE(((SUM(new_mkt_cap) / SUM(old_mkt_cap)) -1), 0.0) AS pct_change
				FROM
				(
				SELECT
					(s_num_out * old_price) AS old_mkt_cap,
					(s_num_out * new_price) AS new_mkt_cap
				FROM
					(
					SELECT
						lt_price AS new_price
						, dm_close AS old_price
						, s_num_out
					FROM
						last_trade
						, daily_market
						, security
						, (
								SELECT
									wi_s_symb
								FROM
									watch_item
									, watch_list
								WHERE
									wi_wl_id = wl_id
									AND wl_c_id = :cust_id
							) AS x
					WHERE
						lt_s_symb = x.wi_s_symb
						AND dm_s_symb = x.wi_s_symb
						AND s_symb = x.wi_s_symb
						AND dm_date = :start_date
						AND dm_s_symb = lt_s_symb
						AND dm_s_symb = s_symb
					) AS s
				)
				AS market_watch_fr1
				WHERE old_mkt_cap <> 0;
				               """;
	}

	@Override
	public String getPctChangeByIndustry()
	{
		return """
				SELECT
					100 * COALESCE(((SUM(new_mkt_cap) / SUM(old_mkt_cap)) -1), 0.0)  AS pct_change
				FROM
				(
				SELECT
					(s_num_out * old_price) AS old_mkt_cap,
					(s_num_out * new_price) AS new_mkt_cap
				FROM
					(
					SELECT
						lt_price AS new_price
						, dm_close AS old_price
						, s_num_out
					FROM
						last_trade
						, daily_market
						, security
						, (
							SELECT
								s_symb AS x_s_symb
							FROM
								industry
								, company
								, security
							WHERE
								in_name = CAST(:industry_name AS varchar(50))
								AND co_in_id = in_id
								AND co_id BETWEEN :starting_co_id AND :ending_co_id
								AND s_co_id = co_id
							) AS x
					WHERE
						lt_s_symb = x.x_s_symb
						AND dm_s_symb = x.x_s_symb
						AND s_symb = x.x_s_symb
						AND dm_date = :start_date
						AND dm_s_symb = lt_s_symb
						AND dm_s_symb = s_symb
					) AS s
				)
				AS market_watch_fr1
				WHERE old_mkt_cap <> 0;
				               """;
	}
}
