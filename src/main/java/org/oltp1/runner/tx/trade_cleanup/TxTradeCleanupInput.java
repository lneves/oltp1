package org.oltp1.runner.tx.trade_cleanup;

import org.oltp1.runner.runtime.TxInput;

public record TxTradeCleanupInput(String stCanceledId, String stPendingId, String stSubmittedId, long startTradeId) implements TxInput
{

}