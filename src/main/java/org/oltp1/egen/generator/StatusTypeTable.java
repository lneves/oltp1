package org.oltp1.egen.generator;

import java.util.List;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.io.records.StatusTypeDataFileRecord;
import org.oltp1.egen.model.StatusTypeRow;

public class StatusTypeTable implements TableGenerator<StatusTypeRow>
{
	private final List<StatusTypeDataFileRecord> dataFile;
	private int recordIndex = -1;
	private boolean hasMoreRecords;

	public StatusTypeTable(DataFileManager dfm)
	{
		this.dataFile = dfm.getStatusTypeDataFile();
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
	public StatusTypeRow generateNextRecord()
	{
		recordIndex++;

		StatusTypeDataFileRecord dataRecord = dataFile.get(recordIndex);
		StatusTypeRow tableRow = new StatusTypeRow();

		tableRow.ST_ID = dataRecord.st_id;
		tableRow.ST_NAME = dataRecord.st_name;

		return tableRow;
	}
}