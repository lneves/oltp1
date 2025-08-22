package org.oltp1.runner.generator;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A thread-safe wrapper for the CRandom class using ThreadLocal. Each thread
 * gets its own instance of CRandom, initialized with a unique seed.
 */
public final class ThreadLocalCRandom
{
	// A base seed to start the sequence for all threads.
	// This is the default Txn Input Generator seed from RNGSeeds.h
	private static final long BASE_SEED = 80534927L;

	// An AtomicLong to safely dispense a unique seed to each new thread.
	private static final AtomicLong seedDispenser = new AtomicLong(BASE_SEED);

	// The ThreadLocal that holds a CRandom instance for each thread.
	private static final ThreadLocal<CRandom> threadLocalRandom = new ThreadLocal<>()
	{
		@Override
		protected CRandom initialValue()
		{
			// Get a unique seed for this thread and create a new CRandom instance.
			// getAndIncrement() is an atomic operation, ensuring no two threads get the
			// same seed.
			return new CRandom(seedDispenser.getAndIncrement());
		}
	};

	/**
	 * Returns the CRandom instance for the current thread.
	 *
	 * @return The thread-safe CRandom instance.
	 */
	public static CRandom get()
	{
		return threadLocalRandom.get();
	}
}