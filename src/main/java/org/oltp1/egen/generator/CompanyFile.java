package org.oltp1.egen.generator;

import java.util.List;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.io.records.CompanyDataFileRecord;

/**
 * A Java port of the CCompanyFile class. This class is a higher-level
 * abstraction over the raw Company.txt data file. It understands the TPC-E rule
 * that the number of companies scales with the total number of customers in the
 * database, and provides methods to access
 * 
 * this scaled data deterministically. Based on InputFiles/inc/CompanyFile.h and
 * InputFiles/src/CompanyFile.cpp
 */
public class CompanyFile
{

	// Constants from MiscConsts.h
	private static final int DEFAULT_LOAD_UNIT_SIZE = 1000;
	private static final int ONE_LOAD_UNIT_COMPANY_COUNT = 500;
	private static final long IDENT_T_SHIFT = 4300000000L;

	private final List<CompanyDataFileRecord> dataFile;
	private final long configuredCompanyCount;
	private final long activeCompanyCount;

	public CompanyFile(DataFileManager dfm)
	{
		this.dataFile = dfm.getCompanyDataFile();
		this.configuredCompanyCount = calculateCompanyCount(dfm.getConfiguredCustomers());
		this.activeCompanyCount = calculateCompanyCount(dfm.getActiveCustomers());
	}

	/**
	 * Calculates the total number of companies for a given number of customers. The
	 * rule is 500 companies for every 1000 customers.
	 * 
	 * @param customerCount
	 *            The total number of customers.
	 * @return The scaled total number of companies.
	 */
	public long calculateCompanyCount(long customerCount)
	{
		return (customerCount / DEFAULT_LOAD_UNIT_SIZE) * ONE_LOAD_UNIT_COMPANY_COUNT;
	}

	/**
	 * Calculates the starting company index (0-based) for a given starting
	 * customer.
	 * 
	 * @param startFromCustomer
	 *            The 1-based starting customer ID of a partition.
	 * @return The 0-based starting company index for that partition.
	 */
	public long calculateStartFromCompany(long startFromCustomer)
	{
		// The -1 converts the 1-based customer ID to a 0-based index for the
		// calculation.
		return ((startFromCustomer - 1) / DEFAULT_LOAD_UNIT_SIZE) * ONE_LOAD_UNIT_COMPANY_COUNT;
	}

	/**
	 * Gets the total number of companies that will exist in the fully generated
	 * database.
	 * 
	 * @return The configured company count.
	 */
	public long getConfiguredCompanyCount()
	{
		return configuredCompanyCount;
	}

	/**
	 * Gets the number of companies for the active customer set.
	 * 
	 * @return The active company count.
	 */
	public long getActiveCompanyCount()
	{
		return activeCompanyCount;
	}

	/**
	 * Calculates the globally unique, scaled company ID for a given index.
	 * 
	 * @param index
	 *            The 0-based global index of the company.
	 * @return The unique, shifted company ID.
	 */
	public long getCompanyId(long index)
	{
		int fileIndex = (int) (index % dataFile.size());
		long multiplier = index / dataFile.size();

		// The ID is the base ID from the file + the 64-bit shift + a scaled offset.
		return dataFile.get(fileIndex).co_id + IDENT_T_SHIFT + (multiplier * dataFile.size());
	}

	/**
	 * Gets the underlying data file record for a given global company index. This
	 * method correctly wraps around the base data file.
	 * 
	 * @param index
	 *            The 0-based global index of the company.
	 * @return The corresponding CompanyDataFileRecord.
	 */
	public CompanyDataFileRecord getRecord(long index)
	{
		return dataFile.get((int) (index % dataFile.size()));
	}

	/**
	 * Creates a scaled company name. For companies beyond the base file size, it
	 * appends a " #N" suffix to the base name.
	 * 
	 * @param index
	 *            The 0-based global index of the company.
	 * @return The unique, potentially suffixed company name.
	 */
	public String createName(long index)
	{
		int fileIndex = (int) (index % dataFile.size());
		long suffixNum = index / dataFile.size();

		String baseName = dataFile.get(fileIndex).co_name;

		if (suffixNum > 0)
		{
			return String.format("%s #%d", baseName, suffixNum);
		}
		else
		{
			return baseName;
		}
	}

	/**
	 * Gets the number of base companies defined in the Company.txt input file.
	 * 
	 * @return The size of the base company list.
	 */
	public int getBaseCompanyCount()
	{
		return dataFile.size();
	}

	/**
	 * Returns the number of companies generated for a single load unit (1000
	 * customers).
	 */
	public int getCompanyCountForOneLoadUnit()
	{
		return ONE_LOAD_UNIT_COMPANY_COUNT;
	}
}