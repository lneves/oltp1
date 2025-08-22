package org.oltp1.egen;

import java.io.File;
import java.io.IOException;

import org.oltp1.egen.generator.AddressTable;
import org.oltp1.egen.generator.ChargeTable;
import org.oltp1.egen.generator.CommissionRateTable;
import org.oltp1.egen.generator.CompanyCompetitorTable;
import org.oltp1.egen.generator.CompanyTable;
import org.oltp1.egen.generator.CustomerAccountsAndPermissionsTable;
import org.oltp1.egen.generator.CustomerTable;
import org.oltp1.egen.generator.CustomerTaxRateTable;
import org.oltp1.egen.generator.DailyMarketTable;
import org.oltp1.egen.generator.ExchangeTable;
import org.oltp1.egen.generator.FinancialTable;
import org.oltp1.egen.generator.IndustryTable;
import org.oltp1.egen.generator.LastTradeTable;
import org.oltp1.egen.generator.NewsItemAndXrefTable;
import org.oltp1.egen.generator.SectorTable;
import org.oltp1.egen.generator.SecurityTable;
import org.oltp1.egen.generator.StatusTypeTable;
import org.oltp1.egen.generator.TableGenerator;
import org.oltp1.egen.generator.TaxrateTable;
import org.oltp1.egen.generator.TradeGen;
import org.oltp1.egen.generator.TradeTypeTable;
import org.oltp1.egen.generator.WatchListsAndItemsTable;
import org.oltp1.egen.generator.ZipCodeTable;
import org.oltp1.egen.io.AppendableRow;
import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.io.FastFlatFileWriter;
import org.oltp1.egen.io.FlatFileWriter;
import org.oltp1.egen.model.CustomerTaxRateRow;
import org.oltp1.egen.model.TradeRow;
import org.oltp1.egen.model.WatchItemRow;
import org.oltp1.egen.model.WatchListRow;

/**
 * Orchestrates the entire data generation and loading process. This class
 * initializes all table generators and calls them in the correct order to
 * produce a consistent TPC-E database flat file set. Based on
 * inc/EGenGenerateAndLoad.h and src/EGenGenerateAndLoad.cpp
 */
public class GenerateAndLoad
{
	// Configuration parameters
	private final DataFileManager dfm;
	private final long customerCount;
	private final long startFromCustomer;
	private final long totalCustomers;
	private final int loadUnitSize;
	private final int scaleFactor;
	private final int hoursOfInitialTrades;
	private final String outDir;

	// Constants from C++ version for progress updates
	private static final int STATUS_UPDATE_INTERVAL_FAST = 20000;
	private static final int STATUS_UPDATE_INTERVAL_SLOW = 1000;
	private int daysOfInitialTrades;

	public GenerateAndLoad(DataFileManager dfm, long customerCount, long startFromCustomer,
			long totalCustomers, int loadUnitSize, int scaleFactor,
			int daysOfInitialTrades, String outDir)
	{
		this.dfm = dfm;
		this.customerCount = customerCount;
		this.startFromCustomer = startFromCustomer;
		this.totalCustomers = totalCustomers;
		this.loadUnitSize = loadUnitSize;
		this.scaleFactor = scaleFactor;
		this.daysOfInitialTrades = daysOfInitialTrades;
		this.hoursOfInitialTrades = daysOfInitialTrades * 8; // Convert days to hours
		this.outDir = outDir;

		// Ensure the output directory exists
		new File(outDir).mkdirs();
	}

	/**
	 * Generates and loads all tables that are constant in size (Fixed Tables).
	 */
	public void generateAndLoadFixedTables() throws IOException
	{
		System.out.println("\n--- Generating Fixed Tables ---\n");
		generateAndLoadCharge();
		generateAndLoadCommissionRate();
		generateAndLoadExchange();
		generateAndLoadIndustry();
		generateAndLoadSector();
		generateAndLoadStatusType();
		generateAndLoadTaxrate();
		generateAndLoadTradeType();
		generateAndLoadZipCode();
	}

	/**
	 * Generates and loads all tables that scale with the number of customers.
	 */
	public void generateAndLoadScalingTables() throws IOException
	{
		System.out.println("\n--- Generating Scaling Tables ---\n");

		// Customer-related tables
		generateAndLoadAddress();
		generateAndLoadCustomer();
		generateAndLoadCustomerAccountAndAccountPermission();
		generateAndLoadCustomerTaxrate();
		generateAndLoadWatchListAndWatchItem();

		// Now security/company related tables
		generateAndLoadCompany();
		generateAndLoadCompanyCompetitor();
		generateAndLoadDailyMarket();
		generateAndLoadFinancial();
		generateAndLoadLastTrade();
		generateAndLoadNewsItemAndNewsXRef();
		generateAndLoadSecurity();
	}

	/**
	 * Generates and loads all tables related to trading activity (Growing Tables).
	 */
	public void generateAndLoadGrowingTables() throws IOException
	{
		System.out.println("\n--- Generating Growing Tables ---\n");
		generateAndLoadHoldingAndTrade();
	}

	/*
	 * ============================================================================
	 * FIXED TABLES
	 * ============================================================================
	 */

	// A template method to handle the simple fixed-data tables
	private <T> void generateAndLoadSimpleFixedTable(String tableName, String fileName, TableGenerator<T> generator) throws IOException
	{
		System.out.printf("Generating %s table...%n", tableName.toUpperCase());
		try (FlatFileWriter writer = new FlatFileWriter(outDir + File.separator + fileName);)
		{
			while (generator.hasMoreRecords())
			{
				writer.writeRecord(generator.generateNextRecord());
			}
		}
		System.out.printf("%s done", fileName);
	}

	private void generateAndLoadCharge() throws IOException
	{
		ChargeTable generator = new ChargeTable(dfm);
		generateAndLoadSimpleFixedTable("charge", "Charge.txt", generator);
	}

	private void generateAndLoadCommissionRate() throws IOException
	{
		CommissionRateTable generator = new CommissionRateTable(dfm);
		generateAndLoadSimpleFixedTable("commission_rate", "CommissionRate.txt", generator);
	}

	private void generateAndLoadExchange() throws IOException
	{
		ExchangeTable generator = new ExchangeTable(dfm);
		generateAndLoadSimpleFixedTable("exchange", "Exchange.txt", generator);
	}

	public void generateAndLoadIndustry() throws IOException
	{
		IndustryTable generator = new IndustryTable(dfm);
		generateAndLoadSimpleFixedTable("industry", "Industry.txt", generator);
	}

	public void generateAndLoadSector() throws IOException
	{
		SectorTable generator = new SectorTable(dfm);
		generateAndLoadSimpleFixedTable("sector", "Sector.txt", generator);
	}

	private void generateAndLoadStatusType() throws IOException
	{
		StatusTypeTable generator = new StatusTypeTable(dfm);
		generateAndLoadSimpleFixedTable("status_type", "StatusType.txt", generator);
	}

	private void generateAndLoadTaxrate() throws IOException
	{
		TaxrateTable generator = new TaxrateTable(dfm);
		generateAndLoadSimpleFixedTable("taxrate", "Taxrate.txt", generator);
	}

	private void generateAndLoadTradeType() throws IOException
	{
		TradeTypeTable generator = new TradeTypeTable(dfm);
		generateAndLoadSimpleFixedTable("trade_type", "TradeType.txt", generator);
	}

	private void generateAndLoadZipCode() throws IOException
	{
		ZipCodeTable generator = new ZipCodeTable(dfm);
		generateAndLoadSimpleFixedTable("zip_code", "ZipCode.txt", generator);
	}

	/*
	 * ============================================================================
	 * SCALING TABLES
	 * ============================================================================
	 */

	private <T> void generateAndLoadScalingTable(String tableName, String fileName, TableGenerator<T> generator) throws IOException
	{
		System.out.printf("Generating %s table...%n", tableName.toUpperCase());

		try (
				FlatFileWriter writer = new FlatFileWriter(outDir + File.separator + fileName);
				Spinner spinner = new Spinner(fileName, STATUS_UPDATE_INTERVAL_FAST);)
		{
			while (generator.hasMoreRecords())
			{
				writer.writeRecord(generator.generateNextRecord());

				spinner.step();
			}
		}
	}
	
	private <T extends AppendableRow> void generateAndLoadScalingTableFast(String tableName, String fileName, TableGenerator<T> generator) throws IOException
	{
		System.out.printf("Generating %s table...%n", tableName.toUpperCase());

		try (
				FastFlatFileWriter writer = new FastFlatFileWriter(outDir + File.separator + fileName);
				Spinner spinner = new Spinner(fileName, STATUS_UPDATE_INTERVAL_FAST);)
		{
			while (generator.hasMoreRecords())
			{
				writer.writeRecord(generator.generateNextRecord());

				spinner.step();
			}
		}
	}

	private void generateAndLoadCustomer() throws IOException
	{
		CustomerTable generator = new CustomerTable(dfm, customerCount, startFromCustomer);

		generateAndLoadScalingTable("customer", "Customer.txt", generator);
	}

	private void generateAndLoadAddress() throws IOException
	{
		boolean generateOnlyCustomerAddresses = (startFromCustomer != 1);
		AddressTable generator = new AddressTable(dfm, customerCount, startFromCustomer, generateOnlyCustomerAddresses);
		generateAndLoadScalingTable("address", "Address.txt", generator);
	}

	private void generateAndLoadCustomerAccountAndAccountPermission() throws IOException
	{
		System.out.println("Generating CUSTOMER_ACCOUNT and ACCOUNT_PERMISSION tables...");

		try (
				FlatFileWriter caWriter = new FlatFileWriter(outDir + File.separator + "CustomerAccount.txt");
				FlatFileWriter apWriter = new FlatFileWriter(outDir + File.separator + "AccountPermission.txt");
				Spinner spinner = new Spinner("CustomerAccount.txt - AccountPermission.txt", STATUS_UPDATE_INTERVAL_SLOW);)
		{
			CustomerAccountsAndPermissionsTable table = new CustomerAccountsAndPermissionsTable(
					dfm,
					customerCount,
					startFromCustomer);

			while (table.hasMoreRecords())
			{
				table.generateNextRecord();

				caWriter.writeRecord(table.getCARow());

				for (int i = 0; i < table.getCAPermsCount(); i++)
				{
					apWriter.writeRecord(table.getAPRow(i));
				}

				spinner.step();
			}
		}
	}

	public void generateAndLoadCompany() throws IOException
	{
		CompanyTable generator = new CompanyTable(dfm, customerCount, startFromCustomer);
		generateAndLoadScalingTable("company", "Company.txt", generator);
	}

	public void generateAndLoadCompanyCompetitor() throws IOException
	{
		CompanyCompetitorTable generator = new CompanyCompetitorTable(dfm, customerCount, startFromCustomer);
		generateAndLoadScalingTable("company_competitor", "CompanyCompetitor.txt", generator);
	}

	public void generateAndLoadSecurity() throws IOException
	{
		SecurityTable generator = new SecurityTable(dfm, customerCount, startFromCustomer);
		generateAndLoadScalingTable("security", "Security.txt", generator);
	}

	public void generateAndLoadDailyMarket() throws IOException
	{
		DailyMarketTable generator = new DailyMarketTable(dfm, customerCount, startFromCustomer);
		generateAndLoadScalingTableFast("daily_market", "DailyMarket.txt", generator);
	}

	public void generateAndLoadFinancial() throws IOException
	{
		FinancialTable generator = new FinancialTable(dfm, customerCount, startFromCustomer);
		generateAndLoadScalingTableFast("financial", "Financial.txt", generator);
	}

	public void generateAndLoadLastTrade() throws IOException
	{
		LastTradeTable generator = new LastTradeTable(dfm, customerCount, startFromCustomer, daysOfInitialTrades);
		generateAndLoadScalingTable("last_trade", "LastTrade.txt", generator);
	}

	public void generateAndLoadNewsItemAndNewsXRef() throws IOException
	{
		System.out.print("Generating NEWS_ITEM and NEWS_XREF tables...");
		NewsItemAndXrefTable generator = new NewsItemAndXrefTable(dfm, customerCount, startFromCustomer, daysOfInitialTrades);

		try (
				FlatFileWriter niWriter = new FlatFileWriter(outDir + File.separator + "NewsItem.txt");
				FlatFileWriter nxWriter = new FlatFileWriter(outDir + File.separator + "NewsXRef.txt");
				Spinner spinner = new Spinner("NewsItem.txt - NewsXRef.txt", STATUS_UPDATE_INTERVAL_SLOW);)
		{
			while (generator.hasMoreRecords())
			{
				NewsItemAndXrefTable g = generator.generateNextRecord();

				niWriter.writeRecord(g.getNewsItemRow());
				nxWriter.writeRecord(g.getNewsXrefRow());

				spinner.step();
			}
		}
	}

	public void generateAndLoadCustomerTaxrate() throws IOException
	{
		System.out.print("Generating CUSTOMER_TAXRATE table...");
		CustomerTaxRateTable gen = new CustomerTaxRateTable(dfm, customerCount, startFromCustomer);

		String fileName = "CustomerTaxrate.txt";
		try (
				FlatFileWriter writer = new FlatFileWriter(outDir + File.separator + fileName);
				Spinner spinner = new Spinner(fileName, STATUS_UPDATE_INTERVAL_FAST);)
		{
			while (gen.hasMoreRecords())
			{
				CustomerTaxRateRow[] row = gen.generateNextRecord();
				for (CustomerTaxRateRow trRow : row)
				{
					writer.writeRecord(trRow);
				}

				spinner.step();
			}
		}
	}

	public void generateAndLoadWatchListAndWatchItem() throws IOException
	{
		System.out.println("Generating WATCH_LIST and WATCH_ITEM tables...");
		WatchListsAndItemsTable generator = new WatchListsAndItemsTable(dfm, customerCount, startFromCustomer);

		try (
				FlatFileWriter wlWriter = new FlatFileWriter(outDir + File.separator + "WatchList.txt");
				FlatFileWriter wiWriter = new FlatFileWriter(outDir + File.separator + "WatchItem.txt");
				Spinner spinner = new Spinner("WatchList.txt - WatchItem.txt", 500);)
		{
			while (generator.hasMoreRecords())
			{
				generator.generateNextRecord();
				WatchListRow wlrow = generator.getWlRow();

				wlWriter.writeRecord(wlrow);

				for (WatchItemRow item : generator.getWiRows())
				{
					wiWriter.writeRecord(item);
				}

				spinner.step();
			}
		}
	}

	public void generateAndLoadHoldingAndTrade() throws IOException
	{
		// The TradeGen class encapsulates all the logic for simulating trades
		// and generating the 8 interconnected growing tables.
		TradeGen tradeGen = new TradeGen(
				dfm,
				customerCount,
				startFromCustomer,
				totalCustomers,
				loadUnitSize,
				scaleFactor,
				daysOfInitialTrades * 8); // Convert days to hours

		try (
				FastFlatFileWriter tradeWriter = new FastFlatFileWriter(outDir + File.separator + "Trade.txt");
				FastFlatFileWriter tradeHistoryWriter = new FastFlatFileWriter(outDir + File.separator + "TradeHistory.txt");
				FastFlatFileWriter settlementWriter = new FastFlatFileWriter(outDir + File.separator + "Settlement.txt");
				FastFlatFileWriter cashTransactionWriter = new FastFlatFileWriter(outDir + File.separator + "CashTransaction.txt");
				FastFlatFileWriter holdingWriter = new FastFlatFileWriter(outDir + File.separator + "Holding.txt");
				FastFlatFileWriter holdingHistoryWriter = new FastFlatFileWriter(outDir + File.separator + "HoldingHistory.txt");
				FastFlatFileWriter holdingSummaryWriter = new FastFlatFileWriter(outDir + File.separator + "HoldingSummary.txt");
				FastFlatFileWriter brokerWriter = new FastFlatFileWriter(outDir + File.separator + "Broker.txt");
				Spinner tradeSpinner = new Spinner("Trade files", 10000);)
		{
			int currentLoadUnit = 1;
			do
			{
				System.out.printf("Generating trades for load unit %d...%n", currentLoadUnit++);

				// Generate all trades for the current load unit
				boolean hasMoreRecords = true;

				do
				{
					hasMoreRecords = tradeGen.generateNextTrade();
					TradeRow tr = tradeGen.getTradeRow();

					tradeWriter.writeRecord(tr);
					for (int i = 0; i < tradeGen.getTradeHistoryRowCount(); i++)
					{
						tradeHistoryWriter.writeRecord(tradeGen.getTradeHistoryRow(i));
					}

					if (tradeGen.getSettlementRowCount() > 0)
					{
						settlementWriter.writeRecord(tradeGen.getSettlementRow());
					}

					if (tradeGen.getCashTransactionRowCount() > 0)
					{
						cashTransactionWriter.writeRecord(tradeGen.getCashTransactionRow());
					}

					for (int i = 0; i < tradeGen.getHoldingHistoryRowCount(); i++)
					{
						holdingHistoryWriter.writeRecord(tradeGen.getHoldingHistoryRow(i));
					}

					tradeSpinner.step();
				}
				while (hasMoreRecords);

				System.out.print("\nGenerating holdings and brokers for load unit...");

				// Generate brokers for the load unit
				do
				{
					hasMoreRecords = tradeGen.generateNextBrokerRecord();
					brokerWriter.writeRecord(tradeGen.getBrokerRow());
				}
				while (hasMoreRecords);

				// Generate holding summaries for the load unit
				do
				{
					hasMoreRecords = tradeGen.generateNextHoldingSummaryRow();
					holdingSummaryWriter.writeRecord(tradeGen.getHoldingSummaryRow());
				}
				while (hasMoreRecords);

				// Generate the final holdings for the load unit
				do
				{
					hasMoreRecords = tradeGen.generateNextHolding();
					holdingWriter.writeRecord(tradeGen.getHoldingRow());
				}
				while (hasMoreRecords);
				System.out.println(" done.");

			}
			while (tradeGen.initNextLoadUnit());
		}
		System.out.println("Growing tables loaded.");
	}
}