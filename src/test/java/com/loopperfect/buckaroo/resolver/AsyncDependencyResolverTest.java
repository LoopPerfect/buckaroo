package com.loopperfect.buckaroo.resolver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.Process;
import com.loopperfect.buckaroo.sources.RecipeFetchException;
import com.loopperfect.buckaroo.sources.RecipeSources;
import com.loopperfect.buckaroo.versioning.AnySemanticVersion;
import org.javatuples.Pair;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

        final RecipeSource recipeSource = new RecipeSource() {
            @Override
            public Process<Event, Recipe> fetch(final RecipeIdentifier identifier) {
                if (identifier.equals(identifier)) {
                    return Process.just(example);
                }
                return Process.error(
                    new RecipeFetchException(this, identifier, "Could not find " + identifier.encode() + ". "));
            }
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

        final RecipeSource recipeSource = new RecipeSource() {
            @Override
            public Process<Event, Recipe> fetch(final RecipeIdentifier identifier) {
                if (identifier.equals(RecipeIdentifier.of("org", "example-a"))) {
                    return Process.just(recipeA);
                }
                if (identifier.equals(RecipeIdentifier.of("org", "example-b"))) {
                    return Process.just(recipeB);
                }
                return Process.error(new RecipeFetchException(
                    this, identifier, "Could not find " + identifier.encode() + ". "));
            }
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

        final CountDownLatch latch = new CountDownLatch(1);

        AsyncDependencyResolver.resolve(recipeSource, toResolve).result().subscribe(
            actual -> {
                assertEquals(expected, actual);
                latch.countDown();
            }, error -> {
                latch.countDown();
            });

        latch.await(5000L, TimeUnit.MILLISECONDS);
    }

    // TODO: Re-enable this test once the resolver does not use recursion.

//    private static Recipe createRecipeForResolveDeepTransitive(final int depth) throws Exception {
//        return Recipe.of(
//            "Example " + depth,
//            "https://github.com/org/example-" + depth,
//            ImmutableMap.of(
//                SemanticVersion.of(1),
//                RecipeVersion.of(
//                    RemoteArchive.of(
//                        new URL("https://github.com/org/example-" + depth + ".zip"),
//                        Hash.sha256("example" + depth)),
//                    Optional.empty(),
//                    DependencyGroup.of(depth > 0 ?
//                        ImmutableMap.of(
//                            RecipeIdentifier.of("org", "example-" + (depth - 1)), AnySemanticVersion.of()) :
//                        ImmutableMap.of()),
//                    Optional.empty())));
//    }
//
//    @Test
//    public void resolveDeepTransitive() throws Exception {
//
//        final RecipeSource recipeSource = recipeIdentifier -> {
//            try {
//                int index = Integer.parseInt(recipeIdentifier.recipe.name.split("-")[1]);
//                return Process.just(createRecipeForResolveDeepTransitive(index));
//            } catch (final Throwable e){
//                return Process.error(new FetchRecipeException("Could not find " + recipeIdentifier.encode() + ". "));
//            }
//        };
//
//        final CountDownLatch latch = new CountDownLatch(1);
//
//        final int depth = 256; // 85
//
//        final ImmutableList<Dependency> toResolve = ImmutableList.of(
//            Dependency.of(RecipeIdentifier.of("org", "example-" + depth), AnySemanticVersion.of()));
//
//        AsyncDependencyResolver.resolve(recipeSource, toResolve).toObservable()
//            .subscribe(next -> {
//
//            }, error -> {
//                latch.countDown();
//            }, () -> {
//                latch.countDown();
//            });
//
//        latch.await(5000L, TimeUnit.MILLISECONDS);
//    }
}
