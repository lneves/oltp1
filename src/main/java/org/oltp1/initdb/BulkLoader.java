package org.oltp1.initdb;

import java.nio.file.Path;

public abstract class BulkLoader
{
	protected final Path dataDir;

	public BulkLoader(Path dataDir)
	{
		this.dataDir = dataDir;
	}

	public abstract void loadAllTables() throws Exception;

	protected Path getDataFile(String fileName)
	{
		return dataDir.resolve(fileName);
	}

	// Standard table loading order based on dependencies
	protected static final String[] TABLE_LOAD_ORDER = {
			// Reference data / Fixed tables
			"Charge.txt",
			"CommissionRate.txt",
			"Exchange.txt",
			"Industry.txt",
			"Sector.txt",
			"StatusType.txt",
			"TaxRate.txt",
			"TradeType.txt",
			"ZipCode.txt",

			// Core entities // Scaling tables
			"AccountPermission.txt",
			"Address.txt",
			"Broker.txt",
			"Company.txt",
			"CompanyCompetitor.txt",
			"Customer.txt",
			"CustomerAccount.txt",
			"CustomerTaxrate.txt",
			"DailyMarket.txt",
			"Financial.txt",
			"LastTrade.txt",
			"NewsItem.txt",
			"NewsXRef.txt",
			"Security.txt",
			"WatchItem.txt",
			"WatchList.txt",

			// Transaction data / Growing tables
			"CashTransaction.txt",
			"Holding.txt",
			"HoldingHistory.txt",
			"HoldingSummary.txt",
			"Settlement.txt",
			"Trade.txt",
			"TradeHistory.txt"
	};

	protected String getTableName(String fileName)
	{
		// Convert file names to table names
		// Remove .txt extension and convert to snake_case
		String tableName = fileName.replace(".txt", "").toLowerCase();

		// Handle special cases
		switch (tableName)
		{
		case "customeraccount":
			return "customer_account";
		case "customertaxrate":
			return "customer_taxrate";
		case "companycompetitor":
			return "company_competitor";
		case "dailymarket":
			return "daily_market";
		case "lasttrade":
			return "last_trade";
		case "newsitem":
			return "news_item";
		case "newsxref":
			return "news_xref";
		case "watchitem":
			return "watch_item";
		case "watchlist":
			return "watch_list";
		case "cashtransaction":
			return "cash_transaction";
		case "holdinghistory":
			return "holding_history";
		case "holdingsummary":
			return "holding_summary";
		case "tradehistory":
			return "trade_history";
		case "commissionrate":
			return "commission_rate";
		case "statustype":
			return "status_type";
		case "tradetype":
			return "trade_type";
		case "accountpermission":
			return "account_permission";
		case "zipcode":
			return "zip_code";
		default:
			return tableName;
		}
	}
}