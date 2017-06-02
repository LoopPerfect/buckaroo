package com.loopperfect.buckaroo;

import org.junit.Test;

import static org.junit.Assert.*;

public final class PairTest {

    @Test
    public void equals() throws Exception {

        assertEquals(Pair.of(1, 2), Pair.of(1, 2));
        assertEquals(Pair.of(1, "abc"), Pair.of(1, "abc"));

        assertNotEquals(Pair.of(1, 2), Pair.of(2, 1));
        assertNotEquals(Pair.of(1, 1), Pair.of(2, 1));
        assertNotEquals(Pair.of(2, 2), Pair.of(2, 1));
        assertNotEquals(Pair.of(1, "abc"), Pair.of("abc", 1));

        final Object object = Pair.of(1, 2);

        assertEquals(Pair.of(1, 2), object);
    }

    @Test
    public void equalsRespectsTypes() {
        final Pair<Integer, Object> a = Pair.of(1, (Object)("abc"));
        final Pair<Integer, String> b = Pair.of(1, "abc");

        assertNotEquals(a, b);
    }
}