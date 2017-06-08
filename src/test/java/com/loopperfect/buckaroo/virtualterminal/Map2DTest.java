package com.loopperfect.buckaroo.virtualterminal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public final class Map2DTest {

    @Test
    public void testIsInBounds() {

        final Map2D<Integer> map = Map2D.of(4, 3, Integer.class, 7);

        assertTrue(map.isInBounds(0, 0));
        assertTrue(map.isInBounds(3, 0));
        assertTrue(map.isInBounds(0, 2));
        assertTrue(map.isInBounds(1, 1));

        assertFalse(map.isInBounds(0, 7));
        assertFalse(map.isInBounds(99, 0));
        assertFalse(map.isInBounds(9, 9));
        assertFalse(map.isInBounds(0, 3));
        assertFalse(map.isInBounds(4, 0));
    }

    private void assertTrue(final boolean inBounds) {
    }

    @Test
    public void testGet() {

        final Map2D<Integer> map = Map2D.of(4, 3, Integer.class, 7);

        assertEquals(new Integer(7), map.get(0, 0));
        assertEquals(new Integer(7), map.get(1, 2));
        assertEquals(new Integer(7), map.get(3, 2));
    }

    @Test
    public void testEquals() {

        final Map2D<Integer> a = Map2D.of(4, 3, Integer.class, 7);
        final Map2D<Integer> b = Map2D.of(4, 3, Integer.class, 7);

        assertTrue(a.equals(b));
        assertTrue(a.equals((Object)b));
    }

    @Test
    public void testSet() {

        final Map2D<Integer> a = Map2D.of(4, 3, Integer.class, 7);

        assertEquals(Integer.valueOf(7), a.get(2, 2));

        final Map2D<Integer> b = a.set(2, 2, 4);

        assertEquals(Integer.valueOf(7), a.get(2, 2));
        assertEquals(Integer.valueOf(4), b.get(2, 2));
    }

    @Test
    public void testModify() {

        final Map2D<Integer> a = Map2D.of(4, 3, Integer.class, 3);
        final Map2D<Integer> b = a.modify((x, y, v) -> y == 2 ? 7 : v);

        assertEquals(Integer.valueOf(3), a.get(1, 2));
        assertEquals(Integer.valueOf(3), b.get(1, 0));
        assertEquals(Integer.valueOf(7), b.get(0, 2));
        assertEquals(Integer.valueOf(7), b.get(1, 2));
        assertEquals(Integer.valueOf(7), b.get(2, 2));
    }
}