package org.oltp1.runner.tx.customer_position;

/**
 * Provides the Microsoft SQL Server-specific SQL queries for the
 * Customer-Position transaction.
 */
public class MsSqlCustomerPositionQueries extends DefaultCustomerPositionQueries
{

	@Override
	public String getCustomerAccounts()
	{
		return """
				SELECT TOP 10
					ca_id AS acct_id
					, ca_bal AS cash_bal
					, SUM(COALESCE(hs_qty * lt_price, 0)) AS assets_total
				FROM
					customer_account
					LEFT LOOP JOIN holding_summary ON ca_id=hs_ca_id
					LEFT LOOP JOIN last_trade ON hs_s_symb = lt_s_symb
				WHERE
					ca_c_id = :cust_id
				GROUP BY
					ca_id
					, ca_bal
				ORDER BY
					3 ASC
				OPTION (FORCE ORDER);
								""";
	}

	@Override
	public String getTradeHistory()
	{
		return """
				SELECT TOP 30
					t_id AS trade_id
					, t_s_symb AS symbol
					, t_qty AS qty
					, st_name AS trade_status
					, th_dts AS hist_dts
				FROM
					(
						SELECT TOP 10
							t_id AS id
						FROM
							trade
						WHERE
							t_ca_id = :acct_id
						ORDER BY
							t_dts DESC
					) AS t
					JOIN trade ON t_id=id
					INNER LOOP JOIN trade_history ON th_t_id=t_id
					, status_type
				WHERE
					st_id = th_st_id
				ORDER BY
					th_dts DESC
				OPTION (FORCE ORDER);
								""";
	}
}
