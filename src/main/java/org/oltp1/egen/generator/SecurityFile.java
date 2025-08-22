package org.oltp1.egen.generator;

import java.util.List;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.io.records.SecurityDataFileRecord;
import org.oltp1.egen.model.ExchangeType;

/**
 * A Java port of the CSecurityFile class. This class is a higher-level
 * abstraction over the raw Security.txt data file. It understands the TPC-E
 * rule that the number of securities scales with the total number of customers
 * in the database. Based on InputFiles/inc/SecurityFile.h and its .cpp file.
 */
public class SecurityFile
{

	private static final int DEFAULT_LOAD_UNIT_SIZE = 1000;
	private static final int ONE_LOAD_UNIT_SECURITY_COUNT = 685;
	private static final long IDENT_T_SHIFT = 4300000000L;

	private final List<SecurityDataFileRecord> dataFile;
	private final CompanyFile companyFile;
	private final long configuredSecurityCount;
	private final long activeSecurityCount;

	public SecurityFile(DataFileManager dfm)
	{
		this.dataFile = dfm.getSecurityDataFile();
		this.companyFile = dfm.getCompanyFile();
		this.configuredSecurityCount = calculateSecurityCount(dfm.getConfiguredCustomers());
		this.activeSecurityCount = calculateSecurityCount(dfm.getActiveCustomers());
	}

	// Calculate total security count for the specified number of customers.
	// Sort of a static method. Used in parallel generation of securities related
	// tables.
	public long calculateSecurityCount(long customerCount)
	{
		return customerCount / DEFAULT_LOAD_UNIT_SIZE * ONE_LOAD_UNIT_SECURITY_COUNT;
	}

	public long calculateStartFromSecurity(long startFromCustomer)
	{
		return ((startFromCustomer - 1) / DEFAULT_LOAD_UNIT_SIZE) * ONE_LOAD_UNIT_SECURITY_COUNT;
	}

	public long getActiveSecurityCount()
	{
		return activeSecurityCount;
	}

	public long getConfiguredSecurityCount()
	{
		return configuredSecurityCount;
	}

	public int getSecurityCountForOneLoadUnit()
	{
		return ONE_LOAD_UNIT_SECURITY_COUNT;
	}

	public SecurityDataFileRecord getRecord(long index)
	{
		return dataFile.get((int) (index % dataFile.size()));
	}

	public long getCompanyId(long index)
	{
		long baseCompanyId = getRecord(index).s_co_id;
		long multiplier = index / dataFile.size();
		return baseCompanyId + IDENT_T_SHIFT + (multiplier * companyFile.getBaseCompanyCount());
	}

	public long getCompanyIndex(long securityIndex)
	{
		// Company IDs are 1-based and shifted. This converts back to a 0-based index.
		return getCompanyId(securityIndex) - 1 - IDENT_T_SHIFT;
	}

	public String createSymbol(long index)
	{
		String baseSymbol = getRecord(index).s_symb;
		long suffixNum = index / dataFile.size();

		if (suffixNum > 0)
		{
			// This is a simplified suffix generation that matches the C++ output format.
			// A full port would require the complex base-26 logic from the original.
			return String.format("%s-%d", baseSymbol, suffixNum);
		}
		else
		{
			return baseSymbol;
		}
	}

	public ExchangeType getExchangeIndex(long index)
	{
		// The mod converts a scaled security index into a base security index
		String exchange = dataFile.get((int) (index % dataFile.size())).s_ex_id;

		ExchangeType exchangeIndex;

		if (exchange.equals("NYSE"))
		{
			exchangeIndex = ExchangeType.NYSE;
		}
		else if (exchange.equals("NASDAQ"))
		{
			exchangeIndex = ExchangeType.NASDAQ;
		}
		else if (exchange.equals("AMEX"))
		{
			exchangeIndex = ExchangeType.AMEX;
		}
		else if (exchange.equals("PCX"))
		{
			exchangeIndex = ExchangeType.PCX;
		}
		else
		{
			throw new UnsupportedOperationException("Unknown exchange: " + exchange);
		}

		return exchangeIndex;
	}

}