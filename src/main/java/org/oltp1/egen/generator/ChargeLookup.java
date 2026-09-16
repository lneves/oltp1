package org.oltp1.egen.generator;

import java.util.List;

import org.oltp1.egen.io.records.ChargeDataFileRecord;
import org.oltp1.egen.io.records.TradeTypeDataFileRecord;

/**
 * Indexed CHARGE lookup. The charge file holds exactly one row per (trade type,
 * customer tier); this class resolves that key once at construction and serves
 * it with a single array access per trade. A missing, duplicated or unknown key
 * is rejected at construction.
 */
final class ChargeLookup
{
	private static final int TIER_COUNT = 3;

	private final ChargeDataFileRecord[][] byTypeTier;
	private final int tradeTypeCount;

	ChargeLookup(List<ChargeDataFileRecord> records, List<TradeTypeDataFileRecord> tradeTypes)
	{
		this.tradeTypeCount = tradeTypes.size();
		this.byTypeTier = new ChargeDataFileRecord[tradeTypeCount][TIER_COUNT];

		for (ChargeDataFileRecord record : records)
		{
			int typeOrdinal = indexOfType(tradeTypes, record.ch_tt_id);

			if (typeOrdinal < 0)
			{
				throw new IllegalArgumentException(String.format(
						"CHARGE file references unknown trade type '%s'",
						record.ch_tt_id));
			}

			if (record.ch_c_tier < 1 || record.ch_c_tier > TIER_COUNT)
			{
				throw new IllegalArgumentException(String.format(
						"CHARGE file has invalid tier %d for trade type '%s'",
						record.ch_c_tier,
						record.ch_tt_id));
			}

			if (byTypeTier[typeOrdinal][record.ch_c_tier - 1] != null)
			{
				throw new IllegalArgumentException(String.format(
						"CHARGE file has a duplicate row for trade type '%s', tier %d",
						record.ch_tt_id,
						record.ch_c_tier));
			}

			byTypeTier[typeOrdinal][record.ch_c_tier - 1] = record;
		}

		for (int typeOrdinal = 0; typeOrdinal < tradeTypeCount; typeOrdinal++)
		{
			for (int tier = 0; tier < TIER_COUNT; tier++)
			{
				if (byTypeTier[typeOrdinal][tier] == null)
				{
					throw new IllegalArgumentException(String.format(
							"CHARGE file is missing a row for trade type '%s', tier %d",
							tradeTypes.get(typeOrdinal).tt_id,
							tier + 1));
				}
			}
		}
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

	ChargeDataFileRecord find(int tradeTypeOrdinal, int tier)
	{
		if (tradeTypeOrdinal < 0 || tradeTypeOrdinal >= tradeTypeCount || tier < 1 || tier > TIER_COUNT)
		{
			throw new IllegalArgumentException(String.format(
					"Invalid charge lookup key: tradeType=%d, tier=%d",
					tradeTypeOrdinal,
					tier));
		}

		return byTypeTier[tradeTypeOrdinal][tier - 1];
	}
}
