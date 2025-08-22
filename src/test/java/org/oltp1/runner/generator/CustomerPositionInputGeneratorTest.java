package org.oltp1.runner.generator;

import org.oltp1.runner.model.CustomerTier;
import org.oltp1.runner.model.RandomCustomer;
import org.oltp1.runner.tx.customer_position.TxCustomerPositionInput;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CustomerPositionInputGeneratorTest {

    @Mock
    private CustomerSelector customerSelector;

    // We do not mock CRandom as it's a final class.
    // Instead, we will control its behavior by setting a fixed seed.
    private CRandom predictableRandom;

    @InjectMocks
    private CustomerPositionInputGenerator generator;

    private final RandomCustomer fakeCustomer = new RandomCustomer(1234L, CustomerTier.TierOne);
    private final String fakeTaxId = "FAKE12345TAXID";
    private final long FIXED_SEED = 12345L; // Use a fixed seed for deterministic behavior

    @Before
    public void setUp() {
        // Use a real CRandom object with a fixed seed to make its output predictable.
        predictableRandom = new CRandom(FIXED_SEED);
        // Set this predictable instance for the current test thread.
        //ThreadLocalCRandom.threadLocalRandom.set(predictableRandom);

        when(customerSelector.randomCustomer()).thenReturn(fakeCustomer);
    }

    @Test
    public void testGenerate_ByTaxIdPath() {
        // With seed 12345, the first call to rndPercent(50) returns true.
        when(customerSelector.getTaxId(fakeCustomer.cId)).thenReturn(fakeTaxId);

        TxCustomerPositionInput input = generator.generateCustomerPositionInput();

        assertEquals(fakeTaxId, input.tax_id);
        assertEquals(0, input.cust_id);
    }

    @Test
    public void testGenerate_ByCustIdPath() {
        // To test the "false" path, we advance the generator once.
        predictableRandom.rndPercent(100); // This call consumes the "true" result from the seed.
        // The next call to rndPercent(50) will now return false.

        TxCustomerPositionInput input = generator.generateCustomerPositionInput();

        assertNull(input.tax_id);
        assertEquals(fakeCustomer.cId, input.cust_id);
    }
    
    @Test
    public void testGenerate_GetHistoryPath() {
        // With seed 12345:
        // 1st call to rndPercent(50) [by_tax_id] is true.
        // 2nd call to rndPercent(50) [get_history] is false.
        // To test get_history = true, we need a different seed.
        predictableRandom = new CRandom(1L); // A different seed.
        //ThreadLocalCRandom.threadLocalRandom.set(predictableRandom);
        // With seed 1:
        // 1st call to rndPercent(50) is false.
        // 2nd call to rndPercent(50) is true.
        
        when(customerSelector.getNumberOfAccounts(fakeCustomer)).thenReturn(5);
        // The next random call is rndIntRange(0, 4), which for this seed state gives 3.

        TxCustomerPositionInput input = generator.generateCustomerPositionInput();

        // Verifies the path: by_cust_id=true, get_history=true
        assertNull(input.tax_id);
        assertEquals(fakeCustomer.cId, input.cust_id);
        assertTrue(input.get_history);
        assertEquals(3, input.acct_id_idx);
    }
}

