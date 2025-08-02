package org.caudexorigo.oltp1.model;

import org.caudexorigo.oltp1.QuietExecutor;

import net.openhft.chronicle.map.ChronicleMap;

public class CompanyStore
{
	private final ChronicleMap<Integer, Company> ixMap;
	private final ChronicleMap<String, Company> symbolMap;

	public CompanyStore(long estimatedEntries)
	{
		this.ixMap = QuietExecutor.callQuietly(() -> { // silence the output of ChronicleMap

			return ChronicleMap
					.of(Integer.class, Company.class)
					.name("costumer-id-store")
					.averageValue(new Company())
					.entries(estimatedEntries)
					.create(); // in-memory, off-heap
		});

		this.symbolMap = QuietExecutor.callQuietly(() -> {

			return ChronicleMap
					.of(String.class, Company.class)
					.name("costumer-symbol-store")
					.averageKeySize(15)
					.averageValue(new Company())
					.entries(estimatedEntries)
					.create(); // in-memory, off-heap
		});
	}

	public void add(Integer ix, String symbol, Company c)
	{
		ixMap.put(ix, c);
		symbolMap.put(symbol, c);
	}

	public Company getCompanyByIndex(Integer ix)
	{
		return ixMap.get(ix);
	}

	public Company getCompanyBySymbol(String symbol)
	{
		return symbolMap.get(symbol);
	}

	public void close()
	{
		ixMap.close();
		symbolMap.close();
	}
}