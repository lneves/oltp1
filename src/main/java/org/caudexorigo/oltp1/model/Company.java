package org.caudexorigo.oltp1.model;

import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.bytes.BytesOut;

public class Company implements BytesMarshallable
{
	public String symbol;
	public String issue;
	public long coId;
	public String coName;

	public Company()
	{
		super();
	}

	public Company(String symbol, String issue, long coId, String coName)
	{
		super();
		this.symbol = symbol;
		this.issue = issue;
		this.coId = coId;
		this.coName = coName;
	}

	public String getSymbol()
	{
		return symbol;
	}

	public String getIssue()
	{
		return issue;
	}

	public long getCoId()
	{
		return coId;
	}

	public String getCoName()
	{
		return coName;
	}

	@Override
	public String toString()
	{
		return String.format("Company [s_symb=%s, s_issue=%s, co_id=%s, co_name=%s]", symbol, issue, coId, coName);
	}

	@Override
	public void writeMarshallable(BytesOut<?> out)
	{
		out.writeUtf8(symbol);
		out.writeUtf8(issue);
		out.writeLong(coId);
		out.writeUtf8(coName);
	}

	@Override
	public void readMarshallable(BytesIn<?> in)
	{
		symbol = in.readUtf8();
		issue = in.readUtf8();
		coId = in.readLong();
		coName = in.readUtf8();
	}
}