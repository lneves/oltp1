package org.oltp1.runner.tx.trade_lookup;

/**
 * Provides the PostgreSQL-specific SQL queries for the Trade-Lookup
 * transaction.
 */
public class PgSqlTradeLookupQueries extends DefaultTradeLookupQueries
{

	@Override
	public String getTradeInfoFrame1()
	{
		return """
				SELECT
					t_id
					, t_bid_price AS bid_price
					, t_exec_name AS exec_name
					, t_is_cash AS is_cash
					, t_trade_price AS trade_price
					, tt_is_mrkt AS is_market
					, se_amt AS settlement_amount
					, se_cash_due_date AS settlement_cash_due_date
					, se_cash_type AS settlement_cash_type
					, ct_amt AS cash_transaction_amount
					, ct_dts AS cash_transaction_dts
					, ct_name AS cash_transaction_name
				FROM
					trade
					INNER JOIN trade_type ON t_tt_id = tt_id
					INNER JOIN settlement ON se_t_id = t_id
					LEFT JOIN cash_transaction ON t_id = ct_t_id
				WHERE
					t_id = ANY(:trade_ids::bigint[])
					AND t_tt_id = tt_id
				ORDER BY
					t_id
				OFFSET 0 ROWS FETCH NEXT (:max_trades) ROWS ONLY;
				               """;
	}

	@Override
	public String getFrame3()
	{
		return """
				WITH trades AS (

				SELECT
					t_id
					, t_ca_id
					, t_exec_name
					, t_is_cash
					, t_trade_price
					, t_qty
					, t_dts
					, t_tt_id
				FROM
					trade
				WHERE
					t_s_symb = CAST(:symbol as varchar(15))
					AND t_dts BETWEEN :start_dts AND :end_dts
				ORDER BY
					t_dts
				OFFSET 0 ROWS FETCH NEXT (:max_trades) ROWS ONLY

				), cash_trades AS (
				SELECT
					ct_t_id
					, ct_amt
					, ct_dts
					, ct_name
				FROM
					cash_transaction
				WHERE
					ct_t_id IN (SELECT t_id FROM trades WHERE t_is_cash=true)
				)
				SELECT
					t_id
					, t_ca_id AS acct_id
					, t_exec_name AS exec_name
					, t_is_cash AS is_cash
					, t_id AS trade_list
					, t_trade_price AS price
					, t_qty AS quantity
					, t_dts AS trade_dts
					, t_tt_id AS trade_type
					, se_amt AS settlement_amount
					, se_cash_due_date AS settlement_cash_due_date
					, se_cash_type AS settlement_cash_type
					, ct_amt AS cash_transaction_amount
					, ct_dts AS cash_transaction_dts
					, ct_name AS cash_transaction_name
				FROM
					trades
					INNER JOIN settlement ON se_t_id = trades.t_id
					LEFT JOIN cash_trades ON ct_t_id = trades.t_id
				ORDER BY
					t_dts
								""";
	}

	@Override
	public String getTradeHistory()
	{
		return """
				SELECT
					th_dts AS trade_history_dts
					, th_st_id AS trade_history_status_id
				FROM
					(
					SELECT
						th_dts,
						th_st_id,
						RANK() OVER(PARTITION BY th_t_id ORDER BY th_dts) AS pos
					FROM
						trade_history
					WHERE
						th_t_id = ANY(:trade_ids::bigint[])
					) AS t
				WHERE
					pos <= 3
				               """;
	}
}
