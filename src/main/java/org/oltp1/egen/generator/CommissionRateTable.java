package org.oltp1.egen.generator;

import java.util.List;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.io.records.CommissionRateDataFileRecord;
import org.oltp1.egen.model.CommissionRateRow;

/**
 * Table generator for the CommissionRate table. This class reads the data
 * parsed by the DataFileManager and transforms it into CommissionRateRow
 * objects, ready for output. Based on inc/CommissionRateTable.h and
 * src/CommissionRateTable.cpp
 */
public class CommissionRateTable implements TableGenerator<CommissionRateRow>
{
	private final List<CommissionRateDataFileRecord> dataFileRecords;
	private int currentIndex = 0;

	public CommissionRateTable(DataFileManager dfm)
	{
		// Retrieve the pre-parsed data from the DataFileManager
		this.dataFileRecords = dfm.getCommissionRateDataFile();
	}

	@Override
	public boolean hasMoreRecords()
	{
		return currentIndex < dataFileRecords.size();
	}

	@Override
	public CommissionRateRow generateNextRecord()
	{
		CommissionRateDataFileRecord record = dataFileRecords.get(currentIndex);
		currentIndex++;

		// Map the input record to the output row format
		CommissionRateRow row = new CommissionRateRow();
		row.CR_C_TIER = record.cr_c_tier;
		row.CR_TT_ID = record.cr_tt_id;
		row.CR_EX_ID = record.cr_ex_id;
		row.CR_FROM_QTY = record.cr_from_qty;
		row.CR_TO_QTY = record.cr_to_qty;
		row.CR_RATE = record.cr_rate;

		return row;
	}
}