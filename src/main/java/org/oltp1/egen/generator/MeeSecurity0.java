package org.oltp1.egen.generator;

import org.oltp1.egen.util.TpcRandom;

/**
 * A Java port of the CMEESecurity class. This class provides the price/time
 * functionality needed to emulate a security's behavior in the market. It
 * calculates prices based on a deterministic triangular function, which is
 * essential for both initial data generation (e.g., LAST_TRADE) and the runtime
 * Market Exchange Emulator (MEE). Based on inc/MEESecurity.h and
 * src/MEESecurity.cpp
 */
public class MeeSecurity0
{

	// Constants from the C++ implementation
	private static final int SECURITY_PRICE_PERIOD_SECONDS = 900; // 15 minutes
	private static final double MIN_SECURITY_PRICE = 20.00;
	private static final double MAX_SECURITY_PRICE = 30.00;

	private final TpcRandom random;
	private final double priceRange;
	private final int tradingTimeSoFar; // In seconds

	public MeeSecurity0(int hoursOfInitialTrades)
	{
		// This constructor is for data generation, where time is simulated.
		this.tradingTimeSoFar = hoursOfInitialTrades * 3600; // Convert hours to seconds
		this.random = new TpcRandom(0); // Seed is not critical here as it's used for stateless calculations
		this.priceRange = MAX_SECURITY_PRICE - MIN_SECURITY_PRICE;
	}

	/**
	 * Calculates the "unique" starting offset in the price curve for a security.
	 * This ensures each security has a different, but deterministic, price history.
	 *
	 * @param securityIndex
	 *            The 0-based index of the security.
	 * @return The initial time offset in seconds.
	 */
	private double getInitialTime(long securityIndex)
	{
		int msPerPeriod = SECURITY_PRICE_PERIOD_SECONDS * 1000;
		long securityFactor = securityIndex * 556237 + 253791;
		long tradingFactor = (long) tradingTimeSoFar * 1000;

		return ((tradingFactor + securityFactor) % msPerPeriod) / 1000.0;
	}

	/**
	 * Calculates the price of a security at a specific point in time using a
	 * deterministic triangular function.
	 *
	 * @param securityIndex
	 *            The 0-based index of the security.
	 * @param timeInSeconds
	 *            The time elapsed since the start of the simulation.
	 * @return The calculated price of the security.
	 */
	public double calculatePrice(long securityIndex, double timeInSeconds)
	{
		// Calculate the effective time within the repeating 900-second period
		double periodTime = (timeInSeconds + getInitialTime(securityIndex)) / SECURITY_PRICE_PERIOD_SECONDS;
		double timeWithinPeriod = (periodTime - (long) periodTime) * SECURITY_PRICE_PERIOD_SECONDS;

		double pricePosition; // A value from 0.0 to 1.0 representing position in the price range

		// The price follows a triangular wave: up for the first half, down for the
		// second
		if (timeWithinPeriod < SECURITY_PRICE_PERIOD_SECONDS / 2.0)
		{
			// Price is rising
			pricePosition = timeWithinPeriod / (SECURITY_PRICE_PERIOD_SECONDS / 2.0);
		}
		else
		{
			// Price is falling
			pricePosition = (SECURITY_PRICE_PERIOD_SECONDS - timeWithinPeriod) / (SECURITY_PRICE_PERIOD_SECONDS / 2.0);
		}

		return MIN_SECURITY_PRICE + (priceRange * pricePosition);
	}
}