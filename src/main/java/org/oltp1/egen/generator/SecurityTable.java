package org.oltp1.egen.generator;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.model.SecurityRow;
import org.oltp1.egen.util.DateTime;
import org.oltp1.egen.util.TpcRandom;

public class SecurityTable implements TableGenerator<SecurityRow>
{
	// Constants
	private static final int DEFAULT_LOAD_UNIT_SIZE = 1000;
	private static final long RNG_SEED_TABLE_DEFAULT = 37039940L;
	private static final long S_NUM_OUT_MIN = 4000000L;
	private static final long S_NUM_OUT_MAX = 9500000000L;

	private static final double MIN_SEC_PRICE = 20.00;
	private static final double MAX_SEC_PRICE = 30.00;

	private static final double S_PE_MIN = 1.0;
	private static final double S_PE_MAX = 120.0;

	private static final double S_YIELD_NON_ZERO_MIN = 0.01;
	private static final double S_YIELD_MAX = 120.0;

	private static final int WEEKS_PER_YEAR = 52;
	private static final int DAYS_PER_WEEK = 7;

	private static final int PERCENT_COMPANIES_WITH_NON_ZERO_DIVIDEND = 60;

	private static final int RNG_SKIP_ONE_ROW_SECURITY = 11; // number of RNG calls for one row

	// Member variables
	private long securityCount;
	private long startFromSecurity;
	private final CompanyFile companyFile;
	private final SecurityFile securityFile;
	private int currentDayNo;
	private int jan1_1900DayNo;
	private int jan2_2000DayNo;
	private long securityCountForOneLoadUnit;

	private final TpcRandom random;

	private long lastRowNumber; // This is now the GLOBAL index, not a local counter.
	private boolean hasMoreRecords;
	private SecurityRow currentSecurityRow;

	public SecurityTable(DataFileManager dfm, long customerCount, long startFromCustomer)
	{
		super();

		this.random = new TpcRandom(RNG_SEED_TABLE_DEFAULT);

		this.companyFile = dfm.getCompanyFile();
		this.securityFile = dfm.getSecurityFile();

		this.currentDayNo = DateTime.ymdToDayNumber(2005, 1, 3);
		this.jan1_1900DayNo = DateTime.ymdToDayNumber(1900, 1, 1);
		this.jan2_2000DayNo = DateTime.ymdToDayNumber(2000, 1, 2);

		this.securityCount = securityFile.calculateSecurityCount(customerCount);
		this.startFromSecurity = securityFile.calculateStartFromSecurity(startFromCustomer);

		this.securityCountForOneLoadUnit = securityFile.calculateSecurityCount(DEFAULT_LOAD_UNIT_SIZE);

		this.lastRowNumber = startFromSecurity;
		this.hasMoreRecords = lastRowNumber < (startFromSecurity + securityCount);
	}

	/**
	 * SECURITY table row generation
	 */
	private SecurityRow generateSecurityRow()
	{
		int startDayNo, exchangeDayNo, i52HighDayNo, i52LowDayNo;

		SecurityRow row = new SecurityRow();

		row.S_SYMB = securityFile.createSymbol(lastRowNumber);
		row.S_CO_ID = securityFile.getCompanyId(lastRowNumber);
		row.S_EX_ID = securityFile.getRecord(lastRowNumber).s_ex_id;
		row.S_ISSUE = securityFile.getRecord(lastRowNumber).s_issue;
		row.S_ST_ID = securityFile.getRecord(lastRowNumber).s_st_id;

		String companyName = companyFile.createName(securityFile.getCompanyIndex(lastRowNumber));
		row.S_NAME = String.format("%s of %s", row.S_ISSUE, companyName);

		row.S_NUM_OUT = random.rndInt64Range(S_NUM_OUT_MIN, S_NUM_OUT_MAX);

		startDayNo = random.rndIntRange(jan1_1900DayNo, jan2_2000DayNo); // generate random date
		row.S_START_DATE = new DateTime(startDayNo);
		exchangeDayNo = random.rndIntRange(startDayNo, jan2_2000DayNo);
		row.S_EXCH_DATE = new DateTime(exchangeDayNo);

		row.S_PE = random.rndDoubleIncrRange(S_PE_MIN, S_PE_MAX, 0.01);
		// exchangeDayNo contains S_EXCH_DATE date in days.

		// 52 week high - selected from upper half of security price range
		row.S_52WK_HIGH = (float) random
				.rndDoubleIncrRange(
						MIN_SEC_PRICE + ((MAX_SEC_PRICE - MIN_SEC_PRICE) / 2),
						MAX_SEC_PRICE,
						0.01);

		// row.S_52WK_HIGH = (float) random.rndDoubleIncrRange(20.0 + ((30.0 - 20.0) /
		// 2.0), 30.0, 0.01);

		i52HighDayNo = random.rndIntRange(currentDayNo - DAYS_PER_WEEK * WEEKS_PER_YEAR, currentDayNo);
		row.S_52WK_HIGH_DATE = new DateTime(i52HighDayNo);

		// 52 week low - selected from the minimum security price up to the 52wk high
		row.S_52WK_LOW = (float) random.rndDoubleIncrRange(MIN_SEC_PRICE, row.S_52WK_HIGH, 0.01);
		i52LowDayNo = random.rndIntRange(currentDayNo - DAYS_PER_WEEK * WEEKS_PER_YEAR, currentDayNo);
		row.S_52WK_LOW_DATE = new DateTime(i52LowDayNo);

		if (random.rndPercent(PERCENT_COMPANIES_WITH_NON_ZERO_DIVIDEND))
		{
			row.S_YIELD = random.rndDoubleIncrRange(S_YIELD_NON_ZERO_MIN, S_YIELD_MAX, 0.01);
			row.S_DIVIDEND = random.rndDoubleIncrRange(row.S_YIELD * 0.20, row.S_YIELD * 0.30, 0.01);
		}
		else
		{
			row.S_DIVIDEND = 0.0;
			row.S_YIELD = 0.0;
		}

		return row;
	}

	private double emulFp(double value)
	{
		float f = (float) value;
		return (double) f;
	}

	/**
	 * Reset the state for the next load unit.
	 */
	private void initNextLoadUnit()
	{
		// The RNG skip count MUST be based on the global lastRowNumber.
		long rngSkipCount = lastRowNumber * RNG_SKIP_ONE_ROW_SECURITY;
		long seed = TpcRandom.rndNthElement(RNG_SEED_TABLE_DEFAULT, rngSkipCount);
		random.setSeed(seed);
	}

	@Override
	public boolean hasMoreRecords()
	{
		return hasMoreRecords;
	}

	@Override
	public SecurityRow generateNextRecord()
	{
		// Reset RNG at Load Unit boundary, so that all data is repeatable.
		if (lastRowNumber % securityCountForOneLoadUnit == 0)
		{
			initNextLoadUnit();
		}

		currentSecurityRow = generateSecurityRow();

		++lastRowNumber;

		// Update state info
		hasMoreRecords = lastRowNumber < (startFromSecurity + securityCount);
		return currentSecurityRow;
	}

	// Getters for the member variables if needed
	public long getSecurityCount()
	{
		return securityCount;
	}

	public long getStartFromSecurity()
	{
		return startFromSecurity;
	}

	public CompanyFile getCompanyFile()
	{
		return companyFile;
	}

	public SecurityFile getSecurityFile()
	{
		return securityFile;
	}

	public int getCurrentDayNo()
	{
		return currentDayNo;
	}

	public String createName(long index)
	{
		String companyName = companyFile.createName(securityFile.getCompanyIndex(index));
		return String
				.format(
						"%s of %s",
						securityFile.getRecord(index).s_issue,
						companyName);
	}

}