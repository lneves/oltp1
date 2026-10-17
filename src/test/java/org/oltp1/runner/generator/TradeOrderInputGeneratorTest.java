package org.oltp1.runner.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.oltp1.runner.model.AccountPermission;
import org.oltp1.runner.model.Company;
import org.oltp1.runner.model.CustomerTier;
import org.oltp1.runner.model.RandomCustomer;
import org.oltp1.runner.tx.trade_order.TxTradeOrderInput;

class TradeOrderInputGeneratorTest
{
	private static final long SEED = 20250915L;
	private static final long ACCOUNT_ID = 4_300_000_777L;

	private static final Company HELD_COMPANY = new Company("HELD", "1", 10L, "Held Company");
	private static final Company RANDOM_COMPANY = new Company("RAND", "2", 20L, "Random Company");

	private CustomerSelector customerSelector;
	private CompanySelector companySelector;
	private AccountPermissionSelector aclSelector;

	@BeforeEach
	void setUp()
	{
		ThreadLocalCRandom.set(new CRandom(SEED));

		customerSelector = mock(CustomerSelector.class);
		when(customerSelector.randomCustomer()).thenReturn(new RandomCustomer(1234L, CustomerTier.TierOne));
		when(customerSelector.randomAccId(any(RandomCustomer.class))).thenReturn(ACCOUNT_ID);

		companySelector = mock(CompanySelector.class);
		when(companySelector.forSymbol("HELD")).thenReturn(HELD_COMPANY);
		when(companySelector.randomCompany()).thenReturn(RANDOM_COMPANY);

		aclSelector = mock(AccountPermissionSelector.class);
		when(aclSelector.getAdditionalPermCount(anyLong())).thenReturn(0);
		when(aclSelector.getOwnerAcl(anyLong())).thenReturn(new AccountPermission("123", "Owner", "Person", true));
	}

	@Test
	void securityComesFromTheAccountsHoldings()
	{
		TradeOrderInputGenerator generator = generatorWith((accountId, random) -> "HELD");

		boolean sawSymbolPath = false;
		boolean sawCompanyPath = false;

		for (int i = 0; i < 300; i++)
		{
			TxTradeOrderInput input = generator.generateTradeOrderInput();

			assertEquals(ACCOUNT_ID, input.acct_id);

			if (input.symbol != null)
			{
				assertEquals("HELD", input.symbol);
				sawSymbolPath = true;
			}
			else
			{
				assertEquals("Held Company", input.co_name);
				assertEquals("1", input.issue);
				sawCompanyPath = true;
			}
		}

		assertTrue(sawSymbolPath, "symbol identification path expected");
		assertTrue(sawCompanyPath, "company-name identification path expected");
	}

	@Test
	void fallsBackToARandomSecurityWhenTheAccountHoldsNothing()
	{
		TradeOrderInputGenerator generator = generatorWith((accountId, random) -> null);

		for (int i = 0; i < 100; i++)
		{
			TxTradeOrderInput input = generator.generateTradeOrderInput();

			if (input.symbol != null)
			{
				assertEquals("RAND", input.symbol);
			}
			else
			{
				assertEquals("Random Company", input.co_name);
				assertEquals("2", input.issue);
			}
		}
	}

	@Test
	void fallsBackWhenTheHeldSymbolIsNotInTheSecurityUniverse()
	{
		when(companySelector.forSymbol("MISSING")).thenReturn(null);

		TradeOrderInputGenerator generator = generatorWith((accountId, random) -> "MISSING");

		for (int i = 0; i < 50; i++)
		{
			TxTradeOrderInput input = generator.generateTradeOrderInput();

			if (input.symbol != null)
			{
				assertEquals("RAND", input.symbol);
			}
			else
			{
				assertEquals("Random Company", input.co_name);
			}
		}
	}

	private TradeOrderInputGenerator generatorWith(HeldSymbolProvider heldSymbols)
	{
		return new TradeOrderInputGenerator(customerSelector, companySelector, aclSelector, heldSymbols);
	}
}
