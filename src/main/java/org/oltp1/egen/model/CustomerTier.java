package org.oltp1.egen.model;

// Enum to represent customer tiers for better type safety
public enum CustomerTier
{
	TIER_ONE(1), TIER_TWO(2), TIER_THREE(3);

	private final int value;

	CustomerTier(int value)
	{
		this.value = value;
	}

	public int getValue()
	{
		return value;
	}

	public static CustomerTier valueOf(int v)
	{

		if (v == 1)
		{
			return TIER_ONE;
		}
		if (v == 2)
		{
			return TIER_TWO;
		}
		if (v == 3)
		{
			return TIER_THREE;
		}
		return TIER_ONE;
	}
}