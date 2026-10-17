package org.oltp1.runner.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

class TxStatsCollectorTest
{
	@Test
	void accumulatesSuccessfulSamples()
	{
		TxStatsCollector collector = new TxStatsCollector(TransactionSpec.TRADE_RESULT);

		collector.addValue(10.0);
		collector.addValue(20.0);
		collector.addValue(30.0);

		assertEquals(3, collector.getCount());
		assertEquals(10.0, collector.getMin(), 1e-9);
		assertEquals(30.0, collector.getMax(), 1e-9);
		assertEquals(20.0, collector.getMean(), 1e-9);
	}

	@Test
	void ignoresNonFiniteAndNegativeSamples()
	{
		TxStatsCollector collector = new TxStatsCollector(TransactionSpec.TRADE_STATUS);

		collector.addValue(-1.0);
		collector.addValue(Double.NaN);
		collector.addValue(Double.POSITIVE_INFINITY);
		collector.addValue(5.0);

		assertEquals(1, collector.getCount());
		assertEquals(5.0, collector.getMean(), 1e-9);
	}

	@Test
	void percentileApproximatesSortedSamples()
	{
		TxStatsCollector collector = new TxStatsCollector(TransactionSpec.TRADE_RESULT);

		for (int i = 1; i <= 100; i++)
		{
			collector.addValue(i);
		}

		assertEquals(90.0, collector.getPercentile(90), 3.0);
	}

	@Test
	void countersTrackErrorsWarningsAndRollbacks()
	{
		TxStatsCollector collector = new TxStatsCollector(TransactionSpec.TRADE_UPDATE);

		collector.incrementErrors();
		collector.incrementErrors();
		collector.incrementWarnings();
		collector.incrementRollbackCount();

		assertEquals(2, collector.getErrorCount());
		assertEquals(1, collector.getWarningCount());
		assertEquals(1, collector.getRollbackCount());
	}

	@Test
	void addValueDoesNotWriteToStdOutOrStdErr()
	{
		PrintStream originalOut = System.out;
		PrintStream originalErr = System.err;
		ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
		ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();

		try
		{
			System.setOut(new PrintStream(capturedOut));
			System.setErr(new PrintStream(capturedErr));

			TxStatsCollector collector = new TxStatsCollector(TransactionSpec.TRADE_RESULT);
			for (int i = 0; i < 100; i++)
			{
				collector.addValue(i);
			}

			assertEquals("", capturedOut.toString());
			assertEquals("", capturedErr.toString());
		}
		finally
		{
			System.setOut(originalOut);
			System.setErr(originalErr);
		}
	}

	@Test
	void percentilesRequireAValueInRange()
	{
		TxStatsCollector collector = new TxStatsCollector(TransactionSpec.TRADE_RESULT);
		collector.addValue(1.0);

		boolean thrown = false;
		try
		{
			collector.getPercentile(150);
		}
		catch (IllegalArgumentException e)
		{
			thrown = true;
		}

		assertTrue(thrown);
	}
}
