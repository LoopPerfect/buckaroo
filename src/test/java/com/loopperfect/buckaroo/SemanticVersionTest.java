package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableList;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public final class SemanticVersionTest {

    @org.junit.Test
    public void testEquals() throws Exception {

        assertEquals(SemanticVersion.of(1), SemanticVersion.of(1));
        assertEquals(SemanticVersion.of(1, 0), SemanticVersion.of(1));
        assertEquals(SemanticVersion.of(7), SemanticVersion.of(7, 0, 0, 0));

        assertNotEquals(SemanticVersion.of(1), SemanticVersion.of(2));
        assertNotEquals(SemanticVersion.of(1), SemanticVersion.of(1, 0, 0, 1));
    }

    @org.junit.Test
    public void testComparison() throws Exception {

        final SemanticVersion a = SemanticVersion.of(1);
        final SemanticVersion b = SemanticVersion.of(2);
        final SemanticVersion c = SemanticVersion.of(9, 1, 2, 3);
        final SemanticVersion d = SemanticVersion.of(2, 4);
        final SemanticVersion e = SemanticVersion.of(2, 4, 0, 1);
        final SemanticVersion f = SemanticVersion.of(0, 7);
        final SemanticVersion g = SemanticVersion.of(0, 7, 9);

        assertEquals(
            ImmutableList.of(f, g, a, b, d, e, c),
            ImmutableList.of(a, b, c, d, e, f, g).stream().sorted().collect(ImmutableList.toImmutableList()));
    }

    @org.junit.Test
    public void testParse() throws Exception {

        assertEquals(Optional.of(SemanticVersion.of(1, 4, 5)), SemanticVersion.parse(" 1.4.5"));
        assertEquals(Optional.of(SemanticVersion.of(1, 8, 0)), SemanticVersion.parse("1.8.0"));
        assertEquals(Optional.of(SemanticVersion.of(12)), SemanticVersion.parse(" 12"));
        assertEquals(Optional.of(SemanticVersion.of(2, 6)), SemanticVersion.parse(" 2.6  "));
        assertEquals(Optional.of(SemanticVersion.of(1, 2)), SemanticVersion.parse(" v1.2  "));
        assertEquals(Optional.of(SemanticVersion.of(1, 2, 3)), SemanticVersion.parse("V1.2.3"));
        assertEquals(Optional.of(SemanticVersion.of(1, 2, 3)), SemanticVersion.parse("V1.2.3"));
        assertEquals(Optional.of(SemanticVersion.of(1, 2, 3, 4)), SemanticVersion.parse(" 1.2.3.4"));
        assertEquals(Optional.of(SemanticVersion.of(0, 9, 8, 4)), SemanticVersion.parse("v0.9.8.4"));

        assertEquals(Optional.empty(), SemanticVersion.parse(" asdadg"));
        assertEquals(Optional.empty(), SemanticVersion.parse(" 1."));
        assertEquals(Optional.empty(), SemanticVersion.parse(" 1,212"));
        assertEquals(Optional.empty(), SemanticVersion.parse(" 1.-5.3"));
        assertEquals(Optional.empty(), SemanticVersion.parse(" 1.2.3."));
        assertEquals(Optional.empty(), SemanticVersion.parse(" 1..2"));
        assertEquals(Optional.empty(), SemanticVersion.parse("   "));
    }
}