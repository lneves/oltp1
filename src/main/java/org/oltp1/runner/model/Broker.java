package org.oltp1.runner.model;

public class Broker
{
	private final long id;
	private final String statusId;
	private final String name;

	public Broker(long id, String statusId, String name)
	{
		super();
		this.id = id;
		this.statusId = statusId;
		this.name = name;
	}

	public long getId()
	{
		return id;
	}

	public String getStatusId()
	{
		return statusId;
	}

	public String getName()
	{
		return name;
	}

	@Override
	public String toString()
	{
		return String.format("Broker {b_id=%s, b_name=%s, b_st_id=%s}", id, name, statusId);
	}
}