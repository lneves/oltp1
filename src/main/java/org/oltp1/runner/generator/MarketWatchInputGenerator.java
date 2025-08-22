package org.oltp1.runner.generator;

import java.time.LocalDate;
import java.time.Month;

import org.oltp1.runner.model.RandomCustomer;
import org.oltp1.runner.tx.market_watch.TxMarketWatchInput;

public class MarketWatchInputGenerator
{
	// Default values from DriverParamSettings.h for a compliant run
	private static final int MW_PERCENT_BY_INDUSTRY = 5;
	private static final int MW_PERCENT_BY_WATCH_LIST = 60;
	// The remaining 35% is by_acct_id

	// Constants from DailyMarketTable.h for date generation
	private static final LocalDate MW_DAILY_MARKET_BASE_DATE = LocalDate.of(2000, Month.JANUARY, 3);

	private final CustomerSelector customerSelector;
	private final IndustrySelector industrySelector;

	public MarketWatchInputGenerator(CustomerSelector customerSelector, IndustrySelector industrySelector)
	{
		this.customerSelector = customerSelector;
		this.industrySelector = industrySelector;
	}

	/**
	 * Generates the input for the Market-Watch transaction. Based on
	 * CCETxnInputGenerator::GenerateMarketWatchInput.
	 *
	 * @return A TxMarketWatchInput object populated with generated data.
	 */
	public TxMarketWatchInput generateMarketWatchInput()
	{
		// Securities chosen by
		// Watch list:
		// 60% 57% to 63%
		// Account_ID:
		// 35% 33% to 37%
		// Industry:
		// 5% 4.5% to 5.5%

		TxMarketWatchInput input = new TxMarketWatchInput();
		CRandom random = ThreadLocalCRandom.get();

		// Randomly determine the input type for the transaction.
		int threshold = random.rndIntRange(1, 100);

		if (threshold <= MW_PERCENT_BY_INDUSTRY)
		{
			// By Industry: Select a random industry name.
			int industryIndex = random.rndIntRange(0, industrySelector.getLen() - 1);
			input.industry_name = industrySelector.get(industryIndex).getInName();
		}
		else if (threshold <= MW_PERCENT_BY_INDUSTRY + MW_PERCENT_BY_WATCH_LIST)
		{
			// By Watch List: Select a random customer ID.
			RandomCustomer customer = customerSelector.randomCustomer();
			input.c_id = customer.cId;
		}
		else
		{
			// By Account ID: Select a random customer account ID.
			RandomCustomer customer = customerSelector.randomCustomer();
			input.acct_id = customerSelector.getRndAccIdForCustomer(customer);
		}

		// Generate a random start date for the query.
		input.start_day = getStartDay(random);

		return input;
	}

	// This logic is ported from the C++ source to generate a non-uniform date.
	private final LocalDate getStartDay(CRandom random)
	{
		// Selects a random week from the 5-year (260-week) period.
		// + 5 ensures the selected date is always at least five weeks into the
		// historical data.
		int week = (int) random.nonUniformRandom(0, 255, 255, 0) + 5;

		// Select a day within that week using a non-uniform, weighted distribution
		// to simulate realistic user behavior where queries favor the start and end of
		// the week.
		// The probabilities are as follows:
		// - Monday: 20%
		// - Tuesday: 7%
		// - Wednesday: 6%
		// - Thursday: 7%
		// - Friday: 60%

		int dayOfWeek;
		int dayThreshold = random.rndIntRange(1, 100);
		if (dayThreshold > 40)
			dayOfWeek = 4; // Friday
		else if (dayThreshold <= 20)
			dayOfWeek = 0; // Monday
		else if (dayThreshold <= 27)
			dayOfWeek = 1; // Tuesday
		else if (dayThreshold <= 33)
			dayOfWeek = 2; // Wednesday
		else
			dayOfWeek = 3; // Thursday

		int totalDayOffset = week * 7 + dayOfWeek;

		// go back to our start date and add our calculated day.
		LocalDate startDate = MW_DAILY_MARKET_BASE_DATE.plusDays(totalDayOffset);

		return startDate;
	}
}