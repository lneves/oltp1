package org.oltp1.egen.generator;

import java.util.List;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.io.records.TradeTypeDataFileRecord;
import org.oltp1.egen.model.TradeTypeRow;

public class TradeTypeTable implements TableGenerator<TradeTypeRow>
{
	private final List<TradeTypeDataFileRecord> dataFile;
	private int recordIndex = -1;
	private boolean hasMoreRecords;

	public TradeTypeTable(DataFileManager dfm)
	{
		this.dataFile = dfm.getTradeTypeDataFile();
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

	public TradeTypeRow generateNextRecord()
	{
		recordIndex++;

		TradeTypeDataFileRecord dataRecord = dataFile.get(recordIndex);
		TradeTypeRow tableRow = new TradeTypeRow();

		tableRow.TT_ID = dataRecord.tt_id;
		tableRow.TT_NAME = dataRecord.tt_name;
		tableRow.TT_IS_SELL = dataRecord.tt_is_sell;
		tableRow.TT_IS_MRKT = dataRecord.tt_is_mrkt;

		return tableRow;
	}
}