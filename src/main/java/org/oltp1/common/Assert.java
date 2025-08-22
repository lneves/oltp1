package org.oltp1.common;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

public class Assert
{
	public static void notNull(String name, Object obj)
	{
		if (obj == null)
		{
			throw new IllegalArgumentException(String.format("'%s' can not be null", name));
		}
	}

	public static void notBlank(String name, String value)
	{
		if (StringUtils.isBlank(value))
		{
			throw new IllegalArgumentException(String.format("'%s' can not be blank", name));
		}
	}

	public static void notEmpty(String name, Collection<?> value)
	{
		notNull(name, value);
		if (value.isEmpty())
		{
			throw new IllegalArgumentException(String.format("'%s' can not be empty", name));
		}
	}

	public static void minSize(String name, Collection<?> value, int minSize)
	{
		notEmpty(name, value);
		if (value.size() < minSize)
		{
			throw new IllegalArgumentException(String.format("'%s' must hold at least %s elements", name, minSize));
		}
	}

	public static void isInRange(String pname, double v, double min, double max)
	{
		if ((v < min) || (v > max))
		{
			throw new IllegalArgumentException(String.format("'%s' must be in range %s to %s ", pname, min, max));
		}
	}
}
