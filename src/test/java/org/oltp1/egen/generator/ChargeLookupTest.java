package org.oltp1.egen.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.io.records.ChargeDataFileRecord;
import org.oltp1.egen.io.records.TradeTypeDataFileRecord;

class ChargeLookupTest
{
	@Test
	void findMatchesLinearScanForAllCombinations()
	{
		DataFileManager dfm = new DataFileManager(1000, 1000);
		List<ChargeDataFileRecord> records = dfm.getChargeDataFile();
		List<TradeTypeDataFileRecord> tradeTypes = dfm.getTradeTypeDataFile();

		ChargeLookup lookup = new ChargeLookup(records, tradeTypes);

		for (int type = 0; type < tradeTypes.size(); type++)
		{
			String ttId = tradeTypes.get(type).tt_id;

			for (int tier = 1; tier <= 3; tier++)
			{
				ChargeDataFileRecord expected = null;

				for (ChargeDataFileRecord record : records)
				{
					if (record.ch_tt_id.equals(ttId) && record.ch_c_tier == tier)
					{
						expected = record;
						break;
					}
				}

				assertNotNull(expected, "missing reference row for " + ttId + "/" + tier);
				assertEquals(expected.ch_chrg, lookup.find(type, tier).ch_chrg, 0.0);
			}
		}
	}

	@Test
	void rejectsMissingRow()
	{
		DataFileManager dfm = new DataFileManager(1000, 1000);
		List<ChargeDataFileRecord> records = new ArrayList<>(dfm.getChargeDataFile());
		records.remove(0);

		assertThrows(IllegalArgumentException.class, () -> new ChargeLookup(records, dfm.getTradeTypeDataFile()));
	}

	@Test
	void rejectsDuplicateRow()
	{
		DataFileManager dfm = new DataFileManager(1000, 1000);
		List<ChargeDataFileRecord> records = new ArrayList<>(dfm.getChargeDataFile());
		records.add(ChargeDataFileRecord.parse("TMB\t1\t5"));

		assertThrows(IllegalArgumentException.class, () -> new ChargeLookup(records, dfm.getTradeTypeDataFile()));
	}

	@Test
	void rejectsUnknownTradeType()
	{
		DataFileManager dfm = new DataFileManager(1000, 1000);
		List<ChargeDataFileRecord> records = new ArrayList<>(dfm.getChargeDataFile());
		records.add(ChargeDataFileRecord.parse("ZZZ\t1\t5"));

		assertThrows(IllegalArgumentException.class, () -> new ChargeLookup(records, dfm.getTradeTypeDataFile()));
	}

	@Test
	void rejectsInvalidLookupKeys()
	{
		DataFileManager dfm = new DataFileManager(1000, 1000);
		ChargeLookup lookup = new ChargeLookup(dfm.getChargeDataFile(), dfm.getTradeTypeDataFile());

		assertThrows(IllegalArgumentException.class, () -> lookup.find(-1, 1));
		assertThrows(IllegalArgumentException.class, () -> lookup.find(5, 1));
		assertThrows(IllegalArgumentException.class, () -> lookup.find(0, 0));
		assertThrows(IllegalArgumentException.class, () -> lookup.find(0, 4));
	}
}
