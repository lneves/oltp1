package org.oltp1.runner.tx.trade_result;

import org.oltp1.runner.perf.TxSession;

public class TradeResultSession extends TxSession
{
	@Override
	public String toString()
	{
		return String.format("TradeResultSession [%n%s%n]", getSessionData());
	}
}