package org.oltp1.runner.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.oltp1.runner.model.CustomerTier;
import org.oltp1.runner.model.RandomCustomer;
import org.oltp1.runner.tx.customer_position.TxCustomerPositionInput;

class CustomerPositionInputGeneratorTest
{
	private static final long SEED = 12345L;
	private static final int NUM_ACCOUNTS = 5;
	private static final RandomCustomer CUSTOMER = new RandomCustomer(1234L, CustomerTier.TierOne);
	private static final String TAX_ID = "123456789";

	private CustomerPositionInputGenerator generator;

	@BeforeEach
	void setUp()
	{
		ThreadLocalCRandom.set(new CRandom(SEED));

		CustomerSelector customerSelector = mock(CustomerSelector.class);
		when(customerSelector.randomCustomer()).thenReturn(CUSTOMER);
		when(customerSelector.getTaxId(CUSTOMER.cId)).thenReturn(TAX_ID);
		when(customerSelector.getNumberOfAccounts(CUSTOMER)).thenReturn(NUM_ACCOUNTS);

		generator = new CustomerPositionInputGenerator(customerSelector);
	}

	@Test
	void generatedInputsAlwaysSatisfyTheInputContract()
	{
		boolean sawTaxIdPath = false;
		boolean sawCustIdPath = false;
		boolean sawHistoryPath = false;

		for (int i = 0; i < 500; i++)
		{
			TxCustomerPositionInput input = generator.generateCustomerPositionInput();

			assertNotNull(input);

			if (input.taxId() != null)
			{
				sawTaxIdPath = true;
				assertEquals(TAX_ID, input.taxId());
				assertEquals(-1L, input.custId());
			}
			else
			{
				sawCustIdPath = true;
				assertEquals(CUSTOMER.cId, input.custId());
			}

			if (input.getHistory())
			{
				sawHistoryPath = true;
				assertTrue(input.acctIdIdx() >= 0);
				assertTrue(input.acctIdIdx() < NUM_ACCOUNTS);
			}
			else
			{
				assertEquals(-1, input.acctIdIdx());
			}
		}

		assertTrue(sawTaxIdPath);
		assertTrue(sawCustIdPath);
		assertTrue(sawHistoryPath);
	}

	@Test
	void taxIdLookupIsOnlyUsedForTheTaxIdPath()
	{
		for (int i = 0; i < 500; i++)
		{
			TxCustomerPositionInput input = generator.generateCustomerPositionInput();

			if (input.taxId() == null)
			{
				assertNull(input.taxId());
				assertEquals(CUSTOMER.cId, input.custId());
			}
		}
	}
}
