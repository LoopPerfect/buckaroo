package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.versioning.WildcardVersion;
import org.junit.Test;

import static org.junit.Assert.*;

public final class DependencyGroupTest {

    @Test
    public void addDependencies() throws Exception {

        final DependencyGroup a = DependencyGroup.of(ImmutableMap.of(
            RecipeIdentifier.of(Identifier.of("google"), Identifier.of("gtest")), WildcardVersion.of()));

        final DependencyGroup b = DependencyGroup.of(ImmutableMap.of(
            RecipeIdentifier.of(Identifier.of("google"), Identifier.of("gtest")), WildcardVersion.of(),
            RecipeIdentifier.of(Identifier.of("github"), Identifier.of("cmark")), WildcardVersion.of()));

        final DependencyGroup c = a.add(ImmutableList.of(Dependency.of(
            RecipeIdentifier.of(Identifier.of("github"), Identifier.of("cmark")), WildcardVersion.of())));

        assertEquals(b, c);
    }
}