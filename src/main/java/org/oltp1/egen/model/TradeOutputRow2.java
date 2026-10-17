package org.oltp1.egen.model;

import java.util.ArrayList;
import java.util.List;

public class TradeOutputRow2
{
	public TradeRow trade;
	public List<TradeHistoryRow> tradeHistory;
	public SettlementRow settlement;
	public CashTransactionRow cashTransaction;
	public List<HoldingHistoryRow> holdingHistory;

	public TradeOutputRow2()
	{
		this.trade = new TradeRow();
		this.tradeHistory = new ArrayList<>();
		this.settlement = new SettlementRow();
		this.cashTransaction = new CashTransactionRow();
		this.holdingHistory = new ArrayList<>();
	}
}