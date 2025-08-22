package org.oltp1.egen.generator;

import java.util.List;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.io.records.SectorDataFileRecord;
import org.oltp1.egen.model.SectorRow;

/**
 * Generates data for the SECTOR table. This is a port of the C++ CSectorTable
 * class. It is a "Fixed Table" generator, meaning it generates one row for each
 * record in its input file. Based on inc/SectorTable.h and src/SectorTable.cpp
 */
public class SectorTable implements TableGenerator<SectorRow>
{
	private final List<SectorDataFileRecord> dataFile;
	private int recordIndex = -1;
	private boolean hasMoreRecords;

	public SectorTable(DataFileManager dfm)
	{
		this.dataFile = dfm.getSectorDataFile();
		this.hasMoreRecords = !this.dataFile.isEmpty();
	}

	public boolean hasMoreRecords()
	{
		if (recordIndex >= (dataFile.size() - 1))
		{
			hasMoreRecords = false;
		}

		return hasMoreRecords;
	}

	public SectorRow generateNextRecord()
	{
		recordIndex++;

		SectorDataFileRecord dataRecord = dataFile.get(recordIndex);
		SectorRow tableRow = new SectorRow();

		tableRow.SC_ID = dataRecord.sc_id;
		tableRow.SC_NAME = dataRecord.sc_name;

		return tableRow;
	}
}