
package org.oltp1.egen.util;

/**
 * A precise Java port of the 64-bit Linear Congruential Generator (LCG) from
 * the C++ EGen project. This version uses java.math.BigInteger to correctly
 * handle the 128-bit unsigned arithmetic required for perfect replication of
 * the C++ random sequence.
 * <p>
 * Based on Utilities/inc/Random.h, Utilities/src/Random.cpp, and
 * Utilities/inc/BigMath.h
 */
public class TpcRandom
{
	private static final double UINT64_RAND_RECIPROCAL_2_POWER_64 = 5.421010862427522e-20;

	private static final long MULTIPLIER = 6364136223846793005L;
	private static final long INCREMENT = 1L;

	private static final long MASK32 = 0xFFFFFFFFL;
	private static final int UPPER32 = 32;
	private static final long BIT63 = 0x8000000000000000L;
	private static final long CARRY32 = 0x100000000L;

	private long seed;

	public TpcRandom(long seed)
	{
		setSeed(seed);
	}

	public long getSeed()
	{
		return seed;
	}

	public void setSeed(long seed)
	{
		this.seed = seed;
	}

	private long nextLong()
	{
		seed = (seed * MULTIPLIER) + INCREMENT;
		return seed;
	}

	public int rndIntRange(int min, int max)
	{
		if (max <= min)
			return min;
		int range = max - min + 1;
		if (range <= 1)
			return min;
		nextLong();
		return min + (int) mul6432WithShiftRight64(this.seed, range);
	}

	public long rndInt64Range(long min, long max)
	{
		if (max <= min)
			return min;
		long range = max - min + 1;
		if (range <= 1)
			return min;
		nextLong();
		return min + mul6464WithShiftRight64(this.seed, range);
	}

	public double rndDoubleIncrRange(double min, double max, double incr)
	{
		long width = (long) ((max - min) / incr); // need [0..width], so no +1
		return min + ((double) rndInt64Range(0, width) * incr);
	}

	public int rndIntRangeExclude(int low, int high, int exclude)
	{
		int temp = rndIntRange(low, high - 1);
		return (temp >= exclude) ? temp + 1 : temp;
	}

	public String rndAlphaNumFormatted(String format)
	{
		StringBuilder sb = new StringBuilder(format.length());
		for (char c : format.toCharArray())
		{
			if (c == 'n')
			{
				sb.append("0123456789".charAt(rndIntRange(0, 9)));
			}
			else if (c == 'a')
			{
				sb.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt(rndIntRange(0, 25)));
			}
			else
			{
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public long rndInt64RangeExclude(long low, long high, long exclude)
	{
		long temp;
		temp = rndInt64Range(low, high - 1);
		if (temp >= exclude)
		{
			temp += 1;
		}
		return temp;
	}

	public boolean rndPercent(int percent)
	{
		return (rndIntRange(1, 100) <= percent);
	}

	public double rndDouble()
	{
		return (double) nextLong() * UINT64_RAND_RECIPROCAL_2_POWER_64;
	}

	/*
	 * Returns a random double value from a negative exponential distribution with
	 * the given mean
	 */
	public double rndDoubleNegExp(double mean)
	{
		return ((-1.0 * Math.log(rndDoubleIncrRange(0.0, 1.0, 0.000000000001))) * mean);
	}

	/**
	 * Returns the Nth element in a pseudo-random sequence over a given integer
	 * range. This is a static, stateless function.
	 *
	 * @param seed
	 *            The base seed for the sequence (e.g., RNGSeedBaseBrokerId).
	 * @param n
	 *            The 0-based index of the desired element in the sequence.
	 * @param min
	 *            The minimum value of the range (inclusive).
	 * @param max
	 *            The maximum value of the range (inclusive).
	 * @return The deterministically generated Nth random long in the range.
	 */
	public static long rndNthInt64Range(long seed, long n, long min, long max)
	{
		if (max <= min)
			return min;
		long range = max - min + 1;
		if (range <= 1)
			return min;

		long nthSeed = rndNthElement(seed, n);
		return min + mul6464WithShiftRight64(nthSeed, range);
	}

	// Multiply two 64-bit factors, followed by a right-shift of 64 bits (retaining
	// upper 64-bit quantity)
	// This is implemented as four 64-bit multiplications with summation of partial
	// products and carry.
	public static long mul6464WithShiftRight64(long seed, long range)
	{
		long sl = (seed & MASK32); // lower 32 bits of seed
		long su = (seed >>> UPPER32); // upper 32 bits of seed (unsigned shift)
		long rl = (range & MASK32); // lower 32 bits of range
		long ru = (range >>> UPPER32); // upper 32 bits of range (unsigned shift)

		long p0 = (sl * rl); // partial products
		long p1 = (su * rl);
		long p2 = (sl * ru);
		long p3 = (su * ru);
		long p12Carry = 0;
		long s;

		s = p0;
		s >>>= UPPER32; // unsigned shift
		s += p1;
		p12Carry = (((((p1 & BIT63) != 0) || ((s & BIT63) != 0)) && ((p2 & BIT63) != 0)) ? CARRY32 : 0);
		s += p2;
		s >>>= UPPER32; // unsigned shift
		s += p12Carry;
		s += p3;

		return s;
	}

	// Multiply 64-bit and 32-bit factors, followed by a right-shift of 64 bits
	// (retaining upper 64-bit quantity)
	// This is implemented as two 64-bit multiplications with summation of partial
	// products.
	public static int mul6432WithShiftRight64(long seed, int range)
	{
		long sl = (seed & MASK32); // lower 32 bits of seed
		long su = (seed >>> UPPER32); // upper 32 bits of seed (unsigned shift)
		long rl = Integer.toUnsignedLong(range); // range as unsigned long

		long p0 = (sl * rl); // partial products
		long p1 = (su * rl);
		long s;

		s = p0;
		s >>>= UPPER32; // unsigned shift
		s += p1;
		s >>>= UPPER32; // unsigned shift

		return (int) s;
	}

	public static long rndNthElement(long startSeed, long count)
	{
		if (count == 0)
			return startSeed;

		long a = MULTIPLIER;
		long c = INCREMENT;
		long apow = a;
		long dsum = 1L;
		long tempCount = count;
		int nbit = 63 - Long.numberOfLeadingZeros(tempCount);
		while (--nbit >= 0)
		{
			dsum *= (apow + 1);
			apow *= apow;
			if (((tempCount >>> nbit) & 1) == 1)
			{
				dsum += apow;
				apow *= a;
			}
		}
		return startSeed * apow + dsum * c;
	}
}