package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static org.junit.Assert.*;

public final class MoreListsTest {

    @Test
    public void concat() throws Exception {

        final ImmutableList<Integer> a = ImmutableList.of(1, 2, 3);
        final ImmutableList<Integer> b = ImmutableList.of(4, 5, 6);

        final ImmutableList<Integer> expected = ImmutableList.of(1, 2, 3, 4, 5, 6);

        assertEquals(expected, MoreLists.concat(a, b));
    }

    @Test
    public void append() throws Exception {

        assertEquals(
            ImmutableList.of(1, 2, 3, 4),
            MoreLists.append(ImmutableList.of(1, 2, 3), 4));
    }
}