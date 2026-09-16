package org.oltp1.runner.generator;

/**
 * Supplies a security held by a customer account for Trade-Order input
 * generation. Matches the reference driver's
 * {@code GenerateRandomAccountSecurity} behavior: the chosen security must be
 * one the account actually holds.
 */
public interface HeldSymbolProvider
{
	/**
	 * @param accountId
	 *            the account to select a held security for
	 * @param random
	 *            the compliant random number generator
	 * @return a symbol currently held by the account, or {@code null} when the
	 *         account holds no securities
	 */
	String randomHeldSymbol(long accountId, CRandom random);
}
