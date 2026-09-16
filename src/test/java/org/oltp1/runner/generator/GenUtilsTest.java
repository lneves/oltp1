package org.oltp1.runner.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class GenUtilsTest
{
	private static final int COMPLIANT_DAYS = 300;
	private static final int FRAME2_BACKOFF_SECONDS = 4 * 8 * 3600;
	private static final int FRAME3_BACKOFF_SECONDS = 200 * 60;
	private static final int FRAME4_BACKOFF_SECONDS = 500 * 60;

	@Test
	void nonUniformTradeDtsStaysWithinInitialTradePeriod()
	{
		LocalDateTime end = GenUtils.endOfInitialTrades(COMPLIANT_DAYS);
		LocalDateTime base = LocalDateTime.of(2005, 1, 3, 9, 0);
		CRandom random = new CRandom(80534927L);

		int[] backOffs = { FRAME2_BACKOFF_SECONDS, FRAME3_BACKOFF_SECONDS, FRAME4_BACKOFF_SECONDS };

		for (int backOff : backOffs)
		{
			for (int i = 0; i < 1000; i++)
			{
				LocalDateTime start = GenUtils.nonUniformTradeDts(random, COMPLIANT_DAYS, backOff, 4095, 16);

				assertTrue(start.isAfter(base), "start " + start + " must be after the base date");
				assertFalse(start.isAfter(end), "start " + start + " must not be after " + end);
			}
		}
	}

	@Test
	void endOfInitialTradesIncludesFifteenMinuteBuffer()
	{
		// One 8-hour workday from Monday 2005-01-03 09:00 is Tuesday 2005-01-04 09:00,
		// plus the 15-minute buffer for completing pending trades.
		assertEquals(LocalDateTime.of(2005, 1, 4, 9, 15), GenUtils.endOfInitialTrades(1));

		// 300 workdays are exactly 60 weeks later: Monday 2006-02-27, plus the buffer.
		assertEquals(LocalDateTime.of(2006, 2, 27, 9, 15), GenUtils.endOfInitialTrades(300));
	}

	@Test
	void shortHistoryClampsBackOffInsteadOfOverflowing()
	{
		LocalDateTime end = GenUtils.endOfInitialTrades(1);
		CRandom random = new CRandom(42L);

		// The frame-2 back-off (32 hours) is longer than a one-workday history.
		LocalDateTime start = GenUtils.nonUniformTradeDts(random, 1, FRAME2_BACKOFF_SECONDS, 4095, 16);

		assertFalse(start.isAfter(end), "start " + start + " must not be after " + end);
	}

	@Test
	void nonUniformTradeDtsIsDeterministicForTheSameSeed()
	{
		LocalDateTime first = GenUtils
				.nonUniformTradeDts(new CRandom(777L), COMPLIANT_DAYS, FRAME2_BACKOFF_SECONDS, 4095, 16);
		LocalDateTime second = GenUtils
				.nonUniformTradeDts(new CRandom(777L), COMPLIANT_DAYS, FRAME2_BACKOFF_SECONDS, 4095, 16);

		assertEquals(first, second);
	}
}
