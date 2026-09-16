package org.oltp1.egen.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.Test;

public class AppendableRowTest
{
    private final AppendableRow row = new AppendableRow()
    {
        @Override
        public void writeObject(Appendable out) throws IOException
        {
            // Not needed for these tests.
        }
    };

    @Test
    public void testSimplePositiveNumber() throws IOException
    {
        assertWrite(123.456);
    }

    @Test
    public void testSimpleNegativeNumber() throws IOException
    {
        assertWrite(-123.456);
    }

    @Test
    public void testZero() throws IOException
    {
        assertWrite(0.0);
    }

    @Test
    public void testRoundingUp() throws IOException
    {
        assertWrite(1.235);
    }

    @Test
    public void testRoundingDown() throws IOException
    {
        assertWrite(1.234);
    }

    @Test
    public void testRoundingAtBoundary() throws IOException
    {
        assertWrite(1.225);
    }

    @Test
    public void testVerySmallPositive() throws IOException
    {
        assertWrite(0.001);
    }

    @Test
    public void testVerySmallNegative() throws IOException
    {
        assertWrite(-0.001);
    }

    @Test
    public void testSmallNegativeRoundingToZero() throws IOException
    {
        assertWrite(-0.004);
    }

    @Test
    public void testSmallNegativeRoundingAwayFromZero() throws IOException
    {
        assertWrite(-0.005);
    }

    @Test
    public void testPositiveRoundingCausesCarry() throws IOException
    {
        assertWrite(9.995);
    }

    @Test
    public void testNegativeRoundingCausesCarry() throws IOException
    {
        assertWrite(-9.995);
    }

    @Test
    public void testLargePositiveNumber() throws IOException
    {
        assertWrite(999999.999);
    }

    @Test
    public void testLargeNegativeNumber() throws IOException
    {
        assertWrite(-999999.999);
    }

    @Test
    public void testOneDecimalPlace() throws IOException
    {
        assertWrite(42.1);
    }

    @Test
    public void testNoDecimalPlaces() throws IOException
    {
        assertWrite(42.0);
    }

    @Test
    public void testNegativeNoDecimalPlaces() throws IOException
    {
        assertWrite(-42.0);
    }

    @Test
    public void testPositiveNearHalf() throws IOException
    {
        assertWrite(1.115);
    }

    @Test
    public void testNegativeNearHalf() throws IOException
    {
        assertWrite(-1.115);
    }

    @Test
    public void testEdgeCaseNegativeZeroPointZeroZeroFour() throws IOException
    {
        assertWrite(-0.004999);
    }

    @Test
    public void testBoundaryBetweenZeroAndNegative() throws IOException
    {
        assertWrite(-0.0049);
    }

    @Test
    public void testBoundaryBetweenZeroAndNegativeRoundsAway() throws IOException
    {
        assertWrite(-0.0051);
    }

    @Test
    public void testRoundingExactlyPointFive() throws IOException
    {
        assertWrite(1.125);
    }

    @Test
    public void testRoundingNegativeExactlyPointFive() throws IOException
    {
        assertWrite(-1.125);
    }

    @Test
    public void testSpecialCaseNineNineNine() throws IOException
    {
        assertWrite(0.999);
    }

    @Test
    public void testSpecialCaseNegativeNineNineNine() throws IOException
    {
        assertWrite(-0.999);
    }

    @Test
    public void testIOExceptionPropagation()
    {
        Appendable failingAppendable = new FailingAppendable();

        IOException exception = assertThrows(
            IOException.class,
            () -> row.write(failingAppendable, 1.23)
        );

        assertEquals("Test exception", exception.getMessage());
    }

    private void assertWrite(double value) throws IOException
    {
        StringBuilder output = new StringBuilder();

        row.write(output, value);

        assertEquals(correct(Double.toString(value)), output.toString());
    }

    private String correct(String value)
    {
        return new BigDecimal(value)
            .setScale(2, RoundingMode.HALF_UP)
            .toString();
    }

    /**
     * Simple test double replacing anything that would otherwise
     * require Mockito.
     */
    private static final class FailingAppendable implements Appendable
    {
        @Override
        public Appendable append(CharSequence csq) throws IOException
        {
            throw new IOException("Test exception");
        }

        @Override
        public Appendable append(CharSequence csq, int start, int end)
            throws IOException
        {
            throw new IOException("Test exception");
        }

        @Override
        public Appendable append(char c) throws IOException
        {
            throw new IOException("Test exception");
        }
    }
}