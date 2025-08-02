package org.caudexorigo.oltp1.generator;

import org.caudexorigo.db.SqlContext;
import org.caudexorigo.oltp1.model.AccountPermission;
import org.caudexorigo.oltp1.model.AccountPermissionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Query;

public class AccountPermissionSelector
{
	private static final Logger log = LoggerFactory.getLogger(AccountPermissionSelector.class);

	private final AccountPermissionStore aclStore;

	private static final AccountPermission defAccPerm = new AccountPermission("00000000000000", "alpha", "omega", false);

	public AccountPermissionSelector(final SqlContext sqlCtx)
	{
		try (Connection con = sqlCtx.getSql2o().open())
		{
			int aclCount = con
					.createQuery("SELECT COUNT(*) FROM account_permission;")
					.executeScalar(Integer.class);

			aclStore = new AccountPermissionStore(aclCount * 2);

			Query tx = con.createQuery("""
					SELECT ap_ca_id, ap_tax_id, ap_l_name, ap_f_name, ap_acl
					FROM account_permission;
					""");

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

						aclStore.addToMultimap(r.getLong("ap_ca_id"), ap);

					});
		}

		log.info("Loaded {} account permission records from database", aclStore.size());
	}

	public AccountPermission getAcl(final long accountId, final boolean isOnwner)
	{
		return aclStore
				.get(accountId)
				.stream()
				.filter(ap -> ap.isOwner == isOnwner)
				.findFirst()
				.orElseGet(() -> getAnyAcl(accountId));
	}

	private AccountPermission getAnyAcl(final long accountId)
	{
		return aclStore
				.get(accountId)
				.stream()
				.findFirst()
				.orElseGet(() -> defAccPerm);
	}
}