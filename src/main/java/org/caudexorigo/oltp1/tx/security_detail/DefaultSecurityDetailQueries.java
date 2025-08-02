package org.caudexorigo.oltp1.tx.security_detail;

/**
 * Provides the database-agnostic SQL queries for the Security-Detail transaction.
 */
public class DefaultSecurityDetailQueries implements SecurityDetailQueries
{

	@Override
	public String getInfo1()
	{
		return """
				SELECT
					s_name AS s_name
					, co_id AS co_id
					, co_name AS co_name
					, co_sp_rate AS sp_rate
					, co_ceo AS ceo_name
					, co_desc AS co_desc
					, co_open_date AS open_date
					, co_st_id AS co_st_id
					, ca.ad_line1 AS co_ad_line1
					, ca.ad_line2 AS co_ad_line2
					, zca.zc_town AS co_ad_town
					, zca.zc_div AS co_ad_div
					, ca.ad_zc_code AS co_ad_zip
					, ca.ad_ctry AS co_ad_ctry
					, s_num_out AS num_out
					, s_start_date AS start_date
					, s_exch_date AS exch_date
					, s_pe AS pe_ratio
					, s_52wk_high AS _52_wk_high
					, s_52wk_high_date AS _52_wk_high_date
					, s_52wk_low AS _52_wk_low
					, s_52wk_low_date AS _52_wk_low_date
					, s_dividend AS divid
					, s_yield AS yield
					, zea.zc_div AS ex_ad_div
					, ea.ad_ctry AS ex_ad_ctry
					, ea.ad_line1 AS ex_ad_line1
					, ea.ad_line2 AS ex_ad_line2
					, zea.zc_town AS ex_ad_town
					, ea.ad_zc_code AS ex_ad_zip
					, ex_close AS ex_close
					, ex_desc AS ex_desc
					, ex_name AS ex_name
					, ex_num_symb AS ex_num_symb
					, ex_open AS ex_open
				FROM
					security
					, company
					, address ca
					, address ea
					, zip_code zca
					, zip_code zea
					, exchange
				WHERE
					s_symb = CAST(:symbol as varchar(15))
					AND co_id = s_co_id
					AND ca.ad_id = co_ad_id
					AND ea.ad_id = ex_ad_id
					AND ex_id = s_ex_id
					AND ca.ad_zc_code = zca.zc_code
					AND ea.ad_zc_code = zea.zc_code
				               """;
	}

	@Override
	public String getInfo2()
	{
		return """
				SELECT
					co_name
					, in_name
				FROM
					company_competitor
					, company
					, industry
				WHERE
					cp_co_id = :co_id
					AND co_id = cp_comp_co_id
					AND in_id = cp_in_id
				ORDER BY co_name
				OFFSET 0 ROWS FETCH NEXT 3 ROWS ONLY
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
				OFFSET 0 ROWS FETCH NEXT (:max_rows_to_return) ROWS ONLY;
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
					, news_item
				WHERE
					ni_id = nx_ni_id
					AND nx_co_id = :co_id
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
					, news_item
				WHERE
					ni_id = nx_ni_id
					AND nx_co_id = :co_id
				ORDER BY
					ni_dts ASC
				OFFSET 0 ROWS FETCH NEXT 2 ROWS ONLY;
				               """;
	}
}
