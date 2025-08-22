package org.oltp1.egen.generator;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.model.CompanyRow;
import org.oltp1.egen.util.DateTime;
import org.oltp1.egen.util.TpcRandom;

/**
 * Generates data for the COMPANY table. This class is a direct port of the C++
 * CCompanyTable class. It handles the scaling of companies based on customer
 * count and deterministically generates all company attributes. Based on
 * inc/CompanyTable.h and src/CompanyTable.cpp
 */
public class CompanyTable implements TableGenerator<CompanyRow>
{
	private static final long IDENT_T_SHIFT = 4300000000L;
	private static final int RNG_SKIP_ONE_ROW_COMPANY = 2;
	private static final long RNG_SEED_TABLE_DEFAULT = 37039940L;
	private static final long RNG_SEED_BASE_SP_RATE = 56593330L;
	private static final long DEFAULT_LOAD_UNIT_SIZE = 1000;

	private final DataFileManager dfm;
	private final CompanyFile companyFile;
	private final Person person;
	private final TpcRandom random;

	private final long companyCount;
	private final long startFromCompany;
	private long lastRowNumber; // 0-based index for this instance's generation
	private boolean hasMoreRecords;

	private final int jan1_1800_DayNo;
	private final int jan2_2000_DayNo;

	public CompanyTable(DataFileManager dfm, long customerCount, long startFromCustomer)
	{
		this.dfm = dfm;
		this.random = new TpcRandom(0); // Seed is managed by initNextLoadUnit
		this.person = new Person(dfm, 0, false); // Not cached

		this.companyFile = dfm.getCompanyFile();
		this.companyCount = this.companyFile.calculateCompanyCount(customerCount);
		this.startFromCompany = this.companyFile.calculateStartFromCompany(startFromCustomer);

		this.lastRowNumber = 0;
		this.hasMoreRecords = this.companyCount > 0;

		this.jan1_1800_DayNo = DateTime.ymdToDayNumber(1800, 1, 1);
		this.jan2_2000_DayNo = DateTime.ymdToDayNumber(2000, 1, 2);
	}

	public long getLastRowNumber()
	{
		return lastRowNumber;
	}

	public long getCurrentCompanyId()
	{
		return (companyFile.getCompanyId(lastRowNumber));
	}

	public boolean hasMoreRecords()
	{
		if (lastRowNumber >= companyCount)
		{
			hasMoreRecords = false;
		}
		return hasMoreRecords;
	}

	public CompanyRow generateNextRecord()
	{
        if (lastRowNumber % companyFile.getCompanyCountForOneLoadUnit() == 0)
        {
            initNextLoadUnit();
        }

		CompanyRow row = new CompanyRow();

		row.CO_ID = companyFile.getCompanyId(lastRowNumber);
		row.CO_ST_ID = companyFile.getRecord(lastRowNumber).co_st_id;
		row.CO_IN_ID = companyFile.getRecord(lastRowNumber).co_in_id;
		row.CO_NAME = companyFile.createName(lastRowNumber);
		row.CO_DESC = companyFile.getRecord(lastRowNumber).co_desc;
		row.CO_CEO = generateCeoName(row.CO_ID);
		row.CO_SP_RATE = generateSpRating(row.CO_ID);
		row.CO_AD_ID = dfm.getExchangeDataFile().size() + lastRowNumber + 1 + IDENT_T_SHIFT;

		int openDayNum = random.rndIntRange(jan1_1800_DayNo, jan2_2000_DayNo);
		row.CO_OPEN_DATE = new DateTime(openDayNum);

		lastRowNumber++;

		return row;
	}
	
	private void initNextLoadUnit()
	{
		long rngSkipCount = lastRowNumber * RNG_SKIP_ONE_ROW_COMPANY;
		long seed = TpcRandom.rndNthElement(RNG_SEED_TABLE_DEFAULT, rngSkipCount);
		random.setSeed(seed);
	}

	private String generateCeoName(long companyId)
	{
		long ceoId = companyId * 1000;
		String fName = person.getFirstName(ceoId, person.isMaleGender(ceoId));
		String lName = person.getLastName(ceoId);
		return String.format("%s %s", fName, lName);
	}

	private String generateSpRating(long companyId)
	{
		long oldSeed = random.getSeed();
		random.setSeed(TpcRandom.rndNthElement(RNG_SEED_BASE_SP_RATE, companyId));
		String rating = dfm.getCompanySpRateDataFile().getRecord(random.rndIntRange(0, dfm.getCompanySpRateDataFile().size() - 1)).co_sp_rate;
		random.setSeed(oldSeed);
		return rating;
	}

	public boolean generateNextCoId()
	{
		lastRowNumber++;
		hasMoreRecords = lastRowNumber < (startFromCompany + companyCount);
		return hasMoreRecords;
	}

}