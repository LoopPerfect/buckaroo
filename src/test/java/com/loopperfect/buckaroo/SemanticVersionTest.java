package com.loopperfect.buckaroo;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public final class SemanticVersionTest {

    @org.junit.Test
    public void testParse() throws Exception {

        assertEquals(Optional.of(SemanticVersion.of(1, 4, 5)), SemanticVersion.parse(" 1.4.5"));
        assertEquals(Optional.of(SemanticVersion.of(1, 8, 0)), SemanticVersion.parse("1.8.0"));
        assertEquals(Optional.of(SemanticVersion.of(12)), SemanticVersion.parse(" 12"));
        assertEquals(Optional.of(SemanticVersion.of(2, 6)), SemanticVersion.parse(" 2.6  "));
        assertEquals(Optional.of(SemanticVersion.of(1, 2)), SemanticVersion.parse(" v1.2  "));
        assertEquals(Optional.of(SemanticVersion.of(1, 2, 3)), SemanticVersion.parse("V1.2.3"));

        assertEquals(Optional.empty(), SemanticVersion.parse(" asdadg"));
        assertEquals(Optional.empty(), SemanticVersion.parse(" 1."));
        assertEquals(Optional.empty(), SemanticVersion.parse(" 1,212"));
        assertEquals(Optional.empty(), SemanticVersion.parse(" 1.-5.3"));
        assertEquals(Optional.empty(), SemanticVersion.parse(" 1.2.3.4"));
        assertEquals(Optional.empty(), SemanticVersion.parse(" 1.2.3."));
        assertEquals(Optional.empty(), SemanticVersion.parse(" 1..2"));
        assertEquals(Optional.empty(), SemanticVersion.parse("   "));
    }
}