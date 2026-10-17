package org.oltp1.egen.generator;

import java.util.ArrayList;
import java.util.List;

import org.oltp1.egen.io.BucketedDataFile;
import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.io.records.TaxRateCountryDataFileRecord;
import org.oltp1.egen.io.records.TaxRateDivisionDataFileRecord;

/**
 * A Java port of the CTaxRateFile class. This class provides a unified, ordered
 * view of tax rate records from both the country and division files. Based on
 * InputFiles/inc/TaxRateFile.h and its .cpp file.
 */
public class TaxRateFile
{
	private final List<Object> unifiedTaxRecords = new ArrayList<>();

	public TaxRateFile(DataFileManager dfm)
	{
		BucketedDataFile<TaxRateCountryDataFileRecord> countryFile = dfm.getTaxRateCountryDataFile();
		BucketedDataFile<TaxRateDivisionDataFileRecord> divisionFile = dfm.getTaxRateDivisionDataFile();

		// Add all country records to the unified list
		for (int i = 1; i <= countryFile.getBucketCount(); i++)
		{
			unifiedTaxRecords.addAll(countryFile.getBucket(i));
		}

		// Add all division records to the unified list
		for (int i = 1; i <= divisionFile.getBucketCount(); i++)
		{
			unifiedTaxRecords.addAll(divisionFile.getBucket(i));
		}
	}

	/**
	 * Gets a tax record from the unified list by its index.
	 * 
	 * @param index
	 *            The 0-based index.
	 * @return The tax record object (either a TaxRateCountry or TaxRateDivision
	 *         record).
	 */
	public Object getRecord(int index)
	{
		return unifiedTaxRecords.get(index);
	}

	/**
	 * Gets the total number of tax records from both files.
	 * 
	 * @return The total size of the unified list.
	 */
	public int size()
	{
		return unifiedTaxRecords.size();
	}
}