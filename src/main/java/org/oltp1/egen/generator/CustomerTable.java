package org.oltp1.egen.generator;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.model.CustomerRow;
import org.oltp1.egen.util.DateTime;
import org.oltp1.egen.util.TpcRandom;

/**
 * Generates data for the CUSTOMER table. This class is a direct port of the
 * logic from C++ EGen, responsible for creating each field of a customer row
 * according to TPC-E specification rules. It manages its own state to generate
 * a sequence of customers. Based on inc/CustomerTable.h and
 * src/CustomerTable.cpp
 */
public class CustomerTable implements TableGenerator<CustomerRow>
{
	private static final int RNG_SKIP_ONE_ROW_CUSTOMER = 35;
	private static final long RNG_SEED_TABLE_DEFAULT = 37039940L;
	private static final long IDENT_T_SHIFT = 4300000000L;
	private static final int LOAD_UNIT_SIZE = 1000;

	private static final long RNG_SEED_BASE_C_AREA_1 = 97905013L;
	private static final long RNG_SEED_BASE_C_AREA_2 = 68856487L;
	private static final long RNG_SEED_BASE_C_AREA_3 = 67142295L;

	private final DataFileManager dfm;
	private final long customerCount;
	private final long startFromCustomer;
	private long lastRowNumber;
	private boolean hasMoreRecords;

	private final TpcRandom random;
	private final Person person;
	private final CustomerSelection customerSelection;

	// For calculating AD_ID
	private final long exchangeCount;
	private final long companyCount;

	public CustomerTable(DataFileManager dfm, long customerCount, long startFromCustomer)
	{
		this.dfm = dfm;
		this.customerCount = customerCount;
		this.startFromCustomer = startFromCustomer;
		this.lastRowNumber = 0;
		this.hasMoreRecords = customerCount > 0;

		this.random = new TpcRandom(0);
		this.person = new Person(dfm, startFromCustomer, true);
		this.customerSelection = new CustomerSelection();

		// Get constants needed for AD_ID calculation
		this.exchangeCount = dfm.getExchangeDataFile().size();

		// ( customerCount / LOAD_UNIT_SIZE ) * companiesPerLoadUnit
		this.companyCount = dfm.getCompanyFile().getConfiguredCompanyCount();

		initNextLoadUnit();
	}

	@Override
	public boolean hasMoreRecords()
	{
		if (lastRowNumber >= customerCount)
		{
			hasMoreRecords = false;
		}
		return hasMoreRecords;
	}

	public CustomerSelection getCustomerSelection()
	{
		return this.customerSelection;
	}

	@Override
	public CustomerRow generateNextRecord()
	{
		if (lastRowNumber > 0 && lastRowNumber % LOAD_UNIT_SIZE == 0)
		{
			initNextLoadUnit();
		}

		lastRowNumber++;
		long currentCustomerId = startFromCustomer + lastRowNumber - 1;
		CustomerRow row = new CustomerRow();
		row.c_id = currentCustomerId + IDENT_T_SHIFT;

		// CRITICAL: The order of generation now precisely matches the C++
		// EGenLoader.cpp logic.
		row.c_st_id = dfm.getStatusTypeDataFile().get(1).st_id; // 1 = Active
		person.populate(row);
		row.c_tier = (char) (customerSelection.getTier(row.c_id).getValue() + '0');

		row.c_dob = generateDob();

		// C_AD_ID is a direct calculation, it does not consume a random number.
		row.c_ad_id = exchangeCount + companyCount + currentCustomerId + IDENT_T_SHIFT;

		generatePhoneInfo(row, 1);
		generatePhoneInfo(row, 2);
		generatePhoneInfo(row, 3);
		generateEmail(row);

		return row;
	}

	private void initNextLoadUnit()
	{
		long globalCustomerIndex = startFromCustomer + lastRowNumber - 1;
		long rngSkipCount = globalCustomerIndex * RNG_SKIP_ONE_ROW_CUSTOMER;
		long seedValue = TpcRandom.rndNthElement(RNG_SEED_TABLE_DEFAULT, rngSkipCount);

		random.setSeed(seedValue);
		person.initNextLoadUnit();
	}

	// Inside the CustomerTable.java class

	private DateTime generateDob()
	{
		// 1. FIRST RNG CALL: Determine the age bracket
		int threshold = random.rndIntRange(1, 100);

		int[] ageBrackets = { 10, 19, 25, 35, 45, 55, 65, 75, 85, 100 };
		int[] percentages = { 5, 16, 17, 19, 16, 11, 8, 7, 1 };
		int cumulativePercent = 0;
		int ageBracketIndex = 0;

		for (int i = 0; i < percentages.length; i++)
		{
			cumulativePercent += percentages[i];
			if (threshold <= cumulativePercent)
			{
				ageBracketIndex = i;
				break;
			}
		}

		int baseYear = 2005; // InitialTradePopulationBaseYear
		int baseMonth = 1; // InitialTradePopulationBaseMonth
		int baseDay = 3; // InitialTradePopulationBaseDay
		int minAge = ageBrackets[ageBracketIndex];
		int maxAge = ageBrackets[ageBracketIndex + 1];

		// Calculate the valid range of birth dates as absolute day numbers
		int dobDayNumMin = DateTime.ymdToDayNumber(baseYear - maxAge, baseMonth, baseDay) + 1;
		int dobDayNumMax = DateTime.ymdToDayNumber(baseYear - minAge, baseMonth, baseDay);

		// Pick a single random day number from the valid range
		int dobInDays = random.rndIntRange(dobDayNumMin, dobDayNumMax);

		// Create the DateTime object from the single, calculated day number
		return new DateTime(dobInDays);
	}

	private void generatePhoneInfo(CustomerRow row, int phoneIndex)
	{
		String country = "011";
		String area, local, ext;

		// Generate each part using its own deterministic, stateless method
		switch (phoneIndex)
		{
		case 1:
			area = generateAreaCode(row.c_id, RNG_SEED_BASE_C_AREA_1);
			local = generateLocal(); // Uses the main stateful RNG
			ext = generateExtension(25); // Uses the main stateful RNG
			row.c_ctry_1 = country;
			row.c_area_1 = area;
			row.c_local_1 = local;
			row.c_ext_1 = ext;
			break;
		case 2:
			area = generateAreaCode(row.c_id, RNG_SEED_BASE_C_AREA_2);
			local = generateLocal();
			ext = generateExtension(15);
			row.c_ctry_2 = country;
			row.c_area_2 = area;
			row.c_local_2 = local;
			row.c_ext_2 = ext;
			break;
		case 3:
			area = generateAreaCode(row.c_id, RNG_SEED_BASE_C_AREA_3);
			local = generateLocal();
			ext = generateExtension(5);
			row.c_ctry_3 = country;
			row.c_area_3 = area;
			row.c_local_3 = local;
			row.c_ext_3 = ext;
			break;
		}
	}

	private String generateAreaCode(long customerId, long baseSeed)
	{
		long oldSeed = random.getSeed(); // Save state
		random.setSeed(TpcRandom.rndNthElement(baseSeed, customerId));
		String area = dfm.getAreaCodeDataFile().getRecord(random.rndIntRange(0, dfm.getAreaCodeDataFile().size() - 1)).areaCode;
		random.setSeed(oldSeed); // Restore state
		return area;
	}

	private String generateLocal()
	{
		// This method correctly uses the main stateful RNG
		StringBuilder localBuilder = new StringBuilder(7);
		for (int i = 0; i < 7; i++)
		{
			localBuilder.append(random.rndIntRange(0, 9));
		}
		return localBuilder.toString();
	}

	private String generateExtension(int probability)
	{
		// This method uses the main stateful RNG
		if (random.rndIntRange(1, 100) <= probability)
		{
			StringBuilder extBuilder = new StringBuilder(3);
			for (int i = 0; i < 3; i++)
			{
				extBuilder.append(random.rndIntRange(0, 9));
			}
			return extBuilder.toString();
		}
		return "";
	}

	private void generateEmail(CustomerRow row)
	{
		String[] domains = { "@msn.com", "@hotmail.com", "@rr.com", "@netzero.com", "@earthlink.com", "@attbi.com" };
		int domain1Index = random.rndIntRange(0, domains.length - 1);
		int domain2Index = random.rndIntRangeExclude(0, domains.length - 1, domain1Index);
		String fName = row.c_f_name != null ? row.c_f_name : "f";
		String lName = row.c_l_name != null ? row.c_l_name : "l";
		String baseEmail = String.format("%s%s", fName.substring(0, 1).toUpperCase(), lName.substring(0, 1).toUpperCase() + lName.substring(1));
		row.c_email_1 = baseEmail + domains[domain1Index];
		row.c_email_2 = baseEmail + domains[domain2Index];
	}

	public long getCurrentC_ID()
	{
		// The formula is: (start) + (how many have been generated) - 1 + (the 64-bit
		// shift)
		// This correctly calculates the ID for the row that was just generated or is
		// about to be.
		return startFromCustomer + lastRowNumber - 1 + IDENT_T_SHIFT;
	}

	public long generateNextC_ID()
	{
		if (lastRowNumber % LOAD_UNIT_SIZE == 0)
		{
			initNextLoadUnit();
		}

		++lastRowNumber; // increment state info
		hasMoreRecords = lastRowNumber < customerCount;

		return lastRowNumber + startFromCustomer - 1 + IDENT_T_SHIFT;
	}

	public long getStartFromCustomer()
	{
		return startFromCustomer;
	}

	public long getLastRowNumber()
	{
		return lastRowNumber;
	}
}