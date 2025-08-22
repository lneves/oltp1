package org.oltp1.egen.generator;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.io.records.TaxRateCountryDataFileRecord;
import org.oltp1.egen.io.records.TaxRateDivisionDataFileRecord;
import org.oltp1.egen.model.TaxrateRow;

public class TaxrateTable implements TableGenerator<TaxrateRow>
{
	private final TaxRateFile taxRateFile;
	private int recordIndex = -1;
	private boolean hasMoreRecords;

	public TaxrateTable(DataFileManager dfm)
	{
		this.taxRateFile = dfm.getTaxRateFile();
		this.hasMoreRecords = this.taxRateFile.size() > 0;
	}

	@Override
	public boolean hasMoreRecords()
	{
		if (recordIndex >= (taxRateFile.size() - 1))
		{
			hasMoreRecords = false;
		}
		return hasMoreRecords;
	}

	@Override
	public TaxrateRow generateNextRecord()
	{
		recordIndex++;

		Object dataRecord = taxRateFile.getRecord(recordIndex);
		TaxrateRow tableRow = new TaxrateRow();

		// The record could be a country or division record, so we check its type.
		if (dataRecord instanceof TaxRateCountryDataFileRecord)
		{
			TaxRateCountryDataFileRecord countryRecord = (TaxRateCountryDataFileRecord) dataRecord;
			tableRow.TX_ID = countryRecord.tx_id;
			tableRow.TX_NAME = countryRecord.tx_name;
			tableRow.TX_RATE = countryRecord.tx_rate;
		}
		else if (dataRecord instanceof TaxRateDivisionDataFileRecord)
		{
			TaxRateDivisionDataFileRecord divisionRecord = (TaxRateDivisionDataFileRecord) dataRecord;
			tableRow.TX_ID = divisionRecord.tx_id;
			tableRow.TX_NAME = divisionRecord.tx_name;
			tableRow.TX_RATE = divisionRecord.tx_rate;
		}
		else
		{
			throw new IllegalStateException("Unexpected record type in TaxRateFile.");
		}

		return tableRow;
	}
}