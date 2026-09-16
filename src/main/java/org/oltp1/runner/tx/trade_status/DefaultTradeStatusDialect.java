package org.oltp1.runner.tx.trade_status;

public class DefaultTradeStatusDialect implements TradeStatusDialect
{
	@Override
	public String getTradeStatus()
	{
		return """
				SELECT
				    t.t_id AS trade_id
				    , t.t_dts AS trade_dts
				    , st.st_name AS status_name
				    , tt.tt_name AS type_name
				    , t.t_is_cash AS is_cash
				    , s.s_name AS s_name
				    , t.t_qty AS trade_qty
				    , t.t_bid_price AS bid_price
				    , t.t_trade_price AS trade_price
				    , t.t_chrg AS charge
				    , t.t_comm AS comm_amount
				    , t.t_tax AS tax_amount
				    , t.t_lifo AS is_lifo
				    , t.t_exec_name AS exec_name
				FROM (
				    SELECT t_id, t_dts, t_st_id, t_tt_id, t_s_symb, t_qty, t_exec_name,
				           t_chrg, t_is_cash, t_bid_price, t_trade_price, t_comm, t_tax, t_lifo
				    FROM trade
				    WHERE t_ca_id = :acct_id
				    ORDER BY t_dts DESC, t_id DESC
				    OFFSET 0 ROWS FETCH NEXT 50 ROWS ONLY
				) AS t
				JOIN status_type st ON st.st_id = t.t_st_id
				JOIN trade_type tt ON tt.tt_id = t.t_tt_id
				JOIN security s  ON s.s_symb = t.t_s_symb
				ORDER BY t.t_dts DESC, t.t_id DESC;
				""";
	}

	@Override
	public String getTradeName()
	{
		return """
				SELECT
					c_l_name AS cust_l_name
					, c_f_name AS cust_f_name
					, b_name AS broker_name
				FROM
					customer_account
					JOIN customer ON c_id = ca_c_id
					JOIN broker ON b_id = ca_b_id
				WHERE
					ca_id = :acct_id
								""";
	}
}
