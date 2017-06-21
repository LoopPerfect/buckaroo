package com.loopperfect.buckaroo.resolver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.Process;
import com.loopperfect.buckaroo.sources.RecipeSources;
import com.loopperfect.buckaroo.versioning.AnySemanticVersion;
import org.javatuples.Pair;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public final class AsyncDependencyResolverTest {

    @Test
    public void resolveEmpty() throws Exception {

        assertEquals(
            ResolvedDependencies.of(),
            AsyncDependencyResolver.resolve(RecipeSources.empty(), ImmutableList.of()).result().blockingGet());
    }

    @Test
    public void resolveSimple() throws Exception {

        final RecipeIdentifier identifier = RecipeIdentifier.of("org", "example");

        final Recipe example = Recipe.of(
            "example",
            "https://github.com/org/example",
            ImmutableMap.of(
                SemanticVersion.of(1),
                RecipeVersion.of(
                    GitCommit.of("https://github.com/org/example/commit", "b4515d5"),
                    Optional.empty(),
                    DependencyGroup.of(),
                    Optional.empty())));

        final RecipeSource recipeSource = recipeIdentifier -> {
            if (recipeIdentifier.equals(identifier)) {
                return Process.just(example);
            }
            return Process.error(new FetchRecipeException("Could not find " + recipeIdentifier.encode() + ". "));
        };

        final ImmutableList<Dependency> toResolve = ImmutableList.of(
            Dependency.of(identifier, AnySemanticVersion.of()));

        final ResolvedDependencies expected = ResolvedDependencies.of(ImmutableMap.of(
            identifier,
            Pair.with(
                SemanticVersion.of(1),
                    example.versions.get(SemanticVersion.of(1)))));

        assertEquals(
            expected,
            AsyncDependencyResolver.resolve(recipeSource, toResolve).result().blockingGet());
    }

    @Test
    public void resolveSimpleTransitive() throws Exception {

        final Recipe recipeA = Recipe.of(
            "Example A",
            "https://github.com/org/example-a",
            ImmutableMap.of(
                SemanticVersion.of(1),
                RecipeVersion.of(
                    GitCommit.of("https://github.com/org/example-a/commit", "a0215d5"),
                    Optional.empty(),
                    DependencyGroup.of(ImmutableMap.of(
                        RecipeIdentifier.of("org", "example-b"), AnySemanticVersion.of())),
                    Optional.empty())));

        final Recipe recipeB = Recipe.of(
            "Example B",
            "https://github.com/org/example-b",
            ImmutableMap.of(
                SemanticVersion.of(1),
                RecipeVersion.of(
                    GitCommit.of("https://github.com/org/example-b/commit", "b7315e5"),
                    Optional.empty(),
                    DependencyGroup.of(),
                    Optional.empty())));

        final RecipeSource recipeSource = recipeIdentifier -> {
            if (recipeIdentifier.equals(RecipeIdentifier.of("org", "example-a"))) {
                return Process.just(recipeA);
            }
            if (recipeIdentifier.equals(RecipeIdentifier.of("org", "example-b"))) {
                return Process.just(recipeB);
            }
            return Process.error(new FetchRecipeException("Could not find " + recipeIdentifier.encode() + ". "));
        };

        final ImmutableList<Dependency> toResolve = ImmutableList.of(
            Dependency.of(RecipeIdentifier.of("org", "example-a"), AnySemanticVersion.of()));

        final ResolvedDependencies expected = ResolvedDependencies.of(ImmutableMap.of(
            RecipeIdentifier.of("org", "example-a"),
            Pair.with(
                SemanticVersion.of(1),
                recipeA.versions.get(SemanticVersion.of(1))),
            RecipeIdentifier.of("org", "example-b"),
            Pair.with(
                SemanticVersion.of(1),
                recipeB.versions.get(SemanticVersion.of(1)))));

            assertEquals(
            expected,
            AsyncDependencyResolver.resolve(recipeSource, toResolve).result().blockingGet());
    }
}
