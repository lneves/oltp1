package org.oltp1.runner.model;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum TradeType
{
	LIMIT_BUY("TLB", false, false), // limit buy
	LIMIT_SELL("TLS", false, true), // limit sell
	MARKET_BUY("TMB", true, false), // market buy
	MARKET_SELL("TMS", true, true), // market sell
	STOP_LOSS("TSL", false, true); // stop loss

	public final String id;
	private final boolean market;
	private final boolean sell;

	TradeType(String id, boolean market, boolean sell)
	{
		this.id = id;
		this.market = market;
		this.sell = sell;
	}

	public boolean isMarket()
	{
		return market;
	}

	public boolean isSell()
	{
		return sell;
	}

	private static final Map<String, TradeType> BY_ID = Stream.of(values()).collect(Collectors.toMap(e -> e.id, Function.identity()));

	public static TradeType fromId(String id)
	{
		return BY_ID.get(id);
	}
}