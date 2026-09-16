package org.oltp1.runner.runtime;

/**
 * Identifies a benchmarked transaction together with its nominal TPC-E
 * transaction-mix weight and whether it is produced by the Market Exchange
 * Emulator.
 * <p>
 * {@link #getTargetWeight()} is the nominal TPC-E percentage; it is used only
 * as the reported {@code Target(%)}. Client transactions are selected after
 * {@link org.oltp1.runner.runtime.WorkLoadClient} renormalizes the client-side
 * weights; MEE-generated transactions and Data-Maintenance are not part of
 * that mix. The reported {@code Actual(%)} is the observed share over every
 * attempted transaction, so it is not expected to equal the target.
 */
public enum TransactionSpec
{
	DATA_MAINTENANCE("Data-Maintenance", 0.0, TransactionType.PERIODIC)
	, TRADE_CLEANUP("Trade-Cleanup", 0.0, TransactionType.ONE_SHOT)
	
	, BROKER_VOLUME("Broker-Volume", 4.9, TransactionType.WORKLOAD)
	, CUSTOMER_POSITION("Customer-Position", 13.0, TransactionType.WORKLOAD)
	, MARKET_WATCH("Market-Watch", 18.0, TransactionType.WORKLOAD)
	, SECURITY_DETAIL("Security-Detail", 14.0, TransactionType.WORKLOAD)
	, TRADE_LOOKUP("Trade-Lookup", 8.0, TransactionType.WORKLOAD)
	, TRADE_ORDER("Trade-Order", 10.1, TransactionType.WORKLOAD)	
	, TRADE_STATUS("Trade-Status", 19.0, TransactionType.WORKLOAD)
	, TRADE_UPDATE("Trade-Update", 2.0, TransactionType.WORKLOAD)
	, BASELINE("Baseline", 100.0, TransactionType.WORKLOAD)
	
	, MARKET_FEED("Market-Feed", 1.0, TransactionType.MEE)
	, TRADE_RESULT("Trade-Result", 10.0, TransactionType.MEE);

	private final String displayName;
	private final double targetWeight;
	private final TransactionType txType;

	public static TransactionSpec fromDisplayName(String displayName)
	{
		for (TransactionSpec type : TransactionSpec.values())
		{
			if (type.getDisplayName().equals(displayName))
			{
				return type;
			}
		}

		throw new IllegalArgumentException(String.format("Unknown TransactionSpec displayName: %s", displayName));
	}

	TransactionSpec(String displayName, double targetWeight, TransactionType txType)
	{
		this.displayName = displayName;
		this.targetWeight = targetWeight;
		this.txType = txType;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public boolean isMee()
	{
		return txType == TransactionType.MEE;
	}
	
	public boolean isWorkload()
	{
		return txType == TransactionType.WORKLOAD;
	}
	
	public TransactionType getTxType()
	{
		return txType;
	}

	public double getTargetWeight()
	{
		return targetWeight;
	}
}
