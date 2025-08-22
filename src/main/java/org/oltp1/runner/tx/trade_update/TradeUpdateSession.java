package org.oltp1.runner.tx.trade_update;

import org.oltp1.runner.perf.TxSession;

public class TradeUpdateSession extends TxSession
{
	@Override
	public String toString()
	{
		return String.format("TradeUpdateSession [%n%s%n]", getSessionData());
	}
}