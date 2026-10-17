package org.oltp1.runner.runtime;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;

public class ConsoleReportWriter implements BiConsumer<String, BenchmarkMetrics>
{
	private static final Locale FMT = Locale.ROOT;

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

		PrintWriter pw = new PrintWriter(System.out);
		try
		{
			pw.write("\n");
			pw.printf("#SUT%n%n%s%n%n", sutInfo);
			pw.printf("Date: %s%n%n", LocalDateTime.now().toString());

			pw.println(StringUtils.leftPad("----------- Response Time(ms) ----------", 100, " "));
			pw.print(StringUtils.rightPad("Transaction", 20, " "));
			pw.print(StringUtils.leftPad("Target(%)", 10, " "));
			pw.print(StringUtils.leftPad("Actual(%)", 10, " "));
			pw.print(StringUtils.leftPad("Rate(tx/sec)", 14, " "));
			pw.print(StringUtils.leftPad("Mean", 9, " "));
			pw.print(StringUtils.leftPad("StdDev", 9, " "));
			pw.print(StringUtils.leftPad("Min", 9, " "));
			pw.print(StringUtils.leftPad("Max", 9, " "));
			pw.print(StringUtils.leftPad("Pct90", 9, " "));
			pw.print(StringUtils.leftPad("Attempts", 10, " "));
			pw.print(StringUtils.leftPad("Success", 9, " "));
			pw.print(StringUtils.leftPad("Warnings", 10, " "));
			pw.print(StringUtils.leftPad("Errors", 8, " "));
			pw.println(StringUtils.leftPad("Rollbacks", 11, " "));

			summary
					.values()
					.stream()
					.forEach(test -> {

						String tTargetMixPct = String.format(FMT, "%.2f", test.getTxSpec().getTargetWeight());
						String tActualMixPct = String
								.format(
										FMT,
										"%.2f",
										ReportMetrics.actualMixPercent(ReportMetrics.attemptedCount(test), totalAttempts));
						String tRate = String.format(FMT, "%.2f", ReportMetrics.successRate(test, elapsedSec));
						String tMean = String.format(FMT, "%.2f", test.getMean());
						String tStdDev = String.format(FMT, "%.2f", test.getStdDev());
						String tMin = String.format(FMT, "%.2f", test.getMin());
						String tMax = String.format(FMT, "%.2f", test.getMax());
						String tP90 = String.format(FMT, "%.2f", test.getPercentile(90));
						String tAttempts = String.format("%s", ReportMetrics.attemptedCount(test));
						String tCount = String.format("%s", test.getCount());
						String tWCount = String.format("%s", test.getWarningCount());
						String tECount = String.format("%s", test.getErrorCount());
						String tRCount = String.format("%s", test.getRollbackCount());

						pw.print(StringUtils.rightPad(test.getTxSpec().getDisplayName(), 20, " "));
						pw.print(StringUtils.leftPad(tTargetMixPct, 10, " "));
						pw.print(StringUtils.leftPad(tActualMixPct, 10, " "));
						pw.print(StringUtils.leftPad(tRate, 14, " "));
						pw.print(StringUtils.leftPad(tMean, 9, " "));
						pw.print(StringUtils.leftPad(tStdDev, 9, " "));
						pw.print(StringUtils.leftPad(tMin, 9, " "));
						pw.print(StringUtils.leftPad(tMax, 9, " "));
						pw.print(StringUtils.leftPad(tP90, 9, " "));
						pw.print(StringUtils.leftPad(tAttempts, 10, " "));
						pw.print(StringUtils.leftPad(tCount, 9, " "));
						pw.print(StringUtils.leftPad(tWCount, 10, " "));
						pw.print(StringUtils.leftPad(tECount, 8, " "));
						pw.println(StringUtils.leftPad(tRCount, 11, " "));
					});

			pw.printf(FMT, "%nRun time: %.2f sec.%n", elapsedSec);
			pw.printf("Clients: %d%n", metrics.getNumClients());
			pw.printf("Total Trade-Result (Completed): %d%n", tradeResultStat != null ? tradeResultStat.getCount() : 0);
			pw.printf(FMT, "TPC-E Metric (tpsE): %.2f tpsE%n", tpsE);
			pw.printf("Total Tx (attempted): %d%n", totalAttempts);
			pw.printf("Total Tx (successful): %d%n", totalSuccess);
			pw.printf(FMT, "Total System Transaction Rate: %.2f tx/sec%n", ReportMetrics.transactionRate(totalAttempts, elapsedSec));

		}
		catch (Throwable tw)
		{
			throw new RuntimeException(tw);
		}
		finally
		{
			pw.flush();
		}
	}
}