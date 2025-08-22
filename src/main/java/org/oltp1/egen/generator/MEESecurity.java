package org.oltp1.egen.generator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.oltp1.egen.model.TradeType;
import org.oltp1.egen.util.DateTime;
import org.oltp1.egen.util.Money;
import org.oltp1.egen.util.TpcRandom;

/**
 * This class emulates the price and time behavior of a security in a simulated
 * market. It provides functionality for calculating a security's price at any
 * given time, the time it takes for a price to be reached, and
 * submission/completion times for trades.
 */
public class MEESecurity
{

	/**
	 * A result object to return multiple values from getCompletionTime.
	 */
	public static class CompletionResult
	{
		public final double completionTime;
		public final Money completionPrice;

		public CompletionResult(double completionTime, Money completionPrice)
		{
			this.completionTime = completionTime;
			this.completionPrice = completionPrice;
		}
	}

	// From SecurityPriceRange.h
	public static final Money MIN_SEC_PRICE = new Money(20.00);
	public static final Money MAX_SEC_PRICE = new Money(30.00);
	// From MEESecurity.cpp
	public static final int SEC_PRICE_PERIOD = 900; // 15 minutes
	public static final double MEAN_COMPLETION_TIME_DELAY = 1.0;
	public static final double COMPLETION_SUT_DELAY = 1.0; // seconds
	// From EGen an RNG seed
	public static final long RNG_SEED_BASE_MEE_SECURITY = 75791232L;
	// Time conversion constants
	public static final int MS_PER_SECOND = 1000;
	public static final double MS_PER_SECOND_DIVISOR = 1000.0;

	private final TpcRandom rnd;
	private final Money rangeLow;
	private final Money rangeHigh;
	private final Money range;
	private final int period;

	private int tradingTimeSoFar;
	private DateTime baseTime;
	private DateTime currentTime;
	private double meanInTheMoneySubmissionDelay;
	// endregion

	/**
	 * Default constructor. Initializes the class with default
	 */
	public MEESecurity()
	{
		this.rnd = new TpcRandom(RNG_SEED_BASE_MEE_SECURITY);
		this.rangeLow = MIN_SEC_PRICE;
		this.rangeHigh = MAX_SEC_PRICE;
		this.range = MAX_SEC_PRICE.subtract(MIN_SEC_PRICE);
		this.period = SEC_PRICE_PERIOD;
		this.tradingTimeSoFar = 0;
		this.baseTime = null;
		this.currentTime = null;
	}

	/**
	 * Initializes or re-initializes the security's state.
	 *
	 * @param tradingTimeSoFar
	 *            Point to resume on the price curve.
	 * @param baseTime
	 *            Wall clock time for the initial time of all securities.
	 * @param currentTime
	 *            Current time for this security.
	 * @param meanInTheMoneySubmissionDelay
	 *            Mean delay for an in-the-money limit order.
	 */
	public void init(int tradingTimeSoFar, DateTime baseTime, DateTime currentTime, double meanInTheMoneySubmissionDelay)
	{
		this.tradingTimeSoFar = tradingTimeSoFar;
		this.baseTime = baseTime;
		this.currentTime = currentTime;
		this.meanInTheMoneySubmissionDelay = meanInTheMoneySubmissionDelay;

		// Reset the RNG seed for consistent results across multiple instances.
		this.rnd.setSeed(RNG_SEED_BASE_MEE_SECURITY);
	}

	/**
	 * Calculates the price at a certain point in time using a triangular wave
	 * function.
	 *
	 * @param securityIndex
	 *            Unique index for the security.
	 * @param time
	 *            Seconds from the initial time.
	 * @return The calculated price.
	 */
	public Money calculatePrice(long securityIndex, double time)
	{
		double periodTime = (time + initialTime(securityIndex)) / ((double) this.period);
		double timeWithinPeriod = (periodTime - (int) periodTime) * ((double) this.period);

		double pricePosition; // A value from 0.0 to 1.0 representing position in the price range.
		if (timeWithinPeriod < this.period / 2.0)
		{
			pricePosition = timeWithinPeriod / (this.period / 2.0);
		}
		else
		{
			pricePosition = (this.period - timeWithinPeriod) / (this.period / 2.0);
		}

		// Scale the position to the actual price range.
		return MIN_SEC_PRICE.add(this.range.multiply(pricePosition));
	}

	/**
	 * Calculates the current price for the security.
	 *
	 * @param securityIndex
	 *            Unique identifier for the security.
	 * @return The price at the current time.
	 */
	public Money getCurrentPrice(long securityIndex)
	{
		if (this.currentTime == null || this.baseTime == null)
		{
			throw new IllegalStateException("MEESecurity has not been properly initialized with times.");
		}
		double secElapsed = this.currentTime.milliSecondsSince(this.baseTime) / 1000.0;
		return calculatePrice(securityIndex, secElapsed);
	}

	public Money getMinPrice()
	{
		return this.rangeLow;
	}

	public Money getMaxPrice()
	{
		return this.rangeHigh;
	}

	/**
	 * Calculates the submission time for limit orders.
	 *
	 * @param securityIndex
	 *            Unique index for the security.
	 * @param pendingTime
	 *            Time the order was placed, in seconds from time 0.
	 * @param limitPrice
	 *            The limit price of the order.
	 * @param tradeType
	 *            The type of trade (Limit Buy, Limit Sell, Stop Loss).
	 * @return The expected submission time in seconds.
	 */
	public double __getSubmissionTime(long securityIndex, double pendingTime, Money limitPrice, TradeType tradeType)
	{
		Money priceAtPendingTime = calculatePrice(securityIndex, pendingTime);
		double submissionTimeFromPending;

		// Check if the order is already in-the-money.
		boolean isInTheMoney = ((tradeType == TradeType.LIMIT_BUY || tradeType == TradeType.STOP_LOSS) && priceAtPendingTime.compareTo(limitPrice) <= 0) ||
				((tradeType == TradeType.LIMIT_SELL) && priceAtPendingTime.compareTo(limitPrice) >= 0);

		if (isInTheMoney)
		{
			// Order triggers immediately, with a small random delay.
			double low = 0.5 * this.meanInTheMoneySubmissionDelay;
			double high = 1.5 * this.meanInTheMoneySubmissionDelay;
			submissionTimeFromPending = low + (rnd.rndDouble() * (high - low));
		}
		else
		{
			// Order is not in-the-money, calculate time until price is reached.
			int directionAtPendingTime;
			if ((int) (pendingTime + initialTime(securityIndex)) % this.period < this.period / 2)
			{
				directionAtPendingTime = 1; // Price is going up
			}
			else
			{
				directionAtPendingTime = -1; // Price is going down
			}
			submissionTimeFromPending = calculateTime(priceAtPendingTime, limitPrice, directionAtPendingTime);
		}

		return BigDecimal
				.valueOf(pendingTime + submissionTimeFromPending)
				.setScale(3, RoundingMode.HALF_UP)
				.doubleValue();
	}

	public double getSubmissionTime(long securityIndex, double pendingTime, Money limitPrice, TradeType tradeType)
	{
		Money priceAtPendingTime = calculatePrice(securityIndex, pendingTime);

		double submissionTimeFromPending; // Submission - Pending time difference

		// Check if the order is already in the money
		// e.g. if the current price is less than the buy price
		// or the current price is more than the sell price.
		//
		boolean isInTheMoney = ((tradeType == TradeType.LIMIT_BUY || tradeType == TradeType.STOP_LOSS) &&
				priceAtPendingTime.lessThanOrEqual(limitPrice))
				||
				((tradeType == TradeType.LIMIT_SELL) && priceAtPendingTime.greaterThanOrEqual(limitPrice));

		if (isInTheMoney)
		{
			// Order triggers immediately, with a small random delay.

			double low = 0.5 * this.meanInTheMoneySubmissionDelay;
			double high = 1.5 * this.meanInTheMoneySubmissionDelay;
			submissionTimeFromPending = rnd.rndDoubleIncrRange(low, high, 0.001);
		}
		else
		{
			int directionAtPendingTime;
			if ((int) (pendingTime + initialTime(securityIndex)) % this.period < this.period / 2)
			{
				// In the first half of the period => price is going up
				directionAtPendingTime = 1;
			}
			else
			{
				// In the second half of the period => price is going down
				directionAtPendingTime = -1;
			}

			submissionTimeFromPending = calculateTime(priceAtPendingTime, limitPrice, directionAtPendingTime);
		}

		return pendingTime + submissionTimeFromPending;
	}

	/**
	 * Returns the expected completion time and the completion price for a trade.
	 *
	 * @param securityIndex
	 *            Unique index for the security.
	 * @param submissionTime
	 *            Time the order was submitted, in seconds from time 0.
	 * @return A {@link CompletionResult} object containing the completion time and
	 *         price.
	 */
	public CompletionResult getCompletionTime(long securityIndex, double submissionTime)
	{
		double completionDelay = negExp(MEAN_COMPLETION_TIME_DELAY);

		// Clip at 5 seconds to prevent rare, long delays.
		if (completionDelay > 5.0)
		{
			completionDelay = 5.0;
		}

		Money completionPrice = calculatePrice(securityIndex, submissionTime + completionDelay);
		double finalCompletionTime = submissionTime + completionDelay + COMPLETION_SUT_DELAY;

		return new CompletionResult(finalCompletionTime, completionPrice);
	}

	/**
	 * Calculates the "unique" starting offset in the price curve based on the
	 * security ID. This ensures each security starts at a different point on the
	 * triangular wave.
	 */
	private double initialTime(long securityIndex)
	{
		int msPerPeriod = SEC_PRICE_PERIOD * MS_PER_SECOND;
		long securityFactor = securityIndex * 556237 + 253791;
		long tradingFactor = (long) this.tradingTimeSoFar * MS_PER_SECOND;

		return ((tradingFactor + securityFactor) % msPerPeriod) / MS_PER_SECOND_DIVISOR;
	}

	/**
	 * Generates a random value from a negative exponential distribution.
	 */
	private double negExp(double mean)
	{
		return DateTime.roundToNearestNsec(rnd.rndDoubleNegExp(mean));
	}

	/**
	 * Calculates the time required to move between two prices on the triangular
	 * wave.
	 */
	private double calculateTime(Money startPrice, Money endPrice, int startDirection)
	{
		double halfPeriod = this.period / 2.0;

		// The time (in seconds) it takes for the price to move by $1.
		double speed = halfPeriod / this.range.dollarAmount();
		Money distance;

		if (endPrice.compareTo(startPrice) > 0)
		{ // Price needs to go up
			if (startDirection > 0)
			{
				distance = endPrice.subtract(startPrice);
			}
			else
			{
				distance = (startPrice.subtract(this.rangeLow)).add(endPrice.subtract(this.rangeLow));
			}
		}
		else
		{ // Price needs to go down
			if (startDirection > 0)
			{
				distance = (this.rangeHigh.subtract(startPrice)).add(this.rangeHigh.subtract(endPrice));
			}
			else
			{
				distance = startPrice.subtract(endPrice);
			}
		}

		return distance.dollarAmount() * speed;
	}

}