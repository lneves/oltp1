package org.oltp1.runner.perf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class TxStatsCollectorTest {

    private TxStatsCollector collector;

    @Before
    public void setUp() {
        collector = new TxStatsCollector("TestTx");
    }

    @Test
    public void testInitialState() {
        TxSummary summary = collector.getStats();
        assertEquals("TestTx", summary.getTxName());
        assertEquals(0, summary.getCount());
        assertEquals(0, summary.getErroCount());
        assertEquals(0, summary.getWarningCount());
        assertEquals(0, summary.getRollbackCount());
        assertTrue(Double.isNaN(summary.getMean()));
    }

    @Test
    public void testAddValue() {
        collector.addValue(10.0);
        collector.addValue(20.0);
        collector.addValue(30.0);

        TxSummary summary = collector.getStats();
        assertEquals(3, summary.getCount());
        assertEquals(10.0, summary.getMin(), 0.001);
        assertEquals(30.0, summary.getMax(), 0.001);
        assertEquals(20.0, summary.getMean(), 0.001);
        assertEquals(60.0, summary.getSum(), 0.001);
    }

    @Test
    public void testCounters() {
        collector.incrementErrors();
        collector.incrementErrors();
        collector.incrementWarnings();
        collector.incrementRollBacks();

        assertEquals(2, collector.getErrorCount());
        assertEquals(1, collector.getWarningCount());
        assertEquals(1, collector.getRollBacksCount());
    }

    @Test
    public void testClearStats() {
        collector.addValue(50.0);
        collector.incrementErrors();
        collector.incrementWarnings();
        collector.incrementRollBacks();
        collector.offerMinTs(1000L);
        collector.offerMaxTs(2000L);

        collector.clearStats();

        TxSummary summary = collector.getStats();
        assertEquals(0, summary.getCount());
        assertEquals(0, summary.getErroCount());
        assertEquals(0, summary.getWarningCount());
        assertEquals(0, summary.getRollbackCount());
        assertEquals(Long.MAX_VALUE, summary.getMinTs());
        assertEquals(Long.MIN_VALUE, summary.getMaxTs());
        assertTrue(Double.isNaN(summary.getMean()));
    }

    @Test
    public void testTimestampUpdates() {
        collector.offerMinTs(5000L);
        collector.offerMaxTs(10000L);
        collector.offerMinTs(4000L); // should update min
        collector.offerMaxTs(11000L); // should update max
        collector.offerMinTs(6000L); // should not update min
        collector.offerMaxTs(9000L); // should not update max

        TxSummary summary = collector.getStats();
        assertEquals(4000L, summary.getMinTs());
        assertEquals(11000L, summary.getMaxTs());
    }
}

