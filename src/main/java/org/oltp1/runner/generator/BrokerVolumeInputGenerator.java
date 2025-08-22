package org.oltp1.runner.generator;

import org.oltp1.runner.tx.broker_volume.TxBrokerVolumeInput;

public class BrokerVolumeInputGenerator
{
	// Constants from TxnHarnessStructs.h
	private static final int BV_MIN_BROKER_LIST_LEN = 20;
	private static final int BV_MAX_BROKER_LIST_LEN = 40;

	private final BrokerSelector brokerSelector;
	private final SectorSelector sectorSelector;

	public BrokerVolumeInputGenerator(BrokerSelector brokerSelector, SectorSelector sectorSelector)
	{
		this.brokerSelector = brokerSelector;
		this.sectorSelector = sectorSelector;
	}

	public TxBrokerVolumeInput generateBrokerVolumeInput()
	{

		TxBrokerVolumeInput input = new TxBrokerVolumeInput();
		CRandom crand = ThreadLocalCRandom.get();

		// Determine the number of brokers to select.
		int numBrokers = crand.rndIntRange(BV_MIN_BROKER_LIST_LEN, BV_MAX_BROKER_LIST_LEN);

		// Adjust for small databases where the number of brokers might be less than the
		// random number.
		if (numBrokers > brokerSelector.getBrokerCount())
		{
			numBrokers = brokerSelector.getBrokerCount();
		}

		String[] aBrk = brokerSelector
				.getList(numBrokers)
				.stream()
				.map(b -> b.getName())
				.toArray(String[]::new);

		input.broker_list = aBrk;

		// Select a random sector name.
		input.sector_name = sectorSelector.get().getName();

		return input;
	}
}