package org.oltp1.runner.generator;

import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.model.CustomerTier;
import org.oltp1.runner.model.RandomCustomer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.data.Row;

public final class CustomerSelector
{
	private static final Logger log = LoggerFactory.getLogger(CustomerSelector.class);

	// From Utilities/inc/MiscConsts.h
	private static final long T_IDENT_SHIFT = 4300000000L;
	private static final long DEFAULT_LOAD_UNIT_SIZE = 1000L;

	// Constants from Person.h and RNGSeeds.h
	private static final String TAX_ID_FORMAT = "nnnaannnnaannn";
	private static final char[] TAX_ID_FORMAT_CHARS = TAX_ID_FORMAT.toCharArray();
	private static final int TAX_ID_FMT_LEN = 14;
	private static final long RNG_SEED_BASE_TAX_ID = 8731255L;

	private static final int[] MIN_ACCOUNTS_PER_CUST_RANGE = { 1, 2, 5 };
	private static final int[] MAX_ACCOUNTS_PER_CUST_RANGE = { 4, 8, 10 };
	private final static int MAX_ACCOUNTS_PER_CUST = 10;

	private final long startFromCustomer;
	private final long customerCount;
	private final boolean partitionByCID;
	private final int partitionPercent;
	private long myStartFromCustomer;
	private long myCustomerCount;

	/**
	 * Constructor for non-partitioned customer selection.
	 */
	public CustomerSelector(SqlContext sqlCtx)
	{
		try (Connection con = sqlCtx.getSql2o().open())
		{
			Row row = con
					.createQuery("SELECT MIN(c_id) AS min_c_id, COUNT(*) AS c_count FROM customer")
					.executeAndFetchTable()
					.rows()
					.getFirst();

			this.startFromCustomer = row.getLong("min_c_id");
			this.customerCount = row.getLong("c_count");

			log.info("Start from Customer: {}", startFromCustomer);
			log.info("Customer count: {}", customerCount);
		}
		catch (Throwable t)
		{
			throw new RuntimeException(t);
		}

		this.partitionByCID = false;
		this.partitionPercent = 0;
		this.myStartFromCustomer = 0;
		this.myCustomerCount = 0;
	}

	/**
	 * Determines a customer's tier directly from their ID.
	 */
	public CustomerTier getTier(long customerId)
	{
		long inverseCID = inversePermute(cLow(customerId), cHigh(customerId));

		if (inverseCID < 200)
		{
			return CustomerTier.TierOne;
		}
		else if (inverseCID < 800)
		{
			return CustomerTier.TierTwo;
		}
		else
		{
			return CustomerTier.TierThree;
		}
	}

	/**
	 * Generates a non-uniform random customer ID and tier.
	 *
	 * @return A RandomCustomer object containing the new ID and tier.
	 */

	public RandomCustomer randomCustomer()
	{
		CRandom random = ThreadLocalCRandom.get();

		// Uniformly select the higher portion of the C_ID (the load unit)
		long cHigh;
		if (partitionByCID && random.rndPercent(partitionPercent))
		{
			// Generate a load unit inside the partition
			cHigh = (random.rndInt64Range(myStartFromCustomer, myStartFromCustomer + myCustomerCount - 1) - 1) / DEFAULT_LOAD_UNIT_SIZE;
		}
		else
		{
			// Generate a load unit across the entire range
			cHigh = (random.rndInt64Range(startFromCustomer, startFromCustomer + customerCount - 1) - 1) / DEFAULT_LOAD_UNIT_SIZE;
		}

		// Non-uniformly select the lower portion of the C_ID
		double fCW = random.rndDoubleIncrRange(0.0001, 2000, 0.000000001);
		long cLow;
		CustomerTier tier;

		if (fCW <= 200)
		{
			// Tier 1
			cLow = (long) Math.ceil(Math.sqrt(22500 + 500 * fCW) - 151);
			tier = CustomerTier.TierOne;
		}
		else if (fCW <= 1400)
		{
			// Tier 2
			cLow = (long) Math.ceil(Math.sqrt(290000 + 1000 * fCW) - 501);
			tier = CustomerTier.TierTwo;
		}
		else
		{
			// Tier 3
			cLow = (long) Math.ceil(149 + Math.sqrt(500 * fCW - 277500));
			tier = CustomerTier.TierThree;
		}

		long customerId = cHigh * 1000 + permute(cLow, cHigh) + 1;
		return new RandomCustomer(customerId, tier);
	}

	/**
	 * Gets the formatted tax ID for a given Customer ID.
	 */
	public String getTaxId(long customerId)
	{
		CRandom random = ThreadLocalCRandom.get();
		long originalSeed = random.getSeed();
		long idSeed = random.rndNthElement(RNG_SEED_BASE_TAX_ID, (customerId * TAX_ID_FMT_LEN));
		random.setSeed(idSeed);

		StringBuilder sb = new StringBuilder();
		for (char c : TAX_ID_FORMAT_CHARS)
		{
			if (c == 'n')
			{
				sb.append("0123456789".charAt(random.rndIntRange(0, 9)));
			}
			else if (c == 'a')
			{
				sb.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt(random.rndIntRange(0, 25)));
			}
		}
		random.setSeed(originalSeed);
		return sb.toString();
	}

	public int getNumberOfAccounts(RandomCustomer c)
	{
		int tierIndex = c.cTier.ordinal();
		int minAccountCount = MIN_ACCOUNTS_PER_CUST_RANGE[tierIndex];
		int mod = MAX_ACCOUNTS_PER_CUST_RANGE[tierIndex] - minAccountCount + 1;
		int inverseCid = getInverseCid(c.cId);

		// Note: the calculations below assume load unit contains 1000 customers.
		if (inverseCid < 200)
		{ // Tier 1
			return ((inverseCid % mod) + minAccountCount);
		}
		else if (inverseCid < 800)
		{ // Tier 2
			return (((inverseCid - 200 + 1) % mod) + minAccountCount);
		}
		else
		{ // Tier 3
			return (((inverseCid - 800 + 2) % mod) + minAccountCount);
		}
	}

	/*
	 * Get starting account id for a given customer id. This is needed to know what
	 * account ids belong to a given customer.
	 *
	 */
	private static long getStartingAccId(long cid)
	{
		// start account ids on the next boundary for the new customer
		return ((cid - 1) * MAX_ACCOUNTS_PER_CUST + 1);
	}

	public long getRndAccIdForCustomer(RandomCustomer c)
	{
		CRandom random = ThreadLocalCRandom.get();

		int numAccounts = getNumberOfAccounts(c);
		long startingAccountId = getStartingAccId(c.cId);

		// Select a random offset within the customer's account range
		long accountOffset = random.rndInt64Range(0, numAccounts - 1);

		return startingAccountId + accountOffset;
	}

	public long getMaxAccId()
	{
		return (customerCount + T_IDENT_SHIFT) * MAX_ACCOUNTS_PER_CUST;
	}

	// lower 3 digits
	private long cLow(long customerID)
	{
		return (customerID - 1) % 1000;
	}

	// higher 3 digits
	private long cHigh(long customerID)
	{
		return (customerID - 1) / 1000;
	}

	private long permute(long low, long high)
	{
		return (677 * low + 33 * (high + 1)) % 1000;
	}

	private long inversePermute(long low, long high)
	{
		// Extra mod to make the result always positive
		return (((613 * (low - 33 * (high + 1))) % 1000) + 1000) % 1000;
	}

	// Return scrambled inverse customer id in range of 0 to 999.
	private int getInverseCid(long cid)
	{
		int cHigh = (int) cHigh(cid);
		int inverseCid = (int) inversePermute(cLow(cid), cHigh);

		if (inverseCid < 200)
		{ // Tier 1: value 0 to 199
			return ((3 * inverseCid + (cHigh + 1)) % 200);
		}
		else
		{
			if (inverseCid < 800)
			{ // Tier 2: value 200 to 799
				return (((59 * inverseCid + 47 * (cHigh + 1)) % 600) + 200);
			}
			else
			{ // Tier 3: value 800 to 999
				return (((23 * inverseCid + 17 * (cHigh + 1)) % 200) + 800);
			}
		}
	}

	public long randomAccId(RandomCustomer c)
	{
		return getRndAccIdForCustomer(c);
	}

	public long randomAccId()
	{
		RandomCustomer c = randomCustomer();
		return randomAccId(c);
	}

}