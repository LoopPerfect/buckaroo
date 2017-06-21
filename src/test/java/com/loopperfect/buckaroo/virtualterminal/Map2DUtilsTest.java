package com.loopperfect.buckaroo.virtualterminal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class Map2DUtilsTest {

    @Test
    public void pasteBelow() {

        final Map2D<Integer> a = Map2D.of(20, 3, Integer.class, 1);
        final Map2D<Integer> b = Map2D.of(20, 4, Integer.class, 2);

        final Map2D<Integer> c = Map2DUtils.pasteBelow(a, b);

        assertEquals(a.height() + b.height(), c.height());
        assertEquals(a.width(), c.width());
        assertEquals(b.width(), c.width());
        assertEquals(a.get(2, 2), c.get(2, 2));
        assertEquals(b.get(2, 2), c.get(2, a.height() + 2));
    }
}