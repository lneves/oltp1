package org.oltp1.egen.generator;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.model.LastTradeRow;
import org.oltp1.egen.util.DateTime;

public class LastTradeTable implements TableGenerator<LastTradeRow>
{
	private static final int initialTradePopulationBaseYear = 2005;
	private static final int initialTradePopulationBaseMonth = 1;
	private static final int initialTradePopulationBaseDay = 3;
	private static final int initialTradePopulationBaseHour = 9;
	private static final int initialTradePopulationBaseMinute = 0;
	private static final int initialTradePopulationBaseSecond = 0;
	private static final int initialTradePopulationBaseFraction = 0;

	private final DataFileManager dfm;
	private final SecurityFile securityFile;
	private final MEESecurity meeSecurity; // For calculating prices

	private final long securityCountForThisInstance;
	private final long startFromSecurity;
	private long lastSecurityRowNumber; // Tracks the global index
	private boolean hasMoreRecords;
	private final DateTime lastTradeDts;

	public LastTradeTable(DataFileManager dfm, long customerCount, long startFromCustomer, int daysOfInitialTrades)
	{
		this.dfm = dfm;
		this.securityFile = dfm.getSecurityFile();

		int hoursOfInitialTrades = daysOfInitialTrades * 8;
		// MEE Security is used for deterministic price calculations.

		// Calculate the final timestamp for all last trades
		this.lastTradeDts = new DateTime(2005, 1, 3); // InitialTradePopulationBaseDate

		MEESecurity mee = new MEESecurity();
		mee.init(hoursOfInitialTrades * 3600, null, null, 0);

		this.meeSecurity = mee;

		this.securityCountForThisInstance = securityFile.calculateSecurityCount(customerCount);
		this.startFromSecurity = securityFile.calculateStartFromSecurity(startFromCustomer);

		this.lastSecurityRowNumber = this.startFromSecurity;
		this.hasMoreRecords = this.securityCountForThisInstance > 0;

		lastTradeDts
				.set(
						initialTradePopulationBaseYear,
						initialTradePopulationBaseMonth,
						initialTradePopulationBaseDay,
						initialTradePopulationBaseHour,
						initialTradePopulationBaseMinute,
						initialTradePopulationBaseSecond,
						initialTradePopulationBaseFraction);

		this.lastTradeDts.add(daysOfInitialTrades, 0, true);

	}

	@Override
	public boolean hasMoreRecords()
	{

		if (lastSecurityRowNumber >= startFromSecurity + securityCountForThisInstance)
		{
			hasMoreRecords = false;
		}

		return hasMoreRecords;
	}

	@Override
	public LastTradeRow generateNextRecord()
	{
		LastTradeRow row = new LastTradeRow();

		row.LT_S_SYMB = securityFile.createSymbol(lastSecurityRowNumber);
		row.LT_DTS = lastTradeDts;

		// The price is the calculated price at the end of the initial trade period
		// (time = 0 for the MEE).

		row.LT_PRICE = meeSecurity.calculatePrice(lastSecurityRowNumber, 0).dollarAmount();
		row.LT_OPEN_PRICE = meeSecurity.calculatePrice(lastSecurityRowNumber, 0).dollarAmount();

		// LT_VOL tracks trading volume for the current day. Since the initial
		// population ends at a day boundary, this is initialized to 0.
		row.LT_VOL = 0;

		lastSecurityRowNumber++;

		return row;
	}
}