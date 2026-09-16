package org.oltp1.runner.generator;

import java.time.LocalDate;
import java.time.Month;

import org.oltp1.runner.tx.security_detail.TxSecurityDetailInput;

public final class SecurityDetailInputGenerator
{
	// Constants from DailyMarketTable.h for date generation

	private static final int SD_TRADE_DAYS_IN_YEAR = 261; // the number of trading days in a year (for DAILY_MARKET)
	private static final int SD_DAILY_MARKET_YEARS = 5; // number of years of history in DAILY_MARKET
	private static final int SD_DAILY_MARKET_TOTAL_ROWS = SD_TRADE_DAYS_IN_YEAR * SD_DAILY_MARKET_YEARS;

	private static final LocalDate SD_DAILY_MARKET_BASE_DATE = LocalDate.of(2000, Month.JANUARY, 3);

	private static final int SD_LOB_ACCESS_PERCENTAGE = 1;
	private static final int SD_MIN_ROWS_TO_RETURN = 5;
	private static final int SD_MAX_ROWS_TO_RETURN = 20;

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
		// Get the thread-local instance of CRandom for all random operations
		CRandom random = ThreadLocalCRandom.get();

		// Select a random security symbol.

		String symbol = companySelector.randomCompany().getSymbol();

		// Determine whether to access the LOB (Large Object) data.
		boolean accessLobFlag = random.rndPercent(SD_LOB_ACCESS_PERCENTAGE);

		// Select a random number of rows of historical data to return.
		int maxRowsToReturn = random.rndIntRange(SD_MIN_ROWS_TO_RETURN, SD_MAX_ROWS_TO_RETURN);

		// Generate a random start date for the historical data query.
		int startDayOffset = random.rndIntRange(0, SD_DAILY_MARKET_TOTAL_ROWS - maxRowsToReturn);
		LocalDate startDay = addWorkDays(SD_DAILY_MARKET_BASE_DATE, startDayOffset);

		return new TxSecurityDetailInput(accessLobFlag, maxRowsToReturn, startDay, symbol);
	}

	private LocalDate addWorkDays(LocalDate startDate, int workdays)
	{
		if (workdays <= 0)
		{
			return startDate;
		}

		int dayOfWeek = startDate.getDayOfWeek().getValue(); // Mon=1 ... Sun=7
		long calendarDays = 0;

		// If starting on a weekend, move to Monday.
		// That Monday counts as the first added workday.
		if (dayOfWeek >= 6)
		{
			calendarDays += 8 - dayOfWeek; // Sat -> +2, Sun -> +1
			workdays--;

			if (workdays == 0)
			{
				return startDate.plusDays(calendarDays);
			}

			dayOfWeek = 1; // Monday
		}

		// Skip complete working weeks.
		calendarDays += (workdays / 5L) * 7;

		int remainingDays = workdays % 5;

		// If the remaining days cross a weekend, add 2 more calendar days.
		if (dayOfWeek + remainingDays > 5)
		{
			calendarDays += remainingDays + 2;
		}
		else
		{
			calendarDays += remainingDays;
		}

		return startDate.plusDays(calendarDays);
	}
}