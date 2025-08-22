package org.oltp1.runner.model;

public class AccountPermission
{
	public String taxId;
	public String fName;
	public String lName;
	public boolean isOwner;

	public AccountPermission()
	{
		super();
	}

	public AccountPermission(String taxId, String fName, String lName, boolean isOwner)
	{
		super();
		this.taxId = taxId;
		this.fName = fName;
		this.lName = lName;
		this.isOwner = isOwner;
	}

	@Override
	public String toString()
	{
		return String.format("AccountPermission [taxId=%s, lName=%s, fName=%s, isOwner=%s]%n", taxId, lName, fName, isOwner);
	}
}
