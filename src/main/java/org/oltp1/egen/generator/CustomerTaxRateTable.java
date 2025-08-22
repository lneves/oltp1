package org.oltp1.egen.generator;

import org.oltp1.egen.io.BucketedDataFile;
import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.io.records.TaxRateCountryDataFileRecord;
import org.oltp1.egen.io.records.TaxRateDivisionDataFileRecord;
import org.oltp1.egen.io.records.TaxRateFileRecord;
import org.oltp1.egen.model.CustomerTaxRateRow;
import org.oltp1.egen.util.TpcRandom;

public class CustomerTaxRateTable
{
	// Constants
	private static final int TAX_RATES_PER_CUSTOMER = 2;
	private static final long RNG_SEED_BASE_TAX_RATE_ROW = 92740731L;
	private static final long RNG_SEED_TABLE_DEFAULT = 37039940L;
	private static final int TAX_RATES_PER_CUST = 2; // number of tax rates per customer
	private static final int MAX_DIV_OR_CTRY_NAME = 6;
	private static final long DEFAULT_LOAD_UNIT_SIZE = 1000;

	// Number of RNG calls to skip for one row in order
	// to not use any of the random values from the previous row.
	private static final int RNG_SKIP_ONE_ROW_CUSTOMER_TAXRATE = 5; // real max count in v3.5: 2

	// Member variables
	private CustomerTable cust;
	private AddressTable addr;
	private final TpcRandom random;
	private final BucketedDataFile<TaxRateDivisionDataFileRecord> divisionRates;
	private final BucketedDataFile<TaxRateCountryDataFileRecord> countryRates;

	private boolean hasMoreRecords;
	private CustomerTaxRateRow[] row = new CustomerTaxRateRow[TAX_RATES_PER_CUSTOMER];

	public CustomerTaxRateTable(DataFileManager dfm,
			long customerCount,
			long startFromCustomer)
	{
		super();
		this.random = new TpcRandom(0);
		this.cust = new CustomerTable(dfm, customerCount, startFromCustomer);
		this.addr = new AddressTable(dfm, customerCount, startFromCustomer, true);
		this.divisionRates = dfm.getTaxRateDivisionDataFile();
		this.countryRates = dfm.getTaxRateCountryDataFile();
		this.hasMoreRecords = true;
	}

	public boolean hasMoreRecords()
	{
		hasMoreRecords = cust.hasMoreRecords();
		return hasMoreRecords;
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
										cust.getCurrentC_ID() * RNG_SKIP_ONE_ROW_CUSTOMER_TAXRATE));

	}

	/**
	 * Generate the tax row deterministically for a given customer and country or
	 * division code
	 * 
	 * @param cId
	 *            customer ID
	 * @param code
	 *            country or division code
	 * @param isCountry
	 *            true if it's a country code, false if division code
	 * @return tax rate file record
	 */
	private TaxRateFileRecord getTaxRow(long cId, int code, boolean isCountry)
	{
		long oldSeed = random.getSeed();
		random.setSeed(TpcRandom.rndNthElement(RNG_SEED_BASE_TAX_RATE_ROW, cId));

		int threshold;
		TaxRateFileRecord result;

		if (isCountry)
		{
			// Return appropriate country record.
			threshold = random.rndIntRange(0, countryRates.getBucket(code).size() - 1);
			result = countryRates.getBucket(code).get(threshold);
		}
		else
		{
			// It's not a country so return the appropriate division record.
			threshold = random.rndIntRange(0, divisionRates.getBucket(code).size() - 1);
			result = divisionRates.getBucket(code).get(threshold);
		}

		random.setSeed(oldSeed);
		return result;
	}

	public CustomerTaxRateRow[] generateNextRecord()
	{
		if (cust.getCurrentC_ID() % DEFAULT_LOAD_UNIT_SIZE == 0)
		{
			initNextLoadUnit();
		}

		cust.generateNextC_ID(); // next customer id
		addr.generateNextAdId(); // next address id (to get the one for this customer)

		int[] divAndCountryCodes = addr.getDivisionAndCountryCodesForAddress(addr.getRow().AD_ID);
		int divCode = divAndCountryCodes[0];
		int ctryCode = divAndCountryCodes[1];

		row[0] = new CustomerTaxRateRow();

		// Fill the country tax rate row
		row[0].CX_C_ID = cust.getCurrentC_ID(); // fill the customer id
		// Select the country rate
		String countryTaxId = getCountryTaxRow(cust.getCurrentC_ID(), ctryCode).tx_id;
		row[0].CX_TX_ID = countryTaxId;

		row[1] = new CustomerTaxRateRow();
		// Fill the division tax rate row
		row[1].CX_C_ID = cust.getCurrentC_ID(); // fill the customer id
		// Select the division rate
		String divisionTaxId = getDivisionTaxRow(cust.getCurrentC_ID(), divCode).tx_id;
		row[1].CX_TX_ID = divisionTaxId;

		return row;
	}

	/**
	 * Get row by index
	 * 
	 * @param i
	 *            index of the row (0 or 1)
	 * @return customer tax rate row at the specified index
	 * @throws IndexOutOfBoundsException
	 *             if index is out of bounds
	 */
	public CustomerTaxRateRow getRowByIndex(int i)
	{
		if (i < TAX_RATES_PER_CUST)
		{
			return row[i];
		}
		else
		{
			throw new IndexOutOfBoundsException("Customer Taxrate row index out of bounds.");
		}
	}

	/**
	 * Get the number of tax rates per customer
	 * 
	 * @return number of tax rates per customer
	 */
	public int getTaxRatesCount()
	{
		return TAX_RATES_PER_CUST;
	}

	/**
	 * Generate country tax row for a given customer
	 * 
	 * @param cId
	 *            customer ID
	 * @param ctryCode
	 *            country code
	 * @return tax rate file record for the country
	 */
	public TaxRateFileRecord getCountryTaxRow(long cId, int ctryCode)
	{
		return getTaxRow(cId, ctryCode, true);
	}

	/**
	 * Generate division tax row for a given customer
	 * 
	 * @param cId
	 *            customer ID
	 * @param divCode
	 *            division code
	 * @return tax rate file record for the division
	 */
	public TaxRateDivisionDataFileRecord getDivisionTaxRow(long cId, int divCode)
	{
		return (TaxRateDivisionDataFileRecord) getTaxRow(cId, divCode, false);
	}

	// Getters for member variables if needed
	public CustomerTable getCustomerTable()
	{
		return cust;
	}

	public AddressTable getAddressTable()
	{
		return addr;
	}

	public BucketedDataFile<TaxRateDivisionDataFileRecord> getDivisionRates()
	{
		return divisionRates;
	}

	public BucketedDataFile<TaxRateCountryDataFileRecord> getCountryRates()
	{
		return countryRates;
	}

}
