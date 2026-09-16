package org.oltp1.runner.tx.trade_result;

public class MsSqlTradeResultDialect extends DefaultTradeResultDialect
{
	@Override
	public String claimTrade()
	{
		return """
				SELECT
					t_id
				FROM
					trade WITH (UPDLOCK, ROWLOCK)
				WHERE
					t_id = :trade_id
					AND t_st_id = :st_submitted_id;
				""";
	}

	@Override
	public String getHoldingSummaryForUpdate()
	{
		return """
				SELECT
					hs_qty
				FROM
					holding_summary WITH (UPDLOCK, ROWLOCK)
				WHERE
					hs_ca_id = :acct_id
					AND hs_s_symb = CAST(:symbol as varchar(15));
				""";
	}

	@Override
	public String getHoldingAscForUpdate()
	{
		return """
				SELECT
					h_t_id AS hold_id
					, h_qty AS hold_qty
					, h_price AS hold_price
				FROM
					holding WITH (UPDLOCK, ROWLOCK)
				WHERE
					h_ca_id = :acct_id
					AND h_s_symb = CAST(:symbol as varchar(15))
				ORDER BY
					h_dts ASC, h_t_id ASC;
				""";
	}

	@Override
	public String getHoldingDescForUpdate()
	{
		return """
				SELECT
					h_t_id AS hold_id
					, h_qty AS hold_qty
					, h_price AS hold_price
				FROM
					holding WITH (UPDLOCK, ROWLOCK)
				WHERE
					h_ca_id = :acct_id
					AND h_s_symb = CAST(:symbol as varchar(15))
				ORDER BY
					h_dts DESC, h_t_id DESC;
				""";
	}

	@Override
	public String upsertHoldingSummaryBuy()
	{
		return """
				MERGE INTO holding_summary WITH (HOLDLOCK) AS target
				USING (
				    VALUES (:acct_id, :symbol, :trade_qty)
				) AS source (hs_ca_id, hs_s_symb, hs_qty)
				ON target.hs_ca_id = source.hs_ca_id
				   AND target.hs_s_symb = source.hs_s_symb

				WHEN MATCHED THEN
				    UPDATE SET target.hs_qty = target.hs_qty + source.hs_qty

				WHEN NOT MATCHED THEN
				    INSERT (hs_ca_id, hs_s_symb, hs_qty)
				    VALUES (source.hs_ca_id, source.hs_s_symb, source.hs_qty);
								""";
	}

	@Override
	public String upsertHoldingSummarySell()
	{
		return """
				MERGE INTO holding_summary WITH (HOLDLOCK) AS target
				USING (
				    VALUES (:acct_id, :symbol, :trade_qty)
				) AS source (hs_ca_id, hs_s_symb, hs_qty)
				ON target.hs_ca_id = source.hs_ca_id
				   AND target.hs_s_symb = source.hs_s_symb

				WHEN MATCHED THEN
				    UPDATE SET target.hs_qty = target.hs_qty + source.hs_qty

				WHEN NOT MATCHED THEN
				    INSERT (hs_ca_id, hs_s_symb, hs_qty)
				    VALUES (source.hs_ca_id, source.hs_s_symb, source.hs_qty);
								""";
	}

}
