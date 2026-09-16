package org.oltp1.egen.generator;

import java.util.List;

import org.oltp1.egen.io.records.CommissionRateDataFileRecord;
import org.oltp1.egen.io.records.ExchangeDataFileRecord;
import org.oltp1.egen.io.records.TradeTypeDataFileRecord;

/**
 * Indexed COMMISSION_RATE lookup. Mirrors the reference EGen optimization: the
 * commission file is laid out by customer tier, then trade type, then exchange,
 * with a small number of quantity-range rows per (tier, type, exchange) block.
 * The reference computes the block offset and scans at most
 * {@code fileSize / (3 * tradeTypes * exchanges)} rows; this class validates
 * that layout once at construction and performs the same bounded scan per
 * trade, without any string comparisons in the hot path.
 * <p>
 * A file that does not follow the official layout is rejected at construction.
 */
final class CommissionRateLookup
{
	private static final int TIER_COUNT = 3;

	private final CommissionRateDataFileRecord[][] blocks;
	private final int rowsPerBlock;
	private final int tradeTypeCount;
	private final int exchangeCount;

	CommissionRateLookup(
			List<CommissionRateDataFileRecord> records,
			List<TradeTypeDataFileRecord> tradeTypes,
			List<ExchangeDataFileRecord> exchanges)
	{
		this.tradeTypeCount = tradeTypes.size();
		this.exchangeCount = exchanges.size();

		int blockCount = TIER_COUNT * tradeTypeCount * exchangeCount;

		if (blockCount == 0 || records.isEmpty() || records.size() % blockCount != 0)
		{
			throw new IllegalArgumentException(String.format(
					"COMMISSION_RATE file layout is invalid: %d records cannot be split into %d blocks (3 tiers x %d trade types x %d exchanges)",
					records.size(),
					blockCount,
					tradeTypeCount,
					exchangeCount));
		}

		this.rowsPerBlock = records.size() / blockCount;
		this.blocks = new CommissionRateDataFileRecord[blockCount][rowsPerBlock];

		for (int slot = 0; slot < blockCount; slot++)
		{
			int tier = slot / (tradeTypeCount * exchangeCount) + 1;
			int typeOrdinal = (slot / exchangeCount) % tradeTypeCount;
			int exchangeOrdinal = slot % exchangeCount;

			String expectedType = tradeTypes.get(typeOrdinal).tt_id;
			String expectedExchange = exchanges.get(exchangeOrdinal).ex_id;

			for (int i = 0; i < rowsPerBlock; i++)
			{
				int recordIndex = slot * rowsPerBlock + i;
				CommissionRateDataFileRecord record = records.get(recordIndex);

				if (record.cr_c_tier != tier
						|| !expectedType.equals(record.cr_tt_id)
						|| !expectedExchange.equals(record.cr_ex_id))
				{
					throw new IllegalArgumentException(String.format(
							"COMMISSION_RATE file layout is invalid at record %d: expected tier=%d, type=%s, exchange=%s but found tier=%d, type=%s, exchange=%s",
							recordIndex,
							tier,
							expectedType,
							expectedExchange,
							record.cr_c_tier,
							record.cr_tt_id,
							record.cr_ex_id));
				}

				blocks[slot][i] = record;
			}
		}
	}

	CommissionRateDataFileRecord find(int tier, int tradeTypeOrdinal, int exchangeOrdinal, int qty)
	{
		if (tier < 1 || tier > TIER_COUNT
				|| tradeTypeOrdinal < 0 || tradeTypeOrdinal >= tradeTypeCount
				|| exchangeOrdinal < 0 || exchangeOrdinal >= exchangeCount)
		{
			throw new IllegalArgumentException(String.format(
					"Invalid commission lookup key: tier=%d, tradeType=%d, exchange=%d",
					tier,
					tradeTypeOrdinal,
					exchangeOrdinal));
		}

		int slot = ((tier - 1) * tradeTypeCount + tradeTypeOrdinal) * exchangeCount + exchangeOrdinal;

		for (CommissionRateDataFileRecord rule : blocks[slot])
		{
			if (qty >= rule.cr_from_qty && qty <= rule.cr_to_qty)
			{
				return rule;
			}
		}

		throw new IllegalStateException(String.format(
				"Could not find a matching commission rate for tier=%d, tradeType=%d, exchange=%d, qty=%d",
				tier,
				tradeTypeOrdinal,
				exchangeOrdinal,
				qty));
	}
}
