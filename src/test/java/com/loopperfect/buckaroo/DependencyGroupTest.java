package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.versioning.AnySemanticVersion;
import org.junit.Test;

import static org.junit.Assert.*;

public final class DependencyGroupTest {

    @Test
    public void addDependencies() throws Exception {

        final DependencyGroup a = DependencyGroup.of(ImmutableMap.of(
            RecipeIdentifier.of(Identifier.of("google"), Identifier.of("gtest")), AnySemanticVersion.of()));

        final DependencyGroup b = DependencyGroup.of(ImmutableMap.of(
            RecipeIdentifier.of(Identifier.of("google"), Identifier.of("gtest")), AnySemanticVersion.of(),
            RecipeIdentifier.of(Identifier.of("github"), Identifier.of("cmark")), AnySemanticVersion.of()));

        final DependencyGroup c = a.addDependencies(ImmutableList.of(Dependency.of(
            RecipeIdentifier.of(Identifier.of("github"), Identifier.of("cmark")), AnySemanticVersion.of())));

        assertEquals(b, c);
    }
}