package org.oltp1.egen.model;

public class AccountSecurity
{
	public final long caId;
	public final long securityFlatFileIndex;
	public final int securityAccountIndex;

	public AccountSecurity(long caId, long securityFlatFileIndex, int securityAccountIndex)
	{
		this.caId = caId;
		this.securityFlatFileIndex = securityFlatFileIndex;
		this.securityAccountIndex = securityAccountIndex;
	}

}
