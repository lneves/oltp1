package org.oltp1.runner.generator;

import org.oltp1.runner.model.RandomCustomer;
import org.oltp1.runner.tx.customer_position.TxCustomerPositionInput;

public class CustomerPositionInputGenerator
{
	// Default values from DriverParamSettings.h for a compliant run
	private static final int CP_PERCENT_BY_TAX_ID = 50;
	private static final int CP_PERCENT_GET_HISTORY = 50;

	private CustomerSelector customerSelector;

	public CustomerPositionInputGenerator(CustomerSelector customerSelector)
	{
		this.customerSelector = customerSelector;
	}

	public TxCustomerPositionInput generateCustomerPositionInput()
	{
		TxCustomerPositionInput txInput = new TxCustomerPositionInput();

		CRandom random = ThreadLocalCRandom.get();

		// by_tax_id:
		// 1 50% 48% to 52%
		// get_history:
		// 1 50% 48% to 52%

		// Select a non-uniform random customer and their tier.
		RandomCustomer customer = customerSelector.randomCustomer();
		long customerId = customer.cId;

		// Decide whether to identify the customer by tax_id or cust_id.
		if (random.rndPercent(CP_PERCENT_BY_TAX_ID))
		{
			// Use tax_id for the lookup.
			txInput.tax_id = customerSelector.getTaxId(customerId);
		}
		else
		{
			// Use cust_id for the lookup.
			txInput.cust_id = customerId;
		}

		// Decide whether to request the account history.
		txInput.get_history = random.rndPercent(CP_PERCENT_GET_HISTORY);

		// If getting history, select a random account index for that customer.
		if (txInput.get_history)
		{
			int numAccounts = customerSelector.getNumberOfAccounts(customer);
			txInput.acct_id_idx = random.rndIntRange(0, numAccounts - 1);
		}
		else
		{
			txInput.acct_id_idx = -1; // Not used
		}

		return txInput;
	}
}