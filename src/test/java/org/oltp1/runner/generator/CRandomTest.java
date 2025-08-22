package org.oltp1.runner.generator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CRandomTest
{

	@Test
	public void testRndIntRange_Bounds()
	{
		CRandom random = new CRandom(12345L);
		int min = 10;
		int max = 20;
		for (int i = 0; i < 1000; i++)
		{
			long result = random.rndIntRange(min, max);
			assertTrue("Result should be >= min", result >= min);
			assertTrue("Result should be <= max", result <= max);
		}
	}

	@Test
	public void testRndIntRange_SingleValue()
	{
		CRandom random = new CRandom(12345L);
		assertEquals("Should return the same value when min equals max", 10, random.rndIntRange(10, 10));
	}

	@Test
	public void testNonUniformRandom_Range()
	{
		CRandom random = new CRandom(54321L);
		long P = 100;
		long Q = 200;
		int A = 255;
		int s = 8;
		for (int i = 0; i < 1000; i++)
		{
			long result = random.nonUniformRandom(P, Q, A, s);
			assertTrue("Result should be >= P", result >= P);
			assertTrue("Result should be <= Q", result <= Q);
		}
	}

	@Test
	public void testRndNthElement()
	{
		long seed = 80534927L;
		CRandom cRnd1 = new CRandom(seed);
		CRandom cRnd2 = new CRandom(seed);

		// Advance cRnd1 by 100 steps
		long expected = 0;
		for (int i = 0; i < 100; i++)
		{
			expected = cRnd1.rndInt64Range(0, 1000);
		}

		// Use RndNthElement to get the 99th element (since rndInt64Range advances the
		// seed first)
		long nthSeed = cRnd2.rndNthElement(seed, 99);
		cRnd2.setSeed(nthSeed);
		long actual = cRnd2.rndInt64Range(0, 1000);

		assertEquals("The 100th random number should match", expected, actual);
	}
}
