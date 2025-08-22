package org.oltp1.runner.generator;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.Month;

public class GenUtils
{
	// The fixed historical start date for all initial trades.
	// Based on InitialTradePopulationBaseYear etc. in Utilities/inc/MiscConsts.h
	private static final LocalDateTime TRADE_POPULATION_BASE_DATE = LocalDateTime.of(2005, Month.JANUARY, 3, 9, 0, 0);

	// Duration of the initial trade period.
	private static final int HOURS_PER_WORKDAY = 8;
	private static final int SECONDS_PER_HOUR = 3600;
	private static final int MS_PER_SECOND = 1000;

	private static final int ABORT_TRADE = 101;
	private static final int ABORTED_TRADE_MOD_FACTOR = 51;
	private static final long TRADE_SHIFT = 200_000_000_000_000L;

	/**
	 * Generates a non-uniform random Trade ID from the pre-populated range. Based
	 * on the logic in CCETxnInputGenerator::GenerateNonUniformTradeID.
	 *
	 * @param random
	 *            The compliant random number generator.
	 * @param maxActivePrePopulatedTradeID
	 *            The maximum trade ID from the initial data load.
	 * @param aValue
	 *            The 'A' parameter for the NURand function, controlling skew.
	 * @param sValue
	 *            The 's' (shift) parameter for the NURand function.
	 * @return A non-uniformly distributed random Trade ID.
	 */
	public static long nonUniformTradeId(CRandom random, long maxActivePrePopulatedTradeID, int aValue, int sValue)
	{
		// Generate a non-uniform random value in the valid range.
		long tradeId = random.nonUniformRandom(1, maxActivePrePopulatedTradeID, aValue, sValue);

		// Check if this ID corresponds to one that was intentionally skipped
		// during the data load to simulate a rolled-back transaction.
		if ((tradeId % ABORT_TRADE) == ABORTED_TRADE_MOD_FACTOR)
		{
			tradeId++; // If so, skip to the next valid ID.
		}

		// Add the standard high-value offset to place it in the final T_ID range.
		return tradeId + TRADE_SHIFT;
	}

	/**
	 * Generates a non-uniform random date within the initial trade period. Based on
	 * the logic in CCETxnInputGenerator::GenerateNonUniformTradeDTS.
	 *
	 * @param random
	 *            The compliant random number generator.
	 * @param aValue
	 *            The 'A' parameter for the NURand function, controlling skew.
	 * @param sValue
	 *            The 's' (shift) parameter for the NURand function.
	 * @return A randomly generated LocalDateTime.
	 */
	public static LocalDateTime nonUniformTradeDts(CRandom random, int daysOfInitialTrades, int aValue, int sValue)
	{

		long totalInitialTradeMs = (long) daysOfInitialTrades * HOURS_PER_WORKDAY * SECONDS_PER_HOUR * MS_PER_SECOND;
		// Generate a non-uniform random offset in milliseconds from the start time.
		long tradeTimeOffsetMs = random.nonUniformRandom(1, totalInitialTradeMs, aValue, sValue);

		// Add the generated work-time milliseconds to the base date.
		return addWorkMs(TRADE_POPULATION_BASE_DATE, tradeTimeOffsetMs);
	}

	/**
	 * Calculates the end date for the initial trade population period.
	 *
	 * @param daysOfInitialTrades
	 *            The number of 8-hour workdays to simulate.
	 * @return The calculated end date and time.
	 */
	public static LocalDateTime endOfInitialTrades(int daysOfInitialTrades)
	{
		// Calculate the total duration in milliseconds.
		long totalWorkMs = (long) daysOfInitialTrades * HOURS_PER_WORKDAY * SECONDS_PER_HOUR * MS_PER_SECOND;

		// Add a 15-minute buffer to allow for completion of pending trades.
		// This is explicitly done in the CETxnInputGenerator.cpp constructor.
		totalWorkMs += 15 * 60 * MS_PER_SECOND;

		// 4. Add the work duration to the start date, skipping weekends.
		return addWorkMs(TRADE_POPULATION_BASE_DATE, totalWorkMs);
	}

	/**
	 * Adds a specified number of "work" milliseconds to a base date, simulating an
	 * 8-hour workday and skipping weekends. Based on CDateTime::AddWorkMs.
	 */
	private static LocalDateTime addWorkMs(LocalDateTime startDate, long workMs)
	{
		long msPerWorkDay = (long) HOURS_PER_WORKDAY * SECONDS_PER_HOUR * MS_PER_SECOND;

		long workDays = workMs / msPerWorkDay;
		long remainingMsInDay = workMs % msPerWorkDay;

		LocalDateTime resultDate = startDate;
		long daysAdded = 0;

		// Add full work weeks first for efficiency
		long workWeeks = workDays / 5;
		resultDate = resultDate.plusWeeks(workWeeks);
		daysAdded += workWeeks * 5;

		// Add the remaining days one by one, skipping weekends
		while (daysAdded < workDays)
		{
			resultDate = resultDate.plusDays(1);
			if (resultDate.getDayOfWeek() != DayOfWeek.SATURDAY && resultDate.getDayOfWeek() != DayOfWeek.SUNDAY)
			{
				daysAdded++;
			}
		}

		// Add the remaining milliseconds for the final day
		resultDate = resultDate.plusNanos(remainingMsInDay * 1_000_000);

		return resultDate;
	}
}
