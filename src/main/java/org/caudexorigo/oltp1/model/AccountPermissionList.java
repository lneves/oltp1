package org.caudexorigo.oltp1.model;

import java.util.ArrayList;
import java.util.List;

import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.bytes.BytesOut;

public class AccountPermissionList implements BytesMarshallable
{
	private final List<AccountPermission> list = new ArrayList<>();

	public void add(AccountPermission ap)
	{
		list.add(ap);
	}

	public List<AccountPermission> getList()
	{
		return list;
	}

	@Override
	public void writeMarshallable(BytesOut<?> out)
	{
		out.writeInt(list.size());
		for (AccountPermission ap : list)
		{
			ap.writeMarshallable(out);
		}
	}

	@Override
	public void readMarshallable(BytesIn<?> in)
	{
		list.clear();
		int size = in.readInt();
		for (int i = 0; i < size; i++)
		{
			AccountPermission ap = new AccountPermission();
			ap.readMarshallable(in);
			list.add(ap);

			// System.out.printf("read-ap: %s%n", ap);
		}
	}

	@Override
	public String toString()
	{
		return list.toString();
	}
}
