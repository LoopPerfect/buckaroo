package com.loopperfect.buckaroo.github;

import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.resolver.AsyncDependencyResolver;
import com.loopperfect.buckaroo.sources.GitProviderRecipeSource;
import com.loopperfect.buckaroo.versioning.AnySemanticVersion;
import io.reactivex.Single;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public final class AsyncDependencyResolverTest {

    @Test
    public void resolverWorksWithGitHub1() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final RecipeSource recipeSource = GitProviderRecipeSource.of(
            fs,
            GitHubGitProvider.of());

        final Single<ResolvedDependencies> task = AsyncDependencyResolver.resolve(
            recipeSource,
            ImmutableList.of(Dependency.of(
                RecipeIdentifier.of("github", "njlr", "test-lib-a"),
                AnySemanticVersion.of())))
            .result();

        final ResolvedDependencies resolved =
            task.timeout(120, TimeUnit.SECONDS).blockingGet();

        assertTrue(resolved.dependencies.containsKey(RecipeIdentifier.of("github", "njlr", "test-lib-a")));
        assertTrue(resolved.dependencies.containsKey(RecipeIdentifier.of("github", "njlr", "test-lib-b")));
        assertTrue(resolved.dependencies.containsKey(RecipeIdentifier.of("github", "njlr", "test-lib-c")));
    }

    @Test
    public void resolverWorksWithGitHub2() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final RecipeSource recipeSource = GitProviderRecipeSource.of(
            fs,
            GitHubGitProvider.of());

        final Single<ResolvedDependencies> task = AsyncDependencyResolver.resolve(
            recipeSource,
            ImmutableList.of(Dependency.of(
                RecipeIdentifier.of("github", "njlr", "test-lib-e"),
                AnySemanticVersion.of())))
            .result();

        final ResolvedDependencies resolved =
            task.timeout(120, TimeUnit.SECONDS).blockingGet();

        assertTrue(resolved.dependencies.containsKey(
            RecipeIdentifier.of("github", "njlr", "test-lib-e")));
    }
}
