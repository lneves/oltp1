package org.oltp1.egen.generator;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.model.AccountId;
import org.oltp1.egen.model.AccountPermissionRow;
import org.oltp1.egen.model.CustomerAccountRow;
import org.oltp1.egen.model.CustomerTier;
import org.oltp1.egen.model.TaxStatus;
import org.oltp1.egen.util.TpcRandom;

/**
 * Generates data for the CUSTOMER_ACCOUNT and ACCOUNT_PERMISSION tables. This
 * class is a direct port of the C++ C...Table class, responsible for creating a
 * variable number of accounts for each customer and setting the appropriate
 * permissions on them. Based on inc/CustomerAccountsAndPermissionsTable.h and
 * its logic.
 */
public class CustomerAccountsAndPermissionsTable
{
	// Constants from the C++ implementation
	private static final long IDENT_T_SHIFT = 4300000000L;
	private static final int MAX_ACCOUNTS_PER_CUST = 10;
	private static final int RNG_SKIP_ONE_ROW_CUSTOMER_ACCOUNT = 10;
	private static final long RNG_SEED_TABLE_DEFAULT = 37039940L;
	private static final long RNG_SEED_BASE_NUM_PERMS = 27794203L;
	private static final long RNG_SEED_BASE_PERM_CID1 = 76103629L;
	private static final long RNG_SEED_BASE_PERM_CID2 = 103275149L;
	private static final long RNG_SEED_BASE_TAX_STATUS = 34376701L;
	private static final long RNG_SEED_BASE_BROKER_ID = 75607774L;
	private static final int LOAD_UNIT_SIZE = 1000;
	private static final int BROKERS_DIV = 100;
	private static final long STARTING_BROKER_ID = 1L;

	private static final int PERCENT_ACCOUNT_ADDITIONAL_PERMISSIONS_0 = 60;
	private static final int PERCENT_ACCOUNT_ADDITIONAL_PERMISSIONS_1 = 38;
	private static final int PERCENT_ACCOUNT_ADDITIONAL_PERMISSIONS_2 = 2;

	private final DataFileManager dfm;
	private final CustomerTable customerTable;
	private final Person person;
	private final TpcRandom random;
	private final CustomerSelection customerSelection;

	private CustomerAccountRow currentAccountRow;
	private AccountPermissionRow[] currentPermissionRows;
	private int permsForCurrentAccount;

	private long rowsToGenForCurrentCust;
	private long rowsGeneratedForCurrentCust;
	private long startingAccountIdForCurrentCust;

	private final long brokersCountInLoadUnit;

	public CustomerAccountsAndPermissionsTable(DataFileManager dfm, long customerCount, long startFromCustomer)
	{
		this(dfm, LOAD_UNIT_SIZE, customerCount, startFromCustomer);
	}

	public CustomerAccountsAndPermissionsTable(DataFileManager dfm, long loadUnitSize, long customerCount, long startFromCustomer)
	{
		this.dfm = dfm;
		this.random = new TpcRandom(0);
		this.customerTable = new CustomerTable(dfm, customerCount, startFromCustomer);
		this.person = new Person(dfm, startFromCustomer, true);
		this.customerSelection = new CustomerSelection();
		this.brokersCountInLoadUnit = loadUnitSize / BROKERS_DIV;
		this.rowsToGenForCurrentCust = 0;
		this.rowsGeneratedForCurrentCust = 0;
	}

	public boolean hasMoreRecords()
	{
		return customerTable.hasMoreRecords() || (rowsGeneratedForCurrentCust < rowsToGenForCurrentCust);
	}

	public void generateNextRecord()
	{
		// This logic precisely mirrors the C++ state machine.
		long currentCid = customerTable.getCurrentC_ID();

		if ((currentCid - IDENT_T_SHIFT) % LOAD_UNIT_SIZE == 0)
		{
			initNextLoadUnit();
		}

		if (rowsGeneratedForCurrentCust >= rowsToGenForCurrentCust)
		{
			if (!customerTable.hasMoreRecords())
			{
				rowsGeneratedForCurrentCust = rowsToGenForCurrentCust + 1; // End condition
				return;
			}
			customerTable.generateNextRecord();
			initForNextCustomer();
		}

		generateCaRow();
		generateApRows();
		rowsGeneratedForCurrentCust++;
	}

	private void initForNextCustomer()
	{
		long currentCid = customerTable.getCurrentC_ID();
		CustomerTier tier = customerSelection.getTier(currentCid);
		rowsToGenForCurrentCust = getNumberOfAccounts(currentCid, tier);
		startingAccountIdForCurrentCust = getStartingAccountId(currentCid);
		rowsGeneratedForCurrentCust = 0;
	}

	protected void initNextLoadUnit()
	{
		long currentCid = customerTable.getCurrentC_ID();
		long rngSkipCount = currentCid * MAX_ACCOUNTS_PER_CUST * RNG_SKIP_ONE_ROW_CUSTOMER_ACCOUNT;
		long seed = TpcRandom.rndNthElement(RNG_SEED_TABLE_DEFAULT, rngSkipCount);
		random.setSeed(seed);
	}

	private void generateCaRow()
	{
		currentAccountRow = new CustomerAccountRow();
		long currentCid = customerTable.getCurrentC_ID();

		currentAccountRow.CA_ID = startingAccountIdForCurrentCust + rowsGeneratedForCurrentCust;
		currentAccountRow.CA_C_ID = currentCid;

		long ca_b_id = generateBrokerIdForAccount(currentAccountRow.CA_ID);

		currentAccountRow.CA_B_ID = ca_b_id;

		currentAccountRow.CA_TAX_ST = (char) (getAccountTaxStatus(currentAccountRow.CA_ID).ordinal() + '0');

		String fName = person.getFirstName(currentCid, person.isMaleGender(currentCid));
		String lName = person.getLastName(currentCid);

		int accType;
		TaxStatus taxStatus = getAccountTaxStatus(currentAccountRow.CA_ID);
		int iTaxStatus = taxStatus.ordinal();
		currentAccountRow.CA_TAX_ST = Character.forDigit(iTaxStatus, 10);
		if (taxStatus == TaxStatus.NON_TAXABLE)
		{ // Non-taxable
			accType = ((int) currentAccountRow.CA_ID) % dfm.getNonTaxableAccountNameDataFile().size();
			String suffix = dfm.getNonTaxableAccountNameDataFile().get(accType).name;
			currentAccountRow.CA_NAME = String.format("%s %s %s", fName, lName, suffix);
		}
		else
		{ // Taxable
			accType = ((int) currentAccountRow.CA_ID) % dfm.getTaxableAccountNameDataFile().size();
			String suffix = dfm.getTaxableAccountNameDataFile().get(accType).name;
			currentAccountRow.CA_NAME = String.format("%s %s %s", fName, lName, suffix);
		}

		int pct = random.rndIntRange(1, 100);

		if (pct <= 80)
		{ // 80% positive balance
			currentAccountRow.CA_BAL = random.rndDoubleIncrRange(0.00, 9999999.99, 0.01);
		}
		else
		{
			currentAccountRow.CA_BAL = random.rndDoubleIncrRange(-9999999.99, 0.00, 0.01);
		}
	}

	/**
	 * A direct and faithful port of the C++ GenerateBrokerIdForAccount function.
	 * This is a stateless, deterministic calculation.
	 * 
	 * @param caId
	 *            The Customer Account ID.
	 * @return The deterministically generated Broker ID.
	 */
	protected long generateBrokerIdForAccount(long caId)
	{
		long customerId = ((caId - 1) / MAX_ACCOUNTS_PER_CUST) - IDENT_T_SHIFT;
		long startFromBroker = (customerId / LOAD_UNIT_SIZE) * brokersCountInLoadUnit + STARTING_BROKER_ID + IDENT_T_SHIFT;
		long n = caId - (10 * IDENT_T_SHIFT); // This unusual calculation is a direct port from the C++

		return TpcRandom
				.rndNthInt64Range(
						RNG_SEED_BASE_BROKER_ID,
						n,
						startFromBroker,
						startFromBroker + brokersCountInLoadUnit - 1);
	}

	protected TaxStatus getAccountTaxStatus(long caId)
	{
		long oldSeed = random.getSeed(); // Save state
		random.setSeed(TpcRandom.rndNthElement(RNG_SEED_BASE_TAX_STATUS, caId));
		int threshold = random.rndIntRange(1, 100);
		random.setSeed(oldSeed); // Restore state

		if (threshold <= 20)
			return TaxStatus.NON_TAXABLE;
		if (threshold <= 70)
			return TaxStatus.TAXABLE_AND_WITHHOLD; // 20 + 50
		return TaxStatus.TAXABLE_AND_DONT_WITHHOLD;
	}

	/**
	 * Gets the number of accounts for a given customer.
	 */
	private static final int[] MIN_ACCOUNTS_PER_CUST_RANGE = { 1, 2, 5 };
	private static final int[] MAX_ACCOUNTS_PER_CUST_RANGE = { 4, 8, 10 };

	public int getNumberOfAccounts(long cId, CustomerTier tier)
	{
		int tierIndex = tier.getValue() - 1;
		int minAccountCount = MIN_ACCOUNTS_PER_CUST_RANGE[tierIndex];
		int mod = MAX_ACCOUNTS_PER_CUST_RANGE[tierIndex] - minAccountCount + 1;
		int inverseCid = customerSelection.getInverseCid(cId);

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

	public void generateApRows()
	{
		long currentCid = customerTable.getCurrentC_ID();
		long currentCaId = currentAccountRow.CA_ID;

		int extraPerms = getNumPermsForCA(currentCaId);
		permsForCurrentAccount = extraPerms + 1;
		currentPermissionRows = new AccountPermissionRow[permsForCurrentAccount];

		// Create the primary owner's permission row.
		currentPermissionRows[0] = createPermissionRow(currentCaId, currentCid, "0000"); // C++ uses "0000" for owner

		long[] cids = new long[2]; // For CID_1 and CID_2

		switch (extraPerms)
		{
		case 0:
			// in 60% of the cases there is only the owner, do nothing
			break;

		case 1: // 38%
			getCidsForPermissions(currentCaId, currentCid, cids);
			// generate second account permission row
			currentPermissionRows[1] = createPermissionRow(currentCaId, cids[0], "0001");
			break;

		case 2: // 2%
			getCidsForPermissions(currentCaId, currentCid, cids);
			// generate second account permission row
			currentPermissionRows[1] = createPermissionRow(currentCaId, cids[0], "0001");
			// generate third account permission row
			currentPermissionRows[2] = createPermissionRow(currentCaId, cids[1], "0011");
			break;
		}
	}

	private AccountPermissionRow createPermissionRow(long caId, long cId, String acl)
	{
		AccountPermissionRow row = new AccountPermissionRow();
		row.AP_CA_ID = caId;
		row.AP_ACL = acl;

		// Directly populate the name and tax ID fields using the stateless Person
		// methods.
		row.AP_L_NAME = person.getLastName(cId);
		row.AP_F_NAME = person.getFirstName(cId, person.isMaleGender(cId));
		row.AP_TAX_ID = person.getTaxId(cId);

		return row;
	}

	protected long getStartingAccountId(long customerId)
	{
		return ((customerId - 1) * MAX_ACCOUNTS_PER_CUST + 1);
	}

	public CustomerAccountRow getCARow()
	{
		return currentAccountRow;
	}

	public int getCAPermsCount()
	{
		return permsForCurrentAccount;
	}

	public AccountPermissionRow getAPRow(int i)
	{
		return currentPermissionRows[i];
	}

	/**
	 * A direct and faithful port of the C++ GetCIDsForPermissions function,
	 * including the precise, deterministic collision-handling logic.
	 *
	 * @param caId
	 *            The Customer Account ID.
	 * @param ownerCid
	 *            The ID of the customer who owns the account.
	 * @param count
	 *            The number of additional permissioned Customer IDs to generate.
	 * @return An array of unique Customer IDs.
	 */

	public void getCidsForPermissions(long caId, long ownerCid, long[] cids)
	{
		long startFromCustomer = customerTable.getStartFromCustomer();
		final long iAccountPermissionIDRange = 4024L * 1024 * 1024 - 1;

		if (cids == null || cids.length != 2)
		{
			return;
		}

		long oldSeed = random.getSeed();
		random.setSeed(TpcRandom.rndNthElement(RNG_SEED_BASE_PERM_CID1, caId));

		// Select from a fixed range that doesn't depend on the number of customers in
		// the database.
		// This allows not to specify the total number of customers to EGenLoader, only
		// how many
		// a particular instance needs to generate (may be a fraction of total).
		// Note: this is not implemented right now.

		cids[0] = random.rndInt64RangeExclude(startFromCustomer, startFromCustomer + iAccountPermissionIDRange, ownerCid);

		// NOTE: Reseeding the RNG here for the second CID value. The use of this
		// sequence
		// is fuzzy because the number of RNG values consumed is dependant on not only
		// the
		// CA_ID, but also the CID value chosen above for the first permission. Using a
		// different sequence here may help prevent potential overlaps that might occur
		// if
		// the same sequence from above were used.
		random.setSeed(TpcRandom.rndNthElement(RNG_SEED_BASE_PERM_CID2, caId));

		do
		{ // make sure the second id is different from the first
			cids[1] = random.rndInt64RangeExclude(startFromCustomer, startFromCustomer + iAccountPermissionIDRange, ownerCid);
		}
		while (cids[1] == cids[0]);

		random.setSeed(oldSeed);
	}

	/**
	 * A direct port of the C++ GetNumPermsForCA function. Determines how many
	 * non-owner permissions an account should have.
	 *
	 * @param caId
	 *            The Customer Account ID.
	 * @return The number of extra permissions (0, 1, or 2).
	 */
	private int getNumPermsForCA(long caId)
	{
		long oldSeed = this.random.getSeed(); // Save state
		this.random.setSeed(TpcRandom.rndNthElement(RNG_SEED_BASE_NUM_PERMS, caId));
		int threshold = this.random.rndIntRange(1, 100);
		this.random.setSeed(oldSeed); // Restore state

		if (threshold <= PERCENT_ACCOUNT_ADDITIONAL_PERMISSIONS_0)
		{
			return 0; // 60% of accounts have just the owner row permissions
		}
		else
		{
			if (threshold <= PERCENT_ACCOUNT_ADDITIONAL_PERMISSIONS_0 +
					PERCENT_ACCOUNT_ADDITIONAL_PERMISSIONS_1)
			{
				return 1; // 38% of accounts have one additional permisison row
			}
			else
			{
				return 2; // 2% of accounts have two additional permission rows
			}
		}
	}

	public AccountId generateRandomAccountId(TpcRandom rnd, long customerId, CustomerTier customerTier)
	{
		int accCount = getNumberOfAccounts(customerId, customerTier);
		long startingAccount = getStartingAccountId(customerId);

		// Select random account for the customer
		long caId = rnd
				.rndInt64Range(
						startingAccount,
						startingAccount + accCount - 1);

		return new AccountId(caId, accCount);
	}
}
