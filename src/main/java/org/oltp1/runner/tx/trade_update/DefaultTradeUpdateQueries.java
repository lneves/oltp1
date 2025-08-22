package org.oltp1.runner.tx.trade_update;

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
				  t.t_ca_id
				  , t.t_exec_name
				  , t.t_is_cash
				  , t.t_id AS trade_id
				  , t.t_trade_price
				  , t.t_qty
				  , t.t_dts
				  , t.t_tt_id
				  , tt.tt_name
				  , s.s_name
				FROM (
				  SELECT
				    t_id, t_ca_id, t_exec_name, t_is_cash,
				    t_trade_price, t_qty, t_dts, t_tt_id, t_s_symb
				  FROM trade
				  WHERE
				    t_s_symb = CAST(:symbol as varchar(15))
				    AND t_dts >= :start_trade_dts
				    AND t_dts <= :end_trade_dts
				    AND t_ca_id <= :max_acct_id
				  ORDER BY t_dts ASC, t_id ASC
				  OFFSET 0 ROWS FETCH NEXT :limit ROWS ONLY
				) AS t
				JOIN trade_type tt ON tt.tt_id = t.t_tt_id
				JOIN security s ON s.s_symb = t.t_s_symb
				ORDER BY t.t_dts ASC, t.t_id ASC;
								""";
	}

}
