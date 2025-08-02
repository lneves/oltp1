package org.caudexorigo.oltp1.tx.trade_lookup;

/**
 * Provides the database-agnostic SQL queries for the Trade-Lookup transaction.
 */
public abstract class DefaultTradeLookupQueries implements TradeLookupQueries
{
	@Override
	public String getFrame2()
	{
		return """
				SELECT
					t_id
					, t_bid_price AS bid_price
					, t_exec_name AS exec_name
					, t_is_cash AS is_cash
					, t_id AS trade_list
					, t_trade_price AS trade_price
					, se_amt AS settlement_amount
					, se_cash_due_date AS settlement_cash_due_date
					, se_cash_type AS settlement_cash_type
					, ct_amt AS cash_transaction_amount
					, ct_dts AS cash_transaction_dts
					, ct_name AS cash_transaction_name
				FROM
					(
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
						AND t_dts BETWEEN :start_dts AND :end_dts
					ORDER BY
						t_dts
					OFFSET 0 ROWS FETCH NEXT (:max_trades) ROWS ONLY
					) AS trades
					INNER JOIN settlement ON se_t_id = trades.t_id
					LEFT JOIN cash_transaction ON ct_t_id = trades.t_id;
								""";
	}

	@Override
	public String getFrame4()
	{
		return """
				SELECT
					hh_h_t_id AS holding_history_id
					, hh_t_id AS holding_history_trade_id
					, hh_before_qty AS quantity_before
					, hh_after_qty AS quantity_after
				FROM
					holding_history
				WHERE
					hh_h_t_id IN (
						SELECT
							hh_h_t_id
						FROM
							holding_history
						WHERE
							hh_t_id IN (
								SELECT
									t_id
								FROM
									trade
								WHERE
									t_ca_id = :ca_id
									AND t_dts >= :start_dts
								ORDER BY t_dts ASC
								OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY
								)
						);
								""";
	}

}
