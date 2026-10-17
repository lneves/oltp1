package org.oltp1.runner.model;

public class Industry
{
	private final String inId;
	private final String inName;
	private final String inScId;

	public Industry(String inId, String inName, String inScId)
	{
		super();
		this.inId = inId;
		this.inName = inName;
		this.inScId = inScId;
	}

	public String getInId()
	{
		return inId;
	}

	public String getInName()
	{
		return inName;
	}

	public String getInScId()
	{
		return inScId;
	}

	@Override
	public String toString()
	{
		return String.format("Industry [in_id=%s, in_name=%s, in_sc_id=%s]", inId, inName, inScId);
	}
}