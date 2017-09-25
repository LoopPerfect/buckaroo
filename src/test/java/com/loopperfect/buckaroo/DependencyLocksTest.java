package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static com.loopperfect.buckaroo.Either.left;
import static org.junit.Assert.*;

public final class DependencyLocksTest {

    @Test
    public void equals() throws Exception {

        final DependencyLocks a = DependencyLocks.of(
            ImmutableList.of(
                RecipeIdentifier.of("org", "example")),
            ImmutableList.of(
                ResolvedPlatformDependencies.of(
                    "^linux.*",
                    ImmutableList.of(
                        RecipeIdentifier.of("org", "linux-only")))),
            ImmutableMap.of(
                RecipeIdentifier.of("org", "example"),
                ResolvedDependency.of(left(GitCommit.of("https://github.com/org/example/commit", "c7355d5"))),
                RecipeIdentifier.of("org", "linux-only"),
                ResolvedDependency.of(left(GitCommit.of("https://github.com/org/linux-only/commit", "b8945e7")))));

        final DependencyLocks b = DependencyLocks.of(
            ImmutableList.of(
                RecipeIdentifier.of("org", "example")),
            ImmutableList.of(
                ResolvedPlatformDependencies.of(
                    "^linux.*",
                    ImmutableList.of(
                        RecipeIdentifier.of("org", "linux-only")))),
            ImmutableMap.of(
                RecipeIdentifier.of("org", "example"),
                ResolvedDependency.of(left(GitCommit.of("https://github.com/org/example/commit", "c7355d5"))),
                RecipeIdentifier.of("org", "linux-only"),
                ResolvedDependency.of(left(GitCommit.of("https://github.com/org/linux-only/commit", "b8945e7")))));

        assertEquals(a, b);
    }
}