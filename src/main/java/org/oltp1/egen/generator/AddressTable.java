package org.oltp1.egen.generator;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.io.WeightedDataFile;
import org.oltp1.egen.io.records.StreetNameDataFileRecord;
import org.oltp1.egen.io.records.StreetSuffixDataFileRecord;
import org.oltp1.egen.io.records.ZipCodeDataFileRecord;
import org.oltp1.egen.model.AddressRow;
import org.oltp1.egen.util.TpcRandom;

/**
 * Generates data for the ADDRESS table. This class is a direct port of the C++
 * CAddressTable, including its stateful random number consumption, which is
 * critical for maintaining a consistent RNG sequence across the entire data
 * generation process. Based on inc/AddressTable.h and src/AddressTable.cpp
 */
public class AddressTable implements TableGenerator<AddressRow>
{
	// Constants
	private static final int DEFAULT_LOAD_UNIT_SIZE = 1000;
	private static final long IDENT_T_SHIFT = 4300000000L;
	private static final int RNG_SKIP_ONE_ROW_ADDRESS = 10; // real number in 3.5: 7
	private static final long RNG_SEED_TABLE_DEFAULT = 37039940L;
	private static final long RNG_SEED_BASE_TOWN_DIV_ZIP = 26778071L;
	private static final int USA_CTRY_CODE = 1; // must be the same as the code in country tax rates file
	private static final int CANADA_CTRY_CODE = 2; // must be the same as the code in country tax rates file

	// Minimum and maximum to use when generating address street numbers.
	private static final int STREET_NUMBER_MIN = 100;
	private static final int STREET_NUMBER_MAX = 25000;

	// Some customers have an AD_LINE_2, some are NULL.
	private static final int PCT_CUSTOMERS_WITH_NULL_AD_LINE_2 = 60;

	// Of the customers that have an AD_LINE_2, some are
	// an apartment, others are a suite.
	private static final int PCT_CUSTOMERS_WITH_APT_AD_LINE_2 = 75;

	// Minimum and maximum to use when generating apartment numbers.
	private static final int APARTMENT_NUMBER_MIN = 1;
	private static final int APARTMENT_NUMBER_MAX = 1000;

	// Minimum and maximum to use when generating suite numbers.
	private static final int SUITE_NUMBER_MIN = 1;
	private static final int SUITE_NUMBER_MAX = 500;

	// Member variables
	private final TpcRandom random;
	private long lastRowNumber;
	private boolean hasMoreRecords;
	private final CompanyFile companies;
	private final WeightedDataFile<StreetNameDataFileRecord> street;
	private final WeightedDataFile<StreetSuffixDataFileRecord> streetSuffix;
	private final WeightedDataFile<ZipCodeDataFileRecord> zipCode;
	private final long startFromCustomer;
	private final long customerCount;
	private final boolean customerAddressesOnly;

	private long exchangeCount;
	private long companyCount;
	private long totalAddressCount;
	private boolean customerAddress;
	private AddressRow row;

	/**
	 * Constructor for the ADDRESS table class.
	 *
	 * @param dfm
	 *            input flat files loaded in memory
	 * @param customerCount
	 *            number of customers to generate
	 * @param startFromCustomer
	 *            ordinal position of the first customer in the sequence (Note:
	 *            1-based) for whom to generate the addresses. Used if generating
	 *            customer addresses only.
	 * @param customerAddressesOnly
	 *            if true, generate only customer addresses if false, generate
	 *            exchange, company, and customer addresses (always start from the
	 *            first customer in this case)
	 * @param cacheEnabled
	 *            whether caching is enabled
	 */
	public AddressTable(DataFileManager dfm,
			long customerCount,
			long startFromCustomer,
			boolean customerAddressesOnly)
	{
		super();
		this.random = new TpcRandom(RNG_SEED_TABLE_DEFAULT); // Internal RNG for its own generation;
		this.companies = dfm.getCompanyFile();
		this.street = dfm.getStreetNameDataFile();
		this.streetSuffix = dfm.getStreetSuffixDataFile();
		this.zipCode = dfm.getZipCodeDataFile();
		this.startFromCustomer = startFromCustomer;
		this.customerCount = customerCount;
		this.customerAddressesOnly = customerAddressesOnly;
		this.customerAddress = customerAddressesOnly;

		this.exchangeCount = dfm.getExchangeDataFile().size(); // number of rows in Exchange
		this.companyCount = companies.getConfiguredCompanyCount(); // number of configured companies

		// Generate customer addresses only (used for CUSTOMER_TAXRATE)
		if (customerAddressesOnly)
		{
			// skip exchanges and companies
			this.lastRowNumber = exchangeCount + companyCount + startFromCustomer - 1;

			// This is not really a count, but the last address row to generate.
			this.totalAddressCount = lastRowNumber + customerCount;
		}
		else
		{
			// Generating not only customer, but also exchange and company addresses
			this.lastRowNumber = startFromCustomer - 1;

			// This is not really a count, but the last address row to generate.
			this.totalAddressCount = lastRowNumber + customerCount + exchangeCount + companyCount;
		}

		this.row = new AddressRow();
		this.row.AD_ID = lastRowNumber + IDENT_T_SHIFT; // extend to 64 bits for address id
	}

	/**
	 * Reset the state for the next load unit.
	 */
	protected void initNextLoadUnit()
	{
		random
				.setSeed(
						TpcRandom
								.rndNthElement(
										RNG_SEED_TABLE_DEFAULT,
										lastRowNumber * RNG_SKIP_ONE_ROW_ADDRESS));

	}

	/**
	 * Generates the next A_ID value. It is stored in the internal record structure
	 * and also returned. The number of rows generated is incremented. This is why
	 * this function cannot be called more than once for a record.
	 *
	 * @return next address id.
	 */
	public long generateNextAdId()
	{
		// Reset RNG at Load Unit boundary, so that all data is repeatable.
		if (lastRowNumber > (exchangeCount + companyCount)
				&& ((lastRowNumber - (exchangeCount + companyCount)) % DEFAULT_LOAD_UNIT_SIZE == 0))
		{
			initNextLoadUnit();
		}

		++lastRowNumber;
		// Find out whether this next row is for a customer (so as to generate
		// AD_LINE_2).
		// Exchange and Company addresses are before Customer ones.
		customerAddress = lastRowNumber >= exchangeCount + companyCount;

		// update state info
		// hasMoreRecords = lastRowNumber < totalAddressCount;

		row.AD_ID = lastRowNumber + IDENT_T_SHIFT;

		return row.AD_ID;
	}

	/**
	 * Returns the address id of the customer specified by the customer id.
	 *
	 * @param cId
	 *            customer id (1-based)
	 * @return address id.
	 */
	public long getAdIdForCustomer(long cId)
	{
		return exchangeCount + companyCount + cId;
	}

	/**
	 * Generate AD_LINE_1 and store it in the record structure. Does not increment
	 * the number of rows generated.
	 */
	private void generateAdLine1()
	{
		int streetNum = random.rndIntRange(STREET_NUMBER_MIN, STREET_NUMBER_MAX);
		int streetThreshold = random.rndIntRange(0, street.size() - 2);
		int streetSuffixThreshold = random.rndIntRange(0, streetSuffix.size() - 1);

		row.AD_LINE1 = String
				.format(
						"%d %s %s",
						streetNum,
						street.getRecord(streetThreshold).street,
						streetSuffix.getRecord(streetSuffixThreshold).suffix);
	}

	/**
	 * Generate AD_LINE_2 and store it in the record structure. Does not increment
	 * the number of rows generated.
	 */
	private void generateAdLine2()
	{
		if (!customerAddress || random.rndPercent(PCT_CUSTOMERS_WITH_NULL_AD_LINE_2))
		{

			row.AD_LINE2 = ""; // null terminator
		}
		else
		{
			String adLine2;
			if (random.rndPercent(PCT_CUSTOMERS_WITH_APT_AD_LINE_2))
			{
				adLine2 = String
						.format(
								"Apt. %d",
								random.rndIntRange(APARTMENT_NUMBER_MIN, APARTMENT_NUMBER_MAX));
			}
			else
			{
				adLine2 = String
						.format(
								"Suite %d",
								random.rndIntRange(SUITE_NUMBER_MIN, SUITE_NUMBER_MAX));
			}

			row.AD_LINE2 = adLine2;
		}
	}

	/**
	 * For a given address id returns the same Threshold used to select the town,
	 * division, zip, and country. Needed to return a specific division/country for
	 * a given address id (for customer tax rates).
	 *
	 * @param adId
	 *            address id
	 * @return threshold value
	 */
	private int getTownDivisionZipCodeThreshold(long adId)
	{
		long oldSeed = random.getSeed();
		random.setSeed(TpcRandom.rndNthElement(RNG_SEED_BASE_TOWN_DIV_ZIP, adId));
		int threshold = random.rndIntRange(0, zipCode.size() - 1);
		random.setSeed(oldSeed);
		return threshold;
	}

	/**
	 * Return the country code for a given zip code.
	 *
	 * @param zipCodeStr
	 *            string with a US or Canada zip code
	 * @return country code.
	 */
	private int getCountryCode(String zipCodeStr)
	{
		if (zipCodeStr != null && zipCodeStr.length() > 0)
		{
			char firstChar = zipCodeStr.charAt(0);
			if ('0' <= firstChar && firstChar <= '9')
			{
				// If the zip code starts with a number, then it's a USA code.
				return USA_CTRY_CODE;
			}
		}
		// If the zip code does NOT start with a number, then it's a Canadian code.
		return CANADA_CTRY_CODE;
	}

	/**
	 * Return a certain division/country code (from the input file) for a given
	 * address id. Used in the loader to properly calculate tax on a trade.
	 *
	 * @param adId
	 *            address id
	 * @return array containing [divCode, ctryCode]
	 */
	public int[] getDivisionAndCountryCodesForAddress(long adId)
	{
		int threshold = getTownDivisionZipCodeThreshold(adId);

		// Get the data.
		int divCode = zipCode.getRecord(threshold).divisionTaxKey;
		int ctryCode = getCountryCode(zipCode.getRecord(threshold).zc_code);

		return new int[] { divCode, ctryCode };
	}

	/**
	 * Generate zip code and country for the current address id and store them in
	 * the record structure. Does not increment the number of rows generated.
	 */
	private void generateAdZcCodeCtry()
	{
		int threshold = getTownDivisionZipCodeThreshold(row.AD_ID);
		ZipCodeDataFileRecord dfr = zipCode.getRecord(threshold);

		String zcCode = dfr.zc_code;
		row.AD_ZC_CODE = zcCode;

		if (USA_CTRY_CODE == getCountryCode(zcCode))
		{
			// US state
			row.AD_CTRY = "USA";
		}
		else
		{
			row.AD_CTRY = "CANADA";
		}

	}

	@Override
	public AddressRow generateNextRecord()
	{
		row = new AddressRow();
		generateNextAdId();
		generateAdLine1();
		generateAdLine2();
		generateAdZcCodeCtry();

		return row;
	}

	@Override
	public boolean hasMoreRecords()
	{
		hasMoreRecords = lastRowNumber < totalAddressCount;
		return hasMoreRecords;
	}

	// Getters for member variables if needed
	public long getStartFromCustomer()
	{
		return startFromCustomer;
	}

	public long getCustomerCount()
	{
		return customerCount;
	}

	public boolean isCustomerAddressesOnly()
	{
		return customerAddressesOnly;
	}

	public long getExchangeCount()
	{
		return exchangeCount;
	}

	public long getCompanyCount()
	{
		return companyCount;
	}

	public long getTotalAddressCount()
	{
		return totalAddressCount;
	}

	public boolean isCustomerAddress()
	{
		return customerAddress;
	}

	public AddressRow getRow()
	{
		return row;
	}
}