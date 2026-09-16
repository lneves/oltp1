package org.oltp1.runner.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CRandomTest {

    @Test
    void testRndIntRange_Bounds() {
        CRandom random = new CRandom(12345L);
        int min = 10;
        int max = 20;

        for (int i = 0; i < 1000; i++) {
            long result = random.rndIntRange(min, max);

            assertTrue(result >= min, "Result should be >= min");
            assertTrue(result <= max, "Result should be <= max");
        }
    }

    @Test
    void testRndIntRange_SingleValue() {
        CRandom random = new CRandom(12345L);

        assertEquals(
            10L,
            random.rndIntRange(10, 10),
            "Should return the same value when min equals max"
        );
    }

    @Test
    void testNonUniformRandom_Range() {
        CRandom random = new CRandom(54321L);
        long P = 100;
        long Q = 200;
        int A = 255;
        int s = 8;

        for (int i = 0; i < 1000; i++) {
            long result = random.nonUniformRandom(P, Q, A, s);

            assertTrue(result >= P, "Result should be >= P");
            assertTrue(result <= Q, "Result should be <= Q");
        }
    }

    @Test
    void testRndNthElement() {
        long seed = 80534927L;

        CRandom cRnd1 = new CRandom(seed);
        CRandom cRnd2 = new CRandom(seed);

        // Advance cRnd1 by 100 steps
        long expected = 0;

        for (int i = 0; i < 100; i++) {
            expected = cRnd1.rndInt64Range(0, 1000);
        }

        // Use rndNthElement to get the 99th element
        // since rndInt64Range advances the seed first.
        long nthSeed = cRnd2.rndNthElement(seed, 99);

        cRnd2.setSeed(nthSeed);

        long actual = cRnd2.rndInt64Range(0, 1000);

        assertEquals(
            expected,
            actual,
            "The 100th random number should match"
        );
    }

    @Test
    void testUnsignedMultiplyHigh_NoOverflow() {
        long x = 0xFFFFFFFF_FFFFFFFFL;
        long y = 0xFFFFFFFF_FFFFFFFFL;

        // (2^64 - 1) * (2^64 - 1) = 2^128 - 2^65 + 1
        // High 64 bits must be 0xFFFFFFFFFFFFFFFE (-2L)
        assertEquals(
            0xFFFFFFFF_FFFFFFFEL,
            CRandom.multiplyHigh(x, y)
        );
    }
}