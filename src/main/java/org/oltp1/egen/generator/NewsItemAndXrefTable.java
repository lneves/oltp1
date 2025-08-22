package org.oltp1.egen.generator;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.model.NewsItemRow;
import org.oltp1.egen.model.NewsXrefRow;
import org.oltp1.egen.util.DateTime;
import org.oltp1.egen.util.TpcRandom;

/**
 * Generates data for the NEWS_ITEM and NEWS_XREF tables. This is a corrected
 * port of the C++ CNewsItemAndXRefTable class, ensuring the correct number of
 * records are generated. Based on inc/NewsItemAndXRefTable.h and its logic.
 */
public class NewsItemAndXrefTable
{
	private static final int NEWS_ITEM_LEN = 100 * 1000;
	private static final int RNG_SKIP_ONE_ROW_NEWS = 4 + NEWS_ITEM_LEN;
	private static final int NEWS_ITEMS_PER_COMPANY = 2;
	private static final long DEFAULT_LOAD_UNIT_SIZE = 1000;
	private static final long RNG_SEED_TABLE_DEFAULT = 37039940L;

	private final DataFileManager dfm;
	private final TpcRandom random;
	private final CompanyFile companyFile;
	private final CompanyTable companyTable;

	private boolean hasMoreRecords;
	private final DateTime newsBaseDate;
	private int itemsGeneratedForCurrentCompany;
	private long lastRowNumber; // Tracks the global 0-based index of the news item

	private NewsItemRow currentNewsItemRow;
	private NewsXrefRow currentNewsXrefRow;
	private long newsCountForOneLoadUnit;

	public NewsItemAndXrefTable(DataFileManager dfm, long customerCount, long startFromCustomer, int daysOfInitialTrades)
	{
		this.dfm = dfm;
		this.random = new TpcRandom(RNG_SEED_TABLE_DEFAULT); // RNG_SEED_TABLE_DEFAULT
		this.companyFile = dfm.getCompanyFile();
		this.companyTable = new CompanyTable(dfm, customerCount, startFromCustomer);

		this.hasMoreRecords = customerCount > 0;

		this.newsBaseDate = new DateTime(2005, 1, 3);
		this.newsBaseDate.add(daysOfInitialTrades, 0, true);

		this.itemsGeneratedForCurrentCompany = 0;
		this.lastRowNumber = NEWS_ITEMS_PER_COMPANY * companyFile.calculateStartFromCompany(startFromCustomer);

		newsCountForOneLoadUnit = companyFile.calculateCompanyCount(DEFAULT_LOAD_UNIT_SIZE) * NEWS_ITEMS_PER_COMPANY;
	}

	public boolean hasMoreRecords()
	{
		return hasMoreRecords;
	}

	public NewsItemAndXrefTable generateNextRecord()
	{

		if (lastRowNumber % newsCountForOneLoadUnit == 0)
		{
			initNextLoadUnit();
		}

		// --- Generate the data for the current record ---
		currentNewsItemRow = new NewsItemRow();
		currentNewsItemRow.NI_ID = lastRowNumber + 1; // row number starts from 0

		generateNewsItemContent(currentNewsItemRow);

		int daysAgo = random.rndIntRange(0, 50);
		int msecAgo = random.rndIntRange(0, 86400000 - 1);
		currentNewsItemRow.NI_DTS = new DateTime(newsBaseDate);
		currentNewsItemRow.NI_DTS.add(-daysAgo, -msecAgo, false);

		currentNewsItemRow.NI_AUTHOR = dfm.getLastNameDataFile().getRecord(random.rndIntRange(0, dfm.getLastNameDataFile().size() - 1)).name;
		currentNewsItemRow.NI_SOURCE = dfm.getLastNameDataFile().getRecord(random.rndIntRange(0, dfm.getLastNameDataFile().size() - 1)).name;

		currentNewsXrefRow = new NewsXrefRow();
		currentNewsXrefRow.NX_NI_ID = currentNewsItemRow.NI_ID;
		currentNewsXrefRow.NX_CO_ID = companyTable.getCurrentCompanyId();

		// --- Update state for the NEXT iteration ---
		itemsGeneratedForCurrentCompany++;
		lastRowNumber++;

		if (itemsGeneratedForCurrentCompany >= NEWS_ITEMS_PER_COMPANY)
		{
			hasMoreRecords = companyTable.generateNextCoId();
			itemsGeneratedForCurrentCompany = 0;
		}
		else
		{
			hasMoreRecords = true;
		}

		return this;
	}

	void initNextLoadUnit()
	{
		random
				.setSeed(
						TpcRandom
								.rndNthElement(
										RNG_SEED_TABLE_DEFAULT,
										lastRowNumber * RNG_SKIP_ONE_ROW_NEWS));

	}

	private void generateNewsItemContent(NewsItemRow row)
	{
		StringBuilder itemSb = new StringBuilder(NEWS_ITEM_LEN + 100);

		while (itemSb.length() < NEWS_ITEM_LEN)
		{
			int threshold = random.rndIntRange(0, dfm.getNewsDataFile().size() - 1);
			itemSb.append(dfm.getNewsDataFile().getRecord(threshold).word);
			itemSb.append(" ");
		}

		row.NI_ITEM = itemSb.substring(0, NEWS_ITEM_LEN);
		row.NI_HEADLINE = row.NI_ITEM.substring(0, 80);
		row.NI_SUMMARY = row.NI_ITEM.substring(0, 255);
	}

	public NewsItemRow getNewsItemRow()
	{
		return currentNewsItemRow;
	}

	public NewsXrefRow getNewsXrefRow()
	{
		return currentNewsXrefRow;
	}
}