package org.oltp1.runner.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ReportMetricsTest
{
	@Test
	void attemptedCountAddsErrorsToSuccesses()
	{
		TxStatsCollector collector = new TxStatsCollector(TransactionSpec.TRADE_RESULT);

		collector.addValue(1.0);
		collector.addValue(2.0);
		collector.incrementErrors();

		assertEquals(2, collector.getCount());
		assertEquals(1, collector.getErrorCount());
		assertEquals(3, collector.getAttemptedCount());
	}

	@Test
	void tpsEUsesSuccessfulTradeResultsOnly()
	{
		BenchmarkMetrics metrics = new BenchmarkMetrics(1);
		TxStatsCollector collector = new TxStatsCollector(TransactionSpec.TRADE_RESULT);
		metrics.registerCollector(collector);
		metrics.markStart();

		for (int i = 0; i < 8; i++)
		{
			collector.addValue(10.0);
		}
		collector.incrementErrors();
		collector.incrementErrors();

		metrics.markEnd();

		TxStatsAggregate aggregate = metrics.aggregateTxStats().get("Trade-Result");

		assertEquals(8, aggregate.getCount());
		assertEquals(10, aggregate.getAttemptedCount());
	}

	@Test
	void errorOnlyCollectorStillContributesErrorsToTheAggregate()
	{
		BenchmarkMetrics metrics = new BenchmarkMetrics(1);
		TxStatsCollector collector = new TxStatsCollector(TransactionSpec.BROKER_VOLUME);
		metrics.registerCollector(collector);
		metrics.markStart();
		collector.incrementErrors();
		metrics.markEnd();

		TxStatsAggregate aggregate = metrics.aggregateTxStats().get("Broker-Volume");

		assertEquals(0, aggregate.getCount());
		assertEquals(1, aggregate.getErrorCount());
		assertEquals(1, aggregate.getAttemptedCount());
	}

	@Test
	void thrownExceptionsCountAsErrorsWithoutLatencySamples()
	{
		TxBase.setQuiet(true);

		BenchmarkMetrics metrics = new BenchmarkMetrics(1);
		TxStatsCollector collector = new TxStatsCollector(TransactionSpec.TRADE_STATUS);

		TxBase tx = new TxBase(metrics, collector)
		{
			@Override
			protected TxOutput run()
			{
				throw new IllegalStateException("boom");
			}
		};

		tx.execute();

		assertEquals(0, collector.getCount());
		assertEquals(1, collector.getErrorCount());
		assertEquals(1, collector.getAttemptedCount());
	}

	@Test
	void businessErrorsCountAsAttemptsButNotSuccesses()
	{
		TxBase.setQuiet(true);

		BenchmarkMetrics metrics = new BenchmarkMetrics(1);
		TxStatsCollector collector = new TxStatsCollector(TransactionSpec.TRADE_UPDATE);

		TxBase tx = new TxBase(metrics, collector)
		{
			@Override
			protected TxOutput run()
			{
				return new TxOutput(-1011);
			}
		};

		tx.execute();

		assertEquals(0, collector.getCount());
		assertEquals(1, collector.getErrorCount());
		assertEquals(1, collector.getAttemptedCount());
	}

	@Test
	void formulasHandleDegenerateInputs()
	{
		assertEquals(50.0, ReportMetrics.actualMixPercent(5, 10), 1e-9);
		assertEquals(0.0, ReportMetrics.actualMixPercent(0, 0), 1e-9);
		assertEquals(0.0, ReportMetrics.tpsE(null, 10.0), 1e-9);
		assertEquals(0.0, ReportMetrics.transactionRate(10, 0.0), 1e-9);
		assertEquals(0.0, ReportMetrics.successRate(null, 10.0), 1e-9);
	}
}
