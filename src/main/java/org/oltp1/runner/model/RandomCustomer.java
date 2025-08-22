package org.oltp1.runner.model;

/**
 * A simple data holder for the results of customer generation.
 */
public class RandomCustomer
{
	public final long cId;
	public final CustomerTier cTier;

	public RandomCustomer(long cId, CustomerTier cTier)
	{
		this.cId = cId;
		this.cTier = cTier;
	}
}