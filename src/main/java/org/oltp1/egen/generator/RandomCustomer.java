package org.oltp1.egen.generator;

import org.oltp1.egen.model.CustomerTier;

public class RandomCustomer
{
	private final long customerId;
	private final CustomerTier customerTier;

	public RandomCustomer(long customerId, CustomerTier customerTier)
	{
		this.customerId = customerId;
		this.customerTier = customerTier;
	}

	public long getCustomerId()
	{
		return customerId;
	}

	public CustomerTier getCustomerTier()
	{
		return customerTier;
	}
}
