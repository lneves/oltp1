package org.oltp1.egen.generator;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.io.WeightedDataFile;
import org.oltp1.egen.io.records.ZipCodeDataFileRecord;
import org.oltp1.egen.model.ZipCodeRow;

public class ZipCodeTable implements TableGenerator<ZipCodeRow>
{
	private final WeightedDataFile<ZipCodeDataFileRecord> dataFile;
	private int recordIndex = -1;
	private boolean hasMoreRecords;

	public ZipCodeTable(DataFileManager dfm)
	{
		this.dataFile = dfm.getZipCodeDataFile();
		this.hasMoreRecords = this.dataFile.uniqueSize() > 0;
	}

	public boolean hasMoreRecords()
	{
		if (recordIndex >= (dataFile.uniqueSize() - 1))
		{
			hasMoreRecords = false;
		}
		return hasMoreRecords;
	}

	public ZipCodeRow generateNextRecord()
	{
		recordIndex++;

		// Get the unique record, not from the weighted list
		ZipCodeDataFileRecord dataRecord = dataFile.getUniqueRecord(recordIndex);
		ZipCodeRow tableRow = new ZipCodeRow();

		tableRow.ZC_CODE = dataRecord.zc_code;
		tableRow.ZC_TOWN = dataRecord.zc_town;
		tableRow.ZC_DIV = dataRecord.zc_div;

		return tableRow;
	}
}