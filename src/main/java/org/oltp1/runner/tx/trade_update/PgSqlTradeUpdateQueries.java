package org.oltp1.runner.tx.trade_update;

/**
 * Provides the PostgreSQL-specific SQL queries for the Trade-Update
 * transaction.
 */
public class PgSqlTradeUpdateQueries extends DefaultTradeUpdateQueries
{

	@Override
	public String updateTradesFrame1()
	{
		return """
				UPDATE
					trade
				SET
					t_exec_name = CASE
									WHEN t_exec_name LIKE '% X %' THEN REPLACE (RTRIM(t_exec_name), ' X ', ' ')
									WHEN t_exec_name LIKE '% %' THEN REPLACE (RTRIM(t_exec_name), ' ', ' X ')
				                   	ELSE t_exec_name
				                   END
				WHERE
					t_id IN (
						SELECT trade_id::bigint FROM string_to_table(:trade_lst, ',') AS trade_id
						);
				""";
	}

	@Override
	public String updateTradesFrame2()
	{
		return """
				UPDATE
				settlement
				SET
					se_cash_type = CASE
									WHEN SE_CASH_TYPE = 'Cash Account' THEN 'Cash'
									WHEN SE_CASH_TYPE = 'Cash' THEN 'Cash Account'
									WHEN SE_CASH_TYPE = 'Margin Account' THEN 'Margin'
									WHEN SE_CASH_TYPE = 'Margin' THEN 'Margin Account'
									ELSE se_cash_type
				                   END
				WHERE
					se_t_id IN (
						SELECT trade_id::bigint FROM string_to_table(:trade_lst, ',') AS trade_id
						);
				""";
	}

	@Override
	public String updateTradesFrame3()
	{
		return """
				WITH rs AS (

				SELECT
					x.trade_id
					, x.t_qty
				    , x.tt_name
				    , x.s_name
				FROM
				    jsonb_to_recordset(
				        :cash_trades::jsonb
				    ) AS x(trade_id BIGINT, t_qty INT, tt_name TEXT, s_name TEXT)

				)
				UPDATE
					cash_transaction
				SET
					ct_name = CASE
						WHEN ct_name LIKE '% Shares of %' THEN rs.tt_name || ' ' || rs.t_qty || ' shares of ' || rs.s_name
						WHEN ct_name LIKE '% shares of %' THEN rs.tt_name || ' ' || rs.t_qty || ' Shares of ' || rs.s_name
						ELSE ct_name
						END
				FROM
					rs
				WHERE
					ct_t_id = rs.trade_id
				""";
	}

	@Override
	public String getTradeInfo()
	{
		return """
				SELECT t_id AS trade_id, t_st_id, tt.tt_name, t_exec_name, t_is_cash
				FROM trade t JOIN trade_type tt ON t.t_tt_id = tt.tt_id
				WHERE t.t_id IN (
						SELECT trade_id::bigint FROM string_to_table(:trade_lst, ',') AS trade_id
						);
				""";
	}

	@Override
	public String getSettlementInfo()
	{
		return """
				SELECT se_t_id AS trade_id, se_cash_type
				FROM settlement
				WHERE se_t_id IN (
						SELECT trade_id::bigint FROM string_to_table(:trade_lst, ',') AS trade_id
						)
				ORDER BY se_t_id
				""";
	}

	@Override
	public String getTradeHistory()
	{
		return """
				SELECT th_t_id AS trade_id, th_dts, th_st_id
				FROM trade_history
				WHERE th_t_id IN (
						SELECT trade_id::bigint FROM string_to_table(:trade_lst, ',') AS trade_id
						)
				ORDER BY th_dts
				""";
	}

	@Override
	public String getCashTransactionInfo()
	{
		return """
				SELECT ct_t_id AS trade_id, ct_name
				FROM cash_transaction
				WHERE ct_t_id IN (
						SELECT trade_id::bigint FROM string_to_table(:trade_lst, ',') AS trade_id
						);
				""";
	}
}
