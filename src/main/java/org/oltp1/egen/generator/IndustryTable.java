package org.oltp1.egen.generator;

import java.util.List;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.io.records.IndustryDataFileRecord;
import org.oltp1.egen.model.IndustryRow;

/**
 * Generates data for the INDUSTRY table. This is a port of the C++
 * CIndustryTable class. It is a "Fixed Table" generator, meaning it generates
 * one row for each record in its input file. Based on inc/IndustryTable.h and
 * src/IndustryTable.cpp
 */
public class IndustryTable implements TableGenerator<IndustryRow>
{
	private final List<IndustryDataFileRecord> dataFile;
	private int recordIndex = -1;
	private boolean hasMoreRecords;

	public IndustryTable(DataFileManager dfm)
	{
		this.dataFile = dfm.getIndustryDataFile();
		this.hasMoreRecords = !this.dataFile.isEmpty();
	}

	@Override
	public boolean hasMoreRecords()
	{
		if (recordIndex >= (dataFile.size() - 1))
		{
			hasMoreRecords = false;
		}
		return hasMoreRecords;
	}

	@Override
	public IndustryRow generateNextRecord()
	{
		recordIndex++;

		IndustryDataFileRecord dataRecord = dataFile.get(recordIndex);
		IndustryRow tableRow = new IndustryRow();

		tableRow.IN_ID = dataRecord.in_id;
		tableRow.IN_NAME = dataRecord.in_name;
		tableRow.IN_SC_ID = dataRecord.in_sc_id;

		return tableRow;
	}
}