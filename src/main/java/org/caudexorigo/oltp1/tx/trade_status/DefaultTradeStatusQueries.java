package org.caudexorigo.oltp1.tx.trade_status;

public class DefaultTradeStatusQueries implements TradeStatusQueries
{

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
					, customer
					, broker
				WHERE
					ca_id = :acct_id
					AND c_id = ca_c_id
					AND b_id = ca_b_id
								""";
	}

	@Override
	public String getTradeStatus()
	{
		return """
				SELECT
					t_id AS trade_id
					, t_dts AS trade_dts
					, st_name AS status_name
					, t_s_symb AS symbol
					, t_qty AS trade_qty
					, t_exec_name AS exec_name
					, t_chrg AS charge
					, tt_name AS type_name
					, s_name AS s_name
					, ex_name AS ex_name
				FROM
					trade
					, status_type
					, trade_type
					, "security"
					, exchange
				WHERE
					t_ca_id = :acct_id
					AND st_id = t_st_id
					AND tt_id = t_tt_id
					AND s_symb = t_s_symb
					AND ex_id = s_ex_id
				ORDER BY
					t_dts DESC
				OFFSET 0 ROWS FETCH NEXT 50 ROWS ONLY
								""";
	}
}
