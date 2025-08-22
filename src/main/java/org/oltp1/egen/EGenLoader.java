package org.oltp1.egen;

import java.io.File;
import java.util.concurrent.Callable;

import org.oltp1.common.ErrorAnalyser;
import org.oltp1.egen.io.DataFileManager;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Main entry point for the EGenLoader Java port. This class is responsible for
 * parsing command-line arguments, initializing the necessary components, and
 * orchestrating the data generation and loading process, mirroring the
 * functionality of the original EGenLoader.cpp.
 */
@Command(name = "egen", mixinStandardHelpOptions = true, description = "Generates data for the TPC-E benchmark.")
public class EGenLoader implements Callable<Integer>
{
	// Default values are set here, matching the C++ application
	@Option(names = "-c", description = "Number of customers for this instance. Must be a multiple of 1000.", defaultValue = "5000")
	private long customerCount;

	@Option(names = "-t", description = "Total number of customers in the database.", defaultValue = "5000")
	private long totalCustomerCount;

	@Option(names = "-b", description = "Beginning customer ordinal position (1-based). Must be N * 1000 + 1.", defaultValue = "1")
	private long startFromCustomer;

	@Option(names = "-f", description = "Scale factor (customers per 1 tpsE).", defaultValue = "500")
	private int scaleFactor;

	@Option(names = "-w", description = "Number of 8-hour Workdays of initial trades to populate.", defaultValue = "300")
	private int daysOfInitialTrades;

	@Option(names = "-o", description = "Directory for output flat files.", required = true)
	private File outDir;


	// Table Generation Flags
	@Option(names = "-x", description = "Generate all tables.")
	private boolean generateAll = false;
	@Option(names = "-xf", description = "Generate all fixed-size tables.")
	private boolean generateFixed = false;
	@Option(names = "-xs", description = "Generate scaling tables.")
	private boolean generateScaling = false;
	@Option(names = "-xg", description = "Generate growing tables and BROKER.")
	private boolean generateGrowing = false;

	@Override
	public Integer call() throws Exception
	{
		System.out.println("EGenLoader Java Port - Starting...");

		if (!validateParameters())
		{
			return 1; // Exit with an error code
		}

		if (!generateFixed && !generateScaling && !generateGrowing)
		{
			generateAll = true;
		}

		printSettings();

		long startTime = System.currentTimeMillis();

		try
		{
			DataFileManager dfm = new DataFileManager(totalCustomerCount, totalCustomerCount);

			GenerateAndLoad generator = new GenerateAndLoad(
					dfm,
					customerCount,
					startFromCustomer,
					totalCustomerCount,
					1000, // Load Unit Size is fixed
					scaleFactor,
					daysOfInitialTrades,
					outDir.getPath());

			if (generateAll || generateFixed)
			{
				if (startFromCustomer == 1)
				{
					generator.generateAndLoadFixedTables();
				}
				else
				{
					System.out
							.println(
									"Skipping fixed tables: only generate for the first customer partition (start customer = 1).");
				}
			}
			if (generateAll || generateScaling)
			{
				generator.generateAndLoadScalingTables();
			}
			if (generateAll || generateGrowing)
			{
				generator.generateAndLoadGrowingTables();
			}
		}
		catch (Throwable t)
		{
			System.err.println("\nFATAL ERROR: An exception occurred during execution.");
			ErrorAnalyser.findRootCause(t).printStackTrace();
			return 1;
		}

		long endTime = System.currentTimeMillis();
		long durationSeconds = (endTime - startTime) / 1000;
		System.out.printf("%nGenerate and load complete. Total time: %d seconds.%n", durationSeconds);

		return 0; // Success
	}

	private boolean validateParameters()
	{
		boolean isValid = true;
		final int loadUnitSize = 1000;

		if ((startFromCustomer % loadUnitSize) != 1)
		{
			System.err
					.printf(
							"ERROR: The specified starting customer (-b %d) must be a non-zero integral multiple of the load unit size (%d) + 1.%n",
							startFromCustomer,
							loadUnitSize);
			isValid = false;
		}

		if (totalCustomerCount < startFromCustomer + customerCount - 1)
		{
			long newTotal = startFromCustomer + customerCount - 1;
			System.err
					.printf(
							"WARNING: Total customer count is less than the range of customers to be generated. Adjusting total to %d.%n",
							newTotal);
			totalCustomerCount = newTotal;
		}

		if (customerCount % loadUnitSize != 0 || customerCount == 0)
		{
			System.err
					.printf(
							"ERROR: The specified customer count (-c %d) must be a non-zero integral multiple of the load unit size (%d).%n",
							customerCount,
							loadUnitSize);
			isValid = false;
		}

		if (totalCustomerCount % loadUnitSize != 0 || totalCustomerCount == 0)
		{
			System.err
					.printf(
							"ERROR: The total customer count (-t %d) must be a non-zero integral multiple of the load unit size (%d).%n",
							totalCustomerCount,
							loadUnitSize);
			isValid = false;
		}

		if (daysOfInitialTrades <= 0)
		{
			System.err
					.printf(
							"ERROR: The specified number of 8-Hour Workdays (-w %d) must be non-zero.%n",
							daysOfInitialTrades);
			isValid = false;
		}

		return isValid;
	}

	private void printSettings()
	{
		System.out.println("\n--- Using the following settings ---");
		System.out.printf("Output Directory: \t%s%n", outDir.getAbsolutePath());
		System.out.printf("Start From Customer: \t%d%n", startFromCustomer);
		System.out.printf("Customer Count: \t%d%n", customerCount);
		System.out.printf("Total Customers: \t%d%n", totalCustomerCount);
		System.out.printf("Scale Factor: \t\t%d%n", scaleFactor);
		System.out.printf("Initial Trade Days: \t%d%n", daysOfInitialTrades);
		System.out.println("------------------------------------\n");
	}

	public static void main(String[] args)
	{
		int exitCode = new CommandLine(new EGenLoader()).execute(args);
		System.exit(exitCode);
	}
}