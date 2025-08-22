package org.oltp1.egen.util;

/**
 * Money type that keeps all calculations in integer number of cents. Needed for
 * consistency and to avoid floating-point precision issues. This is a Java port
 * of the original C++ CMoney class.
 */
public class Money implements Comparable<Money>
{
	private long amountInCents; // dollar amount * 100

	/**
	 * Default constructor - initialize to $0
	 */
	public Money()
	{
		this.amountInCents = 0;
	}

	/**
	 * Copy constructor - initialize from another Money object
	 */
	public Money(Money other)
	{
		if (other == null)
		{
			this.amountInCents = 0;
		}
		else
		{
			this.amountInCents = other.amountInCents;
		}
	}

	/**
	 * Initialize Money from double (dollar amount)
	 */
	public Money(double amount)
	{
		// Round floating-point number correctly
		this.amountInCents = (long) (100.0 * amount + 0.5);
	}

	/**
	 * Return amount in dollars as a double (e.g., 123.99)
	 */
	public double dollarAmount()
	{
		return amountInCents / 100.0;
	}

	/**
	 * Return amount in integer cents (e.g., 12399)
	 */
	public long centsAmount()
	{
		return amountInCents;
	}

	// Arithmetic operations with Money objects

	public Money add(Money other)
	{
		Money r = new Money(this);
		if (other != null)
			r.amountInCents += other.amountInCents;
		return r;
	}

	public Money addEquals(Money other)
	{
		if (other != null)
			this.amountInCents += other.amountInCents;
		return this;
	}

	public Money subtract(Money other)
	{
		Money r = new Money(this);
		if (other != null)
			r.amountInCents -= other.amountInCents;
		return r;
	}

	public Money subtractEquals(Money other)
	{
		if (other != null)
			this.amountInCents -= other.amountInCents;
		return this;
	}

	// Define arithmetic operations on CMoney and int

	public Money multiply(int scalar)
	{
		Money result = new Money(this);
		result.amountInCents *= scalar;
		return result;
	}

	public Money multiply(long scalar)
	{
		Money result = new Money(this);
		result.amountInCents *= scalar;
		return result;
	}

	// Define arithmetic operations on Money and double

	public Money add(double scalar)
	{
		Money result = new Money(this);
		result.amountInCents += (long) (100.0 * scalar + 0.5);
		return result;
	}

	public Money addEquals(double dollarAmount)
	{
		this.amountInCents += (long) (100.0 * dollarAmount + 0.5);
		return this;
	}

	public Money subtract(double scalar)
	{
		Money result = new Money(this);
		result.amountInCents -= (long) (100.0 * scalar + 0.5);
		return result;
	}

	public Money subtractEquals(double dollarAmount)
	{
		this.amountInCents -= (long) (100.0 * dollarAmount + 0.5);
		return this;
	}

	public Money multiply(double scalar)
	{
		Money result = new Money(this);

		if (result.amountInCents > 0)
		{
			result.amountInCents = (long) (result.amountInCents * scalar + 0.5);
		}
		else
		{
			result.amountInCents = (long) (result.amountInCents * scalar - 0.5);
		}

		return result;
	}

	public Money divide(double scalar)
	{
		if (scalar == 0.0)
		{
			throw new ArithmeticException("Division by zero");
		}

		Money result = new Money(this);

		if (result.amountInCents > 0)
		{
			result.amountInCents = (long) (result.amountInCents / scalar + 0.5);
		}
		else
		{
			result.amountInCents = (long) (result.amountInCents / scalar - 0.5);
		}

		return result;
	}

	// Comparison methods

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		Money money = (Money) obj;
		return amountInCents == money.amountInCents;
	}

	@Override
	public int hashCode()
	{
		return Long.hashCode(amountInCents);
	}

	@Override
	public int compareTo(Money other)
	{
		if (other == null)
		{
			throw new NullPointerException("Cannot compare to null Money object");
		}
		return Long.compare(this.amountInCents, other.amountInCents);
	}

	public boolean greaterThan(Money other)
	{
		if (other == null)
			return true;
		return this.amountInCents > other.amountInCents;
	}

	public boolean greaterThanOrEqual(Money other)
	{
		if (other == null)
			return true;
		return this.amountInCents >= other.amountInCents;
	}

	public boolean lessThan(Money other)
	{
		if (other == null)
			return false;
		return this.amountInCents < other.amountInCents;
	}

	public boolean lessThanOrEqual(Money other)
	{
		if (other == null)
			return false;
		return this.amountInCents <= other.amountInCents;
	}

	// Utility methods

	@Override
	public String toString()
	{
		return String.format("$%.2f", dollarAmount());
	}

	/**
	 * Format as currency string with custom format
	 */
	public String format(String pattern)
	{
		return String.format(pattern, dollarAmount());
	}

	/**
	 * Get the absolute value of this Money object
	 */
	public Money abs()
	{
		Money result = new Money(this);
		result.amountInCents = Math.abs(result.amountInCents);
		return result;
	}

	/**
	 * Negate the value (multiply by -1)
	 */
	public Money negate()
	{
		Money result = new Money(this);
		result.amountInCents = -result.amountInCents;
		return result;
	}

	/**
	 * Check if the amount is zero
	 */
	public boolean isZero()
	{
		return amountInCents == 0;
	}

	/**
	 * Check if the amount is positive
	 */
	public boolean isPositive()
	{
		return amountInCents > 0;
	}

	/**
	 * Check if the amount is negative
	 */
	public boolean isNegative()
	{
		return amountInCents < 0;
	}

	/**
	 * Get the minimum of two Money objects
	 */
	public static Money min(Money a, Money b)
	{
		if (a == null)
			return b;
		if (b == null)
			return a;
		return a.lessThan(b) ? new Money(a) : new Money(b);
	}

	/**
	 * Get the maximum of two Money objects
	 */
	public static Money max(Money a, Money b)
	{
		if (a == null)
			return b;
		if (b == null)
			return a;
		return a.greaterThan(b) ? new Money(a) : new Money(b);
	}
}