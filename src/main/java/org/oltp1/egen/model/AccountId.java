package org.oltp1.egen.model;

public class AccountId
{
	public final long caId;
	public final int accCount;

	public AccountId(long caId, int accCount)
	{
		this.caId = caId;
		this.accCount = accCount;
	}
}
