package org.oltp1.runner.tx.security_detail;

/**
 * Provides the database-agnostic SQL queries for the Security-Detail
 * transaction.
 */
public class DefaultSecurityDetailQueries implements SecurityDetailQueries
{

	@Override
	public String getInfo1()
	{
		return """
				SELECT
					s.s_name AS s_name
					, c.co_id AS co_id
					, c.co_name AS co_name
					, c.co_sp_rate AS sp_rate
					, c.co_ceo AS ceo_name
					, c.co_desc AS co_desc
					, c.co_open_date AS open_date
					, c.co_st_id AS co_st_id
					, ca.ad_line1 AS co_ad_line1
					, ca.ad_line2 AS co_ad_line2
					, zca.zc_town AS co_ad_town
					, zca.zc_div AS co_ad_div
					, ca.ad_zc_code AS co_ad_zip
					, ca.ad_ctry AS co_ad_ctry
					, s.s_num_out AS num_out
					, s.s_start_date AS start_date
					, s.s_exch_date AS exch_date
					, s.s_pe AS pe_ratio
					, s.s_52wk_high AS _52_wk_high
					, s.s_52wk_high_date AS _52_wk_high_date
					, s.s_52wk_low AS _52_wk_low
					, s.s_52wk_low_date AS _52_wk_low_date
					, s.s_dividend AS divid
					, s.s_yield AS yield
					, zea.zc_div AS ex_ad_div
					, ea.ad_ctry AS ex_ad_ctry
					, ea.ad_line1 AS ex_ad_line1
					, ea.ad_line2 AS ex_ad_line2
					, zea.zc_town AS ex_ad_town
					, ea.ad_zc_code AS ex_ad_zip
					, ex.ex_close AS ex_close
					, ex.ex_desc AS ex_desc
					, ex.ex_name AS ex_name
					, ex.ex_num_symb AS ex_num_symb
					, ex.ex_open AS ex_open
				FROM
					security AS s
					JOIN company AS c ON c.co_id = s.s_co_id
					JOIN address AS ca ON ca.ad_id = c.co_ad_id
					JOIN zip_code AS zca ON zca.zc_code = ca.ad_zc_code
					JOIN exchange AS ex ON ex.ex_id = s.s_ex_id
					JOIN address AS ea ON ea.ad_id = ex.ex_ad_id
					JOIN zip_code AS zea ON zea.zc_code = ea.ad_zc_code
				WHERE
					s.s_symb = CAST(:symbol AS varchar(15));
								""";
	}

	@Override
	public String getInfo2()
	{
		return """
				SELECT
					c.co_name
					, i.in_name
				FROM
					company_competitor AS cc
					JOIN company AS c ON c.co_id = cc.cp_comp_co_id
					JOIN industry AS i ON i.in_id = cc.cp_in_id
				WHERE
					cc.cp_co_id = :co_id
				ORDER BY c.co_name OFFSET 0 ROWS FETCH NEXT 3 ROWS ONLY;
								""";
	}

	@Override
	public String getInfo3()
	{
		return """
				SELECT
					fi_year AS year
					, fi_qtr AS qtr
					, fi_qtr_start_date AS start_date
					, fi_revenue AS rev
					, fi_net_earn AS net_earn
					, fi_basic_eps AS basic_eps
					, fi_dilut_eps AS dilut_eps
					, fi_margin AS margin
					, fi_inventory AS invent
					, fi_assets AS assets
					, fi_liability AS liab
					, fi_out_basic AS out_basic
					, fi_out_dilut AS out_dilut
				FROM
					financial
				WHERE
					fi_co_id = :co_id
				ORDER BY
					fi_year ASC, fi_qtr
				OFFSET 0 ROWS FETCH NEXT 20 ROWS ONLY;
				""";
	}

	@Override
	public String getInfo4()
	{
		return """
				SELECT
					dm_date
					, dm_close
					, dm_high
					, dm_low
					, dm_vol
				FROM
					daily_market
				WHERE
					dm_s_symb = CAST(:symbol as varchar(15))
					AND dm_date >= :start_day
				ORDER BY
					dm_date ASC
				OFFSET 0 ROWS FETCH NEXT :max_rows_to_return ROWS ONLY;
				""";
	}

	@Override
	public String getInfo5()
	{
		return """
				SELECT
					lt_price
					, lt_open_price
					, lt_vol
				FROM
					last_trade
				WHERE
					lt_s_symb = CAST(:symbol as varchar(15));
				""";
	}

	@Override
	public String getInfo6()
	{
		return """
				SELECT
					ni_dts
					, ni_source
					, ni_author
					, ni_item
				FROM
					news_xref
					INNER JOIN news_item ON ni_id = nx_ni_id
				WHERE
					nx_co_id = :co_id
				ORDER BY
					ni_dts ASC
				OFFSET 0 ROWS FETCH NEXT 2 ROWS ONLY;
				""";
	}

	@Override
	public String getInfo7()
	{
		return """
				SELECT
					ni_dts
					, ni_source
					, ni_author
					, ni_headline
					, ni_summary
				FROM
					news_xref
					INNER JOIN news_item ON ni_id = nx_ni_id
				WHERE
				 	nx_co_id = :co_id
				ORDER BY
					ni_dts ASC
				OFFSET 0 ROWS FETCH NEXT 2 ROWS ONLY;
				""";
	}
}
