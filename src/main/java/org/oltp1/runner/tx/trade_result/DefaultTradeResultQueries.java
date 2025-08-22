package org.oltp1.runner.tx.trade_result;

/**
 * Provides the database-agnostic SQL queries for the Trade-Result transaction.
 */
public class DefaultTradeResultQueries implements TradeResultQueries
{

	// Frame 1
	@Override
	public String getTradeInfo()
	{
		return """
				SELECT
					t_ca_id AS acct_id
					, t_chrg AS charge
					, COALESCE(hs_qty, 0) AS hs_qty
					, t_s_symb AS symbol
					, t_is_cash AS trade_is_cash
					, t_qty As trade_qty
					, t_tt_id AS type_id
					, tt_is_mrkt AS type_is_market
					, tt_is_sell AS type_is_sell
					, tt_name AS type_name
					, t_lifo AS is_lifo
				FROM
					trade
					JOIN trade_type ON t_tt_id = tt_id
					LEFT JOIN holding_summary ON hs_ca_id = t_ca_id AND hs_s_symb = t_s_symb
				WHERE
					t_id = :trade_id
				""";
	}

	// Frame 2
	@Override
	public String getCustomerAccount()
	{
		return """
				SELECT
					ca_b_id AS broker_id
					, ca_c_id AS cust_id
					, ca_tax_st AS tax_status
				FROM
					customer_account
				WHERE
					ca_id = :acct_id
				""";
	}

	@Override
	public String insertHoldingSummary()
	{
		return """
				INSERT INTO holding_summary
				(
					hs_ca_id
					, hs_s_symb
					, hs_qty
				)
				VALUES
				(
					:acct_id
					, :symbol
					, (- :trade_qty)
				);
				""";
	}

	@Override
	public String updateHoldingSummary()
	{
		return """
				UPDATE
					holding_summary
				SET
					hs_qty = (:hs_qty - :trade_qty)
				WHERE
					hs_ca_id = :acct_id
					AND hs_s_symb = :symbol;
				""";
	}

	@Override
	public String getHoldingAsc()
	{
		return """
				SELECT
					h_t_id AS hold_id
					, h_qty AS hold_qty
					, h_price AS hold_price
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
					h_t_id AS hold_id
					, h_qty AS hold_qty
					, h_price AS hold_price
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
	public String insertHoldingHistory()
	{
		return """
				INSERT INTO holding_history
				(
					hh_h_t_id
					, hh_t_id
					, hh_before_qty
					, hh_after_qty
				)
				VALUES
				(
					:hold_id
					, :trade_id
					, :hold_qty
					, :after_qty
				);
				""";
	}

	@Override
	public String updateHolding()
	{
		return """
				UPDATE
					holding
				SET
					h_qty = :qty
				WHERE
					h_t_id = :hold_id;
				""";
	}

	@Override
	public String deleteHolding()
	{
		return """
				DELETE FROM	holding
				WHERE
					h_t_id =:hold_id;
				""";
	}

	@Override
	public String insertHolding()
	{
		return """
				INSERT INTO holding
				(
					h_t_id
					, h_ca_id
					, h_s_symb
					, h_dts
					, h_price
					, h_qty
				)
				VALUES
				(
					:trade_id
					, :acct_id
					, :symbol
					, :trade_dts
					, :trade_price
					, :qty
				);
				""";
	}

	@Override
	public String deleteHoldingSummary()
	{
		return """
				DELETE FROM	holding_summary
				WHERE
					hs_ca_id = :acct_id
					AND hs_s_symb = CAST(:symbol as varchar(15));
				""";
	}

	// Frame 3
	@Override
	public String getTaxRate()
	{
		return """
				SELECT
					COALESCE(SUM(tx_rate), 0) AS tax_rates
				FROM
					taxrate
				WHERE
					tx_id IN
					(
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
	public String updateTradeTax()
	{
		return """
				UPDATE
					trade
				SET
					t_tax = :tax_amount
				WHERE
					t_id = :trade_id;
				""";
	}

	// Frame 4
	@Override
	public String getCommissionRate()
	{
		return """
				SELECT
					s_name
					, cr_rate AS comm_rate
				FROM
					commission_rate
					, customer
					, security
				WHERE
					c_id = :cust_id
					AND s_symb = CAST(:symbol as varchar(15))
					AND cr_from_qty <= :trade_qty
					AND cr_tt_id = :type_id
					AND cr_ex_id = s_ex_id
					AND cr_c_tier = c_tier
				ORDER BY
					cr_from_qty DESC
				OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY;
				""";
	}

	// Frame 5
	@Override
	public String updateTrade()
	{
		return """
				UPDATE
					trade
				SET
					t_comm = :comm_amount,
					t_dts = :trade_dts,
					t_st_id = :st_completed_id,
					t_trade_price = :trade_price
				WHERE
					t_id = :trade_id;
				""";
	}

	@Override
	public String insertTradeHistory()
	{
		return """
				INSERT INTO trade_history
				(
					th_t_id,
					th_dts,
					th_st_id
				)
				VALUES
				(
					:trade_id,
					:trade_dts,
					:st_completed_id
				);
				""";
	}

	@Override
	public String updateBroker()
	{
		return """
				UPDATE
					broker
				SET
					b_comm_total = b_comm_total + :comm_amount,
					b_num_trades = b_num_trades + 1
				WHERE
					b_id = :broker_id;
				""";
	}

	// Frame 6
	@Override
	public String insertSettlement()
	{
		return """
				INSERT INTO settlement
				(
					se_t_id
					, se_cash_type
					, se_cash_due_date
					, se_amt
				)
				VALUES
				(
					:trade_id
					, :cash_type
					, :due_date
					, :se_amount
				);
				""";
	}

	@Override
	public String updateCustomerAccount()
	{
		return """
				UPDATE customer_account
				SET
					ca_bal = ca_bal + :se_amount
				WHERE
					ca_id = :acct_id;
				""";
	}

	@Override
	public String insertCashTransaction()
	{
		return """
				INSERT INTO cash_transaction
				(
					ct_t_id
					, ct_dts
					, ct_amt
					, ct_name
				)
				VALUES
				(
					:trade_id
					, :trade_dts
					, :se_amount
					, :ct_name
				);
				""";
	}

	@Override
	public String getAccountBalance()
	{
		return """
				SELECT
					ca_bal
				FROM
					customer_account
				WHERE
					ca_id =:acct_id;
				""";
	}
}
