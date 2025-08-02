package org.caudexorigo.oltp1.generator;

import org.caudexorigo.oltp1.tx.broker_volume.TxBrokerVolumeInput;

public class BrokerVolumeInputGenerator
{
	// Constants from TxnHarnessStructs.h
	private static final int MIN_BROKER_LIST_LEN = 20;
	private static final int MAX_BROKER_LIST_LEN = 40;

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
		int numBrokers = crand.rndIntRange(MIN_BROKER_LIST_LEN, MAX_BROKER_LIST_LEN);

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