package org.oltp1.egen.model;

import org.oltp1.egen.generator.TradeGen;

public class TradeOutputRow
{
	public TradeRow trade = new TradeRow();
	public TradeHistoryRow[] tradeHistory = new TradeHistoryRow[3];
	public SettlementRow settlement = new SettlementRow();
	public CashTransactionRow cashTransaction = new CashTransactionRow();
	public HoldingHistoryRow[] holdingHistory = new HoldingHistoryRow[TradeGen.MAX_HOLDING_HISTORY_ROWS_PER_TRADE];

	public TradeOutputRow()
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
