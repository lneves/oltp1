package org.oltp1.runner.perf;

import java.math.BigDecimal;
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

	public int getAsInt(String prop)
	{
		return fromInt2(holder.get(prop));
	}

	public long getAsLong(String prop)
	{
		return ((Number) holder.get(prop)).longValue();
	}

	public double getAsDouble(String prop)
	{
		if (holder.get(prop) instanceof Double)
		{
			return ((Double) holder.get(prop)).doubleValue();
		}
		else if (holder.get(prop) instanceof BigDecimal)
		{
			return ((BigDecimal) holder.get(prop)).doubleValue();
		}
		else
		{
			throw new IllegalArgumentException(String.format("invalid type coercion for property: %s", prop));
		}
	}

	public boolean getAsBoolean(String prop)
	{
		return ((Boolean) holder.get(prop)).booleanValue();
	}

	public String getAsString(String prop)
	{
		return (String) holder.get(prop);
	}

	public void put(String name, Object value)
	{
		holder.put(name, value);
	}

	public void putAll(Map<String, Object> data)
	{
		holder.putAll(data);
	}

	private static int fromInt2(Object o)
	{
		if (o instanceof Short)
		{
			return ((Short) o).intValue();
		}
		else if (o instanceof Integer)
		{
			return ((Integer) o).intValue();
		}
		else if (o instanceof Boolean)
		{
			boolean b = ((Boolean) o).booleanValue();
			return (b ? 1 : 0);  // 1 for true, 0 for false
		}

		throw new IllegalArgumentException("invalid data type");
	}
}