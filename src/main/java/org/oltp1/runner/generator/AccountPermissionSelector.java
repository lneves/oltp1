package org.oltp1.runner.generator;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.model.AccountPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Query;

public class AccountPermissionSelector
{
	private static final Logger log = LoggerFactory.getLogger(AccountPermissionSelector.class);

	private static final long RNG_SEED_BASE_NUM_PERMS = 27794203L;
	private static final int PERCENT_ACCOUNT_ADDITIONAL_PERMISSIONS_0 = 60;
	private static final int PERCENT_ACCOUNT_ADDITIONAL_PERMISSIONS_1 = 38;
	private static final int PERCENT_ACCOUNT_ADDITIONAL_PERMISSIONS_2 = 2;

	private final ConcurrentMap<Long, AccountPermission[]> aclStore;

	private static final AccountPermission defAccPerm = new AccountPermission("00000000000000", "alpha", "omega", false);

	private final void addPermission(long accountId, AccountPermission p, int totalPerms, int ix)
	{
		aclStore
				.computeIfAbsent(accountId, k -> new AccountPermission[totalPerms])[ix] = p;
	}

	public AccountPermissionSelector(final SqlContext sqlCtx)
	{
		try (Connection con = sqlCtx.getSql2o().open())
		{
			aclStore = new ConcurrentHashMap<Long, AccountPermission[]>();

			Query tx = con.createQuery("""
					SELECT ap_ca_id, ap_tax_id, ap_l_name, ap_f_name, ap_acl
					FROM account_permission;
					""");

			final AtomicLong previousCaid = new AtomicLong(0L);
			final AtomicInteger ix = new AtomicInteger(0);

			tx
					.executeAndFetchTable()
					.rows()
					.stream()
					.forEach(r -> {
						AccountPermission ap = new AccountPermission(
								r.getString("ap_tax_id"),
								r.getString("ap_f_name"),
								r.getString("ap_l_name"),
								r.getString("ap_acl").equals("0000"));

						int extraPerms = getNumPermsForCA(r.getLong("ap_ca_id"));
						int totalPerms = extraPerms + 1;
						long currentCaid = r.getLong("ap_ca_id");
						int i = previousCaid.getAndSet(currentCaid) == currentCaid ? ix.incrementAndGet() : ix.updateAndGet(x -> x * 0);

						addPermission(currentCaid, ap, totalPerms, i);
					});
		}
		log.info("Loaded {} account permission records from database", aclStore.size());
	}

	public AccountPermission getAcl(final long accountId, final boolean isOnwner)
	{
		return Arrays
				.asList(
						aclStore
								.get(accountId))
				.stream()
				.filter(ap -> ap.isOwner == isOnwner)
				.findFirst()
				.orElseGet(() -> getAnyAcl(accountId));

	}

	private AccountPermission getAnyAcl(final long accountId)
	{
		return Arrays
				.asList(
						aclStore
								.get(accountId))
				.stream()
				.findFirst()
				.orElseGet(() -> defAccPerm);
	}

	private int getNumPermsForCA(long caId)
	{
		CRandom random = ThreadLocalCRandom.get();

		long oldSeed = random.getSeed(); // Save state
		random.setSeed(random.rndNthElement(RNG_SEED_BASE_NUM_PERMS, caId));
		int threshold = random.rndIntRange(1, 100);
		random.setSeed(oldSeed); // Restore state

		if (threshold <= PERCENT_ACCOUNT_ADDITIONAL_PERMISSIONS_0)
		{
			return 0; // 60% of accounts have just the owner row permissions
		}
		else
		{
			if (threshold <= PERCENT_ACCOUNT_ADDITIONAL_PERMISSIONS_0 +
					PERCENT_ACCOUNT_ADDITIONAL_PERMISSIONS_1)
			{
				return 1; // 38% of accounts have one additional permission row
			}
			else
			{
				return 2; // 2% of accounts have two additional permission rows
			}
		}
	}
}