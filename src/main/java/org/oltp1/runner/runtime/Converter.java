package org.oltp1.runner.runtime;

import java.math.BigDecimal;

public class Converter
{
	public static int getAsInt(Object value)
	{
		return fromInt2(value);
	}

	public static long getAsLong(Object value)
	{
		if (value == null)
		{
			return 0l;
		}
		return ((Number) value).longValue();
	}

	public static double getAsDouble(Object value)
	{
		if (value == null)
		{
			return 0.0;
		}

		if (value instanceof Double)
		{
			return ((Double) value).doubleValue();
		}
		else if (value instanceof BigDecimal)
		{
			return ((BigDecimal) value).doubleValue();
		}
		else
		{
			throw new IllegalArgumentException(String.format("invalid type coercion for object: %s", value.toString()));
		}
	}

	public static boolean getAsBoolean(Object value)
	{
		if (value == null)
		{
			return false;
		}

		return ((Boolean) value).booleanValue();
	}

	public static String getAsString(Object value)
	{
		return (String) value;
	}

	private static int fromInt2(Object o)
	{
		if (o == null)
		{
			return 0;
		}

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
			return (b ? 1 : 0); // 1 for true, 0 for false
		}

		throw new IllegalArgumentException("invalid data type");
	}
}