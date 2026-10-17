package org.oltp1.runner.runtime;

import java.util.HashMap;
import java.util.Map;

public class TxSession
{
	private final Map<String, Object> holder = new HashMap<>();

	public Map<String, Object> getSessionData()
	{
		return holder;
	}

	public Object get(String prop)
	{
		return holder.get(prop);
	}

	public void put(String name, Object value)
	{
		holder.put(name, value);
	}

	public void putAll(Map<String, Object> data)
	{
		holder.putAll(data);
	}

	public int getAsInt(String prop)
	{
		return Converter.getAsInt(holder.get(prop));
	}

	public long getAsLong(String prop)
	{
		return Converter.getAsLong(holder.get(prop));
	}

	public double getAsDouble(String prop)
	{
		return Converter.getAsDouble(holder.get(prop));
	}

	public boolean getAsBoolean(String prop)
	{
		return Converter.getAsBoolean(holder.get(prop));
	}

	public String getAsString(String prop)
	{
		return Converter.getAsString(holder.get(prop));
	}
}