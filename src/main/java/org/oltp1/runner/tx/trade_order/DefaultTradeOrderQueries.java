package org.oltp1.runner.tx.trade_order;

/**
 * Provides the database-agnostic SQL queries for the Trade-Order transaction.
 */
public class DefaultTradeOrderQueries implements TradeOrderQueries
{

	@Override
	public String getCustomerInfo()
	{
		return """
				SELECT
					ca_b_id AS broker_id
					, ca_c_id AS cust_id
					, ca_name AS acct_name
					, CAST(ca_tax_st AS int) AS tax_status
					, c_l_name AS cust_l_name
					, c_f_name AS cust_f_name
					, c_tax_id AS tax_id
					, c_tier AS cust_tier
					, b_name AS broker_name
				FROM
					customer_account
					JOIN customer ON ca_c_id = c_id
					JOIN broker ON ca_b_id = b_id
				WHERE
					ca_id = :acct_id;
				""";
	}

	@Override
	public String getAccountPermissions()
	{
		return """
				SELECT
					ap_acl
				FROM
					account_permission
				WHERE
					ap_ca_id = :acct_id
					AND ap_l_name = :exec_l_name
					AND ap_f_name = :exec_f_name
					AND ap_tax_id = :exec_tax_id
				""";
	}

	@Override
	public String getSecurityInfoByCoName()
	{
		return """
				SELECT
					co_id AS co_id
					, s_ex_id AS exch_id
					, s_name AS s_name
					, s_symb AS symbol
				FROM
					company
					INNER JOIN security ON company.co_id = security.s_co_id
				WHERE
					co_name = :co_name
					AND s_issue = :issue;
				""";
	}

	@Override
	public String getSecurityInfoBySymbol()
	{
		return """
				SELECT
					co_id AS co_id
					, s_ex_id AS exch_id
					, s_name AS s_name
					, s_symb AS symbol
				FROM
					company
					INNER JOIN security ON company.co_id = security.s_co_id
				WHERE
					s_symb = CAST(:symbol as varchar(15));
				""";
	}

	@Override
	public String getLastTrade()
	{
		return """
				SELECT
					lt_price AS market_price
				FROM
					last_trade
				WHERE
					lt_s_symb = CAST(:symbol as varchar(15));
				""";
	}

	@Override
	public String getTradeType()
	{
		return """
				SELECT
					tt_is_mrkt AS type_is_market
					, tt_is_sell AS type_is_sell
				FROM
					trade_type
				WHERE
					tt_id = :trade_type_id;
				""";
	}

	@Override
	public String getHoldingSummary()
	{
		return """
				SELECT
					hs_qty
				FROM
					holding_summary
				WHERE
					hs_ca_id = :acct_id
					AND hs_s_symb = CAST(:symbol as varchar(15));
				""";
	}

	@Override
	public String getHoldingAsc()
	{
		return """
				SELECT
					h_qty
					, h_price
				FROM
					holding
				WHERE
					h_ca_id = :acct_id
					AND h_s_symb = CAST(:symbol as varchar(15))
				ORDER BY
					h_dts ASC;
				""";
	}

	@Override
	public String getHoldingDesc()
	{
		return """
				SELECT
					h_qty
					, h_price
				FROM
					holding
				WHERE
					h_ca_id = :acct_id
					AND h_s_symb = CAST(:symbol as varchar(15))
				ORDER BY
					h_dts DESC;
				""";
	}

	@Override
	public String getTaxRate()
	{
		return """
				SELECT
					SUM(tx_rate) AS tax_rates
				FROM
					taxrate
				WHERE
					tx_id IN(
						SELECT
							cx_tx_id
						FROM
							customer_taxrate
						WHERE
							cx_c_id = :cust_id
					);
				""";
	}

	@Override
	public String getFees()
	{
		return """
				SELECT
					ch_chrg AS charge_amount
					, cr_rate AS comm_rate
				FROM
					commission_rate
					, charge
				WHERE
					cr_c_tier = :cust_tier
					AND cr_tt_id = :trade_type_id
					AND cr_ex_id = :exch_id
					AND cr_from_qty <= :f_trade_qty
					AND cr_to_qty >= :t_trade_qty
					AND ch_c_tier = cr_c_tier
					AND ch_tt_id = cr_tt_id;
				""";
	}

	@Override
	public String getCustomerAssets()
	{
		return """
				SELECT
					COALESCE(hold_asset, ca_bal) AS customer_assets
				FROM
					customer_account,
					(	SELECT null AS hold_asset, :acct_id AS hs_ca_id
						UNION ALL
						SELECT
							SUM( hs_qty * lt_price) AS hold_asset
							, hs_ca_id
						FROM
							holding_summary,
							last_trade
						WHERE
							hs_ca_id = :acct_id
							AND lt_s_symb = hs_s_symb
						GROUP BY
							hs_ca_id
					) AS ha
				WHERE
					ca_id = ha.hs_ca_id
				""";
	}

	@Override
	public String insertTrade()
	{
		return """
				INSERT INTO trade
				(
					t_dts
					, t_st_id
					, t_tt_id
					, t_is_cash
					, t_s_symb
					, t_qty
					, t_bid_price
					, t_ca_id
					, t_exec_name
					, t_trade_price
					, t_chrg
					, t_comm
					, t_tax
					, t_lifo
				)
				VALUES
				(
					:trade_dts
					, :status_id
					, :trade_type_id
					, :is_cash
					, :symbol
					, :trade_qty
					, :requested_price
					, :acct_id
					, :exec_name
					, NULL
					, :charge_amount
					, :comm_amount
					, 0
					, :is_lifo
				);
				""";
	}

	@Override
	public String insertTradeRequest()
	{
		return """
				INSERT INTO trade_request
					(
					tr_t_id
					, tr_tt_id
					, tr_s_symb
					, tr_qty
					, tr_bid_price
					, tr_b_id
				)
				VALUES
				(
					:t_id
					, :trade_type_id
					, :symbol
					, :trade_qty
					, :requested_price
					, :broker_id
				);
				""";
	}

	@Override
	public String insertTradeHistory()
	{
		return """
				INSERT INTO trade_history
				(
					th_t_id
					, th_dts
					, th_st_id
				)
				VALUES
				(
					:t_id
					, :trade_dts
					, :status_id
				);
				""";
	}
}
