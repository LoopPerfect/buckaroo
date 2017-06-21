package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.versioning.AnySemanticVersion;
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
                RecipeVersion.of(
                    Either.left(GitCommit.of("https://github.com/magicco/magiclib/commit", "b0215d5")),
                    Optional.of("my-magic-lib"),
                    DependencyGroup.of(ImmutableMap.of(
                        RecipeIdentifier.of("megacorp", "json"), AnySemanticVersion.of())),
                    Optional.empty()))));

        final ResolvedDependencies b = ResolvedDependencies.of(ImmutableMap.of(
            RecipeIdentifier.of("org", "project"),
            Pair.with(
                SemanticVersion.of(1),
                RecipeVersion.of(
                    Either.left(GitCommit.of("https://github.com/magicco/magiclib/commit", "b0215d5")),
                    Optional.of("my-magic-lib"),
                    DependencyGroup.of(ImmutableMap.of(
                        RecipeIdentifier.of("megacorp", "json"), AnySemanticVersion.of())),
                    Optional.empty()))));

        assertEquals(a, b);
    }
}