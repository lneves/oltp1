package org.oltp1.egen.generator;

import java.util.List;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.io.records.ExchangeDataFileRecord;
import org.oltp1.egen.model.ExchangeRow;

/**
 * Generates data for the EXCHANGE table. This is a port of the C++
 * CExchangeTable class. It is a "Fixed Table" generator, meaning it generates
 * one row for each record in its input file. It has special logic to calculate
 * the number of securities on each exchange. Based on inc/ExchangeTable.h and
 * src/ExchangeTable.cpp
 */
public class ExchangeTable implements TableGenerator<ExchangeRow>
{

	private static final long IDENT_T_SHIFT = 4300000000L;
	private final List<ExchangeDataFileRecord> dataFile;
	private int recordIndex = -1;
	private final int[] securityCount = new int[4]; // Hardcoded for 4 exchanges
	private boolean hasMoreRecords;

	public ExchangeTable(DataFileManager dfm)
	{
		this.dataFile = dfm.getExchangeDataFile();
		this.hasMoreRecords = !this.dataFile.isEmpty();
		computeNumSecurities(dfm.getConfiguredCustomers());
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
	public ExchangeRow generateNextRecord()
	{
		recordIndex++;

		ExchangeDataFileRecord dataRecord = dataFile.get(recordIndex);
		ExchangeRow tableRow = new ExchangeRow();

		tableRow.EX_AD_ID = dataRecord.ex_ad_id + IDENT_T_SHIFT;
		tableRow.EX_CLOSE = dataRecord.ex_close;
		tableRow.EX_DESC = dataRecord.ex_desc;
		tableRow.EX_ID = dataRecord.ex_id;
		tableRow.EX_NAME = dataRecord.ex_name;
		tableRow.EX_NUM_SYMB = securityCount[recordIndex];
		tableRow.EX_OPEN = dataRecord.ex_open;

		return tableRow;
	}

	/**
	 * A direct port of the C++ ComputeNumSecurities logic. It calculates the number
	 * of securities per exchange based on a pre-computed distribution table and the
	 * total number of customers.
	 */
	private void computeNumSecurities(long customerCount)
	{
		// This array is a pre-computation of the cumulative number of securities
		// each exchange has in the first 10 Load Units (from Security.txt).
		final int[][] securityCounts = {
				{ 0, 153, 307, 491, 688, 859, 1028, 1203, 1360, 1532, 1704 },
				{ 0, 173, 344, 498, 658, 848, 1006, 1191, 1402, 1572, 1749 },
				{ 0, 189, 360, 534, 714, 875, 1023, 1174, 1342, 1507, 1666 },
				{ 0, 170, 359, 532, 680, 843, 1053, 1227, 1376, 1554, 1731 }
		};

		long numLU = customerCount / 1000;
		long numLU_Tens = numLU / 10;
		int numLU_Ones = (int) (numLU % 10);

		for (int i = 0; i < 4; i++)
		{
			securityCount[i] = (int) (securityCounts[i][10] * numLU_Tens + securityCounts[i][numLU_Ones]);
		}
	}
}