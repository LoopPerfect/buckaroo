package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.javatuples.Pair;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public final class ResolvedDependenciesTest {

    @Test
    public void equals() throws Exception {

        final ResolvedDependencies a = ResolvedDependencies.of(ImmutableMap.of(
            RecipeIdentifier.of("org", "project"),
            Pair.with(
                SemanticVersion.of(1),
                ResolvedDependency.of(
                    Either.left(GitCommit.of("https://github.com/magicco/magiclib/commit", "b0215d5")),
                    Optional.of("my-magic-lib"),
                    Optional.empty(),
                    ImmutableList.of(RecipeIdentifier.of("megacorp", "json"))))));

        final ResolvedDependencies b = ResolvedDependencies.of(ImmutableMap.of(
            RecipeIdentifier.of("org", "project"),
            Pair.with(
                SemanticVersion.of(1),
                ResolvedDependency.of(
                    Either.left(GitCommit.of("https://github.com/magicco/magiclib/commit", "b0215d5")),
                    Optional.of("my-magic-lib"),
                    Optional.empty(),
                    ImmutableList.of(RecipeIdentifier.of("megacorp", "json"))))));

        assertEquals(a, b);
    }
}