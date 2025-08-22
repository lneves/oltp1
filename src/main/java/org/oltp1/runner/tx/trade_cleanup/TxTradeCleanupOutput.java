package org.oltp1.runner.tx.trade_cleanup;

import org.oltp1.runner.perf.TxOutput;

public class TxTradeCleanupOutput extends TxOutput
{
	public int trades_cleaned_up;

	public TxTradeCleanupOutput()
	{
		super(0);
	}

	@Override
	public String toString()
	{
		return String.format("TxTradeCleanupOutput[trades_cleaned_up=%s, tx_status=%s, tx_status_message=%s]", trades_cleaned_up, getStatus(), getStatusMessage());
	}
}