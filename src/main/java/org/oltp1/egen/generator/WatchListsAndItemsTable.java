package org.oltp1.egen.generator;

import java.util.HashSet;
import java.util.Set;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.model.WatchItemRow;
import org.oltp1.egen.model.WatchListAndItemRow;
import org.oltp1.egen.model.WatchListRow;
import org.oltp1.egen.util.TpcRandom;

public class WatchListsAndItemsTable
{

	// Constants
	private static final int LOAD_UNIT_SIZE = 1000;
	private static final long DEFAULT_START_FROM_CUSTOMER = 1;

	private static final long RNG_SEED_TABLE_DEFAULT = 37039940L;

	// Range of security ids indexes
	private static final int MIN_SEC_IDX = 0; // this should always be 0

	// Min number of items in one watch list
	private static final int MIN_ITEMS_IN_WL = 50;

	// Max number of items in one watch list
	private static final int MAX_ITEMS_IN_WL = 150;

	// Percentage of customers that have watch lists
	private static final int PERCENT_WATCH_LIST = 100;

	// Note: these parameters are dependent on the load unit size
	private static final int WATCH_LIST_ID_PRIME = 631;
	private static final int WATCH_LIST_ID_OFFSET = 97;

	// Number of RNG calls to skip for one row in order
	// to not use any of the random values from the previous row.
	private static final int RNG_SKIP_ONE_ROW_WATCH_LIST = 15; // real max count in v3.5: 13

	// Member variables

	private CustomerTable cust;
	private Set<Long> securityIdSet; // needed to generate random unique security ids
	private int wiCount; // # of items for the last list
	private long minSecIdx;
	private long maxSecIdx;
	private final SecurityFile securityFile;
	private boolean initNextLoadUnit;
	private TpcRandom random;
	private boolean hasMoreRecords;
	private WatchListAndItemRow row;

	public WatchListsAndItemsTable(DataFileManager dfm,
			long customerCount,
			long startFromCustomer)
	{
		super();

		this.cust = new CustomerTable(dfm, customerCount, startFromCustomer);
		this.securityIdSet = new HashSet<>();
		this.wiCount = 0;

		this.securityFile = dfm.getSecurityFile();
		this.initNextLoadUnit = false;
		this.random = new TpcRandom(RNG_SEED_TABLE_DEFAULT);

		this.minSecIdx = MIN_SEC_IDX;
		this.maxSecIdx = securityFile.getConfiguredSecurityCount() - 1; // -1 because security indexes are 0-based

		this.hasMoreRecords = cust.hasMoreRecords();
		// Initialize customer for the starting watch list id.
		// Iterate through customers to find the next one with a watch list

		do
		{
			if (cust.getCurrentC_ID() % LOAD_UNIT_SIZE == 0)
			{
				initNextLoadUnit = true; // delay calling initNextLoadUnit until the start of the row generation
			}
			cust.generateNextC_ID();
		}
		while (!random.rndPercent(PERCENT_WATCH_LIST) && cust.hasMoreRecords());
	}

	public boolean hasMoreRecords()
	{
		return hasMoreRecords;
	}

	private void generateNextWlId()
	{
		long customerIdForCalc = cust.getCurrentC_ID() - DEFAULT_START_FROM_CUSTOMER;

		row.watchList.WL_ID = (customerIdForCalc / LOAD_UNIT_SIZE) * LOAD_UNIT_SIZE + // strip last 3 digits
				(customerIdForCalc * WATCH_LIST_ID_PRIME + WATCH_LIST_ID_OFFSET)
						% LOAD_UNIT_SIZE
				+ DEFAULT_START_FROM_CUSTOMER;
	}

	/**
	 * Reset the state for the next load unit
	 */
	private void initNextLoadUnit()
	{
		random
				.setSeed(
						TpcRandom
								.rndNthElement(
										RNG_SEED_TABLE_DEFAULT,
										cust.getCurrentC_ID() * RNG_SKIP_ONE_ROW_WATCH_LIST));

	}

	public void generateNextRecord()
	{
		long customerId;
		boolean ret = false;

		if (initNextLoadUnit)
		{
			initNextLoadUnit();
			initNextLoadUnit = false;
		}

		customerId = cust.getCurrentC_ID();

		// Now generate Watch Items for this Watch List
		wiCount = random.rndIntRange(MIN_ITEMS_IN_WL, MAX_ITEMS_IN_WL); // number of items in the watch list

		row = new WatchListAndItemRow(wiCount);

		generateNextWlId();

		// Fill the customer ID
		row.watchList.WL_C_ID = customerId;

		securityIdSet.clear();
		while (securityIdSet.size() < wiCount)
		{
			// Generate random security id and insert into the set
			long securityIndex = random.rndInt64Range(minSecIdx, maxSecIdx);
			securityIdSet.add(securityIndex);
		}

		// Now remove from the set and fill watch items rows
		int i = 0;
		for (Long securityIndex : securityIdSet)
		{
			row.watchItems[i].WI_WL_ID = row.watchList.WL_ID; // same watch list id for all items
			// get the next element from the set
			row.watchItems[i].WI_S_SYMB = securityFile.createSymbol(securityIndex);
			i++;
		}

		ret = false; // initialize for the case of currently processing the last customer

		// Iterate through customers to find the next one with a watch list
		while (!ret && cust.hasMoreRecords())
		{
			if (cust.getCurrentC_ID() % LOAD_UNIT_SIZE == 0)
			{
				initNextLoadUnit = true; // delay calling initNextLoadUnit until the start of the row generation
			}

			cust.generateNextC_ID();

			ret = random.rndPercent(PERCENT_WATCH_LIST);
		}

		// If the new customer has a watch list, ret was set to true.
		// If there are no more customers, ret was initialized to false.
		if (ret)
		{
			hasMoreRecords = true;
		}
		else
		{
			hasMoreRecords = false;
		}
	}

	public WatchListRow getWlRow()
	{
		return row.watchList;
	}

	public WatchItemRow[] getWiRows()
	{
		return row.watchItems;
	}
}
