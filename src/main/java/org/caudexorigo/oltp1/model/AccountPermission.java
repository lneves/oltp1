package org.caudexorigo.oltp1.model;

import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.bytes.BytesOut;

public class AccountPermission implements BytesMarshallable
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
		return String.format("AccountPermission [taxId=%s, lName=%s, fName=%s, isOwner=%s]", taxId, lName, fName, isOwner);
	}

	@Override
	public void writeMarshallable(BytesOut<?> out)
	{
		out.writeUtf8(taxId);
		out.writeUtf8(fName);
		out.writeUtf8(lName);
		out.writeBoolean(isOwner);
	}

	@Override
	public void readMarshallable(BytesIn<?> in)
	{
		taxId = in.readUtf8();
		fName = in.readUtf8();
		lName = in.readUtf8();
		isOwner = in.readBoolean();
	}
}
