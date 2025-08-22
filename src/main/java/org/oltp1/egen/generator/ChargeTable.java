package org.oltp1.egen.generator;

import java.util.List;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.io.records.ChargeDataFileRecord;
import org.oltp1.egen.model.ChargeRow;

/**
 * Table generator for the CHARGE table. This class reads the data parsed by the
 * DataFileManager and transforms it into ChargeRow objects, ready for output.
 * Based on inc/ChargeTable.h and src/ChargeTable.cpp
 */
public class ChargeTable implements TableGenerator<ChargeRow>
{
	private final List<ChargeDataFileRecord> dataFileRecords;
	private int currentIndex = 0;

	public ChargeTable(DataFileManager dfm)
	{
		// Retrieve the pre-parsed data from the DataFileManager
		this.dataFileRecords = dfm.getChargeDataFile();
	}

	@Override
	public boolean hasMoreRecords()
	{
		return currentIndex < dataFileRecords.size();
	}

	@Override
	public ChargeRow generateNextRecord()
	{
		ChargeDataFileRecord record = dataFileRecords.get(currentIndex);
		currentIndex++;

		// Map the input record to the output row format
		ChargeRow row = new ChargeRow();
		row.CH_TT_ID = record.ch_tt_id;
		row.CH_C_TIER = record.ch_c_tier;
		row.CH_CHRG = record.ch_chrg;

		return row;
	}
}