package org.oltp1.runner.tx.trade_order;

import org.oltp1.runner.perf.TxSession;

public class TradeOrderSession extends TxSession
{
	@Override
	public String toString()
	{
		return String.format("TradeOrderSession [%n%s%n]", getSessionData());
	}
}