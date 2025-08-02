package org.caudexorigo.oltp1.generator;

import java.time.LocalDate;
import java.time.Month;

import org.caudexorigo.oltp1.tx.security_detail.TxSecurityDetailInput;

public final class SecurityDetailInputGenerator
{
	// Constants from DailyMarketTable.h for date generation

	private static final int TRADE_DAYS_IN_YEAR = 261; // the number of trading days in a year (for DAILY_MARKET)
	private static final int DAILY_MARKET_YEARS = 5; // number of years of history in DAILY_MARKET
	private static final int DAILY_MARKET_TOTAL_ROWS = TRADE_DAYS_IN_YEAR * DAILY_MARKET_YEARS;

	private static final LocalDate DAILY_MARKET_BASE_DATE = LocalDate.of(2000, Month.JANUARY, 3);

	private static final int LOB_ACCESS_PERCENTAGE = 1;
	private static final int MIN_ROWS_TO_RETURN = 5;
	private static final int MAX_ROWS_TO_RETURN = 20;



	private final CompanySelector companySelector;

	public SecurityDetailInputGenerator(CompanySelector companySelector)
	{
		this.companySelector = companySelector;
	}

	/**
	 * Generates the input for the Security-Detail transaction.
	 *
	 * @return A TxSecurityDetailInput object populated with generated data.
	 */
	public TxSecurityDetailInput generateSecurityDetailInput()
	{
		TxSecurityDetailInput input = new TxSecurityDetailInput();

		// Get the thread-local instance of CRandom for all random operations
		CRandom random = ThreadLocalCRandom.get();

		// Select a random security symbol.

		input.symbol = companySelector.randomCompany().getSymbol();

		// Determine whether to access the LOB (Large Object) data.
		input.access_lob_flag = random.rndPercent(LOB_ACCESS_PERCENTAGE);

		// Select a random number of rows of historical data to return.
		input.max_rows_to_return = random.rndIntRange(MIN_ROWS_TO_RETURN, MAX_ROWS_TO_RETURN);

		// Generate a random start date for the historical data query.
		int startDayOffset = random.rndIntRange(0, DAILY_MARKET_TOTAL_ROWS - input.max_rows_to_return);
		input.start_day = addWorkDays(DAILY_MARKET_BASE_DATE, startDayOffset);

		return input;
	}

	private LocalDate addWorkDays(LocalDate startDate, int workdays)
	{
		LocalDate result = startDate;
		int added = 0;
		while (added < workdays)
		{
			result = result.plusDays(1);
			if (result.getDayOfWeek().getValue() < 6) // Monday to Friday
			{
				added++;
			}
		}
		return result;
	}
}