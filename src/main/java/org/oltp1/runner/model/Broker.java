package org.oltp1.runner.model;

import java.util.Objects;

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

	@Override
	public int hashCode()
	{
		return Objects.hash(Long.valueOf(id));
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Broker other = (Broker) obj;
		return id == other.id;
	}

}