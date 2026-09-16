package org.oltp1.runner.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

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
	private static final Logger log = LoggerFactory.getLogger(AccountPermissionSelector.class);

	// Every TPC-E account has exactly one owner permission row: the row whose
	// name and tax id match the account's customer. AP_ACL is deliberately NOT
	// used to identify the owner because Data-Maintenance mutates it during a run
	// (toggling the highest-ACL row between '1111' and '0011').
	private static final int MAX_DIAGNOSTIC_IDS = 10;

	// Package-private pure helpers so they can be unit-tested without an MVStore:
	static boolean isOwnerIdentity(String lName, String fName, String taxId, String ownerLName, String ownerFName, String ownerTaxId)
	{
		return lName != null && lName.equals(ownerLName)
				&& fName != null && fName.equals(ownerFName)
				&& taxId != null && taxId.equals(ownerTaxId);
	}

	static int countAdditional(List<AccountPermission> acls)
	{
		/* count !isOwner */
		return (int) acls
				.stream()
				.filter(ap -> ap.isOwner == false)
				.count();
	}

	static AccountPermission findOwner(long accountId, List<AccountPermission> acls)
	{
		/* isOwner, else throw */
		return acls
				.stream()
				.filter(ap -> ap.isOwner == true)
				.findAny()
				.orElseThrow(() -> new IllegalStateException(
						String.format(
								"Account %d has no owner permission row matching the customer's name and tax id; ACCOUNT_PERMISSION data is incomplete",
								accountId)));
	}

	static AccountPermission findNonOwner(long accountId, List<AccountPermission> acls)
	{
		/* !isOwner, else throw */
		return acls
				.stream()
				.filter(ap -> ap.isOwner == false)
				.findAny()
				.orElseThrow(() -> new IllegalStateException(
						String
								.format(
										"Account %d has no non-owner permission row; ACCOUNT_PERMISSION data is inconsistent",
										accountId)));
	}

	static OwnerCoverage inspectOwnerCoverage(Map<Long, List<AccountPermission>> acls, int maxSampleIds)
	{
		long missingCount = 0;
		long duplicateCount = 0;
		List<Long> missingSample = new ArrayList<>();
		List<Long> duplicateSample = new ArrayList<>();

		for (Map.Entry<Long, List<AccountPermission>> entry : acls.entrySet())
		{
			long owners = entry.getValue().stream().filter(ap -> ap.isOwner).count();

			if (owners == 0)
			{
				missingCount++;
				if (missingSample.size() < maxSampleIds)
				{
					missingSample.add(entry.getKey());
				}
			}
			else if (owners > 1)
			{
				duplicateCount++;
				if (duplicateSample.size() < maxSampleIds)
				{
					duplicateSample.add(entry.getKey());
				}
			}
		}

		return new OwnerCoverage(missingCount, duplicateCount, missingSample, duplicateSample);
	}

	record OwnerCoverage(long missingCount, long duplicateCount, List<Long> missingSample, List<Long> duplicateSample)
	{
	}

	private final MVMap<Long, List<AccountPermission>> aclStore;
	private final boolean strictPermissions;

	private long accountCount;
	private long ownerRowCount;

	public AccountPermissionSelector(final SqlContext sqlCtx)
	{
		this(sqlCtx, true);
	}

	public AccountPermissionSelector(final SqlContext sqlCtx, final boolean strictPermissions)
	{
		this.strictPermissions = strictPermissions;
		OffHeapStore offHeap = new OffHeapStore();
		MVStore store = new MVStore.Builder().fileStore(offHeap).autoCommitDisabled().open();
		aclStore = store.openMap("acl");

		// fallback to raw JDBC, sql2o does not expose the "fetchSize" property
		JdbcQuery jdbc = new JdbcQuery(sqlCtx);
		int fetchSize = 1000;

		// Ownership is an identity match, not an ACL check: Data-Maintenance
		// rewrites AP_ACL for the highest-ACL row of an account during a run.
		String query = """
				SELECT
					ap.ap_ca_id
					, ap.ap_tax_id
					, ap.ap_l_name
					, ap.ap_f_name
					, c.c_l_name AS owner_l_name
					, c.c_f_name AS owner_f_name
					, c.c_tax_id AS owner_tax_id
				FROM
					account_permission ap
					LEFT JOIN customer_account ca ON ca.ca_id = ap.ap_ca_id
					LEFT JOIN customer c ON c.c_id = ca.ca_c_id;
				""";

		final LongAdder rowCounter = new LongAdder();
		
		jdbc.executeQuery(query, fetchSize, r -> {
			AccountPermission ap = new AccountPermission(
					r.getString("ap_tax_id"),
					r.getString("ap_f_name"),
					r.getString("ap_l_name"),
					isOwnerIdentity(
							r.getString("ap_l_name"),
							r.getString("ap_f_name"),
							r.getString("ap_tax_id"),
							r.getString("owner_l_name"),
							r.getString("owner_f_name"),
							r.getString("owner_tax_id")));

			long currentCaid = r.getLong("ap_ca_id");

			addToMultimap(currentCaid, ap);
			
			rowCounter.increment();
			if (rowCounter.longValue() % 2500 == 0)
			{
				store.commit();
			}
		});

		store.commit();
		store.compactFile(60000); // max compact time: 1 minute

		validateOwnerCoverage();

		log.info("Loaded account permissions for {} accounts from database", aclStore.size());
	}

	private void addToMultimap(Long accId, AccountPermission ap)
	{
		List<AccountPermission> list = aclStore.get(accId);
		if (list == null)
		{
			list = new ArrayList<AccountPermission>();
			accountCount++;
		}
		list.add(ap);
		if (ap.isOwner)
		{
			ownerRowCount++;
		}
		aclStore.put(accId, list);
	}

	/**
	 * Checks the TPC-E invariant that every account has exactly one owner row
	 * (the row matching the account's customer identity). Fails fast by default;
	 * logs a warning instead when strict validation is disabled
	 * (--skip-permission-validation).
	 */
	private void validateOwnerCoverage()
	{
		if (aclStore.isEmpty())
		{
			throw new IllegalStateException(
					"ACCOUNT_PERMISSION is empty; run 'oltp1 initdb' before the driver");
		}

		if (ownerRowCount != accountCount)
		{
			OwnerCoverage coverage = inspectOwnerCoverage(aclStore, MAX_DIAGNOSTIC_IDS);

			String message = String.format(
					"ACCOUNT_PERMISSION owner coverage mismatch: accounts=%d, ownerRows=%d, accountsMissingOwner=%d (sample=%s), accountsWithDuplicateOwner=%d (sample=%s)",
					accountCount,
					ownerRowCount,
					coverage.missingCount(),
					coverage.missingSample(),
					coverage.duplicateCount(),
					coverage.duplicateSample());

			if (strictPermissions)
			{
				throw new IllegalStateException(message);
			}

			log.warn(
					"{}; continuing because permission validation is disabled. Trade-Order will fail for the affected accounts",
					message);
		}
	}

	/**
	 * Number of non-owner permissions for the account (0, 1 or 2 by loader
	 * distribution).
	 */
	public int getAdditionalPermCount(long accountId)
	{
		return countAdditional(requireAcls(accountId));
	}

	/** The account owner's permission row. */
	public AccountPermission getOwnerAcl(long accountId)
	{
		return findOwner(accountId, requireAcls(accountId));
	}

	/** One of the account's non-owner permission rows. */
	public AccountPermission getNonOwnerAcl(long accountId)
	{
		return findNonOwner(accountId, requireAcls(accountId));
	}

	private List<AccountPermission> requireAcls(long accountId)
	{
		List<AccountPermission> acls = aclStore.get(accountId);
		if (acls == null || acls.isEmpty())
			throw new IllegalStateException("No ACCOUNT_PERMISSION rows for account " + accountId);
		return acls;
	}

}
