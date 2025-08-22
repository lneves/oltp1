package org.oltp1.runner.tx.trade_lookup;

/**
 * Provides the database-agnostic SQL queries for the Trade-Lookup transaction.
 */
public abstract class DefaultTradeLookupQueries implements TradeLookupQueries
{
	@Override
	public String getFrame2()
	{
		return """
				WITH trades
				AS (
					SELECT
				        t_id
						, t_exec_name
						, t_bid_price
						, t_trade_price
						, t_is_cash
					FROM
				        trade
					WHERE
				        t_ca_id = :ca_id
						AND t_dts >= :start_dts
						AND t_dts <= :end_dts
					ORDER BY t_dts OFFSET 0 ROWS FETCH NEXT :max_trades ROWS ONLY
				)
				SELECT
				    tr.t_id
					, tr.t_bid_price AS bid_price
					, tr.t_exec_name AS exec_name
					, tr.t_is_cash AS is_cash
					, tr.t_id AS trade_list
					, tr.t_trade_price AS trade_price
					, s.se_amt AS settlement_amount
					, s.se_cash_due_date AS settlement_cash_due_date
					, s.se_cash_type AS settlement_cash_type
					, ct.ct_amt AS cash_transaction_amount
					, ct.ct_dts AS cash_transaction_dts
					, ct.ct_name AS cash_transaction_name
				FROM
				    trades AS tr
				    JOIN settlement AS s ON s.se_t_id = tr.t_id
				    LEFT JOIN cash_transaction AS ct ON ct.ct_t_id = tr.t_id;
												""";
	}

	@Override
	public String getFrame4()
	{
		return """
				WITH first_trade
				AS (
					SELECT t_id
				    FROM trade
					WHERE t_ca_id = :ca_id AND t_dts >= :start_dts
					ORDER BY t_dts ASC
				    OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY
				) , target_hh_ids AS (
					SELECT hh.hh_h_t_id
					FROM holding_history hh
					JOIN first_trade ft ON hh.hh_t_id = ft.t_id
				)
				SELECT
				    hh.hh_h_t_id AS holding_history_id
					, hh.hh_t_id AS holding_history_trade_id
					, hh.hh_before_qty AS quantity_before
					, hh.hh_after_qty AS quantity_after
				FROM
				    holding_history hh
				    JOIN target_hh_ids t ON t.hh_h_t_id = hh.hh_h_t_id
				ORDER BY
					hh.hh_h_t_id
				OFFSET 0 ROWS FETCH NEXT 20 ROWS ONLY;
												""";
	}

}
