package org.oltp1.egen.generator;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.model.CompanyCompetitorRow;

/**
 * Generates data for the COMPANY_COMPETITOR table. This class is a direct port
 * of the C++ CCompanyCompetitorTable class. It creates relationships between
 * companies based on the input files and scales the number of relationships
 * with the customer count. Based on inc/CompanyCompetitorTable.h and its logic.
 */
public class CompanyCompetitorTable implements TableGenerator<CompanyCompetitorRow>
{
	private final DataFileManager dfm;
	private final CompanyCompetitorFile companyCompetitorFile;

	private final long competitorCount;
	private final long startFromCompetitor;
	private long lastRowNumber; // 0-based index for this instance's generation
	private boolean hasMoreRecords;

	public CompanyCompetitorTable(DataFileManager dfm, long customerCount, long startFromCustomer)
	{
		this.dfm = dfm;
		// The CompanyCompetitorFile abstraction is needed to handle scaling logic.
		this.companyCompetitorFile = new CompanyCompetitorFile(dfm);

		this.competitorCount = this.companyCompetitorFile.getCompetitorCountForCustomers(customerCount);
		this.startFromCompetitor = this.companyCompetitorFile.getStartFromCompetitor(startFromCustomer);

		this.lastRowNumber = 0;
		this.hasMoreRecords = this.competitorCount > 0;
	}

	@Override
	public boolean hasMoreRecords()
	{
		if (lastRowNumber >= competitorCount)
		{
			hasMoreRecords = false;
		}
		return hasMoreRecords;
	}

	@Override
	public CompanyCompetitorRow generateNextRecord()
	{
		long currentCompetitorIndex = startFromCompetitor + lastRowNumber;

		CompanyCompetitorRow row = new CompanyCompetitorRow();

		// The CompanyCompetitorFile handles all the complex logic for getting
		// the correct, scaled IDs and industry information.
		row.CP_CO_ID = companyCompetitorFile.getCompanyId(currentCompetitorIndex);
		row.CP_COMP_CO_ID = companyCompetitorFile.getCompanyCompetitorId(currentCompetitorIndex);
		row.CP_IN_ID = companyCompetitorFile.getIndustryId(currentCompetitorIndex);

		lastRowNumber++;

		return row;
	}
}