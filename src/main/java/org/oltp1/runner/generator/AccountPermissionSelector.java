package org.oltp1.runner.generator;

import java.util.ArrayList;
import java.util.List;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.OffHeapStore;
import org.oltp1.runner.db.JdbcQuery;
import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.model.AccountPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountPermissionSelector
{
	private static final AccountPermission defAccPerm = new AccountPermission("00000000000000", "alpha", "omega", false);

	private static final Logger log = LoggerFactory.getLogger(AccountPermissionSelector.class);

	private final MVMap<Long, List<AccountPermission>> aclStore;

	public AccountPermissionSelector(final SqlContext sqlCtx)
	{
		OffHeapStore offHeap = new OffHeapStore();
		MVStore store = new MVStore.Builder().fileStore(offHeap).open();
		aclStore = store.openMap("acl");

		// fallback to raw JDBC, sql2o does not expose the "fetchSize" property
		JdbcQuery jdbc = new JdbcQuery(sqlCtx);
		int fetchSize = 1000;

		String query = """
				SELECT ap_ca_id, ap_tax_id, ap_l_name, ap_f_name, ap_acl
				FROM account_permission;
				""";

		jdbc.executeQuery(query, fetchSize, r -> {
			AccountPermission ap = new AccountPermission(
					r.getString("ap_tax_id"),
					r.getString("ap_f_name"),
					r.getString("ap_l_name"),
					r.getString("ap_acl").equals("0000"));

			long currentCaid = r.getLong("ap_ca_id");

			addToMultimap(currentCaid, ap);
		});

		store.commit();
		store.compactFile(60000); // max compact time: 1 minute

		log.info("Loaded {} account permission records from database", aclStore.size());
	}

	public AccountPermission getAcl(final long accountId, final boolean isOnwner)
	{
		return aclStore
				.get(accountId)
				.stream()
				.filter(ap -> ap.isOwner == isOnwner)
				.findAny()
				.orElseGet(() -> getAnyAcl(accountId));
	}

	private void addToMultimap(Long accId, AccountPermission ap)
	{
		List<AccountPermission> list = aclStore.get(accId);
		if (list == null)
		{
			list = new ArrayList<AccountPermission>();
		}
		list.add(ap);
		aclStore.put(accId, list);
	}

	private AccountPermission getAnyAcl(final long accountId)
	{
		return aclStore
				.get(accountId)
				.stream()
				.findAny()
				.orElseGet(() -> defAccPerm);
	}
}