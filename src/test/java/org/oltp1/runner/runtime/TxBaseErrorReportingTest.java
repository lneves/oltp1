package org.oltp1.runner.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.oltp1.common.MissingReferenceDataException;

/**
 * Tests that a failed transaction's log line identifies itself.
 *
 * <p>A transaction that raised an unexpected exception used to log the exception text alone. For a
 * {@code NullPointerException} from unboxing a missing reference row that said nothing about which
 * read failed, or which of a dozen transaction types produced it.
 */
class TxBaseErrorReportingTest
{
	private static final class FailingTx extends TxBase
	{
		private final RuntimeException failure;

		private FailingTx(TransactionSpec spec, RuntimeException failure)
		{
			super(null, new TxStatsCollector(spec));
			this.failure = failure;
		}

		@Override
		protected TxOutput run()
		{
			throw failure;
		}
	}

	@Test
	void theErrorNamesTheTransactionAndTheMissingReference()
	{
		TxBase tx = new FailingTx(
				TransactionSpec.TRADE_ORDER,
				new MissingReferenceDataException("LAST_TRADE row for symbol 'ZZZZ'", "no rows for [symbol, ZZZZ]"));

		TxError error = (TxError) tx.execute();
		String text = error.toString();

		assertEquals(-1, error.getStatus());
		assertTrue(text.contains("Trade-Order"), "the transaction must be named: " + text);
		assertTrue(text.contains("LAST_TRADE row for symbol 'ZZZZ'"), text);
		assertTrue(text.contains("ZZZZ"), "the key must be named: " + text);
		assertFalse(text.contains("NullPointerException"), text);
	}

	@Test
	void theErrorIsCountedAsAnErrorAndNotAsALatencySample()
	{
		TxStatsCollector collector = new TxStatsCollector(TransactionSpec.TRADE_RESULT);
		TxBase tx = new TxBase(null, collector)
		{
			@Override
			protected TxOutput run()
			{
				throw new MissingReferenceDataException("tax rate for customer 7", "no rows");
			}
		};

		tx.execute();

		assertEquals(1, collector.getErrorCount());
		assertEquals(0, collector.getCount(), "a failed transaction must not contribute a latency sample");
	}

	@Test
	void theErrorLabelsTheDurationAsMilliseconds()
	{
		TxError error = (TxError) new FailingTx(
				TransactionSpec.BASELINE,
				new IllegalStateException("boom")).execute();

		assertTrue(error.toString().contains("tx_time:"), error.toString());
		assertTrue(error.toString().contains("ms"), "the unit must be labelled: " + error.toString());
	}
}
