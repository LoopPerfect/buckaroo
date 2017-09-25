package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.versioning.AnySemanticVersion;
import org.javatuples.Pair;
import org.junit.Test;

import static org.junit.Assert.*;

public final class PlatformDependencyGroupTest {

    @Test
    public void equals1() throws Exception {

        final PlatformDependencyGroup a = PlatformDependencyGroup.of();
        final PlatformDependencyGroup b = PlatformDependencyGroup.of();
        final PlatformDependencyGroup c = PlatformDependencyGroup.of(
            Pair.with("^linux.*", DependencyGroup.of()));

        assertEquals(a, b);
        assertTrue(a.equals(b));

        assertNotEquals(a, c);
        assertFalse(a.equals(c));
    }

    @Test
    public void equals2() throws Exception {

        final PlatformDependencyGroup a = PlatformDependencyGroup.of(
            Pair.with("^linux.*", DependencyGroup.of())
        );

        final PlatformDependencyGroup b = PlatformDependencyGroup.of(
            Pair.with("^linux.*", DependencyGroup.of())
        );

        assertEquals(a, b);
        assertTrue(a.equals(b));
    }

    @Test
    public void equals3() throws Exception {

        final PlatformDependencyGroup a = PlatformDependencyGroup.of(
            Pair.with("^linux.*", DependencyGroup.of(ImmutableMap.of(
                RecipeIdentifier.of("org", "example"), AnySemanticVersion.of()))));

        final PlatformDependencyGroup b = PlatformDependencyGroup.of(
            Pair.with("^linux.*", DependencyGroup.of(ImmutableMap.of(
                RecipeIdentifier.of("org", "example"), AnySemanticVersion.of()))));

        assertEquals(a, b);
        assertTrue(a.equals(b));
    }
}