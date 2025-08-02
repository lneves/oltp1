package org.caudexorigo.oltp1.model;

import java.util.List;

import org.caudexorigo.oltp1.QuietExecutor;

import net.openhft.chronicle.map.ChronicleMap;

public class AccountPermissionStore
{
	private final ChronicleMap<Long, AccountPermissionList> map;

	public AccountPermissionStore(long estimatedEntries)
	{
		this.map = QuietExecutor.callQuietly(() -> { // silence the output of ChronicleMap

			return ChronicleMap
					.of(Long.class, AccountPermissionList.class)
					.name("account-permission-store")
					.averageValue(new AccountPermissionList())
					.entries(estimatedEntries)
					.create(); // in-memory, off-heap
		}

		);
	}

	public void addToMultimap(Long accId, AccountPermission ap)
	{
		AccountPermissionList list = map.get(accId);
		if (list == null)
		{
			list = new AccountPermissionList();
		}
		list.add(ap);
		map.put(accId, list);
	}

	public List<AccountPermission> get(Long accId)
	{
		AccountPermissionList list = map.get(accId);
		return list != null ? list.getList() : List.of();
	}

	public void close()
	{
		map.close();
	}

	public int size()
	{
		return map.size();
	}
}
