package org.oltp1.runner.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.OffHeapStore;
import org.oltp1.runner.db.JdbcQuery;
import org.oltp1.runner.db.SqlContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads the securities held per customer account from HOLDING_SUMMARY, so
 * Trade-Order inputs can target securities the account actually holds
 * (reference: {@code GenerateRandomAccountSecurity}). Data is kept in an
 * off-heap store, like the other selectors; memory grows with the number of
 * (account, security) holdings in the database.
 * <p>
 * The snapshot is taken once when the driver starts; holdings created during
 * the run are not reflected (matching the reference driver's load-time
 * structure).
 */
public class HoldingSelector implements HeldSymbolProvider
{
	private static final Logger log = LoggerFactory.getLogger(HoldingSelector.class);

	private final MVMap<Long, List<String>> holdingsByAccount;

	public HoldingSelector(SqlContext sqlCtx)
	{
		OffHeapStore offHeap = new OffHeapStore();
		MVStore store = new MVStore.Builder().fileStore(offHeap).autoCommitDisabled().open();
		holdingsByAccount = store.openMap("holdingsByAccount");

		// fallback to raw JDBC, sql2o does not expose the "fetchSize" property
		JdbcQuery jdbc = new JdbcQuery(sqlCtx);

		String query = """
				SELECT hs_ca_id, hs_s_symb
				FROM holding_summary
				WHERE hs_qty <> 0;
				""";

		final LongAdder rowCounter = new LongAdder();

		jdbc.executeQuery(query, 1000, r -> {
			long accountId = r.getLong("hs_ca_id");
			String symbol = r.getString("hs_s_symb");

			List<String> symbols = holdingsByAccount.get(accountId);

			if (symbols == null)
			{
				symbols = new ArrayList<>();
			}

			symbols.add(symbol);
			holdingsByAccount.put(accountId, symbols);
			
			rowCounter.increment();
			if (rowCounter.longValue() % 2500 == 0)
			{
				store.commit();
			}
		});

		store.commit();
		store.compactFile(60000); // max compact time: 1 minute

		log.info("Loaded {} holdings for {} accounts from database", rowCounter.longValue(), holdingsByAccount.size());
	}

	@Override
	public String randomHeldSymbol(long accountId, CRandom random)
	{
		List<String> symbols = holdingsByAccount.get(accountId);

		if (symbols == null || symbols.isEmpty())
		{
			return null;
		}

		return symbols.get(random.rndIntRange(0, symbols.size() - 1));
	}
}
