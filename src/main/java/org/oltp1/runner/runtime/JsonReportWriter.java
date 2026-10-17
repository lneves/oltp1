package org.oltp1.runner.runtime;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonReportWriter implements BiConsumer<String, BenchmarkMetrics>
{
	private final String outputPath;
	private final ObjectMapper objectMapper;

	public JsonReportWriter(String dbInfo)
	{
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
		this.outputPath = String.format("runner-%s-%s.json", normalize(dbInfo), timestamp);
		this.objectMapper = new ObjectMapper();
		this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
	}

	public String getOutputPath()
	{
		return outputPath;
	}

	private double round(double value)
	{
		if (Double.isNaN(value) || Double.isInfinite(value))
		{
			return Double.NaN;
		}

		return (new BigDecimal(value).setScale(2, RoundingMode.HALF_UP)).doubleValue();
	}

	@Override
	public void accept(String sutInfo, BenchmarkMetrics metrics)
	{
		Map<String, TxStatsAggregate> summary = metrics.aggregateTxStats();
		final long totalAttempts = summary
				.values()
				.stream()
				.mapToLong(ReportMetrics::attemptedCount)
				.sum();

		final long totalSuccess = summary
				.values()
				.stream()
				.mapToLong(TxStats::getCount)
				.sum();

		final double elapsedSec = metrics.getElapsedSeconds();

		TxStats tradeResultStat = summary.get("Trade-Result");

		double tpsE = ReportMetrics.tpsE(tradeResultStat, elapsedSec);
		long tradeResultCount = tradeResultStat != null ? tradeResultStat.getCount() : 0;

		Map<String, Object> report = new LinkedHashMap<>();

		report.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
		report.put("sut_info", sutInfo);

		Map<String, Object> holder = new LinkedHashMap<>();
		holder.put("run_time_sec", round(elapsedSec));
		holder.put("total_transactions", totalAttempts);
		holder.put("successful_transactions", totalSuccess);
		holder.put("trade_result_count", tradeResultCount);
		holder.put("tps_E", round(tpsE));
		holder.put("transaction_rate_per_sec", round(ReportMetrics.transactionRate(totalAttempts, elapsedSec)));
		report.put("summary", holder);

		Map<String, Object> transactions = new LinkedHashMap<>();

		summary
				.values()
				.stream()
				.forEach(txStat -> {
					Map<String, Object> txData = new LinkedHashMap<>();

					Map<String, Object> mix = new LinkedHashMap<>();
					mix.put("target_percent", round(txStat.getTxSpec().getTargetWeight()));
					mix.put("actual_percent", round(ReportMetrics.actualMixPercent(ReportMetrics.attemptedCount(txStat), totalAttempts)));
					txData.put("mix", mix);

					txData.put("rate_per_sec", round(ReportMetrics.successRate(txStat, elapsedSec)));

					Map<String, Object> responseTime = new LinkedHashMap<>();
					responseTime.put("mean_ms", round(txStat.getMean()));
					responseTime.put("std_dev_ms", round(txStat.getStdDev()));
					responseTime.put("min_ms", round(txStat.getMin()));
					responseTime.put("max_ms", round(txStat.getMax()));
					responseTime.put("p90_ms", round(txStat.getPercentile(90)));
					responseTime.put("p95_ms", round(txStat.getPercentile(95)));
					responseTime.put("p99_ms", round(txStat.getPercentile(99)));
					txData.put("response_time", responseTime);

					Map<String, Object> counts = new LinkedHashMap<>();
					counts.put("total", ReportMetrics.attemptedCount(txStat));
					counts.put("successful", txStat.getCount());
					counts.put("attempts", ReportMetrics.attemptedCount(txStat));
					counts.put("warnings", txStat.getWarningCount());
					counts.put("errors", txStat.getErrorCount());
					counts.put("rollbacks", txStat.getRollbackCount());
					txData.put("counts", counts);

					transactions.put(txStat.getTxSpec().getDisplayName(), txData);
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

	private static String normalize(String input)
	{
		if (input == null || input.isBlank())
		{
			return "";
		}

		return Arrays
				.stream(input.toLowerCase().split("[^\\p{L}\\p{N}]+"))
				.filter(s -> !s.isEmpty())
				.collect(Collectors.joining("_"));
	}
}