package org.caudexorigo.oltp1.generator;

import org.caudexorigo.oltp1.model.TradeStatus;
import org.caudexorigo.oltp1.tx.trade_cleanup.TxTradeCleanupInput;

public class TradeCleanupInputGenerator
{
	private EnvironementSelector environementSelector;

	public TradeCleanupInputGenerator(EnvironementSelector environementSelector)
	{
		this.environementSelector = environementSelector;
	}

	public TxTradeCleanupInput generateTradeCleanupInput()
	{
		long start_trade_id = environementSelector.getMaxInitialTradeId() + 1;

		return new TxTradeCleanupInput(
				TradeStatus.CANCELED.id,
				TradeStatus.PENDING.id,
				TradeStatus.SUBMITTED.id,
				start_trade_id);
	}
}