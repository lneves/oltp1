package org.oltp1.runner.tx.trade_result;

public class PgSqlTradeResultDialect extends DefaultTradeResultDialect
{
	@Override
	public String upsertHoldingSummaryBuy()
	{
		return """
				INSERT INTO holding_summary (hs_ca_id, hs_s_symb, hs_qty)
				VALUES (:acct_id, :symbol, :trade_qty)
				ON CONFLICT (hs_ca_id, hs_s_symb)
				DO UPDATE SET hs_qty = holding_summary.hs_qty + EXCLUDED.hs_qty;
				""";
	}

	@Override
	public String upsertHoldingSummarySell()
	{
		return """
				INSERT INTO holding_summary (hs_ca_id, hs_s_symb, hs_qty)
				VALUES (:acct_id, :symbol, :trade_qty)
				ON CONFLICT (hs_ca_id, hs_s_symb)
				DO UPDATE SET hs_qty = holding_summary.hs_qty + EXCLUDED.hs_qty;
				""";
	}

}
