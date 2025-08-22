package org.oltp1.runner.model;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum TradeType
{
	MARKET_BUY("TMB"), MARKET_SELL("TMS"), STOP_LOSS("TSL"), LIMIT_SELL("TLS"), LIMIT_BUY("TLB");

	public final String id;

	TradeType(String id)
	{
		this.id = id;
	}

	private static final Map<String, TradeType> BY_ID = Stream.of(values()).collect(Collectors.toMap(e -> e.id, Function.identity()));

	public static TradeType fromId(String id)
	{
		return BY_ID.get(id);
	}

}