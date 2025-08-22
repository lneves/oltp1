package org.oltp1.runner.perf;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

public class ConsoleReportWriter implements Consumer<TxRunSummary>
{
	@Override
	public void accept(final TxRunSummary trun)
	{
		final long globalMinTs = trun.getTxStats().stream().mapToLong(t -> t.getMinTs()).min().getAsLong();
		final long globalMaxTs = trun.getTxStats().stream().mapToLong(t -> t.getMaxTs()).max().getAsLong();
		final double globalElapsed = ((double) (globalMaxTs - globalMinTs) / 1000000.0);
		final long totalTx = trun.getTxStats().stream().mapToLong(t -> t.getCount()).sum();

		try (PrintWriter pw = new PrintWriter(System.out))
		{
			pw.write("\n");
			pw.printf("#SUT%n%n%s%n%n", trun.getSutInfo());
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
			pw.print(StringUtils.leftPad("Count", 10, " "));
			pw.print(StringUtils.leftPad("Warnings", 10, " "));
			pw.print(StringUtils.leftPad("Errors", 8, " "));
			pw.println(StringUtils.leftPad("Rollbacks", 11, " "));

			trun.getTxStats().stream().forEach(test -> {

				String tTargetMixPct = String.format("%.2f", test.getTargetMixPct() * 100.0);
				String tActualMixPct = String.format("%.2f", test.getActualMixPct(totalTx) * 100.0);
				String tRate = String.format("%.2f", test.getCount() / (globalElapsed / 1000.0));
				String tMean = String.format("%.2f", test.getMean());
				String tStdDev = String.format("%.2f", test.getStdDev());
				String tMin = String.format("%.2f", test.getMin());
				String tMax = String.format("%.2f", test.getMax());
				String tP90 = String.format("%.2f", test.getQuantile(0.9));
				String tCount = String.format("%s", test.getCount());
				String tWCount = String.format("%s", test.getWarningCount());
				String tECount = String.format("%s", test.getErroCount());
				String tRCount = String.format("%s", test.getRollbackCount());

				pw.print(StringUtils.rightPad(test.getTxName(), 20, " "));
				pw.print(StringUtils.leftPad(tTargetMixPct, 10, " "));
				pw.print(StringUtils.leftPad(tActualMixPct, 10, " "));
				pw.print(StringUtils.leftPad(tRate, 14, " "));
				pw.print(StringUtils.leftPad(tMean, 9, " "));
				pw.print(StringUtils.leftPad(tStdDev, 9, " "));
				pw.print(StringUtils.leftPad(tMin, 9, " "));
				pw.print(StringUtils.leftPad(tMax, 9, " "));
				pw.print(StringUtils.leftPad(tP90, 9, " "));
				pw.print(StringUtils.leftPad(tCount, 10, " "));
				pw.print(StringUtils.leftPad(tWCount, 10, " "));
				pw.print(StringUtils.leftPad(tECount, 8, " "));
				pw.println(StringUtils.leftPad(tRCount, 11, " "));

			});

			pw.printf("%nRun time: %.0f sec.%n", globalElapsed / 1000);
			pw.printf("Clients: %d%n", trun.getNumClients());
			pw.printf("Total Tx: %d%n", totalTx);
			pw.printf("Tx Rate: %.2f tx/sec%n", ((double) totalTx) / (globalElapsed / 1000.0));

			pw.flush();
		}
		catch (Throwable tw)
		{
			throw new RuntimeException(tw);
		}
	}
}