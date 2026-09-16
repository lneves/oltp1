package org.oltp1.egen.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.io.records.CommissionRateDataFileRecord;
import org.oltp1.egen.io.records.ExchangeDataFileRecord;
import org.oltp1.egen.io.records.TradeTypeDataFileRecord;

class CommissionRateLookupTest
{
	@Test
	void findMatchesLinearScanForEveryKeyAndQuantity()
	{
		DataFileManager dfm = new DataFileManager(1000, 1000);
		List<CommissionRateDataFileRecord> records = dfm.getCommissionRateDataFile();
		List<TradeTypeDataFileRecord> tradeTypes = dfm.getTradeTypeDataFile();
		List<ExchangeDataFileRecord> exchanges = dfm.getExchangeDataFile();

		CommissionRateLookup lookup = new CommissionRateLookup(records, tradeTypes, exchanges);

		for (int tier = 1; tier <= 3; tier++)
		{
			for (int type = 0; type < tradeTypes.size(); type++)
			{
				for (int exchange = 0; exchange < exchanges.size(); exchange++)
				{
					String ttId = tradeTypes.get(type).tt_id;
					String exId = exchanges.get(exchange).ex_id;

					for (int qty : quantitiesToCheck(records, tier, ttId, exId))
					{
						CommissionRateDataFileRecord expected = linearScan(records, tier, ttId, exId, qty);

						if (expected == null)
						{
							final int fTier = tier;
							final int fType = type;
							final int fExchange = exchange;

							assertThrows(
									IllegalStateException.class,
									() -> lookup.find(fTier, fType, fExchange, qty),
									"qty=" + qty + " should not resolve for tier=" + tier + ", type=" + ttId + ", exchange=" + exId);
						}
						else
						{
							CommissionRateDataFileRecord actual = lookup.find(tier, type, exchange, qty);

							assertEquals(expected.cr_rate, actual.cr_rate, 0.0, "rate mismatch for qty=" + qty);
							assertEquals(expected.cr_from_qty, actual.cr_from_qty);
							assertEquals(expected.cr_to_qty, actual.cr_to_qty);
						}
					}
				}
			}
		}
	}

	@Test
	void quantityRangeBoundsAreInclusive()
	{
		DataFileManager dfm = new DataFileManager(1000, 1000);
		List<CommissionRateDataFileRecord> records = dfm.getCommissionRateDataFile();
		List<TradeTypeDataFileRecord> tradeTypes = dfm.getTradeTypeDataFile();
		List<ExchangeDataFileRecord> exchanges = dfm.getExchangeDataFile();

		CommissionRateLookup lookup = new CommissionRateLookup(records, tradeTypes, exchanges);

		for (CommissionRateDataFileRecord record : records)
		{
			int type = indexOfType(tradeTypes, record.cr_tt_id);
			int exchange = indexOfExchange(exchanges, record.cr_ex_id);

			assertTrue(type >= 0 && exchange >= 0);
			assertEquals(record.cr_rate, lookup.find(record.cr_c_tier, type, exchange, record.cr_from_qty).cr_rate, 0.0);
			assertEquals(record.cr_rate, lookup.find(record.cr_c_tier, type, exchange, record.cr_to_qty).cr_rate, 0.0);
		}
	}

	@Test
	void rejectsFileWithUnexpectedRecordCount()
	{
		DataFileManager dfm = new DataFileManager(1000, 1000);
		List<CommissionRateDataFileRecord> records = new ArrayList<>(dfm.getCommissionRateDataFile());
		records.remove(0);

		assertThrows(
				IllegalArgumentException.class,
				() -> new CommissionRateLookup(records, dfm.getTradeTypeDataFile(), dfm.getExchangeDataFile()));
	}

	@Test
	void rejectsFileWithUnexpectedBlockOrdering()
	{
		DataFileManager dfm = new DataFileManager(1000, 1000);
		List<TradeTypeDataFileRecord> reorderedTypes = new ArrayList<>(dfm.getTradeTypeDataFile());
		reorderedTypes.add(reorderedTypes.remove(0));

		assertThrows(
				IllegalArgumentException.class,
				() -> new CommissionRateLookup(dfm.getCommissionRateDataFile(), reorderedTypes, dfm.getExchangeDataFile()));
	}

	@Test
	void rejectsInvalidLookupKeys()
	{
		DataFileManager dfm = new DataFileManager(1000, 1000);
		CommissionRateLookup lookup = new CommissionRateLookup(
				dfm.getCommissionRateDataFile(),
				dfm.getTradeTypeDataFile(),
				dfm.getExchangeDataFile());

		assertThrows(IllegalArgumentException.class, () -> lookup.find(0, 0, 0, 100));
		assertThrows(IllegalArgumentException.class, () -> lookup.find(4, 0, 0, 100));
		assertThrows(IllegalArgumentException.class, () -> lookup.find(1, 5, 0, 100));
		assertThrows(IllegalArgumentException.class, () -> lookup.find(1, 0, 4, 100));
	}

	private static TreeSet<Integer> quantitiesToCheck(
			List<CommissionRateDataFileRecord> records, int tier, String ttId, String exId)
	{
		TreeSet<Integer> quantities = new TreeSet<>();

		quantities.add(100);
		quantities.add(200);
		quantities.add(400);
		quantities.add(800);
		quantities.add(2000);
		quantities.add(999999);
		quantities.add(1000000);

		for (CommissionRateDataFileRecord record : records)
		{
			if (record.cr_c_tier == tier && record.cr_tt_id.equals(ttId) && record.cr_ex_id.equals(exId))
			{
				quantities.add(record.cr_from_qty);
				quantities.add(record.cr_to_qty);
				quantities.add(record.cr_from_qty - 1);
				quantities.add(record.cr_to_qty + 1);
			}
		}

		return quantities;
	}

	private static CommissionRateDataFileRecord linearScan(
			List<CommissionRateDataFileRecord> records, int tier, String ttId, String exId, int qty)
	{
		for (CommissionRateDataFileRecord record : records)
		{
			if (record.cr_c_tier == tier
					&& record.cr_tt_id.equals(ttId)
					&& record.cr_ex_id.equals(exId)
					&& qty >= record.cr_from_qty
					&& qty <= record.cr_to_qty)
			{
				return record;
			}
		}

		return null;
	}

	private static int indexOfType(List<TradeTypeDataFileRecord> tradeTypes, String ttId)
	{
		for (int i = 0; i < tradeTypes.size(); i++)
		{
			if (tradeTypes.get(i).tt_id.equals(ttId))
			{
				return i;
			}
		}

		return -1;
	}

	private static int indexOfExchange(List<ExchangeDataFileRecord> exchanges, String exId)
	{
		for (int i = 0; i < exchanges.size(); i++)
		{
			if (exchanges.get(i).ex_id.equals(exId))
			{
				return i;
			}
		}

		return -1;
	}
}
