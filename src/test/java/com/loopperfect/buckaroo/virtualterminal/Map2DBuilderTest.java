package com.loopperfect.buckaroo.virtualterminal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class Map2DBuilderTest {

    @Test
    public void testBuild() {

        final Map2DBuilder<Integer> builder = new Map2DBuilder<>(16, 9, Integer.class, 1);

        builder.set(6, 3, 7);
        builder.set(1, 5, 8);

        final Map2D<Integer> expected = Map2D.of(16, 9, Integer.class, 1).set(6, 3, 7).set(1, 5, 8);
        final Map2D<Integer> actual = builder.build();

        assertEquals(expected, actual);
    }

}