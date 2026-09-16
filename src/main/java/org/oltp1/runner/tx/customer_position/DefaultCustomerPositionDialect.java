package org.oltp1.runner.tx.customer_position;

/**
 * Provides default, database-agnostic implementations for common
 * Customer-Position queries. Database-specific queries are left abstract for
 * subclasses to implement.
 */
public class DefaultCustomerPositionDialect implements CustomerPositionDialect
{

	@Override
	public String getCustomerByCid()
	{
		return "SELECT *, c_id AS cust_id FROM customer WHERE c_id = :cust_id;";
	}

	@Override
	public String getCustomerByTaxid()
	{
		return "SELECT c_id AS cust_id FROM customer WHERE c_tax_id = :tax_id;";
	}

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
					LEFT JOIN holding_summary ON ca_id = hs_ca_id
					LEFT JOIN last_trade ON hs_s_symb = lt_s_symb
				WHERE
					ca_c_id = :cust_id
				GROUP BY
					ca_id, ca_bal
				ORDER BY
					3 ASC
				OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY;
				""";
	}

	@Override
	public String getTradeHistory()
	{
		return """
				SELECT
					t_id AS trade_id
					, t_s_symb AS symbol
					, t_qty AS qty
					, st_name AS trade_status
					, th_dts AS hist_dts
				FROM
					(
						SELECT t_id AS id
						FROM trade
						WHERE t_ca_id = :acct_id
						ORDER BY t_dts DESC
						OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY
					) AS t
					JOIN trade ON t_id=id
					JOIN trade_history ON th_t_id=t_id
					JOIN status_type ON st_id = th_st_id
				ORDER BY th_dts DESC
				OFFSET 0 ROWS FETCH NEXT 30 ROWS ONLY;
								""";
	}
}