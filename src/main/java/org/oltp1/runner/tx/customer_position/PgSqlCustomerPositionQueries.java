package org.oltp1.runner.tx.customer_position;

/**
 * Provides the PostgreSQL-specific SQL queries for the Customer-Position
 * transaction.
 */
public class PgSqlCustomerPositionQueries extends DefaultCustomerPositionQueries
{
	@Override
	public String getCustomerAccounts()
	{
		return """
				SELECT
				    ca_id AS acct_id,
				    ca_bal AS cash_bal,
				    SUM(COALESCE(hs_qty * lt_price, 0)) AS assets_total
				FROM
				    customer_account
				    LEFT JOIN holding_summary ON ca_id=hs_ca_id
				    LEFT JOIN last_trade ON hs_s_symb = lt_s_symb
				WHERE
				    ca_c_id = :cust_id
				GROUP BY
				    ca_id, ca_bal
				ORDER BY
				    3 ASC
				LIMIT 10;
				""";
	}

	@Override
	public String getTradeHistory()
	{
		return """
				SELECT
				    t_id AS trade_id,
				    t_s_symb AS symbol,
				    t_qty AS qty,
				    st_name AS trade_status,
				    th_dts AS hist_dts
				FROM
				    (
				        SELECT t_id AS id
				        FROM trade
				        WHERE t_ca_id = :acct_id
				        ORDER BY t_dts DESC LIMIT 10
				    ) AS t,
				    trade,
				    trade_history,
				    status_type
				WHERE
				    t_id = id
				    AND th_t_id = t_id
				    AND st_id = th_st_id
				ORDER BY
				    th_dts DESC
				LIMIT 30;
				""";
	}
}
