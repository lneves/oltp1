package org.oltp1.runner.model;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum TradeStatus
{
	SUBMITTED("SBMT"), PENDING("PNDG"), COMPLETED("CMPT"), CANCELED("CNCL");

	public final String id;

	TradeStatus(String id)
	{
		this.id = id;
	}

	private static final Map<String, TradeStatus> BY_ID = Stream.of(values()).collect(Collectors.toMap(e -> e.id, Function.identity()));

	public static TradeStatus fromId(String id)
	{
		return BY_ID.get(id);
	}
}