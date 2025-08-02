package org.caudexorigo.perf;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.function.Consumer;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

public class HtmlReportWriter implements Consumer<TxRunSummary>
{
	private final String baseDir;
	private String reportName;

	public HtmlReportWriter(String baseDir, String reportName)
	{
		super();
		this.baseDir = baseDir;
		this.reportName = RegExUtils.removePattern(StringUtils.trimToEmpty(reportName), "\\s*|\\W*");
	}

	@Override
	public void accept(final TxRunSummary trun)
	{
		String sdate = LocalDateTime.now().toString();
		String fdate = "last";// StringUtils.removePattern(sdate, "\\W");
		String fname = Paths.get(baseDir, String.format("report-%s-%s.html", reportName, fdate)).toAbsolutePath().normalize().toString();

		final long globalMinTs = trun.getTxStats().stream().mapToLong(t -> t.getMinTs()).min().getAsLong();
		final long globalMaxTs = trun.getTxStats().stream().mapToLong(t -> t.getMaxTs()).max().getAsLong();
		final double globalElapsed = ((double) (globalMaxTs - globalMinTs) / 1000000.0);
		final long totalTx = trun.getTxStats().stream().mapToLong(t -> t.getCount()).sum();

		try (PrintWriter pw = new PrintWriter(new FileWriter(fname)))
		{
			pw.println("<!DOCTYPE html>");
			pw.println("<html>");
			pw.println("<head>");
			pw.println("<meta charset='utf-8' />");
			pw.println("<meta content='text/html; charset=utf-8' http-equiv='Content-Type' />");
			pw.println("<meta content='width=device-width, initial-scale=1' name='viewport' />");
			pw.printf("<title>Test run report - %s</title>%n", reportName);
			pw.println("<style>");
			pw.println("html{font-size:medium;}");
			pw.println("body{background-color:#fffff6;color:#330;font-family:Arial,Helvetica,sans-serif;font-size:1rem;line-height:1.5; width:100%; margin:0; padding:0}");
			pw.println("table { width:auto; margin-left:auto; margin-right:auto; border-spacing:0; border:1px dashed gray; border-collapse:collapse;}");
			pw.println("thead,tbody, tfoot,tr,th,td {border-bottom:1px dashed gray; border-collapse: collapse;}");
			pw.println("th,td {vertical-align:baseline; margin:0; padding:0.75em;}");
			pw.println("caption p {font-weight: bold;margin-top:1rem;}");
			pw.println(".label {font-weight: bold; margin-right: 0.5rem;text-align:left;word-break: keep-all;white-space: nowrap;}");
			pw.println(".dimension {text-align:left;word-break: keep-all;white-space: nowrap;}");
			pw.println(".dimension {text-align:left;}");
			pw.println(".metric {text-align:right;}");
			pw.println("th.dimension, th.metric {border-top: 1px dashed gray; border-collapse: collapse;}");
			pw.println(".frame-left {border-left:1px dashed gray;}");
			pw.println(".frame-right {border-right:1px dashed gray;}");
			pw.println(".footer {}");
			pw.println(".footer p {margin:0;}");
			pw.println("</style>");
			pw.println("</head>");
			pw.println("<body>");
			pw.println("<table>");
			pw.println("<caption>");
			pw.printf("<p>%s</p>%n", trun.getSutInfo());
			pw.printf("<p>%s</p>%n", sdate);
			pw.println("</caption>");
			pw.println("<thead>");
			pw.println("<tr>");
			pw.println("<th colspan='3' style='border-left:1px solid transparent; border-top:1px solid transparent;'>&nbsp;</th>");
			pw.println("<th colspan='5' style='border-left:1px dashed gray; border-right:1px dashed gray; text-align:center;'>Response Time (ms)</th>");
			pw.println("<th colspan='3' style='border-right:1px solid transparent; border-top:1px solid transparent;'>&nbsp;</th>");
			pw.println("</tr>");
			pw.println("<tr>");
			pw.println("<th class='dimension'>Transaction</th>");
			pw.println("<th class='metric'>Target(%)</th>");
			pw.println("<th class='metric'>Actual(%)</th>");
			pw.println("<th class='metric frame-left'>Mean</th>");
			pw.println("<th class='metric'>StdDev</th>");
			pw.println("<th class='metric'>Min</th>");
			pw.println("<th class='metric'>Max</th>");
			pw.println("<th class='metric frame-right'>Pct90</th>");
			pw.println("<th class='metric'>Count</th>");
			pw.println("<th class='metric'>Errors</th>");
			pw.println("<th class='metric'>Warnings</th>");
			pw.println("<th class='metric'>Rate(tx/sec)</th>");
			pw.println("</tr>");
			pw.println("<tbody>");

			trun.getTxStats().stream().forEach(test -> {

				pw.println("<tr>");
				pw.printf("<td class='dimension'>%s</td>%n", test.getTxName());
				pw.printf("<td class='metric'>%.2f</td>%n", test.getTargetMixPct() * 100.0);
				pw.printf("<td class='metric'>%.2f</td>%n", test.getActualMixPct(totalTx) * 100.0);
				pw.printf("<td class='metric frame-left'>%.2f</td>%n", test.getMean());
				pw.printf("<td class='metric'>%.2f</td>%n", test.getStdDev());
				pw.printf("<td class='metric'>%.2f</td>%n", test.getMin());
				pw.printf("<td class='metric'>%.2f</td>%n", test.getMax());
				pw.printf("<td class='metric frame-right'>%.2f</td>%n", test.getQuantile(0.9));
				pw.printf("<td class='metric'>%d</td>%n", test.getCount());
				pw.printf("<td class='metric'>%d</td>%n", test.getErroCount());
				pw.printf("<td class='metric'>%d</td>%n", test.getWarningCount());
				pw.printf("<td class='metric'>%.2f</td>%n", test.getCount() / (globalElapsed / 1000.0));

				pw.println("</tr>");
			});

			pw.println("</tbody>");
			pw.println("<tfoot>");
			pw.println("<tr>");
			pw.println("<td colspan='11' class='footer'>");
			pw.printf("<p><span class='label'>Clients:</span><span>%s</span></p>%n", trun.getNumClients());
			pw.printf("<p><span class='label'>Total Tx:</span><span>%d</span></p>%n", totalTx);
			pw.printf("<p><span class='label'>Tx Rate:</span><span>%.2f tx/sec</span></p>%n", ((double) totalTx) / (globalElapsed / 1000.0));
			pw.println("</td>");
			pw.println("</tr>");
			pw.println("</tfoot>");
			pw.println("</table>");
			pw.println("</body>");
			pw.println("</html>");

			pw.flush();
		}
		catch (Throwable tw)
		{
			throw new RuntimeException(tw);
		}
	}
}