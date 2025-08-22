package org.oltp1.runner.tx.trade_update;

/**
 * Provides the MariaDB specific SQL queries for the Trade-Update
 * transaction.
 */
public class MariaDbTradeUpdateQueries extends DefaultTradeUpdateQueries
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
						SELECT value FROM JSON_TABLE(:trade_lst, '$[*]' COLUMNS (value bigint PATH '$')) tid
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
						SELECT value FROM JSON_TABLE(:trade_lst, '$[*]' COLUMNS (value bigint PATH '$')) tid
						);
				""";
	}

	@Override
	public String updateTradesFrame3()
	{
		return """
				UPDATE cash_transaction AS ct
				JOIN (
				  WITH rs AS (
					SELECT trade_id, t_qty, tt_name, s_name
					FROM JSON_TABLE(
					  :cash_trades
					  , '$[*]' COLUMNS (
						trade_id BIGINT PATH '$.trade_id'
						, t_qty INT PATH '$.t_qty'
						, tt_name VARCHAR(12) PATH '$.tt_name'
						, s_name VARCHAR(70) PATH '$.s_name'
					  )
					) AS x
				  )
				  SELECT * FROM rs
				) AS rs ON ct.ct_t_id = rs.trade_id
				SET ct.ct_name = CASE
				  WHEN ct.ct_name LIKE '% Shares of %'
					THEN CONCAT(rs.tt_name, ' ', rs.t_qty, ' shares of ', rs.s_name)
				  WHEN ct.ct_name LIKE '% shares of %'
					THEN CONCAT(rs.tt_name, ' ', rs.t_qty, ' Shares of ', rs.s_name)
				  ELSE ct.ct_name
				END;
								""";
	}

	@Override
	public String getTradeInfo()
	{
		return """
				SELECT t_id AS trade_id, t_st_id, tt.tt_name, t_exec_name, t_is_cash
				FROM trade t JOIN trade_type tt ON t.t_tt_id = tt.tt_id
				WHERE t.t_id IN (
						SELECT value FROM JSON_TABLE(:trade_lst, '$[*]' COLUMNS (value bigint PATH '$')) tid
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
						SELECT value FROM JSON_TABLE(:trade_lst, '$[*]' COLUMNS (value bigint PATH '$')) tid
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
						SELECT value FROM JSON_TABLE(:trade_lst, '$[*]' COLUMNS (value bigint PATH '$')) tid
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
						SELECT value FROM JSON_TABLE(:trade_lst, '$[*]' COLUMNS (value bigint PATH '$')) tid
						);
				""";
	}
}
