package org.oltp1.egen.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AppendableRowTest
{
	private AppendableRow row = new AppendableRow()
	{
		public void writeObject(Appendable out) throws IOException
		{
			// ignore, just test rounding of double values
		}
	};

	private StringBuilder output;

	@BeforeEach
	public void setUp()
	{
		output = new StringBuilder();

	}

	@Test
	public void testSimplePositiveNumber() throws IOException
	{
		row.write(output, 123.456);
		assertEquals(correct("123.456"), output.toString());
	}

	@Test
	public void testSimpleNegativeNumber() throws IOException
	{
		row.write(output, -123.456);
		assertEquals(correct("-123.456"), output.toString());
	}

	@Test
	public void testZero() throws IOException
	{
		row.write(output, 0.0);
		assertEquals(correct("0.0"), output.toString());
	}

	@Test
	public void testRoundingUp() throws IOException
	{
		row.write(output, 1.235);
		assertEquals(correct("1.235"), output.toString());
	}

	@Test
	public void testRoundingDown() throws IOException
	{
		row.write(output, 1.234);
		assertEquals(correct("1.234"), output.toString());
	}

	@Test
	public void testRoundingAtBoundary() throws IOException
	{
		row.write(output, 1.225);
		assertEquals(correct("1.225"), output.toString());
	}

	@Test
	public void testVerySmallPositive() throws IOException
	{
		row.write(output, 0.001);
		assertEquals(correct("0.001"), output.toString());
	}

	@Test
	public void testVerySmallNegative() throws IOException
	{
		row.write(output, -0.001);
		assertEquals(correct("-0.001"), output.toString());
	}

	@Test
	public void testSmallNegativeRoundingToZero() throws IOException
	{
		row.write(output, -0.004);
		assertEquals(correct("-0.004"), output.toString());
	}

	@Test
	public void testSmallNegativeRoundingAwayFromZero() throws IOException
	{
		row.write(output, -0.005);
		assertEquals(correct("-0.005"), output.toString());
	}

	@Test
	public void testPositiveRoundingCausesCarry() throws IOException
	{
		row.write(output, 9.995);
		assertEquals(correct("9.995"), output.toString());
	}

	@Test
	public void testNegativeRoundingCausesCarry() throws IOException
	{
		row.write(output, -9.995);
		assertEquals(correct("-9.995"), output.toString());
	}

	@Test
	public void testLargePositiveNumber() throws IOException
	{
		row.write(output, 999999.999);
		assertEquals(correct("999999.999"), output.toString());
	}

	@Test
	public void testLargeNegativeNumber() throws IOException
	{
		row.write(output, -999999.999);
		assertEquals(correct("-999999.999"), output.toString());
	}

	@Test
	public void testOneDecimalPlace() throws IOException
	{
		row.write(output, 42.1);
		assertEquals(correct("42.1"), output.toString());
	}

	@Test
	public void testNoDecimalPlaces() throws IOException
	{
		row.write(output, 42.0);
		assertEquals(correct("42.0"), output.toString());
	}

	@Test
	public void testNegativeNoDecimalPlaces() throws IOException
	{
		row.write(output, -42.0);
		assertEquals(correct("-42.0"), output.toString());
	}

	@Test
	public void testPositiveNearHalf() throws IOException
	{
		row.write(output, 1.115);
		assertEquals(correct("1.115"), output.toString());
	}

	@Test
	public void testNegativeNearHalf() throws IOException
	{
		row.write(output, -1.115);
		assertEquals(correct("-1.115"), output.toString());
	}

	@Test
	public void testIOExceptionPropagation() throws IOException
	{
		Appendable failingAppendable = new Appendable()
		{
			@Override
			public Appendable append(CharSequence csq) throws IOException
			{
				throw new IOException("Test exception");
			}

			@Override
			public Appendable append(CharSequence csq, int start, int end) throws IOException
			{
				throw new IOException("Test exception");
			}

			@Override
			public Appendable append(char c) throws IOException
			{
				throw new IOException("Test exception");
			}
		};

		assertThrows(IOException.class, () -> row.write(failingAppendable, 1.23));
	}

	@Test
	public void testEdgeCaseNegativeZeroPointZeroZeroFour() throws IOException
	{
		// This tests the special case where a small negative number rounds to -0.00
		row.write(output, -0.004999);

		assertEquals(correct("-0.004999"), output.toString());
	}

	@Test
	public void testBoundaryBetweenZeroAndNegative() throws IOException
	{
		row.write(output, -0.0049);
		assertEquals(correct("-0.0049"), output.toString());
	}

	@Test
	public void testBoundaryBetweenZeroAndNegativeRoundsAway() throws IOException
	{
		row.write(output, -0.0051);
		assertEquals(correct("-0.0051"), output.toString());
	}

	@Test
	public void testRoundingExactlyPointFive() throws IOException
	{
		row.write(output, 1.125);
		assertEquals(correct("1.125"), output.toString());
	}

	@Test
	public void testRoundingNegativeExactlyPointFive() throws IOException
	{
		row.write(output, -1.125);
		assertEquals(correct("-1.125"), output.toString());
	}

	@Test
	public void testSpecialCaseNineNineNine() throws IOException
	{
		row.write(output, 0.999);
		assertEquals(correct("0.999"), output.toString());
	}

	@Test
	public void testSpecialCaseNegativeNineNineNine() throws IOException
	{
		row.write(output, -0.999);
		assertEquals(correct("-0.999"), output.toString());
	}

	private String correct(String value)
	{
		return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toString();
	}
}