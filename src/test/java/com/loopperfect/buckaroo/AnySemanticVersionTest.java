package com.loopperfect.buckaroo;

import org.junit.Test;

import static org.junit.Assert.*;

public final class AnySemanticVersionTest {

    @Test
    public void isSatisfiedBy() {

        assertTrue(AnySemanticVersion.of().isSatisfiedBy(SemanticVersion.of(1, 2, 3)));
        assertTrue(AnySemanticVersion.of(1).isSatisfiedBy(SemanticVersion.of(1, 2, 3)));
        assertTrue(AnySemanticVersion.of(1, 2).isSatisfiedBy(SemanticVersion.of(1, 2, 3)));

        assertFalse(AnySemanticVersion.of(3).isSatisfiedBy(SemanticVersion.of(1, 2, 3)));
        assertFalse(AnySemanticVersion.of(2).isSatisfiedBy(SemanticVersion.of(1, 2, 3)));
        assertFalse(AnySemanticVersion.of(1, 7).isSatisfiedBy(SemanticVersion.of(1, 2, 3)));
        assertFalse(AnySemanticVersion.of(4, 2).isSatisfiedBy(SemanticVersion.of(1, 2, 3)));
    }

    @Test
    public void encode() {

        assertEquals("*", AnySemanticVersion.of().encode());
        assertEquals("1.*", AnySemanticVersion.of(1).encode());
        assertEquals("1.2.*", AnySemanticVersion.of(1, 2).encode());
    }

    @Test
    public void equals() {

        assertEquals(AnySemanticVersion.of(), AnySemanticVersion.of());
        assertEquals(AnySemanticVersion.of(3), AnySemanticVersion.of(3));
        assertEquals(AnySemanticVersion.of(1, 2), AnySemanticVersion.of(1, 2));

        assertNotEquals(AnySemanticVersion.of(1, 2), AnySemanticVersion.of());
        assertNotEquals(AnySemanticVersion.of(1, 2), AnySemanticVersion.of(1));
        assertNotEquals(AnySemanticVersion.of(1, 2), AnySemanticVersion.of(2));
        assertNotEquals(AnySemanticVersion.of(2), AnySemanticVersion.of(2, 1));

        assertNotEquals(AnySemanticVersion.of(2), null);
        assertNotEquals(AnySemanticVersion.of(1, 2), "1.2");
    }
}