package org.oltp1.egen.generator;

import java.util.List;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.io.records.CompanyCompetitorDataFileRecord;

/**
 * A Java port of the CCompanyCompetitorFile class. This class is a higher-level
 * abstraction over the raw CompanyCompetitor.txt data file. It understands the
 * TPC-E rule that the number of competitor relationships scales with the total
 * number of customers in the database. Based on
 * InputFiles/inc/CompanyCompetitorFile.h and its .cpp file.
 */
public class CompanyCompetitorFile
{

	// Constants from MiscConsts.h
	private static final int DEFAULT_LOAD_UNIT_SIZE = 1000;
	private static final int ONE_LOAD_UNIT_COMPANY_COMPETITOR_COUNT = 1500; // 3 * 500
	private static final long IDENT_T_SHIFT = 4300000000L;

	private final List<CompanyCompetitorDataFileRecord> dataFile;
	private final CompanyFile companyFile; // Dependency for base company count

	public CompanyCompetitorFile(DataFileManager dfm)
	{
		this.dataFile = dfm.getCompanyCompetitorDataFile();
		this.companyFile = dfm.getCompanyFile();
	}

	/**
	 * Calculates the total number of competitor relationships for a given number of
	 * customers. The rule is 1500 competitor rows for every 1000 customers.
	 * 
	 * @param customerCount
	 *            The total number of customers.
	 * @return The scaled total number of competitor rows.
	 */
	public long getCompetitorCountForCustomers(long customerCount)
	{
		return (customerCount / DEFAULT_LOAD_UNIT_SIZE) * ONE_LOAD_UNIT_COMPANY_COMPETITOR_COUNT;
	}

	/**
	 * Calculates the starting competitor index (0-based) for a given starting
	 * customer.
	 * 
	 * @param startFromCustomer
	 *            The 1-based starting customer ID of a partition.
	 * @return The 0-based starting competitor index for that partition.
	 */
	public long getStartFromCompetitor(long startFromCustomer)
	{
		return ((startFromCustomer - 1) / DEFAULT_LOAD_UNIT_SIZE) * ONE_LOAD_UNIT_COMPANY_COMPETITOR_COUNT;
	}

	/**
	 * Gets the underlying data file record for a given global competitor index.
	 */
	private CompanyCompetitorDataFileRecord getRecord(long index)
	{
		return dataFile.get((int) (index % dataFile.size()));
	}

	/**
	 * Calculates the scaled company ID for a competitor relationship.
	 * 
	 * @param index
	 *            The 0-based global index of the competitor relationship.
	 * @return The unique, shifted company ID.
	 */
	public long getCompanyId(long index)
	{
		long baseCompanyId = getRecord(index).cp_co_id;
		long multiplier = index / dataFile.size();
		return baseCompanyId + IDENT_T_SHIFT + (multiplier * companyFile.getBaseCompanyCount());
	}

	/**
	 * Calculates the scaled competitor's company ID.
	 * 
	 * @param index
	 *            The 0-based global index of the competitor relationship.
	 * @return The unique, shifted competitor company ID.
	 */
	public long getCompanyCompetitorId(long index)
	{
		long baseCompetitorId = getRecord(index).cp_comp_co_id;
		long multiplier = index / dataFile.size();
		return baseCompetitorId + IDENT_T_SHIFT + (multiplier * companyFile.getBaseCompanyCount());
	}

	/**
	 * Gets the industry ID for a competitor relationship.
	 * 
	 * @param index
	 *            The 0-based global index of the competitor relationship.
	 * @return The industry ID string.
	 */
	public String getIndustryId(long index)
	{
		return getRecord(index).cp_in_id;
	}
}