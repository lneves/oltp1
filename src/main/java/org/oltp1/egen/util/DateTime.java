package org.oltp1.egen.util;

/**
 * A Java port of the C++ CDateTime class. This class is critical for ensuring
 * data consistency and reproducibility. It uses a custom day-numbering system
 * (days since 0001-01-01) and millisecond-of-day representation, exactly like
 * the original. DO NOT replace its logic with standard Java time libraries
 * without extensive testing. Based on Utilities/inc/DateTime.h and
 * Utilities/src/DateTime.cpp
 */

public class DateTime
{

	// Internal representation
	private int dayNumber; // Absolute day number since 0001-01-01 (day 0)
	private int msecInDay; // Milliseconds from the beginning of the day

	// Constants for day number calculations
	private static final int DY1 = 365;
	private static final int DY4 = 4 * DY1 + 1;
	private static final int DY100 = 25 * DY4 - 1;
	private static final int DY400 = 4 * DY100 + 1;
	private static final int[] CUMULATIVE_MONTH_DAYS = { 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334 };

	private static final String[] MONTHS_SHORT = { "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC" };
	private static final String[] MONTHS_FULL = { "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" };
	private static final String[] AM_PM = { "AM", "PM" };
	// Maps 24-hour to 12-hour format
	private static final int[] HOUR_12 = { 12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };

	private static final int[] MONTH_DAYS = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

	private static final int[] MONTH_DAYS_LEAP = { 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

	public static final int daysPerWorkWeek = 5;
	public static final int daysPerWeek = 7;
	private static final double nsPerSecondDivisor = 1000000000.0;
	private static final int nsPerSecond = 1000000000;
	private static final double msPerSecondDivisor = 1000.000;
	private static final int msPerSecond = 1000;
	private static final int secondsPerMinute = 60;
	private static final int minutesPerHour = 60;
	private static final int hoursPerDay = 24;
	private static final int hoursPerWorkDay = 8;
	private static final int secondsPerHour = secondsPerMinute * minutesPerHour;
	private static final int secondsPerDay = secondsPerMinute * minutesPerHour * hoursPerDay;
	private static final int secondsPerWorkDay = secondsPerMinute * minutesPerHour * hoursPerWorkDay;
	private static final int msPerDay = secondsPerDay * msPerSecond;
	private static final int msPerWorkDay = secondsPerWorkDay * msPerSecond;

	public int __diffInMilliSeconds(DateTime baseTime)
	{
		int msecs;
		msecs = (dayNumber - baseTime.dayNumber) * msPerSecond * secondsPerMinute *
				minutesPerHour * hoursPerDay;
		msecs += (msecInDay - baseTime.msecInDay);
		return msecs;
	}

	public int diffInMilliSeconds(DateTime baseTime)
	{
		long msecs = ((long) (dayNumber - baseTime.dayNumber)) * msPerDay;
		msecs += (msecInDay - baseTime.msecInDay);
		return (int) msecs; // Will still overflow for very large differences
	}

	public int diffInSeconds(DateTime baseTime)
	{
		return (diffInMilliSeconds(baseTime) / msPerSecond);
	}

	public double milliSecondsSince(DateTime base)
	{
		long dayDelta = (long) this.dayNumber - (long) base.dayNumber;
		long msDelta = (long) this.msecInDay - (long) base.msecInDay;
		return dayDelta * msPerDay + msDelta;
	}

	public DateTime(int year, int month, int day)
	{
		set(year, month, day, 0, 0, 0, 0);
	}

	public DateTime(int year, int month, int day, int hour, int minute, int second, int msec)
	{
		set(year, month, day, hour, minute, second, msec);
	}

	/**
	 * New constructor that takes a day number directly.
	 * 
	 * @param dayNumber
	 *            The absolute day number since 0001-01-01.
	 */
	public DateTime(int dayNumber)
	{
		this.dayNumber = dayNumber;
		this.msecInDay = 0;
	}

	public DateTime(DateTime dt)
	{
		this.dayNumber = dt.dayNumber;
		this.msecInDay = dt.msecInDay;
	}

	public void set(int year, int month, int day, int hour, int minute, int second, int msec)
	{
		this.dayNumber = ymdToDayNumber(year, month, day);
		this.msecInDay = (((hour * 60 + minute) * 60 + second) * 1000) + msec;
	}

	public void set(int hour, int minute, int second, int msec)
	{
		this.msecInDay = ((hour * minutesPerHour + minute) * secondsPerMinute + second) * msPerSecond + msec;
	}

	public int getDayNumber()
	{
		return dayNumber;
	}

	public void add(int days, int msec, boolean adjust_weekend /* =false */)
	{
		if (adjust_weekend)
		{
			days = ((days / daysPerWorkWeek) * daysPerWeek) + (days % daysPerWorkWeek);
		}

		dayNumber += days;

		msecInDay += msec;
		dayNumber += msecInDay / msPerDay;
		msecInDay %= msPerDay;
		if (msecInDay < 0)
		{
			dayNumber--;
			msecInDay += msPerDay;
		}
	}

	private static boolean isLeapYear(int year)
	{
		if ((year % 4) != 0)
			return false;
		if ((year % 400) == 0)
			return true;
		return (year % 100) != 0;
	}

	/**
	 * A direct port of the C++ YMDtoDayno logic. Computes the number of days since
	 * 0001-01-01.
	 * 
	 * @return The absolute day number.
	 */
	public static int ymdToDayNumber(int year, int month, int day)
	{
		int dayOfYear = CUMULATIVE_MONTH_DAYS[month - 1] + day - 1;
		if (month > 2 && isLeapYear(year))
		{
			dayOfYear++;
		}

		int tempYear = year - 1;
		int dayNum = dayOfYear;
		dayNum += tempYear / 400 * DY400;
		tempYear %= 400;
		dayNum += tempYear / 100 * DY100;
		tempYear %= 100;
		dayNum += tempYear / 4 * DY4;
		tempYear %= 4;
		dayNum += tempYear * DY1;

		return dayNum;
	}

	/**
	 * Converts the internal dayNumber and msecInDay back into individual date and
	 * time components. This is a direct port of the C++ GetYMDHMS method.
	 *
	 * @return An integer array containing {year, month, day, hour, minute, second,
	 *         millisecond}.
	 */
	public int[] getYmdhms()
	{
		int[] result = new int[7];
		int tempDayNo = this.dayNumber;

		// --- Get Year, Month, Day (from GetYMD) ---
		int y = 1 + (tempDayNo / DY400) * 400;
		tempDayNo %= DY400;
		if (tempDayNo == DY400 - 1)
		{
			y += 399;
			tempDayNo -= 3 * DY100 + 24 * DY4 + 3 * DY1;
		}
		else
		{
			y += (tempDayNo / DY100) * 100;
			tempDayNo %= DY100;
			y += (tempDayNo / DY4) * 4;
			tempDayNo %= DY4;
			if (tempDayNo == DY4 - 1)
			{
				y += 3;
				tempDayNo -= 3 * DY1;
			}
			else
			{
				y += tempDayNo / DY1;
				tempDayNo %= DY1;
			}
		}
		result[0] = y; // Year

		int m = 1;
		tempDayNo++; // Convert from 0-based to 1-based for day calculation
		int[] monthArray = isLeapYear(y) ? MONTH_DAYS_LEAP : MONTH_DAYS;
		while (tempDayNo > monthArray[m - 1])
		{
			tempDayNo -= monthArray[m - 1];
			m++;
		}

		result[1] = m; // Month
		result[2] = tempDayNo; // Day

		// --- Get Hour, Minute, Second, Millisecond (from GetHMS) ---
		int ms = this.msecInDay;
		result[6] = ms % 1000; // Millisecond
		ms /= 1000;
		result[5] = ms % 60; // Second
		ms /= 60;
		result[4] = ms % 60; // Minute
		result[3] = ms / 60; // Hour

		return result;
	}

	/**
	 * Formats the date for output. Note: For 100% accuracy, this should also be
	 * ported from the day number. However, using Java's LocalDate is correct for
	 * the date ranges in TPC-E.
	 */
	public String toFormattedString(int style)
	{
		int[] components = getYmdhms();
		int year = components[0];
		int month = components[1];
		int day = components[2];
		int hour = components[3];
		int minute = components[4];
		int second = components[5];
		int msec = components[6];

		StringBuilder datePart = new StringBuilder();
		StringBuilder timePart = new StringBuilder();

		// --- Format Date Part ---
		switch (style / 10)
		{
		case 1: // YYYY-MM-DD
			datePart.append(String.format("%04d-%02d-%02d", year, month, day));
			break;
		case 2: // MM/DD/YY
			datePart.append(String.format("%02d/%02d/%02d", month, day, year % 100));
			break;
		default: // No date part
			break;
		}

		// --- Format Time Part ---
		switch (style % 10)
		{
		case 1: // HH:MM:SS (24hr)
			timePart.append(String.format("%02d:%02d:%02d", hour, minute, second));
			break;
		case 2: // HH:MM:SS.mmm (24hr)
			timePart.append(String.format("%02d:%02d:%02d.%03d", hour, minute, second, msec));
			break;
		case 4: // HH:MM:SS [AM|PM]
			timePart.append(String.format("%02d:%02d:%02d %s", HOUR_12[hour], minute, second, AM_PM[hour / 12]));
			break;
		default: // No time part
			break;
		}

		// Combine parts
		if (datePart.length() > 0 && timePart.length() > 0)
		{
			return datePart.toString() + " " + timePart.toString();
		}
		else
		{
			return datePart.toString() + timePart.toString();
		}
	}

	public static double roundToNearestNsec(double seconds)
	{
		return (((long) (((seconds) * nsPerSecond) + 0.5)) / nsPerSecondDivisor);
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (!(o instanceof DateTime))
			return false;
		DateTime d = (DateTime) o;
		return dayNumber == d.dayNumber && msecInDay == d.msecInDay;
	}

	@Override
	public int hashCode()
	{
		return 31 * dayNumber + msecInDay;
	}

}