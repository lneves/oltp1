package org.oltp1.egen.generator;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.model.DailyMarketRow;
import org.oltp1.egen.util.DateTime;
import org.oltp1.egen.util.TpcRandom;

/**
 * Generates data for the DAILY_MARKET table. This is a port of the C++
 * CDailyMarketTable class. It creates a 5-year history of daily market data for
 * each security in the database. Based on inc/DailyMarketTable.h and
 * src/DailyMarketTable.cpp
 */
public class DailyMarketTable implements TableGenerator<DailyMarketRow>
{
	private static final int RNG_SKIP_ONE_ROW_DAILY_MARKET = 2;
	private static final long RNG_SEED_TABLE_DEFAULT = 37039940L;
	private static final int DAILY_MARKET_YEARS = 5;
	private static final int TRADE_DAYS_IN_YEAR = 261;
	private static final int TOTAL_MARKET_ROWS = DAILY_MARKET_YEARS * TRADE_DAYS_IN_YEAR;

	private final TpcRandom random;
	private final SecurityFile securityFile;

	private final long securityCountForThisInstance;
	private final long startFromSecurity;
	private long lastSecurityRowNumber; // Tracks the current security being processed
	private int rowsGeneratedForCurrentSecurity;
	private boolean hasMoreRecords;
	private boolean moreSecurities;

	private final DailyMarketRow[] currentRowBlock = new DailyMarketRow[TOTAL_MARKET_ROWS];

	public DailyMarketTable(DataFileManager dfm, long customerCount, long startFromCustomer)
	{
		this.random = new TpcRandom(0);
		this.securityFile = dfm.getSecurityFile();

		this.securityCountForThisInstance = securityFile.calculateSecurityCount(customerCount);
		this.startFromSecurity = securityFile.calculateStartFromSecurity(startFromCustomer);

		this.lastSecurityRowNumber = this.startFromSecurity;
		this.rowsGeneratedForCurrentSecurity = TOTAL_MARKET_ROWS; // Trigger generation on first call
		this.hasMoreRecords = this.securityCountForThisInstance > 0;
		this.moreSecurities = true; // initialize once

	}

	@Override
	public boolean hasMoreRecords()
	{
		return hasMoreRecords;
	}

	@Override
	public DailyMarketRow generateNextRecord()
	{
		// Check if we need to generate a new block of rows for the next security
		if (rowsGeneratedForCurrentSecurity >= TOTAL_MARKET_ROWS)
		{
			if (moreSecurities)
			{
				// Check for load unit boundary before generating new block
				if (lastSecurityRowNumber % securityFile.getSecurityCountForOneLoadUnit() == 0)
				{
					initNextLoadUnit();
				}
				generateDailyMarketRowsForSecurity();
				rowsGeneratedForCurrentSecurity = 0;
				lastSecurityRowNumber++;

				// Update state info
				moreSecurities = lastSecurityRowNumber < (startFromSecurity + securityCountForThisInstance);
			}
		}

		// Return false when generated the last row of the last security
		if (!moreSecurities && (rowsGeneratedForCurrentSecurity == TOTAL_MARKET_ROWS - 1))
		{
			hasMoreRecords = false;
		}

		// Return the next row from the pre-generated block
		return currentRowBlock[rowsGeneratedForCurrentSecurity++];
	}

	private void initNextLoadUnit()
	{
		long rngSkipCount = lastSecurityRowNumber * RNG_SKIP_ONE_ROW_DAILY_MARKET;
		long seed = TpcRandom.rndNthElement(RNG_SEED_TABLE_DEFAULT, rngSkipCount);
		random.setSeed(seed);
	}

	private void generateDailyMarketRowsForSecurity()
	{
		DateTime currentDate = new DateTime(2000, 1, 3); // Base date from C++
		int dayNumber = currentDate.getDayNumber();

		String symbol = securityFile.createSymbol(lastSecurityRowNumber);

		for (int i = 0; i < TOTAL_MARKET_ROWS; i++)
		{
			DailyMarketRow row = new DailyMarketRow();
			row.DM_S_SYMB = symbol;
			row.DM_DATE = new DateTime(dayNumber); // Create a copy

			row.DM_CLOSE = random.rndDoubleIncrRange(20.00, 30.00, 0.01);
			row.DM_HIGH = row.DM_CLOSE * 1.05;
			row.DM_LOW = row.DM_CLOSE * 0.92;
			row.DM_VOL = random.rndInt64Range(1000, 10000);

			currentRowBlock[i] = row;

			++dayNumber; // go one day forward for the next row

			if ((dayNumber % DateTime.daysPerWeek) == DateTime.daysPerWorkWeek)
				dayNumber += 2; // skip weekend
		}
	}
}