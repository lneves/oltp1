package org.oltp1.runner.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.oltp1.runner.model.AccountPermission;

class AccountPermissionSelectorTest
{
	private static AccountPermission owner()
	{
		return new AccountPermission("11111111111111", "Owner", "Person", true);
	}

	private static AccountPermission nonOwner()
	{
		return new AccountPermission("22222222222222", "Alpha", "Omega", false);
	}

	@Test
	void findOwnerReturnsOwner()
	{
		AccountPermission owner = owner();
		List<AccountPermission> acls = List.of(nonOwner(), owner, nonOwner());

		assertSame(owner, AccountPermissionSelector.findOwner(42L, acls));
	}

	@Test
	void findOwnerThrowsWithAccountIdWhenNoOwnerRow()
	{
		IllegalStateException ex = assertThrows(
				IllegalStateException.class,
				() -> AccountPermissionSelector.findOwner(42L, List.of(nonOwner())));

		assertTrue(ex.getMessage().contains("42"), ex.getMessage());
		assertTrue(ex.getMessage().contains("owner"), ex.getMessage());
	}

	@Test
	void findOwnerThrowsWithAccountIdWhenListIsEmpty()
	{
		IllegalStateException ex = assertThrows(
				IllegalStateException.class,
				() -> AccountPermissionSelector.findOwner(7L, List.of()));

		assertTrue(ex.getMessage().contains("7"), ex.getMessage());
	}

	@Test
	void findNonOwnerReturnsNonOwner()
	{
		AccountPermission nonOwner = nonOwner();
		List<AccountPermission> acls = List.of(owner(), nonOwner, nonOwner());

		assertSame(nonOwner, AccountPermissionSelector.findNonOwner(42L, acls));
	}

	@Test
	void findNonOwnerThrowsWithAccountIdWhenOnlyOwnerRows()
	{
		IllegalStateException ex = assertThrows(
				IllegalStateException.class,
				() -> AccountPermissionSelector.findNonOwner(42L, List.of(owner())));

		assertTrue(ex.getMessage().contains("42"), ex.getMessage());
		assertTrue(ex.getMessage().contains("non-owner"), ex.getMessage());
	}

	@Test
	void countAdditionalCountsOnlyNonOwners()
	{
		assertEquals(2, AccountPermissionSelector.countAdditional(List.of(owner(), nonOwner(), nonOwner())));
		assertEquals(0, AccountPermissionSelector.countAdditional(List.of(owner())));
		assertEquals(0, AccountPermissionSelector.countAdditional(List.of()));
	}

	@Test
	void inspectOwnerCoverageCountsMissingAndDuplicateOwners()
	{
		Map<Long, List<AccountPermission>> acls = new HashMap<>();
		acls.put(1L, List.of(owner()));
		acls.put(2L, List.of(nonOwner()));
		acls.put(3L, List.of(owner(), owner()));
		acls.put(4L, List.of(nonOwner()));
		acls.put(5L, List.of(owner(), nonOwner()));

		AccountPermissionSelector.OwnerCoverage coverage = AccountPermissionSelector.inspectOwnerCoverage(acls, 10);

		assertEquals(2, coverage.missingCount());
		assertEquals(1, coverage.duplicateCount());
		assertEquals(Set.of(2L, 4L), new HashSet<>(coverage.missingSample()));
		assertEquals(Set.of(3L), new HashSet<>(coverage.duplicateSample()));
	}

	@Test
	void inspectOwnerCoverageRespectsMaxSampleIds()
	{
		Map<Long, List<AccountPermission>> acls = new HashMap<>();
		acls.put(1L, List.of(nonOwner()));
		acls.put(2L, List.of(nonOwner()));
		acls.put(3L, List.of(nonOwner()));

		AccountPermissionSelector.OwnerCoverage coverage = AccountPermissionSelector.inspectOwnerCoverage(acls, 1);

		assertEquals(3, coverage.missingCount());
		assertEquals(1, coverage.missingSample().size());
	}

	@Test
	void isOwnerIdentityMatchesCustomerIdentity()
	{
		assertTrue(AccountPermissionSelector
				.isOwnerIdentity("Fowle", "Joshua", "078GO5457DB627", "Fowle", "Joshua", "078GO5457DB627"));
		assertFalse(AccountPermissionSelector
				.isOwnerIdentity("Fowle", "Joshua", "078GO5457DB627", "Other", "Joshua", "078GO5457DB627"));
		assertFalse(AccountPermissionSelector
				.isOwnerIdentity("Fowle", "Joshua", "078GO5457DB627", "Fowle", "Other", "078GO5457DB627"));
		assertFalse(AccountPermissionSelector
				.isOwnerIdentity("Fowle", "Joshua", "078GO5457DB627", "Fowle", "Joshua", "999"));
	}

	@Test
	void isOwnerIdentityRejectsMissingIdentity()
	{
		assertFalse(AccountPermissionSelector.isOwnerIdentity(null, "Joshua", "x", "Fowle", "Joshua", "x"));
		assertFalse(AccountPermissionSelector.isOwnerIdentity("Fowle", "Joshua", "x", null, null, null));
		assertFalse(AccountPermissionSelector.isOwnerIdentity("Fowle", "Joshua", "x", "Fowle", "Joshua", null));
	}
}
