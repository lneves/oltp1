package org.oltp1.runner.model;

public class Sector
{
	private final String id;
	private final String name;

	public Sector(String id, String name)
	{
		super();
		this.id = id;
		this.name = name;
	}

	public String getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	@Override
	public String toString()
	{
		return String.format("Sector {sc_id=%s, sc_name=%s}", id, name);
	}
}
