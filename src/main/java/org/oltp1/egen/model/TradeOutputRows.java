package org.oltp1.egen.model;

/*
 * A bundle of all rows generated for a single trade event.
 */
public class TradeOutputRows
{
	private static final int MAX_HOLDING_HISTORY_ROWS_PER_TRADE = 800 / 100;

	TradeRow trade = new TradeRow();
	TradeHistoryRow[] tradeHistory = new TradeHistoryRow[3];
	SettlementRow settlement = new SettlementRow();
	CashTransactionRow cashTransaction = new CashTransactionRow();
	HoldingHistoryRow[] holdingHistory = new HoldingHistoryRow[MAX_HOLDING_HISTORY_ROWS_PER_TRADE];

	public TradeOutputRows()
	{
		for (int i = 0; i < tradeHistory.length; i++)
			tradeHistory[i] = new TradeHistoryRow();
		for (int i = 0; i < holdingHistory.length; i++)
			holdingHistory[i] = new HoldingHistoryRow();
	}

	public void reset()
	{
		trade = new TradeRow();
		settlement = new SettlementRow();
		cashTransaction = new CashTransactionRow();
		for (int i = 0; i < tradeHistory.length; i++)
			tradeHistory[i] = new TradeHistoryRow();
		for (int i = 0; i < holdingHistory.length; i++)
			holdingHistory[i] = new HoldingHistoryRow();
	}
}