package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.junit.Assert.*;

public final class MoreMapsTest {

    @Test
    public void merge1() throws Exception {

        final ImmutableMap<String, Integer> x = ImmutableMap.of("a", 1);
        final ImmutableMap<String, Integer> y = ImmutableMap.of("b", 2);

        final ImmutableMap<String, Integer> expected = ImmutableMap.of(
            "a", 1,
            "b", 2);

        assertEquals(expected, MoreMaps.merge(x, y));
    }

    @Test
    public void merge2() throws Exception {

        final ImmutableMap<String, Integer> x = ImmutableMap.of();
        final ImmutableMap<String, Integer> y = ImmutableMap.of();

        final ImmutableMap<String, Integer> expected = ImmutableMap.of();

        assertEquals(expected, MoreMaps.merge(x, y));
    }

    @Test
    public void merge3() throws Exception {

        final ImmutableMap<String, Integer> x = ImmutableMap.of("a", 1, "b", 7);
        final ImmutableMap<String, Integer> y = ImmutableMap.of("b", 2, "c", 3);

        final ImmutableMap<String, Integer> expected = ImmutableMap.of(
            "a", 1,
            "b", 2,
            "c", 3);

        assertEquals(expected, MoreMaps.merge(x, y));
    }
}