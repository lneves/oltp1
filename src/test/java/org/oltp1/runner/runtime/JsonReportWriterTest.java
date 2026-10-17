package org.oltp1.runner.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class JsonReportWriterTest
{
	@Test
	void reportUsesSuccessCountForTpsEAndAttemptsForMix() throws Exception
	{
		BenchmarkMetrics metrics = new BenchmarkMetrics(1);
		TxStatsCollector collector = new TxStatsCollector(TransactionSpec.TRADE_RESULT);
		metrics.registerCollector(collector);
		metrics.markStart();

		for (int i = 0; i < 6; i++)
		{
			collector.addValue(5.0);
		}
		collector.incrementErrors();
		collector.incrementErrors();

		metrics.markEnd();

		JsonReportWriter writer = new JsonReportWriter("unit-test");
		Path report = Path.of(writer.getOutputPath());

		try
		{
			writer.accept("unit-test", metrics);

			JsonNode root = new ObjectMapper().readTree(report.toFile());

			assertEquals(6, root.at("/summary/trade_result_count").asLong());
			assertEquals(8, root.at("/summary/total_transactions").asLong());
			assertEquals(6, root.at("/summary/successful_transactions").asLong());
			assertTrue(root.at("/summary/tps_E").asDouble() > 0.0);
			assertEquals(8, root.at("/transactions/Trade-Result/counts/attempts").asLong());
			assertEquals(6, root.at("/transactions/Trade-Result/counts/successful").asLong());
			assertEquals(2, root.at("/transactions/Trade-Result/counts/errors").asLong());
		}
		finally
		{
			Files.deleteIfExists(report);
		}
	}
}
