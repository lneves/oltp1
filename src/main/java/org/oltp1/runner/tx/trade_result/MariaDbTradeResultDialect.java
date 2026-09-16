package org.oltp1.runner.tx.trade_result;

public class MariaDbTradeResultDialect extends DefaultTradeResultDialect
{
	@Override
	public String upsertHoldingSummaryBuy()
	{
		return """
				INSERT INTO holding_summary (hs_ca_id, hs_s_symb, hs_qty)
				VALUES (:acct_id, :symbol, :trade_qty)
				ON DUPLICATE KEY
				UPDATE hs_qty = hs_qty + VALUE(hs_qty);
				""";
	}

	@Override
	public String upsertHoldingSummarySell()
	{
		return """
				INSERT INTO holding_summary (hs_ca_id, hs_s_symb, hs_qty)
				VALUES (:acct_id, :symbol, :trade_qty)
				ON DUPLICATE KEY
				UPDATE hs_qty = hs_qty + VALUE(hs_qty);
				""";
	}

}
