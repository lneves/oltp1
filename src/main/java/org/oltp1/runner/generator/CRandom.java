package org.oltp1.runner.generator;

import java.util.List;

/**
 * A Java port of the TPC-E EGen CRandom class. This class implements a specific
 * 64-bit linear congruential generator (LCG) required for compliant data
 * generation.
 *
 * Based on Utilities/inc/Random.h and Utilities/inc/BigMath.h
 */
public final class CRandom
{
	// Constants from Random.h and BigMath.h
	private static final long A_MULTIPLIER = 6364136223846793005L;

	private static final long C_INCREMENT = 1L;

	private static final long MASK32 = 0xFFFFFFFFL;
	private static final int UPPER32 = 32;

	private long seed;

	public CRandom(long seed)
	{
		setSeed(seed);
	}

	public long getSeed()
	{
		return this.seed;
	}

	public void setSeed(long seed)
	{
		this.seed = seed;
	}

	/**
	 * Advances the random number generator's state by one step.
	 * <p>
	 * <b>Note on Unsigned 64-bit Arithmetic:</b><br>
	 * This method is a direct port of the {@code UInt64Rand} function from the
	 * TPC-E EGen C++ source, which uses unsigned 64-bit integers. Java does not
	 * have an unsigned {@code long}, but this implementation is correct because
	 * Java's signed {@code long} arithmetic is defined to wrap around on overflow
	 * (two's complement arithmetic).
	 * <p>
	 * For the multiplication and addition operations used in this Linear
	 * Congruential Generator, the wrap-around behavior produces the <b>exact same
	 * bit patterns</b> as C++ unsigned 64-bit arithmetic. This bit-level
	 * equivalence is critical for generating a compliant and reproducible sequence
	 * of random numbers. The name {@code uint64Rand} is preserved to maintain a
	 * clear correspondence with the TPC-E reference implementation.
	 *
	 * @return The next 64-bit random seed in the sequence.
	 */
	private long uint64Rand()
	{
		this.seed = (this.seed * A_MULTIPLIER) + C_INCREMENT;
		return this.seed;
	}

	/**
	 * Replicates the C++ Mul6464WithShiftRight64 function for 128-bit
	 * multiplication. In Java, we can use the Math.multiplyHigh() method (available
	 * since Java 9) for a more direct and efficient 128-bit multiplication. For
	 * compatibility, this version mimics the C++ bit-shifting logic.
	 */
	private static long multiplyHigh(long x, long y)
	{
		long x_low = x & MASK32;
		long x_high = x >>> UPPER32;
		long y_low = y & MASK32;
		long y_high = y >>> UPPER32;

		long p0 = x_low * y_low;
		long p1 = x_high * y_low;
		long p2 = x_low * y_high;
		long p3 = x_high * y_high;

		long carry = ((p0 >>> UPPER32) + (p1 & MASK32) + (p2 & MASK32)) >>> UPPER32;

		return (p1 >>> UPPER32) + (p2 >>> UPPER32) + p3 + carry;
	}

	/**
	 * Generates a random long value within the specified range [min, max].
	 */
	public long rndInt64Range(long min, long max)
	{
		if (max <= min)
		{
			return min;
		}
		long range = (max - min) + 1;
		if (range <= 1)
		{
			return min;
		}
		uint64Rand(); // Advance the seed
		return min + multiplyHigh(this.seed, range);
		// return min + Math.multiplyHigh(this.seed, range);
	}

	public int rndIntRange(int min, int max)
	{
		return (int) rndInt64Range(min, max);
	}

	/**
	 * Returns a random double value in the range of [min, max] with 'incr'
	 * precision.
	 */
	public double rndDoubleIncrRange(double min, double max, double incr)
	{
		long width = (long) ((max - min) / incr);
		return min + ((double) rndInt64Range(0, width) * incr); //
	}

	/**
	 * Returns true with a probability of (percent / 100).
	 */
	public boolean rndPercent(int percent)
	{
		return (rndIntRange(1, 100) <= percent);
	}

	public <T> T rndChoice(List<T> lst)
	{
		return lst.get(rndIntRange(0, lst.size() - 1));
	}

	/**
	 * Returns a non-uniform random 64-bit integer in range of [P .. Q].
	 * 
	 * This is a direct port of the NURnd function.
	 * 
	 * NURnd is used to create a skewed data access pattern. The function is similar
	 * to NURand in TPC-C. (The two functions are identical when C=0 and s=0.)
	 * 
	 * The parameter A must be of the form 2^k - 1, so that Rnd[0..A] will produce a
	 * k-bit field with all bits having 50/50 probability of being 0 or 1.
	 * 
	 * With a k-bit A value, the weights range from 3^k down to 1 with the number of
	 * equal probability values given by C(k,i) = k! /(i!(k-i)!) for 0 <= i <= k. So
	 * a bigger A value from a larger k has much more skew.
	 * 
	 * Left shifting of Rnd[0..A] by "s" bits gets a larger interval without getting
	 * huge amounts of skew. For example, when applied to elapsed time in
	 * milliseconds, s=10 effectively ignores the milliseconds, while s=16
	 * effectively ignores seconds and milliseconds, giving a granularity of just
	 * over 1 minute (65.536 seconds). A smaller A value can then give the desired
	 * amount of skew at effectively one-minute resolution.
	 * 
	 * @param P
	 *            The lower bound of the range (inclusive).
	 * @param Q
	 *            The upper bound of the range (inclusive).
	 * @param A
	 *            A constant defining the range of the non-uniform component.
	 * @param s
	 *            The number of bits to left-shift the non-uniform component.
	 * @return A non-uniformly distributed random long within the specified range.
	 */
	public long nonUniformRandom(long P, long Q, int A, int s)
	{
		long range = (Q - P) + 1;
		long rndA = rndInt64Range(0, A);
		long rndB = rndInt64Range(P, Q);

		return (((rndB | (rndA << s)) % range) + P);
	}

	/**
	 * Calculates the Nth element in the random sequence from a given seed without
	 * iterating. This is a direct port of the C++ EGen RndNthElement function.
	 *
	 * @param seed
	 *            The starting seed for the sequence.
	 * @param count
	 *            The number of steps to advance (N).
	 * @return The Nth random number seed in the sequence.
	 */
	public long rndNthElement(long seed, long count)
	{
		if (count == 0)
		{
			return seed;
		}

		long a = A_MULTIPLIER;
		long c = C_INCREMENT;

		long aPow = a;
		long dSum = 1L;

		// Find the highest non-zero bit in count.
		int nBit = 0;
		// The C++ loop is `for(nBit = 0; (nCount >> nBit) != 1; nBit++){}` which finds
		// the position of the second-highest bit. A more direct way in Java is to find
		// the
		// highest bit.
		if (count > 1)
		{
			nBit = 63 - Long.numberOfLeadingZeros(count);
		}

		// This loop performs binary exponentiation (exponentiation by squaring)
		// to efficiently calculate the final seed without iterating 'count' times.
		while (--nBit >= 0)
		{
			dSum *= (aPow + 1);
			aPow *= aPow;
			if (((count >> nBit) & 1) == 1)
			{
				dSum += aPow;
				aPow *= a;
			}
		}
		return (seed * aPow) + (dSum * c);
	}

	public static void main(String[] args)
	{
		CRandom rnd = new CRandom(80534927L);
		rnd.uint64Rand();

		System.out.println(rnd.rndNthElement(123456, 33));
	}
}