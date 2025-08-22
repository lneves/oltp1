package org.oltp1.runner.perf;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.oltp1.runner.db.SqlEngine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonReportWriter implements Consumer<TxRunSummary>
{
	private final String outputPath;
	private final ObjectMapper objectMapper;

	public JsonReportWriter(SqlEngine engine)
	{
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
		this.outputPath = String.format("runner-%s-%s.json", engine.toString().toLowerCase(), timestamp);
		this.objectMapper = new ObjectMapper();
		this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
	}

	public String getOutputPath()
	{
		return outputPath;
	}

	private double round(double value)
	{
		return (new BigDecimal(value).setScale(2, RoundingMode.HALF_UP)).doubleValue();
	}

	@Override
	public void accept(final TxRunSummary trun)
	{
		final long globalMinTs = trun.getTxStats().stream().mapToLong(t -> t.getMinTs()).min().getAsLong();
		final long globalMaxTs = trun.getTxStats().stream().mapToLong(t -> t.getMaxTs()).max().getAsLong();
		final double globalElapsed = ((double) (globalMaxTs - globalMinTs) / 1000000.0);
		final long totalTx = trun.getTxStats().stream().mapToLong(t -> t.getCount()).sum();

		Map<String, Object> report = new LinkedHashMap<>();

		report.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
		report.put("sut_info", trun.getSutInfo());

		Map<String, Object> summary = new LinkedHashMap<>();
		summary.put("run_time_sec", round(globalElapsed / 1000));
		summary.put("clients", trun.getNumClients());
		summary.put("total_transactions", totalTx);
		summary.put("transaction_rate_per_sec", round(((double) totalTx) / (globalElapsed / 1000.0)));
		report.put("summary", summary);

		Map<String, Object> transactions = new LinkedHashMap<>();

		trun.getTxStats().stream().forEach(txStat -> {
			Map<String, Object> txData = new LinkedHashMap<>();

			Map<String, Object> mix = new LinkedHashMap<>();
			mix.put("target_percent", round(txStat.getTargetMixPct() * 100.0));
			mix.put("actual_percent", round(txStat.getActualMixPct(totalTx) * 100.0));
			txData.put("mix", mix);

			txData.put("rate_per_sec", round(txStat.getCount() / (globalElapsed / 1000.0)));

			Map<String, Object> responseTime = new LinkedHashMap<>();
			responseTime.put("mean_ms", round(txStat.getMean()));
			responseTime.put("std_dev_ms", round(txStat.getStdDev()));
			responseTime.put("min_ms", round(txStat.getMin()));
			responseTime.put("max_ms", round(txStat.getMax()));
			responseTime.put("p90_ms", round(txStat.getQuantile(0.9)));
			responseTime.put("p95_ms", round(txStat.getQuantile(0.95)));
			responseTime.put("p99_ms", round(txStat.getQuantile(0.99)));
			txData.put("response_time", responseTime);

			Map<String, Object> counts = new LinkedHashMap<>();
			counts.put("total", txStat.getCount());
			counts.put("warnings", txStat.getWarningCount());
			counts.put("errors", txStat.getErroCount());
			counts.put("rollbacks", txStat.getRollbackCount());
			txData.put("counts", counts);

			transactions.put(txStat.getTxName(), txData);
		});

		report.put("transactions", transactions);

		try (FileWriter fileWriter = new FileWriter(outputPath);
				PrintWriter printWriter = new PrintWriter(fileWriter))
		{
			String jsonOutput = objectMapper.writeValueAsString(report);
			printWriter.print(jsonOutput);
			printWriter.flush();
		}
		catch (IOException e)
		{
			throw new RuntimeException("Failed to write JSON report to " + outputPath, e);
		}
	}
}