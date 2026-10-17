package org.oltp1.runner.tx.trade_result;

import java.math.BigDecimal;
import java.util.Objects;

import org.oltp1.runner.model.TradeType;
import org.oltp1.runner.runtime.TxInput;

public record TxTradeResultInput(
		BigDecimal requestedPrice,
		String symbol,
		long tradeId,
		int quantity,
		TradeType tradeType) implements TxInput
{

	public TxTradeResultInput
	{
		Objects.requireNonNull(requestedPrice, "requestedPrice");
		Objects.requireNonNull(symbol, "symbol");
		Objects.requireNonNull(tradeType, "tradeType");
		if (symbol.isBlank())
			throw new IllegalArgumentException("blank symbol");
		if (tradeId <= 0)
			throw new IllegalArgumentException("tradeId <= 0");
		if (quantity <= 0)
			throw new IllegalArgumentException("quantity <= 0");
		if (requestedPrice.signum() < 0)
			throw new IllegalArgumentException("negative price");
	}
}