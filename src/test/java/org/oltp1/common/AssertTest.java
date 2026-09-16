package org.oltp1.common;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class AssertTest
{

	// notNull
	@Test
	public void notNull_withNonNullObject_shouldPass()
	{
		Assert.notNull("testObject", new Object());
	}

	@Test
	void notNull_withNullObject_shouldThrowException()
	{
		assertThrows(
				IllegalArgumentException.class,
				() -> Assert.notNull("testObject", null));
	}

	// notBlank
	@Test
	public void notBlank_withValidString_shouldPass()
	{
		Assert.notBlank("testString", "some value");
	}

    @Test
    void notBlank_withNullString_shouldThrowException() {
        assertThrows(
            IllegalArgumentException.class,
            () -> Assert.notBlank("testString", null)
        );
    }

    @Test
    void notBlank_withEmptyString_shouldThrowException() {
        assertThrows(
            IllegalArgumentException.class,
            () -> Assert.notBlank("testString", "")
        );
    }

    @Test
    void notBlank_withWhitespaceString_shouldThrowException() {
        assertThrows(
            IllegalArgumentException.class,
            () -> Assert.notBlank("testString", "   ")
        );
    }

    @Test
    void isInRange_withValueBelowRange_shouldThrowException() {
        assertThrows(
            IllegalArgumentException.class,
            () -> Assert.isInRange("testValue", -1.0, 0.0, 10.0)
        );
    }

    @Test
    void isInRange_withValueAboveRange_shouldThrowException() {
        assertThrows(
            IllegalArgumentException.class,
            () -> Assert.isInRange("testValue", 11.0, 0.0, 10.0)
        );
    }
}
