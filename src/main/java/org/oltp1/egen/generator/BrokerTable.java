package org.oltp1.egen.generator;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.model.BrokerRow;
import org.oltp1.egen.util.TpcRandom;

public class BrokerTable
{
	private static final long IDENT_T_SHIFT = 4300000000L;
	private static final long STARTING_BROKER_ID = 1L;
	private static final int BROKERS_DIV = 100;
	private static final long BROKER_NAME_ID_SHIFT = 1_000_000L;
	private static final long DEFAULT_LOAD_UNIT_SIZE = 1000;

	private final DataFileManager dfm;
	private final Person person;

	private long totalBrokersInInstance;
	private long startFromBroker;

	private int[] numTrades;
	private double[] commTotal;

	private long lastRowNumber; // 0-based index for this instance's generation
	private boolean hasMoreRecords;

	private BrokerRow currentRow;
	private long startFromCustomer;
	private long customerCount;

	public BrokerTable(DataFileManager dfm, long customerCount, long startFromCustomer)
	{
		this.dfm = dfm;
		this.customerCount = customerCount;
		this.startFromCustomer = startFromCustomer;
		// The Person generator needs a random instance, but its state will be
		// saved and restored for the deterministic name generation.
		this.person = new Person(dfm, BROKER_NAME_ID_SHIFT, true);

		// Initial setup, will be properly configured in initForGen
		this.totalBrokersInInstance = 0;
		this.startFromBroker = 0;
		this.lastRowNumber = 0;
		this.hasMoreRecords = false;
	}

	public void initForGen(long customerCount, long startFromCustomer)
	{
		if (totalBrokersInInstance != customerCount / BROKERS_DIV || (numTrades == null) || (commTotal == null))
		{
			this.totalBrokersInInstance = customerCount / BROKERS_DIV;

		}

		this.numTrades = new int[(int) totalBrokersInInstance];
		this.commTotal = new double[(int) totalBrokersInInstance];

		for (int i = 0; i < totalBrokersInInstance; i++)
		{
			numTrades[i] = 0;
			commTotal[i] = 0.0;
		}

		if (this.startFromCustomer != ((startFromCustomer / BROKERS_DIV) + STARTING_BROKER_ID + IDENT_T_SHIFT))
		{
			// Multiplying by iBrokersDiv again to get 64-bit broker ids
			// with 4.3bln IDENT_T shift value.
			// Removing shift factor prior to arithmetic so that contiguous
			// B_IDs values are obtained, and then add it back so that we
			// get shifted values.
			//
			startFromBroker = (startFromCustomer / BROKERS_DIV) + STARTING_BROKER_ID + IDENT_T_SHIFT;
		}

		this.lastRowNumber = 0;
		this.hasMoreRecords = this.totalBrokersInInstance > 0;

		if (this.startFromCustomer != startFromCustomer)
		{
			person.initNextLoadUnit(DEFAULT_LOAD_UNIT_SIZE / BROKERS_DIV);
		}
	}

	public void updateTradeAndCommissionYTD(long brokerId, int tradeIncrement, double commissionIncrement)
	{
		if (brokerId >= startFromBroker && brokerId < (startFromBroker + totalBrokersInInstance))
		{
			int brokerIndex = (int) (brokerId - startFromBroker);
			numTrades[brokerIndex] += tradeIncrement;
			commTotal[brokerIndex] += commissionIncrement;
		}
	}

	public boolean hasMoreRecords()
	{
		if (lastRowNumber >= totalBrokersInInstance)
		{
			hasMoreRecords = false;
		}
		return hasMoreRecords;
	}

	public BrokerRow generateNextRecord()
	{
		currentRow = new BrokerRow();

		currentRow.B_ID = startFromBroker + lastRowNumber;
		currentRow.B_ST_ID = dfm.getStatusTypeDataFile().get(1).st_id; // 1 = Active
		currentRow.B_NAME = generateBrokerName(currentRow.B_ID);
		currentRow.B_NUM_TRADES = numTrades[(int) lastRowNumber];
		currentRow.B_COMM_TOTAL = commTotal[(int) lastRowNumber];

		lastRowNumber++;

		return currentRow;
	}

	public long generateRandomBrokerId(TpcRandom rnd)
	{
		return rnd.rndInt64Range(startFromBroker, startFromBroker + totalBrokersInInstance - 1);
	}

	/**
	 * Deterministically generates a broker's name based on their ID.
	 */
	private String generateBrokerName(long brokerId)
	{
		// Broker names are generated from a different part of the ID space
		long nameId = brokerId + BROKER_NAME_ID_SHIFT;
		String fName = person.getFirstName(nameId, person.isMaleGender(nameId));
		char mInitial = person.getMiddleName(nameId);
		String lName = person.getLastName(nameId);

		return String.format("%s %c. %s", fName, mInitial, lName);
	}

	public BrokerRow getRow()
	{
		return currentRow;
	}
}