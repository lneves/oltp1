package org.oltp1.runner.tx.trade_status;

import org.oltp1.runner.runtime.TxInput;

public record TxTradeStatusInput(long acctId) implements TxInput
{

}