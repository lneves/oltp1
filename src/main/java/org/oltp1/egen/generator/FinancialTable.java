package org.oltp1.egen.generator;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.model.FinancialRow;
import org.oltp1.egen.util.DateTime;
import org.oltp1.egen.util.Money;
import org.oltp1.egen.util.TpcRandom;

/**
 * This class generates financial data for a set of companies over several
 * years, producing one row of data per quarter.
 */
public class FinancialTable implements TableGenerator<FinancialRow>
{

	public static final int YEARS_FOR_FINS = 5;
	public static final int QUARTERS_IN_YEAR = 4;
	public static final int FINS_PER_COMPANY = YEARS_FOR_FINS * QUARTERS_IN_YEAR;

	public static final double DILUTED_SHARES_MULTIPLIER = 1.1;

	public static final double FIN_DATA_DOWN_MULT = 0.9;
	public static final double FIN_DATA_UP_MULT = 1.15;
	public static final double FIN_DATA_INCR = 1e-14;

	public static final double FINANCIAL_REVENUE_MIN = 100000.00;
	public static final double FINANCIAL_REVENUE_MAX = 16000000000.00;
	public static final double FINANCIAL_EARNINGS_MIN = -300000000.00;
	public static final double FINANCIAL_EARNINGS_MAX = 3000000000.00;

	public static final long FINANCIAL_OUT_BASIC_MIN = 400000L;
	public static final long FINANCIAL_OUT_BASIC_MAX = 9500000000L;

	public static final double FINANCIAL_INVENT_MIN = 0.00;
	public static final double FINANCIAL_INVENT_MAX = 2000000000.00;
	public static final double FINANCIAL_ASSETS_MIN = 100000.00;
	public static final double FINANCIAL_ASSETS_MAX = 65000000000.00;
	public static final double FINANCIAL_LIAB_MIN = 100000.00;
	public static final double FINANCIAL_LIAB_MAX = 35000000000.00;

	private static final long RNG_SEED_TABLE_DEFAULT = 37039940L;
	public static final int RNG_SKIP_ONE_ROW_FINANCIAL = 6 + FINS_PER_COMPANY * 6;

	// Base date values from EGenTables_common.h
	public static final int DAILY_MARKET_BASE_YEAR = 2000;
	public static final int DAILY_MARKET_BASE_MONTH = 1;
	public static final int DEFAULT_LOAD_UNIT_SIZE = 1000;

	/**
	 * Holds a block of 20 financial rows for a single company (5 years * 4
	 * quarters).
	 */
	static class FinancialGenRow
	{
		public final FinancialRow[] financials = new FinancialRow[FINS_PER_COMPANY];

		public FinancialGenRow()
		{
			for (int i = 0; i < FINS_PER_COMPANY; i++)
			{
				financials[i] = new FinancialRow();
			}
		}
	}

	private final CompanyTable companyTable;
	private int financialYear;
	private int financialQuarter; // 0-based
	private int rowsGeneratedPerCompany;
	private long lastRowNumber;
	private boolean moreCompanies;
	private final long financialCountForOneLoadUnit;
	private boolean hasMoreRecords;
	private final CompanyFile companyFile;
	private FinancialGenRow row;
	private TpcRandom random;

	public FinancialTable(DataFileManager dfm, long customerCount, long startFromCustomer)
	{
		super();
		this.row = new FinancialGenRow(); // Allocate the row structure
		this.companyTable = new CompanyTable(dfm, customerCount, startFromCustomer);
		this.rowsGeneratedPerCompany = FINS_PER_COMPANY; // Ensures first call generates new data
		this.moreCompanies = true;
		this.random = new TpcRandom(RNG_SEED_TABLE_DEFAULT);

		// Start year and quarter to generate financials.
		this.financialYear = DAILY_MARKET_BASE_YEAR;
		this.financialQuarter = (DAILY_MARKET_BASE_MONTH - 1) / 3; // Convert 1-based month to 0-based quarter

		this.companyFile = dfm.getCompanyFile();
		this.financialCountForOneLoadUnit = this.companyFile.calculateCompanyCount(DEFAULT_LOAD_UNIT_SIZE) * FINS_PER_COMPANY;
		this.lastRowNumber = this.companyFile.calculateStartFromCompany(startFromCustomer) * FINS_PER_COMPANY;

		this.hasMoreRecords = this.companyFile.calculateCompanyCount(customerCount) > 0;
	}

	@Override
	public boolean hasMoreRecords()
	{
		if (!moreCompanies && (rowsGeneratedPerCompany == FINS_PER_COMPANY - 1))
		{
			hasMoreRecords = false;
		}
		return hasMoreRecords;
	}

	@Override
	public FinancialRow generateNextRecord()
	{
		if (lastRowNumber % financialCountForOneLoadUnit == 0)
		{
			initNextLoadUnit();
		}
		lastRowNumber++;
		rowsGeneratedPerCompany++;

		if (rowsGeneratedPerCompany >= FINS_PER_COMPANY)
		{
			if (moreCompanies)
			{
				// All rows for the current company have been returned, so generate data for the
				// next one.
				moreCompanies = generateFinancialRows();
				rowsGeneratedPerCompany = 0;
			}
		}

		return row.financials[rowsGeneratedPerCompany];
	}

	private void initNextLoadUnit()
	{
		long rngSkipCount = lastRowNumber * RNG_SKIP_ONE_ROW_FINANCIAL;
		long seed = TpcRandom.rndNthElement(RNG_SEED_TABLE_DEFAULT, rngSkipCount);
		random.setSeed(seed);
	}

	/**
	 * Generates a block of 20 financial rows (5 years) for the next company.
	 *
	 * @return true if there are more companies to process; false otherwise.
	 */
	private boolean generateFinancialRows()
	{
		long companyId = companyTable.getCurrentCompanyId();
		int currentYear = this.financialYear;
		int currentQuarter = this.financialQuarter;

		// Set initial random values for the first quarter
		Money revenue = new Money(random.rndDoubleIncrRange(FINANCIAL_REVENUE_MIN, FINANCIAL_REVENUE_MAX, 0.01));
		double earningsMax = Math.min(revenue.dollarAmount(), FINANCIAL_EARNINGS_MAX);
		Money earnings = new Money(random.rndDoubleIncrRange(FINANCIAL_EARNINGS_MIN, earningsMax, 0.01));
		long basicShares = random.rndInt64Range(FINANCIAL_OUT_BASIC_MIN, FINANCIAL_OUT_BASIC_MAX);
		Money inventory = new Money(random.rndDoubleIncrRange(FINANCIAL_INVENT_MIN, FINANCIAL_INVENT_MAX, 0.01));
		Money assets = new Money(random.rndDoubleIncrRange(FINANCIAL_ASSETS_MIN, FINANCIAL_ASSETS_MAX, 0.01));
		Money liability = new Money(random.rndDoubleIncrRange(FINANCIAL_LIAB_MIN, FINANCIAL_LIAB_MAX, 0.01));

		for (int i = 0; i < FINS_PER_COMPANY; ++i)
		{
			revenue = revenue.multiply(random.rndDoubleIncrRange(FIN_DATA_DOWN_MULT, FIN_DATA_UP_MULT, FIN_DATA_INCR));
			earnings = earnings.multiply(random.rndDoubleIncrRange(FIN_DATA_DOWN_MULT, FIN_DATA_UP_MULT, FIN_DATA_INCR));
			if (earnings.greaterThanOrEqual(revenue))
			{
				earnings = earnings.multiply(FIN_DATA_DOWN_MULT);
			}
			basicShares = (long) (basicShares * random.rndDoubleIncrRange(FIN_DATA_DOWN_MULT, FIN_DATA_UP_MULT, FIN_DATA_INCR));
			inventory = inventory.multiply(random.rndDoubleIncrRange(FIN_DATA_DOWN_MULT, FIN_DATA_UP_MULT, FIN_DATA_INCR));
			assets = assets.multiply(random.rndDoubleIncrRange(FIN_DATA_DOWN_MULT, FIN_DATA_UP_MULT, FIN_DATA_INCR));
			liability = liability.multiply(random.rndDoubleIncrRange(FIN_DATA_DOWN_MULT, FIN_DATA_UP_MULT, FIN_DATA_INCR));

			// Calculate derived values
			long dilutedShares = (long) (basicShares * DILUTED_SHARES_MULTIPLIER);

			Money basicEps = (basicShares > 0) ? earnings.divide(basicShares) : new Money();
			Money dilutedEps = (dilutedShares > 0) ? earnings.divide(dilutedShares) : new Money();
			Money margin = (!revenue.equals(new Money(0))) ? earnings.divide(revenue.dollarAmount()) : new Money();

			// Assign values to the current row in the block
			FinancialRow currentRow = row.financials[i];

			currentRow.FI_CO_ID = companyId;
			currentRow.FI_YEAR = currentYear;
			currentRow.FI_QTR = currentQuarter + 1; // Convert 0-based to 1-based
			currentRow.FI_QTR_START_DATE = new DateTime(currentYear, currentQuarter * 3 + 1, 1);
			currentRow.FI_REVENUE = revenue.dollarAmount();
			currentRow.FI_NET_EARN = earnings.dollarAmount();
			currentRow.FI_OUT_BASIC = basicShares;
			currentRow.FI_OUT_DILUT = dilutedShares;
			currentRow.FI_BASIC_EPS = basicEps.dollarAmount();
			currentRow.FI_DILUT_EPS = dilutedEps.dollarAmount();
			currentRow.FI_MARGIN = margin.dollarAmount();
			currentRow.FI_INVENTORY = inventory.dollarAmount();
			currentRow.FI_ASSETS = assets.dollarAmount();
			currentRow.FI_LIABILITY = liability.dollarAmount();

			// Increment quarter and year for the next loop iteration
			currentQuarter++;
			if (currentQuarter == QUARTERS_IN_YEAR)
			{
				currentQuarter = 0;
				currentYear++;
			}
		}

		// Move to the next company for the next block generation
		return companyTable.generateNextCoId();
	}
}