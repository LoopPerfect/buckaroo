package com.loopperfect.buckaroo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class FunctionsTest {

    private static int square(final int x) {
        return x * x;
    }

    private static int increment(final int x) {
        return x + 1;
    }

    private static int negate(final int x) {
        return -x;
    }

    @Test
    public void then() {

        final F1<Integer, Integer> f = F1.of(FunctionsTest::increment)
            .then(FunctionsTest::square)
            .then(FunctionsTest::negate);

        assertEquals(Integer.valueOf(-4), f.apply(1));
    }
}