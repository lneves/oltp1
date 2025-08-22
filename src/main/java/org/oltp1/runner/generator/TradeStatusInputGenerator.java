package org.oltp1.runner.generator;

import org.oltp1.runner.model.RandomCustomer;
import org.oltp1.runner.tx.trade_status.TxTradeStatusInput;

public class TradeStatusInputGenerator
{
	private final CustomerSelector customerSelector;

	public TradeStatusInputGenerator(CustomerSelector customerSelector)
	{
		this.customerSelector = customerSelector;
	}

	public TxTradeStatusInput generateTradeStatusInput()
	{
		// From the spec:
		// acct_id:
		// A single customer is chosen non-uniformly by customer tier, from the range of
		// available customers. The rules for determining the range of available
		// customers are described in clause 3.2.2.1.
		// A single customer account id, as defined by CA_ID in CUSTOMER_ACCOUNT,
		// is chosen at random, uniformly, from the range of customer account ids for
		// the chosen customer

		TxTradeStatusInput input = new TxTradeStatusInput();

		// Select a non-uniform random customer to get their ID and tier.
		RandomCustomer customer = customerSelector.randomCustomer();

		// Select a random account ID belonging to that customer.
		input.acct_id = customerSelector.randomAccId(customer);

		return input;
	}
}