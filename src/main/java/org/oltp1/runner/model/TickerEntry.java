package org.oltp1.runner.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single trade update within the Market-Feed transaction.
 * <p>
 * The JSON property names must match the ones the Market-Feed dialects read out
 * of the serialized batch, so they are pinned here rather than derived from the
 * record components.
 */

public record TickerEntry(
		String symbol,
		@JsonProperty("trade_price") BigDecimal price,
		@JsonProperty("trade_qty") int quantity)
{

	@Override
	public String toString()
	{
		return String.format("Symbol: %-5s, Price: %8.2f, Quantity: %d", symbol, price.doubleValue(), quantity);
	}
}