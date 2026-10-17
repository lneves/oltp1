package org.oltp1.runner.model;

import java.io.Serializable;

public class Company implements Serializable
{
	private static final long serialVersionUID = -4090216740689456057L;
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
		return String.format("Company [s_symb=%s, s_issue=%s, co_id=%s,co_name=%s]", symbol, issue, coId, coName);
	}
}