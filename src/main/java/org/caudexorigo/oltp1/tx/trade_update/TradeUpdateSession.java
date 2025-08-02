package org.caudexorigo.oltp1.tx.trade_update;

import org.caudexorigo.perf.TxSession;

public class TradeUpdateSession extends TxSession
{
	@Override
	public String toString()
	{
		return String.format("TradeUpdateSession [%n%s%n]", getSessionData());
	}
}