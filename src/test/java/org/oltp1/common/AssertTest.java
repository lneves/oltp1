package org.oltp1.common;

import org.junit.Test;

public class AssertTest
{

	// notNull
	@Test
	public void notNull_withNonNullObject_shouldPass()
	{
		Assert.notNull("testObject", new Object());
	}

	@Test(expected = IllegalArgumentException.class)
	public void notNull_withNullObject_shouldThrowException()
	{
		Assert.notNull("testObject", null);
	}

	// notBlank
	@Test
	public void notBlank_withValidString_shouldPass()
	{
		Assert.notBlank("testString", "some value");
	}

	@Test(expected = IllegalArgumentException.class)
	public void notBlank_withNullString_shouldThrowException()
	{
		Assert.notBlank("testString", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void notBlank_withEmptyString_shouldThrowException()
	{
		Assert.notBlank("testString", "");
	}

	@Test(expected = IllegalArgumentException.class)
	public void notBlank_withWhitespaceString_shouldThrowException()
	{
		Assert.notBlank("testString", "   ");
	}

	// isInRange
	@Test
	public void isInRange_withValueInRange_shouldPass()
	{
		Assert.isInRange("testValue", 5.0, 0.0, 10.0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void isInRange_withValueBelowRange_shouldThrowException()
	{
		Assert.isInRange("testValue", -1.0, 0.0, 10.0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void isInRange_withValueAboveRange_shouldThrowException()
	{
		Assert.isInRange("testValue", 11.0, 0.0, 10.0);
	}
}
