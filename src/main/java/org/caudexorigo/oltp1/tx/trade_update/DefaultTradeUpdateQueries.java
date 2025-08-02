package org.caudexorigo.oltp1.tx.trade_update;

public abstract class DefaultTradeUpdateQueries implements TradeUpdateQueries
{
	@Override
	public String getTradesFrame2()
	{
		return """
				SELECT
					t_id AS trade_id
					, t_exec_name
					, t_bid_price
					, t_trade_price
					, t_is_cash
				FROM
					trade
				WHERE
					t_ca_id = :acct_id
					AND t_dts >= :start_trade_dts
					AND t_dts <= :end_trade_dts
				ORDER BY t_dts
				OFFSET 0 ROWS FETCH NEXT :limit ROWS ONLY;
				""";
	}
	
	@Override
	public String getTradesFrame3()
	{
		return """
				SELECT
					t_ca_id
					, t_exec_name
					, t_is_cash
					, t_id AS trade_id
					, t_trade_price
					, t_qty
					, t_dts
					, t_tt_id
					, tt_name
					, s_name
				FROM
					trade ,trade_type ,security
				WHERE t_s_symb = CAST(:symbol as varchar(15))
					AND t_dts >= :start_trade_dts
					AND t_dts <= :end_trade_dts
					AND tt_id = t_tt_id
					AND s_symb = t_s_symb
					AND t_ca_id <= :max_acct_id
				ORDER BY
					t_dts ASC
				OFFSET 0 ROWS FETCH NEXT :limit ROWS ONLY;
				""";
	}

}
