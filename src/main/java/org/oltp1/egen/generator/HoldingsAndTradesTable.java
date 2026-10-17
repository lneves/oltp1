package org.oltp1.egen.generator;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.model.AccountId;
import org.oltp1.egen.model.AccountSecurity;
import org.oltp1.egen.model.CustomerTier;
import org.oltp1.egen.util.TpcRandom;

/**
 * This class is responsible for generating the portfolio of securities for
 * customer accounts. It determines how many and which specific securities an
 * account holds, and provides functionality for selecting a random security for
 * a trade.
 */
public class HoldingsAndTradesTable
{
	public static final int[][] MIN_SECURITIES_PER_ACCOUNT_RANGE = {
			{ 6, 4, 2, 2, 0, 0, 0, 0, 0, 0 },
			{ 0, 7, 5, 4, 3, 2, 2, 2, 0, 0 },
			{ 0, 0, 0, 0, 4, 4, 3, 3, 2, 2 }
	};
	public static final int[][] MAX_SECURITIES_PER_ACCOUNT_RANGE = {
			{ 14, 16, 18, 18, 0, 0, 0, 0, 0, 0 },
			{ 0, 13, 15, 16, 17, 18, 18, 18, 0, 0 },
			{ 0, 0, 0, 0, 16, 16, 17, 17, 18, 18 }
	};
	public static final int MAX_SECURITIES_PER_ACCOUNT = 18;
	public static final int DEFAULT_LOAD_UNIT_SIZE = 1000;
	public static final int MAX_ACCOUNTS_PER_CUST = 10;
	public static final int ABORTED_TRADE_MOD_FACTOR = 51;
	public static final int ABORT_TRADE = 101;

	// RNG Seeds
	private static final long RNG_SEED_TABLE_DEFAULT = 37039940L;
	public static final long RNG_SEED_BASE_NUMBER_OF_SECURITIES = 23361736;
	public static final long RNG_SEED_BASE_STARTING_SECURITY_ID = 12020070;

	private final TpcRandom rnd;
	private final CustomerAccountsAndPermissionsTable customerAccountTable;
	private final long securityCount;
	private long[] securityIds = new long[MAX_SECURITIES_PER_ACCOUNT];

	public HoldingsAndTradesTable(DataFileManager dfm, int loadUnitSize, long customerCount, long startFromCustomer)
	{
		this.rnd = new TpcRandom(RNG_SEED_TABLE_DEFAULT);
		this.customerAccountTable = new CustomerAccountsAndPermissionsTable(dfm, loadUnitSize, customerCount, startFromCustomer);
		this.securityCount = dfm.getSecurityFile().getConfiguredSecurityCount();
	}

	/**
	 * Resets the state for the next load unit.
	 */
	public void initNextLoadUnit(long tradesToSkip, long startingAccountId)
	{
		long seed = TpcRandom.rndNthElement(RNG_SEED_TABLE_DEFAULT, tradesToSkip);
		this.rnd.setSeed(seed);
		this.customerAccountTable.initNextLoadUnit();
	}

	/**
	 * Generates a random customer account and a security within that account.
	 *
	 * @param customer
	 *            The customer's ID.
	 * @param tier
	 *            The customer's tier.
	 * @return A {@link RandomAccountSecurityResult} containing account and security
	 *         info.
	 */
	public AccountSecurity generateRandomAccountSecurity(long customer, CustomerTier tier)
	{
		// Select random account for the customer
		AccountId accountInfo = this.customerAccountTable.generateRandomAccountId(this.rnd, customer, tier);
		long customerAccountId = accountInfo.caId;
		int accountCount = accountInfo.accCount;

		int totalAccountSecurities = getNumberOfSecurities(customerAccountId, tier, accountCount);

		// Select random security in the account (1-based index)
		int securityAccountIndex = rnd.rndIntRange(1, totalAccountSecurities);

		long securityFlatFileIndex = getSecurityFlatFileIndex(customerAccountId, securityAccountIndex);

		return new AccountSecurity(customerAccountId, securityFlatFileIndex, securityAccountIndex);
	}

	/**
	 * Determines if a trade should be marked as aborted based on its ID.
	 */
	public boolean isAbortedTrade(long tradeId)
	{
		return ABORTED_TRADE_MOD_FACTOR == tradeId % ABORT_TRADE;
	}

	private int getNumberOfSecurities(long caId, CustomerTier tier, int accountCount)
	{
		int numberOfSecurities = 0;

		int tierOne = CustomerTier.TIER_ONE.getValue();

		int minRange = MIN_SECURITIES_PER_ACCOUNT_RANGE[tier.getValue() - tierOne][accountCount - 1];
		int maxRange = MAX_SECURITIES_PER_ACCOUNT_RANGE[tier.getValue() - tierOne][accountCount - 1];

		long oldSeed = rnd.getSeed();
		rnd.setSeed(TpcRandom.rndNthElement(RNG_SEED_BASE_NUMBER_OF_SECURITIES, caId));
		numberOfSecurities = rnd.rndIntRange(minRange, maxRange);

		rnd.setSeed(oldSeed); // Restore original RNG state

		return numberOfSecurities;
	}

	protected long getSecurityFlatFileIndex(long customerAccountId, int securityAccountIndex)
	{
		long securityFlatFileIndex = -1L;

		long oldSeed = rnd.getSeed();
		rnd.setSeed(getStartingSecIdSeed(customerAccountId));

		int generatedIndexCount = 0; // number of currently generated unique flat file indexes

		int i = 0;
		while (generatedIndexCount < securityAccountIndex)
		{
			securityFlatFileIndex = rnd.rndInt64Range(0, securityCount - 1);

			boolean isDuplicate = false;

			for (i = 0; i < generatedIndexCount; i++)
			{
				if (securityIds[i] == securityFlatFileIndex)
				{
					isDuplicate = true;
					break;
				}
			}

			// If a duplicate is found, overwrite it in the same location
			// so basically no changes are made.
			securityIds[i] = securityFlatFileIndex;

			// If no duplicate is found, increment the count of unique ids
			if (!isDuplicate)
			{
				generatedIndexCount++;
			}
		}
		rnd.setSeed(oldSeed); // Restore original RNG state
		return securityFlatFileIndex;
	}

	/*
	 * Get seed for the starting security ID seed for a given customer id.
	 */
	long getStartingSecIdSeed(long caId)
	{
		return (TpcRandom.rndNthElement(RNG_SEED_BASE_STARTING_SECURITY_ID, caId * MAX_SECURITIES_PER_ACCOUNT));
	}

}