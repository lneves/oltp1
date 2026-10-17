package org.oltp1.egen.generator;

import org.oltp1.egen.model.CustomerTier;
import org.oltp1.egen.util.TpcRandom;

/**
 * This class encapsulates customer tier distribution functions and provides
 * functionality to: - Generate customer tier based on customer ID - Generate
 * non-uniform customer ID - Generate customer IDs in a specified partition, and
 * outside the specified partition a set percentage of the time.
 */
public class CustomerSelection
{
	private static final long IDENT_T_SHIFT = 4300000000L;

	// External random number generator wrapper
	private TpcRandom random;

	private long startFromCustomer;
	private long customerCount;

	// Used when partitioning by C_ID
	private boolean partitionByCID;
	private int partitionPercent;
	private long myStartFromCustomer;
	private long myCustomerCount;

	/**
	 * Default constructor.
	 */
	public CustomerSelection()
	{
		this.random = null;
		this.startFromCustomer = 0 + IDENT_T_SHIFT;
		this.customerCount = 0;
		this.partitionByCID = false;
		this.partitionPercent = 0;
		this.myStartFromCustomer = 0 + IDENT_T_SHIFT;
		this.myCustomerCount = 0;
	}

	/**
	 * Constructor to set the customer range when not partitioning.
	 */
	public CustomerSelection(TpcRandom random, long startFromCustomer, long customerCount)
	{
		this.random = random;
		this.startFromCustomer = startFromCustomer + IDENT_T_SHIFT;
		this.customerCount = customerCount;
		this.partitionByCID = false;
		this.partitionPercent = 0;
		this.myStartFromCustomer = 0 + IDENT_T_SHIFT;
		this.myCustomerCount = 0;
	}

	/**
	 * Constructor to set subrange when partitioning by C_ID.
	 */
	public CustomerSelection(TpcRandom random,
			long startFromCustomer,
			long customerCount,
			int partitionPercent,
			long myStartFromCustomer,
			long myCustomerCount)
	{
		this.random = random;
		this.startFromCustomer = startFromCustomer + IDENT_T_SHIFT;
		this.customerCount = customerCount;
		this.partitionByCID = true;
		this.partitionPercent = partitionPercent;
		this.myStartFromCustomer = myStartFromCustomer + IDENT_T_SHIFT;
		this.myCustomerCount = myCustomerCount;

		if ((startFromCustomer == myStartFromCustomer) && (customerCount == myCustomerCount))
		{
			// Even though the partitioning constructor was called, we're apparently
			// not really partitioning.
			this.partitionByCID = false;
		}
	}

	/**
	 * Re-set the customer range for the partition.
	 */
	public void setPartitionRange(long startFromCustomer, long customerCount)
	{
		if (partitionByCID)
		{
			this.myStartFromCustomer = startFromCustomer;
			this.myCustomerCount = customerCount;
		}
	}

	/**
	 * Get lower 3 digits.
	 */
	private long getLowerId(long customerId)
	{
		return ((customerId - 1) % 1000);
	}

	/**
	 * Get higher digits.
	 */
	private long getHigherId(long customerId)
	{
		return ((customerId - 1) / 1000);
	}

	/**
	 * Forward permutation (used to convert ordinal C_ID into real C_ID).
	 */
	private long permute(long low, long high)
	{
		return ((677 * low + 33 * (high + 1)) % 1000);
	}

	/**
	 * Inverse permutation (used to convert real C_ID into its ordinal number).
	 */
	private long inversePermute(long low, long high)
	{
		// Extra mod to make the result always positive
		return (((((613 * (low - 33 * (high + 1))) % 1000) + 1000) % 1000));
	}

	/**
	 * Return scrambled inverse customer id in range of 0 to 999.
	 */
	public int getInverseCid(long customerId)
	{
		int higherId = (int) getHigherId(customerId);
		int inverseCID = (int) inversePermute(getLowerId(customerId), higherId);

		if (inverseCID < 200)
		{ // Tier 1: value 0 to 199
			return ((3 * inverseCID + (higherId + 1)) % 200);
		}
		else if (inverseCID < 800)
		{ // Tier 2: value 200 to 799
			return (((59 * inverseCID + 47 * (higherId + 1)) % 600) + 200);
		}
		else
		{ // Tier 3: value 800 to 999
			return (((23 * inverseCID + 17 * (higherId + 1)) % 200) + 800);
		}
	}

	/**
	 * Return customer tier.
	 */
	public CustomerTier getTier(long customerId)
	{
		long reversedCustomerId = inversePermute(getLowerId(customerId), getHigherId(customerId));

		if (reversedCustomerId < 200)
		{
			return CustomerTier.TIER_ONE;
		}
		else if (reversedCustomerId < 800)
		{
			return CustomerTier.TIER_TWO;
		}
		else
		{
			return CustomerTier.TIER_THREE;
		}
	}

	/**
	 * Return a non-uniform random customer and the associated tier. Note: In Java,
	 * we'll return a CustomerResult object since Java doesn't support
	 * pass-by-reference for primitives.
	 */
	public RandomCustomer generateRandomCustomer()
	{
		// Can't use this function if there is no external RNG.
		if (random == null)
		{
			return null;
		}

		double customerWeight = random.rndDoubleIncrRange(0.0001, 2000, 0.000000001);

		// Uniformly select the higher portion of the C_ID.
		// Use "short-circuit" logic to avoid unnecessary call to RNG.
		long higherId;
		if (partitionByCID && random.rndPercent(partitionPercent))
		{
			// Generate a load unit inside the partition.
			higherId = (random
					.rndInt64Range(
							myStartFromCustomer,
							myStartFromCustomer + myCustomerCount - 1)
					- 1) // minus 1 for the upper boundary case
					/ 1000;
		}
		else
		{
			// Generate a load unit across the entire range
			higherId = (random
					.rndInt64Range(
							startFromCustomer,
							startFromCustomer + customerCount - 1)
					- 1) // minus 1 for the upper boundary case
					/ 1000;
		}

		// Non-uniformly select the lower portion of the C_ID.
		int lowerId;
		CustomerTier tier;

		if (customerWeight <= 200)
		{
			// tier one
			lowerId = (int) Math.ceil(Math.sqrt(22500 + 500 * customerWeight) - 151);
			tier = CustomerTier.TIER_ONE;
		}
		else if (customerWeight <= 1400)
		{
			// tier two
			lowerId = (int) Math.ceil(Math.sqrt(290000 + 1000 * customerWeight) - 501);
			tier = CustomerTier.TIER_TWO;
		}
		else
		{
			// tier three
			lowerId = (int) Math.ceil(149 + Math.sqrt(500 * customerWeight - 277500));
			tier = CustomerTier.TIER_THREE;
		}

		long customerId = higherId * 1000 + permute(lowerId, higherId) + 1;

		return new RandomCustomer(customerId, tier);
	}
}