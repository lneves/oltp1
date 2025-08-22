package org.oltp1.egen.io;

import java.io.IOException;

import org.oltp1.egen.util.DateTime;

public interface AppendableRow
{
	public void writeObject(Appendable out) throws IOException;
	
	default public void write(Appendable out, String value) throws IOException
	{
		out.append(value);
	}

	// YYYY-MM-DD HH:mm:ss.SSS
	default public void writeDateTime(Appendable out, DateTime value) throws IOException
	{
		AppendableDateTime.appendDateTime(out, value);
	}

	// YYYY-MM-DD
	default public void writeDate(Appendable out, DateTime value) throws IOException
	{
		AppendableDateTime.appendDate(out, value);
	}

	default public void write(Appendable out, char value) throws IOException
	{
		out.append(value);
	}

	default public void write(Appendable out, int value) throws IOException
	{
		out.append(Integer.toString(value));
	}

	default public void write(Appendable out, long value) throws IOException
	{
		out.append(Long.toString(value));
	}

	// formats doubles with two decimal places
	default public void write(Appendable out, double value) throws IOException
	{
		if (!Double.isFinite(value))
			throw new IllegalArgumentException("value must be finite");

		long cents = roundToCentsHalfUp(value); // exact BigDecimal-like behavior, fast

		if (cents == 0)
		{ // avoid "-0.00"
			out.append("0.00");
			return;
		}

		if (cents < 0)
			out.append('-');
		long abs = Math.abs(cents);
		long intPart = abs / 100;
		int frac = (int) (abs % 100);

		out.append(Long.toString(intPart));
		out.append('.');
		if (frac < 10)
			out.append('0');
		out.append(Integer.toString(frac));
	}

	/*
	 * Round to 2 decimals with decimal HALF_UP semantics, without BigDecimal.
	 * Strategy: integer-domain rounding of v*100, plus a small near-0.5 correction
	 * so that "9.995", "1.115", etc. behave like BigDecimal.
	 */
	private long roundToCentsHalfUp(double v)
	{
		if (v == 0.0)
			return 0L;

		long bits = Double.doubleToRawLongBits(v);
		int sign = ((bits >>> 63) == 0) ? 1 : -1;
		int expBits = (int) ((bits >>> 52) & 0x7FF);
		long frac = bits & 0x000F_FFFF_FFFF_FFFFL;

		// v = sign * m * 2^e2
		long m;
		int e2;
		if (expBits == 0)
		{ // subnormal
			if (frac == 0)
				return 0L; // ±0.0
			m = frac;
			e2 = -1074;
		}
		else
		{ // normal
			m = (1L << 52) | frac; // implicit 1
			e2 = expBits - 1075; // (exp - 1023) - 52
		}

		long n = m * 100L; // exact integer scale by 100

		long mag; // |rounded(v*100)|
		if (e2 >= 0)
		{
			// Left shift; if it can't fit in a long crap out
			if (e2 >= 63 || (n >>> (63 - e2)) != 0)
			{
				throw new ArithmeticException("value too large for this method.");
			}
			mag = n << e2;
		}
		else
		{
			int k = -e2;

			if (k >= 63)
			{
				// Extremely small values: at most 1 cent after rounding.
				double s = Math.abs(v) * 100.0;
				double f = s - Math.floor(s);
				boolean roundUp = (f > 0.5) || isNearHalfTie(s);
				mag = (long) Math.floor(s) + (roundUp ? 1 : 0);
			}
			else
			{
				long q = n >>> k; // floor(n / 2^k)
				long r = n & ((1L << k) - 1); // remainder
				long half = 1L << (k - 1);

				// Normal half-up on the actual binary value
				if (r >= half)
					q += 1;

				// Near-half correction: if s is within ~2 ulps of .5, treat as a true tie and
				// round away from zero
				double s = Math.abs(v) * 100.0;
				if (isNearHalfTie(s) && r < half)
				{
					q += 1;
				}
				mag = q;
			}
		}
		return (sign > 0) ? mag : -mag;
	}

	// Treat fractional parts within ~2 ulps of 0.5 as ties (matches BigDecimal
	// behavior for 9.995, 1.115, etc.)
	private boolean isNearHalfTie(double s)
	{
		double f = s - Math.floor(s);
		return Math.abs(f - 0.5) <= Math.ulp(s) * 2.0;
	}
}
